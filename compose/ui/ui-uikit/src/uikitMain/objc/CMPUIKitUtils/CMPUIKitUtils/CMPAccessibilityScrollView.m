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

@implementation CMPAccessibilityScrollView {
    CMPAccessibilityElement *_element;
}

- (instancetype)initWithAccessibilityElement:(CMPAccessibilityElement *)element {
    self = [super initWithFrame:CGRectZero];
    
    if (self) {
        _element = element;
    }
    
    return self;
}

- (UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event {
    return nil;
}

- (BOOL)isAccessibilityElement {
    if (!_element.isAlive) {
        return NO;
    }

    if (_element.isAccessibilityElement) {
        return YES;
    }
    
    
    if (self.contentSize.width > self.frame.size.width ||
        self.contentSize.height > self.frame.size.height) {
        return !_element.areAnyAccessibilityServicesEnabled;
    } else {
        return NO;
    }
}

- (NSString *)accessibilityLabel {
    return _element.accessibilityLabel;
}

- (NSString *)accessibilityHint {
    return _element.accessibilityHint;
}

- (BOOL)accessibilityActivate {
    return [_element accessibilityActivate];
}

- (void)accessibilityIncrement {
    [_element accessibilityIncrement];
}

- (void)accessibilityDecrement {
    [_element accessibilityDecrement];
}

- (BOOL)accessibilityScroll:(UIAccessibilityScrollDirection)direction {
    return [_element accessibilityScroll:direction];
}

- (BOOL)accessibilityPerformEscape {
    return [_element accessibilityPerformEscape];
}

- (void)accessibilityElementDidBecomeFocused {
    [_element accessibilityElementDidBecomeFocused];
}

- (void)accessibilityElementDidLoseFocus {
    [_element accessibilityElementDidLoseFocus];
}

- (id)accessibilityContainer {
    return _element.accessibilityContainer;
}

- (NSInteger)accessibilityElementCount {
    return _element.childrenCount;
}

@end
