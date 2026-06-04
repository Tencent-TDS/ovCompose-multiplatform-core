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

#import "OVMPAccessibilityElement.h"

NS_INLINE NSObject *OVMPDummyContainerObject(void) {
    static NSObject *dummyObject = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        dummyObject = [[NSObject alloc] init];
    });
    return dummyObject;
}

@interface OVMPAccessibilityElement()
@property (nonatomic, strong) OVMPAccessibilityElementProxy *proxy;
@end

@implementation OVMPAccessibilityElement

- (instancetype)initWithElementProxy:(OVMPAccessibilityElementProxy *)proxy {
    if (self = [self initWithAccessibilityContainer:OVMPDummyContainerObject()]) {
        _proxy = proxy;
    }
    return self;
}

- (NSString *)accessibilityLabel {
    return self.proxy.accessibilityLabel;
}

- (CGRect)accessibilityFrame {
    return self.proxy.accessibilityFrame;
}

- (void)accessibilityElementDidBecomeFocused {
    [self.proxy accessibilityElementDidBecomeFocused];
}

- (void)accessibilityElementDidLoseFocus {
    [self.proxy accessibilityElementDidLoseFocus];
}

- (BOOL)accessibilityActivate {
    return self.proxy.accessibilityActivate;
}

- (void)accessibilityIncrement {
    [self.proxy accessibilityIncrement];
}

- (void)accessibilityDecrement {
    [self.proxy accessibilityDecrement];
}

- (BOOL)accessibilityScroll:(UIAccessibilityScrollDirection)direction {
    return [self.proxy accessibilityScroll:direction];
}

- (BOOL)isAccessibilityElement {
    return [self.proxy isAccessibilityElement];
}

- (NSString *)accessibilityIdentifier {
    return self.proxy.accessibilityIdentifier;
}

- (NSString *)accessibilityHint {
    return [self.proxy accessibilityHint];
}

- (NSArray<UIAccessibilityCustomAction *> *)accessibilityCustomActions {
    return [self.proxy accessibilityCustomActions];
}

- (UIAccessibilityTraits)accessibilityTraits {
    return [self.proxy accessibilityTraits];
}

- (NSString *)accessibilityValue {
    return [self.proxy accessibilityValue];
}

- (BOOL)accessibilityPerformEscape {
    return [self.proxy accessibilityPerformEscape];
}

- (UIAccessibilityContainerType)accessibilityContainerType {
    return [self.proxy accessibilityContainerType];
}

// Private SDK method. Calls when the item is swipe-to-focused in VoiceOver.
- (BOOL)accessibilityScrollToVisible {
    return [self.proxy accessibilityScrollToVisible];
}

// Private SDK method. Calls when the item is swipe-to-focused in VoiceOver.
- (BOOL)accessibilityScrollToVisibleWithChild:(id)child {
    OVMPAccessibilityElement *childElement = child;
    if ([childElement isKindOfClass:[OVMPAccessibilityElement class]]) {
        return [childElement accessibilityScrollToVisible];
    }
    return NO;
}

#pragma mark - UIFocusItem & UIFocusItemContainer
- (BOOL)canBecomeFocused {
    return [self.proxy canBecomeFocused];
}

- (void)didUpdateFocusInContext:(UIFocusUpdateContext *)context withAnimationCoordinator:(UIFocusAnimationCoordinator *)coordinator {
    if (context.previouslyFocusedItem == self) {
        [self.proxy didResignFocused];
    }
    
    if (context.nextFocusedItem == self) {
        [self.proxy didBecomeFocused];
    }
}

- (id<UIFocusItemContainer>)focusItemContainer API_AVAILABLE(ios(12.0)){
    return self;
}

- (CGRect)frame {
    return [self.proxy focusFrame];
}

- (id<UIFocusEnvironment>)parentFocusEnvironment {
    return [self accessibilityContainer];
}

- (NSArray<id<UIFocusEnvironment>> *)preferredFocusEnvironments {
    NSMutableArray <id<UIFocusEnvironment>> *focusEnvironments = [NSMutableArray array];
    NSArray <id> *accessibilityElements = [self accessibilityElements];
    for (id obj in accessibilityElements) {
        if ([obj conformsToProtocol:@protocol(UIFocusEnvironment)]) {
            [focusEnvironments addObject:obj];
        }
    }
    return focusEnvironments;
}

- (void)setNeedsFocusUpdate {
    [self.proxy setNeedsFocusUpdate];
}

- (void)updateFocusIfNeeded {
    if (@available(iOS 12.0, *)) {
        [[UIFocusSystem focusSystemForEnvironment:self] updateFocusIfNeeded];
    }
}

- (BOOL)shouldUpdateFocusInContext:(UIFocusUpdateContext *)context {
    return YES;
}

- (id<UICoordinateSpace>)coordinateSpace {
    NSObject *component = self.accessibilityContainer;
    while (component) {
        if ([component isKindOfClass:[UIView class]]) {
            return (id)component;
        }
        
        if ([(UIAccessibilityElement *)component isKindOfClass:[UIAccessibilityElement class]]) {
            component = ((UIAccessibilityElement *)component).accessibilityContainer;
        }
    }
    return nil;
}

- (NSArray<id<UIFocusItem>> *)focusItemsInRect:(CGRect)rect {
    if (@available(iOS 12.0, *)) {
        NSMutableArray <id<UIFocusItem>> *array = [NSMutableArray array];
        NSArray <id <UIFocusItem>> *elements = [self accessibilityElements];
        for (id <UIFocusItem> obj in elements) {
            if ([obj respondsToSelector:@selector(frame)] && CGRectIntersectsRect([obj frame], rect)) {
                [array addObject:obj];
            }
        }
        return array;
    }
    return nil;
}

- (BOOL)isTransparentFocusItem {
    return YES;
}

#pragma mark - public

- (int64_t)debugIdentifierKey {
    return [self.proxy identifierKey];
}

- (void)dispose {
    [self setAccessibilityContainer:nil];
    [self setAccessibilityElements:nil];
    _proxy = nil;
}

@end
