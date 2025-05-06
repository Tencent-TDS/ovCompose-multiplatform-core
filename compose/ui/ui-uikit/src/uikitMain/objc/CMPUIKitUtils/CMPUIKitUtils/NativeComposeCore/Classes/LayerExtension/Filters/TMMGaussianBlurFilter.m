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

#import "TMMGaussianBlurFilter.h"

static NSString *const gCIGaussianBlurFilterName = @"CIGaussianBlur";
static CGFloat const gBlurRadiusMinimumDiff = 0.01;

@interface TMMGaussianBlurFilter ()

/// blur filter
@property (nonatomic, strong) CIFilter *blurFilter;

/// 输出的图像
@property (nonatomic, strong) UIImage *outputImage;

/// 图像缓存
@property (nonatomic, assign) CGImageRef imageRefCache;

/// CIContext
@property (nonatomic, strong) CIContext *ciContext;

@end

@implementation TMMGaussianBlurFilter

#pragma mark - public
- (void)setBlurRadius:(CGFloat)blurRadius {
    CGFloat diff = ABS(_blurRadius - blurRadius);
    _blurRadius = blurRadius;
    if (diff > gBlurRadiusMinimumDiff) {
        _imageRefCache = NULL;
        _outputImage = nil;
        [self.blurFilter setValue:@(blurRadius) forKey:kCIInputRadiusKey];
    }
}

- (CIFilter *)filter {
    return _blurFilter;
}

- (UIImage *)filterImageWithImageRef:(CGImageRef)imageRef {
    if (self.imageRefCache != imageRef && imageRef != NULL) {
        self.imageRefCache = imageRef;

        CIImage *ciImage = [CIImage imageWithCGImage:imageRef];
        [self.blurFilter setValue:ciImage forKey:kCIInputImageKey];

        // 获取处理后的图像
        CIImage *outputCIImage = [self.blurFilter outputImage];
        size_t width = CGImageGetWidth(imageRef);
        size_t height = CGImageGetHeight(imageRef);
        CGRect rect = CGRectMake(0, 0, width, height);
        CGImageRef processedImageRef = [self.ciContext createCGImage:outputCIImage fromRect:rect];
        if (processedImageRef) {
            UIImage *image = [UIImage imageWithCGImage:processedImageRef];
            CFRelease(processedImageRef);
            self.outputImage = image;
        } else {
            // 采用原图兜底
            self.outputImage = [UIImage imageWithCGImage:imageRef];
        }
    }
    return self.outputImage;
}

#pragma mark - getter
- (CIFilter *)blurFilter {
    if (_blurFilter) {
        return _blurFilter;
    }
    _blurFilter = [CIFilter filterWithName:gCIGaussianBlurFilterName];
    return _blurFilter;
}

- (CIContext *)ciContext {
    if (_ciContext) {
        return _ciContext;
    }
    _ciContext = [CIContext contextWithOptions:nil];
    return _ciContext;
}

@end
