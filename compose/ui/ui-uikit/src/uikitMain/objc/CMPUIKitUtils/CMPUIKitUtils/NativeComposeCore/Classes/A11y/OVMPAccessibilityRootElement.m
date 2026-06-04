/*
 * Copyright 2023 The Android Open Source Project
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

#import "OVMPAccessibilityRootElement.h"
#import "OVMPAccessibilityElement.h"
#import "OVMPAccessibilityElementsCache.h"

#pragma mark - debug
@interface OVMPAccessibilityElementsCache()
- (nullable id)accessibilityElementWithIdentifier:(NSString *)identifier;
@end

#pragma mark - OVMPAccessibilityRootElement
@interface OVMPAccessibilityRootElement()
@property (nonatomic, strong, nullable) OVMPAccessibilityElement *element;
@property (nonatomic, strong, readwrite) OVMPAccessibilityElementsCache *cache;
@end

@implementation OVMPAccessibilityRootElement

+ (instancetype)accessibilityRootElement {
    return [[self alloc] initWithAccessibilityContainer:[NSObject new]];
}

- (OVMPAccessibilityElementsCache *)cache {
    if (!_cache) {
        _cache = [[OVMPAccessibilityElementsCache alloc] init];
    }
    return _cache;
}

- (void)setElement:(OVMPAccessibilityElement *)element {
    // 1. 清除旧的 accessibilityContainer，孩子不用管
    if (_element.accessibilityContainer == self) {
        _element.accessibilityContainer = nil;
    }
    
    _element = element;
    _element.accessibilityContainer = self;

    // 2. 更新 accessibilityElements
    NSArray *childElements = element ? @[element] : nil;
    [self setAccessibilityElements:childElements];
    
    // 3. 通知 kotlin 侧
    if (self.onScreenReaderActive) {
        self.onScreenReaderActive(element != nil);
    }
}

- (void)bindProxy:(OVMPAccessibilityElementProxy *)proxy {
    self.element = [self.cache getOrCreateElementWithProxy:proxy];
}

- (void)callKtActivateAccessibilityIfNeeded {
    if (self.activateAccessibilityIfNeeded) {
        self.activateAccessibilityIfNeeded();
    }
}

#pragma mark - public
- (id)accessibilityContainer {
    return self.mediatorView;
}

- (CGRect)accessibilityFrame {
    UIView *mediatorView = self.mediatorView;
    CGRect rect = [mediatorView convertRect:mediatorView.bounds toView:nil];
    return rect;
}

- (BOOL)isAccessibilityElement {
    return NO;
}

- (NSInteger)accessibilityElementCount {
    return 1;
}

- (id)accessibilityElementAtIndex:(NSInteger)index {
    [self callKtActivateAccessibilityIfNeeded];
    return self.element;
}

- (NSArray *)accessibilityElements {
    [self callKtActivateAccessibilityIfNeeded];
    
    if (self.element) {
        return @[self.element];
    }
    return nil;
}

- (void)pressesBegan:(NSSet<UIPress *> *)presses withEvent:(UIPressesEvent *)event {
    if (self.onKeyboardPresses) {
        self.onKeyboardPresses(presses);
    }
    [super pressesBegan:presses withEvent:event];
}

- (void)pressesEnded:(NSSet<UIPress *> *)presses withEvent:(UIPressesEvent *)event {
    if (self.onKeyboardPresses) {
        self.onKeyboardPresses(presses);
    }
    [super pressesEnded:presses withEvent:event];
}

#pragma mark - UIFocusItemContainer
- (id<UICoordinateSpace>)coordinateSpace {
    return self.mediatorView;
}

- (nonnull NSArray<id<UIFocusItem>> *)focusItemsInRect:(CGRect)rect {
    [self callKtActivateAccessibilityIfNeeded];
    
    if (self.element) {
        return @[self.element];
    }
    return nil;
}

- (void)dispose {
    [_cache dispose];
    _cache = nil;
    _mediatorView = nil;
    _element = nil;
    _activateAccessibilityIfNeeded = nil;
    _onScreenReaderActive = nil;
    _onKeyboardPresses = nil;
}

#pragma mark - debug

/// 给到通过无障碍模式的单测使用，仅限于 debug 环境
- (nullable id)accessibilityElementWithIdentifier:(NSString *)identifier {
    return [self.cache accessibilityElementWithIdentifier:identifier];
}

@end
