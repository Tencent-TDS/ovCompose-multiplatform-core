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

#import "TMMXXHashFuncs.h"

namespace TMM {

constexpr uint64_t kHashPrime = 0x9E3779B97F4A7C15ULL;

OS_ALWAYS_INLINE uint64_t hashMerge(NSUInteger a, NSUInteger b) noexcept {
    const uint64_t ua = static_cast<uint64_t>(a);
    const uint64_t ub = static_cast<uint64_t>(b);
    const uint64_t base_hash = 31 * ua + ub;
    return base_hash ^ (ub * kHashPrime);
}

// 三参数哈希合并函数
OS_ALWAYS_INLINE uint64_t hashMerge(NSUInteger a, NSUInteger b, NSUInteger c) noexcept {
    const uint64_t ua = static_cast<uint64_t>(a);
    const uint64_t ub = static_cast<uint64_t>(b);
    const uint64_t uc = static_cast<uint64_t>(c);

    uint64_t h = (ua * 0x9E3779B97F4A7C15ULL) ^ (ub * 0x6C8E9CF570932BABULL) ^ (uc * 0x41C3C4F712193BEFULL);

    h += 0xAAAAAAAAAAAAAAAAULL;
    h ^= h >> 33;
    h *= 0x62A9D9ED799705F5ULL;
    h ^= h >> 29;
    h += (ub << 24) | (uc >> 40);

    h ^= h >> 32;
    h *= 0x88D3E5F4F60DEDCDULL;
    h ^= h >> 37;
    return h;
}

}
