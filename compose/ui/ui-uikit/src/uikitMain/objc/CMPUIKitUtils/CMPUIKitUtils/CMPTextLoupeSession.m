//
//  CMPTextLoupeSession.m
//  CMPUIKitUtils
//
//  Created by Andrei.Salavei on 17.04.24.
//

#import "CMPTextLoupeSession.h"
#import <UIKit/UIKit.h>

@implementation CMPTextLoupeSession {
    id _session;
}

+ (nullable instancetype)beginLoupeSessionAtPoint:(CGPoint)point
                          fromSelectionWidgetView:(nullable UIView *)selectionWidget
                                           inView:(UIView *)interactionView {
    CMPTextLoupeSession *session = [CMPTextLoupeSession new];
    if (@available(iOS 17, *)) {
        session->_session = [UITextLoupeSession beginLoupeSessionAtPoint:point
                                                 fromSelectionWidgetView:selectionWidget
                                                                  inView:interactionView];
    } else {
        [NSException raise:@"UITextLoupeSession is not available" format:@"The method must be called from iOS 17+"];
    }
    return session;
}

- (UITextLoupeSession *)session API_AVAILABLE(ios(17.0)) {
    return (UITextLoupeSession *)_session;
}

- (void)moveToPoint:(CGPoint)point
      withCaretRect:(CGRect)caretRect
      trackingCaret:(BOOL)tracksCaret API_AVAILABLE(ios(17.0)) {
    [self.session moveToPoint:point withCaretRect:caretRect trackingCaret:tracksCaret];
}

- (void)invalidate API_AVAILABLE(ios(17.0)) {
    [self.session invalidate];
}

@end
