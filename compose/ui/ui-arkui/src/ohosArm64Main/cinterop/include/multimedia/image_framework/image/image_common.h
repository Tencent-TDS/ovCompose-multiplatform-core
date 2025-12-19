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
 * @file image_common.h
 *
 * @brief Declares the common enums and structs used by the image interface.
 *
 * @library libimage_common.so
 * @kit ImageKit
 * @syscap SystemCapability.Multimedia.Image.Core
 * @since 12
 */

#ifndef INTERFACES_KITS_NATIVE_INCLUDE_IMAGE_IMAGE_COMMON_H_
#define INTERFACES_KITS_NATIVE_INCLUDE_IMAGE_IMAGE_COMMON_H_
#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @brief Defines the image size.
 *
 * @since 12
 */
struct Image_Size {
    /** Image width, in pixels. */
    uint32_t width;
    /** Image height, in pixels. */
    uint32_t height;
};

/**
 * @brief Declaration the image size.
 *
 * @since 12
 */
typedef struct Image_Size Image_Size;

/**
 * @brief Defines the region of the image source to decode.
 *
 * @since 12
 */
struct Image_Region {
    /** X coordinate of the start point, in pixels. */
    uint32_t x;
    /** Y coordinate of the start point, in pixels. */
    uint32_t y;
    /** Width of the region, in pixels. */
    uint32_t width;
    /** Height of the region, in pixels. */
    uint32_t height;
};

/**
 * @brief Declaration the image region.
 *
 * @since 12
 */
typedef struct Image_Region Image_Region;

#ifdef __cplusplus
/**
 * @brief Defines the region of the image source to decode.
 *
 * @since 12
 */
struct Image_String {
    /** data for string type */
    char *data = nullptr;
    /** data lenth for string type */
    size_t size = 0;
};
#else
/**
 * @brief Defines the region of the image source to decode.
 *
 * @since 12
 */
struct Image_String {
    /** data for string type */
    char *data;
    /** data lenth for string type */
    size_t size;
};
#endif

/**
 * @brief Define a PictureMetadata struct type, used for picture metadata.
 *
 * @since 13
 */
struct OH_PictureMetadata;

/**
 * @brief Define a PictureMetadata struct type, used for picture metadata.
 *
 * @since 13
 */
typedef struct OH_PictureMetadata OH_PictureMetadata;

/**
 * @brief Defines the property string (in key-value format) of the image source.
 *
 * @since 12
 */
typedef struct Image_String Image_String;

/**
 * @brief Defines the image encode format.
 *
 * @since 12
 */
typedef struct Image_String Image_MimeType;

/**
 * @brief Enumerates the return values that may be used by the interface.
 *
 * @since 12
 */
typedef enum {
    /** operation success */
    IMAGE_SUCCESS = 0,
    /** invalid parameter */
    IMAGE_BAD_PARAMETER = 401,
    /** unsupported mime type */
    IMAGE_UNSUPPORTED_MIME_TYPE = 7600101,
    /** unknown mime type */
    IMAGE_UNKNOWN_MIME_TYPE = 7600102,
    /** too large data or image */
    IMAGE_TOO_LARGE = 7600103,
    /** @error DMA memory does not exist */
    IMAGE_DMA_NOT_EXIST = 7600173,
    /** @error DMA operation failed */
    IMAGE_DMA_OPERATION_FAILED = 7600174,
    /** unsupported operations */
    IMAGE_UNSUPPORTED_OPERATION = 7600201,
    /** unsupported metadata */
    IMAGE_UNSUPPORTED_METADATA = 7600202,
    /** unsupported conversion */
    IMAGE_UNSUPPORTED_CONVERSION = 7600203,
    /** invalid region */
    IMAGE_INVALID_REGION = 7600204,
    /**
     *  @error unsupported memory format
     *  @since 13
     */
    IMAGE_UNSUPPORTED_MEMORY_FORMAT = 7600205,
    /**
     * @error Invalid parameter.
     * @since 19
     */
    IMAGE_INVALID_PARAMETER = 7600206,
    /** failed to allocate memory */
    IMAGE_ALLOC_FAILED = 7600301,
    /** memory copy failed */
    IMAGE_COPY_FAILED = 7600302,
    /**
     * @error memory lock or unlock failed
     * @since 15
     */
    IMAGE_LOCK_UNLOCK_FAILED = 7600303,
    /**
     * @error unsupported allocator mode, e.g., use share memory to create a HDR image as only
     * DMA supported hdr metadata.
     * @since 20
     */
    IMAGE_ALLOCATOR_MODE_UNSUPPORTED = 7600501,
    /** unknown error */
    IMAGE_UNKNOWN_ERROR = 7600901,
    /** decode data source exception */
    IMAGE_BAD_SOURCE = 7700101,
    /**
     * @error unsupported mime type
     * @since 15
     */
    IMAGE_SOURCE_UNSUPPORTED_MIME_TYPE = 7700102,
    /**
     * @error image to large
     * @since 15
     */
    IMAGE_SOURCE_TOO_LARGE = 7700103,
    /**
     * @error unsupported allocator type, e.g., use share memory to decode a HDR image as only
     * DMA supported hdr metadata.
     * @since 15
     */
    IMAGE_SOURCE_UNSUPPORTED_ALLOCATOR_TYPE = 7700201,
    /* @error unsupported options, e.g, cannot convert image into desired pixel format.
     * @since 15
     */
    IMAGE_SOURCE_UNSUPPORTED_OPTIONS = 7700203,
    /**
     * @error Invalid parameter.
     * @since 19
     */
    IMAGE_SOURCE_INVALID_PARAMETER = 7700204,
    /** decode failed */
    IMAGE_DECODE_FAILED = 7700301,
    /**
     * @error memory allocation failed
     * @since 15
     */
    IMAGE_SOURCE_ALLOC_FAILED = 7700302,
    /**
     * @error Invalid parameter for ImagePacker.
     * @since 19
     */
    IMAGE_PACKER_INVALID_PARAMETER = 7800202,
    /** encode failed */
    IMAGE_ENCODE_FAILED = 7800301,
    /**
     * @error Invalid parameter for ImageReceiver.
     * @since 20
     */
    IMAGE_RECEIVER_INVALID_PARAMETER = 7900201,
} Image_ErrorCode;

/**
 * @brief Define the metadata type.
 *
 * @since 13
 */
typedef enum {
    /**
     * EXIF metadata.
     */
    EXIF_METADATA = 1,

    /**
     * Fragment metadata.
     */
    FRAGMENT_METADATA = 2,

    /**
     * Metadata of a GIF image.
     *
     * @since 20
     */
    GIF_METADATA = 5
} Image_MetadataType;

/**
 * @brief Type of allocator used to allocate memory of a PixelMap.
 *
 * @since 20
 */
typedef enum {
    /**
     * The system determines which memory to use to create the PixelMap.
     *
     * @since 20
     */
    IMAGE_ALLOCATOR_MODE_AUTO = 0,
    /**
     * Use DMA buffer to create the PixelMap.
     *
     * @since 20
     */
    IMAGE_ALLOCATOR_MODE_DMA = 1,
    /**
     * Use share memory to create the PixelMap.
     *
     * @since 20
     */
    IMAGE_ALLOCATOR_MODE_SHARED_MEMORY = 2,
} IMAGE_ALLOCATOR_MODE;

/**
 * @brief Creates a <b>PictureMetadata</b> object.
 *
 * @param metadataType The type of metadata.
 * @param metadata The PictureMetadata pointer will be operated.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} metadata is nullptr.
 * @since 13
 */
Image_ErrorCode OH_PictureMetadata_Create(Image_MetadataType metadataType, OH_PictureMetadata **metadata);

/**
 * @brief Obtains the property of picture metadata.
 *
 * @param metadata The PictureMetadata pointer will be operated.
 * @param key The property's key.
 * @param value The property's value.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} metadata is nullptr, or key is nullptr, or value is nullptr.
 *         {@link IMAGE_UNSUPPORTED_METADATA} unsupported metadata type, or the metadata type does not match the
 *         auxiliary picture type.
 * @since 13
 */
Image_ErrorCode OH_PictureMetadata_GetProperty(OH_PictureMetadata *metadata, Image_String *key, Image_String *value);

/**
 * @brief Set picture metadata property.
 *
 * @param metadata The PictureMetadata pointer will be operated.
 * @param key The property's key.
 * @param value The property's value.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} metadata is nullptr, or key is nullptr, or value is nullptr.
 *         {@link IMAGE_UNSUPPORTED_METADATA} unsupported metadata type, or the metadata type does not match the
 *         auxiliary picture type.
 * @since 13
 */
Image_ErrorCode OH_PictureMetadata_SetProperty(OH_PictureMetadata *metadata, Image_String *key, Image_String *value);
 
/**
 * @brief Obtains the property of picture metadata. The output value.data is null-terminated. 
 *
 * @param metadata Pointer to OH_PictureMetadata.
 * @param key Pointer to property's key.
 * @param value Pointer to property's value. Output parameter.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_INVALID_PARAMETER} metadata is nullptr, or key is nullptr, or value is nullptr.
 *         {@link IMAGE_UNSUPPORTED_METADATA} unsupported metadata type, or the metadata type does not match the
 *         auxiliary picture type.
 * @since 19
 */
Image_ErrorCode OH_PictureMetadata_GetPropertyWithNull(OH_PictureMetadata *metadata, Image_String *key, Image_String *value);

/**
 * @brief Releases this PictureMetadata object.
 *
 * @param metadata The PictureMetadata pointer will be operated.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} metadata is nullptr.
 * @since 13
 */
Image_ErrorCode OH_PictureMetadata_Release(OH_PictureMetadata *metadata);

/**
 * @brief Obtains a clone of metadata.
 *
 * @param oldMetadata The PictureMetadata pointer will be operated.
 * @param newMetadata The PictureMetadata pointer will be cloned.
 * @return Image functions result code.
 *         {@link IMAGE_SUCCESS} if the execution is successful.
 *         {@link IMAGE_BAD_PARAMETER} metadata is nullptr.
 *         {@link IMAGE_ALLOC_FAILED} memory alloc failed.
 *         {@link IMAGE_COPY_FAILED} memory copy failed.
 * @since 13
 */
Image_ErrorCode OH_PictureMetadata_Clone(OH_PictureMetadata *oldMetadata, OH_PictureMetadata **newMetadata);

/**
 * @brief Defines the bmp mime type.
 *
 * @since 12
 */
static const char* MIME_TYPE_BMP = "image/bmp";

/**
 * @brief Defines the jpeg mime type.
 *
 * @since 12
 */
static const char* MIME_TYPE_JPEG = "image/jpeg";

/**
 * @brief Defines the heic mime type.
 *
 * @since 12
 */
static const char* MIME_TYPE_HEIC = "image/heic";

/**
 * @brief Defines the png mime type.
 *
 * @since 12
 */
static const char* MIME_TYPE_PNG = "image/png";

/**
 * @brief Defines the webp mime type.
 *
 * @since 12
 */
static const char* MIME_TYPE_WEBP = "image/webp";

/**
 * @brief Defines the gif mime type.
 *
 * @since 12
 */
static const char* MIME_TYPE_GIF = "image/gif";

/**
 * @brief Defines the x-icon mime type.
 *
 * @since 12
 */
static const char* MIME_TYPE_ICON = "image/x-icon";

/**
 * @brief Defines a pointer to bits per sample, one of the image properties.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_BITS_PER_SAMPLE = "BitsPerSample";

/**
 * @brief Defines a pointer to the orientation, one of the image properties.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_ORIENTATION = "Orientation";

/**
 * @brief Defines a pointer to the image length, one of the image properties.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_IMAGE_LENGTH = "ImageLength";

/**
 * @brief Defines a pointer to the image width, one of the image properties.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_IMAGE_WIDTH = "ImageWidth";

/**
 * @brief Defines a pointer to the GPS latitude, one of the image properties.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_LATITUDE = "GPSLatitude";

/**
 * @brief Defines a pointer to the GPS longitude, one of the image properties.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_LONGITUDE = "GPSLongitude";

/**
 * @brief Defines a pointer to the GPS latitude reference information, one of the image properties.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_LATITUDE_REF = "GPSLatitudeRef";

/**
 * @brief Defines a pointer to the GPS longitude reference information, one of the image properties.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_LONGITUDE_REF = "GPSLongitudeRef";

/**
 * @brief Defines a pointer to the created date and time, one of the image properties.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_DATE_TIME_ORIGINAL = "DateTimeOriginal";

/**
 * @brief Defines a pointer to the exposure time, one of the image properties.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_EXPOSURE_TIME = "ExposureTime";

/**
 * @brief Defines a pointer to the scene type, one of the image properties.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_SCENE_TYPE = "SceneType";

/**
 * @brief Defines a pointer to the ISO speed ratings, one of the image properties.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_ISO_SPEED_RATINGS = "ISOSpeedRatings";

/**
 * @brief Defines a pointer to the f-number of the image, one of the image properties.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_F_NUMBER = "FNumber";

/**
 * @brief Defines a pointer to the compressed bits per pixel, one of the image properties.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_COMPRESSED_BITS_PER_PIXEL = "CompressedBitsPerPixel";

/**
 * @brief The scheme used for image compression.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_COMPRESSION = "Compression";

/**
 * @brief Pixel composition, such as RGB or YCbCr.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_PHOTOMETRIC_INTERPRETATION = "PhotometricInterpretation";

/**
 * @brief For each strip, the byte offset of that strip.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_STRIP_OFFSETS = "StripOffsets";

/**
 * @brief The number of components per pixel.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_SAMPLES_PER_PIXEL = "SamplesPerPixel";

/**
 * @brief The number of rows per strip of image data.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_ROWS_PER_STRIP = "RowsPerStrip";

/**
 * @brief The total number of bytes in each strip of image data.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_STRIP_BYTE_COUNTS = "StripByteCounts";

/**
 * @brief The image resolution in the width direction.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_X_RESOLUTION = "XResolution";

/**
 * @brief The image resolution in the height direction.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_Y_RESOLUTION = "YResolution";

/**
 * @brief Indicates whether pixel components are recorded in a chunky or planar format.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_PLANAR_CONFIGURATION = "PlanarConfiguration";

/**
 * @brief The unit used to measure XResolution and YResolution.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_RESOLUTION_UNIT = "ResolutionUnit";

/**
 * @brief The transfer function for the image, typically used for color correction.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_TRANSFER_FUNCTION = "TransferFunction";

/**
 * @brief The name and version of the software used to generate the image.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_SOFTWARE = "Software";

/**
 * @brief The name of the person who created the image.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_ARTIST = "Artist";

/**
 * @brief The chromaticity of the white point of the image.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_WHITE_POINT = "WhitePoint";

/**
 * @brief The chromaticity of the primary colors of the image.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_PRIMARY_CHROMATICITIES = "PrimaryChromaticities";

/**
 * @brief The matrix coefficients for transformation from RGB to YCbCr image data.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_YCBCR_COEFFICIENTS = "YCbCrCoefficients";

/**
 * @brief The sampling ratio of chrominance components to the luminance component.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_YCBCR_SUB_SAMPLING = "YCbCrSubSampling";

/**
 * @brief The position of chrominance components in relation to the luminance component.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_YCBCR_POSITIONING = "YCbCrPositioning";

/**
 * @brief The reference black point value and reference white point value.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_REFERENCE_BLACK_WHITE = "ReferenceBlackWhite";

/**
 * @brief Copyright information for the image.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_COPYRIGHT = "Copyright";

/**
 * @brief The offset to the start byte (SOI) of JPEG compressed thumbnail data.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_JPEG_INTERCHANGE_FORMAT = "JPEGInterchangeFormat";

/**
 * @brief The number of bytes of JPEG compressed thumbnail data.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_JPEG_INTERCHANGE_FORMAT_LENGTH = "JPEGInterchangeFormatLength";

/**
 * @brief The class of the program used by the camera to set exposure when the picture is taken.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_EXPOSURE_PROGRAM = "ExposureProgram";

/**
 * @brief Indicates the spectral sensitivity of each channel of the camera used.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_SPECTRAL_SENSITIVITY = "SpectralSensitivity";

/**
 * @brief Indicates the Opto-Electric Conversion Function (OECF) specified in ISO 14524.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_OECF = "OECF";

/**
 * @brief The version of the Exif standard supported.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_EXIF_VERSION = "ExifVersion";

/**
 * @brief The date and time when the image was stored as digital data.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_DATE_TIME_DIGITIZED = "DateTimeDigitized";

/**
 * @brief Information specific to compressed data.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_COMPONENTS_CONFIGURATION = "ComponentsConfiguration";

/**
 * @brief The shutter speed, expressed as an APEX (Additive System of Photographic Exposure) value.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_SHUTTER_SPEED_VALUE = "ShutterSpeedValue";

/**
 * @brief The brightness value of the image, in APEX units.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_BRIGHTNESS_VALUE = "BrightnessValue";

/**
 * @brief The smallest F number of lens.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_MAX_APERTURE_VALUE = "MaxApertureValue";

/**
 * @brief The distance to the subject, measured in meters.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_SUBJECT_DISTANCE = "SubjectDistance";

/**
 * @brief This tag indicate the location and area of the main subject in the overall scene.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_SUBJECT_AREA = "SubjectArea";

/**
 * @brief A tag for manufacturers of Exif/DCF writers to record any desired infomation.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_MAKER_NOTE = "MakerNote";

/**
 * @brief A tag for record fractions of seconds for the DateTime tag.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_SUBSEC_TIME = "SubsecTime";

/**
 * @brief A tag used to record fractions of seconds for the DateTimeOriginal tag.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_SUBSEC_TIME_ORIGINAL = "SubsecTimeOriginal";

/**
 * @brief A tag used to record fractions of seconds for the DateTimeDigitized tag.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_SUBSEC_TIME_DIGITIZED = "SubsecTimeDigitized";

/**
 * @brief This tag denotes the Flashpix format version supported by an FPXR file, enhancing device compatibility.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_FLASHPIX_VERSION = "FlashpixVersion";

/**
 * @brief The color space information tag, often recorded as the color space specifier.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_COLOR_SPACE = "ColorSpace";

/**
 * @brief The name of an audio file related to the image data.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_RELATED_SOUND_FILE = "RelatedSoundFile";

/**
 * @brief Strobe energy at image capture, in BCPS.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_FLASH_ENERGY = "FlashEnergy";

/**
 * @brief Camera or input device spatial frequency table.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_SPATIAL_FREQUENCY_RESPONSE = "SpatialFrequencyResponse";

/**
 * @brief Pixels per FocalPlaneResolutionUnit in the image width.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_FOCAL_PLANE_X_RESOLUTION = "FocalPlaneXResolution";

/**
 * @brief Pixels per FocalPlaneResolutionUnit in the image height.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_FOCAL_PLANE_Y_RESOLUTION = "FocalPlaneYResolution";

/**
 * @brief Unit for measuring FocalPlaneXResolution and FocalPlaneYResolution.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_FOCAL_PLANE_RESOLUTION_UNIT = "FocalPlaneResolutionUnit";

/**
 * @brief Location of the main subject, relative to the left edge.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_SUBJECT_LOCATION = "SubjectLocation";

/**
 * @brief Selected exposure index at capture.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_EXPOSURE_INDEX = "ExposureIndex";

/**
 * @brief Image sensor type on the camera.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_SENSING_METHOD = "SensingMethod";

/**
 * @brief Indicates the image source.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_FILE_SOURCE = "FileSource";

/**
 * @brief Color filter array (CFA) geometric pattern of the image sensor.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_CFA_PATTERN = "CFAPattern";

/**
 * @brief Indicates special processing on image data.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_CUSTOM_RENDERED = "CustomRendered";

/**
 * @brief Exposure mode set when the image was shot.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_EXPOSURE_MODE = "ExposureMode";

/**
 * @brief Digital zoom ratio at the time of capture.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_DIGITAL_ZOOM_RATIO = "DigitalZoomRatio";

/**
 * @brief Type of scene captured.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_SCENE_CAPTURE_TYPE = "SceneCaptureType";

/**
 * @brief Degree of overall image gain adjustment.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GAIN_CONTROL = "GainControl";

/**
 * @brief Direction of contrast processing applied by the camera.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_CONTRAST = "Contrast";

/**
 * @brief Direction of saturation processing applied by the camera.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_SATURATION = "Saturation";

/**
 * @brief The direction of sharpness processing applied by the camera.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_SHARPNESS = "Sharpness";

/**
 * @brief Information on picture-taking conditions for a specific camera model.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_DEVICE_SETTING_DESCRIPTION = "DeviceSettingDescription";

/**
 * @brief Indicates the distance range to the subject.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_SUBJECT_DISTANCE_RANGE = "SubjectDistanceRange";

/**
 * @brief An identifier uniquely assigned to each image.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_IMAGE_UNIQUE_ID = "ImageUniqueID";

/**
 * @brief The version of the GPSInfoIFD.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_VERSION_ID = "GPSVersionID";

/**
 * @brief Reference altitude used for GPS altitude.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_ALTITUDE_REF = "GPSAltitudeRef";

/**
 * @brief The altitude based on the reference in GPSAltitudeRef.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_ALTITUDE = "GPSAltitude";

/**
 * @brief The GPS satellites used for measurements.
 * Used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_SATELLITES = "GPSSatellites";

/**
 * @brief The status of the GPS receiver when the image is recorded.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_STATUS = "GPSStatus";

/**
 * @brief The GPS measurement mode.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_MEASURE_MODE = "GPSMeasureMode";

/**
 * @brief The GPS DOP (data degree of precision).
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_DOP = "GPSDOP";

/**
 * @brief The unit used to express the GPS receiver speed of movement.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_SPEED_REF = "GPSSpeedRef";

/**
 * @brief The speed of GPS receiver movement.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_SPEED = "GPSSpeed";

/**
 * @brief The reference for giving the direction of GPS receiver movement.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_TRACK_REF = "GPSTrackRef";

/**
 * @brief The direction of GPS receiver movement.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_TRACK = "GPSTrack";

/**
 * @brief The reference for the image's direction.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_IMG_DIRECTION_REF = "GPSImgDirectionRef";

/**
 * @brief The direction of the image when captured.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_IMG_DIRECTION = "GPSImgDirection";

/**
 * @brief Geodetic survey data used by the GPS receiver.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_MAP_DATUM = "GPSMapDatum";

/**
 * @brief Indicates the latitude reference of the destination point.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_DEST_LATITUDE_REF = "GPSDestLatitudeRef";

/**
 * @brief The latitude of the destination point.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_DEST_LATITUDE = "GPSDestLatitude";

/**
 * @brief Indicates the longitude reference of the destination point.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_DEST_LONGITUDE_REF = "GPSDestLongitudeRef";

/**
 * @brief A character string recording the name of the method used for location finding.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_PROCESSING_METHOD = "GPSProcessingMethod";

/**
 * @brief A character string recording the name of the GPS area.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_AREA_INFORMATION = "GPSAreaInformation";

/**
 * @brief This field denotes if differential correction was applied to GPS data, crucial for precise location accuracy.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_DIFFERENTIAL = "GPSDifferential";

/**
 * @brief The serial number of the camera body.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_BODY_SERIAL_NUMBER = "BodySerialNumber";

/**
 * @brief The name of the camera owner.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_CAMERA_OWNER_NAME = "CameraOwnerName";

/**
 * @brief The name of the camera owner.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_COMPOSITE_IMAGE = "CompositeImage";

/**
 * @brief The DNGVersion tag encodes the four-tier version number for DNG specification compliance.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_DNG_VERSION = "DNGVersion";

/**
 * @brief The longitude of the destination point.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_DEST_LONGITUDE = "GPSDestLongitude";

/**
 * @brief The reference for the bearing to the destination point.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_DEST_BEARING_REF = "GPSDestBearingRef";

/**
 * @brief The bearing to the destination point.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_DEST_BEARING = "GPSDestBearing";

/**
 * @brief The measurement unit for the distance to the target point.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_DEST_DISTANCE_REF = "GPSDestDistanceRef";

/**
 * @brief The distance to the destination point.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_DEST_DISTANCE = "GPSDestDistance";

/**
 * @brief DefaultCropSize specifies the final image size in raw coordinates, accounting for extra edge pixels.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_DEFAULT_CROP_SIZE = "DefaultCropSize";

/**
 * @brief Indicates the value of coefficient gamma.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GAMMA = "Gamma";

/**
 * @brief The tag indicate the ISO speed latitude yyy value of the camera or input device that is defined in ISO 12232.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_ISO_SPEED_LATITUDEYYY = "ISOSpeedLatitudeyyy";

/**
 * @brief The tag indicate the ISO speed latitude zzz value of the camera or input device that is defined in ISO 12232.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_ISO_SPEED_LATITUDEZZZ = "ISOSpeedLatitudezzz";

/**
 * @brief The manufacturer of the lens.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_LENS_MAKE = "LensMake";

/**
 * @brief The model name of the lens.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_LENS_MODEL = "LensModel";

/**
 * @brief The serial number of the lens.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_LENS_SERIAL_NUMBER = "LensSerialNumber";

/**
 * @brief Specifications of the lens used.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_LENS_SPECIFICATION = "LensSpecification";

/**
 * @brief This tag provides a broad description of the data type in this subfile.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_NEW_SUBFILE_TYPE = "NewSubfileType";

/**
 * @brief This tag records the UTC offset for the DateTime tag, ensuring accurate timestamps regardless of location.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_OFFSET_TIME = "OffsetTime";

/**
 * @brief This tag logs the UTC offset when the image was digitized, aiding in accurate timestamp adjustment.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_OFFSET_TIME_DIGITIZED = "OffsetTimeDigitized";

/**
 * @brief This tag records the UTC offset when the original image was created, crucial for time-sensitive applications.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_OFFSET_TIME_ORIGINAL = "OffsetTimeOriginal";

/**
 * @brief Exposure times of source images for a composite image.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_SOURCE_EXPOSURE_TIMES_OF_COMPOSITE_IMAGE = "SourceExposureTimesOfCompositeImage";

/**
 * @brief The number of source images used for a composite image.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_SOURCE_IMAGE_NUMBER_OF_COMPOSITE_IMAGE = "SourceImageNumberOfCompositeImage";

/**
 * @brief This deprecated field signifies the type of data in this subfile. Use the NewSubfileType field instead.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_SUBFILE_TYPE = "SubfileType";

/**
 * @brief This tag indicates horizontal positioning errors in meters.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GPS_H_POSITIONING_ERROR = "GPSHPositioningError";

/**
 * @brief This tag indicates the sensitivity of the camera or input device when the image was shot.
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_PHOTOGRAPHIC_SENSITIVITY = "PhotographicSensitivity";

/**
 * @brief Burst Number
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_BURST_NUMBER = "HwMnoteBurstNumber";

/**
 * @brief Face Conf
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_FACE_CONF = "HwMnoteFaceConf";

/**
 * @brief Face Leye Center
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_FACE_LEYE_CENTER = "HwMnoteFaceLeyeCenter";

/**
 * @brief Face Mouth Center
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_FACE_MOUTH_CENTER = "HwMnoteFaceMouthCenter";

/**
 * @brief Face Pointer
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_FACE_POINTER = "HwMnoteFacePointer";

/**
 * @brief Face Rect
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_FACE_RECT = "HwMnoteFaceRect";

/**
 * @brief Face Reye Center
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_FACE_REYE_CENTER = "HwMnoteFaceReyeCenter";

/**
 * @brief Face Smile Score
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_FACE_SMILE_SCORE = "HwMnoteFaceSmileScore";

/**
 * @brief Face Version
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_FACE_VERSION = "HwMnoteFaceVersion";

/**
 * @brief Front Camera
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_FRONT_CAMERA = "HwMnoteFrontCamera";

/**
 * @brief Scene Pointer
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_SCENE_POINTER = "HwMnoteScenePointer";

/**
 * @brief Scene Version
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_SCENE_VERSION = "HwMnoteSceneVersion";

/**
 * @brief Is Xmage Supported
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_IS_XMAGE_SUPPORTED = "HwMnoteIsXmageSupported";

/**
 * @brief Xmage Mode
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_XMAGE_MODE = "HwMnoteXmageMode";

/**
 * @brief Xmage X1 Coordinate
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_XMAGE_LEFT = "HwMnoteXmageLeft";

/**
 * @brief Xmage Y1 Coordinate
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_XMAGE_TOP = "HwMnoteXmageTop";

/**
 * @brief Xmage X2 Coordinate
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_XMAGE_RIGHT = "HwMnoteXmageRight";

/**
 * @brief Xmage Y2 Coordinate
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_XMAGE_BOTTOM = "HwMnoteXmageBottom";

/**
 * @brief Cloud Enhancement Mode
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_CLOUD_ENHANCEMENT_MODE = "HwMnoteCloudEnhancementMode";

/**
 * @brief Wind Snapshot Mode
 * It is used in {@link OH_ImageSource_GetImageProperty} and {@link OH_ImageSource_ModifyImageProperty}.
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_WIND_SNAPSHOT_MODE = "HwMnoteWindSnapshotMode";


/**
 * @brief Gif Loop Count
 * It is used in {@link OH_ImageSource_GetImageProperty}.
 * If infinite loop returns 0, other values represent the number of loops
 *
 * @since 12
 */
static const char *OHOS_IMAGE_PROPERTY_GIF_LOOP_COUNT = "GIFLoopCount";

/**
 * @brief X in original
 * It is used in {@link OH_ImageSource_GetImageProperty}.
 * The top left corner of the fragment image is at the X-coordinate of the original image
 *
 * @since 13
 */
static const char *OHOS_IMAGE_PROPERTY_X_IN_ORIGINAL = "XInOriginal";

/**
 * @brief Y in original
 * It is used in {@link OH_ImageSource_GetImageProperty}.
 * The top left corner of the fragment image is at the Y-coordinate of the original image
 *
 * @since 13
 */
static const char *OHOS_IMAGE_PROPERTY_Y_IN_ORIGINAL = "YInOriginal";

/**
 * @brief Fragment map width
 * It is used in {@link OH_ImageSource_GetImageProperty}.
 * The width of the fragment image
 *
 * @since 13
 */
static const char *OHOS_IMAGE_PROPERTY_FRAGMENT_WIDTH = "FragmentImageWidth";

/**
 * @brief Fragment map height
 * It is used in {@link OH_ImageSource_GetImageProperty}.
 * The height of the fragment image
 *
 * @since 13
 */
static const char *OHOS_IMAGE_PROPERTY_FRAGMENT_HEIGHT = "FragmentImageHeight";

/**
 * @brief Delay of each frame in a GIF image, in milliseconds.
 *
 * @since 20
 */
static const char *IMAGE_PROPERTY_GIF_DELAY_TIME = "GifDelayTime";

/**
 * @brief Disposal type of each frame in a GIF image.
 *
 * @since 20
 */
static const char *IMAGE_PROPERTY_GIF_DISPOSAL_TYPE = "GifDisposalType";
#ifdef __cplusplus
};
#endif
/** @} */

#endif // INTERFACES_KITS_NATIVE_INCLUDE_IMAGE_IMAGE_COMMON_H_