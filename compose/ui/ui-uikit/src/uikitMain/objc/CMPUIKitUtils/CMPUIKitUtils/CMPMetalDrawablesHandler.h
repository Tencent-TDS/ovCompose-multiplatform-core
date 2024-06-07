/*
 * Copyright 2023 The Android Open Source Project
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

#import <Foundation/Foundation.h>
#import <QuartzCore/QuartzCore.h>
#import <Metal/Metal.h>

#import "CMPMacros.h"

NS_ASSUME_NONNULL_BEGIN

@interface CMPMetalDrawablesHandler : NSObject

- (instancetype)initWithMetalLayer:(CAMetalLayer *)metalLayer;

- (void * CMP_OWNED)nextDrawable;
- (void)releaseDrawable:(void * CMP_CONSUMED)drawablePtr;
- (void * CMP_BORROWED)drawableTexture:(void * CMP_BORROWED)drawablePtr;
- (void)presentDrawable:(void * CMP_CONSUMED)drawablePtr;
- (void)scheduleDrawablePresentation:(void * CMP_CONSUMED)drawablePtr onCommandBuffer:(id <MTLCommandBuffer>)commandBuffer;


@end

NS_ASSUME_NONNULL_END
