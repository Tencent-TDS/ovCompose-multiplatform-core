#include <stdexcept>
#include <algorithm>
#include <native_drawing/drawing_font_collection.h>
#include "oh_native_paragraph.h"
#include "../xcomponent_log.h"

namespace OH {

// ========== 构造函数和析构函数 ==========

Paragraph::Paragraph(const std::string &text, std::unique_ptr<ITextStyleStrategy> textStyleStrategy,
                     std::unique_ptr<IParagraphStyleStrategy> paragraphStyleStrategy,
                     const std::vector<SpanStyleRange> &spanStyles, const std::vector<PlaceholderRange> &placeholders,
                     const std::string &fontFamily) : BaseRenderNode(), text_(text), textStyleStrategy_(std::move(textStyleStrategy)),
                                                      paragraphStyleStrategy_(std::move(paragraphStyleStrategy)), spanStyles_(spanStyles), placeholders_(placeholders),
                                                      fontFamily_(fontFamily), fontCollection_(DrawingResourceFactory::createFontCollection()),
                                                      typography_(nullptr, nullptr), isLayouted_(false) {
    LOGI("[Paragraph] Constructor: text='%{public}s', textLength=%{public}zu, spanStylesCount=%{public}zu, "
         "fontFamily='%{public}s'",
         text_.c_str(), text_.length(), spanStyles_.size(), fontFamily_.c_str());

    // 初始化ArkUI渲染修改器，用于后续的绘制操作
    initModifier();
    // 创建资源
    if (!fontCollection_.isValid()) {
        LOGE("[Paragraph] Failed to create font collection");
        throw std::runtime_error("Failed to create font collection");
    }
    LOGI("[Paragraph] Font collection created successfully");

    // 初始化Typography
    initializeTypography();

    // 创建缓存管理器（使用lambda捕获this）
    lineMetricsCache_ = std::make_unique<LineMetricsCacheManager>([this]() { return buildLineMetricsCache(); });

    metricsCache_ = std::make_unique<MetricsCacheManager>([this]() { return buildParagraphMetrics(); });

    LOGI("[Paragraph] Constructor completed: this=%{public}p", this);
}

Paragraph::~Paragraph() {
    if (posProperty_) {
        OH_ArkUI_RenderNodeUtils_DisposeVector2Property(posProperty_);
    }
    if (modifier_) {
        OH_ArkUI_RenderNodeUtils_DisposeContentModifier(modifier_);
    }
    // RAII自动清理所有资源
    // ResourceHandle会自动调用析构函数
}

// ========== Template Method 实现 ==========

void Paragraph::initializeTypography() {
    createTypographyResources();
}

void Paragraph::createTypographyResources() {
    LOGI("[Paragraph] createTypographyResources: Starting typography creation");

    // 创建段落样式
    auto typoStyle = DrawingResourceFactory::createTypographyStyle();
    if (!typoStyle.isValid()) {
        LOGE("[Paragraph] Failed to create typography style");
        throw std::runtime_error("Failed to create typography style");
    }
    LOGI("[Paragraph] Typography style created successfully");

    // 应用段落样式策略
    if (paragraphStyleStrategy_) {
        paragraphStyleStrategy_->applyTo(typoStyle.get());
        LOGI("[Paragraph] Paragraph style strategy applied");
    }

    // 创建Typography处理器
    auto handler = DrawingResourceFactory::createTypographyHandler(typoStyle.get(), fontCollection_.get());
    if (!handler.isValid()) {
        LOGE("[Paragraph] Failed to create typography handler");
        throw std::runtime_error("Failed to create typography handler");
    }
    LOGI("[Paragraph] Typography handler created successfully");

    // 创建基础文本样式
    auto textStyle = DrawingResourceFactory::createTextStyle();
    if (!textStyle.isValid()) {
        LOGE("[Paragraph] Failed to create text style");
        throw std::runtime_error("Failed to create text style");
    }

    // 应用文本样式策略
    if (textStyleStrategy_) {
        textStyleStrategy_->applyTo(textStyle.get());
        LOGI("[Paragraph] Text style strategy applied");
    }

    // 应用全局字体族（如果设置）
    if (!fontFamily_.empty()) {
        LOGI("[Paragraph] Applying font family: %{public}s", fontFamily_.c_str());
        // TODO: 根据 HarmonyOS API 设置字体族
        // OH_Drawing_SetTextStyleFontFamilies(textStyle.get(), ...);
    }

    // 如果没有 spanStyles，使用简单模式
    if (spanStyles_.empty()) {
        LOGI("[Paragraph] Simple text mode (no span styles)");
        // 设置文本样式并添加文本
        OH_Drawing_TypographyHandlerPushTextStyle(handler.get(), textStyle.get());
        OH_Drawing_TypographyHandlerAddText(handler.get(), text_.c_str());
    } else {
        LOGI("[Paragraph] Rich text mode with %{public}zu span styles", spanStyles_.size());
        // 富文本模式：需要为每个区间应用不同的样式
        applySpanStyles(handler.get(), textStyle.get());
    }

    // 应用占位符（如果有）
    if (!placeholders_.empty()) {
        LOGI("[Paragraph] Applying %{public}zu placeholders", placeholders_.size());
        applyPlaceholders(handler.get());
    }

    // 创建Typography对象
    typography_ = DrawingResourceFactory::createTypography(handler.get());
    if (!typography_.isValid()) {
        LOGE("[Paragraph] Failed to create typography");
        throw std::runtime_error("Failed to create typography");
    }
    LOGI("[Paragraph] Typography created successfully: typography=%{public}p", typography_.get());

    // textStyle, handler, typoStyle在作用域结束时自动释放
}

/**
 * 应用 SpanStyles 到文本的不同区间
 *
 * 算法：
 * 1. 按起始位置排序所有 spanStyles
 * 2. 遍历文本，为每个区间创建对应的 TextStyle
 * 3. 合并重叠的样式范围
 */
void Paragraph::applySpanStyles(OH_Drawing_TypographyCreate *handler, OH_Drawing_TextStyle *baseStyle) {
    if (text_.empty() || spanStyles_.empty()) {
        OH_Drawing_TypographyHandlerPushTextStyle(handler, baseStyle);
        OH_Drawing_TypographyHandlerAddText(handler, text_.c_str());
        return;
    }

    // 复制并排序 spanStyles（按起始位置）
    std::vector<SpanStyleRange> sortedSpans = spanStyles_;
    std::sort(sortedSpans.begin(), sortedSpans.end(),
              [](const SpanStyleRange &a, const SpanStyleRange &b) { return a.start < b.start; });

    uint32_t currentPos = 0;
    const uint32_t textLength = text_.length();

    for (const auto &span : sortedSpans) {
        // 如果当前位置在 span 之前，先添加基础样式的文本
        if (currentPos < span.start) {
            OH_Drawing_TypographyHandlerPushTextStyle(handler, baseStyle);
            std::string segment = text_.substr(currentPos, span.start - currentPos);
            OH_Drawing_TypographyHandlerAddText(handler, segment.c_str());
            currentPos = span.start;
        }

        // 创建新的 TextStyle 应用 span ���式
        auto spanStyle = DrawingResourceFactory::createTextStyle();
        if (!spanStyle.isValid())
            continue;

        // 先复制基础样式的所有属性
        if (textStyleStrategy_) {
            textStyleStrategy_->applyTo(spanStyle.get());
        }

        // 然后覆盖 span 指定的属性
        if (span.fontSize > 0) {
            OH_Drawing_SetTextStyleFontSize(spanStyle.get(), span.fontSize);
        }
        if (span.fontWeight >= 0) {
            OH_Drawing_SetTextStyleFontWeight(spanStyle.get(), span.fontWeight);
        }
        if (span.fontStyle >= 0) {
            OH_Drawing_SetTextStyleFontStyle(spanStyle.get(), span.fontStyle);
        }
        if (span.color != 0xFFFFFFFF) { // 0xFFFFFFFF 表示未设置
            OH_Drawing_SetTextStyleColor(spanStyle.get(), span.color);
        }
        if (span.letterSpacing > -999.0) { // -999.0 表示未设置
            OH_Drawing_SetTextStyleLetterSpacing(spanStyle.get(), span.letterSpacing);
        }

        // TODO: 应用 textDecoration
        // if (span.textDecoration != TextDecoration::None) {
        //     OH_Drawing_SetTextStyleDecoration(spanStyle.get(), ...);
        // }

        // TODO: 应用字体族
        // if (!span.fontFamily.empty()) {
        //     OH_Drawing_SetTextStyleFontFamilies(spanStyle.get(), ...);
        // }

        // 添加带样式的文本段
        OH_Drawing_TypographyHandlerPushTextStyle(handler, spanStyle.get());

        uint32_t spanEnd = std::min(span.end, textLength);
        std::string segment = text_.substr(span.start, spanEnd - span.start);
        OH_Drawing_TypographyHandlerAddText(handler, segment.c_str());

        currentPos = spanEnd;
    }

    // 添加剩余的文本（如果有）
    if (currentPos < textLength) {
        OH_Drawing_TypographyHandlerPushTextStyle(handler, baseStyle);
        std::string segment = text_.substr(currentPos);
        OH_Drawing_TypographyHandlerAddText(handler, segment.c_str());
    }
}

/**
 * 应用占位符（Placeholders）
 *
 * 占位符用于在文本中预留空间，通常用于内联图片、组件等
 */
void Paragraph::applyPlaceholders(OH_Drawing_TypographyCreate *handler) {
    if (placeholders_.empty()) {
        return;
    }

    for (const auto &placeholder : placeholders_) {
        // TODO: 根据 HarmonyOS API 添加占位符
        // 目前 HarmonyOS Drawing API 可能还不支持占位符
        // 需要等待 API 更新或使用其他方案

        // 伪代码示例：
        // OH_Drawing_PlaceholderStyle placeholderStyle;
        // placeholderStyle.width = placeholder.width;
        // placeholderStyle.height = placeholder.height;
        // placeholderStyle.alignment = convertVerticalAlign(placeholder.verticalAlign);
        // OH_Drawing_TypographyHandlerAddPlaceholder(handler, &placeholderStyle);
    }
}

// ========== 布局操作 ==========

void Paragraph::layout(double maxWidth) {
    LOGI("[Paragraph::layout] Starting layout: this=%{public}p, maxWidth=%{public}.2f", this, maxWidth);
    layoutWidth_ = maxWidth;
    performLayout(maxWidth);
    onLayoutComplete();
    LOGI("[Paragraph::layout] Layout completed: width=%{public}.2f, height=%{public}.2f, lineCount=%{public}u",
         getWidth(), getHeight(), getLineCount());
}

void Paragraph::performLayout(double maxWidth) {
    LOGI("[Paragraph::performLayout] Performing layout: maxWidth=%{public}.2f", maxWidth);

    if (!typography_.isValid()) {
        LOGE("[Paragraph::performLayout] Typography is invalid, skipping layout");
        return;
    }

    OH_Drawing_TypographyLayout(typography_.get(), maxWidth);
    isLayouted_ = true;

    LOGI("[Paragraph::performLayout] Layout performed, invalidating caches");

    // 使缓存失效
    if (lineMetricsCache_) {
        lineMetricsCache_->invalidate();
    }
    if (metricsCache_) {
        metricsCache_->invalidate();
    }
}

// ========== 度量查询 ==========

double Paragraph::getHeight() const {
    if (!typography_.isValid())
        return 0.0;
    return OH_Drawing_TypographyGetHeight(typography_.get());
}

double Paragraph::getMinIntrinsicWidth() const {
    if (!typography_.isValid())
        return 0.0;
    return OH_Drawing_TypographyGetMinIntrinsicWidth(typography_.get());
}

double Paragraph::getMaxIntrinsicWidth() const {
    if (!typography_.isValid())
        return 0.0;
    return OH_Drawing_TypographyGetMaxIntrinsicWidth(typography_.get());
}

double Paragraph::getAlphabeticBaseline() const {
    if (!typography_.isValid())
        return 0.0;
    return OH_Drawing_TypographyGetAlphabeticBaseline(typography_.get());
}

double Paragraph::getIdeographicBaseline() const {
    if (!typography_.isValid())
        return 0.0;
    return OH_Drawing_TypographyGetIdeographicBaseline(typography_.get());
}

double Paragraph::getLongestLine() const {
    if (!typography_.isValid())
        return 0.0;
    return OH_Drawing_TypographyGetLongestLine(typography_.get());
}

bool Paragraph::didExceedMaxLines() const {
    if (!typography_.isValid())
        return false;
    return OH_Drawing_TypographyDidExceedMaxLines(typography_.get());
}

uint32_t Paragraph::getLineCount() const {
    if (!typography_.isValid())
        return 0;

    uint32_t count = OH_Drawing_TypographyGetLineCount(typography_.get());
    return (text_.empty() && count < 1) ? 1 : count;
}

const ParagraphMetrics &Paragraph::getMetrics() const {
    if (metricsCache_) {
        return metricsCache_->getMetrics();
    }

    static ParagraphMetrics emptyMetrics;
    return emptyMetrics;
}

// ========== 行信息查询 ==========

const LineMetrics *Paragraph::getLineMetrics(uint32_t lineIndex) const {
    if (lineMetricsCache_) {
        return lineMetricsCache_->getLineMetrics(lineIndex);
    }
    return nullptr;
}

const std::vector<LineMetrics> &Paragraph::getAllLineMetrics() const {
    if (lineMetricsCache_) {
        return lineMetricsCache_->getAllLineMetrics();
    }

    static std::vector<LineMetrics> emptyVector;
    return emptyVector;
}

double Paragraph::getLineLeft(uint32_t lineIndex) const {
    const LineMetrics *metrics = getLineMetrics(lineIndex);
    return metrics ? metrics->left : 0.0;
}

double Paragraph::getLineRight(uint32_t lineIndex) const {
    const LineMetrics *metrics = getLineMetrics(lineIndex);
    return metrics ? metrics->right : layoutWidth_;
}

double Paragraph::getLineTop(uint32_t lineIndex) const {
    const LineMetrics *metrics = getLineMetrics(lineIndex);
    return metrics ? metrics->top : 0.0;
}

double Paragraph::getLineBottom(uint32_t lineIndex) const {
    const LineMetrics *metrics = getLineMetrics(lineIndex);
    return metrics ? metrics->bottom : 0.0;
}

double Paragraph::getLineWidth(uint32_t lineIndex) const {
    const LineMetrics *metrics = getLineMetrics(lineIndex);
    return metrics ? metrics->width : 0.0;
}

double Paragraph::getLineHeight(uint32_t lineIndex) const {
    const LineMetrics *metrics = getLineMetrics(lineIndex);
    return metrics ? metrics->height : 0.0;
}

uint32_t Paragraph::getLineStart(uint32_t lineIndex) const {
    const LineMetrics *metrics = getLineMetrics(lineIndex);
    return metrics ? metrics->startIndex : 0;
}

uint32_t Paragraph::getLineEnd(uint32_t lineIndex, bool visibleEnd) const {
    const LineMetrics *metrics = getLineMetrics(lineIndex);
    if (!metrics)
        return 0;

    return visibleEnd ? metrics->endExcludingWhitespaces : metrics->endIndex;
}

double Paragraph::getLineBaseline(uint32_t lineIndex) const {
    const LineMetrics *metrics = getLineMetrics(lineIndex);
    return metrics ? metrics->baseline : 0.0;
}

// ========== 位置查询 ==========

double Paragraph::getHorizontalPosition(uint32_t offset, bool usePrimaryDirection) const {
    // TODO: 实现具体逻辑
    return 0.0;
}

uint32_t Paragraph::getOffsetForPosition(double dx, double dy) const {
    if (!typography_.isValid())
        return 0;

    OH_Drawing_PositionAndAffinity *posAndAffinity =
        OH_Drawing_TypographyGetGlyphPositionAtCoordinateWithCluster(typography_.get(), dx, dy);

    if (!posAndAffinity)
        return 0;

    return OH_Drawing_GetPositionFromPositionAndAffinity(posAndAffinity);
}

TextRect Paragraph::getCursorRect(uint32_t offset) const {
    double horizontal = getHorizontalPosition(offset, true);
    uint32_t lineIndex = getLineForOffset(offset);
    const LineMetrics *metrics = getLineMetrics(lineIndex);

    if (metrics) {
        return TextRect(horizontal, metrics->top, horizontal, metrics->bottom);
    }

    return TextRect();
}

WordBoundary Paragraph::getWordBoundary(uint32_t offset) const {
    if (!typography_.isValid()) {
        return WordBoundary(offset, offset);
    }

    OH_Drawing_Range *range = OH_Drawing_TypographyGetWordBoundary(typography_.get(), offset);

    if (range) {
        return WordBoundary(OH_Drawing_GetStartFromRange(range), OH_Drawing_GetEndFromRange(range));
    }

    return WordBoundary(offset, offset);
}

std::vector<TextRect> Paragraph::getRectsForRange(uint32_t start, uint32_t end) const {
    std::vector<TextRect> result;

    if (!typography_.isValid()) {
        return result;
    }

    OH_Drawing_TextBox *textBox = OH_Drawing_TypographyGetRectsForRange(typography_.get(), start, end,
                                                                        RECT_HEIGHT_STYLE_MAX, RECT_WIDTH_STYLE_TIGHT);

    if (!textBox) {
        return result;
    }

    uint32_t boxCount = OH_Drawing_GetSizeOfTextBox(textBox);
    result.reserve(boxCount);

    for (uint32_t i = 0; i < boxCount; ++i) {
        result.emplace_back(OH_Drawing_GetLeftFromTextBox(textBox, static_cast<int>(i)),
                            OH_Drawing_GetTopFromTextBox(textBox, static_cast<int>(i)),
                            OH_Drawing_GetRightFromTextBox(textBox, static_cast<int>(i)),
                            OH_Drawing_GetBottomFromTextBox(textBox, static_cast<int>(i)));
    }

    return result;
}

// ========== 绘制 ==========

void Paragraph::paint(float x, float y) {
    LOGI("[Paragraph::paint] Paint called: this=%{public}p, position=(%{public}.2f, %{public}.2f)", this, x, y);
    this->createOrUpdatePositionProperty(x, y);
}

void Paragraph::createOrUpdatePositionProperty(float x, float y) {
    if (!posProperty_) {
        LOGI("[Paragraph::createOrUpdatePositionProperty] Creating new position property: (%{public}.2f, %{public}.2f)",
             x, y);
        posProperty_ = OH_ArkUI_RenderNodeUtils_CreateVector2Property(x, y);
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachVector2Property(modifier_, posProperty_));
    } else {
        LOGI("[Paragraph::createOrUpdatePositionProperty] Updating position property: (%{public}.2f, %{public}.2f)", x,
             y);
        maybeThrow(OH_ArkUI_RenderNodeUtils_SetVector2PropertyValue(posProperty_, x, y));
    }
}

/**
 * @brief Initializes the content modifier for the paragraph rendering node.
 *
 * This method creates and attaches a content modifier to the render node if one doesn't
 * already exist. The modifier sets up a custom draw callback that:
 * - Retrieves the drawing canvas from the draw context
 * - Gets the position (x, y) from the position property
 * - Validates the typography and canvas objects
 * - Paints the typography content on the canvas at the specified position
 *
 * The draw callback is invoked during the rendering phase to paint the paragraph
 * text content using the native drawing API.
 *
 * @note This method is idempotent - calling it multiple times will only initialize once.
 * @throws May throw an exception if node modifier creation or attachment fails.
 */
void Paragraph::initModifier() {
    if (!modifier_) {
        modifier_ = OH_ArkUI_RenderNodeUtils_CreateContentModifier();
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachContentModifier(nodeHandle_, modifier_));
        maybeThrow(OH_ArkUI_RenderNodeUtils_SetContentModifierOnDraw(
            modifier_, this, [](ArkUI_DrawContext *context, void *userData) {
                LOGI("Paragraph::onDraw called");
                auto *data = static_cast<Paragraph *>(userData);
                auto *canvas1 = OH_ArkUI_DrawContext_GetCanvas(context);
                OH_Drawing_Canvas *canvas = reinterpret_cast<OH_Drawing_Canvas *>(canvas1);

                float x = 0;
                float y = 0;
                if (!data->posProperty_) {
                    return;
                }
                OH_ArkUI_RenderNodeUtils_GetVector2PropertyValue(data->posProperty_, &x, &y);
                if (!data->typography_.isValid() || !canvas) {
                    return;
                }

                OH_Drawing_TypographyPaint(data->typography_.get(), canvas, x, y);
                LOGI("Paragraph::onDraw completed");
            }));
    }
}

// ========== 辅助查询 ==========

uint32_t Paragraph::getLineForOffset(uint32_t offset) const {
    const auto &allMetrics = getAllLineMetrics();

    for (uint32_t i = 0; i < allMetrics.size(); ++i) {
        const LineMetrics &metrics = allMetrics[i];
        if (offset >= metrics.startIndex && offset < metrics.endIndex) {
            return i;
        }
    }

    return allMetrics.empty() ? 0 : allMetrics.size() - 1;
}

uint32_t Paragraph::getLineForVerticalPosition(double vertical) const {
    const auto &allMetrics = getAllLineMetrics();

    if (allMetrics.empty())
        return 0;

    // 二分查找
    uint32_t low = 0;
    uint32_t high = allMetrics.size();

    while (low < high) {
        uint32_t mid = (low + high) / 2;
        const LineMetrics &metrics = allMetrics[mid];

        if (vertical < metrics.bottom) {
            if (mid == 0 || vertical >= allMetrics[mid - 1].bottom) {
                return mid;
            }
            high = mid;
        } else {
            low = mid + 1;
        }
    }

    return allMetrics.size() - 1;
}

// ========== 缓存构建器 ==========

std::vector<LineMetrics> Paragraph::buildLineMetricsCache() {
    std::vector<LineMetrics> cache;

    if (!typography_.isValid()) {
        return cache;
    }

    uint32_t lineCount = getLineCount();
    cache.reserve(lineCount);

    for (uint32_t i = 0; i < lineCount; ++i) {
        OH_Drawing_LineMetrics *ohLineMetrics = OH_Drawing_TypographyGetLineMetrics(typography_.get());
        LineMetrics metrics;
        metrics.startIndex = ohLineMetrics[i].startIndex;
        metrics.endIndex = ohLineMetrics[i].endIndex;
        metrics.left = ohLineMetrics[i].x;
        metrics.top = ohLineMetrics[i].y;
        metrics.right = ohLineMetrics[i].x + ohLineMetrics[i].width;
        metrics.bottom = ohLineMetrics[i].y + ohLineMetrics[i].height;
        metrics.width = ohLineMetrics[i].width;
        metrics.height = ohLineMetrics[i].height;
        metrics.baseline = ohLineMetrics[i].y + ohLineMetrics[i].ascender;
        metrics.ascent = ohLineMetrics[i].ascender;
        metrics.descent = ohLineMetrics[i].descender;

        cache.push_back(metrics);
    }

    return cache;
}

ParagraphMetrics Paragraph::buildParagraphMetrics() {
    LOGI("[Paragraph::buildParagraphMetrics] Building paragraph metrics");

    ParagraphMetrics metrics;

    metrics.width = getWidth();
    metrics.height = getHeight();
    metrics.minIntrinsicWidth = getMinIntrinsicWidth();
    metrics.maxIntrinsicWidth = getMaxIntrinsicWidth();
    metrics.alphabeticBaseline = getAlphabeticBaseline();
    metrics.ideographicBaseline = getIdeographicBaseline();
    metrics.longestLine = getLongestLine();
    metrics.lineCount = getLineCount();
    metrics.didExceedMaxLines = didExceedMaxLines();

    LOGI("[Paragraph::buildParagraphMetrics] Metrics built: width=%{public}.2f, height=%{public}.2f, "
         "minIntrinsicWidth=%{public}.2f, maxIntrinsicWidth=%{public}.2f, lineCount=%{public}u, "
         "alphabeticBaseline=%{public}.2f, ideographicBaseline=%{public}.2f, longestLine=%{public}.2f, "
         "didExceedMaxLines=%{public}d",
         metrics.width, metrics.height, metrics.minIntrinsicWidth, metrics.maxIntrinsicWidth, metrics.lineCount,
         metrics.alphabeticBaseline, metrics.ideographicBaseline, metrics.longestLine, metrics.didExceedMaxLines);

    return metrics;
}

OH_DrawingNode_Type Paragraph::getType() {
    return OH_DrawingNode_Type::ParagraphNode;
};
} // namespace OH
