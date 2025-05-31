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

#import "TMMNativeRoundRectLayer.h"
#import "TMMComposeNativePaint.h"
#import "TMMComposeNativePath.h"
#import "TMMDrawUtils.h"

@interface TMMNativeRoundRectLayer ()

/// 当前的 mask layer
@property (nonatomic, strong) CAShapeLayer *maskLayer;

@end

@implementation TMMNativeRoundRectLayer

- (id<CAAction>)actionForKey:(NSString *)event {
    return nil;
}

- (void)updateUIBezierPath:(UIBezierPath *)path
               strokeWidth:(CGFloat)strokeWidth
                     color:(UIColor *)color
                 strokeCap:(TMMNativeDrawStrokeCap)strokeCap
             pathOperation:(TMMNativeDrawPathOperation)pathOperation {
    self.backgroundColor = color.CGColor;
    self.maskLayer.path = path.CGPath;
    if (pathOperation == TMMNativeDrawPathOperationDifference) {
        // 如果pathOperation为Diffrence，则设置fillRule，对重叠区域进行裁剪
        self.maskLayer.fillRule = kCAFillRuleEvenOdd;
    } else {
        self.maskLayer.fillRule = kCAFillRuleNonZero;
    }
    if (strokeWidth > 0) {
        self.maskLayer.fillColor = [UIColor clearColor].CGColor;
        self.maskLayer.lineWidth = strokeWidth;
        self.maskLayer.strokeColor = color.CGColor;
        // 处理两端的样式
        switch (strokeCap) {
            case TMMNativeDrawStrokeCapRound:
                self.maskLayer.lineCap = kCALineCapRound;
                break;
            case TMMNativeDrawStrokeCapButt:
                self.maskLayer.lineCap = kCALineCapButt;
                break;
            case TMMNativeDrawStrokeCapSquare:
                self.maskLayer.lineCap = kCALineCapSquare;
            default:
                self.maskLayer.lineCap = kCALineCapButt;
                break;
        }
    }
}

#pragma mark -
- (CAShapeLayer *)maskLayer {
    if (_maskLayer) {
        return _maskLayer;
    }
    _maskLayer = [CAShapeLayer layer];
    self.mask = _maskLayer;
    return _maskLayer;
}

#pragma mark - debug
- (NSDictionary *)lookin_customDebugInfos {
    return @{
        @"title" : @"RoundRect",
    };
}

@end
