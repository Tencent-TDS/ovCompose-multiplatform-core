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

#import "TMMNativeBasicShader.h"
#import "TMMNativeEnums.h"

NS_ASSUME_NONNULL_BEGIN

/// 图片渐变
@interface TMMNativeImageShader : TMMNativeBasicShader

/// 原始图片
@property (nonatomic, strong) UIImage *image;

/// x 方向的 tileMode
@property (nonatomic, assign) TMMNativeTileMode tileModeX;

/// y 方向的 tileMode
@property (nonatomic, assign) TMMNativeTileMode tileModeY;

@end

NS_ASSUME_NONNULL_END
