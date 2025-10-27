#ifndef REFACTORED_PARAGRAPH_H
#define REFACTORED_PARAGRAPH_H

#include <native_drawing/drawing_canvas.h>
#include <string>
#include <memory>
#include <vector>
#include "oh_native_paragraph_types.h"
#include "oh_native_text_style_strategy.h"
#include "oh_resource_manager.h"
#include "oh_cache_manager.h"
#include "../render_node/oh_base_render_node.h"

namespace OH {

/**
 * 重构后的段落类（应用多种设计模式）
 *
 * 应用的设计模式：
 * 1. Strategy Pattern - 文本和段落样式策略
 * 2. RAII Pattern - 资源自动管理
 * 3. Template Method - 布局流程模板
 * 4. Observer Pattern - 缓存失效通知
 * 5. Facade Pattern - 统一的对外接口
 *
 * 核心改进：
 * 1. 职责单一：每个类只负责一件事
 * 2. 依赖注入：通过策略模式注入行为
 * 3. 资源安全：使用RAII确保资源正确释放
 * 4. 高性能：智能缓存策略
 * 5. 易测试：接口与实现分离
 */
class Paragraph : public BaseRenderNode {
public:
    /**
     * 构造函数（通过Builder创建）
     * 现在支持富文本样式、占位符和字体族
     */
    Paragraph(const std::string &text, std::unique_ptr<ITextStyleStrategy> textStyleStrategy,
              std::unique_ptr<IParagraphStyleStrategy> paragraphStyleStrategy,
              const std::vector<SpanStyleRange> &spanStyles = {},
              const std::vector<PlaceholderRange> &placeholders = {}, const std::string &fontFamily = "");

    /**
     * 析构函数（RAII自动清理）
     */
    ~Paragraph();

    // 禁止拷贝和赋值（唯一所有权）
    Paragraph(const Paragraph &) = delete;
    Paragraph &operator=(const Paragraph &) = delete;

    // 支持移动语义
    Paragraph(Paragraph &&) noexcept = default;
    Paragraph &operator=(Paragraph &&) noexcept = default;

    // ========== 布局操作（Template Method） ==========

    /**
     * 重新布局
     */
    void layout(double maxWidth);

    // ========== 度量查询（Facade） ==========

    double getWidth() const {
        return layoutWidth_;
    }
    double getHeight() const;
    double getMinIntrinsicWidth() const;
    double getMaxIntrinsicWidth() const;
    double getAlphabeticBaseline() const;
    double getIdeographicBaseline() const;
    double getLongestLine() const;
    bool didExceedMaxLines() const;
    uint32_t getLineCount() const;

    /**
     * 获取所有度量信息（一次性获取，减少调用）
     */
    const ParagraphMetrics &getMetrics() const;

    // ========== 行信息查询 ==========

    const LineMetrics *getLineMetrics(uint32_t lineIndex) const;
    const std::vector<LineMetrics> &getAllLineMetrics() const;

    double getLineLeft(uint32_t lineIndex) const;
    double getLineRight(uint32_t lineIndex) const;
    double getLineTop(uint32_t lineIndex) const;
    double getLineBottom(uint32_t lineIndex) const;
    double getLineWidth(uint32_t lineIndex) const;
    double getLineHeight(uint32_t lineIndex) const;
    uint32_t getLineStart(uint32_t lineIndex) const;
    uint32_t getLineEnd(uint32_t lineIndex, bool visibleEnd = false) const;
    double getLineBaseline(uint32_t lineIndex) const;

    // ========== 位置查询 ==========

    double getHorizontalPosition(uint32_t offset, bool usePrimaryDirection = true) const;
    uint32_t getOffsetForPosition(double dx, double dy) const;
    TextRect getCursorRect(uint32_t offset) const;
    WordBoundary getWordBoundary(uint32_t offset) const;
    std::vector<TextRect> getRectsForRange(uint32_t start, uint32_t end) const;

    // ========== 占位符查询 ==========

    /**
     * 获取占位符的矩形区域列表
     * 返回与 placeholders_ 数组对应的矩形区域
     * 参考 SkiaParagraph.placeholderRects 实现
     */
    std::vector<TextRect> getPlaceholderRects() const;

    // ========== 绘制 ==========

    void paint(float x, float y);

    // ========== 辅助查询 ==========

    uint32_t getLineForOffset(uint32_t offset) const;
    uint32_t getLineForVerticalPosition(double vertical) const;

    // ========== 文本内容访问 ==========

    const std::string &getText() const {
        return text_;
    }
    uint32_t getTextLength() const {
        return text_.length();
    }
    OH_DrawingNode_Type getType() override;

protected:
    // ========== Template Method 步骤 ==========

    /**
     * 初始化Typography（模板方法）
     */
    virtual void initializeTypography();

    /**
     * 执行布局（可被子类重写）
     */
    virtual void performLayout(double maxWidth);

    /**
     * 布局后处理（钩子方法）
     */
    virtual void onLayoutComplete() {
    }

private:
    // ========== 资源创建 ==========

    void initModifier() override;
    void createOrUpdatePositionProperty(float x, float y);
    void createTypographyResources();
    OH_Drawing_Typography *getOrCreateTypography();

    // ========== 富文本辅助方法 ==========

    /**
     * 应用 SpanStyles 到 Typography Handler
     * 使用 Cut-Op 机制（参考 SkiaParagraph）
     */
    void applySpanStyles(OH_Drawing_TypographyCreate *handler, OH_Drawing_TextStyle *baseStyle);

    // ========== Cut-Op 机制的内部数据结构（仅在 .cpp 中可见）==========
    struct StyleCut;
    struct StyleOp;

    // ========== 样式应用辅助方法 ==========

    /**
     * Step 1: 生成样式切分点列表
     */
    std::vector<StyleCut> generateStyleCuts();

    /**
     * Step 2: 将 Cut 转换为 Op（合并样式）
     */
    std::vector<StyleOp> convertCutsToOps(const std::vector<StyleCut> &cuts);

    /**
     * 处理样式添加（Cut::Add）
     */
    void handleStyleAdd(
        const StyleCut &cut,
        std::vector<const SpanStyleRange *> &activeStyles,
        std::vector<StyleOp> &ops);

    /**
     * 处理样式移除（Cut::Remove）
     */
    void handleStyleRemove(
        const StyleCut &cut,
        std::vector<const SpanStyleRange *> &activeStyles,
        std::vector<StyleOp> &ops);

    /**
     * Step 3: 根据 Op 构建段落
     */
    void buildParagraphFromOps(
        OH_Drawing_TypographyCreate *handler,
        const std::vector<StyleOp> &ops);

    /**
     * 创建合并样式（从活跃样式栈）
     */
    ResourceHandle<OH_Drawing_TextStyle> createMergedStyleFromStack(
        const std::vector<const SpanStyleRange *> &activeStyles);

    /**
     * 应用单个 SpanStyleRange 到 TextStyle
     */
    void applySpanStyleToTextStyle(OH_Drawing_TextStyle *textStyle, const SpanStyleRange &span);

    // ========== 缓存构建器 ==========

    std::vector<LineMetrics> buildLineMetricsCache();
    ParagraphMetrics buildParagraphMetrics();

    // ========== 成员变量 ==========

    // 文本内容
    std::string text_;

    // 策略对象
    std::unique_ptr<ITextStyleStrategy> textStyleStrategy_;
    std::unique_ptr<IParagraphStyleStrategy> paragraphStyleStrategy_;

    // 布局状态
    double layoutWidth_;
    mutable bool isLayouted_;

    // 资源管理（RAII）
    FontCollectionPtr fontCollection_;
    TypographyPtr typography_;

    // 富文本支持（新增）
    std::vector<SpanStyleRange> spanStyles_;
    std::vector<PlaceholderRange> placeholders_;
    std::string fontFamily_;

    // 缓存管理
    mutable std::unique_ptr<LineMetricsCacheManager> lineMetricsCache_;
    mutable std::unique_ptr<MetricsCacheManager> metricsCache_;
    ArkUI_RenderContentModifierHandle modifier_ = nullptr;

    // 位置
    ArkUI_Vector2PropertyHandle posProperty_ = nullptr;
};

/**
 * 转换 PlaceholderVerticalAlign 到 OH_Drawing_PlaceholderVerticalAlignment
 */
static OH_Drawing_PlaceholderVerticalAlignment convertPlaceholderAlignment(PlaceholderVerticalAlign align) {
    switch (align) {
    case PlaceholderVerticalAlign::AboveBaseline:
        return ALIGNMENT_ABOVE_BASELINE;
    case PlaceholderVerticalAlign::Top:
        return ALIGNMENT_TOP_OF_ROW_BOX;
    case PlaceholderVerticalAlign::Bottom:
        return ALIGNMENT_BOTTOM_OF_ROW_BOX;
    case PlaceholderVerticalAlign::Center:
        return ALIGNMENT_CENTER_OF_ROW_BOX;
    case PlaceholderVerticalAlign::TextTop:
        return ALIGNMENT_TOP_OF_ROW_BOX;
    case PlaceholderVerticalAlign::TextBottom:
        return ALIGNMENT_BELOW_BASELINE;
    case PlaceholderVerticalAlign::TextCenter:
        return ALIGNMENT_CENTER_OF_ROW_BOX;
    default:
        return ALIGNMENT_CENTER_OF_ROW_BOX;
    }
}

} // namespace OH

#endif // REFACTORED_PARAGRAPH_H
