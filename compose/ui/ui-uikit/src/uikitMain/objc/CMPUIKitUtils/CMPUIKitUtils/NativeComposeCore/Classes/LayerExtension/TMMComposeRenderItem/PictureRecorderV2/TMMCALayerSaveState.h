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

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

typedef NS_ENUM(NSInteger, CALayerSaveStateMakeType) {
    CALayerSaveStateMakeTypeSafeGuard, // 安全守卫
    CALayerSaveStateMakeTypeSave,      // 纯 save 操作
    CALayerSaveStateMakeTypeClip,      // clip 产生的
};

typedef struct CALayerSaveState {
    CATransform3D transform;
    CGFloat translateX;
    CGFloat translateY;
    int clipCount;
    CALayerSaveStateMakeType makeType; // 该次 save 产生的类型
} CALayerSaveState;

/// 创建一个 作为守卫标志的 CALayerSaveState
FOUNDATION_EXTERN OS_ALWAYS_INLINE CALayerSaveState CALayerSaveStateCreateSafeGuard(void);

/// 根据 CATransform3D 生成一个 NSString
/// - Parameter transForm: CATransform3D
FOUNDATION_EXTERN NSString *TMMNSStringFromCATransform3D(CATransform3D transForm);

NS_ASSUME_NONNULL_END
