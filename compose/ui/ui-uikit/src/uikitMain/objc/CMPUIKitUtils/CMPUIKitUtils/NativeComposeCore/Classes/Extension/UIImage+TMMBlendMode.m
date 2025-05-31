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

@implementation UIImage (TMMBlendMode)

- (UIImage *)tmmcompose_imageWithTintColor:(UIColor *)tintColor blendMode:(TMMNativeDrawBlendMode)blendMode {
    UIGraphicsBeginImageContextWithOptions(self.size, NO, self.scale);

    CGContextRef ctx = UIGraphicsGetCurrentContext();
    CGRect area = CGRectMake(0, 0, self.size.width, self.size.height);
    // Flip the image
    CGContextScaleCTM(ctx, 1, -1);
    CGContextTranslateCTM(ctx, 0, -area.size.height);
    CGContextDrawImage(ctx, area, self.CGImage);
    CGContextSaveGState(ctx);

    CGContextClipToMask(ctx, area, self.CGImage);
    CGContextSetFillColorWithColor(ctx, tintColor.CGColor);
    CGBlendMode cgBlendMode = CGBlendModeFromNativeDrawBlendMode(blendMode);
    CGContextSetBlendMode(ctx, cgBlendMode);

    CGContextFillRect(ctx, area);
    CGContextRestoreGState(ctx);
    UIImage *newImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return newImage;
}

- (NSData *)tmm_decodedData {
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
    CGBitmapInfo bitmapInfo = kCGImageAlphaPremultipliedLast;

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

@end
