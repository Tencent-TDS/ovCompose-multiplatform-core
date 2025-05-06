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

#import "TMMInteropScrollView.h"
#import "TMMComposeAspects.h"

@interface TMMInteropScrollView ()

/// Compose 根容器
@property (nonatomic, weak) UIView *composeInteropContainer;

@end

@implementation TMMInteropScrollView

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        self.bounces = NO;
        self.alwaysBounceVertical = NO;
        self.alwaysBounceHorizontal = NO;
        self.showsVerticalScrollIndicator = NO;
        self.showsHorizontalScrollIndicator = NO;
        self.backgroundColor = [UIColor clearColor];
        // 这一行非常重要，否则会导致给 composeInteropContainer 发送的事件不完整，可能断掉 end 事件
        self.contentSize = CGSizeMake(INT_MAX, INT_MAX);
        self.autoresizesSubviews = NO;
        [self processPanGestureRecognizer];
        self.contentInsetAdjustmentBehavior = UIScrollViewContentInsetAdjustmentNever;
        if (@available(iOS 13.0, *)) {
            self.automaticallyAdjustsScrollIndicatorInsets = NO;
        }
    }
    return self;
}

#pragma mark - super overwrite
- (void)setContentOffset:(CGPoint)contentOffset {
}

#pragma mark - private
- (void)processPanGestureRecognizer {
    NSError *error = nil;
    __weak TMMInteropScrollView *weakSelf = self;
    UIPanGestureRecognizer *panGesture = self.panGestureRecognizer;
    [panGesture
        tmm_compose_aspect_hookSelector:@selector(touchesBegan:withEvent:)
                            withOptions:(TMMComposeAspectPositionAfter)usingBlock:^(id<TMMComposeAspectInfo> info, NSSet *touches, UIEvent *event) {
                                __strong TMMInteropScrollView *strongSelf = weakSelf;
                                UIView *composeInteropContainer = strongSelf.composeInteropContainer;
                                if (strongSelf.window && composeInteropContainer) {
                                    [composeInteropContainer touchesBegan:touches withEvent:event];
                                }
                            }
                                  error:&error];

    [panGesture
        tmm_compose_aspect_hookSelector:@selector(touchesMoved:withEvent:)
                            withOptions:(TMMComposeAspectPositionAfter)usingBlock:^(id<TMMComposeAspectInfo> info, NSSet *touches, UIEvent *event) {
                                __strong TMMInteropScrollView *strongSelf = weakSelf;
                                UIView *composeInteropContainer = strongSelf.composeInteropContainer;
                                if (strongSelf.window && composeInteropContainer) {
                                    [composeInteropContainer touchesMoved:touches withEvent:event];
                                }
                            }
                                  error:&error];

    [panGesture
        tmm_compose_aspect_hookSelector:@selector(touchesCancelled:withEvent:)
                            withOptions:(TMMComposeAspectPositionAfter)usingBlock:^(id<TMMComposeAspectInfo> info, NSSet *touches, UIEvent *event) {
                                __strong TMMInteropScrollView *strongSelf = weakSelf;
                                UIView *composeInteropContainer = strongSelf.composeInteropContainer;
                                if (strongSelf.window && composeInteropContainer) {
                                    UIView *composeInteropContainer = strongSelf.composeInteropContainer;
                                    [composeInteropContainer touchesCancelled:touches withEvent:event];
                                }
                            }
                                  error:&error];

    [panGesture
        tmm_compose_aspect_hookSelector:@selector(touchesEnded:withEvent:)
                            withOptions:(TMMComposeAspectPositionAfter)usingBlock:^(id<TMMComposeAspectInfo> info, NSSet *touches, UIEvent *event) {
                                __strong TMMInteropScrollView *strongSelf = weakSelf;
                                UIView *composeInteropContainer = strongSelf.composeInteropContainer;
                                if (strongSelf.window && composeInteropContainer) {
                                    [composeInteropContainer touchesEnded:touches withEvent:event];
                                }
                            }
                                  error:&error];
}

#pragma mark - public
- (void)bindComposeInteropContainer:(UIView *)view {
    if (self.composeInteropContainer != view) {
        self.composeInteropContainer = view;
    }
}

@end
