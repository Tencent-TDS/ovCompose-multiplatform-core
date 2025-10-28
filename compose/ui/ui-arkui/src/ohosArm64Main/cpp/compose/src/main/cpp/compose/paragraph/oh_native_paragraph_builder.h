#ifndef PARAGRAPH_BUILDER_H
#define PARAGRAPH_BUILDER_H

#include <string>
#include <memory>
#include "oh_native_paragraph_types.h"
#include "oh_native_text_style_strategy.h"

namespace OH {

// 前向声明
class Paragraph;

/**
 * Builder模式：段落构建器
 *
 * 设计目的：
 * 1. 简化复杂对象的创建过程
 * 2. 支持流式API（链式调用）
 * 3. 提供参数验证和默认值处理
 * 4. 分离构建逻辑和对象本身
 *
 * 使用示例：
 * auto paragraph = ParagraphBuilder()
 *     .setText("Hello World")
 *     .setFontSize(16.0)
 *     .setColor(0xFF000000)
 *     .build();
 */
class ParagraphBuilder {
public:
    ParagraphBuilder();
    ~ParagraphBuilder() = default;

    // ========== 文本内容 ==========

    ParagraphBuilder &setText(const std::string &text);
    ParagraphBuilder &setText(const char *text, uint32_t length);

    // ========== 基础样式 ==========

    ParagraphBuilder &setFontSize(double fontSize);
    ParagraphBuilder &setFontWeight(FontWeight fontWeight);
    ParagraphBuilder &setFontStyle(FontStyle fontStyle);
    ParagraphBuilder &setColor(uint32_t color);

    // ========== 高级样式 ==========

    ParagraphBuilder &setLetterSpacing(double letterSpacing);
    ParagraphBuilder &setWordSpacing(double wordSpacing);
    ParagraphBuilder &setLineHeight(double lineHeight);

    // ========== 段落样式 ==========

    ParagraphBuilder &setTextAlign(TextAlign textAlign);
    ParagraphBuilder &setTextDirection(TextDirection textDirection);
    ParagraphBuilder &setMaxLines(int maxLines);
    ParagraphBuilder &setNeedEllipsis(bool needEllipsis);
    ParagraphBuilder &setEllipsis(const char *ellipsis, uint32_t length);

    // ========== 富文本支持（新增） ==========

    /**
     * 添加局部样式范围
     * @param spanStyle 样式范围对象
     */
    ParagraphBuilder &addSpanStyle(const SpanStyleRange &spanStyle);

    /**
     * 批量添加局部样式
     * @param spanStyles 样式范围列表
     */
    ParagraphBuilder &setSpanStyles(const std::vector<SpanStyleRange> &spanStyles);

    /**
     * 添加占位符
     * @param placeholder 占位符对象
     */
    ParagraphBuilder &addPlaceholder(const PlaceholderRange &placeholder);

    /**
     * 批量添加占位符
     * @param placeholders 占位符列表
     */
    ParagraphBuilder &setPlaceholders(const std::vector<PlaceholderRange> &placeholders);

    /**
     * 设置全局字体族
     * @param fontFamily 字体族名称
     */
    ParagraphBuilder &setFontFamily(const std::string &fontFamily);

    // ========== 策略注入（高级用法） ==========

    ParagraphBuilder &setTextStyleStrategy(std::unique_ptr<ITextStyleStrategy> strategy);
    ParagraphBuilder &setParagraphStyleStrategy(std::unique_ptr<IParagraphStyleStrategy> strategy);

    // ========== 构建方法 ==========

    /**
     * 构建段落对象
     * @return 构建完成的段落对象（智能指针）
     * @throws std::runtime_error 如果必需参数未设置
     */
    std::unique_ptr<Paragraph> build();

    /**
     * 验证参数是否有效
     */
    bool validate() const;

    /**
     * 重置构建器（复用）
     */
    ParagraphBuilder &reset();

private:
    // 文本内容
    std::string text_;

    // 基础样式
    double fontSize_;
    FontWeight fontWeight_;
    FontStyle fontStyle_;
    uint32_t color_;

    // 高级样式
    double letterSpacing_;
    double wordSpacing_;
    double lineHeight_;

    // 段落样式
    TextAlign textAlign_;
    TextDirection textDirection_;
    int maxLines_;
    bool needEllipsis_;
    std::string ellipsis_;

    // 富文本支持（新增）
    std::vector<SpanStyleRange> spanStyles_;
    std::vector<PlaceholderRange> placeholders_;
    std::string fontFamily_;

    // 策略对象（可选）
    std::unique_ptr<ITextStyleStrategy> textStyleStrategy_;
    std::unique_ptr<IParagraphStyleStrategy> paragraphStyleStrategy_;

    // 标记是否已设置必需参数
    bool hasText_;

    // 辅助方法
    void setDefaults();
    std::unique_ptr<ITextStyleStrategy> createDefaultTextStyleStrategy();
    std::unique_ptr<IParagraphStyleStrategy> createDefaultParagraphStyleStrategy();
};
} // namespace OH

#endif // PARAGRAPH_BUILDER_H
