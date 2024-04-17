//
//  CMPTextLoupeSession.h
//  CMPUIKitUtils
//
//  Created by Andrei.Salavei on 17.04.24.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

/// Wrapper class around UITextLoupeSession that prevents class type loading for iOS < 17.0
API_AVAILABLE(ios(17.0))
@interface CMPTextLoupeSession : NSObject

- (id)init NS_UNAVAILABLE;

+ (nullable instancetype)beginLoupeSessionAtPoint:(CGPoint)point
                          fromSelectionWidgetView:(nullable UIView *)selectionWidget
                                           inView:(UIView *)interactionView;

- (void)moveToPoint:(CGPoint)point withCaretRect:(CGRect)caretRect trackingCaret:(BOOL)tracksCaret;

- (void)invalidate;

@end

NS_ASSUME_NONNULL_END
