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
    CALayerSaveStateMakeTypeError,     // 错误类型
};

typedef struct CALayerTransform3DInfo {
    CATransform3D transform;
    CGPoint instantlyTranslate;
} CALayerTransform3DInfo;

typedef struct CALayerSaveState {
    CATransform3D transform;
    CGFloat translateX;
    CGFloat translateY;
    int clipCount;
    CALayerSaveStateMakeType makeType; // 该次 save 产生的类型
    CALayerTransform3DInfo transformInfo;
    bool hasRotateOrScale; // 该次 save 是否存在 scale 或者 rotate 操作
} CALayerSaveState;

/// 创建一个 Identity 类型的 CALayerTransform3DInfo
FOUNDATION_EXTERN OS_ALWAYS_INLINE CALayerTransform3DInfo CALayerTransform3DInfoIdentity(void);

/// 创建一个作为守卫标志的 CALayerSaveState
FOUNDATION_EXTERN OS_ALWAYS_INLINE CALayerSaveState CALayerSaveStateCreateSafeGuard(void);

/// 根据 CATransform3D 生成一个 NSString
/// - Parameter transForm: CATransform3D
FOUNDATION_EXTERN NSString *TMMNSStringFromCATransform3D(CATransform3D transForm);

NS_ASSUME_NONNULL_END
