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

@interface CMPAccessibilityScrollView : UIScrollView

- (instancetype)init;

- (instancetype)initWithFrame:(CGRect)frame NS_UNAVAILABLE;

- (instancetype)initWithCoder:(NSCoder *)coder NS_UNAVAILABLE;

// MARK: Unexported methods redeclaration block

- (BOOL)isAccessibilityElement CMP_MUST_BE_OVERRIDED;

- (NSString *__nullable)accessibilityLabel CMP_MUST_BE_OVERRIDED;

- (NSString *__nullable)accessibilityHint CMP_MUST_BE_OVERRIDED;

- (BOOL)accessibilityActivate CMP_MUST_BE_OVERRIDED;

- (void)accessibilityIncrement CMP_MUST_BE_OVERRIDED;

- (void)accessibilityDecrement CMP_MUST_BE_OVERRIDED;

- (BOOL)accessibilityScroll:(UIAccessibilityScrollDirection)direction CMP_MUST_BE_OVERRIDED;

- (BOOL)accessibilityPerformEscape CMP_MUST_BE_OVERRIDED;

- (void)accessibilityElementDidBecomeFocused CMP_MUST_BE_OVERRIDED;

- (void)accessibilityElementDidLoseFocus CMP_MUST_BE_OVERRIDED;

- (__nullable id)accessibilityContainer CMP_MUST_BE_OVERRIDED;

- (NSInteger)accessibilityElementCount CMP_MUST_BE_OVERRIDED;

@end

NS_ASSUME_NONNULL_END
