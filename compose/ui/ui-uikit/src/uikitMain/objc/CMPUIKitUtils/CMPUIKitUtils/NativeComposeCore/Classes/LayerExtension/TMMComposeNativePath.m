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

#import "TMMComposeNativePath.h"
#import "TMMDrawUtils.h"
#import "TMMNativeRoundRect.h"
#import "TMMNativeBezierPathUtil.h"

// 需要可以注释该宏
#define NSLog(...)

static const float TMMComposeNativePathOpTypeNone = 0.0f;
static const float TMMComposeNativePathOpTypeMoveTo = 1.0f;
static const float TMMComposeNativePathOpTypeRelativeMoveTo = 2.0f;
static const float TMMComposeNativePathOpTypeLineTo = 3.0f;
static const float TMMComposeNativePathOpTypeRelativeLineTo = 3.1f;
static const float TMMComposeNativePathOpTypeQuadraticBezierTo = 4.0f;
static const float TMMComposeNativePathOpTypeRelativeQuadraticBezierTo = 5.0f;
static const float TMMComposeNativePathOpTypeCubicTo = 6.0f;
static const float TMMComposeNativePathOpTypeRelativeCubicTo = 7.0f;
static const float TMMComposeNativePathOpTypeArcTo = 8.0f;
static const float TMMComposeNativePathOpTypeAddRect = 9.0f;
static const float TMMComposeNativePathOpTypeAddOval = 10.0f;
static const float TMMComposeNativePathOpTypeAddArcRad = 11.0f;
static const float TMMComposeNativePathOpTypeAddArc = 12.0f;
static const float TMMComposeNativePathOpTypeAddRoundRect = 13.0f;
static const float TMMComposeNativePathOpTypeAddPath = 14.0f;
static const float TMMComposeNativePathOpTypeClose = 15.0f;
static const float TMMComposeNativePathOpTypeReset = 16.0f;
static const float TMMComposeNativePathOpTypeRewind = 17.0f;
static const float TMMComposeNativePathOpTypeTranslate = 18.0f;
static const float TMMComposeNativePathOpTypeOp = 19.0f;

@interface TMMComposeNativePath ()

/// 内部的 UIBezierPath
@property (nonatomic, strong) UIBezierPath *bezierPath;

/// 当前的数据增量哈希
@property (nonatomic, assign) NSInteger currentDataHash;

/// PathOperation
@property (nonatomic, assign) TMMNativeDrawPathOperation pathOperation;

@end

@implementation TMMComposeNativePath

#pragma mark -
- (UIBezierPath *)bezierPath {
    if (_bezierPath) {
        return _bezierPath;
    }
    _bezierPath = [UIBezierPath bezierPath];
    return _bezierPath;
}

- (void)moveTo:(float)x y:(float)y {
    _currentDataHash = TMMComposeCoreHash4Floats(TMMComposeNativePathOpTypeMoveTo, _currentDataHash, x, y);
    const CGFloat denstity = TMMComposeCoreDeviceDensity();
    [self.bezierPath moveToPoint:CGPointMake(x / denstity, y / denstity)];
    NSLog(@"[NativePath] self:%@ moveTo", self);
}

- (void)relativeMoveTo:(float)dx dy:(float)dy {
    _currentDataHash = TMMComposeCoreHash4Floats(TMMComposeNativePathOpTypeRelativeMoveTo, _currentDataHash, dx, dy);
    NSLog(@"[NativePath] self:%@ relativeMoveTo", self);
}

- (void)lineTo:(float)x y:(float)y {
    _currentDataHash = TMMComposeCoreHash4Floats(TMMComposeNativePathOpTypeLineTo, _currentDataHash, x, y);
    const CGFloat denstity = TMMComposeCoreDeviceDensity();
    [self.bezierPath addLineToPoint:CGPointMake(x / denstity, y / denstity)];
    NSLog(@"[NativePath] self:%@ lineTo", self);
}

- (void)relativeLineTo:(float)dx dy:(float)dy {
    _currentDataHash = TMMComposeCoreHash4Floats(TMMComposeNativePathOpTypeRelativeLineTo, _currentDataHash, dx, dy);
    NSLog(@"[NativePath] self:%@ relativeLineTo", self);
}

- (void)quadraticBezierTo:(float)x1 y1:(float)y1 x2:(float)x2 y2:(float)y2 {
    _currentDataHash = TMMComposeCoreHash6Floats(TMMComposeNativePathOpTypeQuadraticBezierTo, _currentDataHash, x1, y1, x2, y2);
    NSLog(@"[NativePath] self:%@ quadraticBezierTo", self);
}

- (void)relativeQuadraticBezierTo:(float)dx1 dy1:(float)dy1 dx2:(float)dx2 dy2:(float)dy2 {
    _currentDataHash = TMMComposeCoreHash6Floats(TMMComposeNativePathOpTypeRelativeQuadraticBezierTo, _currentDataHash, dx1, dy1, dx2, dy2);
    NSLog(@"[NativePath] self:%@ relativeQuadraticBezierTo", self);
}

- (void)cubicTo:(float)x1 y1:(float)y1 x2:(float)x2 y2:(float)y2 x3:(float)x3 y3:(float)y3 {
    _currentDataHash = TMMComposeCoreHash6Floats(TMMComposeNativePathOpTypeCubicTo, _currentDataHash, x1, y1, x2, y2);
    const CGFloat denstity = TMMComposeCoreDeviceDensity();
    [self.bezierPath addCurveToPoint:CGPointMake(x3 / denstity, y3 / denstity)
                       controlPoint1:CGPointMake(x1 / denstity, y1 / denstity)
                       controlPoint2:CGPointMake(x2 / denstity, y2 / denstity)];
    NSLog(@"[NativePath] self:%@ cubicTo", self);
}

- (void)relativeCubicTo:(float)dx1 dy1:(float)dy1 dx2:(float)dx2 dy2:(float)dy2 dx3:(float)dx3 dy3:(float)dy3 {
    _currentDataHash = TMMComposeCoreHash8Floats(TMMComposeNativePathOpTypeRelativeCubicTo, _currentDataHash, dx1, dy1, dx2, dy2, dx3, dy3);
}
// 目前要求传入的left,top,right, bottom是正方形区域
- (void)arcTo:(float)left
                  top:(float)top
                right:(float)right
               bottom:(float)bottom
    startAngleDegrees:(float)startAngleDegrees
    sweepAngleDegrees:(float)sweepAngleDegrees
          forceMoveTo:(BOOL)forceMoveTo {
    const float floats[9] = {
        TMMComposeNativePathOpTypeArcTo, _currentDataHash, left, top, right, bottom, startAngleDegrees, sweepAngleDegrees, forceMoveTo ? 1.0 : 0.0,
    };
    _currentDataHash = TMMFNVHash(floats, sizeof(floats));
    /*
     TODO
     标记一下，待验证。addArcWithCenter方法的clockwise参数固定为YES，但sweepAngleDegrees可能为负值会导致方向错误。应根据sweepAngleDegrees的正负动态设置clockwise参数。
     */
    const float density = TMMComposeCoreDeviceDensity();
    float centerX = (left + right) / 2 / density;
    float centerY = (top + bottom) / 2 / density;
    float width = (right - left) / density;
    CGPoint center = CGPointMake(centerX, centerY);
    float startAngle = (startAngleDegrees / 180) * M_PI;
    float endAngle = ((startAngleDegrees + sweepAngleDegrees) / 180) * M_PI;
    [self.bezierPath addArcWithCenter:center radius:width / 2 startAngle:startAngle endAngle:endAngle clockwise:YES];
}

- (void)addRect:(float)left top:(float)top right:(float)right bottom:(float)bottom {
    _currentDataHash = TMMComposeCoreHash6Floats(TMMComposeNativePathOpTypeAddRect, _currentDataHash, left, top, right, bottom);
    NSLog(@"[NativePath] self:%@ addRect", self);
}

- (void)addOval:(float)left top:(float)top right:(float)right bottom:(float)bottom {
    _currentDataHash = TMMComposeCoreHash6Floats(TMMComposeNativePathOpTypeAddOval, _currentDataHash, left, top, right, bottom);
    const CGFloat denstity = TMMComposeCoreDeviceDensity();
    CGRect ovalRect = CGRectMake(left / denstity, top / denstity, (right - left) / denstity, (bottom - top) / denstity);
    UIBezierPath *path = [UIBezierPath bezierPathWithOvalInRect:ovalRect];
    [self.bezierPath appendPath:path];
}

- (void)addArcRad:(float)left
                  top:(float)top
                right:(float)right
               bottom:(float)bottom
    startAngleRadians:(float)startAngleRadians
    sweepAngleRadians:(float)sweepAngleRadians {
    _currentDataHash = TMMComposeCoreHash8Floats(TMMComposeNativePathOpTypeArcTo, _currentDataHash, left, top, right, bottom, startAngleRadians,
                                                 sweepAngleRadians);
    NSLog(@"[NativePath] self:%@ addArcRad", self);
}

- (void)addArc:(float)left
                  top:(float)top
                right:(float)right
               bottom:(float)bottom
    startAngleDegrees:(float)startAngleDegrees
    sweepAngleDegrees:(float)sweepAngleDegrees {
    _currentDataHash = TMMComposeCoreHash8Floats(TMMComposeNativePathOpTypeAddArc, _currentDataHash, left, top, right, bottom, startAngleDegrees,
                                                 sweepAngleDegrees);
    NSLog(@"[NativePath] self:%@ addArc", self);
}

- (void)addRoundRect:(TMMNativeRoundRect *)roundRect {
    _currentDataHash = TMMComposeCoreHash4Floats(TMMComposeNativePathOpTypeAddRoundRect, _currentDataHash, (float)[roundRect dataHash], 0);
    UIBezierPath *path = TMMCreatRoundedRectPathWithComposeRoundRect(roundRect);
    [self.bezierPath appendPath:path];
}

- (void)addPath:(TMMComposeNativePath *)path offsetX:(float)offsetX offsetY:(float)offsetY {
    _currentDataHash = TMMComposeCoreHash6Floats(TMMComposeNativePathOpTypeAddPath, _currentDataHash, (float)[path dataHash], offsetX, offsetY, 0);
    UIBezierPath *newPath = path.bezierPath;
    CGAffineTransform translation = CGAffineTransformMakeTranslation(offsetX, offsetY);
    [newPath applyTransform:translation];
    [self.bezierPath appendPath:newPath];
    NSLog(@"[NativePath] self:%@ addPath:%@", self, path);
}

- (void)close {
    _currentDataHash = TMMComposeCoreHash4Floats(TMMComposeNativePathOpTypeClose, _currentDataHash, 0, 0);
    NSLog(@"[NativePath] self:%@ close", self);
}

- (void)reset {
    _currentDataHash = TMMComposeCoreHash4Floats(TMMComposeNativePathOpTypeReset, _currentDataHash, 0, 0);
    [self.bezierPath removeAllPoints];
    NSLog(@"[NativePath] self:%@ reset", self);
}

- (void)rewind {
    _currentDataHash = TMMComposeCoreHash4Floats(TMMComposeNativePathOpTypeRewind, _currentDataHash, 0, 0);
    NSLog(@"[NativePath] self:%@ rewind", self);
}

- (void)translate:(float)offsetX offsetY:(float)offsetY {
    _currentDataHash = TMMComposeCoreHash4Floats(TMMComposeNativePathOpTypeTranslate, _currentDataHash, offsetX, offsetY);
    NSLog(@"[NativePath] self:%@ translate offsetX：%f, offsetY:%f", self, offsetX, offsetY);
}

- (CGRect)getBounds {
    return CGRectZero;
}

- (BOOL)op:(TMMComposeNativePath *)path1 path2:(TMMComposeNativePath *)path2 pathOperation:(TMMNativeDrawPathOperation)pathOperation {
    _currentDataHash = TMMComposeCoreHash4Floats(TMMComposeNativePathOpTypeOp, _currentDataHash, (float)[path1 dataHash], (float)[path2 dataHash]);
    NSLog(@"[NativePath] self:%@ op path1：%@, path2:%@", self, path1, path2);
    if (pathOperation == TMMNativeDrawPathOperationDifference) {
        // Compose处理RoundedCornerShape的时候会通过Diffrence的Op对两个Path的重复区域进行裁剪，达到Border的效果
        [path1.bezierPath appendPath:path2.bezierPath];
        _pathOperation = TMMNativeDrawPathOperationDifference;
    }
    return NO;
}

- (uint64_t)dataHash {
    return _currentDataHash;
}

@end
