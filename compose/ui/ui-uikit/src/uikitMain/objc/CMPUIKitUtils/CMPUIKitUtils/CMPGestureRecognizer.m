//
//  CMPGestureRecognizer.m
//  CMPUIKitUtils
//
//  Created by Ilia.Semenov on 28/06/2024.
//

#import "CMPGestureRecognizer.h"

@implementation CMPGestureRecognizer {
    dispatch_block_t _scheduledFailureBlock;
}

- (instancetype)init {
    self = [super init];
    
    if (self) {        
        self.delegate = self;
        [self addTarget:self action:@selector(handleStateChange)];
    }
    
    return self;
}

- (void)handleStateChange {
    switch (self.state) {
        case UIGestureRecognizerStateEnded:
        case UIGestureRecognizerStateCancelled:
            [self cancelFailure];
            break;

        default:
            break;
    }
}

- (BOOL)shouldRecognizeSimultaneouslyWithGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer {
    // We should recognize simultaneously only with the gesture recognizers
    // belonging to itself or to the views up in the hierarchy.
    // An exception: UIScreenEdgePanGestureRecognizer, this always has precedence over us and is
    // not allowed to recognize simultaneously

    // Can't proceed if either view is null
    UIView *view = self.view;
    UIView *otherView = otherGestureRecognizer.view;
    
    if (view == nil || otherView == nil) {
        return NO;
    }
    
    BOOL otherIsAscendant = ![otherView isDescendantOfView:view];
    
    if (otherIsAscendant && [otherGestureRecognizer isKindOfClass:[UIScreenEdgePanGestureRecognizer class]]) {
        return NO;
    }

    return otherView == view || otherIsAscendant;
}

- (BOOL)shouldRequireFailureOfGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer {
    // Two situations are possible here.
    // 1. If it's a gesture recognizer of a descendant (interop) view,
    // we should wait until it fails,
    // if it's a UITapGestureRecognizer.
    //
    // 2. It's a gesture recognizer of the view itself, or it's an ascendant view.
    // We don't require failure of it, unless it's a `UIScreenEdgePanGestureRecognizer`.
    UIView *view = self.view;
    UIView *otherView = otherGestureRecognizer.view;
    
    if (view == nil || otherView == nil) {
        return NO;
    }

    BOOL otherIsDescendant = [otherView isDescendantOfView:view];
    BOOL otherIsAscendantOrSameView = !otherIsDescendant;

    // (1)
    if (otherIsDescendant && [otherGestureRecognizer isKindOfClass:[UITapGestureRecognizer class]]) {
        return YES;
    }
    
    // (2)
    if (otherIsAscendantOrSameView && [otherGestureRecognizer isKindOfClass:[UIScreenEdgePanGestureRecognizer class]]) {
        return YES;
    }

    return NO;
}

- (BOOL)shouldBeRequiredToFailByGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer {
    
    // otherGestureRecognizer is UITapGestureRecognizer,
    // it must not wait till we fail and has priority
    if ([otherGestureRecognizer isKindOfClass:[UITapGestureRecognizer class]]) {
        return NO;
    }
    
    UIView *view = self.view;
    UIView *otherView = otherGestureRecognizer.view;

    if (view == nil || otherView == nil) {
        return NO;
    }

    BOOL otherIsDescendant = [otherView isDescendantOfView:view];
    BOOL otherIsAscendantOrSameView = !otherIsDescendant;
    
    if (otherIsAscendantOrSameView && [otherGestureRecognizer isKindOfClass:[UIScreenEdgePanGestureRecognizer class]]) {
        return NO;
    }
    // Otherwise it is required to fail (aka other kind of gesture recognizer on interop view)
    return view != otherView;
}

- (void)cancelFailure {
    if (_scheduledFailureBlock) {
        dispatch_block_cancel(_scheduledFailureBlock);
        _scheduledFailureBlock = NULL;
    }
}

- (void)fail {
    [self.handler onFailure];
}

- (void)scheduleFailure:(NSTimeInterval)failureDelay {
    __weak typeof(self) weakSelf = self;
    dispatch_block_t dispatchBlock = dispatch_block_create(0, ^{
        [weakSelf fail];
    });
    
    if (_scheduledFailureBlock) {
        dispatch_block_cancel(_scheduledFailureBlock);
    }
    _scheduledFailureBlock = dispatchBlock;
    
    dispatch_time_t dispatchTime = dispatch_time(DISPATCH_TIME_NOW, (int64_t)(failureDelay * NSEC_PER_SEC));

    // Schedule the block to be executed at `dispatchTime`
    dispatch_after(dispatchTime, dispatch_get_main_queue(), dispatchBlock);
}

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.handler touchesBegan:touches withEvent:event];
}

- (void)touchesMoved:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.handler touchesMoved:touches withEvent:event];
}

- (void)touchesEnded:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.handler touchesEnded:touches withEvent:event];
}

- (void)touchesCancelled:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.handler touchesCancelled:touches withEvent:event];
}

@end
