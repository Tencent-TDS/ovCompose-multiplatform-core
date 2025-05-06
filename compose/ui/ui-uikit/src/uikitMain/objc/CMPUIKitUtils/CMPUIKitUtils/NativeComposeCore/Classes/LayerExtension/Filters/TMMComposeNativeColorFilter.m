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

#import "TMMComposeNativeColorFilter.h"
#import "TMMDrawUtils.h"

@interface TMMComposeNativeColorFilter ()

- (nullable UIImage *)filterBlendMode:(CGImageRef)imageRef;

- (nullable UIImage *)filterMatrixMode:(CGImageRef)imageRef;

- (nullable UIImage *)filterMatrixModeFor13AndAbove:(CGImageRef)imageRef;

- (nullable UIImage *)filterMatrixModeFor12AndBelow:(CGImageRef)imageRef;

- (NSString *)filterNameForOpenGLBlendMode:(TMMNativeDrawBlendMode)blendMode;

- (CIImage *)getColorImage:(CGSize)size;

- (CIImage *)blendImage:(CIImage *)srcImage dstImage:(CIImage *)dstImage;

+ (unsigned char *)requestImagePixelData:(UIImage *)inImage;

+ (CGContextRef)createRGBABitmapContext:(CGImageRef)inImage;

+ (void)printMatrix:(const CGFloat *)matrix size:(int)size;

+ (void)changeRGBA:(int *)red green:(int *)green blue:(int *)blue alpha:(int *)alpha withFloatArray:(const CGFloat *)f;

+ (CGSize)sizeInPoints:(CGSize)size;

@end

@implementation TMMComposeNativeColorFilter

- (instancetype)init {
    self = [super init];
    if (self) {
        // 初始化颜色矩阵
        static CGFloat initialMatrix[20] = {
            1.f, 0.f, 0.f, 0.f, 0.f, // Red
            0.f, 1.f, 0.f, 0.f, 0.f, // Green
            0.f, 0.f, 1.f, 0.f, 0.f, // Blue
            0.f, 0.f, 0.f, 1.f, 0.f  // Alpha
        };

        // 不能随意改动大小，需要与compose框架匹配
        _matrixSize = sizeof(initialMatrix);
        unsigned long byteSize = _matrixSize * sizeof(CGFloat);
        _matrix = (CGFloat *)malloc(byteSize);
        // 检查内存分配是否成功
        if (_matrix == NULL) {
            NSLog(@"Memory allocation failed for matrix");
            return nil; // 返回nil表示初始化失败
        }

        // 复制初始化值
        memcpy(_matrix, initialMatrix, byteSize);
    }
    return self;
}

- (void)dealloc {
    if (_matrix != NULL) {
        free(_matrix);
        _matrix = NULL;
    }
}

- (nullable UIImage *)filterImageWithImageRef:(CGImageRef)imageRef {
    switch (_type) {
        case TMMNativeColorFilterTypeBlend:
            return [self filterBlendMode:imageRef];
        case TMMNativeColorFilterTypeMatrix:
        case TMMNativeColorFilterTypeLighting:
            return [self filterMatrixMode:imageRef];
        default:
            return nil;
    }
}

- (nullable UIImage *)filterMatrixMode:(CGImageRef)imageRef {
    if (@available(iOS 13.0, *)) {
        // iOS 13+支持matrix，低于这个版本的返回原图
        return [self filterMatrixModeFor13AndAbove:imageRef];
    } else {
        return [self filterMatrixModeFor12AndBelow:imageRef];
    }
}

- (nullable UIImage *)filterMatrixModeFor13AndAbove:(CGImageRef)imageRef {
    UIImage *origin = [UIImage imageWithCGImage:imageRef];
    CIImage *inImage = [CIImage imageWithCGImage:imageRef];

    CIVector *biasVector = [CIVector vectorWithX:_matrix[4] / 255 Y:_matrix[9] / 255 Z:_matrix[14] / 255 W:_matrix[19] / 255];
    CIFilter *filter = [CIFilter filterWithName:@"CIColorMatrix"
                            withInputParameters:@{
                                kCIInputImageKey : inImage,
                                @"inputRVector" : [CIVector vectorWithValues:_matrix count:4],
                                @"inputGVector" : [CIVector vectorWithValues:_matrix + 5 count:4],
                                @"inputBVector" : [CIVector vectorWithValues:_matrix + 10 count:4],
                                @"inputAVector" : [CIVector vectorWithValues:_matrix + 15 count:4],
                                @"inputBiasVector" : biasVector
                            }];

    CIImage *output = [filter outputImage];
    if (output != nil) {
        CIContext *context = [CIContext context];
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

- (nullable UIImage *)filterMatrixModeFor12AndBelow:(CGImageRef)imageRef {
    UIImage *inImage = [UIImage imageWithCGImage:imageRef];
    unsigned char *imgPixel = [TMMComposeNativeColorFilter requestImagePixelData:inImage];
    if (imgPixel == NULL) {
        NSLog(@"filterMatrixModeFor12AndBelow imgPixel nil, return origin");
        return inImage;
    }

    GLuint w = (GLuint)CGImageGetWidth(imageRef);
    GLuint h = (GLuint)CGImageGetHeight(imageRef);
    int wOff = 0;
    int pixOff = 0;
    // 双层循环按照长宽的像素个数迭代每个像素点
    for (GLuint y = 0; y < h; y++) {
        pixOff = wOff;

        for (GLuint x = 0; x < w; x++) {
            int red = (unsigned char)imgPixel[pixOff];
            int green = (unsigned char)imgPixel[pixOff + 1];
            int blue = (unsigned char)imgPixel[pixOff + 2];
            int alpha = (unsigned char)imgPixel[pixOff + 3];
            [TMMComposeNativeColorFilter changeRGBA:&red green:&green blue:&blue alpha:&alpha withFloatArray:_matrix];

            // 回写数据
            imgPixel[pixOff] = red;
            imgPixel[pixOff + 1] = green;
            imgPixel[pixOff + 2] = blue;
            imgPixel[pixOff + 3] = alpha;
            // 将数组的索引指向下四个元素
            pixOff += 4;
        }

        wOff += w * 4;
    }

    NSInteger dataLength = w * h * 4;

    // 下面的代码创建要输出的图像的相关参数
    CGDataProviderRef provider = CGDataProviderCreateWithData(NULL, imgPixel, dataLength, NULL);
    if (provider == nil) {
        NSLog(@"filterMatrixModeFor12AndBelow provider nil");
        free(imgPixel);
        return inImage;
    }

    int bitsPerComponent = 8;
    int bitsPerPixel = 32;
    int bytesPerRow = 4 * w;
    CGColorSpaceRef colorSpaceRef = CGColorSpaceCreateDeviceRGB();
    CGBitmapInfo bitmapInfo = kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big;

    // 创建要输出的图像
    CGImageRef imageRefOut
        = CGImageCreate(w, h, bitsPerComponent, bitsPerPixel, bytesPerRow, colorSpaceRef, bitmapInfo, provider, NULL, NO, kCGRenderingIntentDefault);

    UIImage *outImage = nil;
    if (imageRefOut) {
        outImage = [UIImage imageWithCGImage:imageRefOut];
        CFRelease(imageRefOut);
    } else {
        NSLog(@"filterMatrixModeFor12AndBelow imageRefOut nil");
        outImage = inImage;
    }
    CGColorSpaceRelease(colorSpaceRef);
    CGDataProviderRelease(provider);
    free(imgPixel);
    return outImage;
}

- (nullable UIImage *)filterBlendMode:(CGImageRef)imageRef {
    UIImage *origin = [UIImage imageWithCGImage:imageRef];
    if (_blendMode == TMMNativeDrawBlendModeDst ||
        // TODO 补全Clear模式
        _blendMode == TMMNativeDrawBlendModeClear) {
        NSLog(@"filterBlendMode origin");
        return origin;
    }

    CIImage *srcImage = [self getColorImage:origin.size];
    if (_blendMode == TMMNativeDrawBlendModeSrc) {
        return [UIImage imageWithCGImage:srcImage.CGImage];
    }

    // 混合模式中的DEST
    CIImage *dstImage = [CIImage imageWithCGImage:imageRef];

    // 获取输出图像
    CIImage *outputImage = [self blendImage:srcImage dstImage:dstImage];
    if (outputImage == nil) {
        NSLog(@"filterBlendMode outputImage nil, src=%p, dst=%p", srcImage, dstImage);
        return origin;
    }

    // 将输出CIImage转换回CGImage
    CIContext *ciContext = [CIContext contextWithOptions:nil];
    CGImageRef processedImageRef = [ciContext createCGImage:outputImage fromRect:[outputImage extent]];
    if (processedImageRef) {
        UIImage *image = [UIImage imageWithCGImage:processedImageRef];
        CFRelease(processedImageRef);
        return image;
    } else {
        NSLog(@"filterBlendMode last nil");
        // 采用原图兜底
        return origin;
    }
}

- (CIImage *)blendImage:(CIImage *)srcImage dstImage:(CIImage *)dstImage {
    // 创建一个滤镜
    CIFilter *blendFilter = [CIFilter filterWithName:[self filterNameForOpenGLBlendMode:_blendMode]];
    if (blendFilter == nil) {
        NSLog(@"blendImage filter nil");
        return nil;
    }
    [blendFilter setValue:dstImage forKey:kCIInputBackgroundImageKey];
    [blendFilter setValue:srcImage forKey:kCIInputImageKey];
    // 获取输出图像
    return blendFilter.outputImage;
}

- (CIImage *)getColorImage:(CGSize)size {
    CGSize pointSize = [TMMComposeNativeColorFilter sizeInPoints:size];
    // 创建一个CIColorMatrix滤镜，以应用绿色滤镜
    // 创建一个 CGBitmapContext
    UIGraphicsBeginImageContextWithOptions(pointSize, YES, 0);
    CGContextRef context = UIGraphicsGetCurrentContext();
    // 设置填充颜色
    CGContextSetFillColorWithColor(context, TMMComposeCoreMakeUIColorFromULong(_colorValue).CGColor);
    // 填充矩形
    CGContextFillRect(context, CGRectMake(0, 0, pointSize.width, pointSize.height));
    // 从位图上下文创建一个 CGImageRef
    CGImageRef colorImageRef = CGBitmapContextCreateImage(context);

    // 结束图形上下文
    UIGraphicsEndImageContext();
    return [CIImage imageWithCGImage:colorImageRef];
}

- (void)setColorFilterInfo:(TMMComposeNativeColorFilter *_Nonnull)colorFilter {
    _type = colorFilter.type;
    _blendMode = colorFilter.blendMode;
    _colorValue = colorFilter.colorValue;

    // 复制初始化值
    memcpy(_matrix, colorFilter.matrix, _matrixSize * sizeof(CGFloat));
}

- (NSString *)filterNameForOpenGLBlendMode:(TMMNativeDrawBlendMode)blendMode {
    NSString *name = nil;
    switch (blendMode) {
        case TMMNativeDrawBlendModeClear:
        case TMMNativeDrawBlendModeSrc:
        case TMMNativeDrawBlendModeDst:
            name = nil;
            break;
        case TMMNativeDrawBlendModeSrcOver:
            name = @"CISourceOverCompositing";
            break;
        case TMMNativeDrawBlendModeDstOver:
            name = @"CIDestinationOverCompositing";
            break;
        case TMMNativeDrawBlendModeSrcIn:
            name = @"CISourceInCompositing";
            break;
        case TMMNativeDrawBlendModeDstIn:
            name = @"CIDestinationInCompositing";
            break;
        case TMMNativeDrawBlendModeSrcOut:
            name = @"CISourceOutCompositing";
            break;
        case TMMNativeDrawBlendModeDstOut:
            name = @"CIDestinationOutCompositing";
            break;
        case TMMNativeDrawBlendModeDstAtop:
            name = @"CIDestinationAtopCompositing";
            break;
        case TMMNativeDrawBlendModeSrcAtop:
            name = @"CISourceAtopCompositing";
            break;
        case TMMNativeDrawBlendModeXor:
            name = @"CIXorCompositing";
            break;
        case TMMNativeDrawBlendModePlus:
            name = @"CIAdditionCompositing";
            break;
        case TMMNativeDrawBlendModeModulate:
            // TODO 发现结果颜色有一定差异，需要后续继续研究一下。
            name = @"CIMultiplyCompositing";
            break;
        case TMMNativeDrawBlendModeScreen:
            name = @"CIScreenBlendMode";
            break;
        case TMMNativeDrawBlendModeOverlay:
            name = @"CIOverlayBlendMode";
            break;
        case TMMNativeDrawBlendModeDarken:
            name = @"CIDarkenBlendMode";
            break;
        case TMMNativeDrawBlendModeLighten:
            name = @"CILightenBlendMode";
            break;
        case TMMNativeDrawBlendModeColorDodge:
            name = @"CIColorDodgeBlendMode";
            break;
        case TMMNativeDrawBlendModeColorBurn:
            name = @"CIColorBurnBlendMode";
            break;
        case TMMNativeDrawBlendModeHardlight:
            name = @"CIHardLightBlendMode";
            break;
        case TMMNativeDrawBlendModeSoftlight:
            name = @"CISoftLightBlendMode";
            break;
        case TMMNativeDrawBlendModeDifference:
            name = @"CIDifferenceBlendMode";
            break;
        case TMMNativeDrawBlendModeExclusion:
            name = @"CIExclusionBlendMode";
            break;
        case TMMNativeDrawBlendModeMultiply:
            name = @"CIMultiplyBlendMode";
            break;
        case TMMNativeDrawBlendModeHue:
            name = @"CIHueBlendMode";
            break;
        case TMMNativeDrawBlendModeSaturation:
            name = @"CISaturationBlendMode";
            break;
        case TMMNativeDrawBlendModeColor:
            name = @"CIColorBlendMode";
            break;
        case TMMNativeDrawBlendModeLuminosity:
            name = @"CILuminosityBlendMode";
            break;
        case TMMNativeDrawBlendModeUnknown:
        default:
            name = nil;
            break;
    }
    return name;
}

// 返回一个指针，该指针指向一个数组，数组中的每四个元素都是图像上的一个像素点的RGBA的数值(0-255)，用无符号的char是因为它正好的取值范围就是0-255
+ (unsigned char *)requestImagePixelData:(UIImage *)inImage {
    CGImageRef imageRef = [inImage CGImage];
    CGSize size = [inImage size];

    CGContextRef context = [self createRGBABitmapContext:imageRef]; // 使用上面的函数创建上下文

    CGRect rect = CGRectMake(0, 0, size.width, size.height);

    // 将目标图像绘制到指定的上下文，实际为上下文内的bitmapData。
    CGContextDrawImage(context, rect, imageRef);
    unsigned char *data = CGBitmapContextGetData(context);

    CGContextRelease(context);
    return data;
}

// 返回一个使用RGBA通道的位图上下文
+ (CGContextRef)createRGBABitmapContext:(CGImageRef)inImage {
    CGContextRef context = NULL;
    CGColorSpaceRef colorSpace;
    // 内存空间的指针，该内存空间的大小等于图像使用RGB通道所占用的字节数。
    void *bitmapData;
    unsigned long bitmapByteCount;
    unsigned long bitmapBytesPerRow;

    // 获取横向的像素点的个数
    size_t pixelsWide = CGImageGetWidth(inImage);
    // 纵向
    size_t pixelsHigh = CGImageGetHeight(inImage);

    // 每一行的像素点占用的字节数，每个像素点的ARGB四个通道各占8个bit(0-255)的空间
    bitmapBytesPerRow = (pixelsWide * 4);
    // 计算整张图占用的字节数
    bitmapByteCount = (bitmapBytesPerRow * pixelsHigh);

    // 创建依赖于设备的RGB通道
    colorSpace = CGColorSpaceCreateDeviceRGB();

    // 分配足够容纳图片字节数的内存空间
    bitmapData = malloc(bitmapByteCount);

    // 创建CoreGraphic的图形上下文，该上下文描述了bitmaData指向的内存空间需要绘制的图像的一些绘制参数
    if (bitmapData != NULL) {
        context = CGBitmapContextCreate(bitmapData, pixelsWide, pixelsHigh, 8, bitmapBytesPerRow, colorSpace, kCGImageAlphaPremultipliedLast);
    } else {
        NSLog(@"createRGBABitmapContext malloc null");
    }

    // Core Foundation中通过含有Create、Alloc的方法名字创建的指针，需要使用CFRelease()函数释放
    CGColorSpaceRelease(colorSpace);

    return context;
}

+ (void)printMatrix:(const CGFloat *)matrix size:(int)size {
    // 初始化可变字符串
    NSMutableString *outputString = [NSMutableString string];
    // 遍历数组并将值添加到字符串中
    int columns = 5;
    for (int i = 0; i < size; i++) {
        [outputString appendFormat:@"%f ", matrix[i]];

        // 在每5个元素后添加换行符
        if ((i + 1) % columns == 0) {
            [outputString appendString:@"\n"];
        }
    }
    // 输出字符串
    NSLog(@"matrix, size = %d, content=%@", size, outputString);
}

// 修改RGB的值
+ (void)changeRGBA:(int *)red green:(int *)green blue:(int *)blue alpha:(int *)alpha withFloatArray:(const CGFloat *)f {
    int values[4] = { *red, *green, *blue, *alpha };
    int result[4] = { 0, 0, 0, 0 };

    for (int i = 0; i < 4; i++) {
        result[i] = f[i * 5] * values[0] + f[i * 5 + 1] * values[1] + f[i * 5 + 2] * values[2] + f[i * 5 + 3] * values[3] + f[i * 5 + 4];
        // 限制在0到255之间
        result[i] = MIN(MAX(result[i], 0), 255);
    }

    *red = result[0];
    *green = result[1];
    *blue = result[2];
    *alpha = result[3];
}

+ (CGSize)sizeInPoints:(CGSize)size {
    const CGFloat density = TMMComposeCoreDeviceDensity();
    return CGSizeMake(size.width / density, size.height / density);
}

- (NSUInteger)hash {
    static CGFloat matrix[20] = { 0 };
    const uint64_t hashMatrix = TMMFNVHash(_matrix, sizeof(matrix));
    const float floats[6] = { (float)_type, (float)_colorValue, (float)_blendMode, (float)_multiply, (float)_add, (float)hashMatrix };
    return TMMFNVHash(floats, sizeof(floats));
}

@end
