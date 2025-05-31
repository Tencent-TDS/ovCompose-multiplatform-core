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

#import "TMMInteropWrapView.h"
#import "TMMInteropScrollView.h"
#import "TMMReportUtil.h"

@interface TMMInteropWrapView ()

/// 内部用来接受滚动 touch 事件的 scrollView
@property (nonatomic, strong) TMMInteropScrollView *interopScrollView;
@property (nonatomic, strong) UIView *lastChild;
@property (nonatomic, assign) CGRect lastFrame;
@property (nonatomic, copy) void (^onSizeChange)(CGFloat width, CGFloat height);

@end

@implementation TMMInteropWrapView

- (instancetype)init {
    self = [super init];
    if (self) {
        TMMComposeMarkViewForVideoReport(self);
    }
    return self;
}

- (void)layoutSubviews {
    [super layoutSubviews];
    [self prepareForScrollViewIfNeeded];
}

- (void)prepareForScrollViewIfNeeded {
    if (!_interopScrollView) {
        return;
    }
    _interopScrollView.frame = self.bounds;
    if (self.subviews.count > 0 && self.subviews[0] != _interopScrollView) {
        [super sendSubviewToBack:_interopScrollView];
    }
}

- (void)addSubview:(UIView *)view {
    if (_interopScrollView) {
        [_interopScrollView addSubview:view];
    } else {
        [super addSubview:view];
    }

    [self addFrameListener:view];
}

- (void)addFrameListener:(UIView *)child {
    [self removeFrameListener];

    [child addObserver:self forKeyPath:@"frame" options:NSKeyValueObservingOptionNew context:nil];
    _lastChild = child;
}

- (void)removeFrameListener {
    if (_lastChild) {
        [_lastChild removeObserver:self forKeyPath:@"frame"];
        _lastChild = nil;
    }
}

- (void)dealloc {
    [self removeFrameListener];
}

- (void)observeValueForKeyPath:(NSString *)keyPath ofObject:(id)object change:(NSDictionary<NSKeyValueChangeKey,id> *)change context:(void *)context {
    if ([keyPath isEqualToString:@"frame"]) {
        CGRect frame = [[change objectForKey:NSKeyValueChangeNewKey] CGRectValue];
        if (!CGRectEqualToRect(_lastFrame, frame)) {
            _lastFrame = frame;
            if (_onSizeChange) {
                _onSizeChange(frame.size.width, frame.size.height);
            }
        }
    }
}

- (void)setOnSizeChange:(void (^)(CGFloat width, CGFloat height))onSizeChange {
    _onSizeChange = [onSizeChange copy];
}


- (UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event {
    UIView *view = [super hitTest:point withEvent:event];
    if (view == self) {
        view = nil;
    }
    return view;
}

- (id)accessibilityContainer {
    return self.actualAccessibilityContainer;
}

#pragma mark - public
- (void)bindComposeInteropContainer:(UIView *)view {
    [self.interopScrollView bindComposeInteropContainer:view];
}

- (TMMInteropScrollView *)interopScrollView {
    if (_interopScrollView) {
        return _interopScrollView;
    }
    _interopScrollView = [[TMMInteropScrollView alloc] init];
    // 这里需要直接调用父类的方法
    [super addSubview:_interopScrollView];
    return _interopScrollView;
}

@end
