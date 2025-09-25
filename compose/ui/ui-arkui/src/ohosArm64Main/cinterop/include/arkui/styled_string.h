/*
 * Copyright (c) 2024 Huawei Device Co., Ltd.
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
 * @addtogroup ArkUI_NativeModule
 * @{
 *
 * @brief Provides ArkUI UI capabilities on the Native side, such as UI component creation and destruction,
 * tree node operation, property setting, event monitoring, and so on.
 *
 * @since 12
 */

/**
 * @file styled_string.h
 *
 * @brief Provides ArkUI with property string capabilities on the Native side.
 *
 * @library libace_ndk.z.so
 * @syscap SystemCapability.ArkUI.ArkUI.Full
 * @kit ArkUI
 * @since 12
 */

#ifndef ARKUI_NATIVE_STYLED_STRING_H
#define ARKUI_NATIVE_STYLED_STRING_H

#include "native_drawing/drawing_text_declaration.h"
#include "native_drawing/drawing_text_typography.h"
#include "native_type.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @brief Defines formatted string data objects supported by the text component.
 *
 * @since 12
 */
typedef struct ArkUI_StyledString ArkUI_StyledString;

/**
 * @brief Creates a pointer to the ArkUI_StyledString object.
 *
 * @param style A pointer to OH_Drawing_TypographyStyle, obtained by {@link OH_Drawing_CreateTypographyStyle}.
 * @param collection A pointer to OH_Drawing_FontCollection, obtained by {@link OH_Drawing_CreateFontCollection}.
 * @return Creates a pointer to the ArkUI_StyledString object. If the object returns a null pointer,
 *         the creation failed, either because the address space was full,
 *         or because the style, collection parameter was an exception such as a null pointer.
 * @since 12
 */
ArkUI_StyledString* OH_ArkUI_StyledString_Create(
    OH_Drawing_TypographyStyle* style, OH_Drawing_FontCollection* collection);

/**
 * @brief Free the memory occupied by the ArkUI_StyledString object.
 *
 * @param handle A pointer to the ArkUI_StyledString object.
 * @since 12
 */
void OH_ArkUI_StyledString_Destroy(ArkUI_StyledString* handle);

/**
 * @brief Sets the new layout style to the top of the current format string style stack.
 *
 * @param handle A pointer to the ArkUI_StyledString object.
 * @param style A pointer to the OH_Drawing_TextStyle object.
 * @since 12
 */
void OH_ArkUI_StyledString_PushTextStyle(ArkUI_StyledString* handle, OH_Drawing_TextStyle* style);

/**
 * @brief Sets the corresponding text content based on the current format string style.
 *
 * @param handle A pointer to the ArkUI_StyledString object.
 * @param content A pointer to the text content.
 * @since 12
 */
void OH_ArkUI_StyledString_AddText(ArkUI_StyledString* handle, const char* content);

/**
 * @brief Removes the top style from the stack in the current format string object.
 *
 * @param handle A pointer to the ArkUI_StyledString object.
 * @since 12
 */
void OH_ArkUI_StyledString_PopTextStyle(ArkUI_StyledString* handle);

/**
 * @brief Creates a pointer to an OH_Drawing_Typography object based on a format string object
 * for advanced text estimation and typography.
 *
 * @param handle A pointer to the ArkUI_StyledString object.
 * @return A pointer to the OH_Drawing_Typography object. If the object returns a null pointer,
 *         the creation fails because the handle parameter is abnormal, such as a null pointer.
 * @since 12
 */
OH_Drawing_Typography* OH_ArkUI_StyledString_CreateTypography(ArkUI_StyledString* handle);

/**
 * @brief Set the placeholder.
 *
 * @param handle A pointer to the ArkUI_StyledString object.
 * @param placeholder A pointer to the OH_Drawing_PlaceholderSpan object.
 * @since 12
 */
void OH_ArkUI_StyledString_AddPlaceholder(ArkUI_StyledString* handle, OH_Drawing_PlaceholderSpan* placeholder);

/**
 * @brief Creates an <b>ArkUI_StyledString_Descriptor</b> object.
 *
 * @return Returns the pointer to the <b>ArkUI_StyledString_Descriptor</b> object created.
 * @since 14
 */
ArkUI_StyledString_Descriptor* OH_ArkUI_StyledString_Descriptor_Create(void);

/**
 * @brief Destroys an <b>ArkUI_StyledString_Descriptor</b> object and reclaims the memory occupied by the object.
 *
 * @param descriptor Pointer to an <b>ArkUI_StyledString_Descriptor</b> object.
 * @since 14
 */
void OH_ArkUI_StyledString_Descriptor_Destroy(ArkUI_StyledString_Descriptor* descriptor);

/**
 * @brief Converts styled string information into HTML.
 *
 * @param descriptor Pointer to an <b>ArkUI_StyledString_Descriptor</b> object.
 * @return Returns the pointer to the resulting HTML string. This pointer is managed internally and should be destroyed
 *         by calling <b>OH_ArkUI_StyledString_Descriptor_Destroy()</b> when no longer needed to free the memory.
 * @since 14
 */
const char* OH_ArkUI_ConvertToHtml(ArkUI_StyledString_Descriptor* descriptor);

/**
 * @brief Deserializes a byte array containing styled string information into a styled string.
 *
 * @param buffer Byte array to be deserialized.
 * @param bufferSize Length of the byte array.
 * @param descriptor Pointer to an <b>ArkUI_StyledString_Descriptor</b> object.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 14
 */
int32_t OH_ArkUI_UnmarshallStyledStringDescriptor(
    uint8_t* buffer, size_t bufferSize, ArkUI_StyledString_Descriptor* descriptor);

/**
 * @brief Serializes the styled string information into a byte array.
 *
 * @param buffer Byte array where the serialized data will be stored.
 * @param bufferSize Length of the byte array.
 * @param descriptor Pointer to an <b>ArkUI_StyledString_Descriptor</b> object.
 * @param resultSize Actual length of the byte array.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 *         Returns {@link ARKUI_ERROR_CODE_INVALID_STYLED_STRING} if the styled string is invalid.
 * @since 14
 */
int32_t OH_ArkUI_MarshallStyledStringDescriptor(
    uint8_t* buffer, size_t bufferSize, ArkUI_StyledString_Descriptor* descriptor, size_t* resultSize);

#ifdef __cplusplus
};
#endif

#endif // ARKUI_NATIVE_STYLED_STRING_H
/** @} */
