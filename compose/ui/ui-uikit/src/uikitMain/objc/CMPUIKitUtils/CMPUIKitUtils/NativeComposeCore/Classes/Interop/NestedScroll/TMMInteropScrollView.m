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
#import "ITMMCanvasViewProxy.h"
#import <WebKit/WebKit.h>
#import "TMMComposeAdaptivedCanvasViewV2.h"
#import "TMMComposeInjectCommonServiceImpl.h"
#import "TMMInteropBaseView.h"

static NSString* const gLogTag = @"TMMInteropScrollView";
static NSString* const gTabSwtich = @"ios_compose_ineropscrollview_tab";
static NSString* const gFixTMMNestScroll = @"ios_compose_tmm_nestscroll";
static NSString* const gFixTMMNestScrollLog = @"ios_compose_tmm_nestscroll_log";

/// 是否开启手势转发的修复
static BOOL TMMInteropScrollViewEnableFixDispatchEvent(void) {
    static BOOL enable = NO;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        NSString *tabKey = @"ios_compose_ineropscrollview_enable_fix_dispatch_envent";
        enable = [TMMComposeInjectCommonServiceImpl.sharedInstance tmm_getTabToggleIsOnKey:tabKey defaultValue:NO];
    });
    return enable;
}

/// 将滑动速度转为方向描述字符串
static NSString* ScrollDirectionString(CGPoint velocity) {
    if (CGPointEqualToPoint(velocity, CGPointZero)) {
        return @"None";
    }
    BOOL isHorizontal = fabs(velocity.x) >= fabs(velocity.y);
    if (isHorizontal) {
        return velocity.x > 0 ? @"Right" : @"Left";
    } else {
        return velocity.y > 0 ? @"Down" : @"Up";
    }
}

/// 将手势状态转为可读字符串
static NSString* GestureStateString(UIGestureRecognizerState state) {
    switch (state) {
        case UIGestureRecognizerStatePossible:  return @"Possible";
        case UIGestureRecognizerStateBegan:     return @"Began";
        case UIGestureRecognizerStateChanged:   return @"Changed";
        case UIGestureRecognizerStateEnded:     return @"Ended";
        case UIGestureRecognizerStateCancelled: return @"Cancelled";
        case UIGestureRecognizerStateFailed:    return @"Failed";
        default: return @"Unknown";
    }
}

@interface TMMInteropScrollView ()

/// Compose 根容器
@property (nonatomic, weak) UIView *composeInteropContainer;

/// 标记位禁止转发手势给 Compose
@property (nonatomic, assign) BOOL disableForwardEvent;

/// 标记当前触摸序列中 WKScrollView 是否正在处理滑动，此时不应转发给 Compose
@property (nonatomic, assign) BOOL wkScrollViewHandling;

/// 缓存找到的 WKScrollView，避免每次都递归查找
@property (nonatomic, weak) UIScrollView *cachedWKScrollView;

@property (nonatomic, assign) BOOL fixWebViewNestScrollEnable;

@property (nonatomic, assign) BOOL enablePanGestureLog;

@property (nonatomic, assign) NSUInteger touchCount;

/// 标记当前是否有触摸序列正在进行（已发送 touchesBegan 但尚未发送 touchesEnded/Cancelled）
@property (nonatomic, assign) BOOL isTouchActive;
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
        self.fixWebViewNestScrollEnable = [TMMComposeInjectCommonServiceImpl.sharedInstance tmm_getTabToggleIsOnKey:gFixTMMNestScroll defaultValue:NO];
        self.enablePanGestureLog = [TMMComposeInjectCommonServiceImpl.sharedInstance tmm_getTabToggleIsOnKey:gFixTMMNestScrollLog defaultValue:NO];
        [self processPanGestureRecognizer];
        self.contentInsetAdjustmentBehavior = UIScrollViewContentInsetAdjustmentNever;
        if (@available(iOS 13.0, *)) {
            self.automaticallyAdjustsScrollIndicatorInsets = NO;
        }
    }
    return self;
}

- (void)willMoveToWindow:(UIWindow *)newWindow {
    self.disableForwardEvent = newWindow == nil;
    [super willMoveToWindow:newWindow];
}

#pragma mark - super overwrite
- (void)setContentOffset:(CGPoint)contentOffset {
    // 空实现：不允许 UIScrollView 真正滚动，仅用其 panGestureRecognizer 捕获手势
}

#pragma mark - WKScrollView 查找与状态判断
/// 递归查找子视图中的 WKWebView 的 scrollView
- (UIScrollView *)findWKScrollViewInView:(UIView *)view {
    if ([view isKindOfClass:[WKWebView class]]) {
        return [(WKWebView *)view scrollView];
    }
    for (UIView *subview in view.subviews) {
        UIScrollView *found = [self findWKScrollViewInView:subview];
        if (found) {
            return found;
        }
    }
    return nil;
}

/// 获取 WKScrollView（带缓存）
- (UIScrollView *)wkScrollView {
    if (!_cachedWKScrollView) {
        _cachedWKScrollView = [self findWKScrollViewInView:self];
    }
    return _cachedWKScrollView;
}

/// 判断 WKScrollView 的 panGesture 是否正在活跃（Began 或 Changed）
- (BOOL)isWKScrollViewPanActive {
    UIScrollView *wkScrollView = [self wkScrollView];
    if (!wkScrollView) {
        return NO;
    };
    UIGestureRecognizerState state = wkScrollView.panGestureRecognizer.state;
    return (state == UIGestureRecognizerStateBegan || state == UIGestureRecognizerStateChanged);
}

/// 打印 TMMInteropScrollView 和 WKScrollView 的 panGesture 状态
- (void)logGestureStatesWithPhase:(NSString *)phase {
    if (!self.enablePanGestureLog) {
        return;
    }
    UIPanGestureRecognizer *panGesture = self.panGestureRecognizer;
    CGPoint velocity = [panGesture velocityInView:self];
    CGPoint translation = [panGesture translationInView:self];
    NSString *direction = ScrollDirectionString(velocity);
    UIScrollView *wkScrollView = [self wkScrollView];
    if (wkScrollView) {
        UIPanGestureRecognizer *wkPan = wkScrollView.panGestureRecognizer;
        NSString *logMsg = [NSString stringWithFormat:@"[TMMInteropScrollView] touchCount:%lu %@ - self.panGesture: %@ | direction: %@ (vx:%.1f vy:%.1f) | translation: (tx:%.1f ty:%.1f) | WKScrollView.panGesture: %@ | wkHandling: %d", (unsigned long)self.touchCount,
                            phase,
                            GestureStateString(panGesture.state),
                            direction, velocity.x, velocity.y,
                            translation.x, translation.y,
                            GestureStateString(wkPan.state),
                            self.wkScrollViewHandling];
        [TMMComposeInjectCommonServiceImpl.sharedInstance tmm_logMessage:gLogTag message:logMsg];
    } else {
        NSString *logMsg = [NSString stringWithFormat:@"[TMMInteropScrollView] touchCount:%lu %@ - self.panGesture: %@ | direction: %@ (vx:%.1f vy:%.1f) | translation: (tx:%.1f ty:%.1f)", (unsigned long)self.touchCount,
                            phase,
                            GestureStateString(panGesture.state),
                            direction, velocity.x, velocity.y,
                            translation.x, translation.y];
        [TMMComposeInjectCommonServiceImpl.sharedInstance tmm_logMessage:gLogTag message:logMsg];
    }
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
                                strongSelf.touchCount++;
                                [strongSelf logGestureStatesWithPhase:@"touchesBegan"];
                                UIView *composeInteropContainer = strongSelf.composeInteropContainer;
                                if ([strongSelf ensureResponderChain]) {
                                    if (self.fixWebViewNestScrollEnable) {
                                        // panGestureRecognizer 由于 setContentOffset: 空实现，
                                        // deceleration 流程无法正常结束，导致状态机卡在 Began 状态无法重置回 Possible。
                                        // 当 panGesture 处于非 Possible 状态时（Began/Changed/Ended），
                                        // 通过 enabled=NO/YES 强制将其重置回 Possible，
                                        // 否则 iOS 手势竞争机制会在第一个 touchesMoved 时立即将 WKScrollView.panGesture
                                        // 置为 Failed，导致 H5 无法响应上滑。
                                        UIPanGestureRecognizer *pan = strongSelf.panGestureRecognizer;
                                        if (pan.state != UIGestureRecognizerStatePossible) {
                                            pan.enabled = NO;
                                            pan.enabled = YES;
                                        }
                                        [pan setTranslation:CGPointZero inView:strongSelf];
                                        // 若 Compose 侧有未结束的触摸序列（isTouchActive），先发送 cancel 重置
                                        if (strongSelf.isTouchActive) {
                                            [composeInteropContainer touchesCancelled:touches withEvent:event];
                                            strongSelf.isTouchActive = NO;
                                        }
                                        [composeInteropContainer touchesBegan:touches withEvent:event];
                                        strongSelf.isTouchActive = YES;
                                    } else {
                                        [composeInteropContainer touchesBegan:touches withEvent:event];
                                    }
                                }
                            }
                                  error:&error];

    [panGesture
        tmm_compose_aspect_hookSelector:@selector(touchesMoved:withEvent:)
                            withOptions:(TMMComposeAspectPositionAfter)usingBlock:^(id<TMMComposeAspectInfo> info, NSSet *touches, UIEvent *event) {
                                __strong TMMInteropScrollView *strongSelf = weakSelf;
        [strongSelf logGestureStatesWithPhase:@"touchesMoved"];
                                UIView *composeInteropContainer = strongSelf.composeInteropContainer;
                                if ([strongSelf ensureResponderChain]) {
                                    [composeInteropContainer touchesMoved:touches withEvent:event];
                                }
                            }
                                  error:&error];

    [panGesture
        tmm_compose_aspect_hookSelector:@selector(touchesCancelled:withEvent:)
                            withOptions:(TMMComposeAspectPositionAfter)usingBlock:^(id<TMMComposeAspectInfo> info, NSSet *touches, UIEvent *event) {
                                __strong TMMInteropScrollView *strongSelf = weakSelf;
                                [strongSelf logGestureStatesWithPhase:@"touchesCancelled"];
                                UIView *composeInteropContainer = strongSelf.composeInteropContainer;
                                if ([strongSelf ensureResponderChain]) {
                                    [composeInteropContainer touchesCancelled:touches withEvent:event];
                                }
                                strongSelf.isTouchActive = NO;
                            }
                                  error:&error];

    [panGesture
        tmm_compose_aspect_hookSelector:@selector(touchesEnded:withEvent:)
                            withOptions:(TMMComposeAspectPositionAfter)usingBlock:^(id<TMMComposeAspectInfo> info, NSSet *touches, UIEvent *event) {
                                __strong TMMInteropScrollView *strongSelf = weakSelf;
                                [strongSelf logGestureStatesWithPhase:@"touchesEnded"];
                                UIView *composeInteropContainer = strongSelf.composeInteropContainer;
                                if ([strongSelf ensureResponderChain]) {
                                    [composeInteropContainer touchesEnded:touches withEvent:event];
                                }
                                strongSelf.isTouchActive = NO;
                            }
                                  error:&error];
}

- (BOOL)ensureResponderChain2 {
    // self.superview -> TMMInteropWrapView
    // self.superview.superview -> ViewV2
    // self.superview.superview.superview -> ViewV2.superview
    BOOL isAttached = YES;
    TMMComposeAdaptivedCanvasViewV2 *attachedParent = (TMMComposeAdaptivedCanvasViewV2 *)[self findCanvasView:self];
    if ([attachedParent isKindOfClass:[TMMComposeAdaptivedCanvasViewV2 class]]) {
        isAttached = attachedParent.isAttached;
    }
    BOOL disableForwardEvent = self.disableForwardEvent;
    NSString *logMsg = [NSString stringWithFormat:@"attachedParent:%@ isAttached:%d window:%d composeInteropContainer:%@ superview:%@ disableForwardEvent:%d",attachedParent,
                        isAttached, !!self.window, self.composeInteropContainer, self.superview, disableForwardEvent];
    
    isAttached = isAttached && !disableForwardEvent;
    [TMMComposeInjectCommonServiceImpl.sharedInstance tmm_logMessage:gLogTag message:logMsg];
    
    return isAttached && self.window != nil && self.composeInteropContainer && attachedParent.superview != nil;
}

- (BOOL)ensureResponderChain {
    if (TMMInteropScrollViewEnableFixDispatchEvent()) {
        return [self ensureResponderChain2];
    }
    
    // self.superview -> TMMInteropWrapView
    // self.superview.superview -> ViewV2
    // self.superview.superview.superview -> ViewV2.superview
    BOOL fix = [TMMComposeInjectCommonServiceImpl.sharedInstance tmm_getTabToggleIsOnKey:gTabSwtich defaultValue:NO];
    BOOL isAttached = YES;
    if (fix) {
        TMMComposeAdaptivedCanvasViewV2 *attachedParent = (TMMComposeAdaptivedCanvasViewV2 *)[self findCanvasView:self];
        if ([attachedParent isKindOfClass:[TMMComposeAdaptivedCanvasViewV2 class]]) {
            isAttached = attachedParent.isAttached;
        }
        NSString *logMsg = [NSString stringWithFormat:@"attachedParent:%@ isAttached:%d window:%d composeInteropContainer:%@  superview:%@",attachedParent, isAttached, !!self.window, self.composeInteropContainer, self.superview];
        [TMMComposeInjectCommonServiceImpl.sharedInstance tmm_logMessage:gLogTag message:logMsg];
        
        return isAttached && self.window != nil && self.composeInteropContainer && attachedParent.superview != nil;
    } else {
        TMMComposeAdaptivedCanvasViewV2 *attachedParent = (TMMComposeAdaptivedCanvasViewV2 *)self.superview.superview;
        if ([attachedParent isKindOfClass:[TMMComposeAdaptivedCanvasViewV2 class]]) {
            isAttached = attachedParent.isAttached;
        }
        return isAttached && self.window != nil && self.composeInteropContainer && self.superview.superview.superview != nil;
    }
}

- (UIView *)findCanvasView:(UIView *)view {
    if ([view isKindOfClass:[TMMComposeAdaptivedCanvasViewV2 class]]) {
        return view;
    }
    if ([view isKindOfClass:[TMMInteropBaseView class]] || !view) {
        return nil;
    }
    return [self findCanvasView:view.superview];
}

#pragma mark - public
- (void)bindComposeInteropContainer:(UIView *)view {
    if (self.composeInteropContainer != view) {
        self.composeInteropContainer = view;
    }
}

@end
