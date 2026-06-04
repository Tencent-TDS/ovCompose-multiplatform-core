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


#import <UIKit/UIKit.h>
#import "TMMPictureRecorderDrawingItem.h"
#import "TMMNativeEnums.h"
#import "TMMPictureRecorderUnitTest.h"
#import "TMMCALayerSaveState.h"

#ifdef __cplusplus

#import <stack>
#import <vector>

NS_ASSUME_NONNULL_BEGIN

@class TMMComposeNativePaint;
@class TMMUIKitCanvasLayer;

namespace ovcompose {

constexpr static const uint64_t TMMInitialHash = 0x811c9dc5;

constexpr static const int TMMReverseNumber = 30;

typedef void(^PictureRecorderLayerUpdateBlock)(__kindof CALayer *rootLayer,
                                               __kindof CALayer *layerForDrawing,
                                               const CALayerSaveState *saveState,
                                               float density);

/// 每一次 PictureRecorder 更新的信息
struct PictureRecorderUpdateInfo {
    bool isDirty;
    uint64_t itemHash;
    TMMNativeDrawingType drawingType;
    CALayerSaveState saveState;
};

/// 每一个 ComposeCanvas 都有一个 PictureRecorder 对渲染命令进行处理
struct PictureRecorder {
    
#ifdef COCOAPODS
    PictureRecorderDebugTrace trace;
#endif
    
    static const float density;
    
    bool enableCALayerClipOpt = false;
    
    PictureRecorder() noexcept;

    void startRecording(CALayer *rootLayer);
    void finishRecording(CALayer *rootLayer);
    
    TMM_ALWAYS_INLINE void save();
    TMM_ALWAYS_INLINE void restore();
    
    TMM_ALWAYS_INLINE void translate(float dx, float dy);
    TMM_ALWAYS_INLINE void scale(float sx, float sy);
    TMM_ALWAYS_INLINE void rotate(float degrees);

    TMM_ALWAYS_INLINE CALayer *getOrCreateLayerForDrawing(TMMNativeDrawingType type, uint64_t itemHash);
    
    TMM_ALWAYS_INLINE PictureRecorderUpdateInfo drawLayer(CALayer *layer);
    
    TMM_ALWAYS_INLINE PictureRecorderUpdateInfo clip(uint64_t drawingContentHash);
    
    TMM_ALWAYS_INLINE void clipEnd();
    
    PictureRecorderUpdateInfo draw(TMMNativeDrawingType drawingType, uint64_t drawingContentHash);
    
    void prepareForReuse();

#ifdef COCOAPODS
    const TMM::DrawingItem &drawingCommandAtIndex(NSInteger index);
#endif

private:
    
    struct PictureRecorderProps {
        
        PictureRecorderProps() noexcept;
        
        // 最新的绘制命令
        std::vector<TMM::DrawingItem> currentDrawingItems;
        
        // 上一次的绘制命令
        std::vector<TMM::DrawingItem> finialDrawingItems;
        
        // 每一个绘制命令对应的 layer key 是 itemHash
        std::unordered_map<uint64_t, CALayer *> layerPool;
        
        std::unordered_map<uint64_t, UIView *> clipPool;
        
        TMM_ALWAYS_INLINE void prepareForReuse();
        
    private:
        NSMutableArray <CALayer *> *_newSublayers;
    };

    struct SequenceTypeItem {
        uint64_t itemIndex = 0;
        // 预留的 50 个快速存储 hash 的 array
        std::array<uint64_t, TMMReverseNumber> itemHashArray;
        // 超出 50 个的存这里, std::unordered_map
        std::unordered_map<uint64_t, uint64_t> itemHashMap;
    };
    
    struct SequenceIdInfo {
        uint64_t itemIndex = 0;
        bool isDirty = true;
    };
    
    static CALayerSaveState errorSaveState;
    
    PictureRecorderProps props;
    
    std::array<SequenceTypeItem, TMMNativeDrawingTypeCount> sequenceTable;
    
    uint64_t finishDrawHash = TMMInitialHash;
    uint64_t currentDrawHash = TMMInitialHash;
    
    std::vector<CALayerSaveState> saveStack;
    
    bool isFirstRender = true;
    
    NSInteger rootLayerHash = 0;
    
    TMM_ALWAYS_INLINE constexpr void resetSequenceTableIndex();
    TMM_ALWAYS_INLINE constexpr void resetSequenceTable();
    
    TMM_ALWAYS_INLINE SequenceIdInfo allocSequenceIdInfo(TMMNativeDrawingType type, uint64_t currentContentsHash);
    
    TMM_ALWAYS_INLINE void prepareForNextRecording(CALayer *rootLayer);
    
    TMM_ALWAYS_INLINE UIView *getOrCreateClipView(uint64_t itemHash);
    
    TMM_ALWAYS_INLINE void rebuildLayerHierarchy(CALayer *rootLayer);
    
    TMM_ALWAYS_INLINE void diffDrawingItems(CALayer *rootLayer);
    
    TMM_ALWAYS_INLINE CALayerSaveState &topState();
    
    TMM_ALWAYS_INLINE CALayerSaveState &safeTopState();
    
    TMM_ALWAYS_INLINE void pushSaveStack(CALayerSaveStateMakeType type);
    
    // 将会开启子层级进行 clip，为了不影响坐标系的计算，clipLayer 使用 bounds 做偏移
    TMM_ALWAYS_INLINE void pushClip();
    
    TMM_ALWAYS_INLINE void resetDrawingItemContentsHash(TMMNativeDrawingType type, uint64_t itemHash);
};

}

NS_ASSUME_NONNULL_END

#endif


