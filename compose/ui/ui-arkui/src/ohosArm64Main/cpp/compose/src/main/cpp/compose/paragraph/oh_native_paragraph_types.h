#ifndef NATIVE_PARAGRAPH_TYPES_H
#define NATIVE_PARAGRAPH_TYPES_H

#include <cstdint>
#include <native_drawing/drawing_text_declaration.h>
#include <string>

namespace OH {

/**
 * 文本对齐方式枚举
 */
enum class TextAlign {
    Left = 0,
    Right = 1,
    Center = 2,
    Justify = 3,
    Start = 4,
    End = 5
};

/**
 * 文本方向枚举
 */
enum class TextDirection {
    RTL = 0,
    LTR = 1
};

/**
 * 字体样式枚举
 */
enum class FontStyle {
    Normal = 0,
    Italic = 1
};

/**
 * 字体粗细枚举
 */
enum class FontWeight {
    Thin = 100,
    ExtraLight = 200,
    Light = 300,
    Normal = 400,
    Medium = 500,
    SemiBold = 600,
    Bold = 700,
    ExtraBold = 800,
    Black = 900
};

/**
 * @brief Represents the metrics for a single line of text in a paragraph.
 *
 * This structure contains all the layout and positioning information for a line,
 * including character indices, geometric bounds, and typographic measurements.
 */
struct LineMetrics {
    // The start character index of this line in the paragraph.
    uint32_t startIndex;

    // The end character index of this line in the paragraph (inclusive).
    uint32_t endIndex;

    // The end character index excluding trailing whitespace characters.
    uint32_t endExcludingWhitespaces;

    // The left edge position of the line in pixels.
    double left;

    // The top edge position of the line in pixels.
    double top;

    // The right edge position of the line in pixels.
    double right;

    // The bottom edge position of the line in pixels.
    double bottom;

    // The width of the line in pixels.
    double width;

    // The height of the line in pixels.
    double height;

    // The baseline position of the line relative to the top.
    double baseline;

    // The ascent of the line (distance from baseline to top).
    double ascent;

    // The descent of the line (distance from baseline to bottom).
    double descent;

    /**
     * @brief Default constructor that initializes all members to zero.
     */
    LineMetrics() : startIndex(0), endIndex(0), endExcludingWhitespaces(0),
                    left(0.0), top(0.0), right(0.0), bottom(0.0),
                    width(0.0), height(0.0), baseline(0.0),
                    ascent(0.0), descent(0.0) {
    }
};

/**
 * 矩形结构体
 */
struct TextRect {
    double left;
    double top;
    double right;
    double bottom;

    TextRect() : left(0.0), top(0.0), right(0.0), bottom(0.0) {
    }
    TextRect(double l, double t, double r, double b) : left(l), top(t), right(r), bottom(b) {
    }
};

/**
 * 单词边界结构体
 */
struct WordBoundary {
    uint32_t start;
    uint32_t end;

    WordBoundary() : start(0), end(0) {
    }
    WordBoundary(uint32_t s, uint32_t e) : start(s), end(e) {
    }
};

/**
 * @brief Represents the metrics and measurements of a paragraph layout.
 *
 * This structure contains various measurements and properties that describe
 * the dimensions and characteristics of a laid-out paragraph, including its
 * size, baselines, and line information.
 */
struct ParagraphMetrics {
    // The total width of the paragraph in pixels
    double width;

    // The total height of the paragraph in pixels
    double height;

    // The minimum intrinsic width required to layout the paragraph without wrapping
    double minIntrinsicWidth;

    // The maximum intrinsic width when all text is laid out on a single line
    double maxIntrinsicWidth;

    // The distance from the top of the paragraph to the alphabetic baseline
    double alphabeticBaseline;

    // The distance from the top of the paragraph to the ideographic baseline
    double ideographicBaseline;

    // The width of the longest line in the paragraph
    double longestLine;

    // The total number of lines in the paragraph
    uint32_t lineCount;

    // Indicates whether the paragraph exceeded the maximum number of allowed lines
    bool didExceedMaxLines;

    ParagraphMetrics() : width(0.0), height(0.0), minIntrinsicWidth(0.0),
                         maxIntrinsicWidth(0.0), alphabeticBaseline(0.0),
                         ideographicBaseline(0.0), longestLine(0.0),
                         lineCount(0), didExceedMaxLines(false) {
    }
};

/**
 * 文本装饰枚举
 */
enum class TextDecoration {
    None = 0,
    Underline = 1,
    LineThrough = 2,
    UnderlineAndLineThrough = 3
};

/**
 * 占位符垂直对齐方式
 */
enum class PlaceholderVerticalAlign {
    AboveBaseline = 0,
    Top = 1,
    Bottom = 2,
    Center = 3,
    TextTop = 4,
    TextBottom = 5,
    TextCenter = 6
};

/**
 * SpanStyle 范围结构体
 * 用于表示文本中的局部样式
 */
struct SpanStyleRange {
    uint32_t start;           // 起始位置
    uint32_t end;             // 结束位置

    // 可选样式属性（使用负值表示未设置）
    int fontWeight;           // 字体粗细（-1表示未设置）
    double fontSize;          // 字体大小（-1.0表示未设置）
    int fontStyle;            // 字体样式（-1表示未设置）
    uint32_t color;           // 颜色（使用特殊值0xFFFFFFFF表示未设置）
    uint32_t background;
    double letterSpacing;     // 字母间距（-999.0表示未设置）
    OH_Drawing_TextShadow *shadow;
    TextDecoration textDecoration; // 文本装饰
    std::string fontFamily;   // 字体族名称（空字符串表示未设置）

    SpanStyleRange()
        : start(0), end(0), fontSize(-1.0), fontWeight(-1),
          fontStyle(-1), color(0xFFFFFFFF), letterSpacing(-999.0),
          textDecoration(TextDecoration::None), fontFamily(""), shadow(nullptr) {}

    SpanStyleRange(uint32_t s, uint32_t e)
        : start(s), end(e), fontSize(-1.0), fontWeight(-1),
          fontStyle(-1), color(0xFFFFFFFF), letterSpacing(-999.0),
          textDecoration(TextDecoration::None), fontFamily(""), shadow(nullptr) {}
};

/**
 * Placeholder 范围结构体
 * 用于表示文本中的内联占位符（如图片）
 */
struct PlaceholderRange {
    uint32_t start;                           // 起始位置
    uint32_t end;                             // 结束位置
    float width;                              // 宽度（像素）
    float height;                             // 高度（像素）
    PlaceholderVerticalAlign verticalAlign;   // 垂直对齐方式

    PlaceholderRange()
        : start(0), end(0), width(0.0f), height(0.0f),
          verticalAlign(PlaceholderVerticalAlign::Center) {}

    PlaceholderRange(uint32_t s, uint32_t e, float w, float h, PlaceholderVerticalAlign align)
        : start(s), end(e), width(w), height(h), verticalAlign(align) {}
};

} // namespace OH

#endif // NATIVE_PARAGRAPH_TYPES_H
