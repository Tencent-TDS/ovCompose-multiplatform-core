#ifndef OH_NATIVE_PICTURE_RECORDER_H
#define OH_NATIVE_PICTURE_RECORDER_H

#include <array>
#include <memory>
#include <unordered_map>
#include <vector>
#include "../constants/oh_native_constants.h"
#include "../constants/oh_native_enums.h"
#include "../render_node/oh_base_render_node.h"
#include "../render_node/oh_arc_render_node.h"
#include "../render_node/oh_clip_render_node.h"
#include "../render_node/oh_image_display_render_node.h"
#include "../render_node/oh_line_gradient_render_node.h"
#include "../render_node/oh_line_render_node.h"
#include "../render_node/oh_oval_render_node.h"
#include "../render_node/oh_path_render_node.h"
#include "../render_node/oh_points_render_node.h"
#include "../render_node/oh_rect_gradient_render_node.h"
#include "../trace/oh_systrace_section.h"
#include "../utils/oh_hash_funcs.h"
#include "../xcomponent_log.h"
#include "oh_native_picture_recorder_drawing_Item.h"
#include "oh_native_render_node_save_state.h"

namespace OH {

/**
 * @brief Creates a specific type of drawing render node based on the given
 * OH_Native_Drawing_Type.
 *
 * This function returns a std::unique_ptr to a newly created render node object
 * corresponding to the specified drawing type. For certain types, specialized
 * render node subclasses are created, while for others, a generic
 * BaseRenderNode is returned. If the type is not supported, an
 * std::invalid_argument exception is thrown.
 *
 * @param type The OH_Native_Drawing_Type specifying the type of render node to
 * create.
 * @return std::unique_ptr<BaseRenderNode> A unique pointer to the created
 * render node.
 * @throws std::invalid_argument If the provided type is unsupported.
 */
OH_ALWAYS_INLINE std::unique_ptr<BaseRenderNode> createDrawingRenderNodeFromType(OH_Native_Drawing_Type type) {
    switch (type) {
    case OH_Native_Drawing_Type::DrawingTypeLine:
        return std::make_unique<LineRenderNode>();
    case OH_Native_Drawing_Type::DrawingTypeShaderLine:
        return std::make_unique<LineGradientRenderNode>();
    case OH_Native_Drawing_Type::DrawingTypeShaderRect:
        return std::make_unique<RectGradientRenderNode>();
    case OH_Native_Drawing_Type::DrawingTypeOval:
        return std::make_unique<OvalRenderNode>();
    case OH_Native_Drawing_Type::DrawingTypeArc:
        return std::make_unique<ArcRenderNode>();
    case OH_Native_Drawing_Type::DrawingTypePath:
        return std::make_unique<PathRenderNode>();
    case OH_Native_Drawing_Type::DrawingTypeImageRect:
    case OH_Native_Drawing_Type::DrawingTypeImageData:
    case OH_Native_Drawing_Type::DrawingTypeImage:
        return std::make_unique<ImageDisplayRenderNode>();
    case OH_Native_Drawing_Type::DrawingTypePoints:
    case OH_Native_Drawing_Type::DrawingTypeShaderPoints:
        return std::make_unique<PointsRenderNode>();
    case OH_Native_Drawing_Type::DrawingTypeClip:
        return std::make_unique<ClipRenderNode>();
    default:
        LOGI("OH_Native_Drawing_Type::%{public}d: create BaseRenderNode", type);
        return std::make_unique<BaseRenderNode>();
    }
}

constexpr static const uint64_t OHInitialHash = 0x811c9dc5;
constexpr static const int OHReverseNumber = 30;

inline uint64_t XXH64(const void *data, size_t len, uint64_t seed) {
    const auto *bytes = static_cast<const uint8_t *>(data);
    uint64_t hash = seed;

    for (size_t i = 0; i < len; ++i) {
        hash = (hash * 0x9E3779B185EBCA87ULL) ^ bytes[i];
    }

    return hash;
}

inline uint64_t RenderNodeSaveStateHash(const RenderNodeSaveState &saveState) {
    return XXH64(&saveState, sizeof(RenderNodeSaveState), 0);
}

struct PictureRecorderUpdateInfo {
    bool isDirty;
    uint64_t itemHash;
    OH_Native_Drawing_Type drawingType;
    RenderNodeSaveState saveState;
};

class PictureRecorder {
public:
    PictureRecorder() noexcept;
    ~PictureRecorder();

    void startRecording(BaseRenderNode &rootRenderNode);
    void finishRecording(BaseRenderNode &rootRenderNode);

    OH_ALWAYS_INLINE void save() {
        pushSaveStack(OH_RenderNode_SaveState_MakeType::Save);
    }

    OH_ALWAYS_INLINE void restore() {
        if (saveStack.size() >= 2) {
            popClip();
            if (saveStack.size() >= 2) {
                saveStack.pop_back();
            }
        }
    }

    OH_ALWAYS_INLINE void translate(float dx, float dy) {
        RenderNodeSaveState &currentState = topState();
        currentState.translateX += dx;
        currentState.translateY += dy;
    }

    OH_ALWAYS_INLINE void scale(float sx, float sy) {
        RenderNodeSaveState &currentState = topState();
        currentState.transform.scale(sx, sy, 1.0f);
    }
    OH_ALWAYS_INLINE void rotate(float degrees) {
        RenderNodeSaveState &currentState = topState();
        currentState.transform.rotate(degrees);
    }

    OH_ALWAYS_INLINE void skew(float sx, float sy) {
        RenderNodeSaveState &currentState = topState();
        currentState.transform.skew(sx, sy);
    }

    OH_ALWAYS_INLINE void concat(const float *matrix16) {
        RenderNodeSaveState &currentState = topState();
        Transform3D otherMatrix(matrix16);
        currentState.transform *= otherMatrix;
    }

    OH_ALWAYS_INLINE BaseRenderNode *getOrCreateRenderNodeForDrawing(const OH_Native_Drawing_Type type,
                                                                     const uint64_t itemHash) {
        OH::SystraceSection trace("PictureRecorder:getOrCreateRenderNodeForDrawing");
        initPropsIfNeeded();

        if (type == OH_Native_Drawing_Type::DrawingTypeClip) {
            return getOrCreateClipRenderNode(itemHash);
        }

        if (auto *cachedNode = props->findRenderNode(itemHash)) {
            LOGI("[PV] getOrCreateRenderNodeForDrawing from cache type:%{public}d itemHash:%{public}llu renderNode:%{public}p", type, itemHash, cachedNode);
            return cachedNode;
        }

        auto *renderNode = props->createAndAddRenderNode(type, itemHash);
        LOGI("[PV] getOrCreateRenderNodeForDrawing from no cache type:%{public}d itemHash:%{public}llu renderNode:%{public}p", type, itemHash, renderNode);
        return renderNode;
    }

    OH_ALWAYS_INLINE PictureRecorderUpdateInfo drawRenderNode(BaseRenderNode *renderNode,
                                                              OH_DrawingNode_Type renderNodeType) {
        OH::SystraceSection trace("PictureRecorder:drawRenderNode");
        initPropsIfNeeded();
        const OH_Native_Drawing_Type drawingType = (renderNodeType == OH_DrawingNode_Type::ParagraphNode) ? OH_Native_Drawing_Type::DrawingTypeDrawTextLayer : OH_Native_Drawing_Type::DrawingTypeDrawLayer;

        const uint64_t renderNodeUniqueHash = renderNode->getHash();

        currentDrawHash = hashMerge(currentDrawHash, renderNodeUniqueHash);

        props->currentDrawingItems.emplace_back(DrawingItem{
            .itemHash = renderNodeUniqueHash, .contentsHash = renderNodeUniqueHash, .drawingType = drawingType});

        // 将已有的BaseRenderNode添加到缓存池
        props->renderNodePool[renderNodeUniqueHash] = renderNode;
        renderNode->setHostingHash(rootRenderNodeHash);
        const RenderNodeSaveState &saveState = topState();

        return PictureRecorderUpdateInfo{
            .isDirty = true, .itemHash = renderNodeUniqueHash, .drawingType = drawingType, .saveState = saveState};
    }

    OH_ALWAYS_INLINE PictureRecorderUpdateInfo clip(const uint64_t drawingContentHash) {
        OH::SystraceSection trace("PictureRecorder:clip");
        PictureRecorderUpdateInfo updateItem = draw(OH_Native_Drawing_Type::DrawingTypeClip, drawingContentHash);
        pushClip();
        LOGI("PictureRecorder::clip 的 itemHash: =%{public}ld", updateItem.itemHash);
        return updateItem;
    }

    PictureRecorderUpdateInfo draw(OH_Native_Drawing_Type drawingType, uint64_t drawingContentHash);

    void prepareForReuse();

private:
    struct PictureRecorderProps {
        PictureRecorderProps() noexcept;

        std::vector<DrawingItem> currentDrawingItems;
        std::vector<DrawingItem> finalDrawingItems;

        std::unordered_map<uint64_t, BaseRenderNode *> renderNodePool;
        std::unordered_map<uint64_t, BaseRenderNode *> clipPool;

        std::vector<std::unique_ptr<BaseRenderNode>> ownedNodes;

        void prepareForReuse();

        void eraseFromOwnedNodes(BaseRenderNode *node) {
            auto ownedIt = std::find_if(
                ownedNodes.begin(),
                ownedNodes.end(),
                [node](const std::unique_ptr<BaseRenderNode> &ptr) {
                    return ptr.get() == node;
                });

            if (ownedIt != ownedNodes.end()) {
                // 在 erase 之前，先清理所有子节点的 parent 指针
                // 防止子节点后续调用 removeFromParent() 时访问已 dispose 的父节点
                node->clearChildren();
                ownedNodes.erase(ownedIt);
            }
        }

        BaseRenderNode *findRenderNode(const uint64_t hash) {
            const auto it = renderNodePool.find(hash);
            return it != renderNodePool.end() ? it->second : nullptr;
        }

        BaseRenderNode *findClipNode(const uint64_t hash) {
            const auto it = clipPool.find(hash);
            return it != clipPool.end() ? it->second : nullptr;
        }

        BaseRenderNode *createAndAddRenderNode(const OH_Native_Drawing_Type type, const uint64_t hash) {
            auto node = createDrawingRenderNodeFromType(type);
            BaseRenderNode *ptr = node.get();
            renderNodePool[hash] = ptr;
            ownedNodes.push_back(std::move(node));
            return ptr;
        }

        BaseRenderNode *createAndAddClipNode(uint64_t hash) {
            auto node = std::make_unique<ClipRenderNode>();
            BaseRenderNode *ptr = node.get();
            clipPool[hash] = ptr;
            ownedNodes.push_back(std::move(node));
            return ptr;
        }

    private:
        std::vector<BaseRenderNode *> newSubRenderNode;
    };

    struct SequenceTypeItem {
        uint64_t itemIndex = 0;
        std::array<uint64_t, OHReverseNumber> itemHashArray;
        std::unordered_map<uint64_t, uint64_t> itemHashMap;
    };

    struct SequenceIdInfo {
        uint64_t itemIndex = 0;
        bool isDirty = true;
    };

    static constexpr auto baseTable = []() constexpr {
        std::array<uint64_t, OH_Native_Drawing_Type_Count> table{};
        for (size_t i = 0; i < OH_Native_Drawing_Type_Count; ++i) {
            table[i] = 1ULL << (i + 29);
        }
        return table;
    }();

    std::unique_ptr<PictureRecorderProps> props = nullptr;
    std::array<SequenceTypeItem, OH_Native_Drawing_Type_Count> sequenceTable;

    uint64_t finishDrawHash = OHInitialHash;
    uint64_t currentDrawHash = OHInitialHash;

    std::vector<RenderNodeSaveState> saveStack;

    bool isFirstRender = true;
    uintptr_t rootRenderNodeHash = 0;
    int clipCountDuringOnceOperation = 0;

    void initPropsIfNeeded();
    constexpr void resetSequenceTableIndex();
    constexpr void resetSequenceTable();

    OH_ALWAYS_INLINE BaseRenderNode *getOrCreateClipRenderNode(const uint64_t itemHash) {
        initPropsIfNeeded();

        if (auto *cachedNode = props->findClipNode(itemHash)) {
            LOGI("[PV] getOrCreateClipRenderNode:%{public}p fromCache itemHash:%{public}llu", cachedNode, itemHash);
            return cachedNode;
        }

        auto *clipRenderNode = props->createAndAddClipNode(itemHash);
        LOGI("[PV] getOrCreateClipRenderNode create clipRenderNode:%{public}p itemHash:%{public}llu", clipRenderNode, itemHash);
        return clipRenderNode;
    }

    SequenceIdInfo allocSequenceIdInfo(OH_Native_Drawing_Type type, uint64_t currentContentsHash);
    void detachRenderNode(BaseRenderNode &rootRenderNode, OH_Native_Drawing_Type drawingType, uint64_t itemHash) const;

    void prepareForNextRecording(BaseRenderNode &rootRenderNode);
    void rebuildRenderNodeHierarchy(BaseRenderNode &rootRenderNode);
    void diffDrawingItems(BaseRenderNode &rootRenderNode);
    RenderNodeSaveState &topState();
    void pushSaveStack(OH_RenderNode_SaveState_MakeType type);

    void pushClip();
    void popClip();
    void resetDrawingItemContentsHash(OH_Native_Drawing_Type type, uint64_t itemHash);
};
} // namespace OH
#endif // OH_NATIVE_PICTURE_RECORDER_H