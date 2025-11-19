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

#include "oh_native_text_image_cache.h"
#include "../xcomponent_log.h"

namespace OH {

OHNativeTextImageCache& OHNativeTextImageCache::sharedInstance() {
    static OHNativeTextImageCache instance;
    return instance;
}

void OHNativeTextImageCache::setPixelMap(int32_t cacheKey, OH_PixelmapNative* pixelMap) {
    if (pixelMap == nullptr) {
        return;
    }

    std::lock_guard<std::mutex> lock(mutex_);
    
    // 如果已存在相同的 key，先移除旧的（注意：这里不释放旧的 PixelMap，由调用者管理生命周期）
    auto it = cache_.find(cacheKey);
    if (it != cache_.end()) {
        // 旧的 PixelMap 由调用者负责释放，这里只更新指针
        LOGI("OHNativeTextImageCache::setPixelMap: replacing existing cache for key=%{public}d", cacheKey);
    }
    
    cache_[cacheKey] = pixelMap;
    LOGI("OHNativeTextImageCache::setPixelMap: cached pixelMap for key=%{public}d, totalCount=%{public}zu", 
         cacheKey, cache_.size());
}

OH_PixelmapNative* OHNativeTextImageCache::getPixelMap(int32_t cacheKey) {
    std::lock_guard<std::mutex> lock(mutex_);
    
    auto it = cache_.find(cacheKey);
    if (it != cache_.end()) {
        LOGI("OHNativeTextImageCache::getPixelMap: found cache for key=%{public}d", cacheKey);
        return it->second;
    }
    
    LOGI("OHNativeTextImageCache::getPixelMap: cache miss for key=%{public}d", cacheKey);
    return nullptr;
}

void OHNativeTextImageCache::removePixelMap(int32_t cacheKey) {
    std::lock_guard<std::mutex> lock(mutex_);
    
    auto it = cache_.find(cacheKey);
    if (it != cache_.end()) {
        // 注意：这里不释放 PixelMap，由调用者管理生命周期
        cache_.erase(it);
        LOGI("OHNativeTextImageCache::removePixelMap: removed cache for key=%{public}d, totalCount=%{public}zu", 
             cacheKey, cache_.size());
    }
}

void OHNativeTextImageCache::clear() {
    std::lock_guard<std::mutex> lock(mutex_);
    
    size_t count = cache_.size();
    // 注意：这里不释放 PixelMap，由调用者管理生命周期
    cache_.clear();
    LOGI("OHNativeTextImageCache::clear: cleared all caches, previousCount=%{public}zu", count);
}

size_t OHNativeTextImageCache::getCount() const {
    std::lock_guard<std::mutex> lock(mutex_);
    return cache_.size();
}

} // namespace OH

