#ifndef NATIVE_PARAGRAPH_BRIDGE_H
#define NATIVE_PARAGRAPH_BRIDGE_H

#include <stdint.h>
#include "../constants/oh_native_constants.h"

#ifdef __cplusplus
extern "C" {
#endif

// ========== 创建与销毁 ==========

/**
 * 使用Builder模式创建段落对象
 *
 * @return Builder句柄，失败返回NULL
 */
ParagraphHandle_Handle ParagraphBuilder_create();

/**
 * 设置文本内容
 */
void ParagraphBuilder_setText(ParagraphHandle_Handle builder, const char *text, uint32_t length);

/**
 * 设置字体大小
 */
void ParagraphBuilder_setFontSize(ParagraphHandle_Handle builder, double fontSize);

/**
 * 设置字体粗细
 */
void ParagraphBuilder_setFontWeight(ParagraphHandle_Handle builder, uint32_t fontWeight);

/**
 * 设置字体样式
 */
void ParagraphBuilder_setFontStyle(ParagraphHandle_Handle builder, int fontStyle);

/**
 * 设置文本颜色
 */
void ParagraphBuilder_setColor(ParagraphHandle_Handle builder, uint32_t color);

/**
 * 设置字母间距
 */
void ParagraphBuilder_setLetterSpacing(ParagraphHandle_Handle builder, double spacing);

/**
 * 设置单词间距
 */
void ParagraphBuilder_setWordSpacing(ParagraphHandle_Handle builder, double spacing);

/**
 * 设置行高
 */
void ParagraphBuilder_setLineHeight(ParagraphHandle_Handle builder, double lineHeight);

/**
 * 设置文本对齐方式
 */
void ParagraphBuilder_setTextAlign(ParagraphHandle_Handle builder, int textAlign);

/**
 * 设置文本方向
 */
void ParagraphBuilder_setTextDirection(ParagraphHandle_Handle builder, int textDirection);

/**
 * 设置最大行数
 */
void ParagraphBuilder_setMaxLines(ParagraphHandle_Handle builder, uint32_t maxLines);

/**
 * 设置是否使用省略号
 */
void ParagraphBuilder_setNeedEllipsis(ParagraphHandle_Handle builder, bool needEllipsis);

/**
 *
 */
void ParagraphBuilder_setEllipsis(ParagraphHandle_Handle builder, const char *ellipsis, uint32_t length);

void ParagraphBuilder_addSpanStyle(ParagraphHandle_Handle builder, SpanStyleRange_Handle spanStyleHandle);

/**
 * 构建段落对象
 *
 * @return 段落句柄，失败返回NULL
 * @note builder在调用后会被销毁
 */
ParagraphHandle_Handle ParagraphBuilder_build(ParagraphHandle_Handle builder);

/**
 * 销毁段落对象
 */
void Paragraph_destroy(ParagraphHandle_Handle handle);

// ========== 布局操作 ==========

void Paragraph_layout(ParagraphHandle_Handle handle, double maxWidth);

// ========== 度量属性 ==========

double Paragraph_getWidth(ParagraphHandle_Handle handle);
double Paragraph_getHeight(ParagraphHandle_Handle handle);
double Paragraph_getMinIntrinsicWidth(ParagraphHandle_Handle handle);
double Paragraph_getMaxIntrinsicWidth(ParagraphHandle_Handle handle);
double Paragraph_getAlphabeticBaseline(ParagraphHandle_Handle handle);
double Paragraph_getIdeographicBaseline(ParagraphHandle_Handle handle);
double Paragraph_getLongestLine(ParagraphHandle_Handle handle);
bool Paragraph_didExceedMaxLines(ParagraphHandle_Handle handle);
uint32_t Paragraph_getLineCount(ParagraphHandle_Handle handle);

// ========== 行信息查询 ==========

double Paragraph_getLineLeft(ParagraphHandle_Handle handle, uint32_t lineIndex);
double Paragraph_getLineRight(ParagraphHandle_Handle handle, uint32_t lineIndex);
double Paragraph_getLineTop(ParagraphHandle_Handle handle, uint32_t lineIndex);
double Paragraph_getLineBottom(ParagraphHandle_Handle handle, uint32_t lineIndex);
double Paragraph_getLineWidth(ParagraphHandle_Handle handle, uint32_t lineIndex);
double Paragraph_getLineHeight(ParagraphHandle_Handle handle, uint32_t lineIndex);
uint32_t Paragraph_getLineStart(ParagraphHandle_Handle handle, uint32_t lineIndex);
uint32_t Paragraph_getLineEnd(ParagraphHandle_Handle handle, uint32_t lineIndex, bool visibleEnd);
double Paragraph_getLineBaseline(ParagraphHandle_Handle handle, uint32_t lineIndex);

// ========== 位置查询 ==========

double Paragraph_getHorizontalPosition(ParagraphHandle_Handle handle, uint32_t offset, bool usePrimaryDirection);
uint32_t Paragraph_getOffsetForPosition(ParagraphHandle_Handle handle, double dx, double dy);

void Paragraph_getCursorRect(ParagraphHandle_Handle handle, uint32_t offset, double *outLeft, double *outTop,
                             double *outRight, double *outBottom);

void Paragraph_getWordBoundary(ParagraphHandle_Handle handle, uint32_t offset, uint32_t *outStart, uint32_t *outEnd);

uint32_t Paragraph_getRectsForRangeCount(ParagraphHandle_Handle handle, uint32_t start, uint32_t end);

uint32_t Paragraph_getRectsForRange(ParagraphHandle_Handle handle, uint32_t start, uint32_t end, double *rects,
                                    uint32_t capacity);

// ========== 绘制 ==========

BaseRenderNode_Handle Paragraph_getBaseRenderNode(ParagraphHandle_Handle handle);

void Paragraph_paint(ParagraphHandle_Handle handle, double x, double y);

// ========== 辅助查询 ==========

uint32_t Paragraph_getLineForOffset(ParagraphHandle_Handle handle, uint32_t offset);
uint32_t Paragraph_getLineForVerticalPosition(ParagraphHandle_Handle handle, double vertical);


// ========== 富文本属性相关 ========
SpanStyleRange_Handle Paragraph_CreateSpanStyleRange(int start, int end);
void Paragraph_DestroySpanStyleRange(SpanStyleRange_Handle handle);

// 属性设置
void Paragraph_SpanStyleRange_setFontSize(SpanStyleRange_Handle handle, double fontSize);
void Paragraph_SpanStyleRange_setFontWeight(SpanStyleRange_Handle handle, int weight /*100-900*/);
void Paragraph_SpanStyleRange_setFontStyle(SpanStyleRange_Handle handle, int isItalic /*0|1*/);
void Paragraph_SpanStyleRange_setColor(SpanStyleRange_Handle handle, uint32_t argb);
void Paragraph_SpanStyleRange_setBackground(SpanStyleRange_Handle handle, uint32_t argb);
void Paragraph_SpanStyleRange_setLetterSpacing(SpanStyleRange_Handle handle, double px);
void Paragraph_SpanStyleRange_setBaselineShift(SpanStyleRange_Handle handle, float px);
void Paragraph_SpanStyleRange_setDecoration(SpanStyleRange_Handle handle, int decoration);
void Paragraph_SpanStyleRange_setShadow(SpanStyleRange_Handle handle, float ox, float oy, double blur, uint32_t argb);
void Paragraph_SpanStyleRange_setFontFamily(SpanStyleRange_Handle handle, const char* family);
void Paragraph_SpanStyleRange_setFontFeatureSettings(SpanStyleRange_Handle handle, const char* featureSettings /*UTF-8*/);


#ifdef __cplusplus
}
#endif

#endif // NATIVE_PARAGRAPH_BRIDGE_H
