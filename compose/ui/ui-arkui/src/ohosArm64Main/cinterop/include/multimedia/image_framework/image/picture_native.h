/*
 * Copyright (C) 2024 Huawei Device Co., Ltd.
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
 * @brief Provides APIs for obtaining picture data and information.
 *
 * @syscap SystemCapability.Multimedia.Image.Core
 * @since 13
 */

/**
 * @file picture_native.h
 *
 * @brief Declares the APIs that can access a picture.
 *
 * @library libpicture.so
 * @kit ImageKit
 * @syscap SystemCapability.Multimedia.Image.Core
 * @since 13
 */
#ifndef INTERFACES_KITS_NATIVE_INCLUDE_IMAGE_PICTURE_NATIVE_H_
#define INTERFACES_KITS_NATIVE_INCLUDE_IMAGE_PICTURE_NATIVE_H_
#include "image_common.h"
#include "pixelmap_native.h"
 
#ifdef __cplusplus
extern "C" {
#endif

/**
 * @brief Define a Picture struct type, used for picture pointer controls.
 *
 * @since 13
 */
struct OH_PictureNative;

/**
 * @brief Define a Picture struct type, used for picture pointer controls.
 *
 * @since 13
 */
typedef struct OH_PictureNative OH_PictureNative;

/**
 * @brief Define a AuxiliaryPicture struct type, used for auxiliary
 * picture pointer controls.
 *
 * @since 13
 */
struct OH_AuxiliaryPictureNative;

/**
 * @brief Define a AuxiliaryPicture struct type, used for auxiliary
 * picture pointer controls.
 *
 * @since 13
 */
typedef struct OH_AuxiliaryPictureNative OH_AuxiliaryPictureNative;

/**
 * @brief Define a AuxiliaryPictureInfo struct type, used for auxiliary
 * picture info controls.
 *
 * @since 13
 */
struct OH_AuxiliaryPictureInfo;

/**
 * @brief Define a AuxiliaryPictureInfo struct type, used for auxiliary
 * picture info controls.
 *
 * @since 13
 */
typedef struct OH_AuxiliaryPictureInfo OH_AuxiliaryPictureInfo;

/**
 * @brief Define a auxiliary picture type.
 *
 * @since 13
 */
typedef enum {
    /*
    * Gainmap
    */
    AUXILIARY_PICTURE_TYPE_GAINMAP = 1,
    /*
    * Depth map
    */
    AUXILIARY_PICTURE_TYPE_DEPTH_MAP = 2,
    /*
    * Unrefocus map
    */
    AUXILIARY_PICTURE_TYPE_UNREFOCUS_MAP = 3,
    /*
    * Linear map
    */
    AUXILIARY_PICTURE_TYPE_LINEAR_MAP = 4,
    /*
    * Fragment map
    */
    AUXILIARY_PICTURE_TYPE_FRAGMENT_MAP = 5,
} Image_AuxiliaryPictureType;

/**
 * @brief Create a <b>Picture</b> object.
 *
 * @param mainPixelmap The pixel map of the main image.
 * @param picture Picture pointer for created.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} mainPixelmap is nullptr, or picture is nullptr.
 * @since 13
 */
Image_ErrorCode OH_PictureNative_CreatePicture(OH_PixelmapNative *mainPixelmap, OH_PictureNative **picture);

/**
 * @brief Obtains the pixel map of the main image.
 *
 * @param picture The Picture pointer will be operated.
 * @param mainPixelmap Main pixel map pointer for obtained.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} picture is nullptr, or mainPixelmap is nullptr.
 * @since 13
 */
Image_ErrorCode OH_PictureNative_GetMainPixelmap(OH_PictureNative *picture, OH_PixelmapNative **mainPixelmap);

/**
 * @brief Obtains the hdr pixel map.
 *
 * @param picture The Picture pointer will be operated.
 * @param hdrPixelmap Hdr pixel map pointer for obtained.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} picture is nullptr, or hdrPixelmap is nullptr.
 *         {@link IMAGE_UNSUPPORTED_OPERATION} Unsupported operation, e.g. the picture does not has a gainmap
 * @since 13
 */
Image_ErrorCode OH_PictureNative_GetHdrComposedPixelmap(OH_PictureNative *picture, OH_PixelmapNative **hdrPixelmap);

/**
 * @brief Obtains the gainmap pixel map.
 *
 * @param picture The Picture pointer will be operated.
 * @param gainmapPixelmap Gainmap pointer for obtained.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} picture is nullptr, or gainmapPixelmap is nullptr.
 * @since 13
 */
Image_ErrorCode OH_PictureNative_GetGainmapPixelmap(OH_PictureNative *picture, OH_PixelmapNative **gainmapPixelmap);

/**
 * @brief Set auxiliary picture.
 *
 * @param picture The Picture pointer will be operated.
 * @param type The type of auxiliary picture.
 * @param auxiliaryPicture AuxiliaryPicture object.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} picture is nullptr, or auxiliaryPicture is nullptr, or the type is invalid.
 * @since 13
 */
Image_ErrorCode OH_PictureNative_SetAuxiliaryPicture(OH_PictureNative *picture, Image_AuxiliaryPictureType type,
    OH_AuxiliaryPictureNative *auxiliaryPicture);

/**
 * @brief Obtains the auxiliary picture based on type.
 *
 * @param picture The Picture pointer will be operated.
 * @param type The type of auxiliary picture.
 * @param auxiliaryPicture AuxiliaryPicture pointer for obtained.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} picture is nullptr, or auxiliaryPicture is nullptr, or the type is invalid.
 * @since 13
 */
Image_ErrorCode OH_PictureNative_GetAuxiliaryPicture(OH_PictureNative *picture, Image_AuxiliaryPictureType type,
    OH_AuxiliaryPictureNative **auxiliaryPicture);

/**
 * @brief Obtains the metadata of main picture.
 *
 * @param picture The Picture pointer will be operated.
 * @param metadataType The type of metadata.
 * @param metadata The metadata of main picture.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} picture is nullptr, or metadata is nullptr.
 *         {@link IMAGE_UNSUPPORTED_METADATA} unsupported metadata type.
 * @since 13
 */
Image_ErrorCode OH_PictureNative_GetMetadata(OH_PictureNative *picture, Image_MetadataType metadataType,
    OH_PictureMetadata **metadata);

/**
 * @brief Set main picture metadata.
 *
 * @param picture The Picture pointer will be operated.
 * @param metadataType The type of metadata.
 * @param metadata The metadata will be set.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} picture is nullptr, or metadata is nullptr.
 *         {@link IMAGE_UNSUPPORTED_METADATA} unsupported metadata type.
 * @since 13
 */
Image_ErrorCode OH_PictureNative_SetMetadata(OH_PictureNative *picture, Image_MetadataType metadataType,
    OH_PictureMetadata *metadata);

/**
 * @brief Releases this Picture object.
 *
 * @param picture The Picture pointer will be operated.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} picture is nullptr.
 * @since 13
 */
Image_ErrorCode OH_PictureNative_Release(OH_PictureNative *picture);

/**
 * @brief Create a <b>AuxiliaryPicture</b> object.
 *
 * @param data The image data buffer.
 * @param dataLength The length of data.
 * @param size The size of auxiliary picture.
 * @param type The type of auxiliary picture.
 * @param auxiliaryPicture AuxiliaryPicture pointer for created.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} data is nullptr, or dataLength is invalid, or size is nullptr, or the type
 *         is invalid, or auxiliaryPicture is nullptr.
 * @since 13
 */
Image_ErrorCode OH_AuxiliaryPictureNative_Create(uint8_t *data, size_t dataLength, Image_Size *size,
    Image_AuxiliaryPictureType type, OH_AuxiliaryPictureNative **auxiliaryPicture);

/**
 * @brief Write pixels to auxiliary picture.
 *
 * @param auxiliaryPicture The AuxiliaryPicture pointer will be operated.
 * @param source The pixels will be written.
 * @param bufferSize The size of pixels.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} auxiliaryPicture is nullptr, or source is nullptr, or the bufferSize is invalid.
 *         {@link IMAGE_ALLOC_FAILED} memory alloc failed.
 *         {@link IMAGE_COPY_FAILED} memory copy failed.
 * @since 13
 */
Image_ErrorCode OH_AuxiliaryPictureNative_WritePixels(OH_AuxiliaryPictureNative *auxiliaryPicture, uint8_t *source,
    size_t bufferSize);

/**
 * @brief Read pixels from auxiliary picture.
 *
 * @param auxiliaryPicture The AuxiliaryPicture pointer will be operated.
 * @param destination The pixels will be read.
 * @param bufferSize The size of pixels for reading.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} auxiliaryPicture is nullptr, or destination is nullptr,
 *         or the bufferSize is invalid.
 *         {@link IMAGE_ALLOC_FAILED} memory alloc failed.
 *         {@link IMAGE_COPY_FAILED} memory copy failed.
 * @since 13
 */
Image_ErrorCode OH_AuxiliaryPictureNative_ReadPixels(OH_AuxiliaryPictureNative *auxiliaryPicture, uint8_t *destination,
    size_t *bufferSize);

/**
 * @brief Obtains the type of auxiliary picture.
 *
 * @param auxiliaryPicture The AuxiliaryPicture pointer will be operated.
 * @param type The type of auxiliary picture.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} auxiliaryPicture is nullptr, or type is nullptr.
 * @since 13
 */
Image_ErrorCode OH_AuxiliaryPictureNative_GetType(OH_AuxiliaryPictureNative *auxiliaryPicture,
    Image_AuxiliaryPictureType *type);

/**
 * @brief Obtains the info of auxiliary picture.
 *
 * @param auxiliaryPicture The AuxiliaryPicture pointer will be operated.
 * @param info The info of auxiliary picture.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} auxiliaryPicture is nullptr, or info is nullptr.
 * @since 13
 */
Image_ErrorCode OH_AuxiliaryPictureNative_GetInfo(OH_AuxiliaryPictureNative *auxiliaryPicture,
    OH_AuxiliaryPictureInfo **info);

/**
 * @brief Set auxiliary picture info.
 *
 * @param auxiliaryPicture The AuxiliaryPicture pointer will be operated.
 * @param info The info will be set.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} auxiliaryPicture is nullptr, or info is nullptr.
 * @since 13
 */
Image_ErrorCode OH_AuxiliaryPictureNative_SetInfo(OH_AuxiliaryPictureNative *auxiliaryPicture,
    OH_AuxiliaryPictureInfo *info);

/**
 * @brief Obtains the metadata of auxiliary picture.
 *
 * @param auxiliaryPicture The AuxiliaryPicture pointer will be operated.
 * @param metadataType The type of metadata.
 * @param metadata The metadata of auxiliary picture.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} auxiliaryPicture is nullptr, or metadata is nullptr.
 *         {@link IMAGE_UNSUPPORTED_METADATA} unsupported metadata type, or the metadata type does not match the
 *         auxiliary picture type.
 * @since 13
 */
Image_ErrorCode OH_AuxiliaryPictureNative_GetMetadata(OH_AuxiliaryPictureNative *auxiliaryPicture,
    Image_MetadataType metadataType, OH_PictureMetadata **metadata);

/**
 * @brief Set auxiliary picture metadata.
 *
 * @param auxiliaryPicture The AuxiliaryPicture pointer will be operated.
 * @param metadataType The type of metadata.
 * @param metadata The metadata will be set.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} auxiliaryPicture is nullptr, or metadata is nullptr.
 *         {@link IMAGE_UNSUPPORTED_METADATA} unsupported metadata type, or the metadata type does not match the
 *         auxiliary picture type.
 * @since 13
 */
Image_ErrorCode OH_AuxiliaryPictureNative_SetMetadata(OH_AuxiliaryPictureNative *auxiliaryPicture,
    Image_MetadataType metadataType, OH_PictureMetadata *metadata);

/**
 * @brief Releases this AuxiliaryPicture object.
 *
 * @param picture The Picture pointer will be operated.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} picture is nullptr.
 * @since 13
 */
Image_ErrorCode OH_AuxiliaryPictureNative_Release(OH_AuxiliaryPictureNative *picture);

/**
 * @brief Create a <b>AuxiliaryPictureInfo</b> object.
 *
 * @param info The AuxiliaryPictureInfo pointer will be operated.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} info is nullptr.
 * @since 13
 */
Image_ErrorCode OH_AuxiliaryPictureInfo_Create(OH_AuxiliaryPictureInfo **info);

/**
 * @brief Obtains the type of auxiliary picture info.
 *
 * @param info The AuxiliaryPictureInfo pointer will be operated.
 * @param type The type of auxiliary picture info.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} info is nullptr, or type is nullptr.
 * @since 13
 */
Image_ErrorCode OH_AuxiliaryPictureInfo_GetType(OH_AuxiliaryPictureInfo *info, Image_AuxiliaryPictureType *type);

/**
 * @brief Set auxiliary picture info type.
 *
 * @param info The AuxiliaryPictureInfo pointer will be operated.
 * @param type The type will be set.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} info is nullptr, or type is invalid.
 * @since 13
 */
Image_ErrorCode OH_AuxiliaryPictureInfo_SetType(OH_AuxiliaryPictureInfo *info, Image_AuxiliaryPictureType type);

/**
 * @brief Obtains the size of auxiliary picture info.
 *
 * @param info The AuxiliaryPictureInfo pointer will be operated.
 * @param size The size of auxiliary picture info.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} info is nullptr, or size is nullptr.
 * @since 13
 */
Image_ErrorCode OH_AuxiliaryPictureInfo_GetSize(OH_AuxiliaryPictureInfo *info, Image_Size *size);

/**
 * @brief Set auxiliary picture info size.
 *
 * @param info The AuxiliaryPictureInfo pointer will be operated.
 * @param size The size will be set.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} info is nullptr, or size is nullptr.
 * @since 13
 */
Image_ErrorCode OH_AuxiliaryPictureInfo_SetSize(OH_AuxiliaryPictureInfo *info, Image_Size *size);

/**
 * @brief Obtains the rowStride of auxiliary picture info.
 *
 * @param info The AuxiliaryPictureInfo pointer will be operated.
 * @param rowStride The rowStride of auxiliary picture info.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} info is nullptr, or rowStride is nullptr.
 * @since 13
 */
Image_ErrorCode OH_AuxiliaryPictureInfo_GetRowStride(OH_AuxiliaryPictureInfo *info, uint32_t *rowStride);

/**
 * @brief Set auxiliary picture info rowStride.
 *
 * @param info The AuxiliaryPictureInfo pointer will be operated.
 * @param rowStride The rowStride will be set.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} info is nullptr, or rowStride is nullptr.
 * @since 13
 */
Image_ErrorCode OH_AuxiliaryPictureInfo_SetRowStride(OH_AuxiliaryPictureInfo *info, uint32_t rowStride);

/**
 * @brief Obtains the pixelFormat of auxiliary picture info.
 *
 * @param info The AuxiliaryPictureInfo pointer will be operated.
 * @param pixelFormat The pixelFormat will be get.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} info is nullptr, or pixelFormat is nullptr.
 * @since 13
 */
Image_ErrorCode OH_AuxiliaryPictureInfo_GetPixelFormat(OH_AuxiliaryPictureInfo *info, PIXEL_FORMAT *pixelFormat);

/**
 * @brief Set auxiliary picture info pixelFormat.
 *
 * @param info The AuxiliaryPictureInfo pointer will be operated.
 * @param pixelFormat The pixelFormat will be set.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} info is nullptr.
 * @since 13
 */
Image_ErrorCode OH_AuxiliaryPictureInfo_SetPixelFormat(OH_AuxiliaryPictureInfo *info, PIXEL_FORMAT pixelFormat);

/**
 * @brief Releases this AuxiliaryPictureInfo object.
 *
 * @param info The AuxiliaryPictureInfo pointer will be operated.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} info is nullptr.
 * @since 13
 */
Image_ErrorCode OH_AuxiliaryPictureInfo_Release(OH_AuxiliaryPictureInfo *info);

#ifdef __cplusplus
};
#endif
/** @} */
#endif //INTERFACES_KITS_NATIVE_INCLUDE_IMAGE_PICTURE_NATIVE_H_