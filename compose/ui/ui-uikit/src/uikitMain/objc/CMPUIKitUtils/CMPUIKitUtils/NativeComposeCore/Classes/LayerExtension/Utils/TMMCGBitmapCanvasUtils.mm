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

#import "TMMCGBitmapCanvasUtils.h"
#import "TMMUIKitPictureRecorder.h"
#import "TMMCGBitmapCanvas.h"

using namespace TMM;

#pragma mark - private
CGBitmapCanvas::CGBitmapCanvas()
    : contextPtr(0),
      cgBitmapPtr(0),
      width(0),
      height(0),
      rowBytes(0),
      cacheKey(0),
      hasBrush(false) {
}

CGBitmapCanvas::~CGBitmapCanvas() {
    if (contextPtr != 0) {
        CGContextRelease((CGContextRef)contextPtr);
        contextPtr = 0;
    }
}

void *TMMCreateCGBitmapCanvas(int cacheKey, int width, int height, BOOL hasBrush) {
    CGBitmapCanvas *canvas = new CGBitmapCanvas();
    if (width <= 0 || height <= 0) return canvas;
    CGContextRef context = NULL;
    if (hasBrush) {
         // 【彩色路径】
        static CGColorSpaceRef colorSpace = NULL;
        static dispatch_once_t onceToken;
        dispatch_once(&onceToken, ^{
            colorSpace = CGColorSpaceCreateWithName(kCGColorSpaceSRGB);
        });
        CGBitmapInfo bitmapInfo = (uint32_t)kCGImageAlphaPremultipliedFirst | (uint32_t)kCGBitmapByteOrder32Little;
        context = CGBitmapContextCreate(NULL, width, height, 8, 0, colorSpace, bitmapInfo);
     } else {
         // 【灰度路径】
        context = CGBitmapContextCreate(NULL, width, height, 8, 0, NULL, (CGBitmapInfo)kCGImageAlphaOnly);
     }
    if (context) {
        canvas->contextPtr = (intptr_t)context;
        canvas->cgBitmapPtr = (intptr_t)CGBitmapContextGetData(context);
        canvas->width = (int)CGBitmapContextGetWidth(context);
        canvas->height = (int)CGBitmapContextGetHeight(context);
        canvas->rowBytes = (int)CGBitmapContextGetBytesPerRow(context);
        canvas->hasBrush = hasBrush;
    }
    return canvas;
}

void TMMReleaseCGBitmapCanvas(void *canvasPtr) {
    if (canvasPtr != NULL) {
        CGBitmapCanvas *canvas = (CGBitmapCanvas *)canvasPtr;
        delete canvas;
    }
}

void *TMMCGBitmapCanvasGetPixelsPtr(void *canvasPtr) {
    if (canvasPtr == NULL) {
        return NULL;
    }
    CGBitmapCanvas *canvas = (CGBitmapCanvas *)canvasPtr;
    return (void *)canvas->cgBitmapPtr;
}

int TMMCGBitmapCanvasGetRowBytes(void *canvasPtr) {
    if (canvasPtr == NULL) {
        return 0;
    }
    CGBitmapCanvas *canvas = (CGBitmapCanvas *)canvasPtr;
    return canvas->rowBytes;
}

UIImage *TMMCGBitmapCanvasGetImage(void *canvasPtr, int colorArgb) {
    if (canvasPtr == NULL) {
        return NULL;
    }
    CGBitmapCanvas *canvas = (CGBitmapCanvas *)canvasPtr;
    CGContextRef context = (CGContextRef)canvas->contextPtr;
    if (!context) return NULL;
    CGImageRef cgImage = CGBitmapContextCreateImage(context);
    if (!cgImage) return NULL;
    UIImage *finalImage;
    if (canvas->hasBrush) {
        finalImage = [UIImage imageWithCGImage:cgImage];
    } else {
        finalImage = CreateImageFromAlphaMask(cgImage, colorArgb, canvas->width / PictureRecorder::density, canvas->height / PictureRecorder::density);
    }
    CGImageRelease(cgImage);
    return finalImage;
}

UIImage *CreateImageFromAlphaMask(CGImageRef maskImage, int colorARGB, int width, int height) {
    if (!maskImage) return nil;
    
    CGFloat red   = ((colorARGB >> 16) & 0xFF) / 255.0;
    CGFloat green = ((colorARGB >> 8)  & 0xFF) / 255.0;
    CGFloat blue  = (colorARGB & 0xFF) / 255.0;
    CGFloat alpha = ((colorARGB >> 24) & 0xFF) / 255.0;
    if (width <= 0 && height <= 0) return nil;
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(width, height), NO, 0.0);
    CGContextRef renderContext = UIGraphicsGetCurrentContext();
    if (!renderContext) return nil;

    CGContextTranslateCTM(renderContext, 0, height);
    CGContextScaleCTM(renderContext, 1.0, -1.0);

    CGContextClipToMask(renderContext, CGRectMake(0, 0, width, height), maskImage);

    CGContextSetRGBFillColor(renderContext, red, green, blue, alpha);
    CGContextFillRect(renderContext, CGRectMake(0, 0, width, height));

    UIImage *coloredImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();

    return coloredImage;
}
