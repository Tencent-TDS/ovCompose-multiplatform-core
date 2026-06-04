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
#import "TMMNativeEnums.h"

NS_ASSUME_NONNULL_BEGIN

@class TMMNativeRoundRect;

/// 该类将会在 Kotlin 侧进行初始化，并携带 Kotlin 侧的 Path 信息，最终传递给 Native 侧使用
/// 直接对接 Native 侧的贝塞尔曲线。
/// 额外说明：本类的所有给 kt 方法调用的方法，都尽可能使用基本数据类型，因此会存在参数较多的情况。主要是为了性能考虑
/// 主要是避免创建 kt 对象，比如 kt 侧传递 GCSize 等都会通过对象进行传递，而 kt 侧创建对象占据 GC 内存，并且效率远不如
/// 传递基本数据类型快。
@interface TMMComposeNativePath : NSObject

/// path 填充类型
@property (nonatomic, assign) TMMNativeDrawPathFillType pathFillType;

/// path 是否为凸的
@property (nonatomic, assign, readonly) BOOL isConvex;

/// path 是否为空
@property (nonatomic, assign, readonly) BOOL isEmpty;

/// path 的起始点
/// - Parameters:
///   - x: x 坐标
///   - y: y 坐标
- (void)moveTo:(float)x y:(float)y;

/// 相对移动到指定点
/// - Parameters:
///   - dx: x 变化量
///   - dy: y 变化量
- (void)relativeMoveTo:(float)dx dy:(float)dy;

/// 添加一条线段
/// - Parameters:
///   - x: x 坐标
///   - y: y 坐标
- (void)lineTo:(float)x y:(float)y;

/// 从当前点出发相对绘制直线
/// - Parameters:
///   - dx: x 变化量
///   - dy: y 变化量
- (void)relativeLineTo:(float)dx dy:(float)dy;

/// 添加一条二次贝塞尔曲线
/// - Parameters:
///   - x1: x1 坐标
///   - y1: y1 坐标
///   - x2: x2 坐标
///   - y2: y2 坐标
- (void)quadraticBezierTo:(float)x1 y1:(float)y1 x2:(float)x2 y2:(float)y2;

/// 相对添加一条二次贝塞尔曲线
/// - Parameters:
///   - dx1: x1 变化量
///   - dy1: y1 变化量
///   - dx2: x2 变化量
///   - dy2: y2 变化量
- (void)relativeQuadraticBezierTo:(float)dx1 dy1:(float)dy1 dx2:(float)dx2 dy2:(float)dy2;

/// 添加一条三次贝塞尔曲线
/// - Parameters:
///   - x1: x1 坐标
///   - y1: y1 坐标
///   - x2: x2 坐标
///   - y2: y2 坐标
///   - x3: x3 坐标
///   - y3: y3 坐标
- (void)cubicTo:(float)x1 y1:(float)y1 x2:(float)x2 y2:(float)y2 x3:(float)x3 y3:(float)y3;
;

/// 相对当前点位添加一条三次贝塞尔曲线
/// - Parameters:
///   - dx1: x1 变化量
///   - dy1: y1 变化量
///   - dx2: x2 变化量
///   - dy2: y2 变化量
///   - dx3: x3 变化量
///   - dy3: y3 变化量
- (void)relativeCubicTo:(float)dx1 dy1:(float)dy1 dx2:(float)dx2 dy2:(float)dy2 dx3:(float)dx3 dy3:(float)dy3;

/// 添加一个圆弧
/// - Parameters:
///   - left: 左上角 x 坐标
///   - top: 左上角 y 坐标
///   - right: 右上角 x 坐标
///   - bottom: 右下角 y 坐标
///   - startAngleDegrees: 弧线起始角度的参数
///   - sweepAngleDegrees: 弧线扫描的弧度角度
///   - forceMoveTo: YES=断开前一路径，NO=尝试连接
- (void)arcTo:(float)left
                  top:(float)top
                right:(float)right
               bottom:(float)bottom
    startAngleDegrees:(float)startAngleDegrees
    sweepAngleDegrees:(float)sweepAngleDegrees
          forceMoveTo:(BOOL)forceMoveTo;

/// 添加一个矩形
/// - Parameters:
///   - left:左上角 x 坐标
///   - top: 左上角 y 坐标
///   - right: 右上角 x 坐标
///   - bottom: 右下角 y 坐标
- (void)addRect:(float)left top:(float)top right:(float)right bottom:(float)bottom;

/// 添加一个椭圆
/// - Parameters:
///   - left: 左上角 x 坐标
///   - top: 左上角 y 坐标
///   - right: 右上角 x 坐标
///   - bottom: 右下角 y 坐标
- (void)addOval:(float)left top:(float)top right:(float)right bottom:(float)bottom;

/// 添加一个弧度的椭圆弧线
/// - Parameters:
///   - left: 左上角 x 坐标
///   - top: 左上角 y 坐标
///   - right: 右上角 x 坐标
///   - bottom: 右下角 y 坐标
///   - startAngleRadians: 弧线起始角度
///   - sweepAngleRadians: 弧线扫描的弧度
- (void)addArcRad:(float)left
                  top:(float)top
                right:(float)right
               bottom:(float)bottom
    startAngleRadians:(float)startAngleRadians
    sweepAngleRadians:(float)sweepAngleRadians;

/// 添加一个圆弧
/// - Parameters:
///   - left: 左上角 x 坐标
///   - top: 左上角 y 坐标
///   - right: 右上角 x 坐标
///   - bottom: 右下角 y 坐标
///   - startAngleDegrees: 弧线起始角度
///   - sweepAngleDegrees: 弧线扫描的弧度角度
- (void)addArc:(float)left
                  top:(float)top
                right:(float)right
               bottom:(float)bottom
    startAngleDegrees:(float)startAngleDegrees
    sweepAngleDegrees:(float)sweepAngleDegrees;

/// 添加一个圆角矩形
/// - Parameter roundRect: TMMNativeRoundRect
- (void)addRoundRect:(TMMNativeRoundRect *)roundRect;

/// - Parameters:
///   - path: 添加一个路径
///   - offsetX: x 偏移
///   - offsetY: y 偏移
- (void)addPath:(TMMComposeNativePath *)path offsetX:(float)offsetX offsetY:(float)offsetY;

/// Close 一个路径
- (void)close;

/// 重置
- (void)reset;

/// Rewinds
- (void)rewind;

/// 进行偏移
/// - Parameters:
///   - offsetX: x 偏移
///   - offsetY: y 偏移
- (void)translate:(float)offsetX offsetY:(float)offsetY;

/// 获取当前路径的 bounds
- (CGRect)getBounds;

/// 对两个 path 根据传入的 pathOperation 进行操作
/// - Parameters:
///   - path1: 第一个 path
///   - path2:  第二个 path
///   - pathOperation: TMMNativeDrawPathOperation
- (BOOL)op:(TMMComposeNativePath *)path1 path2:(TMMComposeNativePath *)path2 pathOperation:(TMMNativeDrawPathOperation)pathOperation;

/// 获取一个对所有字段取 hash 的值，如果一个对象上的字段的值一直，则获取的 datahash 一致
- (uint64_t)dataHash;

/// 获取内部的 bezierPath
- (UIBezierPath *)bezierPath;

/// 保存PathOperation，在TMMNativeRoundRectLayer中根据Operation配置fillRule
- (TMMNativeDrawPathOperation)pathOperation;

@end

NS_ASSUME_NONNULL_END
