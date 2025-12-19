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
 * @brief Provides APIs for access to the image interface.
 *
 * @since 12
 */

/**
 * @file image_packer_native.h
 *
 * @brief Declares APIs for encoding image into data or file.
 *
 * @library libimage_packer.so
 * @kit ImageKit
 * @syscap SystemCapability.Multimedia.Image.ImagePacker
 * @since 12
 */

#ifndef INTERFACES_KITS_NATIVE_INCLUDE_IMAGE_IMAGE_PACKER_NATIVE_H_
#define INTERFACES_KITS_NATIVE_INCLUDE_IMAGE_IMAGE_PACKER_NATIVE_H_
#include "image_common.h"
#include "image_source_native.h"
#include "pixelmap_native.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @brief Define a ImagePacker struct type, used for ImagePacker pointer controls.
 *
 * @since 12
 */
struct OH_ImagePackerNative;
typedef struct OH_ImagePackerNative OH_ImagePackerNative;

/**
 * @brief Defines the image packing options.
 *
 * @since 12
 */
struct OH_PackingOptions;
typedef struct OH_PackingOptions OH_PackingOptions;

/**
 * @brief Defines the image sequence packing options.
 *
 * @since 18
 */
struct OH_PackingOptionsForSequence;

/**
 * @brief Defines the image sequence packing options.
 *
 * @since 18
 */
typedef struct OH_PackingOptionsForSequence OH_PackingOptionsForSequence;

/**
 * @brief Enumerates packing dynamic range.
 *
 * @since 12
 */
typedef enum {
    /*
    * Packing according to the content of the image.
    */
    IMAGE_PACKER_DYNAMIC_RANGE_AUTO = 0,
    /*
    * Packing to standard dynamic range.
    */
    IMAGE_PACKER_DYNAMIC_RANGE_SDR = 1,
} IMAGE_PACKER_DYNAMIC_RANGE;

/**
 * @brief Create a pointer for PackingOptions struct.
 *
 * @param options The PackingOptions pointer will be operated.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PackingOptions_Create(OH_PackingOptions **options);

/**
 * @brief Get mime type for OH_PackingOptions struct.
 *
 * @param options The OH_PackingOptions pointer will be operated.
 * @param format the number of image format.The user can pass in a null pointer and zero size, we will allocate memory,
 * but user must free memory after use.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PackingOptions_GetMimeType(OH_PackingOptions *options,
    Image_MimeType *format);

/**
 * @brief Gets MIME type from OH_PackingOptions. The output format.data is null-terminated.
 *
 * @param options The OH_PackingOptions pointer will be operated.
 * @param format MimeType set in the OH_PackingOptions.
 * @return Returns functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_PACKER_INVALID_PARAMETER} if options or format is nullptr.
 * @since 19
 */
Image_ErrorCode OH_PackingOptions_GetMimeTypeWithNull(OH_PackingOptions *options,
    Image_MimeType *format);

/**
 * @brief Set format number for OH_PackingOptions struct.
 *
 * @param options The OH_PackingOptions pointer will be operated.
 * @param format the number of image format.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PackingOptions_SetMimeType(OH_PackingOptions *options,
    Image_MimeType *format);

/**
 * @brief Get quality for OH_PackingOptions struct.
 *
 * @param options The OH_PackingOptions pointer will be operated.
 * @param quality The number of image quality.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PackingOptions_GetQuality(OH_PackingOptions *options,
    uint32_t *quality);

/**
 * @brief Set quality number for OH_PackingOptions struct.
 *
 * @param options The OH_PackingOptions pointer will be operated.
 * @param quality The number of image quality.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PackingOptions_SetQuality(OH_PackingOptions *options,
    uint32_t quality);

/**
 * @brief Get needsPackProperties for OH_PackingOptions struct.
 *
 * @param options The OH_PackingOptions pointer will be operated.
 * @param needsPackProperties Whether the image properties can be saved, like Exif.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PackingOptions_GetNeedsPackProperties(OH_PackingOptions *options,
    bool *needsPackProperties);

/**
 * @brief Set needsPackProperties for OH_PackingOptions struct.
 *
 * @param options The OH_PackingOptions pointer will be operated.
 * @param needsPackProperties Whether the image properties can be saved, like Exif.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PackingOptions_SetNeedsPackProperties(OH_PackingOptions *options,
    bool needsPackProperties);

/**
 * @brief Get desiredDynamicRange for PackingOptions struct.
 *
 * @param options The PackingOptions pointer will be operated. Pointer connot be null.
 * @param desiredDynamicRange The number of dynamic range {@link IMAGE_PACKER_DYNAMIC_RANGE}. Pointer connot be null.
 * @return Returns {@link Image_ErrorCode} IMAGE_SUCCESS - The operation is successful.
 * returns {@link Image_ErrorCode} IMAGE_BAD_PARAMETER - Parameter error.Possible causes:Parameter verification failed.
 * @since 12
 */
Image_ErrorCode OH_PackingOptions_GetDesiredDynamicRange(OH_PackingOptions *options, int32_t* desiredDynamicRange);

/**
 * @brief Set desiredDynamicRange number for PackingOptions struct.
 *
 * @param options The PackingOptions pointer will be operated. Pointer connot be null.
 * @param desiredDynamicRange The number of dynamic range {@link IMAGE_PACKER_DYNAMIC_RANGE}.
 * @return Returns {@link Image_ErrorCode} IMAGE_SUCCESS - The operation is successful.
 * returns {@link Image_ErrorCode} IMAGE_BAD_PARAMETER - Parameter error.Possible causes:Parameter verification failed.
 * @since 12
 */
Image_ErrorCode OH_PackingOptions_SetDesiredDynamicRange(OH_PackingOptions *options, int32_t desiredDynamicRange);

/**
 * @brief delete OH_PackingOptions pointer.
 *
 * @param options The OH_PackingOptions pointer will be operated.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_PackingOptions_Release(OH_PackingOptions *options);

/**
 * @brief Create a pointer for OH_PackingOptionsForSequence struct.
 *
 * @param options The OH_PackingOptionsForSequence pointer will be operated.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} options is nullptr.
 * @since 18
 */
Image_ErrorCode OH_PackingOptionsForSequence_Create(OH_PackingOptionsForSequence **options);

/**
 * @brief Set FrameCount number for OH_PackingOptionsForSequence struct.
 *
 * @param options The OH_PackingOptionsForSequence pointer will be operated.
 * @param frameCount The number of image frameCount.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} options is nullptr.
 * @since 18
 */
Image_ErrorCode OH_PackingOptionsForSequence_SetFrameCount(OH_PackingOptionsForSequence *options,
    uint32_t frameCount);

/**
 * @brief Get FrameCount number for OH_PackingOptionsForSequence struct.
 *
 * @param options The OH_PackingOptionsForSequence pointer will be operated.
 * @param frameCount The number of image frameCount.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} options or frameCount is nullptr.
 * @since 18
 */
Image_ErrorCode OH_PackingOptionsForSequence_GetFrameCount(OH_PackingOptionsForSequence *options,
    uint32_t *frameCount);

/**
 * @brief Set DelayTimeList number for OH_PackingOptionsForSequence struct.
 *
 * @param options The OH_PackingOptionsForSequence pointer will be operated.
 * @param delayTimeList The pointer of image delayTime list.
 * @param delayTimeListLength The number of image delayTimeListLength.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} options or delayTimeList is nullptr.
 * @since 18
 */
Image_ErrorCode OH_PackingOptionsForSequence_SetDelayTimeList(OH_PackingOptionsForSequence *options,
    int32_t *delayTimeList, size_t delayTimeListLength);

/**
 * @brief Get DelayTimeList number for OH_PackingOptionsForSequence struct.
 *
 * @param options The OH_PackingOptionsForSequence pointer will be operated.
 * @param delayTimeList The pointer of image delayTime list.
 * @param delayTimeListLength The number of image delayTimeListLength.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} options or delayTimeList is nullptr.
 * @since 18
 */
Image_ErrorCode OH_PackingOptionsForSequence_GetDelayTimeList(OH_PackingOptionsForSequence *options,
    int32_t *delayTimeList, size_t delayTimeListLength);

/**
 * @brief Set DisposalTypes number for OH_PackingOptionsForSequence struct.
 *
 * @param options The OH_PackingOptionsForSequence pointer will be operated.
 * @param disposalTypes The pointer of image disposalTypes.
 * @param disposalTypesLength The number of image disposalTypesLength.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} options or disposalTypes is nullptr.
 * @since 18
 */
Image_ErrorCode OH_PackingOptionsForSequence_SetDisposalTypes(OH_PackingOptionsForSequence *options,
    uint32_t *disposalTypes, size_t disposalTypesLength);

/**
 * @brief Get DisposalTypes number for OH_PackingOptionsForSequence struct.
 *
 * @param options The OH_PackingOptionsForSequence pointer will be operated.
 * @param disposalTypes The pointer of image disposalTypes.
 * @param disposalTypesLength The number of image disposalTypesLength.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} options or disposalTypes is nullptr.
 * @since 18
 */
Image_ErrorCode OH_PackingOptionsForSequence_GetDisposalTypes(OH_PackingOptionsForSequence *options,
    uint32_t *disposalTypes, size_t disposalTypesLength);

/**
 * @brief Set LoopCount number for OH_PackingOptionsForSequence struct.
 *
 * @param options The OH_PackingOptionsForSequence pointer will be operated.
 * @param loopCount The number of image loopCount.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} options is nullptr.
 * @since 18
 */
Image_ErrorCode OH_PackingOptionsForSequence_SetLoopCount(OH_PackingOptionsForSequence *options, uint32_t loopCount);

/**
 * @brief Get LoopCount number for OH_PackingOptionsForSequence struct.
 *
 * @param options The OH_PackingOptionsForSequence pointer will be operated.
 * @param loopCount The number of image loopCount.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} options or loopCount is nullptr.
 * @since 18
 */
Image_ErrorCode OH_PackingOptionsForSequence_GetLoopCount(OH_PackingOptionsForSequence *options, uint32_t *loopCount);

/**
 * @brief delete OH_PackingOptionsForSequence pointer.
 *
 * @param options The OH_PackingOptionsForSequence pointer will be operated.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} options is nullptr.
 * @since 18
 */
Image_ErrorCode OH_PackingOptionsForSequence_Release(OH_PackingOptionsForSequence *options);

/**
 * @brief Create a pointer for OH_ImagePackerNative struct.
 *
 * @param imagePacker The imagePacker to be created.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_ImagePackerNative_Create(OH_ImagePackerNative **imagePacker);

/**
 * @brief Encoding an <b>ImageSource</b> into the data with required format.
 *
 * @param imagePacker The imagePacker to use for packing.
 * @param options Indicates the encoding {@link OH_PackingOptions}.
 * @param imageSource The imageSource to be packed.
 * @param outData The output data buffer to store the packed image.
 * @param size A pointer to the size of the output data buffer.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_ImagePackerNative_PackToDataFromImageSource(OH_ImagePackerNative *imagePacker,
    OH_PackingOptions *options, OH_ImageSourceNative *imageSource, uint8_t *outData, size_t *size);

/**
 * @brief Encoding a <b>Pixelmap</b> into the data with required format.
 *
 * @param imagePacker The imagePacker to use for packing.
 * @param options Indicates the encoding {@link OH_PackingOptions}.
 * @param pixelmap The pixelmap to be packed.
 * @param outData The output data buffer to store the packed image.
 * @param size A pointer to the size of the output data buffer.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_ImagePackerNative_PackToDataFromPixelmap(OH_ImagePackerNative *imagePacker,
    OH_PackingOptions *options, OH_PixelmapNative *pixelmap, uint8_t *outData, size_t *size);

/**
 * @brief Encoding a <b>Picture</b> into the data with required format.
 *
 * @param imagePacker The imagePacker to use for packing.
 * @param options Indicates the encoding {@link OH_PackingOptions}.
 * @param picture The picture to be packed.
 * @param outData The output data buffer to store the packed image.
 * @param size A pointer to the size of the output data buffer.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} imagePacker is nullptr, or picture is nullptr, or outData is nullptr,
 *         or size is invalid.
 *         {@link IMAGE_ENCODE_FAILED} encode failed.
 * @since 13
 */
Image_ErrorCode OH_ImagePackerNative_PackToDataFromPicture(OH_ImagePackerNative *imagePacker,
    OH_PackingOptions *options, OH_PictureNative *picture, uint8_t *outData, size_t *size);

/**
 * @brief Encoding a <b>PixelMap</b> sequence into the data
 *
 * @param imagePacker The imagePacker to use for packing.
 * @param options Indicates the encoding {@link OH_PackingOptionsForSequence}.
 * @param pixelmapSequence The pixelmap sequence to be packed.
 * @param sequenceLength The pixelmap sequence size to be packed.
 * @param outData The output data buffer to store the packed image.
 * @param outDataSize A pointer to the size of the output data buffer.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} one of the pointer type parameters is nullptr, or size/length is invalid
 *         {@link IMAGE_ENCODE_FAILED} encode failed.
 * @since 18
 */
Image_ErrorCode OH_ImagePackerNative_PackToDataFromPixelmapSequence(OH_ImagePackerNative *imagePacker,
    OH_PackingOptionsForSequence *options, OH_PixelmapNative **pixelmapSequence,
    size_t sequenceLength, uint8_t *outData, size_t *outDataSize);

/**
 * @brief Encoding an <b>ImageSource</b> into the a file with fd with required format.
 *
 * @param imagePacker The image packer to use for packing.
 * @param options Indicates the encoding {@link OH_PackingOptions}.
 * @param imageSource The imageSource to be packed.
 * @param fd Indicates a writable file descriptor.
 * @return Returns {@link Image_ErrorCode}
 * @since 12
 */
Image_ErrorCode OH_ImagePackerNative_PackToFileFromImageSource(OH_ImagePackerNative *imagePacker,
    OH_PackingOptions *options, OH_ImageSourceNative *imageSource, int32_t fd);

/**
  * @brief Encoding a <b>Pixelmap</b> into the a file with fd with required format
  *
  * @param imagePacker The image packer to use for packing.
  * @param options Indicates the encoding {@link OH_PackingOptions}.
  * @param pixelmap The pixelmap to be packed.
  * @param fd Indicates a writable file descriptor.
  * @return Returns {@link Image_ErrorCode}
  * @since 12
 */
Image_ErrorCode OH_ImagePackerNative_PackToFileFromPixelmap(OH_ImagePackerNative *imagePacker,
    OH_PackingOptions *options, OH_PixelmapNative *pixelmap, int32_t fd);

/**
 * @brief Encoding a <b>Picture</b> into the a file with fd with required format.
 *
 * @param imagePacker The imagePacker to use for packing.
 * @param options Indicates the encoding {@link OH_PackingOptions}.
 * @param picture The picture to be packed.
 * @param fd Indicates a writable file descriptor.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} imagePacker is nullptr, or picture is nullptr, or fd is invalid.
 *         {@link IMAGE_ENCODE_FAILED} encode failed.
 * @since 13
 */
Image_ErrorCode OH_ImagePackerNative_PackToFileFromPicture(OH_ImagePackerNative *imagePacker,
    OH_PackingOptions *options, OH_PictureNative *picture, int32_t fd);

/**
  * @brief Encoding a <b>PixelMap</b> sequence into the a file with fd
  *
  * @param imagePacker The image packer to use for packing.
  * @param options Indicates the encoding {@link OH_PackingOptionsForSequence}.
  * @param pixelmapSequence The pixelmap sequence to be packed.
  * @param sequenceLength The pixelmap sequence size to be packed.
  * @param fd Indicates a writable file descriptor.
  * @return Image functions result code.
  *         {@link IMAGE_SUCCESS} if the execution is successful.
  *         {@link IMAGE_BAD_PARAMETER} one of the pointer type parameters is nullptr, or length is invalid
  *         {@link IMAGE_ENCODE_FAILED} encode failed.
  * @since 18
 */
Image_ErrorCode OH_ImagePackerNative_PackToFileFromPixelmapSequence(OH_ImagePackerNative *imagePacker,
    OH_PackingOptionsForSequence *options, OH_PixelmapNative **pixelmapSequence, size_t sequenceLength, int32_t fd);

/**
  * @brief Releases an imagePacker object.
  *
  * @param imagePacker A pointer to the image packer object to be released.
  * @return Returns {@link Image_ErrorCode}
  * @since 12
 */
Image_ErrorCode OH_ImagePackerNative_Release(OH_ImagePackerNative *imagePacker);

/**
  * @brief Obtains the supported image formats that can be encoded.
  *
  * @param supportedFormats Double pointer to an array of the supported image formats.
  * @param length Pointer to the length of the array.
  * @return One of the following result codes:
  *         {@link IMAGE_SUCCESS} if the execution is successful.
  *         {@link IMAGE_PACKER_INVALID_PARAMETER} if <b>supportedFormats</b> or <b>length</b> is empty.
  * @since 20
 */
Image_ErrorCode OH_ImagePackerNative_GetSupportedFormats(Image_MimeType** supportedFormats, size_t* length);

#ifdef __cplusplus
};
#endif
/** @} */
#endif // INTERFACES_KITS_NATIVE_INCLUDE_IMAGE_IMAGE_PACKER_NATIVE_H_