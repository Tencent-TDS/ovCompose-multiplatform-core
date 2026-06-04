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

#import "OVMPAccessibilityElementProxy.h"
#import "OVMPAccessibilityElementsCache.h"
#import "OVMPAccessibilityElement.h"
#import "OVMPAccessibilityUtils.h"

@interface OVMPAccessibilityElementProxy()

/// cache 由 kotlin 侧设置过来
@property (nonatomic, strong, nullable) OVMPAccessibilityElementsCache *cache;
@property (nonatomic, strong) NSMutableArray <OVMPAccessibilityElementProxy *> *children;
@property (nonatomic, strong) UIView *interopView;

@end

@implementation OVMPAccessibilityElementProxy

- (instancetype)init {
    if (self = [super init]) {
        _identifierKey = OVMPAllocAccessibilityElementId();
    }
    return self;
}

#pragma mark - getter
- (NSMutableArray<OVMPAccessibilityElementProxy *> *)children {
    if (!_children) {
        _children = @[].mutableCopy;
    }
    return _children;
}

#pragma mark -
- (NSString *)accessibilityLabel {
    if (!_accessibilityLabel && _sync) {
        _sync(self, OVMPAccessibilityElementSyncTypeAccessibilityLabel, 0);
    }
    return _accessibilityLabel;
}

- (void)accessibilityElementDidBecomeFocused {
    if (_sync) {
        _sync(self, OVMPAccessibilityElementSyncTypeAccessibilityDidBecomeFocused, 0);
    }
}

- (void)accessibilityElementDidLoseFocus {
    if (_sync) {
        _sync(self, OVMPAccessibilityElementSyncTypeAccessibilityDidLoseFocus, 0);
    }
}

- (BOOL)accessibilityActivate {
    if (_sync) {
        _sync(self, OVMPAccessibilityElementSyncTypeActive, 0);
    }
    return _accessibilityActivate;
}

- (void)accessibilityIncrement {
    if (_sync) {
        _sync(self, OVMPAccessibilityElementSyncTypeIncrement, 0);
    }
}

- (void)accessibilityDecrement {
    if (_sync) {
        _sync(self, OVMPAccessibilityElementSyncTypeDecrement, 0);
    }
}

- (BOOL)accessibilityScroll:(UIAccessibilityScrollDirection)direction {
    if (_sync) {
        _sync(self, OVMPAccessibilityElementSyncTypeAccessibilityScrollResult, direction);
    }
    return _accessibilityScrollResult;
}

- (BOOL)isAccessibilityElement {
    // 记得这个值需要直接赋值 这里是 kotlin 直接赋值，不需要处理，但是这里得有这个注释
    return _isAccessibilityElement;
}

- (NSString *)accessibilityIdentifier {
    if (!_accessibilityIdentifier && _sync) {
        _sync(self, OVMPAccessibilityElementSyncTypeAccessibilityIdentifier, 0);
    }
    return _accessibilityIdentifier;
}

- (NSString *)accessibilityHit {
    if (!_accessibilityHint && _sync) {
        _sync(self, OVMPAccessibilityElementSyncTypeAccessibilityHint, 0);
    }
    return _accessibilityHint;
}

- (NSArray<UIAccessibilityCustomAction *> *)accessibilityCustomActions {
    if (!_accessibilityCustomActions && _sync) {
        _sync(self, OVMPAccessibilityElementSyncTypeAccessibilityCustomActions, 0);
    }
    return _accessibilityCustomActions;
}

- (UIAccessibilityTraits)accessibilityTraits {
    // 这个值需要直接赋值 这里是 kotlin 直接赋值，不需要处理，但是这里得有这个注释
    return _accessibilityTraits;
}

- (NSString *)accessibilityValue {
    if (!_accessibilityValue && _sync) {
        _sync(self, OVMPAccessibilityElementSyncTypeAccessibilityValue, 0);
    }
    return _accessibilityValue;
}

- (BOOL)accessibilityPerformEscape {
    if (_sync) {
        _sync(self, OVMPAccessibilityElementSyncTypePerformEscape, 0);
    }
    return _accessibilityPerformEscape;
}

- (UIAccessibilityContainerType)accessibilityContainerType {
    // 这个值需要直接赋值 这里是 kotlin 直接赋值，不需要处理，但是这里得有这个注释
    return _accessibilityContainerType;
}

- (BOOL)canBecomeFocused {
    if (_sync) {
        _sync(self, OVMPAccessibilityElementSyncTypeCanBecomeFocused, 0);
    }
    return _canBecomeFocused;
}

- (void)didResignFocused {
    if (_sync) {
        _sync(self, OVMPAccessibilityElementSyncTypeDidResignFocused, 0);
    }
}

- (void)didBecomeFocused {
    if (_sync) {
        _sync(self, OVMPAccessibilityElementSyncTypeDidBecomeFocused, 0);
    }
}

- (void)setNeedsFocusUpdate {
    if (_sync) {
        _sync(self, OVMPAccessibilityElementSyncTypeSetNeedsFocusUpdate, 0);
    }
}

- (BOOL)accessibilityScrollToVisible {
    if (_sync) {
        _sync(self, OVMPAccessibilityElementSyncTypeAccessibilityScrollToVisible, 0);
    }
    return _accessibilityScrollToVisible;
}

#pragma mark -
- (BOOL)isAccessibilityFrameEmpty {
    return CGRectIsEmpty(self.accessibilityFrame);
}

- (void)setFocusFrame:(CGFloat)originX originY:(CGFloat)originY width:(CGFloat)width height:(CGFloat)height {
    _focusFrame = CGRectMake(originX, originY, width, height);
}

- (void)setAccessibilityFrame:(CGFloat)originX originY:(CGFloat)originY width:(CGFloat)width height:(CGFloat)height {
    _accessibilityFrame = CGRectMake(originX, originY, width, height);
}

- (void)willChangeAccessibilityElements {
    [self.children removeAllObjects];
}

- (void)addAccessibilityElementProxy:(OVMPAccessibilityElementProxy *)element {
    if ([element isKindOfClass:[OVMPAccessibilityElementProxy class]]) {
        [self.children addObject:element];
    }
}

- (void)addAccessibilityInteropView:(UIView *)interopView {
    if ([interopView isKindOfClass:[UIView class]]) {
        self.interopView = interopView;
    }
}

- (void)didChangeAccessibilityElements:(OVMPAccessibilityElementsCache *)cache {
    self.cache = cache;
    NSArray <OVMPAccessibilityElementProxy *> *children = self.children;
    NSMutableArray *accessibilityElements = (children.count > 0) ? [NSMutableArray arrayWithCapacity:children.count + 1] : nil;
    for (NSInteger i = 0; i < children.count; i++) {
        OVMPAccessibilityElementProxy *proxy = children[i];
        OVMPAccessibilityElement *childElement = [cache getOrCreateElementWithProxy:proxy];
        [accessibilityElements addObject:childElement];
    }
    
    UIView *interopView = self.interopView;
    if (interopView) {
        accessibilityElements = accessibilityElements ?: @[].mutableCopy;
        [accessibilityElements addObject:interopView];
        self.interopView = nil;
    }
    
    OVMPAccessibilityElement *currentAccessibilityElement = [cache getOrCreateElementWithProxy:self];
    [currentAccessibilityElement setAccessibilityElements:accessibilityElements];
}

- (void)clear {
    _sync = nil;
    [_children removeAllObjects];
    _accessibilityScrollResult = NO;
    _accessibilityHint = nil;
    _accessibilityLabel = nil;
    _accessibilityValue = nil;
    _accessibilityIdentifier = nil;
    _accessibilityCustomActions = nil;
    _accessibilityActivate = NO;
    _accessibilityContainerType = UIAccessibilityContainerTypeNone;
}

- (void)dispose {
    [self clear];
    [_cache removeElementWithProxy:self];
    _cache = nil;
}

- (id)debugAccessibilityContainer {
    return [[self.cache getOrCreateElementWithProxy:self] accessibilityContainer];
}

@end
