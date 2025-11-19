/*
 * Copyright (C) 2023 Huawei Device Co., Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @addtogroup image
 * @{
 *
 * @brief Provides APIs for obtaining pixel map data and information.
 *
 * @syscap SystemCapability.Multimedia.Image.Core
 * @since 12
 */

/**
 * @file pixelmap_native.h
 *
 * @brief Declares the APIs that can access a pixel map.
 *
 * @library libpixelmap.so
 * @kit ImageKit
 * @syscap SystemCapability.Multimedia.Image.Core
 * @since 12
 */

#ifndef INTERFACES_KITS_NATIVE_INCLUDE_IMAGE_PIXELMAP_NATIVE_H_
#define INTERFACES_KITS_NATIVE_INCLUDE_IMAGE_PIXELMAP_NATIVE_H_

#include <stdbool.h>

#include "image_common.h"
#include "napi/native_api.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @brief Define a Pixelmap struct type, used for pixelmap pointer controls.
 *
 * @since 12
 */
struct OH_PixelmapNative;
typedef struct OH_PixelmapNative OH_PixelmapNative;

/**
 * @brief Define a native buffer type, used for retrieving a native buffer.
 *
 * @since 12
 */
struct OH_NativeBuffer;
typedef struct OH_NativeBuffer OH_NativeBuffer;

/**
 * @brief Define a native ColorSpaceManager type, used for retrieving a native ColorSpaceManager.
 *
 * @since 13
 */
typedef struct OH_NativeColorSpaceManager OH_NativeColorSpaceManager;

/**
 * @brief Define a pixelmap alpha type.
 *
 * @since 12
 */
typedef enum {
    /*
    * Unknown format
    */
    PIXELMAP_ALPHA_TYPE_UNKNOWN = 0,
     /*
    * Opaque format
    */
    PIXELMAP_ALPHA_TYPE_OPAQUE = 1,
     /*
    * Premultiplied format
    */
    PIXELMAP_ALPHA_TYPE_PREMULTIPLIED = 2,
    /*
     * Unpremultiplied format
     */
    PIXELMAP_ALPHA_TYPE_UNPREMULTIPLIED = 3,
}PIXELMAP_ALPHA_TYPE;

typedef enum {
    /*
    * Unknown format
    */
    PIXEL_FORMAT_UNKNOWN = 0,
    /*
    * RGB_565 format
    */
    PIXEL_FORMAT_RGB_565 = 2,
    /*
    * RGBA_8888 format
    */
    PIXEL_FORMAT_RGBA_8888 = 3,
    /*
    * BGRA_8888 format
    */
    PIXEL_FORMAT_BGRA_8888 = 4,
    /*
    * RGB_888 format
    */
    PIXEL_FORMAT_RGB_888 = 5,
    /*
    * ALPHA_8 format
    */
    PIXEL_FORMAT_ALPHA_8 = 6,
    /*
    * RGBA_F16 format
    */
    PIXEL_FORMAT_RGBA_F16 = 7,
    /*
    * NV21 format
    */
    PIXEL_FORMAT_NV21 = 8,
    /*
    * NV12 format
    */
    PIXEL_FORMAT_NV12 = 9,
    /*
    * RGBA_1010102 format
    */
    PIXEL_FORMAT_RGBA_1010102 = 10,
    /*
    * YCBCR_P010 format
    */
    PIXEL_FORMAT_YCBCR_P010 = 11,
    /*
    * YCRCB_P010 format
    */
    PIXEL_FORMAT_YCRCB_P010 = 12,
} PIXEL_FORMAT;

/**
 * @brief Defines the anti-aliasing level.
 *
 * @since 12
 */
typedef enum {
    /**
     * Nearest-neighbor interpolation algorithm
     */
    OH_PixelmapNative_AntiAliasing_NONE = 0,
    /**
     * Bilinear interpolation algorithm
     */
    OH_PixelmapNative_AntiAliasing_LOW = 1,
    /**
     * Bilinear interpolation algorithm with mipmap linear filtering
     */
    OH_PixelmapNative_AntiAliasing_MEDIUM = 2,
    /**
     * Cubic interpolation algorithm
     */
    OH_PixelmapNative_AntiAliasing_HIGH = 3,
} OH_PixelmapNative_AntiAliasingLevel;

/**
 * @brief Enumerates the HDR metadata types that need to be stored in Pixelmap.
 *
 * @since 12
 */
typedef enum {
    /**
     * Indicate the types of metadata that image needs to use.
     */
    HDR_METADATA_TYPE = 0,
    /**
     * Static metadata key.
     */
    HDR_STATIC_METADATA = 1,
    /**
     * Dynamic metadata key.
     */
    HDR_DYNAMIC_METADATA = 2,
    /**
     * Gainmap metadata key.
     */
    HDR_GAINMAP_METADATA = 3,
} OH_Pixelmap_HdrMetadataKey;

/**
 * @brief Value for HDR_METADATA_TYPE.
 *
 * @since 12
 */
typedef enum {
    /**
     * No metadata.
     */
    HDR_METADATA_TYPE_NONE = 0,
    /**
     * Indicates that metadata will be used for the base image.
     */
    HDR_METADATA_TYPE_BASE = 1,
    /**
     * Indicates that metadata will be used for the gainmap image.
     */
    HDR_METADATA_TYPE_GAINMAP = 2,
    /**
     * Indicates that metadata will be used for the alternate image.
     */
    HDR_METADATA_TYPE_ALTERNATE = 3,
} OH_Pixelmap_HdrMetadataType;

/**
 * @brief Value for HDR_STATIC_METADATA.
 *
 * @since 12
 */
typedef struct OH_Pixelmap_HdrStaticMetadata {
    /**
     * The X-coordinate of the primary colors. The length of the array is three. Store in the order of r, g, b.
     */
    float displayPrimariesX[3];
    /**
     * The Y-coordinate of the primary colors. The length of the array is three. Store in the order of r, g, b.
     */
    float displayPrimariesY[3];
    /**
     * The X-coordinate of the white point value.
     */
    float whitePointX;
    /**
     * The Y-coordinate of the white point value.
     */
    float whitePointY;
    /**
     * Max luminance.
     */
    float maxLuminance;
    /**
     * Min luminance.
     */
    float minLuminance;
    /**
     * Maximum brightness of displayed content.
     */
    float maxContentLightLevel;
    /**
     * Maximum average brightness of displayed content.
     */
    float maxFrameAverageLightLevel;
} OH_Pixelmap_HdrStaticMetadata;

/**
 * @brief Value for HDR_DYNAMIC_METADATA.
 *
 * @since 12
 */
typedef struct OH_Pixelmap_HdrDynamicMetadata {
    /**
     * The value of dynamic metadata.
     */
    uint8_t* data;
    /**
     * The length of dynamic metadata.
     */
    uint32_t length;
} OH_Pixelmap_HdrDynamicMetadata;

/**
 * @brief Value for HDR_GAINMAP_METADATA.
 *
 * @since 12
 */
typedef struct OH_Pixelmap_HdrGainmapMetadata {
    /**
     * The version used by the writer.
     */
    uint16_t writerVersion;
    /**
     * The minimum version a parser needs to understand.
     */
    uint16_t miniVersion;
    /**
     * The number of gain map channels, with a value of 1 or 3.
     */
    uint8_t gainmapChannelNum;
    /**
     * Indicate whether to use the color space of the base image.
     */
    bool useBaseColorFlag;
    /**
     * The baseline hdr headroom.
     */
    float baseHeadroom;
    /**
     * The alternate hdr headroom.
     */
    float alternateHeadroom;
    /**
     * The per-component max gain map values.
     */
    float gainmapMax[3];
    /**
     * The per-component min gain map values.
     */
    float gainmapMin[3];
    /**
     * The per-component gamma values.
     */
    float gamma[3];
    /**
     * The per-component baseline offset.
     */
    float baselineOffset[3];
    /**
     * The per-component alternate offset.
     */
    float alternateOffset[3];
} OH_Pixelmap_HdrGainmapMetadata;

/**
 * @brief Value for HDR_METADATA_KEY. Corresponding relationship with HDR_METADATA_KEY.
 *
 * @since 12
 */
typedef struct OH_Pixelmap_HdrMetadataValue {
    /**
     * The value corresponding to the HDR_METADATA_TYPE key
     */
    OH_Pixelmap_HdrMetadataType type;
    /**
     * The value corresponding to the HDR_STATIC_METADATA key
     */
    OH_Pixelmap_HdrStaticMetadata staticMetadata;
    /**
     * The value corresponding to the HDR_DYNAMIC_METADATA key
     */
    OH_Pixelmap_HdrDynamicMetadata dynamicMetadata;
    /**
     * The value corresponding to the HDR_GAINMAP_METADATA key
     */
    OH_Pixelmap_HdrGainmapMetadata gainmapMetadata;
} OH_Pixelmap_HdrMetadataValue;

/**
 * @brief Defines the options used for creating a pixel map.
 *
 * @since 12
 */
struct OH_Pixelmap_InitializationOptions;
typedef struct OH_Pixelmap_InitializationOptions OH_Pixelmap_InitializationOptions;

/**
 * @brief Create a for InitializationOtions struct.
 *
 * @param options The InitializationOtions pointer will be operated.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapInitializationOptions_Create(OH_Pixelmap_InitializationOptions **options);

/**
 * @brief Get width number for InitializationOtions struct.
 *
 * @param options The InitializationOtions pointer will be operated.
 * @param width the number of image width.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapInitializationOptions_GetWidth(OH_Pixelmap_InitializationOptions *options,
    uint32_t *width);

/**
 * @brief Set width number for InitializationOtions struct.
 *
 * @param options The InitializationOtions pointer will be operated.
 * @param width the number of image width.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapInitializationOptions_SetWidth(OH_Pixelmap_InitializationOptions *options,
    uint32_t width);

/**
 * @brief Get height number for InitializationOtions struct.
 *
 * @param options The InitializationOtions pointer will be operated.
 * @param height the number of image height.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapInitializationOptions_GetHeight(OH_Pixelmap_InitializationOptions *options,
    uint32_t *height);

/**
 * @brief Set height number for InitializationOtions struct.
 *
 * @param options The InitializationOtions pointer will be operated.
 * @param height the number of image height.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapInitializationOptions_SetHeight(OH_Pixelmap_InitializationOptions *options,
    uint32_t height);

/**
 * @brief Get pixelFormat number for InitializationOtions struct.
 *
 * @param options The InitializationOtions pointer will be operated.
 * @param pixelFormat the number of image pixelFormat.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapInitializationOptions_GetPixelFormat(OH_Pixelmap_InitializationOptions *options,
    int32_t *pixelFormat);

/**
 * @brief Set pixelFormat number for InitializationOtions struct.
 *
 * @param options The InitializationOtions pointer will be operated.
 * @param pixelFormat the number of image pixelFormat.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapInitializationOptions_SetPixelFormat(OH_Pixelmap_InitializationOptions *options,
    int32_t pixelFormat);

/**
 * @brief Get pixelFormat number for InitializationOtions struct.
 *
 * @param options The InitializationOtions pointer will be operated.
 * @param srcpixelFormat the number of image srcpixelFormat.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapInitializationOptions_GetSrcPixelFormat(OH_Pixelmap_InitializationOptions *options,
    int32_t *srcpixelFormat);

/**
 * @brief Set pixelFormat number for InitializationOtions struct.
 *
 * @param options The InitializationOtions pointer will be operated.
 * @param srcpixelFormat the number of image srcpixelFormat.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapInitializationOptions_SetSrcPixelFormat(OH_Pixelmap_InitializationOptions *options,
    int32_t srcpixelFormat);

/**
 * @brief Get rowStride for InitializationOptions struct.
 *
 * @param options The InitializationOptions pointer will be operated.
 * @param rowStride the rowStride of image buffer.
 * @return Returns {@link Image_ErrorCode} IMAGE_SUCCESS - if the operation is successful.
 * returns {@link Image_ErrorCode} IMAGE_BAD_PARAMETER - if rowStride is null.
 * returns {@link Image_ErrorCode} IMAGE_UNKNOWN_ERROR - inner unknown error, maybe options is released.
 * @since 12
 */
Image_ErrorCode OH_PixelmapInitializationOptions_GetRowStride(OH_Pixelmap_InitializationOptions *options,
    int32_t *rowStride);

/**
 * @brief Set rowStride number for InitializationOptions struct.
 *
 * @param options The InitializationOptions pointer will be operated.
 * @param rowStride the rowStride of image buffer.
 * @return Returns {@link Image_ErrorCode} IMAGE_SUCCESS - if the operation is successful.
 * returns {@link Image_ErrorCode} IMAGE_BAD_PARAMETER - if rowStride does not match width.
 * returns {@link Image_ErrorCode} IMAGE_UNKNOWN_ERROR - inner unknown error, maybe options is released.
 * @since 12
 */
Image_ErrorCode OH_PixelmapInitializationOptions_SetRowStride(OH_Pixelmap_InitializationOptions *options,
    int32_t rowStride);

/**
 * @brief Get alphaType number for InitializationOtions struct.
 *
 * @param options The InitializationOtions pointer will be operated.
 * @param alphaType the number of image alphaType.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapInitializationOptions_GetAlphaType(OH_Pixelmap_InitializationOptions *options,
    int32_t *alphaType);

/**
 * @brief Set alphaType number for InitializationOtions struct.
 *
 * @param options The InitializationOtions pointer will be operated.
 * @param alphaType the number of image alphaType.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapInitializationOptions_SetAlphaType(OH_Pixelmap_InitializationOptions *options,
    int32_t alphaType);

/**
 * @brief Get editable for InitializationOptions struct.
 *
 * @param options The InitializationOptions pointer will be operated.
 * @param editable The boolean value representing the editable status.
 * @return Returns {@link Image_ErrorCode} IMAGE_SUCCESS - if the operation is successful.
 * returns {@link Image_ErrorCode} IMAGE_BAD_PARAMETER - if options or editable is invalid.
 * @since 18
 */
Image_ErrorCode OH_PixelmapInitializationOptions_GetEditable(OH_Pixelmap_InitializationOptions *options,
    bool *editable);

/**
 * @brief Set editable for InitializationOptions struct.
 *
 * @param options The InitializationOptions pointer will be operated.
 * @param editable The boolean value representing the editable status.
 * @return Returns {@link Image_ErrorCode} IMAGE_SUCCESS - if the operation is successful.
 * returns {@link Image_ErrorCode} IMAGE_BAD_PARAMETER - if options is invalid.
 * @since 18
 */
Image_ErrorCode OH_PixelmapInitializationOptions_SetEditable(OH_Pixelmap_InitializationOptions *options,
    bool editable);

/**
 * @brief delete InitializationOtions pointer.
 *
 * @param options The InitializationOtions pointer will be operated.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapInitializationOptions_Release(OH_Pixelmap_InitializationOptions *options);

/**
 * @brief Defines the pixel map information.
 *
 * @since 12
 */
struct OH_Pixelmap_ImageInfo;
typedef struct OH_Pixelmap_ImageInfo OH_Pixelmap_ImageInfo;

/**
 * @brief Create imageinfo struct  .
 *
 * @param info The imageinfo pointer will be operated.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapImageInfo_Create(OH_Pixelmap_ImageInfo **info);

/**
 * @brief Get width number for imageinfo struct.
 *
 * @param info The imageinfo pointer will be operated.
 * @param width The number of imageinfo width.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapImageInfo_GetWidth(OH_Pixelmap_ImageInfo *info, uint32_t *width);

/**
 * @brief Get height number for imageinfo struct.
 *
 * @param info The imageinfo pointer will be operated.
 * @param height The number of imageinfo height.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapImageInfo_GetHeight(OH_Pixelmap_ImageInfo *info, uint32_t *height);

/**
 * @brief Get alphaMode number for imageinfo struct.
 *
 * @param info The imageinfo pointer will be operated.
 * @param alphaMode The number of imageinfo alphaMode.
 * @return Image functions result code.
 *     {@link IMAGE_SUCCESS} if the execution is successful.
 *     {@link IMAGE_BAD_PARAMETER} info is nullptr, or alphaMode is nullptr.
 * @since 20
 */
Image_ErrorCode OH_PixelmapImageInfo_GetAlphaMode(OH_Pixelmap_ImageInfo *info, int32_t *alphaMode);

/**
 * @brief Get rowStride number for imageinfo struct.
 *
 * @param info The imageinfo pointer will be operated.
 * @param rowStride The number of imageinfo rowStride.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapImageInfo_GetRowStride(OH_Pixelmap_ImageInfo *info, uint32_t *rowStride);

/**
 * @brief Get pixelFormat number for imageinfo struct.
 *
 * @param info The imageinfo pointer will be operated.
 * @param pixelFormat The number of imageinfo pixelFormat.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapImageInfo_GetPixelFormat(OH_Pixelmap_ImageInfo *info, int32_t *pixelFormat);

/**
 * @brief Get alphaType number for imageinfo struct.
 *
 * @param info The imageinfo pointer will be operated.
 * @param alphaType The number of imageinfo alphaType.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapImageInfo_GetAlphaType(OH_Pixelmap_ImageInfo *info, int32_t *alphaType);

/**
 * @brief Get isHdr boolean for imageinfo struct.
 *
 * @param info The imageinfo pointer will be operated. Pointer connot be null.
 * @param isHdr Whether the image has a high dynamic range.
 * @return Returns {@link Image_ErrorCode} IMAGE_SUCCESS - The operation is successful.
 * returns {@link Image_ErrorCode} IMAGE_BAD_PARAMETER - Parameter error.Possible causes:Parameter verification failed.
 * @since 12
 */
Image_ErrorCode OH_PixelmapImageInfo_GetDynamicRange(OH_Pixelmap_ImageInfo *info, bool *isHdr);

/**
 * @brief Delete imageinfo struct pointer.
 *
 * @param info The imageinfo pointer will be operated.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapImageInfo_Release(OH_Pixelmap_ImageInfo *info);

/**
 * @brief Creates a <b>PixelMap</b> object.
 *
 * @param data Color buffer in BGRA_8888 format.
 * @param dataLength Color buffer size in BGRA_8888 format.
 * @param options IPixel properties, including the alpha type, size, pixel format, and editable.
 * @param pixelmap Pixelmap pointer for created.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapNative_CreatePixelmap(uint8_t *data, size_t dataLength,
    OH_Pixelmap_InitializationOptions *options, OH_PixelmapNative **pixelmap);

/**
 * @brief Creates a pixelmap based on options {@link OH_Pixelmap_InitializationOptions}, the memory type used by the
 * pixelmap can be specified by allocatorType {@link IMAGE_ALLOCATOR_MODE}. By default, the system selects the memory
 * type based on the image type, image size, platform capability, etc. When processing the pixelmap returned by this
 * interface, please always consider the impact of stride.
 *
 * @param data Input color buffer in BGRA_8888 format by default.
 * @param dataLength Length of input buffer in bytes.
 * @param options Pixelmap initialization properties including size, pixel format, alpha type, and editable flags.
 * @param allocator Indicate which memory type will be used by the returned pixelmap.
 * @param pixelmap Output parameter receiving the created pixelmap object pointer.
 * @return Function result code:
 *         {@link IMAGE_SUCCESS} If the operation is successful.
 *         {@link IMAGE_BAD_PARAMETER} If the param is nullptr or invalid.
 *         {@link IMAGE_TOO_LARGE} too large data or image.
 *         {@link IMAGE_UNSUPPORTED_OPERATION} unsupported operations.
 *         {@link IMAGE_DMA_OPERATION_FAILED} DMA operation failed.
 *         {@link IMAGE_ALLOCATOR_MODE_UNSUPPORTED} unsupported allocator mode, e.g.,
 *         use share memory to create a HDR image as only DMA supported hdr metadata.
 * @since 20
 */
Image_ErrorCode OH_PixelmapNative_CreatePixelmapUsingAllocator(uint8_t *data, size_t dataLength,
    OH_Pixelmap_InitializationOptions *options, IMAGE_ALLOCATOR_MODE allocator, OH_PixelmapNative **pixelmap);

/**
 * @brief Convert a native <b>PixelMap</b> object to <b>PixelMap</b> napi object.
 *
 * @param env Indicates the NAPI environment pointer.
 * @param pixelmapNative Indicates a pointer to the <b>PixelMap</b> object created at the native layer.
 * @param pixelmapNapi the <b>PixelMap</b> pointer will be converted.
 * @return Image functions result code.
 *     {@link IMAGE_SUCCESS} if the execution is successful.
 *     {@link IMAGE_BAD_PARAMETER} pixelmapNative is nullptr
 * @since 12
 */
Image_ErrorCode OH_PixelmapNative_ConvertPixelmapNativeToNapi(napi_env env, OH_PixelmapNative *pixelmapNative,
    napi_value *pixelmapNapi);

/**
 * @brief Convert a <b>PixelMap</b> napi object to native <b>PixelMap</b> object.
 *
 * @param env Indicates the NAPI environment pointer.
 * @param pixelmapNapi Indicates napi <b>PixelMap</b> object.
 * @param pixelmapNative Indicates native <b>PixelMap</b> pointer to created.
 * @return Image functions result code.
 *     {@link IMAGE_SUCCESS} if the execution is successful.
 *     {@link IMAGE_BAD_PARAMETER} pixelmapNative is nullptr, or pixelmapNapi is not a PixelMap
 * @since 12
 */
Image_ErrorCode OH_PixelmapNative_ConvertPixelmapNativeFromNapi(napi_env env, napi_value pixelmapNapi,
    OH_PixelmapNative **pixelmapNative);

/**
 * @brief Reads data of this pixel map to an Buffer. If this pixel map is created in the BGRA_8888 format,
 * the data read is the same as the original data.
 *
 * @param pixelmap The Pixelmap pointer will be operated.
 * @param destination Buffer to which the image pixel map data will be written.
 * @param bufferSize Buffer size to which the image pixel map data will be written.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapNative_ReadPixels(OH_PixelmapNative *pixelmap, uint8_t *destination, size_t *bufferSize);

/**
 * @brief Reads image data in an Buffer and writes the data to a Pixelmap object.
 *
 * @param pixelmap The Pixelmap pointer will be operated.
 * @param source Buffer from which the image data will be read.
 * @param bufferSize Buffer size from which the image data will be read.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapNative_WritePixels(OH_PixelmapNative *pixelmap, uint8_t *source, size_t bufferSize);

/**
 * @brief Get argb pixel buffer from pixelmap.
 *
 * @param pixelmap The Pixelmap pointer to be operated.
 * @param destination Buffer to which the image pixel map data will be written.
 * @param bufferSize Buffer size to which the image pixel map data will be written.
 * @return Function result code:
 *         {@link IMAGE_SUCCESS} If the operation is successful.
 *         {@link IMAGE_BAD_PARAMETER} If invalid parameter, destination and bufferSize are incorrect.
 *         {@link IMAGE_UNSUPPORTED_CONVERSION} If format does not support conversion to argb or conversion failed.
 *         {@link IMAGE_ALLOC_FAILED} If device has no memory.
 *         {@link IMAGE_COPY_FAILED} If memory copy failed.
 * @see OH_PixelmapNative
 * @since 13
 */
Image_ErrorCode OH_PixelmapNative_GetArgbPixels(OH_PixelmapNative *pixelmap, uint8_t *destination, size_t *bufferSize);

/**
 * @brief Convert {@link OH_PixelmapNative} to standard dynamic range.
 *
 * @param pixelmap The Pixelmap pointer will be operated. Pointer connot be null.
 * @return Returns {@link Image_ErrorCode} IMAGE_SUCCESS - The operation is successful.
 * returns {@link Image_ErrorCode} IMAGE_BAD_PARAMETER - Parameter error.Possible causes:Parameter verification failed.
 * returns {@link Image_ErrorCode} IMAGE_UNSUPPORTED_OPERATION - Unsupported operation.Pixelmap can't be converted.
 * @since 12
 */
Image_ErrorCode OH_PixelmapNative_ToSdr(OH_PixelmapNative *pixelmap);

/**
 * @brief Obtains pixel map information of this image.
 *
 * @param pixelmap The Pixelmap pointer will be operated.
 * @param imageInfo Indicates the pointer to the image information.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapNative_GetImageInfo(OH_PixelmapNative *pixelmap, OH_Pixelmap_ImageInfo *imageInfo);

/**
 * @brief Sets an opacity rate for this image pixel map.
 *
 * @param pixelmap The Pixelmap pointer will be operated.
 * @param rate Opacity rate to set. The value ranges from 0 to 1.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapNative_Opacity(OH_PixelmapNative *pixelmap, float rate);

/**
 * @brief Scales this image based on the input width and height.
 *
 * @param pixelmap The Pixelmap pointer will be operated.
 * @param scaleX Scaling ratio of the width.
 * @param scaleY Scaling ratio of the height.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapNative_Scale(OH_PixelmapNative *pixelmap, float scaleX, float scaleY);

/**
 * @brief Scales this image based on the input width and height with anti-aliasing.
 *
 * @param pixelmap The Pixelmap pointer will be operated.
 * @param scaleX Scaling ratio of the width.
 * @param scaleY Scaling ratio of the height.
 * @param level The anti-aliasing algorithm to be used.
 * @return Returns {@link Image_ErrorCode} IMAGE_SUCCESS - if the operation is successful.
 * returns {@link Image_ErrorCode} IMAGE_BAD_PARAMETER - if invalid parameter, x and y are incorrect.
 * returns {@link Image_ErrorCode} IMAGE_TOO_LARGE - if image is too large.
 * returns {@link Image_ErrorCode} IMAGE_ALLOC_FAILED - if device has no memory.
 * returns {@link Image_ErrorCode} IMAGE_UNKNOWN_ERROR - inner unknown error, maybe source pixelmap is released.
 * @see OH_PixelmapNative
 * @since 12
 */
Image_ErrorCode OH_PixelmapNative_ScaleWithAntiAliasing(OH_PixelmapNative *pixelmap, float scaleX, float scaleY,
    OH_PixelmapNative_AntiAliasingLevel level);

/**
 * @brief Create a scaled pixelmap based on the source pixelmap and the input width and height.
 *
 * @param srcPixelmap The source native pixelmap.
 * @param dstPixelmap The destination native pixelmap for create.
 * @param scaleX Scaling ratio of the width.
 * @param scaleY Scaling ratio of the height.
 * @return Function result code:
 *         {@link IMAGE_SUCCESS} If the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} If the param is nullptr or invalid.
 * @see OH_PixelmapNative
 * @since 18
 */
Image_ErrorCode OH_PixelmapNative_CreateScaledPixelMap(OH_PixelmapNative *srcPixelmap, OH_PixelmapNative **dstPixelmap,
    float scaleX, float scaleY);

/**
 * @brief Create a scaled pixelmap based on the source pixelmap and the input width and height with anti-aliasing.
 *
 * @param srcPixelmap The source native pixelmap.
 * @param dstPixelmap The destination native pixelmap for create.
 * @param scaleX Scaling ratio of the width.
 * @param scaleY Scaling ratio of the height.
 * @param level The anti-aliasing algorithm to be used.
 * @return Function result code:
 *         {@link IMAGE_SUCCESS} If the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} If the param is nullptr or invalid.
 *         {@link IMAGE_TOO_LARGE} If image is too large.
 *         {@link IMAGE_ALLOC_FAILED} If device has no memory.
 * @see OH_PixelmapNative
 * @since 18
 */
Image_ErrorCode OH_PixelmapNative_CreateScaledPixelMapWithAntiAliasing(OH_PixelmapNative *srcPixelmap,
    OH_PixelmapNative **dstPixelmap, float scaleX, float scaleY, OH_PixelmapNative_AntiAliasingLevel level);

/**
 * @brief Translates this image based on the input coordinates.
 *
 * @param pixelmap The Pixelmap pointer will be operated.
 * @param x The distance to be translate in the X direction.
 * @param y The distance to be translate in the Y direction.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapNative_Translate(OH_PixelmapNative *pixelmap, float x, float y);

/**
 * @brief Rotates this image based on the input angle.
 *
 * @param pixelmap The Pixelmap pointer will be operated.
 * @param angle Angle to rotate.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapNative_Rotate(OH_PixelmapNative *pixelmap, float angle);

/**
 * @brief Flips this image horizontally or vertically, or both.
 *
 * @param pixelmap The Pixelmap pointer will be operated.
 * @param shouldFilpHorizontally Whether to flip the image horizontally.
 * @param shouldFilpVertically Whether to flip the image vertically.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapNative_Flip(OH_PixelmapNative *pixelmap, bool shouldFilpHorizontally, bool shouldFilpVertically);

/**
 * @brief Crops this image based on the input size.
 *
 * @param pixelmap The Pixelmap pointer will be operated.
 * @param region Area size, read according to area.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapNative_Crop(OH_PixelmapNative *pixelmap, Image_Region *region);

/**
 * @brief Releases an <b>OH_Pixelmap</b> object.
 *
 * @param pixelmap The Pixelmap pointer will be operated.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapNative_Release(OH_PixelmapNative *pixelmap);

/**
 * @brief Destroys an <b>OH_PixelmapNative</b> object and deallocates its resources.
 *
 * @param pixelmap A pointer to the OH_PixelmapNative pointer to destroy.
 * @return Returns {@link Image_ErrorCode} IMAGE_SUCCESS - if the operation is successful.
 * returns {@link Image_ErrorCode} IMAGE_BAD_PARAMETER - if pixelmap is null or *pixelmap is null.
 * @since 18
 */
Image_ErrorCode OH_PixelmapNative_Destroy(OH_PixelmapNative **pixelmap);

/**
 * @brief Converting images to alpha format
 *
 * @param srcpixelmap The source pixel map pointer will be operated.
 * @param dstpixelmap The destination pixel map pointer will be operated.
 * @param isPremul Whether it is pre-multiplied, true for prediction, false for non-pre-multiplied.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapNative_ConvertAlphaFormat(OH_PixelmapNative* srcpixelmap,
    OH_PixelmapNative* dstpixelmap, const bool isPremul);

/**
 * @brief Create a empty <b>PixelMap</b> object.
 *
 * @param options IPixel properties, including the alpha type, size, pixel format, and editable.
 * @param pixelmap Pixelmap pointer for created.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PixelmapNative_CreateEmptyPixelmap(
    OH_Pixelmap_InitializationOptions *options, OH_PixelmapNative **pixelmap);

/**
 * @brief Creates a empty pixelmap based on options {@link OH_Pixelmap_InitializationOptions}, the memory type used
 * by the pixelmap can be specified by allocatorType {@link IMAGE_ALLOCATOR_MODE}. By default,
 * the system selects the memory type based on the image type, image size, platform capability, etc. When processing
 * the pixelmap returned by this interface, please always consider the impact of stride.
 *
 * @param options Pixelmap initialization properties including size, pixel format, alpha type, and editable flags.
 * @param allocator Indicate which memory type will be used by the returned pixelmap.
 * @param pixelmap Output parameter receiving the created pixelmap object pointer.
 * @return Function result code:
 *         {@link IMAGE_SUCCESS} If the operation is successful.
 *         {@link IMAGE_BAD_PARAMETER} If the param is nullptr or invalid.
 *         {@link IMAGE_TOO_LARGE} too large data or image.
 *         {@link IMAGE_UNSUPPORTED_OPERATION} unsupported operations.
 *         {@link IMAGE_ALLOCATOR_MODE_UNSUPPORTED} unsupported allocator mode, e.g., use
 *         share memory to create a HDR image as only DMA supported hdr metadata.
 * @since 20
 */
Image_ErrorCode OH_PixelmapNative_CreateEmptyPixelmapUsingAllocator(
    OH_Pixelmap_InitializationOptions *options, IMAGE_ALLOCATOR_MODE allocator, OH_PixelmapNative **pixelmap);

/**
 * @brief Get metadata.
 *
 * @param pixelmap The Pixelmap pointer to be operated.
 * @param key Type of metadata.
 * @param value Value of metadata.
 * @return Returns {@link Image_ErrorCode} IMAGE_SUCCESS - if the operation is successful.
 * returns {@link Image_ErrorCode} IMAGE_BAD_PARAMETER - if invalid parameter, key and value are incorrect.
 * returns {@link Image_ErrorCode} IMAGE_DMA_NOT_EXIST - if DMA memory does not exist.
 * returns {@link Image_ErrorCode} IMAGE_COPY_FAILED - if memory copy failed.
 * @see OH_PixelmapNative
 * @since 12
 */
Image_ErrorCode OH_PixelmapNative_GetMetadata(OH_PixelmapNative *pixelmap, OH_Pixelmap_HdrMetadataKey key,
    OH_Pixelmap_HdrMetadataValue **value);

/**
 * @brief Set metadata.
 *
 * @param pixelmap The Pixelmap pointer to be operated.
 * @param key Type of metadata.
 * @param value Value of metadata.
 * @return Returns {@link Image_ErrorCode} IMAGE_SUCCESS - if the operation is successful.
 * returns {@link Image_ErrorCode} IMAGE_BAD_PARAMETER - if invalid parameter, key and value are incorrect.
 * returns {@link Image_ErrorCode} IMAGE_DMA_NOT_EXIST - if DMA memory does not exist.
 * returns {@link Image_ErrorCode} IMAGE_COPY_FAILED - if memory copy failed.
 * @see OH_PixelmapNative
 * @since 12
 */
Image_ErrorCode OH_PixelmapNative_SetMetadata(OH_PixelmapNative *pixelmap, OH_Pixelmap_HdrMetadataKey key,
    OH_Pixelmap_HdrMetadataValue *value);

/**
 * @brief Get the native buffer from the PixelMap.
 *
 * @param pixelmap The PixelMap to get the native buffer from.
 * @param nativeBuffer The native buffer to retrieve.
 * @return Returns {@link Image_ErrorCode} IMAGE_RESULT_SUCCESS - if the operation is successful.
 * returns {@link Image_ErrorCode} IMAGE_BAD_PARAMETER - if invalid parameter, pixelmap or nativeBuffer is null.
 * returns {@link Image_ErrorCode} IMAGE_DMA_NOT_EXIST - if DMA memory dose not exist.
 * returns {@link Image_ErrorCode} IMAGE_DMA_OPERATION_FAILED - if operations related to DMA memory has failed.
 * @see OH_PixelmapNative
 * @since 12
 */
Image_ErrorCode OH_PixelmapNative_GetNativeBuffer(OH_PixelmapNative *pixelmap, OH_NativeBuffer **nativeBuffer);

/**
 * @brief Get the native colorspace from the PixelMap.
 *
 * @param pixelmap The native pixelmap to get the native colorspace from.
 * @param colorSpaceNative The native colorspace to retrieve.
 * @return Function result code:
 *         {@link IMAGE_SUCCESS} If the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} The param of pixelmap or colorSpaceNative is nullptr or invalid.
 * @see OH_PixelmapNative
 * @since 13
 */
Image_ErrorCode OH_PixelmapNative_GetColorSpaceNative(OH_PixelmapNative *pixelmap,
    OH_NativeColorSpaceManager **colorSpaceNative);

/**
 * @brief Set the native colorspace for the PixelMap.
 *
 * @param pixelmap The native pixelmap to set the native colorspace for.
 * @param colorSpaceNative The native colorspace to set.
 * @return Function result code:
 *         {@link IMAGE_SUCCESS} If the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} The param of pixelmap or colorSpaceNative is nullptr or invalid.
 * @see OH_PixelmapNative
 * @since 13
 */
Image_ErrorCode OH_PixelmapNative_SetColorSpaceNative(OH_PixelmapNative *pixelmap,
    OH_NativeColorSpaceManager *colorSpaceNative);

/**
 * @brief Set pixelmap memory name.
 *
 * @param pixelmap The Pixelmap pointer to be operated.
 * @param name The pointer of name that needs to be set.
 * @param size The size of name size that needs to be set.
 * @return Function result code:
 *         {@link IMAGE_SUCCESS} If the operation is successful.
 *         {@link IMAGE_BAD_PARAMETER} If invalid parameter, name and size are incorrect.
 *         {@link IMAGE_UNSUPPORTED_MEMORY_FORMAT} If memory format is unsupported.
 * @see OH_PixelmapNative
 * @since 13
 */
Image_ErrorCode OH_PixelmapNative_SetMemoryName(OH_PixelmapNative *pixelmap, char *name, size_t *size);

/**
 * @brief Get the total number of bytes occupied by all pixels in the Pixelmap, without any padding.
 *
 * @param pixelmap The Pixelmap pointer to be operated.
 * @param byteCount The total number of bytes to be retrieved.
 * @return Function result code:
 *         {@link IMAGE_SUCCESS} If the operation is successful.
 *         {@link IMAGE_BAD_PARAMETER} If invalid parameter, pixelmap or byteCount are invalid.
 * @see OH_PixelmapNative
 * @since 18
 */
Image_ErrorCode OH_PixelmapNative_GetByteCount(OH_PixelmapNative *pixelmap, uint32_t *byteCount);

/**
 * @brief Get the size of the allocated memory used to store this pixelmap's pixels.
 *
 * @param pixelmap The Pixelmap pointer to be operated.
 * @param allocationByteCount The size of the allocated memory.
 * @return Function result code:
 *         {@link IMAGE_SUCCESS} If the operation is successful.
 *         {@link IMAGE_BAD_PARAMETER} If invalid parameter, pixelmap or allocationByteCount are invalid.
 * @see OH_PixelmapNative
 * @since 18
 */
Image_ErrorCode OH_PixelmapNative_GetAllocationByteCount(OH_PixelmapNative *pixelmap, uint32_t *allocationByteCount);

/**
 * @brief Obtains the memory address of a PixelMap and locks the memory.
 *        When the memory is locked, any operation that modifies or releases the PixelMap will fail and return
 *        {@link IMAGE_BAD_PARAMETER}.
 *
 * @param pixelmap The PixelMap pointer to be operated.
 * @param addr The double pointer to the memory address of the PixelMap.
 * @return Function result code:
 *         {@link IMAGE_SUCCESS} If the operation is successful.
 *         {@link IMAGE_BAD_PARAMETER} If invalid parameter, pixelmap or addr are invalid.
 *         {@link IMAGE_LOCK_UNLOCK_FAILED} If memory failed to be locked.
 * @see OH_PixelmapNative
 * @since 15
 */
Image_ErrorCode OH_PixelmapNative_AccessPixels(OH_PixelmapNative *pixelmap, void **addr);

/**
 * @brief Unlocks the memory of the PixelMap data.
 *        This function is used with {@link OH_PixelmapNative_AccessPixels} in pairs.
 *
 * @param pixelmap The PixelMap pointer to be operated.
 * @return Function result code:
 *         {@link IMAGE_SUCCESS} If the operation is successful.
 *         {@link IMAGE_BAD_PARAMETER} If invalid parameter, pixelmap is invalid.
 *         {@link IMAGE_LOCK_UNLOCK_FAILED} If memory failed to be unlocked.
 * @see OH_PixelmapNative
 * @since 15
 */
Image_ErrorCode OH_PixelmapNative_UnaccessPixels(OH_PixelmapNative *pixelmap);

#ifdef __cplusplus
};
#endif
/** @} */
#endif // INTERFACES_KITS_NATIVE_INCLUDE_IMAGE_PIXELMAP_NATIVE_H_
