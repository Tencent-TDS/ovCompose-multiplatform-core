//
//  CMPGestureRecognizer.m
//  CMPUIKitUtils
//
//  Created by Ilia.Semenov on 28/06/2024.
//

#import "CMPGestureRecognizer.h"

@implementation CMPGestureRecognizer

- (instancetype)init {
    self = [super init];
    
    if (self) {
        self.cancelsTouchesInView = false;
    }
    
    return self;
}

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    
}

- (void)touchesMoved:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    
}

- (void)touchesEnded:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    
}

- (void)touchesCancelled:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    
}

@end
