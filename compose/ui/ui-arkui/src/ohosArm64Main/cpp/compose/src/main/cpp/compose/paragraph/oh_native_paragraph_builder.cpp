#include <stdexcept>
#include "oh_native_paragraph_builder.h"
#include "oh_native_paragraph.h"
#include "../xcomponent_log.h"

namespace OH {

// ========== 构造函数 ==========

ParagraphBuilder::ParagraphBuilder() {
    setDefaults();
}

void ParagraphBuilder::setDefaults() {
    LOGI("ParagraphBuilder: Setting default values");
    text_ = "";
    fontSize_ = 14.0;
    fontWeight_ = FontWeight::Normal;
    fontStyle_ = FontStyle::Normal;
    color_ = 0xFF000000; // 黑色
    letterSpacing_ = 0.0;
    wordSpacing_ = 0.0;
    lineHeight_ = 0.0;
    textAlign_ = TextAlign::Start;
    textDirection_ = TextDirection::LTR;
    maxLines_ = INT_MAX;
    ellipsis_ = false;
    hasText_ = false;

    // 清空富文本相关数据
    spanStyles_.clear();
    placeholders_.clear();
    fontFamily_ = "";

    textStyleStrategy_ = nullptr;
    paragraphStyleStrategy_ = nullptr;
}

// ========== 文本内容设置 ==========

ParagraphBuilder &ParagraphBuilder::setText(const std::string &text) {
    text_ = text;
    hasText_ = true;
    return *this;
}

ParagraphBuilder &ParagraphBuilder::setText(const char *text, uint32_t length) {
    LOGI("ParagraphBuilder: Setting text, text: %{public}s, length: %{public}u", text, length);
    if (text) {
        // 使用 strlen 获取实际字节长度，而不是使用传入的字符数
        text_ = std::string(text, length);
        hasText_ = true;
        LOGI("ParagraphBuilder: Text set successfully, byte length: %{public}zu", length);
    }
    return *this;
}

// ========== 基础样式设置 ==========

ParagraphBuilder &ParagraphBuilder::setFontSize(double fontSize) {
    if (fontSize > 0.0) {
        fontSize_ = fontSize;
    }
    return *this;
}

ParagraphBuilder &ParagraphBuilder::setFontWeight(FontWeight fontWeight) {
    fontWeight_ = fontWeight;
    return *this;
}

ParagraphBuilder &ParagraphBuilder::setFontStyle(FontStyle fontStyle) {
    fontStyle_ = fontStyle;
    return *this;
}

ParagraphBuilder &ParagraphBuilder::setColor(uint32_t color) {
    color_ = color;
    return *this;
}

// ========== 高级样式设置 ==========

ParagraphBuilder &ParagraphBuilder::setLetterSpacing(double letterSpacing) {
    letterSpacing_ = letterSpacing;
    return *this;
}

ParagraphBuilder &ParagraphBuilder::setWordSpacing(double wordSpacing) {
    wordSpacing_ = wordSpacing;
    return *this;
}

ParagraphBuilder &ParagraphBuilder::setLineHeight(double lineHeight) {
    if (lineHeight >= 0.0) {
        lineHeight_ = lineHeight;
    }
    return *this;
}

// ========== 段落样式设置 ==========

ParagraphBuilder &ParagraphBuilder::setTextAlign(TextAlign textAlign) {
    textAlign_ = textAlign;
    return *this;
}

ParagraphBuilder &ParagraphBuilder::setTextDirection(TextDirection textDirection) {
    textDirection_ = textDirection;
    return *this;
}

ParagraphBuilder &ParagraphBuilder::setMaxLines(int maxLines) {
    if (maxLines > 0) {
        maxLines_ = maxLines;
    }
    return *this;
}

ParagraphBuilder &ParagraphBuilder::setNeedEllipsis(bool needEllipsis) {
    needEllipsis_ = needEllipsis;
    return *this;
}

ParagraphBuilder &ParagraphBuilder::setEllipsis(const char *ellipsis, uint32_t length) {
    ellipsis_ = std::string(ellipsis, length);
    return *this;
}

// ========== 富文本支持（新增） ==========

/**
 * 添加单个 SpanStyle 范围
 */
ParagraphBuilder &ParagraphBuilder::addSpanStyle(const SpanStyleRange &spanStyle) {
    // 验证范围有效性
    if (spanStyle.start < spanStyle.end) {
        spanStyles_.push_back(spanStyle);
    }
    return *this;
}

/**
 * 批量设置 SpanStyles
 */
ParagraphBuilder &ParagraphBuilder::setSpanStyles(const std::vector<SpanStyleRange> &spanStyles) {
    spanStyles_ = spanStyles;
    return *this;
}

/**
 * 添加单个占位符
 */
ParagraphBuilder &ParagraphBuilder::addPlaceholder(const PlaceholderRange &placeholder) {
    // 验证范围有效性
    if (placeholder.start < placeholder.end && placeholder.width > 0 && placeholder.height > 0) {
        placeholders_.push_back(placeholder);
    }
    return *this;
}

/**
 * 批量设置占位符
 */
ParagraphBuilder &ParagraphBuilder::setPlaceholders(const std::vector<PlaceholderRange> &placeholders) {
    placeholders_ = placeholders;
    return *this;
}

/**
 * 设置字体族
 */
ParagraphBuilder &ParagraphBuilder::setFontFamily(const std::string &fontFamily) {
    fontFamily_ = fontFamily;
    return *this;
}

// ========== 策略注入 ==========

ParagraphBuilder &ParagraphBuilder::setTextStyleStrategy(std::unique_ptr<ITextStyleStrategy> strategy) {
    textStyleStrategy_ = std::move(strategy);
    return *this;
}

ParagraphBuilder &ParagraphBuilder::setParagraphStyleStrategy(std::unique_ptr<IParagraphStyleStrategy> strategy) {
    paragraphStyleStrategy_ = std::move(strategy);
    return *this;
}

// ========== 构建方法 ==========

std::unique_ptr<Paragraph> ParagraphBuilder::build() {
    LOGI("[ParagraphBuilder::build] Building paragraph: text='%{public}s', fontSize=%{public}.2f, maxLines=%{public}d",
         text_.c_str(), fontSize_,  maxLines_);

    // 验证必需参数
    if (!validate()) {
        LOGE("[ParagraphBuilder::build] Validation failed");
        throw std::runtime_error("ParagraphBuilder: Invalid parameters");
    }
    LOGI("[ParagraphBuilder::build] Parameters validated successfully");

    // 创建或使用默认策略
    auto textStyleStrategy = textStyleStrategy_ ? std::move(textStyleStrategy_) : createDefaultTextStyleStrategy();
    LOGI("[ParagraphBuilder::build] Text style strategy created");

    auto paragraphStyleStrategy =
        paragraphStyleStrategy_ ? std::move(paragraphStyleStrategy_) : createDefaultParagraphStyleStrategy();
    LOGI("[ParagraphBuilder::build] Paragraph style strategy created");

    // 构建段落对象，传递富文本参数
    LOGI("[ParagraphBuilder::build] Creating Paragraph object with %{public}zu spanStyles and %{public}zu placeholders",
         spanStyles_.size(), placeholders_.size());

    auto paragraph = std::make_unique<Paragraph>(
        text_,
        std::move(textStyleStrategy),
        std::move(paragraphStyleStrategy),
        spanStyles_,   // 传递 spanStyles
        placeholders_, // 传递 placeholders
        fontFamily_    // 传递 fontFamily
    );

    LOGI("[ParagraphBuilder::build] Paragraph built successfully: %{public}p", paragraph.get());
    return paragraph;
}

bool ParagraphBuilder::validate() const {
    // 文本可以为空，但必须明确设置
    if (!hasText_) {
        return false;
    }

    // 验证样式参数合法性
    if (fontSize_ <= 0.0) {
        return false;
    }

    if (maxLines_ <= 0) {
        return false;
    }

    return true;
}

ParagraphBuilder &ParagraphBuilder::reset() {
    setDefaults();
    return *this;
}

// ========== 辅助方法 ==========

std::unique_ptr<ITextStyleStrategy> ParagraphBuilder::createDefaultTextStyleStrategy() {
    auto basicStrategy = std::make_unique<BasicTextStyleStrategy>(fontSize_, fontWeight_, fontStyle_, color_);

    // 如果有高级样式，使用装饰器模式包装
    if (letterSpacing_ != 0.0 || wordSpacing_ != 0.0 || lineHeight_ > 0.0) {
        return std::make_unique<AdvancedTextStyleStrategy>(std::move(basicStrategy), letterSpacing_, wordSpacing_,
                                                           lineHeight_);
    }

    return basicStrategy;
}

std::unique_ptr<IParagraphStyleStrategy> ParagraphBuilder::createDefaultParagraphStyleStrategy() {
    return std::make_unique<StandardParagraphStyleStrategy>(textAlign_, textDirection_, maxLines_, needEllipsis_, ellipsis_);
}

} // namespace OH
