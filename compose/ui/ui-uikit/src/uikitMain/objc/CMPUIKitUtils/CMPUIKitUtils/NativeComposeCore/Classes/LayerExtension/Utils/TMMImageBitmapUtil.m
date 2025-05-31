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

#import "TMMImageBitmapUtil.h"
#import "TMMComposeMemoryCache.h"
#import "TMMDrawUtils.h"

typedef int8_t KBoolean;
typedef int8_t KByte;
typedef int16_t KChar;
typedef int16_t KShort;
typedef int32_t KInt;
typedef float KFloat;
typedef int64_t KLong;
typedef double KDouble;
typedef void *KNativePointer;
typedef void *KInteropPointer;
typedef void *KInteropPointerArray;
typedef void *KNativePointerArray;

typedef void (*SkiaBitmapReleaseFunc)(void *);

typedef NS_ENUM(int, TMMSkiaColorType) {
    TMMSkiaColorTypeUNKNOWN, // UNKNOWN
    TMMSkiaColorTypeALPHA_8,
    TMMSkiaColorTypeRGB_565,
    TMMSkiaColorTypeARGB_4444,
    TMMSkiaColorTypeRGBA_8888,
    TMMSkiaColorTypeRGB_888X,
    TMMSkiaColorTypeBGRA_8888,
    TMMSkiaColorTypeRGBA_1010102,
    TMMSkiaColorTypeBGRA_1010102,
    TMMSkiaColorTypeRGB_101010X,
    TMMSkiaColorTypeBGR_101010X,
    TMMSkiaColorTypeBGR_101010X_XR,
    TMMSkiaColorTypeGRAY_8,
    TMMSkiaColorTypeRGBA_F16NORM,
    TMMSkiaColorTypeRGBA_F16,
    TMMSkiaColorTypeRGBA_F32,
    TMMSkiaColorTypeR8G8_UNORM,
    TMMSkiaColorTypeA16_FLOAT,
    TMMSkiaColorTypeR16G16_FLOAT,
    TMMSkiaColorTypeA16_UNORM,
    TMMSkiaColorTypeR16G16_UNORM,
    TMMSkiaColorTypeR16G16B16A16_UNORM,
};

typedef NS_ENUM(int, TMMSkiaColorAlphaType) {
    TMMSkiaColorAlphaTypeUNKNOWN,
    TMMSkiaColorAlphaTypeOPAQUE,
    TMMSkiaColorAlphaTypePREMUL,
    TMMSkiaColorAlphaTypeUNPREMUL,
};

#pragma mark - Skiko defines
#define SKIKO_EXPORT extern

SKIKO_EXPORT void org_jetbrains_skia_Bitmap__1nGetImageInfo(KNativePointer ptr, KInt *imageInfoResult,
                                                            KNativePointer _Nullable *_Nullable colorSpacePtrsArray);

SKIKO_EXPORT KInt org_jetbrains_skia_Bitmap__1nGetRowBytes(KNativePointer ptr);
SKIKO_EXPORT KInt org_jetbrains_skia_Bitmap__1nComputeByteSize(KNativePointer ptr);

SKIKO_EXPORT KBoolean org_jetbrains_skia_Bitmap__1nReadPixels(KNativePointer ptr, KInt width, KInt height, KInt colorType, KInt alphaType,
                                                              KNativePointer colorSpacePtr, KInt rowBytes, KInt srcX, KInt srcY, KByte *resultBytes);

SKIKO_EXPORT KNativePointer org_jetbrains_skia_Bitmap__1nGetPixels(KNativePointer ptr);

SKIKO_EXPORT KNativePointer org_jetbrains_skia_Bitmap__1nGetFinalizer(void);
SKIKO_EXPORT KNativePointer org_jetbrains_skia_Bitmap__1nMakeClone(KNativePointer ptr);

#pragma mark - private
static inline int32_t readPixelsArraySize(int32_t dstInfoHeight, int32_t srcY, int32_t dstRowBytes) {
    return dstInfoHeight * dstRowBytes;
}

/*
 根据一个 skia 的 colorType 输出 bytesPerPixel
 kt 源码：ColorType.kt 124 行
 val bytesPerPixel: Int
     get() {
         return when (this) {
             UNKNOWN -> 0
             ALPHA_8 -> 1
             RGB_565 -> 2
             ARGB_4444 -> 2
             RGBA_8888 -> 4
             BGRA_8888 -> 4
             RGB_888X -> 4
             RGBA_1010102 -> 4
             RGB_101010X -> 4
             BGRA_1010102 -> 4
             BGR_101010X -> 4
             BGR_101010X_XR -> 4
             GRAY_8 -> 1
             RGBA_F16NORM -> 8
             RGBA_F16 -> 8
             RGBA_F32 -> 16
             R8G8_UNORM -> 2
             A16_UNORM -> 2
             R16G16_UNORM -> 4
             A16_FLOAT -> 2
             R16G16_FLOAT -> 4
             R16G16B16A16_UNORM -> 8
         }
     }
 */
NS_INLINE int bytesPerPixelFromColorType(TMMSkiaColorType colorType) {
    switch (colorType) {
        case TMMSkiaColorTypeGRAY_8:
        case TMMSkiaColorTypeALPHA_8:
            return 1;
        case TMMSkiaColorTypeA16_UNORM:
        case TMMSkiaColorTypeR8G8_UNORM:
        case TMMSkiaColorTypeARGB_4444:
        case TMMSkiaColorTypeRGB_565:
        case TMMSkiaColorTypeA16_FLOAT:
            return 2;
        case TMMSkiaColorTypeRGBA_8888:
        case TMMSkiaColorTypeBGRA_8888:
        case TMMSkiaColorTypeRGB_888X:
        case TMMSkiaColorTypeRGBA_1010102:
        case TMMSkiaColorTypeRGB_101010X:
        case TMMSkiaColorTypeBGRA_1010102:
        case TMMSkiaColorTypeBGR_101010X:
        case TMMSkiaColorTypeBGR_101010X_XR:
        case TMMSkiaColorTypeR16G16_UNORM:
        case TMMSkiaColorTypeR16G16_FLOAT:
            return 4;
        case TMMSkiaColorTypeRGBA_F16NORM:
        case TMMSkiaColorTypeRGBA_F16:
        case TMMSkiaColorTypeR16G16B16A16_UNORM:
            return 8;
        case TMMSkiaColorTypeRGBA_F32:
            return 16;
        default:
            return 0;
    }
}

/**
 原始 kt 代码
 private fun computeCgAlphaInfoRgba(at: ColorAlphaType): CGBitmapInfo {
     val info: CGBitmapInfo = kCGBitmapByteOrder32Big
     return when (at) {
         ColorAlphaType.UNKNOWN -> info
         ColorAlphaType.OPAQUE -> info or
 CGImageAlphaInfo.kCGImageAlphaNoneSkipLast.value ColorAlphaType.PREMUL -> info
 or CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
         ColorAlphaType.UNPREMUL -> info or
 CGImageAlphaInfo.kCGImageAlphaLast.value
     }
 }
 */
NS_INLINE uint32_t computeCgAlphaInfoRgba(TMMSkiaColorAlphaType alphaType) {
    switch (alphaType) {
        case TMMSkiaColorAlphaTypeOPAQUE:
            return (kCGBitmapByteOrder32Big | kCGImageAlphaNoneSkipLast);
        case TMMSkiaColorAlphaTypePREMUL:
            return (kCGBitmapByteOrder32Big | kCGImageAlphaPremultipliedLast);
        case TMMSkiaColorAlphaTypeUNPREMUL:
            return (kCGBitmapByteOrder32Big | kCGImageAlphaLast);
        default:
            return kCGBitmapByteOrder32Big;
    }
}

/**
 private fun computeCgAlphaInfoBgra(at: ColorAlphaType): CGBitmapInfo {
     val info: CGBitmapInfo = kCGBitmapByteOrder32Little
     return when (at) {
         ColorAlphaType.UNKNOWN -> info
         ColorAlphaType.OPAQUE -> info or
 CGImageAlphaInfo.kCGImageAlphaNoneSkipFirst.value ColorAlphaType.PREMUL -> info
 or CGImageAlphaInfo.kCGImageAlphaPremultipliedFirst.value
         ColorAlphaType.UNPREMUL -> info or
 CGImageAlphaInfo.kCGImageAlphaFirst.value
     }
 }
 */
NS_INLINE uint32_t computeCgAlphaInfoBgra(TMMSkiaColorAlphaType alphaType) {
    switch (alphaType) {
        case TMMSkiaColorAlphaTypeOPAQUE:
            return (kCGBitmapByteOrder32Little | kCGImageAlphaNoneSkipFirst);
        case TMMSkiaColorAlphaTypePREMUL:
            return (kCGBitmapByteOrder32Little | kCGImageAlphaPremultipliedFirst);
        case TMMSkiaColorAlphaTypeUNPREMUL:
            return (kCGBitmapByteOrder32Little | kCGImageAlphaFirst);
        default:
            return kCGBitmapByteOrder32Little;
    }
}

/**
 private fun computeCgAlphaInfo4444(at: ColorAlphaType): CGBitmapInfo {
     val info: CGBitmapInfo = kCGBitmapByteOrder16Little
     return when (at) {
         ColorAlphaType.OPAQUE -> info or
 CGImageAlphaInfo.kCGImageAlphaNoneSkipLast.value else -> info or
 CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
     }
 }
 */
NS_INLINE uint32_t computeCgAlphaInfo4444(TMMSkiaColorAlphaType alphaType) {
    switch (alphaType) {
        case TMMSkiaColorAlphaTypeOPAQUE:
            return kCGImageAlphaNoneSkipLast;
        default:
            return (kCGBitmapByteOrder16Little | kCGImageAlphaPremultipliedLast);
    }
}

/// 根据 colorType 和 alphaType 计算出一个 CGBitmapInfo
/// - Parameters:
///   - colorType: TMMSkiaColorType
///   - alphaType: TMMSkiaColorAlphaType
NS_INLINE CGBitmapInfo CGBitmapInfoIntValueFromColorTypeAndAlphaType(TMMSkiaColorType colorType, TMMSkiaColorAlphaType alphaType) {
    CGBitmapInfo bitmapInfo = 0;
    switch (colorType) {
        case TMMSkiaColorTypeRGBA_8888:
        case TMMSkiaColorTypeALPHA_8:
        case TMMSkiaColorTypeRGB_565:
            bitmapInfo = (CGBitmapInfo)computeCgAlphaInfoRgba(alphaType);
            break;
        case TMMSkiaColorTypeBGRA_8888:
            bitmapInfo = (CGBitmapInfo)computeCgAlphaInfoBgra(alphaType);
            break;
        case TMMSkiaColorTypeARGB_4444:
            bitmapInfo = (CGBitmapInfo)computeCgAlphaInfo4444(alphaType);
            break;
        default:
            break;
    }
    return (CGBitmapInfo)bitmapInfo;
}

static UIImage *_Nullable TMMNativeComposeUIImageFromSkBitmapPtr(KNativePointer clonedSkBitmapPtr) {
    KNativePointer skBitmapPtr = (KNativePointer)clonedSkBitmapPtr;
    if (skBitmapPtr == NULL) {
        return nil;
    }

    /*
     step1: 从 skBitmapPtr 中获取 imageInfo
     */
    KInt imageInfoArray[4] = { 0 };
    intptr_t *colorSpaceArray[1] = { 0 };
    void *colorSpace = &colorSpaceArray[0];
    org_jetbrains_skia_Bitmap__1nGetImageInfo(skBitmapPtr, imageInfoArray, &colorSpace);
    const KInt imageWidth = imageInfoArray[0];
    const KInt imageHeight = imageInfoArray[1];
    const TMMSkiaColorType colorType = imageInfoArray[2];
    const TMMSkiaColorAlphaType alphaType = imageInfoArray[3];

    /*
     step2: 从 skBitmapPtr 中拷贝出 Pixels
     想过使用更高效的方式：通过 org_jetbrains_skia_Bitmap__1nGetPixels 拿到 pixels
     直接传入 [[NSData alloc] initWithBytesNoCopy:pixels ...]
     绘制出来的图像异常，必须通过 org_jetbrains_skia_Bitmap__1nReadPixels
     以指定格式进行 copy 后才可以正常渲染。 此外考虑到 skBitmap 可能从 Kt
     侧释放，因此此处 copy 一次也没有问题，且 copy 的 byteArray 不占 GC 的内存了
     */
    const KInt rowBytes = org_jetbrains_skia_Bitmap__1nGetRowBytes(skBitmapPtr);
    const int32_t byteArraySize = readPixelsArraySize(imageHeight, 0, rowBytes);
    KByte *byteArray = (KByte *)malloc(byteArraySize * sizeof(KByte));
    UIImage *image = nil;
    if (byteArray != NULL) {
        bool result = org_jetbrains_skia_Bitmap__1nReadPixels(skBitmapPtr, imageWidth, imageHeight, colorType, alphaType, colorSpace, rowBytes, 0, 0,
                                                              byteArray);
        if (result) {
            /*
             step3: 将 byteArray 转化为 UIImage
             */
            const int bytesPerPixel = bytesPerPixelFromColorType(colorType) * CHAR_BIT;
            const int bitsPerComponent = colorType == TMMSkiaColorTypeARGB_4444 ? 4 : 8;
            const CGBitmapInfo bitmapInfo = CGBitmapInfoIntValueFromColorTypeAndAlphaType(colorType, alphaType);
            NSData *dataObject = [[NSData alloc] initWithBytesNoCopy:byteArray
                                                              length:byteArraySize
                                                         deallocator:^(void *_Nonnull bytes, NSUInteger length) {
                                                             free(byteArray);
                                                         }];
            CFDataRef cfData = (__bridge CFDataRef)dataObject;
            CGDataProviderRef dataRef = CGDataProviderCreateWithCFData(cfData);
            static CGColorSpaceRef colorSpace;
            static dispatch_once_t onceToken;
            dispatch_once(&onceToken, ^{
                colorSpace = CGColorSpaceCreateDeviceRGB();
            });
            CGImageRef imageRef = CGImageCreate(imageWidth, imageHeight, bitsPerComponent, bytesPerPixel, rowBytes, colorSpace, bitmapInfo, dataRef,
                                                NULL, false, kCGRenderingIntentDefault);
            image = [UIImage imageWithCGImage:imageRef];
            CGImageRelease(imageRef);
            CGDataProviderRelease(dataRef);
        }
    }
    return image;
}

#pragma mark - public

FOUNDATION_EXTERN CGSize TMMNativeComposeUIImageSizeFromSkBitmap(intptr_t skBitmapPtrAddress) {
    KNativePointer skBitmapPtr = (KNativePointer)skBitmapPtrAddress;
    if (skBitmapPtr == NULL) {
        return CGSizeZero;
    }
    /*
     step1: 从 skBitmapPtr 中获取 imageInfo
     */
    KInt imageInfoArray[4] = { 0 };
    intptr_t *colorSpaceArray[1] = { 0 };
    void *colorSpace = &colorSpaceArray[0];
    org_jetbrains_skia_Bitmap__1nGetImageInfo(skBitmapPtr, imageInfoArray, &colorSpace);
    const KInt imageWidth = imageInfoArray[0];
    const KInt imageHeight = imageInfoArray[1];
    return CGSizeMake(imageWidth, imageHeight);
}

UIImage *_Nullable TMMNativeComposeUIImageFromSkBitmap(intptr_t skBitmapPtrAddress, int cacheKey) {
    KNativePointer skBitmapPtr = (KNativePointer)skBitmapPtrAddress;
    if (skBitmapPtr == NULL) {
        return nil;
    }
    UIImage *image = TMMNativeComposeUIImageFromSkBitmapPtr(skBitmapPtr);
    if (image) {
        [[TMMComposeMemoryCache sharedInstance] setObject:image forKey:@(cacheKey)];
    }
    return image;
}

intptr_t TMMNativeComposeHasTextImageCache(int32_t cacheKey) {
    UIImage *image = [[TMMComposeMemoryCache sharedInstance] objectForKey:@(cacheKey)];
    if (image) {
        CFTypeRef imageRef = (__bridge_retained CFTypeRef)image;
        return (intptr_t)imageRef;
    }
    return 0;
}
