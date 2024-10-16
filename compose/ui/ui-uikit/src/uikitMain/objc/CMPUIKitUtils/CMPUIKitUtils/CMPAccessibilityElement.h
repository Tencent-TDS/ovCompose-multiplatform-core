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

#import <UIKit/UIKit.h>
#import "CMPMacros.h"

NS_ASSUME_NONNULL_BEGIN

@interface CMPAccessibilityElement : UIAccessibilityElement

- (__nullable id)resolveAccessibilityContainer CMP_ABSTRACT_FUNCTION;

+ (__nullable id)accessibilityContainerOfObject:(id)object;

// MARK: Unexported methods redeclaration block
// Redeclared to make it visible to Kotlin for override purposes, workaround for the following issue:
// https://youtrack.jetbrains.com/issue/KT-56001/Kotlin-Native-import-Objective-C-category-members-as-class-members-if-the-category-is-located-in-the-same-file

- (NSArray<UIAccessibilityCustomAction *> *)accessibilityCustomActions CMP_ABSTRACT_FUNCTION;

- (UIAccessibilityTraits)accessibilityTraits CMP_ABSTRACT_FUNCTION;

- (NSString *__nullable)accessibilityIdentifier CMP_ABSTRACT_FUNCTION;

- (NSString *__nullable)accessibilityHint CMP_ABSTRACT_FUNCTION;

- (NSString *__nullable)accessibilityLabel CMP_ABSTRACT_FUNCTION;

- (NSString *__nullable)accessibilityValue CMP_ABSTRACT_FUNCTION;

- (CGRect)accessibilityFrame CMP_ABSTRACT_FUNCTION;

- (BOOL)isAccessibilityElement CMP_ABSTRACT_FUNCTION;

- (BOOL)accessibilityActivate CMP_ABSTRACT_FUNCTION;

// Private SDK method. Calls when the item is swipe-to-focused in VoiceOver.
- (BOOL)accessibilityScrollToVisible;

// Private SDK method. Calls when the item is swipe-to-focused in VoiceOver.
- (BOOL)accessibilityScrollToVisibleWithChild:(id)child;

- (void)accessibilityElementDidBecomeFocused;

- (void)accessibilityElementDidLoseFocus;

- (BOOL)accessibilityScroll:(UIAccessibilityScrollDirection)direction CMP_ABSTRACT_FUNCTION;

- (BOOL)accessibilityPerformEscape;

@end

NS_ASSUME_NONNULL_END
