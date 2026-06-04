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

#import "TMMImageDisplayLayer.h"
#import "TMMComposeNativeColorFilter.h"
#import "TMMDrawUtils.h"
#import "TMMGaussianBlurFilter.h"
#import "TMMImageClipLayer.h"
#import "TMMNativeBaseLayer.h"
#import "UIImage+TMMBlendMode.h"
#import "TMMComposeInjectCommonServiceImpl.h"
#import "OVComposeExperimentalConfig.h"
#import "OVMPFastLogCAPI.h"

NS_INLINE BOOL CALayerShouldClipImage(CGImageRef image, const CGPoint &srcOffset, const CGSize &srcSize) {
    // 1. 如果起始点不是图片的左上角，则需要裁剪
    if (!CGPointEqualToPoint(srcOffset, CGPointZero)) {
        return YES;
    }
    // 2. srcSize 小于原图说明也需要裁剪
    return srcSize.width < CGImageGetWidth(image) || srcSize.height < CGImageGetHeight(image);
}

#pragma mark - TMMImageDisplayLayer
@interface TMMImageDisplayLayer ()

/// 图片裁剪的 layer
@property (nonatomic, strong) TMMImageClipLayer *imageClipLayer;

/// 对 TMMGaussianBlurFilter 的强引用，出现过 [TMMCanvasViewProxy cxx_destruct]的时候 [CIContext dealloc] 的时候出现 crash
@property (nonatomic, strong) TMMGaussianBlurFilter *blurFilter;

/// 图片设置版本号，用于解决异步任务竞态问题
@property (nonatomic, assign) NSUInteger imageVersion;

@end

@implementation TMMImageDisplayLayer

- (id<CAAction>)actionForKey:(NSString *)event {
    return nil;
}

- (void)renderImageWithNoBlendMode:(CGImageRef)imagePointer
                         srcOffset:(const CGPoint &)srcOffset
                           srcSize:(const CGSize &)srcSize
                         dstOffset:(const CGPoint &)dstOffset
                           dstSize:(const CGSize &)dstSize
                         tintColor:(UIColor *)tintColor
                           density:(CGFloat)density {
    intptr_t selfPtr = (intptr_t)self;
    // 先判断是否需要裁剪，如果需要则走特殊逻辑
    BOOL shouldClipImage = CALayerShouldClipImage(imagePointer, srcOffset, srcSize);
    if (!shouldClipImage) {
        [_imageClipLayer removeFromSuperlayer];
        ovmp_fastlog_log4(OVMP_FASTLOG_TAG_IMAGE,
                          OVMPFastLogPhaseImageOnLayerWillNoClipAndSetImageWithTintColor,
                          ovmp_get_current_trace_id(),
                          selfPtr,
                          (intptr_t)imagePointer);
        [self setImage:imagePointer tintColor:tintColor];
    } else {
        TMMImageClipLayer *clipLayer = self.imageClipLayer;
        clipLayer.experimentalConfig = self.experimentalConfig;
        if (clipLayer.superlayer != self) {
            [self addSublayer:clipLayer];
        }
        [clipLayer setImage:imagePointer srcOffset:srcOffset srcSize:srcSize dstOffset:dstOffset dstSize:dstSize density:density tintColor:tintColor];
    }
}

#pragma mark - public
- (void)setImage:(CGImageRef)imagePointer
       srcOffset:(CGPoint)srcOffset
         srcSize:(CGSize)srcSize
       dstOffset:(CGPoint)dstOffset
         dstSize:(CGSize)dstSize
     colorFilter:(TMMComposeNativeColorFilter *)colorFilter
      blurFilter:(TMMGaussianBlurFilter *)blurFilter
           paint:(TMMComposeNativePaint *)paint
         density:(CGFloat)density {
    
    intptr_t selfPtr = (intptr_t)self;
    int64_t taskId = ovmp_get_current_trace_id();
    ovmp_fastlog_log4(OVMP_FASTLOG_TAG_IMAGE,
                      OVMPFastLogPhaseImageOnLayerBeginSetImage,
                      taskId,
                      selfPtr,
                      (intptr_t)imagePointer);
    
    [self clearContents];
    
    if (ABS(paint.alpha - self.opacity) > FLT_EPSILON) {
        self.opacity = paint.alpha;
    }
    
    if (colorFilter.type == TMMNativeColorFilterTypeMatrix) {
        self.blurFilter = blurFilter;
        UIImage *image = [UIImage imageWithCGImage:imagePointer];
        dispatch_async(dispatch_get_global_queue(0, 0), ^{
            UIImage *renderImage = [image ovCompose_imageByColorMatrix:[colorFilter matrix]];
            ovmp_fastlog_log4(OVMP_FASTLOG_TAG_IMAGE,
                              OVMPFastLogPhaseImageOnLayerAsyncCreateImageByColorMatrix,
                              taskId,
                              selfPtr,
                              (intptr_t)renderImage);
            dispatch_async(dispatch_get_main_queue(), ^{
                ovmp_fastlog_log4(OVMP_FASTLOG_TAG_IMAGE,
                                  OVMPFastLogPhaseImageOnLayerAsyncSetImageByColorMatrix,
                                  taskId,
                                  selfPtr,
                                  (intptr_t)renderImage.CGImage);
                [self renderImageWithNoBlendMode:renderImage.CGImage
                                       srcOffset:srcOffset
                                         srcSize:srcSize
                                       dstOffset:dstOffset
                                         dstSize:dstSize
                                       tintColor:nil
                                         density:density];
            });
        });
        return;
    }
    
    // 最常见 case: colorFilter 和 colorFilter 均未设置的，直接使用原图展示
    if (colorFilter == nil && blurFilter == nil) {
        UIColor *tinitColor = nil;
        [self renderImageWithNoBlendMode:imagePointer
                               srcOffset:srcOffset
                                 srcSize:srcSize
                               dstOffset:dstOffset
                                 dstSize:dstSize
                               tintColor:tinitColor
                                 density:density];
        return;
    }

    // 第二常见 case: 一般都是 darkMode 切换会出现
    if (colorFilter.blendMode == TMMNativeDrawBlendModeSrcIn && blurFilter == nil) {
        UIColor *tintColor = TMMComposeCoreMakeUIColorFromULong(colorFilter.colorValue);
        [self renderImageWithNoBlendMode:imagePointer
                               srcOffset:srcOffset
                                 srcSize:srcSize
                               dstOffset:dstOffset
                                 dstSize:dstSize
                               tintColor:tintColor
                                 density:density];
        return;
    }

    // 最后处理其他 case 开启异步前需要用生成 UIImage 捕获一下 imagePointer，防止被回收导致 crash
    UIImage *originImage = [UIImage imageWithCGImage:imagePointer];
    self.blurFilter = blurFilter;
    dispatch_async(dispatch_get_global_queue(0, 0), ^{
        UIColor *tintColor = TMMComposeCoreMakeUIColorFromULong(colorFilter.colorValue);
        UIImage *finalImage = originImage;
        if (colorFilter && finalImage) {
            finalImage = [originImage ovCompose_imageByTintColor:tintColor blendMode:colorFilter.blendMode];
        }

        if (blurFilter && finalImage) {
            finalImage = [blurFilter filterImageWithImageRef:finalImage.CGImage];
        }
        if (!finalImage) {
            finalImage = originImage;
        }
        dispatch_async(dispatch_get_main_queue(), ^{
            [self renderImageWithNoBlendMode:finalImage.CGImage
                                   srcOffset:srcOffset
                                     srcSize:srcSize
                                   dstOffset:dstOffset
                                     dstSize:dstSize
                                   tintColor:nil
                                     density:density];
        });
    });
}

#pragma mark - getters
- (TMMImageClipLayer *)imageClipLayer {
    if (_imageClipLayer) {
        return _imageClipLayer;
    }
    _imageClipLayer = [TMMImageClipLayer layer];
    return _imageClipLayer;
}

- (NSDictionary *)lookin_customDebugInfos {
    return @{
        @"title" : @"Image",
    };
}

@end
