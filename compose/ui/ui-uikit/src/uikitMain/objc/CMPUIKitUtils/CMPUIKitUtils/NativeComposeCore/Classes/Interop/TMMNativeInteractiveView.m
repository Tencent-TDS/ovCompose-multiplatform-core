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

#import "TMMNativeInteractiveView.h"
#import "TMMInteropBaseView.h"

@interface TMMNativeInteractiveView ()

/// 内部用来拦截的手势
@property (nonatomic, strong) UIGestureRecognizer *panGestureRecognizer;
@end

@implementation TMMNativeInteractiveView

- (instancetype)init {
    self = [super init];
    if (self) {
        [self setup];
    }
    return self;
}

- (instancetype)initWithFrame:(CGRect)frame {
    self = [super initWithFrame:frame];
    if (self) {
        [self setup];
    }
    return self;
}

- (void)setup {
    //    _panGestureRecognizer = [[UIPanGestureRecognizer alloc] initWithTarget:self action:@selector(handlePanGesture:)];
    //    [self addGestureRecognizer:_panGestureRecognizer];
}

- (void)handlePanGesture:(UIPanGestureRecognizer *)panGestureRecognizer {
    // Compose的drag识别成功，需要将次view的pan手势屏蔽掉
}

- (UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event {
    return [super hitTest:point withEvent:event];
}

- (BOOL)pointInside:(CGPoint)point withEvent:(UIEvent *)event {
    return [super pointInside:point withEvent:event];
}

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    // 转发给InteractiveUIView Box.click
    [self.interactiveUIView touchesBegan:touches withEvent:event];
}

- (void)touchesMoved:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    // 转发给InteractiveUIView
    [self.interactiveUIView touchesMoved:touches withEvent:event];
}

- (void)touchesEnded:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    // 转发给InteractiveUIView
    [self.interactiveUIView touchesEnded:touches withEvent:event];
}

- (UIView *)interactiveUIView {
    return [self findComposeInteractiveUIView:self];
}

- (UIView *)findComposeInteractiveUIView:(UIView *)view {
    if (!view) {
        return nil;
    }
    UIView *superView = view.superview;
    if ([superView isKindOfClass:TMMInteropBaseView.class]) {
        return superView;
    } else {
        return [self findComposeInteractiveUIView:superView];
    }
}

- (NSDictionary *)lookin_customDebugInfos {
    return @{
        @"title" : @"ClipView",
        @"properties" : @[
            @{
                @"title" : @"View 信息",
                @"valueType" : @"string",
                @"section" : @"InteractiveView 详细信息",
                @"value" : [NSString stringWithFormat:@"%@", self]
            },
            @{
                @"title" : @"window 信息",
                @"valueType" : @"string",
                @"section" : @"InteractiveView window信息",
                @"value" : [NSString stringWithFormat:@"%@", self.window]
            }
        ]
    };
}

@end
