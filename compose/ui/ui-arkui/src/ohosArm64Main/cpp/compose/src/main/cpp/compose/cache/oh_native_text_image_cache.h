/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 Tencent. All rights reserved.
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

#ifndef OH_NATIVE_TEXT_IMAGE_CACHE_H
#define OH_NATIVE_TEXT_IMAGE_CACHE_H

#include <multimedia/image_framework/image/pixelmap_native.h>
#include <mutex>
#include <unordered_map>

namespace OH {

/**
 * OHNativeTextImageCache - 文本图像缓存类
 * 参考 iOS TMMComposeMemoryCache 实现
 *
 * 设计特点：
 * - 单例模式：sharedInstance() 返回全局唯一实例
 * - 线程安全：使用 std::mutex 保护缓存操作
 * - 存储结构：使用 std::unordered_map 存储缓存
 * - 暂时简化实现，后续可以优化为完整 LRU（使用双向链表）
 */
class OHNativeTextImageCache {
public:
    /**
     * 获取单例实例
     */
    static OHNativeTextImageCache &sharedInstance();

    /**
     * 设置缓存
     * @param cacheKey 缓存键（paragraphHashCode）
     * @param pixelMap PixelMap 指针
     */
    void setPixelMap(int32_t cacheKey, OH_PixelmapNative *pixelMap);

    /**
     * 获取缓存
     * @param cacheKey 缓存键（paragraphHashCode）
     * @return PixelMap 指针，如果不存在则返回 nullptr
     */
    OH_PixelmapNative *getPixelMap(int32_t cacheKey);

    /**
     * 移除缓存
     * @param cacheKey 缓存键（paragraphHashCode）
     */
    void removePixelMap(int32_t cacheKey);

    /**
     * 清空所有缓存
     */
    void clear();

    /**
     * 获取缓存数量
     */
    size_t getCount() const;

private:
    OHNativeTextImageCache() = default;
    ~OHNativeTextImageCache() = default;
    OHNativeTextImageCache(const OHNativeTextImageCache &) = delete;
    OHNativeTextImageCache &operator=(const OHNativeTextImageCache &) = delete;

    mutable std::mutex mutex_;
    std::unordered_map<int32_t, OH_PixelmapNative *> cache_;
};

} // namespace OH

#endif // OH_NATIVE_TEXT_IMAGE_CACHE_H
