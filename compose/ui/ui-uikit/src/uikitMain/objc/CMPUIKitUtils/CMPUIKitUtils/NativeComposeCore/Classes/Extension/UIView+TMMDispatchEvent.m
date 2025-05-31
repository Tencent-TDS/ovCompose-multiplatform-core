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

#import "UIView+TMMDispatchEvent.h"

@implementation UIView (TMMDispatchEvent)

- (BOOL)tmm_consumeComposeEvent:(UIEvent *)event {
    NSSet *touches = [event allTouches];
    UITouch *touch = [touches anyObject];
    if (touch) {
        CGPoint touchPoint = [touch locationInView:self];

        NSLog(@"shouldConsumeComposeEvent touchPoint %ld", (long)touch.phase);
        if (CGRectContainsPoint(self.bounds, touchPoint)) {
            UIView *target = [self hitTest:touchPoint withEvent:event];
            if (!target) {
                return NO;
            }
            if (touch.phase == UITouchPhaseBegan) {
                [target touchesBegan:touches withEvent:event];
                for (UIGestureRecognizer *gesture in target.gestureRecognizers) {
                    [gesture touchesBegan:touches withEvent:event];
                }

            } else if (touch.phase == UITouchPhaseMoved) {
                [target touchesMoved:touches withEvent:event];
                for (UIGestureRecognizer *gesture in target.gestureRecognizers) {
                    [gesture touchesMoved:touches withEvent:event];
                }
            } else if (touch.phase == UITouchPhaseEnded) {
                [target touchesEnded:touches withEvent:event];
                for (UIGestureRecognizer *gesture in target.gestureRecognizers) {
                    [gesture touchesEnded:touches withEvent:event];
                }

                if ([target isKindOfClass:UIControl.class]) {
                    [((UIControl *)target) sendActionsForControlEvents:UIControlEventTouchUpInside];
                }
            } else if (touch.phase == UITouchPhaseCancelled) {
                [target touchesCancelled:touches withEvent:event];
                for (UIGestureRecognizer *gesture in target.gestureRecognizers) {
                    [gesture touchesCancelled:touches withEvent:event];
                }
            }

            return YES;
        }
    }
    return YES;
}
@end
