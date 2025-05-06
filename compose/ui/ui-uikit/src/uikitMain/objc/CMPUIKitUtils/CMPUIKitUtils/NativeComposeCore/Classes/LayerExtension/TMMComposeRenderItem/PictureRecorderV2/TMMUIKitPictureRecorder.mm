/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#import "TMMUIKitPictureRecorder.h"
#import "TMMPictureRecorderDiffer.h"
#import "CALayer+TMMPictureRecorder.h"
#import "TMMNativeComposeGradientLayer.h"
#import "TMMNativeRoundRectLayer.h"
#import "TVComposePointLayer.h"
#import "TMMNativeBaseLayer.h"
#import "TMMNativeLineLayer.h"
#import "TMMNativeCircleLayer.h"
#import "TMMNativeArcLayer.h"
#import "TMMAsyncTaskLayer.h"
#import "TMMNativeClipLayer.h"
#import "TMMNativeLineGradientLayer.h"
#import "TMMImageDisplayLayer.h"
#import "TMMXXHashFuncs.h"
#import "TMMDrawUtils.h"
#import "TMMNativeClipLayer.h"
#import "UIViewUtils.h"

#define NSLog(...)

namespace TMM {

constexpr size_t TMMNativeDrawingTypeCount = 30;

constexpr uint64_t calculateBase(size_t n) noexcept {
    return 1ULL << (n + 29); // 2^(n+29)
}

static TMM_ALWAYS_INLINE uint64_t CALayerSaveStateHash(const CALayerSaveState &saveState) {
    return XXH64(&saveState, sizeof(CALayerSaveState), 0);
}

TMM_ALWAYS_INLINE Class TMMDrawingLayerClassFromTypeV2(TMMNativeDrawingType type) {
    switch (type) {
        case TMMNativeDrawingTypeShaderRect:
        case TMMNativeDrawingTypeShaderCircle:
        case TMMNativeDrawingTypeShaderPath:
            return [TMMNativeComposeGradientLayer class];
        case TMMNativeDrawingTypeLine:
            return [TMMNativeLineLayer class];
        case TMMNativeDrawingTypeShaderLine:
            return [TMMNativeLineGradientLayer class];
        case TMMNativeDrawingTypeCircle:
            return [TMMNativeCircleLayer class];
        case TMMNativeDrawingTypePath:
            return [TMMNativeRoundRectLayer class];
        case TMMNativeDrawingTypePoints:
            return [TVComposePointLayer class];
        case TMMNativeDrawingTypeArc:
            return [TMMNativeArcLayer class];
        case TMMNativeDrawingTypeImageRect:
            return [TMMImageDisplayLayer class];
        case TMMNativeDrawingTypeClip:
            return [TMMNativeClipLayer class];
        case TMMNativeDrawingTypeImageData:
            return [TMMAsyncTaskLayer class];
        default:
            return [TMMNativeBaseLayer class];
    }
}

#pragma mark - PictureRecorderProps
PictureRecorder::PictureRecorderProps::PictureRecorderProps() noexcept {
    currentDrawingItems.reserve(TMMReverseNumber);
    finialDrawingItems.reserve(TMMReverseNumber);
    layerPool.reserve(TMMReverseNumber);
}

void PictureRecorder::PictureRecorderProps::prepareForReuse() {
    NSLog(@"[PV2] this:%p prepareForReuse currentDrawingItems.clear finialDrawingItems.clear", this);
    currentDrawingItems.clear();
    finialDrawingItems.clear();
    layerPool.clear();
    clipPool.clear();
}

#pragma mark - PictureRecorder
const float PictureRecorder::density = TMMComposeCoreDeviceDensity();

PictureRecorder::PictureRecorder() noexcept {
    saveStack.reserve(4);
    saveStack.emplace_back(CALayerSaveStateCreateSafeGuard());
}

void PictureRecorder::initPropsIfNeeded() {
    if (!props) {
        props = new PictureRecorderProps();
    }
}

constexpr void PictureRecorder::resetSequenceTableIndex() {
    // 重置 index
    for (size_t i = 0; i < TMMNativeDrawingTypeCount; ++i) {
        sequenceTable[i].itemIndex = calculateBase(i);
    }
}

constexpr void PictureRecorder::resetSequenceTable() {
    // 清空 sequenceTable
    for (size_t i = 0; i < TMMNativeDrawingTypeCount; ++i) {
        SequenceTypeItem &typeItem = sequenceTable[i];
        typeItem.itemIndex = calculateBase(i);
        typeItem.itemHashArray.fill(0);
        typeItem.itemHashMap.clear();
    }
}

void PictureRecorder::resetDrawingItemContentsHash(TMMNativeDrawingType type, uint64_t itemHash) {
    NSCAssert((type != TMMNativeDrawingTypePop && type != TMMNativeDrawingTypeDrawLayer), @"reset type error");

    SequenceTypeItem &info = sequenceTable[type];
    NSInteger position = itemHash - calculateBase(type);
    if (position < TMMReverseNumber - 1) {
        info.itemHashArray[position] = 0;
    } else {
        info.itemHashMap[itemHash] = 0;
    }
}

PictureRecorder::SequenceIdInfo PictureRecorder::allocSequenceIdInfo(TMMNativeDrawingType type, uint64_t currentContentsHash) {
    SequenceTypeItem &info = sequenceTable[type];
    const uint64_t itemIndex = info.itemIndex;
    info.itemIndex++;
    uint64_t previousHash = 0;

    NSInteger position = itemIndex - calculateBase(type);
    // 如果分配数量少于 50 直接用下标取上一次的 contentsHash，速度远快于 std::unordered_map
    if (position < TMMReverseNumber - 1) {
        previousHash = info.itemHashArray[position];
        info.itemHashArray[position] = currentContentsHash;
    } else {
        previousHash = info.itemHashMap[itemIndex];
        if (previousHash != currentContentsHash) {
            info.itemHashMap[itemIndex] = currentContentsHash;
        }
    }

    return (SequenceIdInfo) {
        .itemIndex = itemIndex,
        .isDirty = previousHash != currentContentsHash,
    };
}

#pragma mark - Drawing API
void PictureRecorder::save() {
    pushSaveStack(CALayerSaveStateMakeTypeSave);
    /*
     save -> clip -> clip -> save -> restore
     clip -> save 这里 save 的时候 并没有 restore 掉 clip，所以仍然继续累加，这里不能 clipCountDuringOnceOperation = 0;
     */
}

void PictureRecorder::restore() {
    if (saveStack.size() >= 2) {
        popClip();
        if (saveStack.size() >= 2) {
            saveStack.pop_back();
        }
    }
}

void PictureRecorder::startRecording(CALayer *rootLayer) {
    clipCountDuringOnceOperation = 0;
    rootLayerHash = [rootLayer hash];
    resetSequenceTableIndex();
    if (!isFirstRender) {
        prepareForNextRecording(rootLayer);
    }
}

CALayerSaveState &PictureRecorder::topState() {
    return saveStack[saveStack.size() - 1];
}

void PictureRecorder::translate(float dx, float dy) {
    CALayerSaveState &currentState = topState();
    currentState.translateX += dx;
    currentState.translateY += dy;
}

void PictureRecorder::scale(float sx, float sy) {
    CALayerSaveState &currentState = topState();
    currentState.transform = CATransform3DScale(currentState.transform, sx, sy, 1);
}

void PictureRecorder::rotate(float degrees) {
    CALayerSaveState &currentState = topState();
    currentState.transform = CATransform3DRotate(currentState.transform, degrees * (M_PI / 180), 0, 0, 1);
}

void PictureRecorder::finishRecording(CALayer *rootLayer) {
    if (!props) {
        return;
    }

    if (currentDrawHash == finishDrawHash) {
        NSLog(@"[PV2] layer:%p finishRecording 差异为0", rootLayer);
        return;
    }

    if (isFirstRender) {
        isFirstRender = false;
        NSLog(@"[PV2] layer:%p finishRecording 首次提交命令，直接 rebuildLayerHierarchy", rootLayer);
        rebuildLayerHierarchy(rootLayer);
    } else {
        NSLog(@"[PV2] layer:%p finishRecording 非首次提交命令 且存在差异开始", rootLayer);
        diffDrawingItems(rootLayer);
    }
}

void PictureRecorder::prepareForNextRecording(CALayer *rootLayer) {
    MARK_PREPARE_FOR_NEXT_RECORDING_CALL;
    props->finialDrawingItems.swap(props->currentDrawingItems);
    props->currentDrawingItems.clear();

    finishDrawHash = currentDrawHash;
    currentDrawHash = TMMInitialHash;

    saveStack.clear();
    saveStack.emplace_back(CALayerSaveStateCreateSafeGuard());

    NSLog(@"[PV2] layer:%p did prepareForNextRecording", rootLayer);
}

PictureRecorderUpdateInfo PictureRecorder::clip(uint64_t drawingContentHash) {
    PictureRecorderUpdateInfo updateItem = draw(TMMNativeDrawingTypeClip, drawingContentHash);
    pushClip();

    NSLog(@"[PV2] clip 的 itemHash:%llu", updateItem.itemHash);
    return updateItem;
}

PictureRecorderUpdateInfo PictureRecorder::drawLayer(CALayer *layer) {
    // 1. 如有必要初始化 props
    initPropsIfNeeded();

    const TMMNativeDrawingType drawingType = TMMNativeDrawingTypeDrawLayer;
    const uint64_t drawLayerUniqueHash = [layer hash];

    // 2. drawLayer 的 currentDrawHash 需要有 layer 的 hash 参与运算
    currentDrawHash = hashMerge(currentDrawHash, drawLayerUniqueHash);

    // 3. drawLayer 的 itemHash 和 contentsHash 固定为同一个
    props->currentDrawingItems.emplace_back((DrawingItem) {
        .itemHash = drawLayerUniqueHash,
        .contentsHash = drawLayerUniqueHash,
        .drawingType = drawingType,
    });

    // 主动存该 layer 方便后面 createLayer 的时候，直接取
    props->layerPool[drawLayerUniqueHash] = layer;
    layer.tmmComposeHostingLayerHash = rootLayerHash;
    const CALayerSaveState &saveState = topState();

    return (PictureRecorderUpdateInfo) {
        .isDirty = true,
        .saveState = saveState,
        .drawingType = drawingType,
        .itemHash = drawLayerUniqueHash,
    };
}

void PictureRecorder::pushSaveStack(CALayerSaveStateMakeType type) {
    const CALayerSaveState &currentState = topState();
    saveStack.emplace_back((CALayerSaveState) {
        .transform = currentState.transform,
        .translateX = currentState.translateX,
        .translateY = currentState.translateY,
        .makeType = type,
    });
}

void PictureRecorder::pushClip() {
    pushSaveStack(CALayerSaveStateMakeTypeClip);
    clipCountDuringOnceOperation += 1;
    auto &currentDrawingItems = props->currentDrawingItems;
    DrawingItem &drawingItem = currentDrawingItems[currentDrawingItems.size() - 1];
    drawingItem.clipIndex = clipCountDuringOnceOperation;
    CALayerSaveState &saveState = topState();
    // transform 已经在 clipLayer 上应用了，会对所有的孩子去生效,因此这里需要复位
    saveState.transform = CATransform3DIdentity;
    saveState.clipCount += 1;
}

void PictureRecorder::popClip() {
    int popCount = 0;
    for (NSInteger i = saveStack.size() - 1; i >= 0; i--) {
        const CALayerSaveState &saveStateItem = saveStack[i];
        if (saveStateItem.makeType == CALayerSaveStateMakeTypeClip) {
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

        // 每一次 对应着 一个 popItem 帮助构建 UI 层级
        // 比如 save -> clip -> drawxxx -> restore -> drawxxx2 -> popItem
        // 只有在 restore 的后面新增一个 popItem 才能让 drawxxx2 的父层级是更上一层级
        if (props) {
            props->currentDrawingItems.emplace_back(DrawingItem::DrawingPopItem);
        }
    }
}

/// 每一次 drawCommon 主要做两件事
/// 1. 记录最新的绘制命令
/// 2. 计算增量 hash，避免进行 diff
/// 3. 如果绘制命令的参数变了，则单独进行更新
/// - Parameters:
///   - drawingType: TMMNativeDrawingType
///   - drawingContentHash: 每一次绘制命令的所有参数的 hash
PictureRecorderUpdateInfo PictureRecorder::draw(TMMNativeDrawingType drawingType, uint64_t drawingContentHash) {
    // 1. 如有必要初始化 props
    initPropsIfNeeded();

    // 2. 计算一次 recording 过程中的增量 hash，如果结果一样可以直接不用 diff，也不用创建任何 OC 对象
    const CALayerSaveState &saveState = topState();
    const uint64_t saveStateHash = CALayerSaveStateHash(saveState);
    const uint64_t finialDrawingContentHash = hashMerge(drawingContentHash, saveStateHash);

    // 3. 计算绘制命令的 hash，注意绘制命令的 hash 只和类型、还有当前类型下的 seqId 有关
    const SequenceIdInfo &drawingItemSequenceId = allocSequenceIdInfo(drawingType, finialDrawingContentHash);
    const uint64_t drawingItemHash = drawingItemSequenceId.itemIndex;
    props->currentDrawingItems.emplace_back((DrawingItem) {
        .itemHash = drawingItemHash,
        .contentsHash = finialDrawingContentHash,
        .drawingType = drawingType,
    });

    // 4. 重要⚠️⚠️⚠️：单次 diff 的准则应该是和 item 有关
    currentDrawHash = hashMerge(currentDrawHash, drawingItemHash);
    return (PictureRecorderUpdateInfo) {
        .isDirty = drawingItemSequenceId.isDirty,
        .saveState = saveState,
        .drawingType = drawingType,
        .itemHash = drawingItemHash,
    };
}

#pragma mark - layer logic

UIView *PictureRecorder::getOrCreateClipView(uint64_t itemHash) const {
    std::unordered_map<uint64_t, UIView *> &clipPool = props->clipPool;
    auto viewIt = clipPool.find(itemHash);
    if (viewIt != clipPool.end()) {
        NSLog(@"[PV2] getOrCreateClipView:%p fromCache itemHash:%llu", viewIt->second, itemHash);
        return viewIt->second;
    }
    TMMNativeClipView *clipView = [[TMMNativeClipView alloc] init];
    clipPool.emplace(itemHash, clipView);
    NSLog(@"[PV2] getOrCreateClipView create clipView:%p itemHash:%llu", clipView, itemHash);
    return clipView;
}

CALayer *PictureRecorder::getOrCreateLayerForDrawing(TMMNativeDrawingType type, uint64_t itemHash) const {
    if (type == TMMNativeDrawingTypeClip) {
        return getOrCreateClipView(itemHash).layer;
    }

    std::unordered_map<uint64_t, CALayer *> &layerPool = props->layerPool;
    auto layerIt = layerPool.find(itemHash);
    if (layerIt != layerPool.end()) {
        NSLog(@"[PV2] getOrCreateLayerForDrawing from cache type:%ld itemHash:%llu layer:%p", type, itemHash, layerIt->second);
        return layerIt->second;
    }

    Class layerClass = TMM::TMMDrawingLayerClassFromTypeV2(type);
    CALayer *layer = [[layerClass alloc] init];
    layerPool.emplace(itemHash, layer);
    NSLog(@"[PV2] getOrCreateLayerForDrawing from no cache type:%ld itemHash:%llu  layer:%p", type, itemHash, layer);
    NSCAssert(type != TMMNativeDrawingTypeDrawLayer, @"TMMNativeDrawingTypeDrawLayer 不应该调用此方法");
    return layer;
}

void PictureRecorder::rebuildLayerHierarchy(CALayer *rootLayer) {
    const std::vector<DrawingItem> &finialDrawingItems = props->currentDrawingItems;
    const NSInteger size = finialDrawingItems.size();

    std::vector<CALayer *> stack;
    stack.emplace_back((CALayer *)rootLayer);

    /*
     这里应该保持 UIView 的树状结构，并且应该在对 layer 进行重排之前添加
     虽然下面的 layer 重排也会 addSubview: 但是这里仍然不能删除，否则 WKWebview，依然渲染有问题，暂时不知道为啥...
     */
    NSLog(@"[PV2] layer:%p 开始处理视图层级 size:%@", rootLayer, @(size));
    for (NSInteger i = 0; i < size; i++) {
        const DrawingItem &drawingItem = finialDrawingItems[i];
        NSLog(@"[PV2] layer:%p loop i=%ld drawingItem hash:%llu", rootLayer, i, drawingItem.itemHash);
        const TMMNativeDrawingType drawingType = drawingItem.drawingType;
        switch (drawingType) {
            case TMMNativeDrawingTypeSave:
                break;
            case TMMNativeDrawingTypeClip: {
                UIView *clipView = getOrCreateClipView(drawingItem.itemHash);
                CALayer *clipLayer = clipView.layer;
                if (drawingItem.clipIndex == 1) {
                    // 一次 save 和 restore 之间，只有第一个 clipLayer 加入到 newSublayers 中
                    TMMCMPUIViewFastAddSubview((UIView *)rootLayer.delegate, clipView);
                    NSLog(@"[PV2] super:%p addSubview:<%p,layer:%p> clipIndex:%d", (UIView *)rootLayer.delegate, clipView, clipView.layer,
                          drawingItem.clipIndex);
                } else {
                    CALayer *parentLayer = stack[stack.size() - 1];
                    TMMCMPUIViewFastAddSubview((UIView *)parentLayer.delegate, clipView);
                    NSLog(@"[PV2] super:%p addSubview:<%p, layer:%p> clipIndex:%d", (UIView *)parentLayer.delegate, clipView, clipView.layer,
                          drawingItem.clipIndex);
                }
                stack.emplace_back(clipLayer);
                break;
            }
            case TMMNativeDrawingTypePop: {
                stack.pop_back();
                NSLog(@"[PV2] layer:%p 开始处理视图层级 pop clip", rootLayer);
                break;
            }
            default:
                CALayer *parentLayer = stack[stack.size() - 1];
                CALayer *drawingLayer = getOrCreateLayerForDrawing(drawingType, drawingItem.itemHash);
                UIView *drawingLayerView = (UIView *)drawingLayer.delegate;
                if ([drawingLayerView isKindOfClass:[UIView class]]) {
                    TMMCMPUIViewFastAddSubview((UIView *)parentLayer.delegate, drawingLayerView);
                    NSLog(@"[PV2] super:%p addSubview:<%p layer:%p>", (UIView *)parentLayer.delegate, drawingLayerView, drawingLayerView.layer);
                } else {
                    [parentLayer addSublayer:drawingLayer];
                    NSLog(@"[PV2] super:%p addSublayer:%p drawingItem.itemHash:%llu", (UIView *)parentLayer.delegate, drawingLayer,
                          drawingItem.itemHash);
                }
                break;
        }
    }
    NSLog(@"[PV2] layer:%p 结束处理视图层级", rootLayer);
}

static TMM_ALWAYS_INLINE void disposeLayer(CALayer *layerForDrawing, NSInteger rootLayerHash) {
    if (!layerForDrawing.superlayer) {
        // 此种情况是 AdaptiveCanvas 的 destory() 调用 removeFromSuperlayer
        return;
    }

    if (layerForDrawing.tmmComposeHostingLayerHash != rootLayerHash) {
        // 说明此时已经被添加其他的 layer 上了
        return;
    }

    if (layerForDrawing.delegate) {
        TMMCMPUIViewFastRemoveFromSuperview((UIView *)layerForDrawing.delegate);
    }
    [layerForDrawing removeFromSuperlayer];
}

static TMM_ALWAYS_INLINE void deatchLayer(CALayer *rootLayer, std::unordered_map<uint64_t, CALayer *> &layerPool,
                                          std::unordered_map<uint64_t, UIView *> &clipPool, TMMNativeDrawingType drawingType, uint64_t itemHash,
                                          NSInteger rootLayerHash) {
    switch (drawingType) {
        case TMMNativeDrawingTypeClip: {
            // clip 也是一个 view 直接 removeFromSuperView 否则会导致 crash
            auto it = clipPool.find(itemHash);
            if (it != clipPool.end()) {
                UIView *clipView = it->second;
                clipPool.erase(itemHash);
                TMMCMPUIViewFastRemoveFromSuperview(clipView);
                NSLog(@"[PV2] layer:%p clipPool remove clipView:%p, itemHash:%llu", rootLayer, clipView, itemHash);
            } else {
                NSLog(@"[PV2] layer:%p clipPool remove 发生错误 itemHash:%llu ", rootLayer, itemHash);
            }
            break;
        }
        case TMMNativeDrawingTypeDrawLayer: {
            auto iterator = layerPool.find(itemHash);
            if (iterator != layerPool.end()) {
                CALayer *layerWillBeDelete = iterator->second;
                layerPool.erase(iterator);
                // drawLayer 一定是存在 view 的，而且必须走 disposeLayer
                disposeLayer(layerWillBeDelete, rootLayerHash);
                NSLog(@"[PV2] layer:%p layerPool removeLayer:%p, itemHash:%llu", rootLayer, layerWillBeDelete, itemHash);
            } else {
                NSLog(@"[PV2] layer:%p layerPool remove 发生错误 itemHash:%llu ", rootLayer, itemHash);
            }
            break;
        }
        default: {
            auto iterator = layerPool.find(itemHash);
            if (iterator != layerPool.end()) {
                CALayer *layerWillBeDelete = iterator->second;
                layerPool.erase(iterator);
                [layerWillBeDelete removeFromSuperlayer];
                NSLog(@"[PV2] layer:%p layerPool removeLayer:%p, itemHash:%llu", rootLayer, layerWillBeDelete, itemHash);
            } else {
                NSLog(@"[PV2] layer:%p layerPool remove 发生错误 itemHash:%llu ", rootLayer, itemHash);
            }
            break;
        }
    }
}

void PictureRecorder::diffDrawingItems(CALayer *rootLayer) {
    MARK_DIFF_DRAW_COMMAND_CALL;
    const std::vector<DrawingItem> &oldArray = props->finialDrawingItems;
    const std::vector<DrawingItem> &newArray = props->currentDrawingItems;

    const NSInteger newSize = newArray.size();
    const NSInteger oldSize = oldArray.size();

    // case1: 纯新增
    if (oldSize == 0 && newSize > 0) {
        // 直接构建 UI 层级
        NSLog(@"[PV2] layer:%p 纯新增 newSize:%ld", rootLayer, newSize);
        rebuildLayerHierarchy(rootLayer);
        return;
    }

    std::unordered_map<uint64_t, CALayer *> &layerPool = props->layerPool;
    std::unordered_map<uint64_t, UIView *> &clipPool = props->clipPool;

    // case2: 纯删除
    if (oldSize > 0 && newSize == 0) {
        NSLog(@"[PV2] layer:%p 纯删除", rootLayer);
        // 1. 将 layer 从视图树上拔掉
        for (NSInteger i = 0; i < oldSize; i++) {
            const DrawingItem &commandToBeDelete = oldArray[i];
            const TMMNativeDrawingType drawingType = commandToBeDelete.drawingType;
            const uint64_t itemHash = commandToBeDelete.itemHash;
            deatchLayer(rootLayer, layerPool, clipPool, drawingType, itemHash, rootLayerHash);
        }
        // 2. 重置 sequenceTable
        resetSequenceTable();
        return;
    }

    // 3. 进行 diff
    const DiffResult diffResult = diffDrawCommands(oldArray, newArray);

    const NSInteger deleteSize = diffResult.deletsItems.size();
    const NSInteger insertSize = diffResult.insertItems.size();

    // 注意 这里只需要处理 delete，因为 update 和 insert 都在 draw 的时候，通过对比 hash 加入了
    bool shouldRebuildLayerHierarchy = insertSize > 0 || diffResult.movedItems.size() > 0;

    NSLog(@"[PV2] layer:%p diff 结果 deleteSize:%ld insertSize:%ld movedSize:%ld", rootLayer, deleteSize, insertSize, diffResult.movedItems.size());
    for (NSInteger i = 0; i < deleteSize; i++) {
        NSInteger removeIndex = diffResult.deletsItems[i];
        const DrawingItem &commandToBeDelete = oldArray[removeIndex];
        const TMMNativeDrawingType drawingType = commandToBeDelete.drawingType;
        const uint64_t itemHash = commandToBeDelete.itemHash;

        // 1. 重置被删掉的 item 的 hash
        if (drawingType != TMMNativeDrawingTypeDrawLayer && drawingType != TMMNativeDrawingTypePop) {
            resetDrawingItemContentsHash(drawingType, itemHash);
        }

        // 2. 从视图树上拔掉 layer
        deatchLayer(rootLayer, layerPool, clipPool, drawingType, itemHash, rootLayerHash);

        // 3. 如果是删除的 clip layer 则需要重新排序
        shouldRebuildLayerHierarchy = shouldRebuildLayerHierarchy || (drawingType == TMMNativeDrawingTypeClip);
    }

    NSLog(@"[PV2] layer:%p 完成 diff 差异 apply shouldRebuildLayerHierarchy:%@", rootLayer, @(shouldRebuildLayerHierarchy));
    if (shouldRebuildLayerHierarchy) {
        rebuildLayerHierarchy(rootLayer);
    }
}

#ifdef COCOAPODS
const DrawingItem &PictureRecorder::drawingCommandAtIndex(NSInteger index) {
    return props->currentDrawingItems[index];
}
#endif

void PictureRecorder::prepareForReuse() {
    if (props) {
        props->prepareForReuse();
        resetSequenceTable();
    }

    saveStack.clear();
    saveStack.emplace_back(CALayerSaveStateCreateSafeGuard());
}

PictureRecorder::~PictureRecorder() {
    if (props) {
        delete props;
    }
}

}
