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


#import "CMPAccessibilityScrollView.h"
#import "CMPAccessibilityElement.h"

@implementation CMPAccessibilityScrollView

- (instancetype)init {
    self = [super initWithFrame:CGRectZero];
    
    return self;
}

- (BOOL)isAccessibilityElement {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (NSString *)accessibilityLabel {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (NSString *)accessibilityHint {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (BOOL)accessibilityActivate {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (void)accessibilityIncrement {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (void)accessibilityDecrement {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (BOOL)accessibilityScroll:(UIAccessibilityScrollDirection)direction {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (BOOL)accessibilityPerformEscape {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (void)accessibilityElementDidBecomeFocused {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (void)accessibilityElementDidLoseFocus {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (id)accessibilityContainer {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (NSInteger)accessibilityElementCount {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

@end
