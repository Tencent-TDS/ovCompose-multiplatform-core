#include "oh_native_paragraph.h"

#include <native_drawing/drawing_font_collection.h>

#include <algorithm>
#include <stdexcept>
#include <utility>

#include "../constants/oh_native_constants.h"
#include "../xcomponent_log.h"

namespace OH {

// ========== Cut-Op 机制的内部数据结构 ==========

/**
 * Cut（切分点）：表示样式的添加或移除点
 */
struct Paragraph::StyleCut {
    enum class Type { Add, Remove };

    uint32_t position;
    Type type;
    const SpanStyleRange *style;

    // 排序：先按位置，同位置时Remove在Add之前
    bool operator<(const StyleCut &other) const {
        if (position != other.position)
            return position < other.position;
        return type > other.type; // Remove > Add
    }
};

/**
 * Op（操作）：表示在某个位置应用的合并样式
 */
struct Paragraph::StyleOp {
    uint32_t position;
    ResourceHandle<OH_Drawing_TextStyle> mergedStyle;
};

// ========== 构造函数和析构函数 ==========

Paragraph::Paragraph(std::string text, std::unique_ptr<ITextStyleStrategy> textStyleStrategy,
                     std::unique_ptr<IParagraphStyleStrategy> paragraphStyleStrategy,
                     const std::vector<SpanStyleRange> &spanStyles, const std::vector<PlaceholderRange> &placeholders,
                     std::string fontFamily)
    : text_(std::move(text)), textStyleStrategy_(std::move(textStyleStrategy)),
      paragraphStyleStrategy_(std::move(paragraphStyleStrategy)), isLayouted_(false),
      fontCollection_(DrawingResourceFactory::createFontCollection()), typography_(nullptr, nullptr),
      spanStyles_(spanStyles), placeholders_(placeholders), fontFamily_(std::move(fontFamily)) {
    LOGI("[Paragraph] Constructor: text='%{public}s', textLength=%{public}zu, "
         "spanStylesCount=%{public}zu, "
         "fontFamily='%{public}s'",
         text_.c_str(), text_.length(), spanStyles_.size(), fontFamily_.c_str());

    // 初始化ArkUI渲染修改器，用于后续的绘制操作
    Paragraph::initModifier();
    // 创建资源
    if (!fontCollection_.isValid()) {
        LOGE("[Paragraph] Failed to create font collection");
        throw std::runtime_error("Failed to create font collection");
    }
    LOGI("[Paragraph] Font collection created successfully");

    // 初始化Typography
    Paragraph::initializeTypography();

    // 创建缓存管理器（使用lambda捕获this）
    lineMetricsCache_ = std::make_unique<LineMetricsCacheManager>([this]() { return buildLineMetricsCache(); });

    metricsCache_ = std::make_unique<MetricsCacheManager>([this]() { return buildParagraphMetrics(); });

    LOGI("[Paragraph] Constructor completed: this=%{public}p", this);
}

Paragraph::~Paragraph() {
    LOGI("Dispose paragraph related resources.");
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

void Paragraph::initializeTypography() { createTypographyResources(); }

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
    const auto handler = DrawingResourceFactory::createTypographyHandler(typoStyle.get(), fontCollection_.get());
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
    if (spanStyles_.empty() && placeholders_.empty()) {
        LOGI("[Paragraph] Simple text mode (no span styles, no placeholders)");
        // 设置文本样式并添加文本
        OH_Drawing_TypographyHandlerPushTextStyle(handler.get(), textStyle.get());
        OH_Drawing_TypographyHandlerAddText(handler.get(), text_.c_str());
    } else if (spanStyles_.empty() && !placeholders_.empty()) {
        LOGI("[Paragraph] Simple text mode with %{public}zu placeholders", placeholders_.size());
        // 只有占位符，没有样式变化
        OH_Drawing_TypographyHandlerPushTextStyle(handler.get(), textStyle.get());

        uint32_t currentPos = 0;
        for (const auto &placeholder : placeholders_) {
            // 添加占位符前的文本
            if (placeholder.start > currentPos) {
                std::string segment = text_.substr(currentPos, placeholder.start - currentPos);
                OH_Drawing_TypographyHandlerAddText(handler.get(), segment.c_str());
            }

            // 添加占位符
            OH_Drawing_PlaceholderSpan placeholderSpan;
            placeholderSpan.width = static_cast<double>(placeholder.width);
            placeholderSpan.height = static_cast<double>(placeholder.height);
            placeholderSpan.alignment = convertPlaceholderAlignment(placeholder.verticalAlign);
            placeholderSpan.baseline = TEXT_BASELINE_ALPHABETIC;
            placeholderSpan.baselineOffset = 0.0;
            OH_Drawing_TypographyHandlerAddPlaceholder(handler.get(), &placeholderSpan);

            currentPos = placeholder.end;
        }

        // 添加最后剩余的文本
        if (currentPos < text_.length()) {
            const std::string segment = text_.substr(currentPos);
            OH_Drawing_TypographyHandlerAddText(handler.get(), segment.c_str());
        }
    } else {
        LOGI("[Paragraph] Rich text mode with %{public}zu span styles", spanStyles_.size());
        // 富文本模式：需要为每个区间应用不同的样式
        applySpanStyles(handler.get(), textStyle.get());
    }

    // 注意：占位符现在已集成到文本构建过程中，不需要单独调用 applyPlaceholders

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
 * 参考 SkiaParagraph 的 Cut-Op 机制实现：
 * 1. Cut（切分点）：将所有样式范围的起始和结束位置记录为切分点
 * 2. Op（操作）：在每个位置合并所有活跃的样式，生成最终的样式操作
 * 3. 优势：正确处理样式重叠、优先级和合并
 */
void Paragraph::applySpanStyles(OH_Drawing_TypographyCreate *handler, OH_Drawing_TextStyle *baseStyle) const {
    if (text_.empty()) {
        OH_Drawing_TypographyHandlerPushTextStyle(handler, baseStyle);
        OH_Drawing_TypographyHandlerAddText(handler, text_.c_str());
        return;
    }

    if (spanStyles_.empty()) {
        OH_Drawing_TypographyHandlerPushTextStyle(handler, baseStyle);
        OH_Drawing_TypographyHandlerAddText(handler, text_.c_str());
        return;
    }

    // ========== 执行三步处理流程 ==========

    // Step 1: 生成切分点列表
    const auto cuts = generateStyleCuts();

    // Step 2: 将 Cut 转换为 Op（合并样式）
    const auto ops = convertCutsToOps(cuts);

    // Step 3: 根据 Op 构建段落
    buildParagraphFromOps(handler, ops);
}

/**
 * Step 1: 生成样式切分点列表
 */
std::vector<Paragraph::StyleCut> Paragraph::generateStyleCuts() const {
    std::vector<StyleCut> cuts;
    cuts.reserve(spanStyles_.size() * 2);

    for (const auto &span : spanStyles_) {
        cuts.push_back({span.start, StyleCut::Type::Add, &span});
        cuts.push_back({span.end, StyleCut::Type::Remove, &span});
    }

    std::sort(cuts.begin(), cuts.end());

    LOGI("[Paragraph] Generated %{public}zu cuts from %{public}zu spans", cuts.size(), spanStyles_.size());

    return cuts;
}

/**
 * Step 2: 将 Cut 转换为 Op（合并样式）
 */
std::vector<Paragraph::StyleOp> Paragraph::convertCutsToOps(const std::vector<StyleCut> &cuts) const {
    std::vector<StyleOp> ops;
    std::vector<const SpanStyleRange *> activeStyles; // 活跃样式栈

    // 在位置 0 添加基础样式
    auto initialStyle = DrawingResourceFactory::createTextStyle();
    if (textStyleStrategy_) {
        textStyleStrategy_->applyTo(initialStyle.get());
    }
    ops.push_back({0, std::move(initialStyle)});

    // 处理所有切分点
    for (const auto &cut : cuts) {
        if (cut.type == StyleCut::Type::Add) {
            handleStyleAdd(cut, activeStyles, ops);
        } else { // Remove
            handleStyleRemove(cut, activeStyles, ops);
        }
    }

    LOGI("[Paragraph] Generated %{public}zu ops from %{public}zu cuts", ops.size(), cuts.size());

    return ops;
}

/**
 * 处理样式添加（Cut::Add）
 */
void Paragraph::handleStyleAdd(const StyleCut &cut, std::vector<const SpanStyleRange *> &activeStyles,
                               std::vector<StyleOp> &ops) const {
    activeStyles.push_back(cut.style);

    // 同一位置的多个 Add 合并到一个 Op
    if (!ops.empty() && ops.back().position == cut.position) {
        // 直接重新创建合并样式
        ops.back().mergedStyle = createMergedStyleFromStack(activeStyles);
    } else {
        ops.push_back({cut.position, createMergedStyleFromStack(activeStyles)});
    }
}

/**
 * 处理样式移除（Cut::Remove）
 */
void Paragraph::handleStyleRemove(const StyleCut &cut, std::vector<const SpanStyleRange *> &activeStyles,
                                  std::vector<StyleOp> &ops) const {
    auto it = std::find(activeStyles.begin(), activeStyles.end(), cut.style);
    if (it != activeStyles.end()) {
        activeStyles.erase(it);
    }
    ops.push_back({cut.position, createMergedStyleFromStack(activeStyles)});
}

/**
 * Step 3: 根据 Op 构建段落
 * 同时处理占位符插入
 */
void Paragraph::buildParagraphFromOps(OH_Drawing_TypographyCreate *handler, const std::vector<StyleOp> &ops) const {
    const uint32_t textLength = text_.length();
    size_t placeholderIndex = 0; // 当前处理的占位符索引

    for (size_t i = 0; i < ops.size(); ++i) {
        const auto &op = ops[i];

        uint32_t startPos = op.position;
        uint32_t endPos = (i + 1 < ops.size()) ? ops[i + 1].position : textLength;

        if (startPos >= endPos || startPos >= textLength) {
            continue;
        }

        // 检查当前段落中是否包含占位符
        while (placeholderIndex < placeholders_.size() && placeholders_[placeholderIndex].start < endPos) {
            const auto &placeholder = placeholders_[placeholderIndex];

            // 添加占位符前的文本
            if (placeholder.start > startPos) {
                OH_Drawing_TypographyHandlerPushTextStyle(handler, op.mergedStyle.get());
                std::string segment = text_.substr(startPos, placeholder.start - startPos);
                OH_Drawing_TypographyHandlerAddText(handler, segment.c_str());
            }

            // 添加占位符
            OH_Drawing_PlaceholderSpan placeholderSpan;
            placeholderSpan.width = static_cast<double>(placeholder.width);
            placeholderSpan.height = static_cast<double>(placeholder.height);
            placeholderSpan.alignment = convertPlaceholderAlignment(placeholder.verticalAlign);
            placeholderSpan.baseline = TEXT_BASELINE_ALPHABETIC;
            placeholderSpan.baselineOffset = 0.0;

            LOGI("[Paragraph] Inserting placeholder at position %{public}u: "
                 "width=%{public}f, height=%{public}f",
                 placeholder.start, placeholderSpan.width, placeholderSpan.height);

            OH_Drawing_TypographyHandlerAddPlaceholder(handler, &placeholderSpan);

            // 更新起始位置，跳过占位符覆盖的文本
            startPos = placeholder.end;
            placeholderIndex++;
        }

        // 添加剩余的文本段
        if (startPos < endPos) {
            OH_Drawing_TypographyHandlerPushTextStyle(handler, op.mergedStyle.get());
            std::string segment = text_.substr(startPos, endPos - startPos);
            OH_Drawing_TypographyHandlerAddText(handler, segment.c_str());
        }

        LOGI("[Paragraph] Applied style at pos %{public}u-%{public}u", startPos, endPos);
    }
}

/**
 * 创建合并样式（从活跃样式栈）
 */
ResourceHandle<OH_Drawing_TextStyle>
Paragraph::createMergedStyleFromStack(const std::vector<const SpanStyleRange *> &activeStyles) const {
    auto mergedStyle = DrawingResourceFactory::createTextStyle();

    // 先应用基础样式
    if (textStyleStrategy_) {
        textStyleStrategy_->applyTo(mergedStyle.get());
    }

    // 按顺序叠加所有活跃样式（后面的覆盖前面的）
    for (const auto *style : activeStyles) {
        applySpanStyleToTextStyle(mergedStyle.get(), *style);
    }

    return mergedStyle;
}

/**
 * 应用单个 SpanStyleRange 到 TextStyle
 */
void Paragraph::applySpanStyleToTextStyle(OH_Drawing_TextStyle *textStyle, const SpanStyleRange &span) {
    if (span.fontSize > 0) {
        OH_Drawing_SetTextStyleFontSize(textStyle, span.fontSize);
    }
    if (span.fontWeight > 0) {
        OH_Drawing_SetTextStyleFontWeight(textStyle, span.fontWeight);
    }
    if (span.fontStyle >= 0) {
        OH_Drawing_SetTextStyleFontStyle(textStyle, span.fontStyle);
    }
    if (span.color != 0xFFFFFFFF && span.color != CLEAR_COLOR) {
        OH_Drawing_SetTextStyleColor(textStyle, span.color);
    }
    if (span.letterSpacing > -999.0) {
        OH_Drawing_SetTextStyleLetterSpacing(textStyle, span.letterSpacing);
    }
    if (span.textDecoration != TextDecoration::None) {
        OH_Drawing_SetTextStyleDecoration(textStyle, static_cast<int>(span.textDecoration));
    }
    if (span.shadow) {
        OH_Drawing_TextStyleAddShadow(textStyle, span.shadow);
    }

    // TODO: 应用背景色、字体族等其他属性
}

// ========== 布局操作 ==========

void Paragraph::layout(const double maxWidth) {
    LOGI("[Paragraph::layout] Starting layout: this=%{public}p, "
         "maxWidth=%{public}.2f",
         this, maxWidth);
    layoutWidth_ = maxWidth;
    performLayout(maxWidth);
    onLayoutComplete();
    LOGI("[Paragraph::layout] Layout completed: width=%{public}.2f, "
         "height=%{public}.2f, lineCount=%{public}u",
         getWidth(), getHeight(), getLineCount());
}

void Paragraph::performLayout(const double maxWidth) {
    LOGI("[Paragraph::performLayout] Performing layout: maxWidth=%{public}.2f", maxWidth);

    if (!typography_.isValid()) {
        LOGE("[Paragraph::performLayout] Typography is invalid, skipping layout");
        return;
    }

    OH_Drawing_TypographyLayout(typography_.get(), maxWidth);
    isLayouted_ = true;

    LOGI("[Paragraph::performLayout] Layout performed, maxWidth=%{public}.2f, text_='%{public}s', invalidating caches",
         maxWidth, text_.c_str());

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
    const double height = OH_Drawing_TypographyGetHeight(typography_.get());
    LOGI("[Paragraph::getHeight] this=%{public}p, layoutWidth_=%{public}.2f, "
         "height=%{public}.2f, text='%{public}s'",
         this, layoutWidth_, height, text_.c_str());
    return height;
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

    const uint32_t count = OH_Drawing_TypographyGetLineCount(typography_.get());
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

double Paragraph::getHorizontalPosition(uint32_t offset, bool usePrimaryDirection) {
    // TODO: 实现具体逻辑
    return 0.0;
}

uint32_t Paragraph::getOffsetForPosition(const double dx, const double dy) const {
    if (!typography_.isValid())
        return 0;

    OH_Drawing_PositionAndAffinity *posAndAffinity =
        OH_Drawing_TypographyGetGlyphPositionAtCoordinateWithCluster(typography_.get(), dx, dy);

    if (!posAndAffinity)
        return 0;

    return OH_Drawing_GetPositionFromPositionAndAffinity(posAndAffinity);
}

TextRect Paragraph::getCursorRect(uint32_t offset) const {
    const double horizontal = getHorizontalPosition(offset, true);
    const uint32_t lineIndex = getLineForOffset(offset);

    if (const LineMetrics *metrics = getLineMetrics(lineIndex)) {
        return {horizontal, metrics->top, horizontal, metrics->bottom};
    }

    return {};
}

WordBoundary Paragraph::getWordBoundary(uint32_t offset) const {
    if (!typography_.isValid()) {
        return {offset, offset};
    }

    if (OH_Drawing_Range *range = OH_Drawing_TypographyGetWordBoundary(typography_.get(), offset)) {
        return {static_cast<uint32_t>(OH_Drawing_GetStartFromRange(range)),
                static_cast<uint32_t>(OH_Drawing_GetEndFromRange(range))};
    }

    return {offset, offset};
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

// ========== 占位符查询 ==========

std::vector<TextRect> Paragraph::getPlaceholderRects() const {
    std::vector<TextRect> result;

    if (!typography_.isValid()) {
        LOGE("[Paragraph] getPlaceholderRects: Typography is not valid");
        return result;
    }

    if (placeholders_.empty()) {
        LOGI("[Paragraph] getPlaceholderRects: No placeholders defined");
        return result;
    }

    // 调用 HarmonyOS API 获取占位符的矩形区域
    // 参考 SkiaParagraph.skiko.kt: paragraph.rectsForPlaceholders.map {
    // it.rect.toComposeRect() }
    OH_Drawing_TextBox *textBox = OH_Drawing_TypographyGetRectsForPlaceholders(typography_.get());

    if (!textBox) {
        LOGE("[Paragraph] getPlaceholderRects: Failed to get placeholder rects from "
             "typography");
        // 返回空列表，与 placeholders_ 数量相同（所有为空）
        result.resize(placeholders_.size());
        return result;
    }

    uint32_t boxCount = OH_Drawing_GetSizeOfTextBox(textBox);
    LOGI("[Paragraph] getPlaceholderRects: Retrieved %{public}u placeholder rects "
         "for %{public}zu placeholders",
         boxCount, placeholders_.size());

    // 为每个占位符创建对应的矩形
    result.reserve(boxCount);
    for (uint32_t i = 0; i < boxCount; ++i) {
        result.emplace_back(OH_Drawing_GetLeftFromTextBox(textBox, static_cast<int>(i)),
                            OH_Drawing_GetTopFromTextBox(textBox, static_cast<int>(i)),
                            OH_Drawing_GetRightFromTextBox(textBox, static_cast<int>(i)),
                            OH_Drawing_GetBottomFromTextBox(textBox, static_cast<int>(i)));

        LOGI("[Paragraph] Placeholder[%{public}u] rect: (%{public}.2f, "
             "%{public}.2f, %{public}.2f, %{public}.2f)",
             i, result.back().left, result.back().top, result.back().right, result.back().bottom);
    }

    return result;
}

// ========== 绘制 ==========

void Paragraph::paint(float x, float y) {
    LOGI("[Paragraph::paint] Paint called: this=%{public}p, "
         "position=(%{public}.2f, %{public}.2f)",
         this, x, y);
    this->createOrUpdatePositionProperty(x, y);
}

void Paragraph::createOrUpdatePositionProperty(float x, float y) {
    if (!posProperty_) {
        LOGI("[Paragraph::createOrUpdatePositionProperty] Creating new position "
             "property: (%{public}.2f, %{public}.2f)",
             x, y);
        posProperty_ = OH_ArkUI_RenderNodeUtils_CreateVector2Property(x, y);
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachVector2Property(modifier_, posProperty_));
    } else {
        LOGI("[Paragraph::createOrUpdatePositionProperty] Updating position "
             "property: (%{public}.2f, %{public}.2f)",
             x, y);
        maybeThrow(OH_ArkUI_RenderNodeUtils_SetVector2PropertyValue(posProperty_, x, y));
    }
}

/**
 * @brief Initializes the content modifier for the paragraph rendering node.
 *
 * This method creates and attaches a content modifier to the render node if one
 * doesn't already exist. The modifier sets up a custom draw callback that:
 * - Retrieves the drawing canvas from the draw context
 * - Gets the position (x, y) from the position property
 * - Validates the typography and canvas objects
 * - Paints the typography content on the canvas at the specified position
 *
 * The draw callback is invoked during the rendering phase to paint the
 * paragraph text content using the native drawing API.
 *
 * @note This method is idempotent - calling it multiple times will only
 * initialize once.
 * @throws RuntimeError May throw an exception if node modifier creation or
 * attachment fails.
 */
void Paragraph::initModifier() {
    if (!modifier_) {
        modifier_ = OH_ArkUI_RenderNodeUtils_CreateContentModifier();
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachContentModifier(nodeHandle_, modifier_));
        maybeThrow(OH_ArkUI_RenderNodeUtils_SetContentModifierOnDraw(
            modifier_, this, [](ArkUI_DrawContext *context, void *userData) {
                LOGI("Paragraph::onDraw called");
                const auto *data = static_cast<Paragraph *>(userData);
                auto *canvas1 = OH_ArkUI_DrawContext_GetCanvas(context);
                auto *canvas = static_cast<OH_Drawing_Canvas *>(canvas1);

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

uint32_t Paragraph::getLineForOffset(const uint32_t offset) const {
    const auto &allMetrics = getAllLineMetrics();

    for (uint32_t i = 0; i < allMetrics.size(); ++i) {
        const LineMetrics &metrics = allMetrics[i];
        if (offset >= metrics.startIndex && offset < metrics.endIndex) {
            return i;
        }
    }

    return allMetrics.empty() ? 0 : allMetrics.size() - 1;
}

uint32_t Paragraph::getLineForVerticalPosition(const double vertical) const {
    const auto &allMetrics = getAllLineMetrics();

    if (allMetrics.empty())
        return 0;

    // 二分查找
    uint32_t low = 0;
    uint32_t high = allMetrics.size();

    while (low < high) {
        const uint32_t mid = (low + high) / 2;

        if (const LineMetrics &metrics = allMetrics[mid]; vertical < metrics.bottom) {
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
        const OH_Drawing_LineMetrics *ohLineMetrics = OH_Drawing_TypographyGetLineMetrics(typography_.get());
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

ParagraphMetrics Paragraph::buildParagraphMetrics() const {
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

    LOGI("[Paragraph::buildParagraphMetrics] Metrics built: width=%{public}.2f, "
         "height=%{public}.2f, "
         "minIntrinsicWidth=%{public}.2f, maxIntrinsicWidth=%{public}.2f, "
         "lineCount=%{public}u, "
         "alphabeticBaseline=%{public}.2f, ideographicBaseline=%{public}.2f, "
         "longestLine=%{public}.2f, "
         "didExceedMaxLines=%{public}d",
         metrics.width, metrics.height, metrics.minIntrinsicWidth, metrics.maxIntrinsicWidth, metrics.lineCount,
         metrics.alphabeticBaseline, metrics.ideographicBaseline, metrics.longestLine, metrics.didExceedMaxLines);

    return metrics;
}

OH_DrawingNode_Type Paragraph::getType() { return OH_DrawingNode_Type::ParagraphNode; };
} // namespace OH
