/*
 * Copyright (c) 2023-2024 Huawei Device Co., Ltd.
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
 * @addtogroup Drawing
 * @{
 *
 * @brief Provides functions such as 2D graphics rendering, text drawing, and image display.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 *
 * @since 11
 * @version 1.0
 */

/**
 * @file drawing_font.h
 *
 * @brief Declares functions related to the <b>font</b> object in the drawing module.
 *
 * @kit ArkGraphics2D
 * @library libnative_drawing.so
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @since 11
 * @version 1.0
 */

#ifndef C_INCLUDE_DRAWING_FONT_H
#define C_INCLUDE_DRAWING_FONT_H

#include "drawing_error_code.h"
#include "drawing_types.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @brief Creates an <b>OH_Drawing_Font</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @return Returns the pointer to the <b>OH_Drawing_Font</b> object created.
 * @since 11
 * @version 1.0
 */
OH_Drawing_Font* OH_Drawing_FontCreate(void);

/**
 * @brief Enumerates font hinting pattern.
 *
 * @since 12
 * @version 1.0
 */
typedef enum {
    /** glyph outlines unchanged */
    FONT_HINTING_NONE,
    /** minimal modification to improve contrast */
    FONT_HINTING_SLIGHT,
    /** glyph outlines modified to improve contrast */
    FONT_HINTING_NORMAL,
    /** modifies glyph outlines for maximum contrast */
    FONT_HINTING_FULL,
} OH_Drawing_FontHinting;

/**
 * @brief Enumerates font edging effect.
 *
 * @since 12
 * @version 1.0
 */
typedef enum {
    /** no transparent pixels on glyph edges */
    FONT_EDGING_ALIAS,
    /** may have transparent pixels on glyph edges */
    FONT_EDGING_ANTI_ALIAS,
    /** glyph positioned in pixel using transparency */
    FONT_EDGING_SUBPIXEL_ANTI_ALIAS,
} OH_Drawing_FontEdging;

/**
 * @brief Sets whether the font baselines and pixels alignment when the transformation matrix is ​​axis aligned.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @param baselineSnap Indicates whether the font baselines and pixels alignment.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_FontSetBaselineSnap(OH_Drawing_Font* font, bool baselineSnap);

/**
 * @brief Gets whether the font baselines and pixels alignment when the transformation matrix is ​​axis aligned.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @return Returns <b>true</b> if the font baselines and pixels alignment; returns <b>false</b> otherwise.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_FontIsBaselineSnap(const OH_Drawing_Font* font);

/**
 * @brief Sets whether the font uses sub-pixel rendering.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @param isSubpixel Indicates whether the font uses sub-pixel rendering.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_FontSetSubpixel(OH_Drawing_Font* font, bool isSubpixel);

/**
 * @brief Gets whether the font uses sub-pixel rendering.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @return Returns <b>true</b> if the font uses sub-pixel rendering; returns <b>false</b> otherwise.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_FontIsSubpixel(const OH_Drawing_Font* font);

/**
 * @brief Sets whether the font outline is automatically adjusted.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @param isForceAutoHinting Indicates whether the font outline is automatically adjusted.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_FontSetForceAutoHinting(OH_Drawing_Font* font, bool isForceAutoHinting);

/**
 * @brief Gets whether the font outline is automatically adjusted.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @return Returns <b>true</b> if the font outline is automatically adjusted; returns <b>false</b> otherwise.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_FontIsForceAutoHinting(const OH_Drawing_Font* font);

/**
 * @brief Sets an <b>OH_Drawing_Typeface</b> object for an <b>OH_Drawing_Font</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @param typeface Indicates the pointer to an <b>OH_Drawing_Typeface</b> object.
 * @since 11
 * @version 1.0
 */
void OH_Drawing_FontSetTypeface(OH_Drawing_Font* font, OH_Drawing_Typeface* typeface);

/**
 * @brief Gets an <b>OH_Drawing_Typeface</b> object from the <b>OH_Drawing_Typeface</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @return OH_Drawing_Typeface Indicates the pointer to an <b>OH_Drawing_Typeface</b> object.
 * @since 12
 * @version 1.0
 */
OH_Drawing_Typeface* OH_Drawing_FontGetTypeface(OH_Drawing_Font* font);

/**
 * @brief Sets text size for an <b>OH_Drawing_Font</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @param textSize Indicates the text size.
 * @since 11
 * @version 1.0
 */
void OH_Drawing_FontSetTextSize(OH_Drawing_Font* font, float textSize);

/**
 * @brief Gets text size for an <b>OH_Drawing_Font</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @return Returns the size of text.
 * @since 12
 * @version 1.0
 */
float OH_Drawing_FontGetTextSize(const OH_Drawing_Font* font);

/**
 * @brief Calculate number of glyphs represented by text.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @param text Indicates the character storage encoded with text encoding.
 * @param byteLength Indicates the text length in bytes.
 * @param encoding Indicates the text encoding.
 * @since 12
 * @version 1.0
 */
int OH_Drawing_FontCountText(OH_Drawing_Font* font, const void* text, size_t byteLength,
    OH_Drawing_TextEncoding encoding);

/**
 * @brief Converts text into glyph indices.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @param text Indicates the character storage encoded with text encoding.
 * @param byteLength Indicates the text length in bytes.
 * @param encoding Indicates the text encoding.
 * @param glyphs Indicates the storage for glyph indices.
 * @param maxGlyphCount Indicates the storage capacity.
 * @return Returns the number of glyph indices represented by text.
 * @since 12
 * @version 1.0
 */
uint32_t OH_Drawing_FontTextToGlyphs(const OH_Drawing_Font* font, const void* text, uint32_t byteLength,
    OH_Drawing_TextEncoding encoding, uint16_t* glyphs, int maxGlyphCount);

/**
 * @brief Retrieves the advance for each glyph in glyphs.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @param glyphs Indicates the array of glyph indices to be measured.
 * @param count Indicates the number of glyphs.
 * @param widths Indicates the text advances for each glyph returned to the caller.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_FontGetWidths(const OH_Drawing_Font* font, const uint16_t* glyphs, int count, float* widths);

/**
 * @brief Measures the width of a single character.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @param str Indicates the single character encoded in UTF-8.
 * @param textWidth Indicates the width of the single character.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if any of font, str
 *                 and textWidth is nullptr or strlen(str) is 0.
 * @since 12
 * @version 1.0
 */
OH_Drawing_ErrorCode OH_Drawing_FontMeasureSingleCharacter(const OH_Drawing_Font* font, const char* str,
    float* textWidth);

/**
 * @brief Measures the width of a single character with font features.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @param str Indicates the single character encoded in UTF-8.
 * @param fontFeatures Indicates the pointer to an <b>OH_Drawing_FontFeatures</b> object.
 * @param textWidth Indicates the width of the single character.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if any of font, str
 *                 fontFeatures or textWidth is nullptr, or if strlen(str) is 0.
 * @since 20
 * @version 1.0
 */
OH_Drawing_ErrorCode OH_Drawing_FontMeasureSingleCharacterWithFeatures(const OH_Drawing_Font* font, const char* str,
    const OH_Drawing_FontFeatures* fontFeatures, float* textWidth);

/**
 * @brief Measures the width of text.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @param text Indicates the character storage encoded with text encoding.
 * @param byteLength Indicates the text length in bytes.
 * @param encoding Indicates the text encoding.
 * @param bounds Gets the bounding box relative to (0, 0) if not nullptr.
 * @param textWidth Indicates the width of text.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if any of font, text
 *                 and textWidth is nullptr or byteLength is 0.
 * @since 12
 * @version 1.0
 */
OH_Drawing_ErrorCode OH_Drawing_FontMeasureText(const OH_Drawing_Font* font, const void* text, size_t byteLength,
    OH_Drawing_TextEncoding encoding, OH_Drawing_Rect* bounds, float* textWidth);

/**
 * @brief Measures the width of text with brush or pen.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @param text Indicates the character storage encoded with text encoding.
 * @param byteLength Indicates the text length in bytes.
 * @param encoding Indicates the text encoding.
 * @param brush Indicates the pointer to an <b>OH_Drawing_Brush</b> object.
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @param bounds Gets the bounding box relative to (0, 0) if not nullptr.
 * @param textWidth Indicates the width of text.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if any of font, text
 *                 and textWidth is nullptr or byteLength is 0 or brush and pen are both not empty.
 * @since 19
 * @version 1.0
 */
OH_Drawing_ErrorCode OH_Drawing_FontMeasureTextWithBrushOrPen(const OH_Drawing_Font* font, const void* text,
    size_t byteLength, OH_Drawing_TextEncoding encoding, const OH_Drawing_Brush* brush, const OH_Drawing_Pen* pen,
    OH_Drawing_Rect* bounds, float* textWidth);

/**
 * @brief Retrieves the advance and bounding box for each glyph in glyphs.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @param glyphs Indicates the array of glyph indices to be measured.
 * @param count Indicates the number of glyphs.
 * @param brush Indicates the pointer to an <b>OH_Drawing_Brush</b> object.
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @param widths Indicates the text advances for each glyph returned to the caller.
 * @param bounds Indicates the text bounding box for each glyph returned to the caller.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if any of font and glyphs is nullptr
 *                 or count is no larger than 0 or brush and pen are both not empty.
 * @since 19
 * @version 1.0
 */
OH_Drawing_ErrorCode OH_Drawing_FontGetWidthsBounds(const OH_Drawing_Font* font, const uint16_t* glyphs, int count,
    const OH_Drawing_Brush* brush, const OH_Drawing_Pen* pen, float* widths, OH_Drawing_Array* bounds);

/**
 * @brief Retrieves the positions for each glyph, beginning at the specified origin.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @param glyphs Indicates the array of glyph indices to be measured.
 * @param count Indicates the number of glyphs.
 * @param origin Indicates the location of the first glyph.
 * @param points Indicates the relative position for each glyph returned to the caller.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if any of font, glyphs and points is nullptr or
 *                 count is no larger than 0.
 * @since 19
 * @version 1.0
 */
OH_Drawing_ErrorCode OH_Drawing_FontGetPos(const OH_Drawing_Font* font, const uint16_t* glyphs, int count,
    const OH_Drawing_Point* origin, OH_Drawing_Point2D* points);

/**
 * @brief Returns the recommended spacing between lines.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @param spacing Indicates the recommended spacing between lines.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if any of font and spacing is nullptr.
 * @since 19
 * @version 1.0
 */
OH_Drawing_ErrorCode OH_Drawing_FontGetSpacing(const OH_Drawing_Font* font, float* spacing);

/**
 * @brief Enables or disables linearly scalable font for an <b>OH_Drawing_Font</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @param isLinearText Indicates whether to enable linearly scalable font.
 * @since 11
 * @version 1.0
 */
void OH_Drawing_FontSetLinearText(OH_Drawing_Font* font, bool isLinearText);

/**
 * @brief Gets whether the font is linearly scalable.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @return Returns <b>true</b> if the font is linearly scalable; returns <b>false</b> otherwise.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_FontIsLinearText(const OH_Drawing_Font* font);

/**
 * @brief Sets text skew on x-axis for an <b>OH_Drawing_Font</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @param skewX Indicates the additional shear on x-axis relative to y-axis.
 * @since 11
 * @version 1.0
 */
void OH_Drawing_FontSetTextSkewX(OH_Drawing_Font* font, float skewX);

/**
 * @brief Gets text skew on x-axis for an <b>OH_Drawing_Font</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @return Returns additional skew on x-axis relative to y-axis.
 * @since 12
 * @version 1.0
 */
float OH_Drawing_FontGetTextSkewX(const OH_Drawing_Font* font);

/**
 * @brief Enables or disables to increase stroke width to approximate bold fonts for an <b>OH_Drawing_Font</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @param isFakeBoldText Indicates whether to enable to increase stroke width.
 * @since 11
 * @version 1.0
 */
void OH_Drawing_FontSetFakeBoldText(OH_Drawing_Font* font, bool isFakeBoldText);

/**
 * @brief Gets whether to increase the stroke width to approximate bold fonts.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @return Returns <b>true</b> to increase the stroke width to approximate bold fonts; returns <b>false</b> otherwise.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_FontIsFakeBoldText(const OH_Drawing_Font* font);

/**
 * @brief Sets text scale on x-axis for an <b>OH_Drawing_Font</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @param scaleX Indicates the text horizontal scale.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_FontSetScaleX(OH_Drawing_Font* font, float scaleX);

/**
 * @brief Gets text scale on x-axis from an <b>OH_Drawing_Font</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @return Returns text horizontal scale on x-axis.
 * @since 12
 * @version 1.0
 */
float OH_Drawing_FontGetScaleX(const OH_Drawing_Font* font);

/**
 * @brief Sets hinting pattern for an <b>OH_Drawing_Font</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @param fontHinting Indicates the font hinting pattern.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_FontSetHinting(OH_Drawing_Font* font, OH_Drawing_FontHinting fontHinting);

/**
 * @brief Gets hinting pattern from an <b>OH_Drawing_Font</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @return Returns the font hinting pattern.
 * @since 12
 * @version 1.0
 */
OH_Drawing_FontHinting OH_Drawing_FontGetHinting(const OH_Drawing_Font* font);

/**
 * @brief Sets whether to use bitmaps instead of outlines in the <b>OH_Drawing_Font</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @param isEmbeddedBitmaps Indicates whether to use bitmaps instead of outlines.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_FontSetEmbeddedBitmaps(OH_Drawing_Font* font, bool isEmbeddedBitmaps);

/**
 * @brief Gets whether to use bitmaps instead of outlines in the <b>OH_Drawing_Font</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @return Returns <b>true</b> if using bitmaps instead of outlines; returns <b>false</b> otherwise.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_FontIsEmbeddedBitmaps(const OH_Drawing_Font* font);

/**
 * @brief Sets the font edging effect for an <b>OH_Drawing_Font</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @param fontEdging Indicates the font edging effect.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_FontSetEdging(OH_Drawing_Font* font, OH_Drawing_FontEdging fontEdging);

/**
 * @brief Gets the font edging effect from an <b>OH_Drawing_Font</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @return Returns the font edging effect.
 * @since 12
 * @version 1.0
 */
OH_Drawing_FontEdging OH_Drawing_FontGetEdging(const OH_Drawing_Font* font);

/**
 * @brief Destroys an <b>OH_Drawing_Font</b> object and reclaims the memory occupied by the object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @since 11
 * @version 1.0
 */
void OH_Drawing_FontDestroy(OH_Drawing_Font* font);

/**
 * @brief Defines a run, supplies storage for the metrics of an <b>OH_Drawing_Font</b>.
 *
 * @since 12
 * @version 1.0
 */
typedef struct OH_Drawing_Font_Metrics {
    /** Indicating which metrics are valid */
    uint32_t flags;
    /** storage for top in font metrics */
    float top;
    /** storage for ascent in font metrics */
    float ascent;
    /** storage for descent in font metrics */
    float descent;
    /** storage for bottom in font metrics */
    float bottom;
    /** storage for leading in font metrics */
    float leading;
    /**  Average character width, zero if unknown */
    float avgCharWidth;
    /** Maximum character width, zero if unknown */
    float maxCharWidth;
    /** Greatest extent to left of origin of any glyph bounding box, typically negative; deprecated with variable fonts */
    float xMin;
    /** Greatest extent to right of origin of any glyph bounding box, typically positive; deprecated with variable fonts */
    float xMax;
    /** Height of lower-case letter, zero if unknown, typically negative */
    float xHeight;
    /** Height of an upper-case letter, zero if unknown, typically negative */
    float capHeight;
    /** @brief Underline thickness */
    float underlineThickness;
    /** Distance from baseline to top of stroke, typically positive */
    float underlinePosition;
    /** Strikeout thickness */
    float strikeoutThickness;
    /** Distance from baseline to bottom of stroke, typically negative */
    float strikeoutPosition;
} OH_Drawing_Font_Metrics;

/**
 * @brief Obtains the metrics of a font.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @param fontMetrics Indicates the pointer to an <b>OH_Drawing_Font_Metrics</b> object.
 * @return Returns a float variable that recommended spacing between lines.
 * @since 12
 * @version 1.0
 */
float OH_Drawing_FontGetMetrics(OH_Drawing_Font* font, OH_Drawing_Font_Metrics* fontMetrics);

/**
 * @brief Retrieves the bound rect for each glyph in glyph array.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @param glyphs Indicates the array of glyph indices to be measured.
 * @param count Indicates the number of glyphs.
 * @param bounds The bound rect array for each glyph, returned to the caller.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if any of font, glyphs
 *                 and bounds is nullptr or count is 0.
 * @since 18
 * @version 1.0
 */
OH_Drawing_ErrorCode OH_Drawing_FontGetBounds(const OH_Drawing_Font* font, const uint16_t* glyphs, uint32_t count,
    OH_Drawing_Array* bounds);

/**
 * @brief Retrieves the path for specified Glyph.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @param glyph glyph index to be obtained.
 * @param path The path object, returned to the caller.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if any of font, path
 *                 is nullptr or glyph not exist.
 * @since 18
 * @version 1.0
 */
OH_Drawing_ErrorCode OH_Drawing_FontGetPathForGlyph(const OH_Drawing_Font* font, uint16_t glyph,
    OH_Drawing_Path* path);

/**
 * @brief Get the text outline path.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @param text Indicates the character storage encoded with text encoding.
 * @param byteLength Indicates to get the byte length of the corresponding text path. If this byte length is greater
 * than the byte length of the text string, undefined behavior will occur.
 * @param encoding <b>OH_Drawing_TextEncoding</b> Indicates the text encoding.
 * @param x Indicates x coordinates of the text.
 * @param y Indicates y coordinates of the text.
 * @param path <b>OH_Drawing_Path</b> The path object, returned to the caller.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if any of font, text or path is nullptr.
 * @since 18
 */
OH_Drawing_ErrorCode OH_Drawing_FontGetTextPath(const OH_Drawing_Font* font, const void* text, size_t byteLength,
    OH_Drawing_TextEncoding encoding, float x, float y, OH_Drawing_Path* path);

/**
 * @brief Creates an <b>OH_Drawing_FontFeatures</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @return Returns the pointer to the <b>OH_Drawing_FontFeatures</b> object created.
 *         If nullptr is returned, the creation fails.
 *         The possible cause of the failure is that the available memory is empty.
 * @since 20
 * @version 1.0
 */
OH_Drawing_FontFeatures* OH_Drawing_FontFeaturesCreate(void);

/**
 * @brief Adds a font feature for an <b>OH_Drawing_FontFeatures</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param fontFeatures Indicates the pointer to an <b>OH_Drawing_FontFeatures</b> object.
 * @param name Indicates the feature name.
 * @param value Indicates the value of the feature.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if either fontFeatures or name is nullptr.
 * @since 20
 * @version 1.0
 */
OH_Drawing_ErrorCode OH_Drawing_FontFeaturesAddFeature(OH_Drawing_FontFeatures* fontFeatures,
    const char* name, float value);

/**
 * @brief Destroys an <b>OH_Drawing_FontFeatures</b> object and reclaims the memory occupied by the object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param fontFeatures Indicates the pointer to an <b>OH_Drawing_FontFeatures</b> object.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if fontFeatures is nullptr.
 * @since 20
 * @version 1.0
 */
OH_Drawing_ErrorCode OH_Drawing_FontFeaturesDestroy(OH_Drawing_FontFeatures* fontFeatures);

/**
 * @brief Sets whether to follow the theme font. If the value is true, the theme font is used when typeface is not set.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @param followed Indicates whether to follow the theme font.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if font is nullptr.
 * @since 15
 */
OH_Drawing_ErrorCode OH_Drawing_FontSetThemeFontFollowed(OH_Drawing_Font* font, bool followed);

/**
 * @brief Gets whether to follow the theme font.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param font Indicates the pointer to an <b>OH_Drawing_Font</b> object.
 * @param followed Indicates whether to follow the theme font.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if font or followed is nullptr.
 * @since 15
 */
OH_Drawing_ErrorCode OH_Drawing_FontIsThemeFontFollowed(const OH_Drawing_Font* font, bool* followed);

#ifdef __cplusplus
}
#endif
/** @} */
#endif
