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

#import "TMMNativeEnums.h"
#import <UIKit/UIKit.h>

#ifdef __cplusplus

namespace TMM {

/// 描述一个绘制指令
struct DrawingItem {
    /// 相同类型且、同一类型下的指令顺序一致 item hash 一致
    uint64_t itemHash = 0;

    /// 指令绘制内容的 hash
    uint64_t contentsHash = 0;

    /// 一次 save 和 restore 中 clip 的出现的次序
    int clipIndex = 0;

    /// 当前指令的类型
    TMMNativeDrawingType drawingType = TMMNativeDrawingTypeNone;

    /// 固定的 用于 Pop 的指令
    static const DrawingItem DrawingPopItem;
};

} // namespace TMM

#endif
