/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#import <Foundation/Foundation.h>
#import <CoreText/CoreText.h>
#import <UIKit/UIKit.h>
#import "OVComposeSkCopyTableFromFont.h"
#import "OVComposeSkiaOptFuncs.h"
#import <unordered_map>
#import <functional>
#import <array>
#import <list>

#pragma mark - Cache
/// 定义从 CTFontCopyTable 返回值的大小，进入缓存的阈值
static int gOVMPLargeFontTableSize = 4096;

// Key 和 Hash 定义保持不变
struct FontTableCacheKey {
    CTFontRef font;
    CTFontTableTag tableTag;
    CTFontTableOptions options;
    bool operator==(const FontTableCacheKey& other) const {
        return CFEqual(font, other.font) && tableTag == other.tableTag && options == other.options;
    }
};

struct FontTableCacheKeyHash {
    std::size_t operator()(const FontTableCacheKey& k) const {
        size_t h1 = CFHash(k.font);
        size_t h2 = std::hash<uint32_t>{}(k.tableTag);
        size_t h3 = std::hash<int>{}(k.options);
        size_t hash = h1;
        hash ^= h2 + 0x9e3779b9 + (hash << 6) + (hash >> 2);
        hash ^= h3 + 0x9e3779b9 + (hash << 6) + (hash >> 2);
        return hash;
    }
};

// 缓存管理类
class FontTableCache {
public:
    static FontTableCache& sharedInstance() {
        static FontTableCache instance;
        return instance;
    }
    CFDataRef tableFromFont(CTFontRef font, CTFontTableTag table, CTFontTableOptions options);
    FontTableCache(const FontTableCache&) = delete;
    void operator=(const FontTableCache&) = delete;

private:
    using CacheEntry = std::pair<FontTableCacheKey, CFDataRef>;
    using L2CacheList = std::list<CacheEntry>;

    // 缓存容量定义
    static constexpr size_t kL1Capacity = 48;
    static constexpr size_t kL2Capacity = 8;

    // L1: 静态数组缓存 (无 LRU)
    std::array<CacheEntry, kL1Capacity> l1Cache;
    size_t l1Count = 0;

    // L2: 简单 LRU 缓存
    L2CacheList l2CacheList;
    std::unordered_map<FontTableCacheKey, L2CacheList::iterator, FontTableCacheKeyHash> l2CacheMap;

private:
    FontTableCache() = default;
    ~FontTableCache();
};

FontTableCache::~FontTableCache() {
    for (size_t i = 0; i < l1Count; ++i) {
        CFRelease(l1Cache[i].second);
    }
    for (const auto& entry : l2CacheList) {
        CFRelease(entry.second);
    }
}

CFDataRef FontTableCache::tableFromFont(CTFontRef font, CTFontTableTag table, CTFontTableOptions options) {
    FontTableCacheKey key = {font, table, options};
    // 1. 查找 L1 缓存
    for (size_t i = 0; i < l1Count; ++i) {
        if (l1Cache[i].first == key) {
            CFDataRef data = l1Cache[i].second;
            CFRetain(data);
            return data;
        }
    }

    // 2. 查找 L2 LRU 缓存（大于 4096 byte 的）
    auto it = l2CacheMap.find(key);
    if (it != l2CacheMap.end()) {
        l2CacheList.splice(l2CacheList.begin(), l2CacheList, it->second);
        CFDataRef data = it->second->second;
        CFRetain(data);
        return data;
    }

    // 3. L1 和 L2 都未命中，从系统获取
    CFDataRef newData = CTFontCopyTable(font, table, options);
    if (!newData) {
        return NULL;
    }
    
    // 4. 缓存不同结果，大的进入 LRU，小的进入固定数组
    CFRetain(font);
    CFIndex length = CFDataGetLength(newData);
    CacheEntry newEntry = {key, newData};
    if (l1Count < kL1Capacity && length <= gOVMPLargeFontTableSize) {
        l1Cache[l1Count++] = newEntry;
    } else {
        if (l2CacheMap.size() >= kL2Capacity) {
            CacheEntry &entry = l2CacheList.back();
            l2CacheMap.erase(entry.first);
            CFRelease(entry.second);
            CFRelease(entry.first.font);
            l2CacheList.pop_back();
        }
        l2CacheList.push_front(newEntry);
        l2CacheMap[key] = l2CacheList.begin();
    }
    CFRetain(newData);
    return newData;
}

#pragma mark - SkCopyTableFromFont
/// SkCopyTableFromFont ANR 修复开关默认值
static bool gOVMPSkCopyTableFromFontFixEnabel = false;

void OVMPSkCopyTableFromFontSetFixEnabel(bool enable) {
    gOVMPSkCopyTableFromFontFixEnabel = enable;
}

/// 返回是否打开修复 SkCopyTableFromFont crash 的开关，由 skia 内部调用过来
FOUNDATION_EXTERN bool OVMPSkCopyTableFromFontFixEnabel(void) {
    return OVMPSkiaGlobalFOOMGetFixEnable() && gOVMPSkCopyTableFromFontFixEnabel;
}

/// 当打 OVMPSkCopyTableFromFontFixEnabel() 为 true，Skia 内部会调用此方法代替 CTFontCopyTable，尝试避免 crash
FOUNDATION_EXTERN CFDataRef OVMPSkCopyTableFromFont(CTFontRef font, CTFontTableTag table, CTFontTableOptions options) {
    return FontTableCache::sharedInstance().tableFromFont(font, table, options);
}
