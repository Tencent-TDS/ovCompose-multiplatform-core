#ifndef TEXT_STYLE_STRATEGY_H
#define TEXT_STYLE_STRATEGY_H

#include <native_drawing/drawing_text_declaration.h>

#include <memory>

#include "oh_native_paragraph_types.h"

namespace OH {

/**
 * 策略模式：文本样式策略接口
 *
 * 设计目的：
 * 1. 解耦样式创建逻辑
 * 2. 便于扩展新的样式类型
 * 3. 提高可测试性
 */
class ITextStyleStrategy {
public:
    virtual ~ITextStyleStrategy() = default;

    /**
     * 应用文本样式到HarmonyOS TextStyle对象
     */
    virtual void applyTo(OH_Drawing_TextStyle *textStyle) const = 0;

    /**
     * 克隆策略对象
     */
    virtual std::unique_ptr<ITextStyleStrategy> clone() const = 0;
};

/**
 * 基础文本样式策略（具体策略）
 */
class BasicTextStyleStrategy final : public ITextStyleStrategy {
public:
    BasicTextStyleStrategy(double fontSize, FontWeight fontWeight, FontStyle fontStyle, uint32_t color);

    void applyTo(OH_Drawing_TextStyle *textStyle) const override;
    std::unique_ptr<ITextStyleStrategy> clone() const override;

private:
    double fontSize_;
    FontWeight fontWeight_;
    FontStyle fontStyle_;
    uint32_t color_;
};

/**
 * 高级文本样式策略（装饰器模式 + 策略模式）
 */
class AdvancedTextStyleStrategy : public ITextStyleStrategy {
public:
    AdvancedTextStyleStrategy(std::unique_ptr<ITextStyleStrategy> baseStrategy, double letterSpacing,
                              double wordSpacing, double lineHeight);

    void applyTo(OH_Drawing_TextStyle *textStyle) const override;
    std::unique_ptr<ITextStyleStrategy> clone() const override;

private:
    std::unique_ptr<ITextStyleStrategy> baseStrategy_;
    double letterSpacing_;
    double wordSpacing_;
    double lineHeight_;
};

/**
 * 段落样式策略接口
 */
class IParagraphStyleStrategy {
public:
    virtual ~IParagraphStyleStrategy() = default;

    virtual void applyTo(OH_Drawing_TypographyStyle *typoStyle) const = 0;
    virtual std::unique_ptr<IParagraphStyleStrategy> clone() const = 0;
};

/**
 * 标准段落样式策略
 */
class StandardParagraphStyleStrategy : public IParagraphStyleStrategy {
public:
    StandardParagraphStyleStrategy(TextAlign textAlign, TextDirection textDirection, int maxLines, bool needEllipsis,
                                   std::string ellipsis);

    void applyTo(OH_Drawing_TypographyStyle *typoStyle) const override;
    std::unique_ptr<IParagraphStyleStrategy> clone() const override;

private:
    TextAlign textAlign_;
    TextDirection textDirection_;
    int maxLines_;
    bool needEllipsis_;
    std::string ellipsis_;
};
} // namespace OH
#endif // TEXT_STYLE_STRATEGY_H
