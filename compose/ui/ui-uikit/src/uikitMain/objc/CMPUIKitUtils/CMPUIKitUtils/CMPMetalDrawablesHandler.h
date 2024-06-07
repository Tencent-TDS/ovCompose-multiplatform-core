//
//  CMPMetalDrawablesHandler.h
//  CMPUIKitUtils
//
//  Created by Ilia.Semenov on 07/06/2024.
//

#import <Foundation/Foundation.h>
#import <QuartzCore/QuartzCore.h>
#import <Metal/Metal.h>

#import "CMPMacros.h"

NS_ASSUME_NONNULL_BEGIN

@interface CMPMetalDrawablesHandler : NSObject

- (instancetype)initWithMetalLayer:(CAMetalLayer *)metalLayer;

- (void * __nullable CMP_OWNED)nextDrawable;
- (void)releaseDrawable:(void * CMP_CONSUMED)drawablePtr;
- (void * CMP_BORROWED)drawableTexture:(void * CMP_BORROWED)drawablePtr;
- (void)presentDrawable:(void * CMP_CONSUMED)drawablePtr;
- (void)scheduleDrawablePresentation:(void * CMP_CONSUMED)drawablePtr onCommandBuffer:(id <MTLCommandBuffer>)commandBuffer;


@end

NS_ASSUME_NONNULL_END
