/*
 * Copyright 2024 The Android Open Source Project
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
#import <UIKit/UIGestureRecognizerSubclass.h>
#import "CMPMacros.h"

NS_ASSUME_NONNULL_BEGIN


/// Class that propery exposes the methods of `UIGestureRecognizerDelegate` to Kotlin without signature conflicts.
/// Default implementations return the same result as if the selector wasn't implemented at all, following the Apple documentation
/// See https://developer.apple.com/documentation/uikit/uigesturerecognizerdelegate
@interface CMPGestureRecognizerDelegateProxy : NSObject <UIGestureRecognizerDelegate>

- (BOOL)gestureRecognizerShouldRequireFailureOfGestureRecognizer:(UIGestureRecognizer *)gestureRecognizer otherGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer CMP_CAN_OVERRIDE;

- (BOOL)gestureRecognizerShouldRecognizeSimultaneouslyWithGestureRecognizer:(UIGestureRecognizer *)gestureRecognizer otherGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer CMP_CAN_OVERRIDE;

- (BOOL)gestureRecognizerShouldBeRequiredToFailByGestureRecognizer:(UIGestureRecognizer *)gestureRecognizer otherGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer CMP_CAN_OVERRIDE;

@end


@interface CMPGestureRecognizer : UIGestureRecognizer <UIGestureRecognizerDelegate>

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event CMP_ABSTRACT_FUNCTION;

- (void)touchesMoved:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event CMP_ABSTRACT_FUNCTION;

- (void)touchesEnded:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event CMP_ABSTRACT_FUNCTION;

- (void)touchesCancelled:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event CMP_ABSTRACT_FUNCTION;

@end


NS_ASSUME_NONNULL_END
 
