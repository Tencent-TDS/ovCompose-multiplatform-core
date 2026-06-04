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

#import "UIImage+TMMBlendMode.h"
#import <ImageIO/ImageIO.h>
#import <CoreImage/CoreImage.h>
#import <Accelerate/Accelerate.h>
#import <CoreText/CoreText.h>

static NSString *const gCIColorMatrixKey = @"CIColorMatrix";
static NSString *const gCIFilterInputRVectorKey = @"inputRVector";
static NSString *const gCIFilterInputBVectorKey = @"inputBVector";
static NSString *const gCIFilterInputGVectorKey = @"inputGVector";
static NSString *const gCIFilterInputAVectorKey = @"inputAVector";
static NSString *const gCIFilterInputBiasVectorKey = @"inputBiasVector";

NS_INLINE CGBlendMode CGBlendModeFromNativeDrawBlendMode(TMMNativeDrawBlendMode blendMode) {
    switch (blendMode) {
        case TMMNativeDrawBlendModeClear:
            return kCGBlendModeClear;
        case TMMNativeDrawBlendModeSrc:
            return kCGBlendModeCopy;
        case TMMNativeDrawBlendModeDst:
            return kCGBlendModeCopy;
        case TMMNativeDrawBlendModeSrcOver:
            return kCGBlendModeCopy;
        case TMMNativeDrawBlendModeDstOver:
            return kCGBlendModeDestinationOver;
        case TMMNativeDrawBlendModeSrcIn:
            return kCGBlendModeSourceIn;
        case TMMNativeDrawBlendModeDstIn:
            return kCGBlendModeDestinationIn;
        case TMMNativeDrawBlendModeSrcOut:
            return kCGBlendModeSourceOut;
        case TMMNativeDrawBlendModeSrcAtop:
            return kCGBlendModeSourceAtop;
        case TMMNativeDrawBlendModeDstAtop:
            return kCGBlendModeDestinationAtop;
        case TMMNativeDrawBlendModeXor:
            return kCGBlendModeXOR;
        case TMMNativeDrawBlendModeScreen:
            return kCGBlendModeScreen;
        case TMMNativeDrawBlendModeOverlay:
            return kCGBlendModeOverlay;
        case TMMNativeDrawBlendModeDarken:
            return kCGBlendModeDarken;
        case TMMNativeDrawBlendModeLighten:
            return kCGBlendModeLighten;
        case TMMNativeDrawBlendModeColorDodge:
            return kCGBlendModeColorDodge;
        case TMMNativeDrawBlendModeColorBurn:
            return kCGBlendModeColorBurn;
        case TMMNativeDrawBlendModeHardlight:
            return kCGBlendModeHardLight;
        case TMMNativeDrawBlendModeSoftlight:
            return kCGBlendModeSoftLight;
        case TMMNativeDrawBlendModeDifference:
            return kCGBlendModeDifference;
        case TMMNativeDrawBlendModeExclusion:
            return kCGBlendModeExclusion;
        case TMMNativeDrawBlendModeMultiply:
            return kCGBlendModeMultiply;
        case TMMNativeDrawBlendModeHue:
            return kCGBlendModeHue;
        case TMMNativeDrawBlendModeSaturation:
            return kCGBlendModeSaturation;
        case TMMNativeDrawBlendModeColor:
            return kCGBlendModeColor;
        case TMMNativeDrawBlendModeLuminosity:
            return kCGBlendModeLuminosity;
        default:
            return kCGBlendModeCopy;
    }
}

/// 释放 vImage 的 buffer
static void cleanupImageBuffer(void *userData, void *buf_data) {
    free(buf_data);
}

NS_INLINE CIContext *UIImageSharedCIContext(void) {
    static CIContext *ctx = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        ctx = [CIContext context];
    });
    return ctx;
}

@implementation UIImage (TMMBlendMode)

- (UIImage *)ovCompose_imageByTintColor:(UIColor *)tintColor blendMode:(TMMNativeDrawBlendMode)blendMode {
    const CGBlendMode cgBlendMode = CGBlendModeFromNativeDrawBlendMode(blendMode);
    
    if (self.size.width < 1 || self.size.height < 1 || !self.CGImage) {
        return nil;
    }
    
    CGFloat saturation = 0.0f;
    [tintColor getHue:NULL saturation:&saturation brightness:NULL alpha:NULL];
    
    BOOL hasSaturation = fabs(saturation - 1.0) > __FLT_EPSILON__;
    
    vImage_Buffer effect = { 0 };
    vImage_Buffer scratch = { 0 };
    vImage_Buffer *input = NULL;
    vImage_Buffer *output = NULL;
    
    vImage_CGImageFormat format = {
        .bitsPerComponent = 8,
        .bitsPerPixel = 32,
        .colorSpace = NULL,
        .bitmapInfo = kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Little,
        .version = 0,
        .decode = NULL,
        .renderingIntent = kCGRenderingIntentDefault
    };
    
    vImage_Error err = vImageBuffer_InitWithCGImage(&effect, &format, NULL, self.CGImage, kvImagePrintDiagnosticsToConsole);
    if (err != kvImageNoError) {
        return nil;
    }
    err = vImageBuffer_Init(&scratch, effect.height, effect.width, format.bitsPerPixel, kvImageNoFlags);
    if (err != kvImageNoError) {
        return nil;
    }
    
    input = &effect;
    output = &scratch;
    
    if (hasSaturation) {
        const CGFloat s = saturation;
        CGFloat matrixFloat[] = {
            0.0722 + 0.9278 * s,  0.0722 - 0.0722 * s,  0.0722 - 0.0722 * s,  0,
            0.7152 - 0.7152 * s,  0.7152 + 0.2848 * s,  0.7152 - 0.7152 * s,  0,
            0.2126 - 0.2126 * s,  0.2126 - 0.2126 * s,  0.2126 + 0.7873 * s,  0,
            0,                    0,                    0,                    1,
        };
        const int32_t divisor = 256;
        const NSUInteger matrixSize = sizeof(matrixFloat) / sizeof(matrixFloat[0]);
        int16_t matrix[matrixSize];
        for (NSUInteger i = 0; i < matrixSize; ++i) {
            matrix[i] = (int16_t)roundf(matrixFloat[i] * divisor);
        }
        vImageMatrixMultiply_ARGB8888(input, output, matrix, divisor, NULL, NULL, kvImageNoFlags);
        vImage_Buffer *template = input;
        input = output;
        output = template;
    }
    
    CGImageRef intermediateImage = vImageCreateCGImageFromBuffer(input, &format, &cleanupImageBuffer, NULL, kvImageNoAllocate, NULL);
    if (intermediateImage == NULL) {
        intermediateImage = vImageCreateCGImageFromBuffer(input, &format, NULL, NULL, kvImageNoFlags, NULL);
        free(input->data);
    }
    free(output->data);
    UIImage *outputImage = [self ovCompose_mergeImageRef:intermediateImage
                                               tintColor:tintColor
                                               blendMode:cgBlendMode];
    CGImageRelease(intermediateImage);
    return outputImage;
}


- (UIImage *)ovCompose_mergeImageRef:(CGImageRef)effectCGImage
                           tintColor:(UIColor *)tintColor
                           blendMode:(CGBlendMode)blendMode {
    const CGSize size = self.size;
    const CGFloat scale = self.scale;
    const CGRect rect = CGRectMake(0, 0, size.width, size.height);
    
    UIGraphicsBeginImageContextWithOptions(size, NO, scale);
    CGContextRef context = UIGraphicsGetCurrentContext();
    CGContextScaleCTM(context, 1.0, -1.0);
    CGContextTranslateCTM(context, 0, -size.height);
    CGContextDrawImage(context, rect, effectCGImage);
    
    const BOOL hasTint = tintColor != nil && CGColorGetAlpha(tintColor.CGColor) > __FLT_EPSILON__;
    if (hasTint) {
        CGContextSaveGState(context);
        CGContextSetBlendMode(context, blendMode);
        CGContextSetFillColorWithColor(context, tintColor.CGColor);
        CGContextFillRect(context, rect);
        CGContextRestoreGState(context);
    }
    UIImage *outputImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return outputImage;
}

- (NSData *)ovCompose_decodedData {
    CGImageRef cgImage = self.CGImage;
    if (!cgImage) {
        return nil;
    }

    size_t width = CGImageGetWidth(cgImage);
    size_t height = CGImageGetHeight(cgImage);
    size_t bitsPerComponent = 8;
    size_t bytesPerPixel = 4;
    size_t bytesPerRow = width * bytesPerPixel;
    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    CGBitmapInfo bitmapInfo = (CGBitmapInfo)kCGImageAlphaPremultipliedLast;

    CGContextRef context = CGBitmapContextCreate(NULL,
                                                 width,
                                                 height,
                                                 bitsPerComponent,
                                                 bytesPerRow,
                                                 colorSpace,
                                                 bitmapInfo);
    if (!context) {
        CGColorSpaceRelease(colorSpace);
        return nil;
    }

    CGContextDrawImage(context, CGRectMake(0, 0, width, height), cgImage);

    void *data = CGBitmapContextGetData(context);
    if (!data) {
        CGContextRelease(context);
        CGColorSpaceRelease(colorSpace);
        return nil;
    }

    NSData *decodedData = [NSData dataWithBytes:data length:bytesPerRow * height];

    CGContextRelease(context);
    CGColorSpaceRelease(colorSpace);
    return decodedData;
}

- (nullable UIImage *)ovCompose_imageByColorMatrix:(const CGFloat *)colorMatrix {
    UIImage *origin = [UIImage imageWithCGImage:self.CGImage];
    CIImage *inImage = [CIImage imageWithCGImage:self.CGImage];
    CIFilter *filter = [CIFilter filterWithName:gCIColorMatrixKey
                            withInputParameters:@{
                                kCIInputImageKey : inImage,
                                gCIFilterInputRVectorKey : [CIVector vectorWithValues:colorMatrix count:4],
                                gCIFilterInputGVectorKey : [CIVector vectorWithValues:colorMatrix + 5 count:4],
                                gCIFilterInputBVectorKey : [CIVector vectorWithValues:colorMatrix + 10 count:4],
                                gCIFilterInputAVectorKey : [CIVector vectorWithValues:colorMatrix + 15 count:4],
                                gCIFilterInputBiasVectorKey : [CIVector vectorWithX:colorMatrix[4] / 255 Y:colorMatrix[9] / 255 Z:colorMatrix[14] / 255 W:colorMatrix[19] / 255]
                            }];

    CIImage *output = [filter outputImage];
    if (output != nil) {
        CIContext *context = UIImageSharedCIContext();
        CGRect outputExtent = output.extent;
        CGImageRef cgImage = [context createCGImage:output fromRect:outputExtent];
        if (cgImage != NULL) {
            UIImage *result = [UIImage imageWithCGImage:cgImage];
            CGImageRelease(cgImage);
            return result;
        }
    }
    return origin;
}

@end
