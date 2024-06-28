//
//  CMPGestureRecognizer.h
//  CMPUIKitUtils
//
//  Created by Ilia.Semenov on 28/06/2024.
//

#import <UIKit/UIKit.h>
#import <UIKit/UIGestureRecognizerSubclass.h>

NS_ASSUME_NONNULL_BEGIN

@protocol CMPGestureRecognizerProxy <NSObject>

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent * _Nullable)event;
- (void)touchesMoved:(NSSet<UITouch *> *)touches withEvent:(UIEvent * _Nullable)event;
- (void)touchesEnded:(NSSet<UITouch *> *)touches withEvent:(UIEvent * _Nullable)event;
- (void)touchesCancelled:(NSSet<UITouch *> *)touches withEvent:(UIEvent * _Nullable)event;

@end

@interface CMPGestureRecognizer : UIGestureRecognizer

@property (weak, nonatomic) id <CMPGestureRecognizerProxy> proxy;

@end

NS_ASSUME_NONNULL_END
