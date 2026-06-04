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

#import "TMMInteropBaseView.h"
#import "TMMDrawUtils.h"

@interface TMMInteropBaseView() <UIGestureRecognizerDelegate>

/// 给 Compose 根容器的基类新增一个手势，使用该手势获取初速度，传给 kt 侧进行 Fling 动画
@property (nonatomic, strong) UIPanGestureRecognizer *velocityGestureRecognizer;

/// 手势的初速度
@property (nonatomic, assign) CGPoint velocityInView;

/// 屏幕缩放系数
@property (nonatomic, assign) float density;

@end

@implementation TMMInteropBaseView

- (instancetype)initWithFrame:(CGRect)frame {
    self = [super initWithFrame:frame];
    if (self) {
        _density = TMMComposeCoreDeviceDensity();
    }
    return self;
}

#pragma mark - UIGestureRecognizerDelegate
- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer shouldRecognizeSimultaneouslyWithGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer {
    if (gestureRecognizer == self.velocityGestureRecognizer) {
        return YES;
    }
    return YES;
}

#pragma mark - private
- (void)didReceiveGestureRecognizer:(UIPanGestureRecognizer *)gesturer {
    if (gesturer != self.velocityGestureRecognizer) {
        return;
    }
    switch (gesturer.state) {
        case UIGestureRecognizerStateEnded:
            // 手势识别成功并结束
            self.velocityInView = [gesturer velocityInView:self];
            break;
        case UIGestureRecognizerStateCancelled:
        case UIGestureRecognizerStateFailed:
            // 手势识别失败，重置
            self.velocityInView = CGPointZero;
            break;
        default:
            // 其他手势阶段不需要保存
            break;
    }
}

#pragma mark - public
- (void)enableNativeGestureVelocity {
    if (!self.velocityGestureRecognizer) {
        UIPanGestureRecognizer *gesturer = [[UIPanGestureRecognizer alloc] initWithTarget:self action:@selector(didReceiveGestureRecognizer:)];
        gesturer.delegate = self;
        gesturer.cancelsTouchesInView = NO;
        gesturer.delaysTouchesEnded = NO;
        [self addGestureRecognizer:gesturer];
        self.velocityGestureRecognizer = gesturer;
    }
}

- (float)panGestureVelocityY {
    return self.velocityInView.y * self.density;
}

- (float)panGestureVelocityX {
    return self.velocityInView.x * self.density;
}

- (void)resetGestureVelocity {
    self.velocityInView = CGPointZero;
}

- (NSArray<NSDictionary<NSString *, id> *> *)tmm_getInspectorNodeParame {
    if (_nodeBlock == nil) {
        return @[];
    }
    
    NSArray *result = _nodeBlock();
 
    return result ?: @[];
}

@end
