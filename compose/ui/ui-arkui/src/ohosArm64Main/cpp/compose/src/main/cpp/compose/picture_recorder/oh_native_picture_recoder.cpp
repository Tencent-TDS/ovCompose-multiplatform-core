#include <array>
#include <cassert>
#include <memory>
#include <unordered_map>
#include <vector>
#include "oh_native_picture_recorder.h"
#include "oh_native_picture_recorder_diff.h"
#include "../trace/oh_systrace_section.h"

namespace OH {
PictureRecorder::PictureRecorderProps::PictureRecorderProps() noexcept {
    currentDrawingItems.reserve(OHReverseNumber);
    finalDrawingItems.reserve(OHReverseNumber);
    renderNodePool.reserve(OHReverseNumber);
    clipPool.reserve(OHReverseNumber);
    ownedNodes.reserve(OHReverseNumber * 2);
}

void PictureRecorder::PictureRecorderProps::prepareForReuse() {
    LOGI("[PV] this:%{public}p prepareForReuse currentDrawingItems.clear finalDrawingItems.clear", this);
    renderNodePool.clear();
    clipPool.clear();
    currentDrawingItems.clear();
    finalDrawingItems.clear();
    newSubRenderNode.clear();
    ownedNodes.clear();
}

PictureRecorder::PictureRecorder() noexcept {
    saveStack.reserve(4);
    saveStack.emplace_back(RenderNodeSaveStateCreateSafeGuard());
    resetSequenceTableIndex();
}

PictureRecorder::~PictureRecorder() = default;

void PictureRecorder::startRecording(BaseRenderNode &rootRenderNode) {
    OH::SystraceSection trace("PictureRecorder:startRecording");
    clipCountDuringOnceOperation = 0;
    rootRenderNodeHash = rootRenderNode.getHash();
    resetSequenceTableIndex();
    if (!isFirstRender) {
        prepareForNextRecording(rootRenderNode);
    }
}

void PictureRecorder::finishRecording(BaseRenderNode &rootRenderNode) {
    OH::SystraceSection trace("PictureRecorder:finishRecording");
    if (!props) {
        return;
    }

    if (currentDrawHash == finishDrawHash) {
        LOGI("[PV] renderNode:%{public}p finishRecording 差异为0", &rootRenderNode);
        return;
    }

    if (isFirstRender) {
        isFirstRender = false;
        LOGI("[PV] renderNode:%{public}p finishRecording 首次提交命令，直接 rebuildRenderNodeHierarchy",
             &rootRenderNode);
        rebuildRenderNodeHierarchy(rootRenderNode);
    } else {
        LOGI("[PV] renderNode:%{public}p finishRecording 非首次提交命令 且存在差异开始", &rootRenderNode);
        diffDrawingItems(rootRenderNode);
    }
}

void PictureRecorder::initPropsIfNeeded() {
    if (!props) {
        props = std::make_unique<PictureRecorderProps>();
    }
}

constexpr void PictureRecorder::resetSequenceTableIndex() {
    for (size_t i = 0; i < OH_Native_Drawing_Type_Count; ++i) {
        sequenceTable[i].itemIndex = baseTable[i];
    }
}

constexpr void PictureRecorder::resetSequenceTable() {
    for (size_t i = 0; i < OH_Native_Drawing_Type_Count; ++i) {
        SequenceTypeItem &typeItem = sequenceTable[i];
        typeItem.itemIndex = baseTable[i];
        typeItem.itemHashArray.fill(0);
        typeItem.itemHashMap.clear();
    }
}

OH_ALWAYS_INLINE void PictureRecorder::resetDrawingItemContentsHash(OH_Native_Drawing_Type type, uint64_t itemHash) {
    assert(type != OH_Native_Drawing_Type::DrawingTypePop && type != OH_Native_Drawing_Type::DrawingTypeDrawLayer);

    const size_t typeIdx = static_cast<size_t>(type);
    SequenceTypeItem &info = sequenceTable[typeIdx];
    const uint64_t base = baseTable[typeIdx];
    const uint64_t maxOffset = OHReverseNumber - 1;

    if (itemHash - base <= maxOffset) {
        info.itemHashArray[itemHash - base] = 0;
    } else {
        info.itemHashMap[itemHash] = 0;
    }
}

PictureRecorder::SequenceIdInfo PictureRecorder::allocSequenceIdInfo(const OH_Native_Drawing_Type type,
                                                                     const uint64_t currentContentsHash) {
    const auto typeIdx = static_cast<size_t>(type);
    SequenceTypeItem &info = sequenceTable[typeIdx];
    const uint64_t itemIndex = info.itemIndex;
    info.itemIndex++;
    uint64_t previousHash = 0;

    const uint64_t base = baseTable[typeIdx];
    const uint64_t position = itemIndex - base;

    if (position < OHReverseNumber - 1) {
        previousHash = info.itemHashArray[position];
        info.itemHashArray[position] = currentContentsHash;
    } else {
        if (auto it = info.itemHashMap.find(itemIndex); it != info.itemHashMap.end()) {
            previousHash = it->second;
        }
        info.itemHashMap[itemIndex] = currentContentsHash;
    }

    return SequenceIdInfo{
        .itemIndex = itemIndex,
        .isDirty = previousHash != currentContentsHash,
    };
}

RenderNodeSaveState &PictureRecorder::topState() {
    return saveStack[saveStack.size() - 1];
}

void PictureRecorder::pushSaveStack(OH_RenderNode_SaveState_MakeType type) {
    const RenderNodeSaveState &currentState = topState();
    saveStack.emplace_back(RenderNodeSaveState{
        .transform = currentState.transform,
        .translateX = currentState.translateX,
        .translateY = currentState.translateY,
        .clipCount = currentState.clipCount,
        .makeType = type,
    });
}

void PictureRecorder::pushClip() {
    pushSaveStack(OH_RenderNode_SaveState_MakeType::Clip);
    clipCountDuringOnceOperation += 1;
    auto &currentDrawingItems = props->currentDrawingItems;
    DrawingItem &drawingItem = currentDrawingItems[currentDrawingItems.size() - 1];
    drawingItem.clipIndex = clipCountDuringOnceOperation;
    RenderNodeSaveState &saveState = topState();
    saveState.transform = Transform3DIdentity;
    saveState.clipCount += 1;
}

void PictureRecorder::popClip() {
    int popCount = 0;
    for (int i = static_cast<int>(saveStack.size()) - 1; i >= 0; i--) {
        const RenderNodeSaveState &saveStateItem = saveStack[i];
        if (saveStateItem.makeType == OH_RenderNode_SaveState_MakeType::Clip) {
            popCount += 1;
        } else {
            break;
        }
    }

    if (popCount > 0) {
        clipCountDuringOnceOperation -= popCount;
        while (popCount > 0) {
            popCount -= 1;
            saveStack.pop_back();
        }

        if (props) {
            props->currentDrawingItems.emplace_back(DrawingItem::DrawingPopItem);
        }
    }
}

void PictureRecorder::clearClip() {
    OH::SystraceSection trace("PictureRecorder:clearClip");

    // 如果栈顶是 Clip 状态，移除它
    if (saveStack.size() >= 2) {
        RenderNodeSaveState &topState = saveStack[saveStack.size() - 1];
        if (topState.makeType == OH_RenderNode_SaveState_MakeType::Clip) {
            // 移除最顶层的 clip 状态
            saveStack.pop_back();
            clipCountDuringOnceOperation -= 1;

            // 更新当前状态的 clipCount
            if (!saveStack.empty()) {
                RenderNodeSaveState &currentState = saveStack[saveStack.size() - 1];
                currentState.clipCount = (currentState.clipCount > 0) ? (currentState.clipCount - 1) : 0;
            }

            LOGI("PictureRecorder::clearClip: removed top clip state, remaining clipCount=%{public}d",
                 clipCountDuringOnceOperation);
        } else {
            // 如果栈顶不是 Clip，尝试从当前状态清除 clipCount
            if (!saveStack.empty()) {
                RenderNodeSaveState &currentState = saveStack[saveStack.size() - 1];
                if (currentState.clipCount > 0) {
                    currentState.clipCount = 0;
                    LOGI("PictureRecorder::clearClip: cleared clipCount from current state");
                }
            }
        }
    }
}

PictureRecorderUpdateInfo PictureRecorder::saveLayer(const uint64_t drawingContentHash) {
    OH::SystraceSection trace("PictureRecorder:saveLayer");
    PictureRecorderUpdateInfo updateItem = draw(OH_Native_Drawing_Type::DrawingTypeSaveLayer, drawingContentHash);

    // 将 SaveLayer 状态压入 saveStack
    pushSaveStack(OH_RenderNode_SaveState_MakeType::SaveLayer);

    LOGI("PictureRecorder::saveLayer: itemHash=%{public}lu", updateItem.itemHash);
    return updateItem;
}

PictureRecorderUpdateInfo PictureRecorder::draw(OH_Native_Drawing_Type drawingType, uint64_t drawingContentHash) {
    OH::SystraceSection trace("PictureRecorder:draw");
    initPropsIfNeeded();

    const RenderNodeSaveState &saveState = topState();
    const uint64_t saveStateHash = RenderNodeSaveStateHash(saveState);
    const uint64_t finialDrawingContentHash = hashMerge(drawingContentHash, saveStateHash);

    const SequenceIdInfo &drawingItemSequenceId = allocSequenceIdInfo(drawingType, finialDrawingContentHash);
    const uint64_t drawingItemHash = drawingItemSequenceId.itemIndex;

    props->currentDrawingItems.emplace_back(
        DrawingItem{.itemHash = drawingItemHash, .contentsHash = finialDrawingContentHash, .drawingType = drawingType});

    currentDrawHash = hashMerge(currentDrawHash, drawingItemHash);
    return PictureRecorderUpdateInfo{drawingItemSequenceId.isDirty, drawingItemHash, drawingType, saveState};
}

void PictureRecorder::prepareForNextRecording(BaseRenderNode &rootRenderNode) {
    OH::SystraceSection trace("PictureRecorder:prepareForNextRecording");
    props->finalDrawingItems.swap(props->currentDrawingItems);
    props->currentDrawingItems.clear();

    finishDrawHash = currentDrawHash;
    currentDrawHash = OHInitialHash;

    saveStack.clear();
    saveStack.emplace_back(RenderNodeSaveStateCreateSafeGuard());
    LOGI("[PV] renderNode:%{public}p did prepareForNextRecording", &rootRenderNode);
}

void PictureRecorder::rebuildRenderNodeHierarchy(BaseRenderNode &rootRenderNode) {
    OH::SystraceSection trace("PictureRecorder:rebuildRenderNodeHierarchy");
    const std::vector<DrawingItem> &finialDrawingItems = props->currentDrawingItems;
    const size_t size = finialDrawingItems.size();

    std::vector<BaseRenderNode *> stack;
    stack.emplace_back(&rootRenderNode);

    LOGI("[PV] renderNode:%{public}p 开始处理视图层级 size:%{public}zu", &rootRenderNode, size);
    for (size_t i = 0; i < size; i++) {
        const DrawingItem &drawingItem = finialDrawingItems[i];
        const OH_Native_Drawing_Type drawingType = drawingItem.drawingType;
        LOGI("[PV] renderNode:%{public}p loop i=%{public}zu drawingItem hash:%{public}llu", &rootRenderNode, i,
             drawingItem.itemHash);
        switch (drawingType) {
        case OH_Native_Drawing_Type::DrawingTypeSave:
            break;
        case OH_Native_Drawing_Type::DrawingTypeClip: {
            auto *clipRenderNode = getOrCreateClipRenderNode(drawingItem.itemHash);
            clipRenderNode->setHostingHash(static_cast<uint32_t>(rootRenderNodeHash));
            if (drawingItem.clipIndex == 1) {
                rootRenderNode.addChild(clipRenderNode);
                LOGI("[PV] renderNode:%{public}p addChild clipRenderNode:%{public}p clipIndex:%{public}d",
                     &rootRenderNode, clipRenderNode, drawingItem.clipIndex);
            } else {
                auto *parentRenderNode = stack[stack.size() - 1];
                parentRenderNode->addChild(clipRenderNode);
                LOGI("[PV] renderNode:%{public}p addChild clipRenderNode:%{public}p clipIndex:%{public}d",
                     parentRenderNode, clipRenderNode, drawingItem.clipIndex);
            }
            stack.emplace_back(clipRenderNode);
            break;
        }
        case OH_Native_Drawing_Type::DrawingTypePop: {
            stack.pop_back();
            break;
        }
        default: {
            auto *parentRenderNode = stack[stack.size() - 1];
            auto *drawingRenderNode = getOrCreateRenderNodeForDrawing(drawingType, drawingItem.itemHash);
            drawingRenderNode->setHostingHash(static_cast<uint32_t>(rootRenderNodeHash));
            parentRenderNode->addChild(drawingRenderNode);
            LOGI("[PV] parentNode: %{public}p addChild: %{public}p "
                 "drawingType: %{public}d, "
                 "drawingItem.itemHash: %{public}llu",
                 parentRenderNode, drawingRenderNode, drawingType, drawingItem.itemHash);
            break;
        }
        }
    }
    LOGI("[PV] renderNode:%{public}p 结束处理视图层级", &rootRenderNode);
}

void PictureRecorder::detachRenderNode(BaseRenderNode &rootRenderNode, const OH_Native_Drawing_Type drawingType,
                                       const uint64_t itemHash) const {
    switch (drawingType) {
    case OH_Native_Drawing_Type::DrawingTypeClip: {
        auto it = props->clipPool.find(itemHash);
        if (it != props->clipPool.end()) {
            auto *willBeDeleteClipRenderNode = it->second;
            props->clipPool.erase(itemHash);
            LOGI("[PV] renderNode:%{public}p clipPool remove clipNode begin:%{public}p, itemHash%{public}lu",
                 &rootRenderNode, willBeDeleteClipRenderNode, itemHash);
            willBeDeleteClipRenderNode->removeFromParent();
            props->eraseFromOwnedNodes(willBeDeleteClipRenderNode);
            LOGI("[PV] renderNode:%{public}p clipPool remove clipNode finish:%{public}p, itemHash%{public}lu",
                 &rootRenderNode, willBeDeleteClipRenderNode, itemHash);
        } else {
            LOGI("[PV] renderNode:%{public}p clipPool remove failed, itemHash%{public}lu", &rootRenderNode, itemHash);
        }
        break;
    }
    default: {
        auto iterator = props->renderNodePool.find(itemHash);
        if (iterator != props->renderNodePool.end()) {
            auto *willBeDeleteRenderNode = iterator->second;
            LOGI("[PV] renderNode:%{public}p renderNodePool remove renderNode begin:%{public}p, itemHash%{public}lu",
                 &rootRenderNode, willBeDeleteRenderNode, itemHash);
            props->renderNodePool.erase(iterator);
            willBeDeleteRenderNode->removeFromParent();
            props->eraseFromOwnedNodes(willBeDeleteRenderNode);
            LOGI("[PV] renderNode:%{public}p renderNodePool remove renderNode finish:%{public}p, itemHash%{public}lu",
                 &rootRenderNode, willBeDeleteRenderNode, itemHash);
        }
        break;
    }
    }
}

void PictureRecorder::diffDrawingItems(BaseRenderNode &rootRenderNode) {
    OH::SystraceSection trace("PictureRecorder:diffDrawingItems");
    const std::vector<DrawingItem> &oldArray = props->finalDrawingItems;
    const std::vector<DrawingItem> &newArray = props->currentDrawingItems;

    const size_t newSize = newArray.size();
    const size_t oldSize = oldArray.size();

    if (oldSize == 0 && newSize > 0) {
        LOGI("[PV] renderNode:%{public}p 纯新增 newSize:%{public}zu", &rootRenderNode, newSize);
        rebuildRenderNodeHierarchy(rootRenderNode);
        return;
    }

    if (oldSize > 0 && newSize == 0) {
        LOGI("[PV] renderNode:%{public}p 纯删除", &rootRenderNode);
        for (size_t i = 0; i < oldSize; i++) {
            const DrawingItem &commandToBeDelete = oldArray[i];
            const OH_Native_Drawing_Type drawingType = commandToBeDelete.drawingType;
            const uint64_t itemHash = commandToBeDelete.itemHash;

            if (drawingType != OH_Native_Drawing_Type::DrawingTypeDrawLayer &&
                drawingType != OH_Native_Drawing_Type::DrawingTypeDrawTextLayer &&
                drawingType != OH_Native_Drawing_Type::DrawingTypePop) {
                resetDrawingItemContentsHash(drawingType, itemHash);
            }

            detachRenderNode(rootRenderNode, drawingType, itemHash);
        }
        resetSequenceTable();
        return;
    }

    const DiffResult diffResult = diffDrawCommands(oldArray, newArray);

    const int deleteSize = diffResult.deletsItems.size();
    const int insertSize = diffResult.insertItems.size();

    bool shouldRebuildRenderNodeHierarchy = insertSize > 0 || diffResult.movedItems.size() > 0;

    LOGI("[PV] renderNode:%{public}p diff 结果 deleteSize:%{public}d insertSize:%{public}d movedSize:%{public}zu",
         &rootRenderNode, deleteSize, insertSize, diffResult.movedItems.size());
    for (size_t i = 0; i < deleteSize; i++) {
        auto removeIndex = diffResult.deletsItems[i];
        const DrawingItem &commandToBeDelete = oldArray[removeIndex];
        auto willBeDeleteDrawingType = commandToBeDelete.drawingType;
        const uint64_t itemHash = commandToBeDelete.itemHash;

        if (willBeDeleteDrawingType != OH_Native_Drawing_Type::DrawingTypeDrawLayer &&
            willBeDeleteDrawingType != OH_Native_Drawing_Type::DrawingTypeDrawTextLayer &&
            willBeDeleteDrawingType != OH_Native_Drawing_Type::DrawingTypePop) {
            resetDrawingItemContentsHash(willBeDeleteDrawingType, itemHash);
        }

        detachRenderNode(rootRenderNode, willBeDeleteDrawingType, itemHash);
        shouldRebuildRenderNodeHierarchy = shouldRebuildRenderNodeHierarchy ||
                                           (commandToBeDelete.drawingType == OH_Native_Drawing_Type::DrawingTypeClip);
        LOGI("[PV] diffDrawingItems insert: %{public}d, moveSize: %{public}d, clip: %{public}d ", insertSize,
             diffResult.movedItems.size(), commandToBeDelete.drawingType == OH_Native_Drawing_Type::DrawingTypeClip);
    }

    LOGI("[PV] renderNode:%{public}p 完成 diff 差异 apply shouldRebuildRenderNodeHierarchy:%{public}d", &rootRenderNode,
         shouldRebuildRenderNodeHierarchy ? 1 : 0);
    if (shouldRebuildRenderNodeHierarchy) {
        rebuildRenderNodeHierarchy(rootRenderNode);
    }
}

void PictureRecorder::prepareForReuse() {
    if (props) {
        props->prepareForReuse();
        resetSequenceTable();
    }

    saveStack.clear();
    saveStack.emplace_back(RenderNodeSaveStateCreateSafeGuard());
    isFirstRender = true;
}
} // namespace OH