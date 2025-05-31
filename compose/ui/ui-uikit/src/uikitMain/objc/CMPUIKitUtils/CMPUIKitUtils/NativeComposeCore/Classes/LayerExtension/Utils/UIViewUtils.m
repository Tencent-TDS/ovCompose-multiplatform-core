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

#import "UIViewUtils.h"
#import <objc/runtime.h>
#import "TMMCMPUIViewMixEvent.h"

static void (*originAddSubviewIMP)(__unsafe_unretained id, SEL, id);
static void (*removeFromSuperViewIMP)(__unsafe_unretained id, SEL);
static void (*originSetHiddenIMP)(__unsafe_unretained id, SEL, BOOL);

#pragma mark - CMPUIViewHookOptimization
@interface CMPUIViewHookOptimization : NSObject

@end

@implementation CMPUIViewHookOptimization

/// 单独抽出 optimization 方法，方便在其他地方更早的调用
+ (void)optimization {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        // 存储原始的 addSubview 和 removeFromSuperview 的指针，即使后续被 hook，也不影响已记录的指针
        Class cls = [UIView class];
        originAddSubviewIMP = (void *)class_getMethodImplementation(cls, @selector(addSubview:));
        removeFromSuperViewIMP = (void *)class_getMethodImplementation(cls, @selector(removeFromSuperview));
        originSetHiddenIMP = (void *)class_getMethodImplementation(cls, @selector(setHidden:));
    });
}

@end

#pragma mark - UIView
@interface UIView (CMP)
@end

@implementation UIView (CMP)

+ (void)load {
    [CMPUIViewHookOptimization optimization];
}

@end

void TMMCMPUIViewFastAddSubview(UIView *superview, UIView *subview) {
    originAddSubviewIMP(superview, @selector(addSubview:), subview);
}

void TMMCMPUIViewFastRemoveFromSuperview(UIView *view) {
    removeFromSuperViewIMP(view, @selector(removeFromSuperview));
}

void TMMCMPUIViewFastSetHidden(UIView *view, BOOL hidden) {
    originSetHiddenIMP(view, @selector(setHidden:), hidden);
}

BOOL TMMCMPUIViewShouldConsumeEvent(_Nullable id touchEvent, UIView *wrappingView) {
    NSArray *subviews = [wrappingView subviews];
    if (subviews.count == 0) {
        // 默认为消费
        return YES;
    }

    UIView *nativeView = subviews[0];
    if ([touchEvent isKindOfClass:[UIEvent class]] && [nativeView respondsToSelector:@selector(shouldConsumeComposeEvent:)]) {
        return [(id<TMMCMPUIViewMixEvent>)nativeView shouldConsumeComposeEvent:touchEvent];
    }
    // 默认为不消费
    return NO;
}

UIImage *OVCMPSnapshotImageFromUIView(UIView *view, float width, float height, float density) {
    static UIGraphicsImageRendererFormat *format;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        format = [UIGraphicsImageRendererFormat defaultFormat];
    });
    format.scale = density;
    CGRect bounds = CGRectMake(0, 0, width, height);
    UIGraphicsImageRenderer *renderer = [[UIGraphicsImageRenderer alloc] initWithBounds:bounds format:format];

    UIImage *image = [renderer imageWithActions:^(UIGraphicsImageRendererContext *context) {
        [view.layer renderInContext:context.CGContext];
    }];
    return image;
}
