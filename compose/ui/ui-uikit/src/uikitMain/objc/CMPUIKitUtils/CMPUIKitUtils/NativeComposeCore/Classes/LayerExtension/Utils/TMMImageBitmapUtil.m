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
#import "TMMComposeInjectCommonServiceImpl.h"
#import "OVComposeExperimentalConfig.h"

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


SKIKO_EXPORT KNativePointer org_jetbrains_skia_Pixmap__1nGetAddr(KNativePointer ptr);
SKIKO_EXPORT KNativePointer org_jetbrains_skia_Bitmap__1nPeekPixels(KNativePointer ptr);
SKIKO_EXPORT KNativePointer org_jetbrains_skia_Pixmap__1nGetFinalizer(void);


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
 对应的原始 kt 代码如下
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
 对应的原始 kt 代码如下
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

static void TMMSkiaPixelsRelease(KNativePointer skPixmapPtr) {
    // 获取 Pixmap 的析构函数并立刻释放 Pixmap 对象，我们不再需要它了
    SkiaBitmapReleaseFunc releasePixmapFunc = (SkiaBitmapReleaseFunc)org_jetbrains_skia_Pixmap__1nGetFinalizer();
    if (releasePixmapFunc && skPixmapPtr) {
        releasePixmapFunc(skPixmapPtr); // 释放临时的 SkPixmap 对象
    }
}

static void TMMSkiaBitmapRelease(KNativePointer skBitmapPtr) {
    // Peek 失败，意味着无法访问像素，释放 Bitmap clone
    SkiaBitmapReleaseFunc releaseBitmapFunc = (SkiaBitmapReleaseFunc)org_jetbrains_skia_Bitmap__1nGetFinalizer();
    if (releaseBitmapFunc) {
        releaseBitmapFunc(skBitmapPtr);
    }
}

static UIImage *_Nullable TMMNativeComposeUIImageFromSkBitmapPtrEncodeImage(KNativePointer originalSkBitmapPtr, int cacheKey) {
    KNativePointer skBitmapPtr = (KNativePointer)originalSkBitmapPtr;
    if (skBitmapPtr == NULL) {
        return nil;
    }

    KNativePointer clonedSkBitmapPtr = org_jetbrains_skia_Bitmap__1nMakeClone(skBitmapPtr);
    if (clonedSkBitmapPtr == NULL) {
        return nil;
    }

    KInt imageInfoArray[4] = { 0 };
    intptr_t *colorSpaceArray[1] = { 0 };
    void *colorSpace = &colorSpaceArray[0];
    org_jetbrains_skia_Bitmap__1nGetImageInfo(clonedSkBitmapPtr, imageInfoArray, &colorSpace);
    const KInt imageWidth = imageInfoArray[0];
    const KInt imageHeight = imageInfoArray[1];
    const TMMSkiaColorType colorType = imageInfoArray[2];
    const TMMSkiaColorAlphaType alphaType = imageInfoArray[3];

    if (imageWidth <= 0 || imageHeight <= 0) {
        // 释放克隆的 Bitmap
        TMMSkiaBitmapRelease(clonedSkBitmapPtr);
        return nil;
    }
    
    KNativePointer skPixmapPtr = org_jetbrains_skia_Bitmap__1nPeekPixels(clonedSkBitmapPtr);
    if (skPixmapPtr == NULL) {
        // Peek 失败，意味着无法访问像素，释放 Bitmap clone
        TMMSkiaBitmapRelease(clonedSkBitmapPtr);
        return nil;
    }

    // 从 SkPixmap 中获取像素缓冲区的真实地址
    void *pixels = org_jetbrains_skia_Pixmap__1nGetAddr(skPixmapPtr);
    
    if (pixels == NULL) {
        // 释放克隆的 Bitmap
        TMMSkiaBitmapRelease(clonedSkBitmapPtr);
        return nil;
    }

    __block UIImage *image = nil;
    const KInt rowBytes = org_jetbrains_skia_Bitmap__1nGetRowBytes(clonedSkBitmapPtr);
    const size_t totalBytes = imageHeight * rowBytes;
    
    const int bytesPerPixel = bytesPerPixelFromColorType(colorType) * CHAR_BIT;
    const int bitsPerComponent = colorType == TMMSkiaColorTypeARGB_4444 ? 4 : 8;
    const CGBitmapInfo bitmapInfo = CGBitmapInfoIntValueFromColorTypeAndAlphaType(colorType, alphaType);

    // 创建一个不拥有内存的 NSData。
    NSData *dataObject = [[NSData alloc] initWithBytesNoCopy:pixels
                                                      length:totalBytes
                                                 deallocator:^(void *_Nonnull bytes, NSUInteger length) {
                                                    TMMSkiaPixelsRelease(skPixmapPtr);
                                                    TMMSkiaBitmapRelease(clonedSkBitmapPtr);
                                                 }];

    CFDataRef cfData = (__bridge CFDataRef)dataObject;
    CGDataProviderRef dataRef = CGDataProviderCreateWithCFData(cfData);

    if (dataRef) {
        static CGColorSpaceRef colorSpace;
        static dispatch_once_t onceToken;
        dispatch_once(&onceToken, ^{
            colorSpace = CGColorSpaceCreateDeviceRGB();
        });
        CGImageRef imageRef = CGImageCreate(imageWidth, imageHeight, bitsPerComponent, bytesPerPixel, rowBytes, colorSpace, bitmapInfo, dataRef,
                                            NULL, false, kCGRenderingIntentDefault);
        
        if (imageRef) {
            image = [UIImage imageWithCGImage:imageRef];
            CGImageRelease(imageRef);
        }
        CGDataProviderRelease(dataRef);
    }
    
    if (image) {
        [[TMMComposeMemoryCache sharedInstance] setObject:image forKey:@(cacheKey) withCost:totalBytes];
    }
    
    return image;
}

static UIImage *_Nullable TMMNativeComposeUIImageFromSkBitmapPtrDecodeImage(KNativePointer originalSkBitmapPtr, int cacheKey) {
    KNativePointer skBitmapPtr = (KNativePointer)originalSkBitmapPtr;
    if (skBitmapPtr == NULL) {
        return nil;
    }

    KNativePointer clonedSkBitmapPtr = org_jetbrains_skia_Bitmap__1nMakeClone(skBitmapPtr);
    if (clonedSkBitmapPtr == NULL) {
        return nil;
    }

    KInt imageInfoArray[4] = { 0 };
    intptr_t *colorSpaceArray[1] = { 0 };
    void *colorSpace = &colorSpaceArray[0];
    org_jetbrains_skia_Bitmap__1nGetImageInfo(clonedSkBitmapPtr, imageInfoArray, &colorSpace);
    const KInt imageWidth = imageInfoArray[0];
    const KInt imageHeight = imageInfoArray[1];
    const TMMSkiaColorType colorType = imageInfoArray[2];
    const TMMSkiaColorAlphaType alphaType = imageInfoArray[3];

    if (imageWidth <= 0 || imageHeight <= 0) {
        // 释放克隆的 Bitmap
        SkiaBitmapReleaseFunc releaseBitmapFunc = (SkiaBitmapReleaseFunc)org_jetbrains_skia_Bitmap__1nGetFinalizer();
        if (releaseBitmapFunc) {
            releaseBitmapFunc(clonedSkBitmapPtr);
        }
        return nil;
    }
    
    KNativePointer skPixmapPtr = org_jetbrains_skia_Bitmap__1nPeekPixels(clonedSkBitmapPtr);
    if (skPixmapPtr == NULL) {
        // Peek 失败，意味着无法访问像素，释放 Bitmap clone
        return nil;
    }

    // 从 SkPixmap 中获取像素缓冲区的真实地址
    void *pixels = org_jetbrains_skia_Pixmap__1nGetAddr(skPixmapPtr);
    
    if (pixels == NULL) {
        // 释放克隆的 Bitmap
        SkiaBitmapReleaseFunc releaseBitmapFunc = (SkiaBitmapReleaseFunc)org_jetbrains_skia_Bitmap__1nGetFinalizer();
        if (releaseBitmapFunc) {
            releaseBitmapFunc(clonedSkBitmapPtr);
        }
        return nil;
    }

    __block UIImage *image = nil;
    const KInt rowBytes = org_jetbrains_skia_Bitmap__1nGetRowBytes(clonedSkBitmapPtr);
    const size_t totalBytes = imageHeight * rowBytes;
    
    const int bytesPerPixel = bytesPerPixelFromColorType(colorType) * CHAR_BIT;
    const int bitsPerComponent = colorType == TMMSkiaColorTypeARGB_4444 ? 4 : 8;
    const CGBitmapInfo bitmapInfo = CGBitmapInfoIntValueFromColorTypeAndAlphaType(colorType, alphaType);

    // 获取 SkBitmap 的析构函数指针，用于在 deallocator 中释放
    SkiaBitmapReleaseFunc releaseBitmapFunc = (SkiaBitmapReleaseFunc)org_jetbrains_skia_Bitmap__1nGetFinalizer();

    // 使用 initWithBytesNoCopy 创建 NSData，不拷贝像素数据
    NSData *dataObject = [[NSData alloc] initWithBytesNoCopy:pixels
                                                      length:totalBytes
                                                 deallocator:^(void *_Nonnull bytes, NSUInteger length) {
                                                    image = nil;
                                                 }];

    CFDataRef cfData = (__bridge CFDataRef)dataObject;
    CGDataProviderRef dataRef = CGDataProviderCreateWithCFData(cfData);
    if (dataRef) {
        static CGColorSpaceRef colorSpace;
        static dispatch_once_t onceToken;
        dispatch_once(&onceToken, ^{
            colorSpace = CGColorSpaceCreateDeviceRGB();
        });
        
        CGImageRef imageRef = CGImageCreate(imageWidth, imageHeight, bitsPerComponent, bytesPerPixel,
                                            rowBytes, colorSpace, bitmapInfo, dataRef,
                                            NULL, false, kCGRenderingIntentDefault);
        
        if (imageRef) {
        
            // create a context with RGBA pixels
            CGContextRef context = CGBitmapContextCreate(pixels, imageWidth, imageHeight, bitsPerComponent, rowBytes, colorSpace,
                                                         kCGBitmapByteOrderDefault | kCGImageAlphaPremultipliedLast);
            
            // paint the bitmap to our context which will fill in the pixels array
            CGContextDrawImage(context, CGRectMake(0, 0, imageWidth, imageHeight), imageRef);
            
            
            // create a new CGImageRef from our context with the modified pixels
            CGImageRef decodeImageRef = CGBitmapContextCreateImage(context);
            
            image = [UIImage imageWithCGImage:decodeImageRef];
            CGImageRelease(imageRef);
            // we're done with the context, color space, and pixels
            CGContextRelease(context);
            CGImageRelease(decodeImageRef);
        }
        
        CGDataProviderRelease(dataRef);
        
        if (releaseBitmapFunc && clonedSkBitmapPtr) {
            releaseBitmapFunc(clonedSkBitmapPtr);
        }
        
        // 获取 Pixmap 的析构函数并立刻释放 Pixmap 对象，我们不再需要它了
        SkiaBitmapReleaseFunc releaseSkPixmapFunc = (SkiaBitmapReleaseFunc)org_jetbrains_skia_Pixmap__1nGetFinalizer();
        if (releaseSkPixmapFunc && skPixmapPtr) {
            releaseSkPixmapFunc(skPixmapPtr); // 释放临时的 SkPixmap 对象
        }
    }
    
    // 如果创建 UIImage 失败，需要手动释放克隆的 Bitmap
    if (image == nil && releaseBitmapFunc && clonedSkBitmapPtr) {
        releaseBitmapFunc(clonedSkBitmapPtr);
    }
    
    if (image) {
        [[TMMComposeMemoryCache sharedInstance] setObject:image forKey:@(cacheKey) withCost:totalBytes];
    }
    
    return image;
}


static UIImage *_Nullable TMMNativeComposeUIImageFromSkBitmapPtr2(KNativePointer clonedSkBitmapPtr, int cacheKey) {
    KNativePointer skBitmapPtr = (KNativePointer)clonedSkBitmapPtr;
    if (skBitmapPtr == NULL) {
        return nil;
    }

    /*
     step1: 从 skBitmapPtr 中获取 imageInfo
     */
    KInt imageInfoArray[4] = { 0 };
    intptr_t *colorSpaceArray[1] = { 0 };
    void *colorSpacePtr = &colorSpaceArray[0];
    org_jetbrains_skia_Bitmap__1nGetImageInfo(skBitmapPtr, imageInfoArray, &colorSpacePtr);
    const KInt imageWidth = imageInfoArray[0];
    const KInt imageHeight = imageInfoArray[1];
    const TMMSkiaColorType colorType = imageInfoArray[2];
    const TMMSkiaColorAlphaType alphaType = imageInfoArray[3];
    
    // 获取 SkBitmap 的 C++ 析构函数指针，后面 deallocator 需要用
    SkiaBitmapReleaseFunc releaseBitmapFunc = (SkiaBitmapReleaseFunc)org_jetbrains_skia_Bitmap__1nGetFinalizer();

    if (imageWidth <= 0 || imageHeight <= 0) {
        return nil;
    }

    /*
     step2: 使用 _1nPeekPixels 获取一个指向像素的 SkPixmap 对象
     */
    KNativePointer skPixmapPtr = org_jetbrains_skia_Bitmap__1nPeekPixels(skBitmapPtr);
    if (skPixmapPtr == NULL) {
        // Peek 失败，意味着无法访问像素，释放 Bitmap clone
        return nil;
    }

    // 从 SkPixmap 中获取像素缓冲区的真实地址
    void *pixels = org_jetbrains_skia_Pixmap__1nGetAddr(skPixmapPtr);

    if (pixels == NULL) {
        // Pixmap 有，但像素地址为空，同样需要释放 Bitmap clone
        return nil;
    }

    /*
     step3: 将 byteArray 转化为 UIImage，使用无拷贝方式
     */
    UIImage *image = nil;
    const KInt rowBytes = org_jetbrains_skia_Bitmap__1nGetRowBytes(skBitmapPtr);
    const size_t totalBytes = imageHeight * rowBytes;
    
    const int bitsPerPixel = bytesPerPixelFromColorType(colorType) * 8;
    const int bitsPerComponent = colorType == TMMSkiaColorTypeARGB_4444 ? 4 : 8;
    const CGBitmapInfo bitmapInfo = CGBitmapInfoIntValueFromColorTypeAndAlphaType(colorType, alphaType);

    // 创建一个不拥有内存的 NSData。
    NSData *dataObject = [[NSData alloc] initWithBytesNoCopy:pixels
                                                      length:totalBytes
                                                freeWhenDone:NO];

    CFDataRef cfData = (__bridge CFDataRef)dataObject;
    CGDataProviderRef dataRef = CGDataProviderCreateWithCFData(cfData);

    if (dataRef) {
        static CGColorSpaceRef colorSpace;
        static dispatch_once_t onceToken;
        dispatch_once(&onceToken, ^{
            colorSpace = CGColorSpaceCreateDeviceRGB();
        });
        CGImageRef imageRef = CGImageCreate(imageWidth, imageHeight, bitsPerComponent, bitsPerPixel, rowBytes, colorSpace, bitmapInfo, dataRef,
                                            NULL, false, kCGRenderingIntentDefault);
        
        if (imageRef) {
            image = [UIImage imageWithCGImage:imageRef];
            CGImageRelease(imageRef);
        }
        
        CGDataProviderRelease(dataRef);
    }
    
    // 获取 Pixmap 的析构函数并立刻释放 Pixmap 对象，我们不再需要它了
    SkiaBitmapReleaseFunc releasePixmapFunc = (SkiaBitmapReleaseFunc)org_jetbrains_skia_Pixmap__1nGetFinalizer();
    if (releasePixmapFunc && skPixmapPtr) {
        releasePixmapFunc(skPixmapPtr); // 释放临时的 SkPixmap 对象
    }
    
    if (image) {
        [[TMMComposeMemoryCache sharedInstance] setObject:image forKey:@(cacheKey) withCost:dataObject.length];
    }
    
    return image;
}

static UIImage *_Nullable TMMNativeComposeUIImageFromSkBitmapPtrByteArray(KNativePointer clonedSkBitmapPtr, int cacheKey) {
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
            if (image) {
                [[TMMComposeMemoryCache sharedInstance] setObject:image forKey:@(cacheKey) withCost:dataObject.length];
            }
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

static NSString* const gTabSwtich = @"ios_compose_skia_bitmap_no_copy";

UIImage *_Nullable TMMNativeComposeUIImageFromSkBitmap(intptr_t skBitmapPtrAddress, int cacheKey, OVComposeExperimentalConfig *experimentalConfig) {
    KNativePointer skBitmapPtr = (KNativePointer)skBitmapPtrAddress;
    if (skBitmapPtr == NULL) {
        return nil;
    }
    UIImage *image = nil;
    BOOL isNoCopy = [TMMComposeInjectCommonServiceImpl.sharedInstance tmm_getTabToggleIsOnKey:gTabSwtich defaultValue:NO];
    if (isNoCopy) {
        image = TMMNativeComposeUIImageFromSkBitmapPtr2(skBitmapPtr, cacheKey);
    } else {
        switch (experimentalConfig.textFixLeakType) {
            case TMMTextFixLeakTypeNone:
            case TMMTextFixLeakTypeByteArray:
                image = TMMNativeComposeUIImageFromSkBitmapPtrByteArray(skBitmapPtr, cacheKey);
                break;
            case TMMTextFixLeakTypeDecodeImage:
                image = TMMNativeComposeUIImageFromSkBitmapPtrDecodeImage(skBitmapPtr, cacheKey);
                break;
            case TMMTextFixLeakTypeEncodeImage:
                image = TMMNativeComposeUIImageFromSkBitmapPtrEncodeImage(skBitmapPtr, cacheKey);
                break;
            default:
                image = TMMNativeComposeUIImageFromSkBitmapPtrByteArray(skBitmapPtr, cacheKey);
                break;
        }
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
