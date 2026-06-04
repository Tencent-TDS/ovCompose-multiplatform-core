/*
 * Tencent is pleased to support the open source community by making ovCompose
 * available. Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights
 * reserved.
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

#import "TMMDrawUtils.h"
#import "TMMComposeAdaptivedCanvasViewV2.h"
#import "TMMComposeNativeColorFilter.h"
#import "TMMComposeNativePaint.h"
#import "TMMComposeNativePath.h"
#import "TMMComposeTextLayer.h"
#import "TMMNativeBasicShader.h"
#import "TMMNativeRoundRect.h"

inline float TMMComposeCoreDeviceDensity(void) {
    static float density = 0;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        density = [UIScreen mainScreen].scale;
    });
    return density;
}

inline UIColor *TMMComposeCoreMakeUIColorFromULong(uint64_t colorValue) {
    return [UIColor colorWithRed:((colorValue >> 48) & 0xff) / 255.0f
                           green:((colorValue >> 40) & 0xff) / 255.0f
                            blue:((colorValue >> 32) & 0xff) / 255.0f
                           alpha:((colorValue >> 56) & 0xff) / 255.0f];
}

inline uint64_t TMMNativeDataHashFromPaint(TMMComposeNativePaint *paint) {
    CGFloat shaderHash = 0;
    if ([paint.shader isKindOfClass:TMMNativeBasicShader.class]) {
        shaderHash = [paint.shader propertyHash];
    }

    CGFloat colorFilterHash = 0;
    if ([paint.colorFilter isKindOfClass:[TMMComposeNativeColorFilter class]]) {
        colorFilterHash = [paint.colorFilter hash];
    }

    const float floats[11]
        = { [paint alpha],         [paint strokeWidth],      [paint strokeMiterLimit],  (float)[paint isAntiAlias],   (float)[paint blendMode],
            (float)[paint style],  (float)[paint strokeCap], (float)[paint strokeJoin], (float)[paint filterQuality], (float)shaderHash,
            (float)colorFilterHash };
    uint64_t hash = TMMFNVHash(floats, sizeof(floats));
    hash ^= [paint colorValue];
    hash *= 1099511628211ULL;
    return hash;
}

// 64-bit FNV-1a hash algorithm
inline uint64_t TMMFNVHash(const void *data, size_t len) {
    uint64_t hash = 14695981039346656037ULL;
    const unsigned char *bytes = data;
    for (size_t i = 0; i < len; ++i) {
        hash ^= (uint64_t)bytes[i];
        hash *= 1099511628211ULL;
    }
    return hash;
}

inline uint64_t TMMFNVHashFloatArray(NSArray<NSNumber *> *array) {
    uint64_t hash = 14695981039346656037ULL;
    for (NSNumber *number in array) {
        hash ^= (uint64_t)[number unsignedLongLongValue];
        hash *= 1099511628211ULL;
    }
    return hash;
}

inline uint64_t TMMComposeCoreHash4Floats(float a, float b, float c, float d) {
    // Combine the floats into an array
    const float floats[4] = { a, b, c, d };
    // Compute the hash
    return TMMFNVHash(floats, sizeof(floats));
}

inline uint64_t TMMComposeCoreHash6Floats(float a, float b, float c, float d, float e, float f) {
    // Combine the floats into an array
    const float floats[6] = { a, b, c, d, e, f };
    // Compute the hash
    return TMMFNVHash(floats, sizeof(floats));
}

inline uint64_t TMMComposeCoreHash8Floats(float a, float b, float c, float d, float e, float f, float g, float h) {
    // Combine the floats into an array
    const float floats[8] = { a, b, c, d, e, f, g, h };
    // Compute the hash
    return TMMFNVHash(floats, sizeof(floats));
}

inline uint64_t TMMComposeCoreHashNSArray(NSArray <id <NSObject>> *array) {
    CGFloat floats[array.count];
    for (int i = 0; i < array.count; ++i) {
        floats[i] = ((NSObject *)array[i]).hash;
    }
    return TMMFNVHash(floats, sizeof(floats));
}

id _Nullable TMMComposeCoreNativeNullableAnyCast(id _Nullable object) {
    return object;
}

id TMMComposeCoreNativeAnyCast(id object) {
    return object;
}

TMMComposeNativePath *TMMComposeCoreNativeCreateNativePath(void) {
    return [[TMMComposeNativePath alloc] init];
}

TMMNativeRoundRect *TMMComposeCoreNativeCreateDefaultRoundRect(void) {
    return [[TMMNativeRoundRect alloc] init];
}

TMMComposeTextLayer *TMMComposeTextCreate(void) {
    return [[TMMComposeTextLayer alloc] init];
}

UIImage *TMMDecodedImageFromPath(NSString *imagePath) {
    NSURL *resourcesURL = [NSURL fileURLWithPath:[NSBundle.mainBundle.resourcePath stringByAppendingPathComponent:@"compose-resources"]];
    NSString *absoluteImagePath = [resourcesURL URLByAppendingPathComponent:imagePath].path;
    UIImage *originalImage = [UIImage imageWithContentsOfFile:absoluteImagePath];
    if (!originalImage) {
        // 如果图像加载失败，返回 nil
        NSLog(@"originalImage null, path=%@", absoluteImagePath);
        return nil;
    }

    CGImageRef cgImage = originalImage.CGImage;
    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    CGContextRef context = CGBitmapContextCreate(NULL, originalImage.size.width, originalImage.size.height, CGImageGetBitsPerComponent(cgImage),
                                                 CGImageGetBytesPerRow(cgImage), CGImageGetColorSpace(cgImage), CGImageGetBitmapInfo(cgImage));

    CGContextDrawImage(context, CGRectMake(0.0, 0.0, originalImage.size.width, originalImage.size.height), cgImage);
    CGImageRef decodedImage = CGBitmapContextCreateImage(context);

    CGContextRelease(context);
    CGColorSpaceRelease(colorSpace);

    return [UIImage imageWithCGImage:decodedImage];
}
