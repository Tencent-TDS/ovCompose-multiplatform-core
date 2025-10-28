/*
 * Copyright (c) 2021-2024 Huawei Device Co., Ltd.
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
 * @brief Provides the 2D drawing capability.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 *
 * @since 8
 * @version 1.0
 */

/**
 * @file drawing_text_typography.h
 *
 * @brief Declares functions related to <b>typography</b> in the drawing module.
 *
 * @kit ArkGraphics2D
 * @library libnative_drawing.so
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @since 8
 * @version 1.0
 */

#ifndef C_INCLUDE_DRAWING_TEXT_TYPOGRAPHY_H
#define C_INCLUDE_DRAWING_TEXT_TYPOGRAPHY_H

#ifdef __cplusplus
#include <cstdint>
#include <cstddef>
#else
#include <stdint.h>
#include <stddef.h>
#endif
#include "drawing_canvas.h"
#include "drawing_color.h"
#include "drawing_font.h"
#include "drawing_text_declaration.h"
#include "drawing_types.h"


#ifdef __cplusplus
extern "C" {
#endif

/**
 * @brief Enumerates text directions.
 *
 * @since 8
 * @version 1.0
 */
typedef enum OH_Drawing_TextDirection {
    /** Right to left (RTL) */
    TEXT_DIRECTION_RTL,
    /** Left to right (LTR) */
    TEXT_DIRECTION_LTR,
} OH_Drawing_TextDirection;

/**
 * @brief Enumerates text alignment modes.
 *
 * @since 8
 * @version 1.0
 */
typedef enum OH_Drawing_TextAlign {
    /** Left-aligned */
    TEXT_ALIGN_LEFT,
    /** Right-aligned */
    TEXT_ALIGN_RIGHT,
    /** Center-aligned */
    TEXT_ALIGN_CENTER,
    /**
     * Justified, which means that each line (except the last line) is stretched so that every line has equal width,
     * and the left and right margins are straight.
     */
    TEXT_ALIGN_JUSTIFY,
    /**
     * <b>TEXT_ALIGN_START</b> achieves the same effect as <b>TEXT_ALIGN_LEFT</b>
     * when <b>OH_Drawing_TextDirection</b> is <b>TEXT_DIRECTION_LTR</b>;
     * it achieves the same effect as <b>TEXT_ALIGN_RIGHT</b>
     * when <b>OH_Drawing_TextDirection</b> is <b>TEXT_DIRECTION_RTL</b>.
     */
    TEXT_ALIGN_START,
    /**
     * <b>TEXT_ALIGN_END</b> achieves the same effect as <b>TEXT_ALIGN_RIGHT</b>
     * when <b>OH_Drawing_TextDirection</b> is <b>TEXT_DIRECTION_LTR</b>;
     * it achieves the same effect as <b>TEXT_ALIGN_LEFT</b>
     * when <b>OH_Drawing_TextDirection</b> is <b>TEXT_DIRECTION_RTL</b>.
     */
    TEXT_ALIGN_END,
} OH_Drawing_TextAlign;

/**
 * @brief Enumerates font weights.
 *
 * @since 8
 * @version 1.0
 */
typedef enum OH_Drawing_FontWeight {
    /** Thin */
    FONT_WEIGHT_100,
    /** Extra-light */
    FONT_WEIGHT_200,
    /** Light */
    FONT_WEIGHT_300,
    /** Normal/Regular */
    FONT_WEIGHT_400,
    /** Medium*/
    FONT_WEIGHT_500,
    /** Semi-bold */
    FONT_WEIGHT_600,
    /** Bold */
    FONT_WEIGHT_700,
    /** Extra-bold */
    FONT_WEIGHT_800,
    /** Black */
    FONT_WEIGHT_900,
} OH_Drawing_FontWeight;

/**
 * @brief Enumerates text baselines.
 *
 * @since 8
 * @version 1.0
 */
typedef enum OH_Drawing_TextBaseline {
    /** Alphabetic, where the letters in alphabets like English sit on. */
    TEXT_BASELINE_ALPHABETIC,
    /** Ideographic. The baseline is at the bottom of the text area. */
    TEXT_BASELINE_IDEOGRAPHIC,
} OH_Drawing_TextBaseline;

/**
 * @brief Enumerates text decorations.
 *
 * @since 8
 * @version 1.0
 */
typedef enum OH_Drawing_TextDecoration {
    /** No decoration. */
    TEXT_DECORATION_NONE = 0x0,
    /** A underline is used for decoration. */
    TEXT_DECORATION_UNDERLINE = 0x1,
    /** An overline is used for decoration. */
    TEXT_DECORATION_OVERLINE = 0x2,
    /** A strikethrough is used for decoration. */
    TEXT_DECORATION_LINE_THROUGH = 0x4,
} OH_Drawing_TextDecoration;

/**
 * @brief Enumerates font styles.
 *
 * @since 8
 * @version 1.0
 */
typedef enum OH_Drawing_FontStyle {
    /** Normal style */
    FONT_STYLE_NORMAL,
    /** Italic style */
    FONT_STYLE_ITALIC,
    /** Oblique style */
    FONT_STYLE_OBLIQUE,
} OH_Drawing_FontStyle;

/**
 * @brief Enumerates placeholder vertical alignment.
 *
 * @since 11
 * @version 1.0
 */
typedef enum {
    /** Offset At Baseline */
    ALIGNMENT_OFFSET_AT_BASELINE,
    /** Above Baseline */
    ALIGNMENT_ABOVE_BASELINE,
    /** Below Baseline */
    ALIGNMENT_BELOW_BASELINE,
    /** Top of Row Box */
    ALIGNMENT_TOP_OF_ROW_BOX,
    /** Bottom of Row Box */
    ALIGNMENT_BOTTOM_OF_ROW_BOX,
    /** Center of Row Box */
    ALIGNMENT_CENTER_OF_ROW_BOX,
    /**
     * Follow paragraph setting
     * @since 20
     */
    ALIGNMENT_FOLLOW_PARAGRAPH,
} OH_Drawing_PlaceholderVerticalAlignment;

/**
 * @brief Defines the placeholder span.
 *
 * @since 11
 * @version 1.0
 */
typedef struct {
    /** width of placeholder */
    double width;
    /** height of placeholder */
    double height;
    /** alignment of placeholder */
    OH_Drawing_PlaceholderVerticalAlignment alignment;
    /** baseline of placeholder */
    OH_Drawing_TextBaseline baseline;
    /** baselineoffset of placeholder */
    double baselineOffset;
} OH_Drawing_PlaceholderSpan;

/**
 * @brief Enumerates text decoration style.
 *
 * @since 11
 * @version 1.0
 */
typedef enum {
    /** Solid style */
    TEXT_DECORATION_STYLE_SOLID,
    /** Double style */
    TEXT_DECORATION_STYLE_DOUBLE,
    /** Dotted style */
    TEXT_DECORATION_STYLE_DOTTED,
    /** Dashed style */
    TEXT_DECORATION_STYLE_DASHED,
    /** Wavy style */
    TEXT_DECORATION_STYLE_WAVY,
} OH_Drawing_TextDecorationStyle;

/**
 * @brief Enumerates ellipsis modal.
 *
 * @since 11
 * @version 1.0
 */
typedef enum {
    /** Head modal */
    ELLIPSIS_MODAL_HEAD = 0,
    /** Middle modal */
    ELLIPSIS_MODAL_MIDDLE = 1,
    /** Tail modal */
    ELLIPSIS_MODAL_TAIL = 2,
} OH_Drawing_EllipsisModal;

/**
 * @brief Enumerates break strategy.
 *
 * @since 11
 * @version 1.0
 */
typedef enum {
    /** Greedy strategy */
    BREAK_STRATEGY_GREEDY = 0,
    /** Quality strategy */
    BREAK_STRATEGY_HIGH_QUALITY = 1,
    /** Balanced strategy */
    BREAK_STRATEGY_BALANCED = 2,
} OH_Drawing_BreakStrategy;

/**
 * @brief Enumerates word break type.
 *
 * @since 11
 * @version 1.0
 */
typedef enum {
    /** Normal type */
    WORD_BREAK_TYPE_NORMAL = 0,
    /** Break All type */
    WORD_BREAK_TYPE_BREAK_ALL = 1,
    /** Break Word type */
    WORD_BREAK_TYPE_BREAK_WORD = 2,
    /**
     * Break word with hyphens
     * @since 18
     */
    WORD_BREAK_TYPE_BREAK_HYPHEN = 3,
} OH_Drawing_WordBreakType;

/**
 * @brief Enumerates rect height style.
 *
 * @since 11
 * @version 1.0
 */
typedef enum {
    /** Tight style */
    RECT_HEIGHT_STYLE_TIGHT,
    /** Max style */
    RECT_HEIGHT_STYLE_MAX,
    /** Includelinespacemiddle style */
    RECT_HEIGHT_STYLE_INCLUDELINESPACEMIDDLE,
    /** Includelinespacetop style */
    RECT_HEIGHT_STYLE_INCLUDELINESPACETOP,
    /** Includelinespacebottom style */
    RECT_HEIGHT_STYLE_INCLUDELINESPACEBOTTOM,
    /** Struct style */
    RECT_HEIGHT_STYLE_STRUCT,
} OH_Drawing_RectHeightStyle;

/**
 * @brief Enumerates rect Width style.
 *
 * @since 11
 * @version 1.0
 */
typedef enum {
    /** Tight style */
    RECT_WIDTH_STYLE_TIGHT,
    /** Max style */
    RECT_WIDTH_STYLE_MAX,
} OH_Drawing_RectWidthStyle;

/**
 * @brief Gets system font configuration information list result enum.
 *
 * @since 12
 * @version 1.0
 */
typedef enum OH_Drawing_FontConfigInfoErrorCode {
    /** The list of system font configuration information was successfully obtained */
    SUCCESS_FONT_CONFIG_INFO = 0,
    /** Unknown error */
    ERROR_FONT_CONFIG_INFO_UNKNOWN = 1,
    /** Parse system config file error */
    ERROR_FONT_CONFIG_INFO_PARSE_FILE = 2,
    /** Alloc memory error */
    ERROR_FONT_CONFIG_INFO_ALLOC_MEMORY = 3,
    /** Copy string data error */
    ERROR_FONT_CONFIG_INFO_COPY_STRING_DATA = 4,
} OH_Drawing_FontConfigInfoErrorCode;

/**
 * @brief Fallback font information.
 *
 * @since 12
 * @version 1.0
 */
typedef struct OH_Drawing_FontFallbackInfo {
    /** The type of language supported by the font set. The language format is bcp47 */
    char* language;
    /** Font family name */
    char* familyName;
} OH_Drawing_FontFallbackInfo;

/**
 * @brief Fallback font group.
 *
 * @since 12
 * @version 1.0
 */
typedef struct OH_Drawing_FontFallbackGroup {
    /**
     * The name of the font set corresponding to the fallback font set. If the value is null,
     * all fonts can be set using the fallback font set list.
     */
    char* groupName;
    /** Fallback font Info Size */
    size_t fallbackInfoSize;
    /** A list of font sets for fallback fonts */
    OH_Drawing_FontFallbackInfo* fallbackInfoSet;
} OH_Drawing_FontFallbackGroup;

/**
 * @brief Font weight mapping information.
 *
 * @since 12
 * @version 1.0
 */
typedef struct OH_Drawing_FontAdjustInfo {
    /** The font's original weight value */
    int weight;
    /** The font weight displayed in the application */
    int to;
} OH_Drawing_FontAdjustInfo;

/**
 * @brief Alias font information.
 *
 * @since 12
 * @version 1.0
 */
typedef struct OH_Drawing_FontAliasInfo {
    /** Font family name */
    char* familyName;
    /**
     * Font weight value. When the weight value is greater than 0,
     * the font set contains only fonts with the specified weight.
     * When the weight value is equal to 0, the font set contains all fonts.
     */
    int weight;
} OH_Drawing_FontAliasInfo;

/**
 * @brief General font set information supported by the system.
 *
 * @since 12
 * @version 1.0
 */
typedef struct OH_Drawing_FontGenericInfo {
    /** Font family name */
    char* familyName;
    /** The size of alias font lists */
    size_t aliasInfoSize;
    /** The size of font weight mapping information lists */
    size_t adjustInfoSize;
    /** List of alias fonts */
    OH_Drawing_FontAliasInfo* aliasInfoSet;
    /** Font weight mapping information lists */
    OH_Drawing_FontAdjustInfo* adjustInfoSet;
} OH_Drawing_FontGenericInfo;

/**
 * @brief System font configuration information.
 *
 * @since 12
 * @version 1.0
 */
typedef struct OH_Drawing_FontConfigInfo {
    /** Count of system font file paths */
    size_t fontDirSize;
    /** List size of generic font sets */
    size_t fontGenericInfoSize;
    /** Count of fallback font set lists */
    size_t fallbackGroupSize;
    /** List of system font file paths */
    char** fontDirSet;
    /** List of generic font sets */
    OH_Drawing_FontGenericInfo* fontGenericInfoSet;
    /** List of fallback font sets */
    OH_Drawing_FontFallbackGroup* fallbackGroupSet;
} OH_Drawing_FontConfigInfo;

/**
 * @brief Describes the font information.
 *
 * @since 12
 * @version 1.0
 */
typedef struct OH_Drawing_FontDescriptor {
    /** The file path of System font */
    char* path;
    /** A name that uniquely identifies the font */
    char* postScriptName;
    /** The name of System font */
    char* fullName;
    /** The family of System font */
    char* fontFamily;
    /** The subfont family of the system font */
    char* fontSubfamily;
    /** The weight of System font */
    int weight;
    /** The width of System font */
    int width;
    /** Whether the system font is tilted */
    int italic;
    /** Whether the system font is compact */
    bool monoSpace;
    /** whether symbolic fonts are supported */
    bool symbolic;
} OH_Drawing_FontDescriptor;

/**
 * @brief The metrics of line.
 *
 * @since 12
 * @version 1.0
 */
typedef struct OH_Drawing_LineMetrics {
    /** Text ascender height */
    double ascender;
    /** Tex descender height */
    double descender;
    /** The height of a capital letter */
    double capHeight;
    /** The height of a lowercase letter */
    double xHeight;
    /** Text width */
    double width;
    /** Line height */
    double height;
    /**
     * The distance from the left end of the text to the left end of the container,
     * aligned to 0, is the width of the container minus the width of the line of text
     */
    double x;
    /**
     * The height from the top of the text to the top of the container, the first line is 0,
     * and the second line is the height of the first line
     */
    double y;
    /** Start Index */
    size_t startIndex;
    /** End Index */
    size_t endIndex;
    /** The metrics information of the first character */
    OH_Drawing_Font_Metrics firstCharMetrics;
} OH_Drawing_LineMetrics;

/**
 * @brief Enumerates of heightmode of text.
 *
 * @since 12
 * @version 1.0
 */
typedef enum OH_Drawing_TextHeightBehavior {
    /** both ascend of first row and last row style */
    TEXT_HEIGHT_ALL = 0x0,
    /** forbidding ascend of first row style*/
    TEXT_HEIGHT_DISABLE_FIRST_ASCENT = 0x1,
     /** forbidding ascend of last row style */
    TEXT_HEIGHT_DISABLE_LAST_ASCENT = 0x2,
      /** neither ascend of first row nor last row style */
    TEXT_HEIGHT_DISABLE_ALL = 0x1 | 0x2,
} OH_Drawing_TextHeightBehavior;

/**
 * @brief Enumerates text style type.
 *
 * @since 12
 * @version 1.0
 */
typedef enum OH_Drawing_TextStyleType {
    /** None style */
    TEXT_STYLE_NONE,
    /** All attributes style */
    TEXT_STYLE_ALL_ATTRIBUTES,
    /** Font style */
    TEXT_STYLE_FONT,
    /** Foreground style */
    TEXT_STYLE_FOREGROUND,
    /** Background style */
    TEXT_STYLE_BACKGROUND,
    /** Shadow style */
    TEXT_STYLE_SHADOW,
    /** Decorations style */
    TEXT_STYLE_DECORATIONS,
    /** Letter spacing style */
    TEXT_STYLE_LETTER_SPACING,
    /** Word spacing style */
    TEXT_STYLE_WORD_SPACING
} OH_Drawing_TextStyleType;

/**
 * @brief Enumerates font width.
 *
 * @since 12
 * @version 1.0
 */
typedef enum OH_Drawing_FontWidth {
    /* Ultra condensed font width */
    FONT_WIDTH_ULTRA_CONDENSED = 1,
    /* Extra condensed font width */
    FONT_WIDTH_EXTRA_CONDENSED = 2,
    /* condensed font width */
    FONT_WIDTH_CONDENSED = 3,
    /* Semi condensed font width */
    FONT_WIDTH_SEMI_CONDENSED = 4,
    /* Normal font width */
    FONT_WIDTH_NORMAL = 5,
    /* Semi expanded font width */
    FONT_WIDTH_SEMI_EXPANDED = 6,
    /* Expanded font width */
    FONT_WIDTH_EXPANDED = 7,
    /* Extra expanded font width */
    FONT_WIDTH_EXTRA_EXPANDED = 8,
    /* Ultra expanded font width */
    FONT_WIDTH_ULTRA_EXPANDED = 9,
} OH_Drawing_FontWidth;

/**
 * @brief Type of badge.
 *
 * @since 20
 * @version 1.0
 */
typedef enum OH_Drawing_TextBadgeType {
    /** No badge */
    TEXT_BADGE_NONE,
    /** Superscript */
    TEXT_SUPERSCRIPT,
    /** Subscript */
    TEXT_SUBSCRIPT,
} OH_Drawing_TextBadgeType;

/**
 * @brief Type of vertical alignment.
 *
 * @since 20
 * @version 1.0
 */
typedef enum OH_Drawing_TextVerticalAlignment {
    /** Baseline of text line */
    TEXT_VERTICAL_ALIGNMENT_BASELINE,
    /** Bottom of text line */
    TEXT_VERTICAL_ALIGNMENT_BOTTOM,
    /** Center of text line */
    TEXT_VERTICAL_ALIGNMENT_CENTER,
    /** Top of text line */
    TEXT_VERTICAL_ALIGNMENT_TOP
} OH_Drawing_TextVerticalAlignment;

/**
 * @brief Defines the font style struct.
 *
 * @since 12
 * @version 1.0
 */
typedef struct OH_Drawing_FontStyleStruct {
    /** Font weight */
    OH_Drawing_FontWeight weight;
    /** Font width */
    OH_Drawing_FontWidth width;
    /** Font slant */
    OH_Drawing_FontStyle slant;
} OH_Drawing_FontStyleStruct;

/**
 * @brief Defines the fontfeature.
 *
 * @since 12
 * @version 1.0
 */
typedef struct {
    /** key of fontfeature */
    char* tag;
    /** value of fontfeature */
    int value;
} OH_Drawing_FontFeature;

/**
 * @brief Defines StrutStyle info struct.
 *
 * @since 12
 * @version 1.0
 */
typedef struct {
    /** The font weight to use when calculating the strut */
    OH_Drawing_FontWeight weight;
    /** The font style to use when calculating the strut */
    OH_Drawing_FontStyle style;
    /** The size of the ascent plus descent in logical pixels */
    double size;
    /** The minimum height of the strut, as a multiple of fontSize */
    double heightScale;
    /** Whether the height is override */
    bool heightOverride;
    /** Whether the halfleading is enable */
    bool halfLeading;
    /** The additional leading to apply to the strut as a multiple of Size */
    double leading;
    /** Whether the strut height should be forced */
    bool forceStrutHeight;
    /** The size of font families */
    size_t familiesSize;
    /** The families of the font to use when calculating the strut */
    char** families;
} OH_Drawing_StrutStyle;

/**
 * @brief Creates an <b>OH_Drawing_TypographyStyle</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @return Returns the pointer to the <b>OH_Drawing_TypographyStyle</b> object created.
 * @since 8
 * @version 1.0
 */
OH_Drawing_TypographyStyle* OH_Drawing_CreateTypographyStyle(void);

/**
 * @brief Releases the memory occupied by an <b>OH_Drawing_TypographyStyle</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TypographyStyle</b> object.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_DestroyTypographyStyle(OH_Drawing_TypographyStyle* style);

/**
 * @brief Sets the text direction.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TypographyStyle</b> object.
 * @param direction Indicates the text direction to set. For details, see the enum <b>OH_Drawing_TextDirection</b>.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_SetTypographyTextDirection(OH_Drawing_TypographyStyle* style, int direction);

/**
 * @brief Sets the text alignment mode.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TypographyStyle</b> object.
 * @param align Indicates the text alignment mode to set. For details, see the enum <b>OH_Drawing_TextAlign</b>.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_SetTypographyTextAlign(OH_Drawing_TypographyStyle* style, int align);

/**
 * @brief Sets the maximum number of lines in a text file.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TypographyStyle</b> object.
 * @param lineNumber Indicates the maximum number of lines to set.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_SetTypographyTextMaxLines(OH_Drawing_TypographyStyle* style, int lineNumber);

/**
 * @brief Creates an <b>OH_Drawing_TextStyle</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @return Returns the pointer to the <b>OH_Drawing_TextStyle</b> object created.
 * @since 8
 * @version 1.0
 */
OH_Drawing_TextStyle* OH_Drawing_CreateTextStyle(void);

/**
 * @brief Releases the memory occupied by an <b>OH_Drawing_TextStyle</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_DestroyTextStyle(OH_Drawing_TextStyle* style);

/**
 * @brief Sets the text color.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @param color Indicates the color to set.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_SetTextStyleColor(OH_Drawing_TextStyle* style, uint32_t color);

/**
 * @brief Sets the font size.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @param fontSize Indicates the font size to set.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_SetTextStyleFontSize(OH_Drawing_TextStyle* style, double fontSize);

/**
 * @brief Sets the font weight.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @param fontWeight Indicates the font weight to set. For details, see the enum <b>OH_Drawing_FontWeight</b>.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_SetTextStyleFontWeight(OH_Drawing_TextStyle* style, int fontWeight);

/**
 * @brief Sets the text baseline.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @param baseline Indicates the text baseline to set. For details, see the enum <b>OH_Drawing_TextBaseline</b>.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_SetTextStyleBaseLine(OH_Drawing_TextStyle* style, int baseline);

/**
 * @brief Sets the text decoration.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @param decoration Indicates the text decoration to set. For details, see the enum <b>OH_Drawing_TextDecoration</b>.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_SetTextStyleDecoration(OH_Drawing_TextStyle* style, int decoration);

/**
 * @brief Add the text decoration.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @param decoration Indicates the text decoration to add. For details, see the enum <b>OH_Drawing_TextDecoration</b>.
 * @since 18
 * @version 1.0
 */
void OH_Drawing_AddTextStyleDecoration(OH_Drawing_TextStyle* style, int decoration);

/**
 * @brief Remove the text decoration.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @param decoration Indicates the text decoration to remove, shoud be match existing text decorations.
 * For details, see the enum <b>OH_Drawing_TextDecoration</b>.
 * @since 18
 * @version 1.0
 */
void OH_Drawing_RemoveTextStyleDecoration(OH_Drawing_TextStyle* style, int decoration);

/**
 * @brief Sets the color for the text decoration.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @param color Indicates the color to set.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_SetTextStyleDecorationColor(OH_Drawing_TextStyle* style, uint32_t color);

/**
 * @brief Sets the font height.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @param fontHeight Indicates the font height to set.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_SetTextStyleFontHeight(OH_Drawing_TextStyle* style, double fontHeight);

/**
 * @brief Sets the font families.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @param fontFamiliesNumber Indicates the number of font families to set.
 * @param fontFamilies Indicates the pointer to the font families to set.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_SetTextStyleFontFamilies(OH_Drawing_TextStyle* style,
    int fontFamiliesNumber, const char* fontFamilies[]);

/**
 * @brief Sets the font style.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @param fontStyle Indicates the font style to set. For details, see the enum <b>OH_Drawing_FontStyle</b>.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_SetTextStyleFontStyle(OH_Drawing_TextStyle* style, int fontStyle);

/**
 * @brief Sets the locale.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @param locale Indicates the pointer to the locale to set.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_SetTextStyleLocale(OH_Drawing_TextStyle* style, const char* locale);

/**
 * @brief Sets the foreground brush style.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a text style object <b>OH_Drawing_TextStyle</b>.
 * @param foregroundBrush Indicates the pointer to a brush object <b>OH_Drawing_Brush</b>.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_SetTextStyleForegroundBrush(OH_Drawing_TextStyle* style, OH_Drawing_Brush* foregroundBrush);

/**
 * @brief Gets the foreground brush style.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a text style object <b>OH_Drawing_TextStyle</b>.
 * @param foregroundBrush Indicates the pointer to a brush object <b>OH_Drawing_Brush</b>.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_TextStyleGetForegroundBrush(OH_Drawing_TextStyle* style, OH_Drawing_Brush* foregroundBrush);

/**
 * @brief Sets the foreground pen style.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a text style object <b>OH_Drawing_TextStyle</b>.
 * @param foregroundPen Indicates the pointer to a pen object <b>OH_Drawing_Pen</b>.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_SetTextStyleForegroundPen(OH_Drawing_TextStyle* style, OH_Drawing_Pen* foregroundPen);

/**
 * @brief Gets the foreground pen style.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a text style object <b>OH_Drawing_TextStyle</b>.
 * @param foregroundPen Indicates the pointer to a pen object <b>OH_Drawing_Pen</b>.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_TextStyleGetForegroundPen(OH_Drawing_TextStyle* style, OH_Drawing_Pen* foregroundPen);

/**
 * @brief Sets the background brush style.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a text style object <b>OH_Drawing_TextStyle</b>.
 * @param foregroundPen Indicates the pointer to a brush object <b>OH_Drawing_Brush</b>.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_SetTextStyleBackgroundBrush(OH_Drawing_TextStyle* style, OH_Drawing_Brush* foregroundPen);

/**
 * @brief Gets the background brush style.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a text style object <b>OH_Drawing_TextStyle</b>.
 * @param backgroundBrush Indicates the pointer to a brush object <b>OH_Drawing_Brush</b>.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_TextStyleGetBackgroundBrush(OH_Drawing_TextStyle* style, OH_Drawing_Brush* backgroundBrush);

/**
 * @brief Sets the background pen style.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a text style object <b>OH_Drawing_TextStyle</b>.
 * @param backgroundPen Indicates the pointer to a pen object <b>OH_Drawing_Pen</b>.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_SetTextStyleBackgroundPen(OH_Drawing_TextStyle* style, OH_Drawing_Pen* backgroundPen);

/**
 * @brief Gets the background pen style.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a text style object <b>OH_Drawing_TextStyle</b>.
 * @param backgroundPen Indicates the pointer to a pen object <b>OH_Drawing_Pen</b>.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_TextStyleGetBackgroundPen(OH_Drawing_TextStyle* style, OH_Drawing_Pen* backgroundPen);

/**
 * @brief Creates a pointer to an <b>OH_Drawing_TypographyCreate</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TypographyStyle</b> object.
 * @param fontCollection Indicates the pointer to an <b>OH_Drawing_FontCollection</b> object.
 * @return Returns the pointer to the <b>OH_Drawing_TypographyCreate</b> object created.
 * @since 8
 * @version 1.0
 */
OH_Drawing_TypographyCreate* OH_Drawing_CreateTypographyHandler(OH_Drawing_TypographyStyle* style,
    OH_Drawing_FontCollection* fontCollection);

/**
 * @brief Releases the memory occupied by an <b>OH_Drawing_TypographyCreate</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param handler Indicates the pointer to an <b>OH_Drawing_TypographyCreate</b> object.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_DestroyTypographyHandler(OH_Drawing_TypographyCreate* handler);

/**
 * @brief Sets the text style.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param handler Indicates the pointer to an <b>OH_Drawing_TypographyCreate</b> object.
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_TypographyHandlerPushTextStyle(OH_Drawing_TypographyCreate* handler, OH_Drawing_TextStyle* style);

/**
 * @brief Sets the text content.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param handler Indicates the pointer to an <b>OH_Drawing_TypographyCreate</b> object.
 * @param text Indicates the pointer to the text content to set.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_TypographyHandlerAddText(OH_Drawing_TypographyCreate* handler, const char* text);

/**
 * @brief Removes the topmost style in the stack, leaving the remaining styles in effect.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param handler Indicates the pointer to an <b>OH_Drawing_TypographyCreate</b> object.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_TypographyHandlerPopTextStyle(OH_Drawing_TypographyCreate* handler);

/**
 * @brief Creates an <b>OH_Drawing_Typography</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param handler Indicates the pointer to an <b>OH_Drawing_TypographyCreate</b> object.
 * @return Returns the pointer to the <b>OH_Drawing_Typography</b> object created.
 * @since 8
 * @version 1.0
 */
OH_Drawing_Typography* OH_Drawing_CreateTypography(OH_Drawing_TypographyCreate* handler);

/**
 * @brief Releases the memory occupied by an <b>OH_Drawing_Typography</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to an <b>OH_Drawing_Typography</b> object.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_DestroyTypography(OH_Drawing_Typography* typography);

/**
 * @brief Lays out the typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to an <b>OH_Drawing_Typography</b> object.
 * @param maxWidth Indicates the maximum text width to set.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_TypographyLayout(OH_Drawing_Typography* typography, double maxWidth);

/**
 * @brief Paints text on the canvas.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to an <b>OH_Drawing_Typography</b> object.
 * @param canvas Indicates the pointer to an <b>OH_Drawing_Canvas</b> object.
 * @param potisionX Indicates the x coordinate.
 * @param potisionY Indicates the y coordinate.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_TypographyPaint(OH_Drawing_Typography* typography, OH_Drawing_Canvas* canvas,
    double potisionX, double potisionY);

/**
 * @brief Paints path text on the canvas.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to an <b>OH_Drawing_Typography</b> object.
 * @param canvas Indicates the pointer to an <b>OH_Drawing_Canvas</b> object.
 * @param path Indicates path information.
 * @param hOffset Indicates the distance along the path to add to the text's starting position.
 * @param vOffset Indicates the distance above(-) or below(+) the path to position the text.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_TypographyPaintOnPath(OH_Drawing_Typography* typography, OH_Drawing_Canvas* canvas,
    OH_Drawing_Path* path, double hOffset, double vOffset);

/**
 * @brief Gets the max width.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to an <b>OH_Drawing_Typography</b> object.
 * @return Returns the max width.
 * @since 9
 * @version 1.1
 */
double OH_Drawing_TypographyGetMaxWidth(OH_Drawing_Typography* typography);

/**
 * @brief Gets the height.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to an <b>OH_Drawing_Typography</b> object.
 * @return Returns the height.
 * @since 9
 * @version 1.1
 */
double OH_Drawing_TypographyGetHeight(OH_Drawing_Typography* typography);

/**
 * @brief Obtains the width of the longest line. You are advised to round up the return value in actual use.
 * When the text content is empty, the minimum float value,
 * that is, -340282346638528859811704183484516925440.000000, is returned.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Pointer to an <b>OH_Drawing_Typography</b> object, which is obtained by
 * {@link OH_Drawing_CreateTypography}.
 * @return Returns the width of the longest line.
 * @since 9
 * @version 1.1
 */
double OH_Drawing_TypographyGetLongestLine(OH_Drawing_Typography* typography);

/**
 * @brief Obtains the width of the longest line with indent. You are advised to
 * round up the return value in actual use. When the text content is empty, the
 * minimum float value, that is, 0.0, is returned.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Pointer to an <b>OH_Drawing_Typography</b> object, which is obtained by
 * {@link OH_Drawing_CreateTypography}.
 * @return Returns the width of the longest line with indent.
 * @since 13
 * @version 1.1
 */
double OH_Drawing_TypographyGetLongestLineWithIndent(OH_Drawing_Typography* typography);

/**
 * @brief Gets the min intrinsic width.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography the pointer to an <b>OH_Drawing_Typography</b> object.
 * @return Returns the min intrinsic width.
 * @since 9
 * @version 1.1
 */
double OH_Drawing_TypographyGetMinIntrinsicWidth(OH_Drawing_Typography* typography);

/**
 * @brief Gets the max intrinsic width.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to an <b>OH_Drawing_Typography</b> object.
 * @return Returns the max intrinsic width.
 * @since 9
 * @version 1.1
 */
double OH_Drawing_TypographyGetMaxIntrinsicWidth(OH_Drawing_Typography* typography);

/**
 * @brief Gets the alphabetic baseline.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to an <b>OH_Drawing_Typography</b> object.
 * @return Returns the alphabetic baseline.
 * @since 9
 * @version 1.1
 */
double OH_Drawing_TypographyGetAlphabeticBaseline(OH_Drawing_Typography* typography);

/**
 * @brief Gets the ideographic baseline.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to an <b>OH_Drawing_Typography</b> object.
 * @return Returns the ideographic baseline.
 * @since 9
 * @version 1.1
 */
double OH_Drawing_TypographyGetIdeographicBaseline(OH_Drawing_Typography* typography);

/**
 * @brief Sets the placeholder.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param handler Indicates the pointer to an <b>OH_Drawing_TypographyCreate</b> object.
 * @param span Indicates the pointer to an <b>OH_Drawing_PlaceholderSpan</b> object.
 * @since 11
 * @version 1.0
 */
void OH_Drawing_TypographyHandlerAddPlaceholder(OH_Drawing_TypographyCreate* handler, OH_Drawing_PlaceholderSpan* span);

/**
 * @brief Gets the exceed maxLines.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to an <b>OH_Drawing_Typography</b> object.
 * @return Returns the exceed maxLines.
 * @since 11
 * @version 1.0
 */
bool OH_Drawing_TypographyDidExceedMaxLines(OH_Drawing_Typography* typography);

/**
 * @brief Gets the rects for range.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to an <b>OH_Drawing_Typography</b> object.
 * @param start Indicates the start of range to set.
 * @param end Indicates the end of range to set.
 * @param heightStyle Indicates the height style to set.
 * For details, see the enum <b>OH_Drawing_RectHeightStyle</b>.
 * @param widthStyle Indicates the width style to set.
 * For details, see the enum <b>OH_Drawing_RectWidthStyle</b>.
 * @return Returns the rects for range.
 * @since 11
 * @version 1.0
 */
OH_Drawing_TextBox* OH_Drawing_TypographyGetRectsForRange(OH_Drawing_Typography* typography,
    size_t start, size_t end, OH_Drawing_RectHeightStyle heightStyle, OH_Drawing_RectWidthStyle widthStyle);

/**
 * @brief Gets the rects for placeholders.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to an <b>OH_Drawing_Typography</b> object.
 * @return Returns the rects for placeholders.
 * @since 11
 * @version 1.0
 */
OH_Drawing_TextBox* OH_Drawing_TypographyGetRectsForPlaceholders(OH_Drawing_Typography* typography);

/**
 * @brief Gets left from textbox.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param textbox Indicates the pointer to an <b>OH_Drawing_TextBox</b> object.
 * @param index Indicates the index of textbox.
 * @return Returns left from textbox.
 * @since 11
 * @version 1.0
 */
float OH_Drawing_GetLeftFromTextBox(OH_Drawing_TextBox* textbox, int index);

/**
 * @brief Gets right from textbox.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param textbox Indicates the pointer to an <b>OH_Drawing_TextBox</b> object.
 * @param index Indicates the index of textbox.
 * @return Returns right from textbox.
 * @since 11
 * @version 1.0
 */
float OH_Drawing_GetRightFromTextBox(OH_Drawing_TextBox* textbox, int index);

/**
 * @brief Gets top from textbox.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param textbox Indicates the pointer to an <b>OH_Drawing_TextBox</b> object.
 * @param index Indicates the index of textbox.
 * @return Returns top from textbox.
 * @since 11
 * @version 1.0
 */
float OH_Drawing_GetTopFromTextBox(OH_Drawing_TextBox* textbox, int index);

/**
 * @brief Gets bottom from textbox.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param textbox Indicates the pointer to an <b>OH_Drawing_TextBox</b> object.
 * @param index Indicates the index of textbox.
 * @return Returns bottom from textbox.
 * @since 11
 * @version 1.0
 */
float OH_Drawing_GetBottomFromTextBox(OH_Drawing_TextBox* textbox, int index);

/**
 * @brief Gets direction from textbox.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param textbox Indicates the pointer to an <b>OH_Drawing_TextBox</b> object.
 * @param index Indicates the index of textbox.
 * @return Returns direction from textbox.
 * @since 11
 * @version 1.0
 */
int OH_Drawing_GetTextDirectionFromTextBox(OH_Drawing_TextBox* textbox, int index);

/**
 * @brief Gets size of textBox.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param textBox Indicates the pointer to an <b>OH_Drawing_TextBox</b> object.
 * @return Returns size of textBox.
 * @since 11
 * @version 1.0
 */
size_t OH_Drawing_GetSizeOfTextBox(OH_Drawing_TextBox* textBox);

/**
 * @brief Gets the glyphposition at coordinate.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to an <b>OH_Drawing_Typography</b> object.
 * @param dx Indicates the positionX of typography to set.
 * @param dy Indicates the positionY of typography to set.
 * @return Returns the glyphposition at coordinate.
 * @since 11
 * @version 1.0
 * @deprecated since 18
 * @useinstead OH_Drawing_TypographyGetGlyphPositionAtCoordinateWithCluster
 */
OH_Drawing_PositionAndAffinity* OH_Drawing_TypographyGetGlyphPositionAtCoordinate(OH_Drawing_Typography* typography,
    double dx, double dy);

/**
 * @brief Gets the glyphposition at coordinate with cluster.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to an <b>OH_Drawing_Typography</b> object.
 * @param dx Indicates the positionX of typography to set.
 * @param dy Indicates the positionY of typography to set.
 * @return Returns the glyphposition at coordinate with cluster.
 * @since 11
 * @version 1.0
 */
OH_Drawing_PositionAndAffinity* OH_Drawing_TypographyGetGlyphPositionAtCoordinateWithCluster(
    OH_Drawing_Typography* typography, double dx, double dy);

/**
 * @brief Gets position from position and affinity.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param positionAndAffinity Indicates the pointer to an <b>OH_Drawing_PositionAndAffinity</b> object.
 * @return Returns position from position and affinity.
 * @since 11
 * @version 1.0
 */
size_t OH_Drawing_GetPositionFromPositionAndAffinity(OH_Drawing_PositionAndAffinity* positionAndAffinity);

/**
 * @brief Gets affinity from position and affinity.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param positionandaffinity Indicates the pointer to an <b>OH_Drawing_PositionAndAffinity</b> object.
 * @return Returns affinity from position and affinity.
 * @since 11
 * @version 1.0
 */
int OH_Drawing_GetAffinityFromPositionAndAffinity(OH_Drawing_PositionAndAffinity* positionandaffinity);

/**
 * @brief Gets the word boundary.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to an <b>OH_Drawing_Typography</b> object.
 * @param offset Indicates the size of text to set.
 * @return Returns the word boundary.
 * @since 11
 * @version 1.0
 */
OH_Drawing_Range* OH_Drawing_TypographyGetWordBoundary(OH_Drawing_Typography* typography, size_t offset);

/**
 * @brief Gets start from range.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param range Indicates the pointer to an <b>OH_Drawing_Range</b> object.
 * @return Returns start from range.
 * @since 11
 * @version 1.0
 */
size_t OH_Drawing_GetStartFromRange(OH_Drawing_Range* range);

/**
 * @brief Gets end from range.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param range Indicates the pointer to an <b>OH_Drawing_Range</b> object.
 * @return Returns end from range.
 * @since 11
 * @version 1.0
 */
size_t OH_Drawing_GetEndFromRange(OH_Drawing_Range* range);

/**
 * @brief Gets the line count.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to an <b>OH_Drawing_Typography</b> object.
 * @return Returns the line count.
 * @since 11
 * @version 1.0
 */
size_t OH_Drawing_TypographyGetLineCount(OH_Drawing_Typography* typography);

/**
 * @brief Sets the decoration style.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @param decorationStyle Indicates the text decoration style to set.
 * For details, see the enum <b>OH_Drawing_TextDecorationStyle</b>.
 * @since 11
 * @version 1.0
 */
void OH_Drawing_SetTextStyleDecorationStyle(OH_Drawing_TextStyle* style, int decorationStyle);

/**
 * @brief Sets the decoration thickness scale.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @param decorationThicknessScale Indicates the thickness scale of text decoration to set.
 * @since 11
 * @version 1.0
 */
void OH_Drawing_SetTextStyleDecorationThicknessScale(OH_Drawing_TextStyle* style, double decorationThicknessScale);

/**
 * @brief Sets the letter spacing.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @param letterSpacing Indicates the letter space to set.
 * @since 11
 * @version 1.0
 */
void OH_Drawing_SetTextStyleLetterSpacing(OH_Drawing_TextStyle* style, double letterSpacing);

/**
 * @brief Sets the word spacing.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @param wordSpacing Indicates the word space to set.
 * @since 11
 * @version 1.0
 */
void OH_Drawing_SetTextStyleWordSpacing(OH_Drawing_TextStyle* style, double wordSpacing);

/**
 * @brief Sets the half leading.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @param halfLeading Indicates the half leading to set.
 * @since 11
 * @version 1.0
 */
void OH_Drawing_SetTextStyleHalfLeading(OH_Drawing_TextStyle* style, bool halfLeading);

/**
 * @brief Sets the ellipsis.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @param ellipsis Indicates the pointer to ellipsis style.
 * @since 11
 * @version 1.0
 * @deprecated since 18
 * @useinstead OH_Drawing_SetTypographyTextEllipsis
 */
void OH_Drawing_SetTextStyleEllipsis(OH_Drawing_TextStyle* style, const char* ellipsis);

/**
 * @brief Sets the ellipsis modal.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @param ellipsisModal Indicates the ellipsis model to set. For details, see the enum <b>OH_Drawing_EllipsisModal</b>.
 * @since 11
 * @version 1.0
 * @deprecated since 18
 * @useinstead OH_Drawing_SetTypographyTextEllipsisModal
 */
void OH_Drawing_SetTextStyleEllipsisModal(OH_Drawing_TextStyle* style, int ellipsisModal);

/**
 * @brief Sets the break strategy.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TypographyStyle</b> object.
 * @param breakStrategy Indicates the break strategy to set. For details, see the enum <b>OH_Drawing_BreakStrategy</b>.
 * @since 11
 * @version 1.0
 */
void OH_Drawing_SetTypographyTextBreakStrategy(OH_Drawing_TypographyStyle* style, int breakStrategy);

/**
 * @brief Sets the word break type.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TypographyStyle</b> object.
 * @param wordBreakType Indicates the word break type to set. For details, see the enum <b>OH_Drawing_WordBreakType</b>.
 * @since 11
 * @version 1.0
 */
void OH_Drawing_SetTypographyTextWordBreakType(OH_Drawing_TypographyStyle* style, int wordBreakType);

/**
 * @brief Sets the ellipsis modal.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TypographyStyle</b> object.
 * @param ellipsisModal Indicates the ellipsis modal to set. For details, see the enum <b>OH_Drawing_EllipsisModal</b>.
 * @since 11
 * @version 1.0
 */
void OH_Drawing_SetTypographyTextEllipsisModal(OH_Drawing_TypographyStyle* style, int ellipsisModal);

/**
 * @brief get line height.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to an <b>OH_Drawing_Typography</b> object.
 * @param lineNumber Indicates the line number.
 * @return Returns line height.
 * @since 11
 * @version 1.0
 */
double OH_Drawing_TypographyGetLineHeight(OH_Drawing_Typography* typography, int lineNumber);

/**
 * @brief get line width.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to an <b>OH_Drawing_Typography</b> object.
 * @param lineNumber Indicates the line number.
 * @return Returns line width.
 * @since 11
 * @version 1.0
 */
double OH_Drawing_TypographyGetLineWidth(OH_Drawing_Typography* typography, int lineNumber);

/**
 * @brief get line text range.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to an <b>OH_Drawing_Typography</b> object.
 * @param lineNumber Indicates the line number.
 * @param includeSpaces Indicates whether spaces are contained.
 * @return Returns line text range.
 * @since 12
 * @version 1.0
 */
OH_Drawing_Range* OH_Drawing_TypographyGetLineTextRange(OH_Drawing_Typography* typography,
    int lineNumber, bool includeSpaces);

/**
 * @brief Creates an <b>OH_Drawing_FontDescriptor</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @return Returns the pointer to the font descriptor object <b>OH_Drawing_FontDescriptor</b> created.
 * @since 12
 * @version 1.0
 */
OH_Drawing_FontDescriptor* OH_Drawing_CreateFontDescriptor(void);

/**
 * @brief Releases the memory occupied by an <b>OH_Drawing_FontDescriptor</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param descriptor the pointer to the font descriptor object <b>OH_Drawing_FontDescriptor</b>.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_DestroyFontDescriptor(OH_Drawing_FontDescriptor* descriptor);

/**
 * @brief Creates an <b>OH_Drawing_FontParser</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @return Returns the pointer to the font parser object <b>OH_Drawing_FontParser</b>.
 * @since 12
 * @version 1.0
 */
OH_Drawing_FontParser* OH_Drawing_CreateFontParser(void);

/**
 * @brief Releases the memory occupied by an <b>OH_Drawing_FontParser</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param parser Indicates the pointer to the font parser object <b>OH_Drawing_FontParser</b>.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_DestroyFontParser(OH_Drawing_FontParser* parser);

/**
 * @brief Gets a list of system font names.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param fontParser Indicates the pointer to the font parser object <b>OH_Drawing_FontParser</b>.
 * @param num Returns the number of obtained system font names.
 * @return Returns a list of obtained system fonts.
 * @since 12
 * @version 1.0
 */
char** OH_Drawing_FontParserGetSystemFontList(OH_Drawing_FontParser* fontParser, size_t* num);

/**
 * @brief Releases the memory occupied by a list of system font names.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param fontList Indicates the pointer to a list of system font names.
 * @param num The number of obtained system font names.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_DestroySystemFontList(char** fontList, size_t num);

/**
 * @brief Gets information about the system font by font name.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param fontParser Indicates the pointer to the font parser object <b>OH_Drawing_FontParser</b>.
 * @param name font name.
 * @return Returns system fonts information.
 * @since 12
 * @version 1.0
 */
OH_Drawing_FontDescriptor* OH_Drawing_FontParserGetFontByName(OH_Drawing_FontParser* fontParser, const char* name);

/**
 * @brief Get line metrics information.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to a typography object <b>OH_Drawing_Typography</b>.
 * @return Indicates the pointer to a line metrics object <b>OH_Drawing_LineMetrics</b>.
 * @since 12
 * @version 1.0
 */
OH_Drawing_LineMetrics* OH_Drawing_TypographyGetLineMetrics(OH_Drawing_Typography* typography);

/**
 * @brief Get the number of lines.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param lineMetrics Indicates the pointer to a line metrics object <b>OH_Drawing_LineMetrics</b>.
 * @return Returns the number of lines.
 * @since 12
 * @version 1.0
 */
size_t OH_Drawing_LineMetricsGetSize(OH_Drawing_LineMetrics* lineMetrics);

/**
 * @brief Releases the memory occupied by line metrics.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param lineMetrics Indicates the pointer to a line metrics object <b>OH_Drawing_LineMetrics</b>.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_DestroyLineMetrics(OH_Drawing_LineMetrics* lineMetrics);

/**
 * @brief Gets the specified line by line number.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to a typography object <b>OH_Drawing_Typography</b>.
 * @param lineNumber Line number.
 * @param lineMetric Indicates the pointer to a line metrics object <b>OH_Drawing_LineMetrics</b>.
 * @return Whether the line metrics was obtained.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_TypographyGetLineMetricsAt(OH_Drawing_Typography* typography,
    int lineNumber, OH_Drawing_LineMetrics* lineMetric);

/**
 * @brief  Sets the ellipsis of lines in a text file.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography object <b>OH_Drawing_Typography</b>.
 * @param ellipsis Indicates the line textellipsis.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_SetTypographyTextEllipsis(OH_Drawing_TypographyStyle* style, const char* ellipsis);

/**
 * @brief Sets the locale of lines in a text file.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @param locale Indicates the pointer to the locale to set.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_SetTypographyTextLocale(OH_Drawing_TypographyStyle* style, const char* locale);

/**
 * @brief Sets the textSplitRatio of lines in a text file.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @param textSplitRatio Indicates the textSplitRatio of lines to set.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_SetTypographyTextSplitRatio(OH_Drawing_TypographyStyle* style, float textSplitRatio);

/**
 * @brief Gets the TextStyle of lines in a text file.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @return Returns line text textstyle.
 * @since 12
 * @version 1.0
 */
OH_Drawing_TextStyle* OH_Drawing_TypographyGetTextStyle(OH_Drawing_TypographyStyle* style);

/**
 * @brief Gets the EffectiveAlign of lines in a text file.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @return Returns line text align.
 * @since 12
 * @version 1.0
 * @deprecated since 18
 * @useinstead OH_Drawing_TypographyStyleGetEffectiveAlignment
 */
int OH_Drawing_TypographyGetEffectiveAlignment(OH_Drawing_TypographyStyle* style);

/**
 * @brief Gets the UnlimitedLines of lines in a text file.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @return Returns whether the text has a maximum line limit,
 * with true indicating a maximum line limit and false indicating no maximum line limit.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_TypographyIsLineUnlimited(OH_Drawing_TypographyStyle* style);

/**
 * @brief Gets the IsEllipsized of lines in a text file.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @return Returns whether the text has ellipsis,
 * true meaning there is an ellipsis and false meaning there is no ellipsis.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_TypographyIsEllipsized(OH_Drawing_TypographyStyle* style);

/**
 * @brief set line textstyle.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param handler Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @param style Indicates the pointer to a text style object <b>OH_Drawing_TextStyle</b>.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_SetTypographyTextStyle(OH_Drawing_TypographyStyle* handler, OH_Drawing_TextStyle* style);

/**
 * @brief get line fontmetrics.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to a typography object <b>OH_Drawing_Typography</b>.
 * @param style Indicates the pointer to a text style object <b>OH_Drawing_TextStyle</b>.
 * @param fontmetrics Indicates the pointer to a font metrics object <b>OH_Drawing_Font_Metrics</b>.
 * @return Whether the font metrics was obtained.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_TextStyleGetFontMetrics(OH_Drawing_Typography* typography,
    OH_Drawing_TextStyle* style, OH_Drawing_Font_Metrics* fontmetrics);

/**
 * @brief Gets the position of the specified line or the first text of the specified line.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to a typography object <b>OH_Drawing_Typography</b>.
 * @param lineNumber Line number.
 * @param oneLine True is the information for the whole line, and false is the information to get the first character
 * @param includeWhitespace Whether the text width contains whitespace.
 * @param drawingLineMetrics Indicates the pointer to a line metrics object <b>OH_Drawing_LineMetrics</b>.
 * @return return whether the information was successfully fetched.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_TypographyGetLineInfo(OH_Drawing_Typography* typography, int lineNumber, bool oneLine,
    bool includeWhitespace, OH_Drawing_LineMetrics* drawingLineMetrics);

/**
 * @brief Sets the font weight of text typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @param weight Indicates the font weight of text typography to set. For details,
 * see the enum <b>OH_Drawing_FontWeight</b>.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_SetTypographyTextFontWeight(OH_Drawing_TypographyStyle* style, int weight);

/**
 * @brief Sets the font style of text typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @param fontStyle Indicates the font style of text typography to set. For details,
 * see the enum <b>OH_Drawing_FontStyle</b>.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_SetTypographyTextFontStyle(OH_Drawing_TypographyStyle* style, int fontStyle);

/**
 * @brief Sets the font family of text typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @param fontFamily Indicates the pointer to the font family of text typography to set.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_SetTypographyTextFontFamily(OH_Drawing_TypographyStyle* style, const char* fontFamily);

/**
 * @brief Sets the font size of text typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @param fontSize Indicates the font size of text typography to set.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_SetTypographyTextFontSize(OH_Drawing_TypographyStyle* style, double fontSize);

/**
 * @brief Sets the font height of text typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @param fontHeight Indicates the font height of text typography to set.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_SetTypographyTextFontHeight(OH_Drawing_TypographyStyle* style, double fontHeight);

/**
 * @brief Sets the half leading of text typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @param halfLeading Indicates the half leading of text typography to set.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_SetTypographyTextHalfLeading(OH_Drawing_TypographyStyle* style, bool halfLeading);

/**
 * @brief Sets whether to enable line style for text typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @param useLineStyle Indicates whether the line style for text typography is used.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_SetTypographyTextUseLineStyle(OH_Drawing_TypographyStyle* style, bool useLineStyle);

/**
 * @brief Sets the font weight of line style for text typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @param weight Indicates the font weight of line style for text typography to set.
 * For details, see the enum <b>OH_Drawing_FontWeight</b>.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_SetTypographyTextLineStyleFontWeight(OH_Drawing_TypographyStyle* style, int weight);

/**
 * @brief Sets the font style of line style for text typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @param fontStyle Indicates the font style of line style for text typography to set. For details,
 * see the enum <b>OH_Drawing_FontStyle</b>.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_SetTypographyTextLineStyleFontStyle(OH_Drawing_TypographyStyle* style, int fontStyle);

/**
 * @brief Sets the font families of line style for text typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @param fontFamiliesNumber Indicates the number of font families to set.
 * @param fontFamilies Indicates the pointer to the font families of line style for text typography to set.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_SetTypographyTextLineStyleFontFamilies(OH_Drawing_TypographyStyle* style,
    int fontFamiliesNumber, const char* fontFamilies[]);

/**
 * @brief Sets the font size of line style for text typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @param lineStyleFontSize Indicates the font size of line style for text typography to set.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_SetTypographyTextLineStyleFontSize(OH_Drawing_TypographyStyle* style, double lineStyleFontSize);

/**
 * @brief Sets the font height of line style for text typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @param lineStyleFontHeight Indicates the font height of line style for text typography to set.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_SetTypographyTextLineStyleFontHeight(OH_Drawing_TypographyStyle* style, double lineStyleFontHeight);

/**
 * @brief Sets the half leading of line style for text typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @param lineStyleHalfLeading Indicates the half leading of line for text typography to set.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_SetTypographyTextLineStyleHalfLeading(OH_Drawing_TypographyStyle* style, bool lineStyleHalfLeading);

/**
 * @brief Sets the spacing scale of line style for text typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @param spacingScale Indicates the space scale of line for text typography to set.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_SetTypographyTextLineStyleSpacingScale(OH_Drawing_TypographyStyle* style, double spacingScale);

/**
 * @brief Sets whether only line style is enabled for text typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @param lineStyleOnly Indicates the line style for text typography to set only.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_SetTypographyTextLineStyleOnly(OH_Drawing_TypographyStyle* style, bool lineStyleOnly);

/**
 * @brief Creates an <b>OH_Drawing_TextShadow</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @return Returns the pointer to the text shadow object created <b>OH_Drawing_TextShadow</b>.
 * @since 12
 * @version 1.0
 */
OH_Drawing_TextShadow* OH_Drawing_CreateTextShadow(void);

/**
 * @brief Releases the memory occupied by the text shadow object <b>OH_Drawing_TextShadow</b>.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param shadow Indicates the pointer to the text shadow object <b>OH_Drawing_TextShadow</b>.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_DestroyTextShadow(OH_Drawing_TextShadow* shadow);

/**
 * @brief Gets the vector of TextShadow in TextStyle.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a text style object <b>OH_Drawing_TextStyle</b>.
 * @param int Indicates the number in vector to set.
 * @param style Indicates the pointer to the text shadow object <b>OH_Drawing_TextShadow</b>.
 * @return Returns the vector of TextShadow.
 * @since 12
 * @version 1.0
 */
OH_Drawing_TextShadow* OH_Drawing_TextStyleGetShadows(OH_Drawing_TextStyle* style);

/**
 * @brief Gets the size of vector of TextShadow in TextStyle.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a text style object <b>OH_Drawing_TextStyle</b>.
 * @return Returns the size of vector.
 * @since 12
 * @version 1.0
 */
int OH_Drawing_TextStyleGetShadowCount(OH_Drawing_TextStyle* style);

/**
 * @brief Adds element in vector of TextShadow in TextStyle.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a text style object <b>OH_Drawing_TextStyle</b>.
 * @param shadow Indicates the pointer to the text shadow object <b>OH_Drawing_TextShadow</b>.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_TextStyleAddShadow(OH_Drawing_TextStyle* style, const OH_Drawing_TextShadow* shadow);

/**
 * @brief clear elements in vector of TextShadow in TextStyle.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a text style object <b>OH_Drawing_TextStyle</b>.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_TextStyleClearShadows(OH_Drawing_TextStyle* style);

/**
 * @brief Gets element in vector of TextShadow with index.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a text style object <b>OH_Drawing_TextStyle</b>.
 * @param index Indicates the index to set.
 * @return Returns the pointer to element with the index in vector of the text style object
 * <b>OH_Drawing_TextStyle</b>.
 * @since 12
 * @version 1.0
 */
OH_Drawing_TextShadow* OH_Drawing_TextStyleGetShadowWithIndex(OH_Drawing_TextStyle* style, int index);

/**
 * @brief Set indents of the typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to a typography object <b>OH_Drawing_Typography</b>.
 * @param indentsNumber Indicates the pointer to the indents to set.
 * @param indents Indicates the pointer to the indents to set.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_TypographySetIndents(OH_Drawing_Typography* typography, int indentsNumber, const float indents[]);

/**
 * @brief Gets element with index in vector of Indents.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to a typography object <b>OH_Drawing_Typography</b>.
 * @param index Indicates the index to set.
 * @return float Indicates the element with the index in vector of Indents.
 * @since 12
 * @version 1.0
 */
float OH_Drawing_TypographyGetIndentsWithIndex(OH_Drawing_Typography* typography, int index);

/**
 * @brief Releases the memory occupied by vector with the text shadow object <b>OH_Drawing_TextShadow</b>.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param shadow the pointer to the text shadow object <b>OH_Drawing_TextShadow</b>.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_DestroyTextShadows(OH_Drawing_TextShadow* shadow);

/**
 * @brief Set mode of applying the leading over and under text.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TypographyStyle</b> object.
 * @param heightMode Indicates the mode to set.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_TypographyTextSetHeightBehavior(OH_Drawing_TypographyStyle* style,
    OH_Drawing_TextHeightBehavior heightMode);

/**
 * @brief Get mode of applying the leading over and under text.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TypographyStyle</b> object.
 * @return Returns the mode.
 * @since 12
 * @version 1.0
 */
OH_Drawing_TextHeightBehavior OH_Drawing_TypographyTextGetHeightBehavior(OH_Drawing_TypographyStyle* style);

/**
 * @brief Set struct of background rect and styleId of text.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @param rectStyleInfo Indicates the pointer to an <b>OH_Drawing_RectStyle_Info</b> object.
 * @param styleId Indicates the styleId of text to set.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_TextStyleSetBackgroundRect(OH_Drawing_TextStyle* style,
    const OH_Drawing_RectStyle_Info* rectStyleInfo, int styleId);

/**
 * @brief Add symbols in creating typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param handler Indicates the pointer to an <b>OH_Drawing_TypographyCreate</b> object.
 * @param symbol Indicates the symbol to set.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_TypographyHandlerAddSymbol(OH_Drawing_TypographyCreate* handler, uint32_t symbol);

/**
 * @brief Add font feature.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @param tag Indicates the pointer to the tag to set.
 * @param value Indicates the value to set.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_TextStyleAddFontFeature(OH_Drawing_TextStyle* style, const char* tag, int value);

/**
 * @brief Add font variation.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @param axis Indicates the pointer to font variation axis.
 * @param value Indicates the font variation value to set.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_TextStyleAddFontVariation(OH_Drawing_TextStyle* style, const char* axis, const float value);

/**
 * @brief Get all font features.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @return OH_Drawing_FontFeature Indicates the pointer to an array of structures of OH_Drawing_FontFeature.
 * Get size of font feature by OH_Drawing_TextStyleGetFontFeatureSize.
 * @since 12
 * @version 1.0
 */
OH_Drawing_FontFeature* OH_Drawing_TextStyleGetFontFeatures(OH_Drawing_TextStyle* style);

/**
 * @brief Release the memory occupied by array of structures of font features.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param fontFeature Indicates the pointer to an array of structures of OH_Drawing_FontFeature.
 * @param fontFeatureSize Indicates the size of array of structures of OH_Drawing_FontFeature.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_TextStyleDestroyFontFeatures(OH_Drawing_FontFeature* fontFeature, size_t fontFeatureSize);

/**
 * @brief Get size of font features.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @return Returns the size of fontfeatures map.
 * @since 12
 * @version 1.0
 */
size_t OH_Drawing_TextStyleGetFontFeatureSize(OH_Drawing_TextStyle* style);

/**
 * @brief Clear font features.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_TextStyleClearFontFeature(OH_Drawing_TextStyle* style);

/**
 * @brief Set baseline shift of text.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @param lineShift Indicates the baseline shift to set.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_TextStyleSetBaselineShift(OH_Drawing_TextStyle* style, double lineShift);

/**
 * @brief Get baseline shift of text.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @return Returns the baseline shift.
 * @since 12
 * @version 1.0
 */
double OH_Drawing_TextStyleGetBaselineShift(OH_Drawing_TextStyle* style);

/**
 * @brief Gets the text color.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @return Returns the text color.
 * @since 12
 * @version 1.0
 */
uint32_t OH_Drawing_TextStyleGetColor(OH_Drawing_TextStyle* style);

/**
 * @brief Gets text decoration style.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @return Returns text decoration style.
 * @since 12
 * @version 1.0
 */
OH_Drawing_TextDecorationStyle OH_Drawing_TextStyleGetDecorationStyle(OH_Drawing_TextStyle* style);

/**
 * @brief Gets font weight.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @return Returns font Weight.
 * @since 12
 * @version 1.0
 */
OH_Drawing_FontWeight OH_Drawing_TextStyleGetFontWeight(OH_Drawing_TextStyle* style);

/**
 * @brief Gets font style.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @return Returns font style.
 * @since 12
 * @version 1.0
 */
OH_Drawing_FontStyle OH_Drawing_TextStyleGetFontStyle(OH_Drawing_TextStyle* style);

/**
 * @brief Gets the font baseline.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @return Returns the font baseline.
 * @since 12
 * @version 1.0
 */
OH_Drawing_TextBaseline OH_Drawing_TextStyleGetBaseline(OH_Drawing_TextStyle* style);

/**
 * @brief Gets a list of font families.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @param num Indicates count of font families result.
 * @return Returns a list of font families.
 * @since 12
 * @version 1.0
 */
char** OH_Drawing_TextStyleGetFontFamilies(OH_Drawing_TextStyle* style, size_t* num);

/**
 * @brief Releases the memory occupied by a list of font families.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param fontFamilies Indicates the pointer to a list of font families.
 * @param num Indicates the count of obtained font families.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_TextStyleDestroyFontFamilies(char** fontFamilies, size_t num);

/**
 * @brief Gets font size.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @return Returns font size.
 * @since 12
 * @version 1.0
 */
double OH_Drawing_TextStyleGetFontSize(OH_Drawing_TextStyle* style);

/**
 * @brief Gets the letter spacing of the text.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @return Returns the size of the letter spacing.
 * @since 12
 * @version 1.0
 */
double OH_Drawing_TextStyleGetLetterSpacing(OH_Drawing_TextStyle* style);

/**
 * @brief Gets the word spacing of the text.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @return Returns word spacing size.
 * @since 12
 * @version 1.0
 */
double OH_Drawing_TextStyleGetWordSpacing(OH_Drawing_TextStyle* style);

/**
 * @brief Gets font height.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @return Returns font height.
 * @since 12
 * @version 1.0
 */
double OH_Drawing_TextStyleGetFontHeight(OH_Drawing_TextStyle* style);

/**
 * @brief Gets whether to set the text to half line spacing.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @return Returns true indicates that the spacing takes effect,
 * false indicates that the spacing does not take effect.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_TextStyleGetHalfLeading(OH_Drawing_TextStyle* style);

/**
 * @brief Sets the typography vertical alignment mode.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TypographyStyle</b> object.
 * @param align Indicates the typography vertical alignment mode. For details,
 * see the enum <b>OH_Drawing_TextVerticalAlignment</b>.
 * @since 20
 * @version 1.0
 */
void OH_Drawing_SetTypographyVerticalAlignment(OH_Drawing_TypographyStyle* style,
    OH_Drawing_TextVerticalAlignment align);

/**
 * @brief Gets the locale.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @return Returns a locale of data type as a pointer to a char. As with the TextStyle lifecycle.
 * No release is required and the return value is invalidated after the set method is called.
 * @since 12
 * @version 1.0
 */
const char* OH_Drawing_TextStyleGetLocale(OH_Drawing_TextStyle* style);

/**
 * @brief Sets whether to use superscript or subscript in text layout.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Pointer to an OH_Drawing_TextStyle object.
 * @param textBadgeType Superscript or subscript to use.
 * @since 20
 */
void OH_Drawing_SetTextStyleBadgeType(OH_Drawing_TextStyle* style, OH_Drawing_TextBadgeType textBadgeType);

/**
 * @brief Sets the text style, including font weight, font width and font slant.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param drawingTextStyle Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @param fontStyle Indicates an <b>OH_Drawing_FontStyleStruct</b> object.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_SetTextStyleFontStyleStruct(OH_Drawing_TextStyle* drawingTextStyle,
    OH_Drawing_FontStyleStruct fontStyle);

/**
 * @brief Gets the text style, including font weight, font width and font slant.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param drawingTextStyle Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @return Returns the <b>OH_Drawing_FontStyleStruct</b> object getted.
 * @since 12
 * @version 1.0
 */
OH_Drawing_FontStyleStruct OH_Drawing_TextStyleGetFontStyleStruct(OH_Drawing_TextStyle* drawingTextStyle);

/**
 * @brief Sets the typography style, including font weight, font width and font slant.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param drawingStyle Indicates the pointer to an <b>OH_Drawing_TypographyStyle</b> object.
 * @param fontStyle Indicates an <b>OH_Drawing_FontStyleStruct</b> object.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_SetTypographyStyleFontStyleStruct(OH_Drawing_TypographyStyle* drawingStyle,
    OH_Drawing_FontStyleStruct fontStyle);

/**
 * @brief Gets the typography style, including font weight, font width and font slant.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param drawingStyle Indicates the pointer to an <b>OH_Drawing_TypographyStyle</b> object.
 * @return Returns the <b>OH_Drawing_FontStyleStruct</b> object getted.
 * @since 12
 * @version 1.0
 */
OH_Drawing_FontStyleStruct OH_Drawing_TypographyStyleGetFontStyleStruct(OH_Drawing_TypographyStyle* drawingStyle);

/**
 * @brief Gets whether the two TextStyle objects are equal.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates source of comparison <b>OH_Drawing_TextStyle</b> object.
 * @param comparedStyle Indicates comparison <b>OH_Drawing_TextStyle</b> object.
 * @return Compare result.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_TextStyleIsEqual(const OH_Drawing_TextStyle* style, const OH_Drawing_TextStyle* comparedStyle);

/**
 * @brief Gets whether the font properties of two TextStyle objects are equal.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates source of <b>comparison OH_Drawing_TextStyle</b> object.
 * @param comparedStyle Indicates comparison <b>OH_Drawing_TextStyle</b> object.
 * @return Compare result.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_TextStyleIsEqualByFont(const OH_Drawing_TextStyle* style, const OH_Drawing_TextStyle* comparedStyle);

/**
 * @brief Gets whether two TextStyle objects match attributes
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates enumerates of text style type.
 * @param comparedStyle Indicates source of comparison <b>OH_Drawing_TextStyle</b> object.
 * @param textStyleType Indicates comparison <b>OH_Drawing_TextStyle</b> object.
 * @return Match attributes result.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_TextStyleIsAttributeMatched(const OH_Drawing_TextStyle* style,
    const OH_Drawing_TextStyle* comparedStyle, OH_Drawing_TextStyleType textStyleType);

/**
 * @brief Set placeholder of TextStyle.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_TextStyleSetPlaceholder(OH_Drawing_TextStyle* style);

/**
 * @brief Gets whether placeholder is enable.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @return Whether placeholder is enable.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_TextStyleIsPlaceholder(OH_Drawing_TextStyle* style);

/**
 * @brief Gets text alignment mode.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TypographyStyle</b> object.
 * @return Returns text alignment mode.
 * @since 12
 * @version 1.0
 */
OH_Drawing_TextAlign OH_Drawing_TypographyStyleGetEffectiveAlignment(OH_Drawing_TypographyStyle* style);

/**
 * @brief Gets whether the hinting is enabled.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TypographyStyle</b> object.
 * @return True, if the hinting takes effect; False, if the hinting does not take effect.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_TypographyStyleIsHintEnabled(OH_Drawing_TypographyStyle* style);

/**
 * @brief Gets system font configuration information.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param errorCode Indicates error code returned, based on the error code to
 * release the memory of system font configuration information.
 * For details, see the enum <b>OH_Drawing_FontConfigInfoErrorCode</b>.
 * @return Returns a pointer to system font configuration information.
 * Indicates the pointer to an <b>OH_Drawing_FontConfigInfo</b> object.
 * @since 12
 * @version 1.0
 */
OH_Drawing_FontConfigInfo* OH_Drawing_GetSystemFontConfigInfo(OH_Drawing_FontConfigInfoErrorCode* errorCode);

/**
 * @brief Releases the memory occupied by system font configuration information.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param drawFontCfgInfo Indicates the pointer to an <b>OH_Drawing_FontConfigInfo</b> object.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_DestroySystemFontConfigInfo(OH_Drawing_FontConfigInfo* drawFontCfgInfo);

/**
 * @brief Sets the strut style for text typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TypographyStyle</b> object.
 * @param strutstyle Indicates the pointer of <b>OH_Drawing_StrutStyle</b> object.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_SetTypographyStyleTextStrutStyle(OH_Drawing_TypographyStyle* style, OH_Drawing_StrutStyle* strutstyle);

/**
 * @brief Releases the memory occupied by an <b>OH_Drawing_StrutStyle</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param strutstyle Indicates the pointer of <b>OH_Drawing_StrutStyle</b> object.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_TypographyStyleDestroyStrutStyle(OH_Drawing_StrutStyle* strutstyle);

/**
 * @brief Gets the strut style for text typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TypographyStyle</b> object.
 * @return Returns the pointer of <b>OH_Drawing_StrutStyle</b> object.
 * @since 12
 * @version 1.0
 */
OH_Drawing_StrutStyle* OH_Drawing_TypographyStyleGetStrutStyle(OH_Drawing_TypographyStyle* style);

/**
 * @brief Overriding the struct StrutStyle equals operator.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param from Indicates source of comparison object.
 * @param to Indicates comparison object.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_TypographyStyleStrutStyleEquals(OH_Drawing_StrutStyle* from, OH_Drawing_StrutStyle* to);

/**
 * @brief Sets the hinting of text typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TypographyStyle</b> object.
 * @param hintsEnabled Indicates the hinting of text typography..
 * @since 12
 * @version 1.0
 */
void OH_Drawing_TypographyStyleSetHintsEnabled(OH_Drawing_TypographyStyle* style, bool hintsEnabled);

/**
 * @brief Getting all font metrics from target row.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates a pointer to a typesetting object.
 * @param lineNumber Indicates specifies the number of rows.
 * @param fontMetricsSize Indicates the return size of font metrics struct from current line.
 * @return Returns all character measures for the current row.
 * @since 12
 * @version 1.0
 */
OH_Drawing_Font_Metrics* OH_Drawing_TypographyGetLineFontMetrics(OH_Drawing_Typography* typography,
    size_t lineNumber, size_t* fontMetricsSize);

/**
 * @brief Free up all the space taken up by the lineFontMetric.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param lineFontMetric Indicates the first address of the lineFontMetric gather to be destroyed.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_TypographyDestroyLineFontMetrics(OH_Drawing_Font_Metrics* lineFontMetric);

/**
 * @brief Mark the Typography as dirty, and initially state the Typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to the text <b>OH_Drawing_Typography</b> object.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_TypographyMarkDirty(OH_Drawing_Typography* typography);

/**
 * @brief Get the unresolved Glyphs count of lines in a text.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to the text <b>OH_Drawing_Typography</b> object.
 * @return Returns unresolved Glyphs count.
 * @since 12
 * @version 1.0
 */
int32_t OH_Drawing_TypographyGetUnresolvedGlyphsCount(OH_Drawing_Typography* typography);

/**
 * @brief Update the font size of lines in a text.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to the text <b>OH_Drawing_Typography</b> object.
 * @param from Indicates the source of the original font size.
 * @param to Indicates the destination of the updated font size.
 * @param fontSize Indicates the size of the font.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_TypographyUpdateFontSize(OH_Drawing_Typography* typography, size_t from, size_t to, float fontSize);

/**
 * @brief Update the font color of the typography
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to the text <b>OH_Drawing_Typography</b> object.
 * @param color Indicates the color to update.
 * @since 20
 * @version 1.0
 */
void OH_Drawing_TypographyUpdateFontColor(OH_Drawing_Typography* typography, uint32_t color);

/**
 * @brief Update the decoration of the typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to the text <b>OH_Drawing_Typography</b> object.
 * @param decoration Indicates the text decoration to update. For details, see the enum
 * <b>OH_Drawing_TextDecoration</b>.
 * @since 20
 * @version 1.0
 */
void OH_Drawing_TypographyUpdateDecoration(OH_Drawing_Typography* typography, OH_Drawing_TextDecoration decoration);

/**
 * @brief Update the decoration thickness scale of the typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to the text <b>OH_Drawing_Typography</b> object.
 * @param decorationThicknessScale Indicates the thickness scale of the text decoration to update.
 * @since 20
 * @version 1.0
 */
void OH_Drawing_TypographyUpdateDecorationThicknessScale(OH_Drawing_Typography* typography,
    double decorationThicknessScale);

/**
 * @brief Update the decoration style of typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to the text <b>OH_Drawing_Typography</b> object.
 * @param decorationStyle Indicates the text decoration style to update.
 * For details, see the enum <b>OH_Drawing_TextDecorationStyle</b>.
 * @since 20
 * @version 1.0
 */
void OH_Drawing_TypographyUpdateDecorationStyle(OH_Drawing_Typography* typography,
    OH_Drawing_TextDecorationStyle decorationStyle);

/**
 * @brief Updates the decoration color of the paragraph.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param typography Indicates the pointer to the text <b>OH_Drawing_Typography</b> object.
 * @param color Indicates the text decoration color to update.
 * @since 20
 * @version 1.0
 */
void OH_Drawing_TypographyUpdateDecorationColor(OH_Drawing_Typography* typography, uint32_t color);

/**
 * @brief Get whether the text layout enables line styles.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to the text <b>OH_Drawing_TypographyStyle</b> object.
 * @return Whether or not to enable line styles in text layout only, true means enable, false means disable.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_TypographyTextGetLineStyle(OH_Drawing_TypographyStyle* style);

/**
 * @brief Get the font weight of line style for text typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @return Return the font weight of line style for text typography.
 * For details, see the enum <b>OH_Drawing_FontWeight</b>.
 * @since 12
 * @version 1.0
 */
OH_Drawing_FontWeight OH_Drawing_TypographyTextlineStyleGetFontWeight(OH_Drawing_TypographyStyle* style);

/**
 * @brief Get the font style of line style for text typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @return Return the font style of line style for text typography.
 * For details, see the enum <b>OH_Drawing_FontStyle</b>.
 * @since 12
 * @version 1.0
 */
OH_Drawing_FontStyle OH_Drawing_TypographyTextlineStyleGetFontStyle(OH_Drawing_TypographyStyle* style);

/**
 * @brief Get the font families of line style for text typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @param num The number of obtained font names.
 * @return Return the font families of line style for text typography.
 * @since 12
 * @version 1.0
 */
char** OH_Drawing_TypographyTextlineStyleGetFontFamilies(OH_Drawing_TypographyStyle* style, size_t* num);

/**
 * @brief Releases the memory occupied by a list of font families names.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param fontFamilies Indicates the pointer to a list of font families names.
 * @param fontFamiliesNum Indicates the number of obtained font names.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_TypographyTextlineStyleDestroyFontFamilies(char** fontFamilies, size_t fontFamiliesNum);

/**
 * @brief Get the font size of font size for text typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @return Return the font size of font size for text typography.
 * @since 12
 * @version 1.0
 */
double OH_Drawing_TypographyTextlineStyleGetFontSize(OH_Drawing_TypographyStyle* style);

/**
 * @brief Get the font height scale in text layout.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @return Retrun the font height scale in text layout.
 * @since 12
 * @version 1.0
 */
double OH_Drawing_TypographyTextlineStyleGetHeightScale(OH_Drawing_TypographyStyle* style);

/**
 * @brief Get whether to enable font height for line styles in text layout only.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @return Whether or not to enable the font height for line styles in text layout only,
 * true means enable, false means disable.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_TypographyTextlineStyleGetHeightOnly(OH_Drawing_TypographyStyle* style);

/**
 * @brief Get the half leading of line style for text typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @return Whether to enable the text line half leading style, true means enable, false means disable.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_TypographyTextlineStyleGetHalfLeading(OH_Drawing_TypographyStyle* style);

/**
 * @brief Get the spacing scale of line style for text typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @return Return the spacing scale of line style for text typography.
 * @since 12
 * @version 1.0
 */
double OH_Drawing_TypographyTextlineStyleGetSpacingScale(OH_Drawing_TypographyStyle* style);

/**
 * @brief Get whether only line style is enabled for text typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @return Returns whether only line style is enabled for text layout, true means it is enabled,
 * false means it is not.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_TypographyTextlineGetStyleOnly(OH_Drawing_TypographyStyle* style);

/**
 * @brief Get the text alignment mode.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @return Return the text alignment mode. For details, see the enum <b>OH_Drawing_TextAlign</b>.
 * @since 12
 * @version 1.0
 */
OH_Drawing_TextAlign OH_Drawing_TypographyGetTextAlign(OH_Drawing_TypographyStyle* style);

/**
 * @brief Get the text direction.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @return Return the text direction. For details, see the enum <b>OH_Drawing_TextDirection</b>.
 * @since 12
 * @version 1.0
 */
OH_Drawing_TextDirection OH_Drawing_TypographyGetTextDirection(OH_Drawing_TypographyStyle* style);

/**
 * @brief Sets the maximum number of lines in a text.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @return Return the maximum number of lines in a text.
 * @since 12
 * @version 1.0
 */
size_t OH_Drawing_TypographyGetTextMaxLines(OH_Drawing_TypographyStyle* style);

/**
 * @brief Get the ellipsis of lines in a text.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to a typography style object
 * <b>OH_Drawing_TypographyStyle</b>.
 * @return Return the ellipsis of lines in a text.
 * @since 12
 * @version 1.0
 */
char* OH_Drawing_TypographyGetTextEllipsis(OH_Drawing_TypographyStyle* style);

/**
 * @brief Releases the memory occupied by a list of Ellipsis names.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param ellipsis Indicates the pointer to a list of Ellipsis names.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_TypographyDestroyEllipsis(char* ellipsis);

/**
 * @brief Overriding the class ParagraphStyle equals operator.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param from Indicates source of comparison object.
 * @param to Indicates comparison object.
 * @return Compare result.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_TypographyStyleEquals(OH_Drawing_TypographyStyle* from, OH_Drawing_TypographyStyle* to);

/**
 * @brief Releases the memory occupied by text box.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param textBox Indicates the pointer to a text box object <b>OH_Drawing_TextBox</b>.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_TypographyDestroyTextBox(OH_Drawing_TextBox* textBox);

/**
 * @brief Sets the parameter of text-shadow.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param shadow Indicates the pointer to an <b>OH_Drawing_TextShadow</b> object.
 * @param color Indicates the color setting of text-shadow.
 * @param offset Indicates the pointer to an <b>OH_Drawing_Point</b> object.
 * @param blurRadius Indicates the radius of blur for text-shadow.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_SetTextShadow(OH_Drawing_TextShadow* shadow, uint32_t color, OH_Drawing_Point* offset,
    double blurRadius);

/**
 * @brief Creates an <b>OH_Drawing_TextTab</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param alignment Indicates enumerates text tab alignment modes. TAB alignment, Support left alignment
 * right alignment center alignment, other enumeration values are left alignment effect.
 * @param location Indicates location of text tab.
 * @return Returns the pointer to the <b>OH_Drawing_TextTab</b> object created. If the object returns NULL,
 * the creation failed. The possible cause of the failure is that the application address space is used up.
 * As a result, space cannot be allocated.
 * @since 18
 * @version 1.0
 */
OH_Drawing_TextTab* OH_Drawing_CreateTextTab(OH_Drawing_TextAlign alignment, float location);

/**
 * @brief Releases the memory occupied by an <b>OH_Drawing_TextTab</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param tab Indicates the pointer to an <b>OH_Drawing_TextTab</b> object.
 * @since 18
 * @version 1.0
 */
void OH_Drawing_DestroyTextTab(OH_Drawing_TextTab* tab);

/**
 * @brief Get alignment of an <b>OH_Drawing_TextTab</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param tab Indicates the pointer to an <b>OH_Drawing_TextTab</b> object.
 * @return Returns align of an <b>OH_Drawing_TextTab</b> object.
 * @since 18
 * @version 1.0
 */
OH_Drawing_TextAlign OH_Drawing_GetTextTabAlignment(OH_Drawing_TextTab* tab);

/**
 * @brief Get location of an <b>OH_Drawing_TextTab</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param tab Indicates the pointer to an <b>OH_Drawing_TextTab</b> object.
 * @return Returns location of an <b>OH_Drawing_TextTab</b> object.
 * @since 18
 * @version 1.0
 */
float OH_Drawing_GetTextTabLocation(OH_Drawing_TextTab* tab);

/**
 * @brief Sets the text tab of <b>OH_Drawing_TypographyStyle</b> object.
 * Tab alignment does not take effect when text alignment is also set, Or when the ellipsis style is configured.
 * When the tab is not set or the tab's location property is less than or equal to 0, it is the default space effect.
 * And all tabs in the paragraph after the setting are aligned according to this tab effect.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TypographyStyle</b> object.
 * @param tab Indicates the pointer to an <b>OH_Drawing_TextTab</b> object.
 * @since 18
 * @version 1.0
 */
void OH_Drawing_SetTypographyTextTab(OH_Drawing_TypographyStyle* style, OH_Drawing_TextTab* tab);

/**
 * @brief Get DrawingArray size.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param drawingArray Indicates the pointer to the array object <b>OH_Drawing_Array</b>.
 * @return Size of array.
 * @since 14
 * @version 1.0
 */
size_t OH_Drawing_GetDrawingArraySize(OH_Drawing_Array* drawingArray);

/**
 * @brief Sets whether to optimize whitespace at the end of each line for text typography.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TypographyStyle</b> object.
 * @param trailingSpaceOptimized Boolean value indicating whether to optimize whitespace at the end of each line
 * for text typography to set.
 * @since 20
 * @version 1.0
 */
void OH_Drawing_SetTypographyTextTrailingSpaceOptimized(OH_Drawing_TypographyStyle* style, bool trailingSpaceOptimized);

/**
 * @brief Sets the text content. The content supports UTF-8, UTF-16, and UTF-32 formats.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param handler Indicates the pointer to an <b>OH_Drawing_TypographyCreate</b> object.
 * @param text Indicates the pointer to the text content to set.
 * @param byteLength Set the byte length of the text content.
 * @param textEncodingType Indicates the text encoding type <b>OH_Drawing_TextEncoding</b>.
 * @since 20
 * @version 1.0
 */
void OH_Drawing_TypographyHandlerAddEncodedText(OH_Drawing_TypographyCreate* handler, const void* text,
    size_t byteLength, OH_Drawing_TextEncoding textEncodingType);

/**
 * @brief Set whether to enable automatic spacing between Chinese and English for paragraph.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TypographyStyle</b> object.
 * @param enableAutoSpace Indicates Whether to enable automatic spacing between Chinese and English.
 * @since 20
 */
void OH_Drawing_SetTypographyTextAutoSpace(OH_Drawing_TypographyStyle* style, bool enableAutoSpace);

/**
 * @brief Copy a typography style object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TypographyStyle</b> object.
 * @return Returns the pointer to the copied <b>OH_Drawing_TypographyStyle</b> object.
 * @since 20
 * @version 1.0
 */
OH_Drawing_TypographyStyle* OH_Drawing_CopyTypographyStyle(OH_Drawing_TypographyStyle* style);

/**
 * @brief Copy a text style object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param style Indicates the pointer to an <b>OH_Drawing_TextStyle</b> object.
 * @return Returns the pointer to the copied <b>OH_Drawing_TextStyle</b> object.
 * @since 20
 * @version 1.0
 */
OH_Drawing_TextStyle* OH_Drawing_CopyTextStyle(OH_Drawing_TextStyle* style);

/**
 * @brief Copy a text shadow object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param shadow Indicates the pointer to an <b>OH_Drawing_TextShadow</b> object.
 * @return Returns the pointer to the copied <b>OH_Drawing_TextShadow</b> object.
 * @since 20
 * @version 1.0
 */
OH_Drawing_TextShadow* OH_Drawing_CopyTextShadow(OH_Drawing_TextShadow* shadow);

#ifdef __cplusplus
}
#endif
/** @} */
#endif
