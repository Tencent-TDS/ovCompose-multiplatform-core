//
//  CMPMetalDrawablesHandler.m
//  CMPUIKitUtils
//
//  Created by Ilia.Semenov on 07/06/2024.
//

#import "CMPMetalDrawablesHandler.h"

@implementation CMPMetalDrawablesHandler {
    CAMetalLayer *_metalLayer;
}

- (instancetype)initWithMetalLayer:(CAMetalLayer *)metalLayer {
    self = [super init];
    if (self) {
        _metalLayer = metalLayer;
    }
    return self;
}

- (void * __nullable CMP_OWNED)nextDrawable {
    id <CAMetalDrawable> drawable = [_metalLayer nextDrawable];
    
    if (drawable) {
        void *ptr = (__bridge_retained void *)drawable;
        return ptr;
    } else {
        return NULL;
    }
}

- (void)releaseDrawable:(void * CMP_CONSUMED)drawablePtr {
    assert(drawablePtr != NULL);
    
    id <CAMetalDrawable> drawable __unused = (__bridge_transfer id <CAMetalDrawable>)drawablePtr;
    // drawable will be released by ARC
}

- (void * CMP_BORROWED)drawableTexture:(void * CMP_BORROWED)drawablePtr {
    assert(drawablePtr != NULL);
    
    id <CAMetalDrawable> drawable = (__bridge id <CAMetalDrawable>)drawablePtr;
    id <MTLTexture> texture = drawable.texture;    
    void *texturePtr = (__bridge void *)texture;
    return texturePtr;
}

- (void)presentDrawable:(void * CMP_CONSUMED)drawablePtr {
    assert(drawablePtr != NULL);
    
    id <CAMetalDrawable> drawable = (__bridge_transfer id <CAMetalDrawable>)drawablePtr;
    [drawable present];
}

- (void)scheduleDrawablePresentation:(void * CMP_CONSUMED)drawablePtr onCommandBuffer:(id <MTLCommandBuffer>)commandBuffer {
    assert(drawablePtr != NULL);
    
    id <CAMetalDrawable> drawable = (__bridge_transfer id <CAMetalDrawable>)drawablePtr;
    [commandBuffer presentDrawable:drawable];
}

@end
