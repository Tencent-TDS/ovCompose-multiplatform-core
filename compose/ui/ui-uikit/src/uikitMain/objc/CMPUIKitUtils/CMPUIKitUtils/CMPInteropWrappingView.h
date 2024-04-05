//
//  CMPInteropWrappingView.h
//  CMPUIKitUtils
//
//  Created by Ilia.Semenov on 05/04/2024.
//

#import <UIKit/UIKit.h>
#import "CMPMacros.h"

NS_ASSUME_NONNULL_BEGIN

@interface CMPInteropWrappingView : UIView

- (__nullable id)accessibilityContainer CMP_MUST_BE_OVERRIDED;

@end

NS_ASSUME_NONNULL_END
