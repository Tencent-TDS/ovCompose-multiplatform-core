/*
 * Tencent is pleased to support the open source community by making tvCompose available.
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

#import "OVComposeKeyboardVisibilityListener.h"

/// 获取 keyWindow
NS_INLINE UIWindow *UIKeyboardtGetCurrentWindow(void) {
    UIApplication *sharedApplication = [UIApplication sharedApplication];
    if (@available(iOS 15, *)) {
        UIScene *tmpUIWindowScene = nil;
        NSSet<UIScene *> *connectedScenes = [sharedApplication connectedScenes];
        for (UIScene *scene in connectedScenes) {
            UISceneActivationState activationState = scene.activationState;
            if (activationState == UISceneActivationStateForegroundActive ||
                activationState == UISceneActivationStateForegroundInactive) {
                tmpUIWindowScene = scene;
                break;
            }
        }
        return [(UIWindowScene *)tmpUIWindowScene keyWindow];
    }
    return sharedApplication.keyWindow;
}

/// 从键盘通知中获取动画时间
/// - Parameter notification: NSNotification
NS_INLINE float UIKeyboardDurationFromNotification(NSNotification *notification) {
    NSNumber *duration = notification.userInfo[UIKeyboardAnimationDurationUserInfoKey];
    return [duration floatValue];
}

/// 从键盘通知中获取键盘高度
/// - Parameter notification: NSNotification
NS_INLINE float UIKeyboardHeightFromNotification(NSNotification *notification) {
    NSValue *endFrame = notification.userInfo[UIKeyboardFrameEndUserInfoKey];
    return CGRectGetHeight([endFrame CGRectValue]);
}

/// 从键盘通知中获取 UIViewAnimationOptions
/// - Parameter notification: NSNotification
NS_INLINE UIViewAnimationOptions UIViewAnimationOptionFromNotification(NSNotification *notification) {
    UIViewAnimationOptions options = [notification.userInfo[UIKeyboardAnimationCurveUserInfoKey] integerValue] << 16;
    return options;
}

#pragma mark - OVComposeKeyboardVisibilityListener

@interface OVComposeKeyboardVisibilityListener()

/// Vsync 信号回调处理
@property (nonatomic, strong) CADisplayLink *displayLink;

/// 用来追踪系统动画的假 UIView
@property (nonatomic, strong) UIView *animationView;

/// 判断是否在键盘弹出的动画期间，防止短时间之内收到多次通知，进行重复动画
@property (nonatomic, assign) BOOL isShowAnimating;

/// 判断是否在键盘关闭的动画期间，防止短时间之内收到多次通知，进行重复动画
@property (nonatomic, assign) BOOL isHideAnimating;

/// Compose 容器视图
@property (nonatomic, weak) UIView *composeView;

@end

@implementation OVComposeKeyboardVisibilityListener

#pragma mark - private
- (void)prepareIfNeeded {
    if (!_animationView) {
        _animationView = [[UIView alloc] initWithFrame:CGRectZero];
        NSNotificationCenter *center = [NSNotificationCenter defaultCenter];
        [center addObserver:self selector:@selector(didReceiveKeyboardWillShowNotification:) name:UIKeyboardWillShowNotification object:nil];
        [center addObserver:self selector:@selector(didReceiveKeyboardDidShowNotification:) name:UIKeyboardDidShowNotification object:nil];
        [center addObserver:self selector:@selector(didReceiveKeyboardWillHideNotification:) name:UIKeyboardWillHideNotification object:nil];
        [center addObserver:self selector:@selector(didReceiveKeyboardDidHideNotification:) name:UIKeyboardDidHideNotification object:nil];
        [center addObserver:self selector:@selector(didReceiveKeyboardWillChangeFrameNotification:) name:UIKeyboardWillChangeFrameNotification object:nil];
    }
}

+ (void)endEditing {
    [UIKeyboardtGetCurrentWindow() endEditing:YES];
}

#pragma mark - public
- (void)keyboardOverlapHeightChanged:(float)keyboardHeight {}

- (void)keyboardWillShow {}

- (void)keyboardWillHide {}

- (void)bindComposeView:(nullable UIView *)view {
    self.composeView = view;
}

- (void)willKeyboardHeightChanged:(CGFloat)height {
    UIScreen *mainScreen = [UIScreen mainScreen];
    const CGFloat composeViewHeight = CGRectGetHeight(self.composeView.frame);
    const CGFloat composeViewBottomY = [mainScreen.coordinateSpace convertPoint:CGPointMake(0, composeViewHeight)
                                                            fromCoordinateSpace:self.composeView.coordinateSpace].y;
    const CGFloat bottomIndent = mainScreen.bounds.size.height - composeViewBottomY;
    
    const CGFloat keyboardCeilHeight = ceil(height);
    const CGFloat keyboardHeight = MAX(keyboardCeilHeight - bottomIndent, 0);
    [self keyboardOverlapHeightChanged:keyboardHeight];
}

#pragma mark - UIKeyboardNotifications

/// 键盘即将弹出
- (void)didReceiveKeyboardWillShowNotification:(NSNotification *)notification {
    float duration = UIKeyboardDurationFromNotification(notification);
    if (duration <= 0) {
        return;
    }
    if (self.isShowAnimating) {
        return;
    }
    self.isShowAnimating = YES;
    float height = UIKeyboardHeightFromNotification(notification);
    _keyboardHeight = height;
    UIViewAnimationOptions option = UIViewAnimationOptionFromNotification(notification);
    [self doAnimation:height duration:duration animationOption:option isShow:YES];
    [self keyboardWillShow];
}

/// 键盘已经弹出
- (void)didReceiveKeyboardDidShowNotification:(NSNotification *)notification {
    self.isShowAnimating = NO;
    [self invalidateDisplayLink];
    float duration = UIKeyboardDurationFromNotification(notification);
    if (duration <= 0) {
        return;
    }
    float height = UIKeyboardHeightFromNotification(notification);
    _keyboardHeight = height;
    [self willKeyboardHeightChanged:height];
}

/// 键盘即将隐藏
- (void)didReceiveKeyboardWillHideNotification:(NSNotification *)notification {
    float duration = UIKeyboardDurationFromNotification(notification);
    if (duration <= 0 || self.isHideAnimating) {
        return;
    }
    self.isHideAnimating = YES;
    UIViewAnimationOptions option = UIViewAnimationOptionFromNotification(notification);
    [self doAnimation:0 duration:duration animationOption:option isShow:NO];
    [self keyboardWillHide];
}

/// 键盘已经隐藏
- (void)didReceiveKeyboardDidHideNotification:(NSNotification *)notification {
    self.isHideAnimating = NO;
    [self invalidateDisplayLink];
    float duration = UIKeyboardDurationFromNotification(notification);
    if (duration <= 0) {
        return;
    }
}

/// 键盘即将改变 frame
- (void)didReceiveKeyboardWillChangeFrameNotification:(NSNotification *)notification {
    float duration = UIKeyboardDurationFromNotification(notification);
    // duration <= 0 表明在键盘弹起的状态下转屏
    if (duration <= 0) {
        float height = UIKeyboardHeightFromNotification(notification);
        [self willKeyboardHeightChanged:height];
    }
}

/// CADisplayLink 回调
- (void)displayLinkCallBack {
    // presentationLayer 是每一帧都需要重新获取的
    CGFloat height = CGRectGetMaxY(self.animationView.layer.presentationLayer.frame);
    [self willKeyboardHeightChanged:height];
}

- (void)doAnimation:(CGFloat)keyBoardHeight duration:(float)duration animationOption:(UIViewAnimationOptions)animationOption isShow:(BOOL)isShow {
    UIView *animationView = self.animationView;
    if (!animationView.superview) {
        [UIKeyboardtGetCurrentWindow() addSubview:animationView];
    }
    
    [animationView.layer removeAllAnimations];
    CGFloat originY = isShow ? keyBoardHeight : 0;
    
    CADisplayLink *displayLink = self.displayLink;
    [displayLink addToRunLoop:[NSRunLoop mainRunLoop] forMode:NSRunLoopCommonModes];
    
    [UIView animateWithDuration:duration delay:0 options:animationOption animations:^{
        animationView.frame = CGRectMake(0, originY, 0, 0);
    } completion:^(BOOL finished) {
        if (finished) {
            [animationView removeFromSuperview];
        }
    }];
}

- (void)invalidateDisplayLink {
    [_displayLink invalidate];
    _displayLink = nil;
}

- (void)dispose {
    [self invalidateDisplayLink];
    [_animationView removeFromSuperview];
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

#pragma mark - getter
- (CADisplayLink *)displayLink {
    if (!_displayLink) {
        _displayLink = [CADisplayLink displayLinkWithTarget:self selector:@selector(displayLinkCallBack)];
    }
    return _displayLink;
}

- (void)dealloc {
    [self dispose];
}

@end
