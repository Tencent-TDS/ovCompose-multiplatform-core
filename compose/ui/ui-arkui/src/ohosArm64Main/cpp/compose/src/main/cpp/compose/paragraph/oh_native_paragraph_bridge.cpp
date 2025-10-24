#include "oh_native_paragraph_bridge.h"
#include "oh_native_paragraph.h"
#include "oh_native_paragraph_builder.h"
#include "../xcomponent_log.h"

using namespace OH;

// ========== 内部结构 ==========

struct ParagraphHandle {
    enum Type { BUILDER,
                PARAGRAPH } type;

    union {
        ParagraphBuilder *builder;
        Paragraph *paragraph;
    } data;

    ParagraphHandle(ParagraphBuilder *paraBuilder) : type(BUILDER) {
        data.builder = paraBuilder;
    }

    ParagraphHandle(Paragraph *para) : type(PARAGRAPH) {
        data.paragraph = para;
    }
};

// ========== 辅助宏 ==========

#define CHECK_BUILDER(handle)                                                         \
    if (!handle || handle->type != ParagraphHandle::BUILDER || !handle->data.builder) \
    return

#define CHECK_BUILDER_RET(handle, ret)                                                \
    if (!handle || handle->type != ParagraphHandle::BUILDER || !handle->data.builder) \
    return ret

#define CHECK_PARAGRAPH(handle)                                                           \
    if (!handle || handle->type != ParagraphHandle::PARAGRAPH || !handle->data.paragraph) \
    return

#define CHECK_PARAGRAPH_RET(handle, ret)                                                  \
    if (!handle || handle->type != ParagraphHandle::PARAGRAPH || !handle->data.paragraph) \
    return ret

// ========== C API 实现 ==========

extern "C" {

// ========== Builder API ==========

ParagraphHandle_Handle ParagraphBuilder_create() {
    try {
        ParagraphBuilder *builder = new ParagraphBuilder();
        LOGI("ParagraphBuilder_create succeeded, Builder: %{public}p", builder);
        return new ParagraphHandle(builder);
    } catch (...) {
        LOGI("ParagraphBuilder_create failed");
        return nullptr;
    }
}

void ParagraphBuilder_setText(ParagraphHandle_Handle handle, const char *text, uint32_t length) {
    CHECK_BUILDER(handle);
    if (text) {
        handle->data.builder->setText(text, length);
    }
}

void ParagraphBuilder_setFontSize(ParagraphHandle_Handle handle, double fontSize) {
    CHECK_BUILDER(handle);
    handle->data.builder->setFontSize(fontSize);
}

void ParagraphBuilder_setFontWeight(ParagraphHandle_Handle handle, uint32_t fontWeight) {
    CHECK_BUILDER(handle);
    handle->data.builder->setFontWeight(static_cast<FontWeight>(fontWeight));
}

void ParagraphBuilder_setFontStyle(ParagraphHandle_Handle handle, int fontStyle) {
    CHECK_BUILDER(handle);
    handle->data.builder->setFontStyle(static_cast<FontStyle>(fontStyle));
}

void ParagraphBuilder_setColor(ParagraphHandle_Handle handle, uint32_t color) {
    CHECK_BUILDER(handle);
    handle->data.builder->setColor(color);
}

void ParagraphBuilder_setLetterSpacing(ParagraphHandle_Handle handle, double spacing) {
    CHECK_BUILDER(handle);
    handle->data.builder->setLetterSpacing(spacing);
}

void ParagraphBuilder_setWordSpacing(ParagraphHandle_Handle handle, double spacing) {
    CHECK_BUILDER(handle);
    handle->data.builder->setWordSpacing(spacing);
}

void ParagraphBuilder_setLineHeight(ParagraphHandle_Handle handle, double lineHeight) {
    CHECK_BUILDER(handle);
    handle->data.builder->setLineHeight(lineHeight);
}

void ParagraphBuilder_setTextAlign(ParagraphHandle_Handle handle, int textAlign) {
    CHECK_BUILDER(handle);
    handle->data.builder->setTextAlign(static_cast<TextAlign>(textAlign));
}

void ParagraphBuilder_setTextDirection(ParagraphHandle_Handle handle, int textDirection) {
    CHECK_BUILDER(handle);
    handle->data.builder->setTextDirection(static_cast<TextDirection>(textDirection));
}

void ParagraphBuilder_setMaxLines(ParagraphHandle_Handle handle, uint32_t maxLines) {
    CHECK_BUILDER(handle);
    handle->data.builder->setMaxLines(maxLines);
}

void ParagraphBuilder_setNeedEllipsis(ParagraphHandle_Handle handle, bool needEllipsis) {
    CHECK_BUILDER(handle);
    handle->data.builder->setNeedEllipsis(needEllipsis);
}

void ParagraphBuilder_setEllipsis(ParagraphHandle_Handle handle, const char *ellipsis, uint32_t length) {
    CHECK_BUILDER(handle);
    handle->data.builder->setEllipsis(ellipsis, length);
}

ParagraphHandle_Handle ParagraphBuilder_build(ParagraphHandle_Handle handle) {
    LOGI("ParagraphBuilder_build called");
    CHECK_BUILDER_RET(handle, nullptr);

    try {
        auto paragraph = handle->data.builder->build();
        delete handle->data.builder;
        delete handle;
        LOGI("ParagraphBuilder_build succeeded, Paragraph: %{public}p", paragraph.get());

        return new ParagraphHandle(paragraph.release());
    } catch (...) {
        LOGE("ParagraphBuilder_build failed");
        return nullptr;
    }
}

// ========== Paragraph API ==========

void Paragraph_destroy(ParagraphHandle_Handle handle) {
    if (!handle)
        return;

    if (handle->type == ParagraphHandle::BUILDER) {
        delete handle->data.builder;
    } else {
        delete handle->data.paragraph;
    }

    delete handle;
}

void Paragraph_layout(ParagraphHandle_Handle handle, double maxWidth) {
    CHECK_PARAGRAPH(handle);
    LOGI("[Paragraph_layout] Layout called: handle=%{public}p, maxWidth=%{public}.2f", handle, maxWidth);
    handle->data.paragraph->layout(maxWidth);
}

// ========== 度量属性 ==========

double Paragraph_getWidth(ParagraphHandle_Handle handle) {
    CHECK_PARAGRAPH_RET(handle, 0.0);
    double width = handle->data.paragraph->getWidth();
    LOGI("[Paragraph_getWidth] Width: %{public}.2f", width);
    return width;
}

double Paragraph_getHeight(ParagraphHandle_Handle handle) {
    CHECK_PARAGRAPH_RET(handle, 0.0);
    double height = handle->data.paragraph->getHeight();
    LOGI("[Paragraph_getHeight] Height: %{public}.2f", height);
    return height;
}

double Paragraph_getMinIntrinsicWidth(ParagraphHandle_Handle handle) {
    CHECK_PARAGRAPH_RET(handle, 0.0);
    double width = handle->data.paragraph->getMinIntrinsicWidth();
    LOGI("[Paragraph_getMinIntrinsicWidth] MinIntrinsicWidth: %{public}.2f", width);
    return width;
}

double Paragraph_getMaxIntrinsicWidth(ParagraphHandle_Handle handle) {
    CHECK_PARAGRAPH_RET(handle, 0.0);
    double width = handle->data.paragraph->getMaxIntrinsicWidth();
    LOGI("[Paragraph_getMaxIntrinsicWidth] MaxIntrinsicWidth: %{public}.2f", width);
    return width;
}

double Paragraph_getAlphabeticBaseline(ParagraphHandle_Handle handle) {
    CHECK_PARAGRAPH_RET(handle, 0.0);
    return handle->data.paragraph->getAlphabeticBaseline();
}

double Paragraph_getIdeographicBaseline(ParagraphHandle_Handle handle) {
    CHECK_PARAGRAPH_RET(handle, 0.0);
    return handle->data.paragraph->getIdeographicBaseline();
}

double Paragraph_getLongestLine(ParagraphHandle_Handle handle) {
    CHECK_PARAGRAPH_RET(handle, 0.0);
    return handle->data.paragraph->getLongestLine();
}

bool Paragraph_didExceedMaxLines(ParagraphHandle_Handle handle) {
    CHECK_PARAGRAPH_RET(handle, false);
    return handle->data.paragraph->didExceedMaxLines();
}

uint32_t Paragraph_getLineCount(ParagraphHandle_Handle handle) {
    CHECK_PARAGRAPH_RET(handle, 0);
    return handle->data.paragraph->getLineCount();
}

// ========== 行信息查询 ==========

double Paragraph_getLineLeft(ParagraphHandle_Handle handle, uint32_t lineIndex) {
    CHECK_PARAGRAPH_RET(handle, 0.0);
    return handle->data.paragraph->getLineLeft(lineIndex);
}

double Paragraph_getLineRight(ParagraphHandle_Handle handle, uint32_t lineIndex) {
    CHECK_PARAGRAPH_RET(handle, 0.0);
    return handle->data.paragraph->getLineRight(lineIndex);
}

double Paragraph_getLineTop(ParagraphHandle_Handle handle, uint32_t lineIndex) {
    CHECK_PARAGRAPH_RET(handle, 0.0);
    return handle->data.paragraph->getLineTop(lineIndex);
}

double Paragraph_getLineBottom(ParagraphHandle_Handle handle, uint32_t lineIndex) {
    CHECK_PARAGRAPH_RET(handle, 0.0);
    return handle->data.paragraph->getLineBottom(lineIndex);
}

double Paragraph_getLineWidth(ParagraphHandle_Handle handle, uint32_t lineIndex) {
    CHECK_PARAGRAPH_RET(handle, 0.0);
    return handle->data.paragraph->getLineWidth(lineIndex);
}

double Paragraph_getLineHeight(ParagraphHandle_Handle handle, uint32_t lineIndex) {
    CHECK_PARAGRAPH_RET(handle, 0.0);
    return handle->data.paragraph->getLineHeight(lineIndex);
}

uint32_t Paragraph_getLineStart(ParagraphHandle_Handle handle, uint32_t lineIndex) {
    CHECK_PARAGRAPH_RET(handle, 0);
    return handle->data.paragraph->getLineStart(lineIndex);
}

uint32_t Paragraph_getLineEnd(ParagraphHandle_Handle handle, uint32_t lineIndex, bool visibleEnd) {
    CHECK_PARAGRAPH_RET(handle, 0);
    return handle->data.paragraph->getLineEnd(lineIndex, visibleEnd);
}

double Paragraph_getLineBaseline(ParagraphHandle_Handle handle, uint32_t lineIndex) {
    CHECK_PARAGRAPH_RET(handle, 0.0);
    return handle->data.paragraph->getLineBaseline(lineIndex);
}

// ========== 位置查询 ==========

double Paragraph_getHorizontalPosition(ParagraphHandle_Handle handle, uint32_t offset, bool usePrimaryDirection) {
    CHECK_PARAGRAPH_RET(handle, 0.0);
    return handle->data.paragraph->getHorizontalPosition(offset, usePrimaryDirection);
}

uint32_t Paragraph_getOffsetForPosition(ParagraphHandle_Handle handle, double dx, double dy) {
    CHECK_PARAGRAPH_RET(handle, 0);
    return handle->data.paragraph->getOffsetForPosition(dx, dy);
}

void Paragraph_getCursorRect(ParagraphHandle_Handle handle, uint32_t offset, double *outLeft, double *outTop,
                             double *outRight, double *outBottom) {
    CHECK_PARAGRAPH(handle);
    if (!outLeft || !outTop || !outRight || !outBottom)
        return;

    TextRect rect = handle->data.paragraph->getCursorRect(offset);
    *outLeft = rect.left;
    *outTop = rect.top;
    *outRight = rect.right;
    *outBottom = rect.bottom;
}

void Paragraph_getWordBoundary(ParagraphHandle_Handle handle, uint32_t offset, uint32_t *outStart, uint32_t *outEnd) {
    CHECK_PARAGRAPH(handle);
    if (!outStart || !outEnd)
        return;

    WordBoundary boundary = handle->data.paragraph->getWordBoundary(offset);
    *outStart = boundary.start;
    *outEnd = boundary.end;
}

uint32_t Paragraph_getRectsForRangeCount(ParagraphHandle_Handle handle, uint32_t start, uint32_t end) {
    CHECK_PARAGRAPH_RET(handle, 0);
    auto rects = handle->data.paragraph->getRectsForRange(start, end);
    return rects.size();
}

uint32_t Paragraph_getRectsForRange(ParagraphHandle_Handle handle, uint32_t start, uint32_t end, double *rects,
                                    uint32_t capacity) {
    CHECK_PARAGRAPH_RET(handle, 0);
    if (!rects || capacity == 0)
        return 0;

    auto rectList = handle->data.paragraph->getRectsForRange(start, end);
    uint32_t count = std::min(static_cast<uint32_t>(rectList.size()), capacity / 4);

    for (uint32_t i = 0; i < count; ++i) {
        uint32_t base = i * 4;
        rects[base + 0] = rectList[i].left;
        rects[base + 1] = rectList[i].top;
        rects[base + 2] = rectList[i].right;
        rects[base + 3] = rectList[i].bottom;
    }

    return count;
}

// ========== 绘制 ==========

void Paragraph_paint(ParagraphHandle_Handle handle, double x, double y) {
    CHECK_PARAGRAPH(handle);
    LOGI("[Paragraph_paint] Paint called from bridge: handle=%{public}p, position=(%{public}.2f, %{public}.2f)", handle, x, y);
    handle->data.paragraph->paint(x, y);
}

// ========== 辅助查询 ==========

BaseRenderNode_Handle Paragraph_getBaseRenderNode(ParagraphHandle_Handle handle) {
    CHECK_PARAGRAPH_RET(handle, nullptr);
    LOGI("Paragraph_getBaseRenderNode, handle=%{public}p, BaseRenderNode: %{public}p", handle, handle->data.paragraph);
    return reinterpret_cast<BaseRenderNode_Handle>(handle->data.paragraph);
}

uint32_t Paragraph_getLineForOffset(ParagraphHandle_Handle handle, uint32_t offset) {
    CHECK_PARAGRAPH_RET(handle, 0);
    return handle->data.paragraph->getLineForOffset(offset);
}

uint32_t Paragraph_getLineForVerticalPosition(ParagraphHandle_Handle handle, double vertical) {
    CHECK_PARAGRAPH_RET(handle, 0);
    return handle->data.paragraph->getLineForVerticalPosition(vertical);
}

} // extern "C"
