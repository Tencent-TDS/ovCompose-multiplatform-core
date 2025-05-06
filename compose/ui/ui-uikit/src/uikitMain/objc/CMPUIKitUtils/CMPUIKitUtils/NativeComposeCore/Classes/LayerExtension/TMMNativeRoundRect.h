/*
 * Tencent is pleased to support the open source community by making ovCompose
 * available. Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights
 * reserved.
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

NS_ASSUME_NONNULL_BEGIN

/// 该类主要是包装了一些圆角参数，将会被直接应用到 Kotlin 侧
@interface TMMNativeRoundRect : NSObject

/// 当前 rect 的左边
@property(nonatomic, assign) float left;

/// 当前 rect 的顶部
@property(nonatomic, assign) float top;

/// 当前 rect 的右边
@property(nonatomic, assign) float right;

/// 当前 rect 的底部
@property(nonatomic, assign) float bottom;

/// 视图左上角圆角的X轴半径（水平方向）
/// 与topLeftCornerRadiusY配合使用可实现不对称圆角
@property(nonatomic, assign) float topLeftCornerRadiusX;

/// 视图左上角圆角的X轴半径（水平方向）
/// 与topLeftCornerRadiusY配合使用可实现不对称圆角
@property(nonatomic, assign) float topLeftCornerRadiusY;

/// 视图右上角圆角的X轴半径（水平方向）
/// 与topRightCornerRadiusY配合使用可实现不对称圆角
@property(nonatomic, assign) float topRightCornerRadiusX;

/// 视图右上角圆角的Y轴半径（垂直方向）
/// 与topRightCornerRadiusX配合使用可实现不对称圆角
@property(nonatomic, assign) float topRightCornerRadiusY;

/// 视图右下角圆角的X轴半径（水平方向）
/// 与bottomRightCornerRadiusY配合使用可实现不对称圆角
@property(nonatomic, assign) float bottomRightCornerRadiusX;

// 视图右下角圆角的Y轴半径（垂直方向）
// 与bottomRightCornerRadiusX配合使用可实现不对称圆角
@property(nonatomic, assign) float bottomRightCornerRadiusY;

// 视图左下角圆角的X轴半径（水平方向）
// 与bottomLeftCornerRadiusY配合使用可实现不对称圆角
@property(nonatomic, assign) float bottomLeftCornerRadiusX;

// 视图左下角圆角的Y轴半径（垂直方向）
// 与bottomLeftCornerRadiusX配合使用可实现不对称圆角
@property(nonatomic, assign) float bottomLeftCornerRadiusY;

/// 是否包含某个点
- (BOOL)containsWithPointX:(float)pointX pointY:(float)pointY;

/// 获取一个对所有字段取 hash 的值，如果一个对象上的字段的值一直，则获取的
/// datahash 一致
- (NSUInteger)dataHash;

@end

NS_ASSUME_NONNULL_END
