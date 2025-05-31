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

#ifdef __cplusplus

#import "xxhash.h"
#import <type_traits>

namespace TMM {

/// 计算所有入参的浮点数的 hash
template<typename... Floats>
OS_ALWAYS_INLINE auto hashFloats(Floats... floats)
-> std::enable_if_t<(std::is_same_v<Floats, float> && ...), uint64_t> {
    const float data[] = {floats...};
    return XXH64(data, sizeof(data), 0);
}


/// 合并两个 NSUInteger 生成一个 hash
OS_ALWAYS_INLINE uint64_t hashMerge(NSUInteger a, NSUInteger b) noexcept;

/// 合并三个 NSUInteger 生成一个 hash
OS_ALWAYS_INLINE uint64_t hashMerge(NSUInteger a, NSUInteger b, NSUInteger c) noexcept;

}

#endif
