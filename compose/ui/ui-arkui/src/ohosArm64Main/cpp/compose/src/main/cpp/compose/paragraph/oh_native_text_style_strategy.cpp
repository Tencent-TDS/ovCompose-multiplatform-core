#include <native_drawing/drawing_text_typography.h>
#include "oh_native_text_style_strategy.h"

namespace OH {

// ========== BasicTextStyleStrategy 实现 ==========

BasicTextStyleStrategy::BasicTextStyleStrategy(
    double fontSize,
    FontWeight fontWeight,
    FontStyle fontStyle,
    uint32_t color) : fontSize_(fontSize),
                      fontWeight_(fontWeight),
                      fontStyle_(fontStyle),
                      color_(color) {
}

void BasicTextStyleStrategy::applyTo(OH_Drawing_TextStyle *textStyle) const {
    if (!textStyle) return;

    OH_Drawing_SetTextStyleColor(textStyle, color_);
    OH_Drawing_SetTextStyleFontSize(textStyle, fontSize_);
    OH_Drawing_SetTextStyleFontWeight(textStyle, static_cast<int>(fontWeight_));
    OH_Drawing_SetTextStyleFontStyle(textStyle, static_cast<int>(fontStyle_));
    OH_Drawing_SetTextStyleBaseLine(textStyle, OH_Drawing_TextBaseline::TEXT_BASELINE_ALPHABETIC);
}

std::unique_ptr<ITextStyleStrategy> BasicTextStyleStrategy::clone() const {
    return std::make_unique<BasicTextStyleStrategy>(
        fontSize_, fontWeight_, fontStyle_, color_);
}

// ========== AdvancedTextStyleStrategy 实现 ==========

AdvancedTextStyleStrategy::AdvancedTextStyleStrategy(
    std::unique_ptr<ITextStyleStrategy> baseStrategy,
    double letterSpacing,
    double wordSpacing,
    double lineHeight) : baseStrategy_(std::move(baseStrategy)),
                         letterSpacing_(letterSpacing),
                         wordSpacing_(wordSpacing),
                         lineHeight_(lineHeight) {
}

void AdvancedTextStyleStrategy::applyTo(OH_Drawing_TextStyle *textStyle) const {
    if (!textStyle) return;

    // 先应用基础样式
    if (baseStrategy_) {
        baseStrategy_->applyTo(textStyle);
    }

    // 再应用高级样式（只在非默认值时）
    if (letterSpacing_ != 0.0) {
        OH_Drawing_SetTextStyleLetterSpacing(textStyle, letterSpacing_);
    }

    if (wordSpacing_ != 0.0) {
        OH_Drawing_SetTextStyleWordSpacing(textStyle, wordSpacing_);
    }

    if (lineHeight_ > 0.0) {
        OH_Drawing_SetTextStyleFontHeight(textStyle, lineHeight_);
    }
}

std::unique_ptr<ITextStyleStrategy> AdvancedTextStyleStrategy::clone() const {
    return std::make_unique<AdvancedTextStyleStrategy>(
        baseStrategy_ ? baseStrategy_->clone() : nullptr,
        letterSpacing_, wordSpacing_, lineHeight_);
}

// ========== StandardParagraphStyleStrategy 实现 ==========

StandardParagraphStyleStrategy::StandardParagraphStyleStrategy(
    TextAlign textAlign,
    TextDirection textDirection,
    int maxLines,
    bool needEllipsis,
    std::string ellipsis) : textAlign_(textAlign),
                     textDirection_(textDirection),
                     maxLines_(maxLines),
                     needEllipsis_(needEllipsis),
                     ellipsis_(ellipsis) {
}

void StandardParagraphStyleStrategy::applyTo(OH_Drawing_TypographyStyle *typoStyle) const {
    if (!typoStyle) return;

    OH_Drawing_SetTypographyTextDirection(typoStyle, static_cast<int>(textDirection_));
    OH_Drawing_SetTypographyTextAlign(typoStyle, static_cast<int>(textAlign_));
    OH_Drawing_SetTypographyTextMaxLines(typoStyle, maxLines_);

    if (ellipsis_.length() > 0 && needEllipsis_) {
        OH_Drawing_SetTypographyTextEllipsis(typoStyle, ellipsis_.c_str());
    }

    OH_Drawing_SetTypographyTextBreakStrategy(typoStyle, OH_Drawing_BreakStrategy::BREAK_STRATEGY_HIGH_QUALITY);
    OH_Drawing_SetTypographyTextWordBreakType(typoStyle, OH_Drawing_WordBreakType::WORD_BREAK_TYPE_NORMAL);
}

std::unique_ptr<IParagraphStyleStrategy> StandardParagraphStyleStrategy::clone() const {
    return std::make_unique<StandardParagraphStyleStrategy>(
        textAlign_, textDirection_, maxLines_, needEllipsis_, ellipsis_);
}
} // namespace OH
