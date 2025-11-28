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

#include "oh_drawing_pixelmap_cache.h"
#include "../xcomponent_log.h"

namespace OH {

OHDrawingPixelMapCache &OHDrawingPixelMapCache::sharedInstance() {
    static OHDrawingPixelMapCache instance;
    return instance;
}

OH_Drawing_PixelMap *OHDrawingPixelMapCache::getOrCreate(OH_PixelmapNative *pixelMap) {
    if (pixelMap == nullptr) {
        LOGE("OHDrawingPixelMapCache::getOrCreate - Input pixelMap is null");
        return nullptr;
    }

    std::lock_guard<std::mutex> lock(mutex_);

    // 检查缓存是否存在
    auto it = cache_.find(pixelMap);
    if (it != cache_.end()) {
        // 缓存命中
        cacheHits_++;
        LOGI("OHDrawingPixelMapCache::getOrCreate - Cache HIT for pixelMap=%{public}p (hits=%{public}zu, misses=%{public}zu, size=%{public}zu)",
             pixelMap, cacheHits_, cacheMisses_, cache_.size());
        return it->second;
    }

    // 缓存未命中，需要创建新的OH_Drawing_PixelMap
    cacheMisses_++;
    LOGI("OHDrawingPixelMapCache::getOrCreate - Cache MISS for pixelMap=%{public}p, creating new OH_Drawing_PixelMap...",
         pixelMap);

    // 调用系统API进行转换
    // 注意：根据OpenHarmony文档，这个调用可能涉及内存复制，
    // 对于大图片可能耗时较长（4K图片约20ms）
    OH_Drawing_PixelMap *drawingPixelMap = OH_Drawing_PixelMapGetFromOhPixelMapNative(pixelMap);

    if (drawingPixelMap != nullptr) {
        // 成功创建，添加到缓存
        cache_[pixelMap] = drawingPixelMap;
        LOGI("OHDrawingPixelMapCache::getOrCreate - Successfully created and cached OH_Drawing_PixelMap=%{public}p for pixelMap=%{public}p (cache size=%{public}zu)",
             drawingPixelMap, pixelMap, cache_.size());
    } else {
        LOGE("OHDrawingPixelMapCache::getOrCreate - Failed to create OH_Drawing_PixelMap for pixelMap=%{public}p",
             pixelMap);
    }

    return drawingPixelMap;
}

void OHDrawingPixelMapCache::remove(OH_PixelmapNative *pixelMap) {
    if (pixelMap == nullptr) {
        return;
    }

    std::lock_guard<std::mutex> lock(mutex_);

    auto it = cache_.find(pixelMap);
    if (it != cache_.end()) {
        LOGI("OHDrawingPixelMapCache::remove - Removing pixelMap=%{public}p from cache (size before: %{public}zu)",
             pixelMap, cache_.size());

        // 注意：OH_Drawing_PixelMap的生命周期由系统管理
        // 我们只移除缓存引用，不负责释放内存
        cache_.erase(it);

        LOGI("OHDrawingPixelMapCache::remove - Removed pixelMap=%{public}p (cache size after: %zu)",
             pixelMap, cache_.size());
    } else {
        LOGE("OHDrawingPixelMapCache::remove - PixelMap=%{public}p not found in cache", pixelMap);
    }
}

void OHDrawingPixelMapCache::clear() {
    std::lock_guard<std::mutex> lock(mutex_);

    size_t oldSize = cache_.size();
    cache_.clear();

    LOGI("OHDrawingPixelMapCache::clear - Cache cleared (removed %zu entries)", oldSize);
}

size_t OHDrawingPixelMapCache::getCount() const {
    std::lock_guard<std::mutex> lock(mutex_);
    return cache_.size();
}

void OHDrawingPixelMapCache::getStats(size_t &outHits, size_t &outMisses) const {
    std::lock_guard<std::mutex> lock(mutex_);
    outHits = cacheHits_;
    outMisses = cacheMisses_;
}

void OHDrawingPixelMapCache::resetStats() {
    std::lock_guard<std::mutex> lock(mutex_);
    cacheHits_ = 0;
    cacheMisses_ = 0;
    LOGI("OHDrawingPixelMapCache::resetStats - Cache statistics reset");
}

OHDrawingPixelMapCache::~OHDrawingPixelMapCache() {
    std::lock_guard<std::mutex> lock(mutex_);

    // 输出最终统计信息
    if (cacheHits_ > 0 || cacheMisses_ > 0) {
        float hitRate = (cacheHits_ + cacheMisses_) > 0 ? (float)cacheHits_ / (cacheHits_ + cacheMisses_) * 100.0f : 0.0f;
        LOGI("OHDrawingPixelMapCache::~OHDrawingPixelMapCache - Final stats: hits=%{public}zu, misses=%{public}zu, hit_rate=%{public}.2f%%, cache_size=%{public}zu",
             cacheHits_, cacheMisses_, hitRate, cache_.size());
    }

    // 清空缓存
    // 注意：OH_Drawing_PixelMap的生命周期由系统管理，这里只清空map
    cache_.clear();
}

} // namespace OH
