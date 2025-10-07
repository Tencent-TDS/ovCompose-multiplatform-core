#ifndef OH_NATIVE_PICTURE_RECORDER_H
#define OH_NATIVE_PICTURE_RECORDER_H

#include <vector>
#include <unordered_map>
#include <array>
#include <functional>
#include <memory>
#include "../constants/oh_native_enum.h"
#include "oh_native_picture_recorder_drawing_Item.h"
#include "oh_native_render_node_save_state.h"
#include "../render_node/oh_base_render_node.h"
#include "../utils/oh_hash_funcs.h"

namespace OH {

    constexpr static const uint64_t OHInitialHash = 0x811c9dc5;
    constexpr static const int OHReverseNumber = 30;

    inline uint64_t XXH64(const void* data, size_t len, uint64_t seed) {
        const uint8_t* bytes = static_cast<const uint8_t*>(data);
        uint64_t hash = seed;

        for (size_t i = 0; i < len; ++i) {
            hash = (hash * 0x9E3779B185EBCA87ULL) ^ bytes[i];
        }

        return hash;
    }

    inline uint64_t RenderNodeSaveStateHash(const RenderNodeSaveState &saveState) {
        return XXH64(&saveState, sizeof(RenderNodeSaveState), 0);
    }

    class OHComposeNativePaint;

    struct PictureRecorderUpdateInfo {
        bool isDirty;
        uint64_t itemHash;
        OHNativeDrawingType drawingType;
        RenderNodeSaveState saveState;
    };

    class PictureRecorder {
    public:
        PictureRecorder() noexcept;
        ~PictureRecorder();

        void startRecording(BaseRenderNode& rootRenderNode);
        void finishRecording(BaseRenderNode& rootRenderNode);

        OH_ALWAYS_INLINE void save() {
            pushSaveStack(RenderNodeSaveStateMakeType::Save);
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
            //currentState.transform = Transform3D::Scale(currentState.transform, sx, sy, 1);
        }
        OH_ALWAYS_INLINE void rotate(float degrees) {
            RenderNodeSaveState &currentState = topState();
            //currentState.transform = Transform3D::Rotate(currentState.transform, degrees * (M_PI / 180), 0, 0, 1);
        }

        OH_ALWAYS_INLINE BaseRenderNode* getOrCreateRenderNodeForDrawing(OHNativeDrawingType type, uint64_t itemHash) {
            initPropsIfNeeded();

            if (type == OHNativeDrawingType::Clip) {
                return getOrCreateClipRenderNode(itemHash);
            }

            if (auto* cachedNode = props->findRenderNode(itemHash)) {
                return cachedNode;
            }

            return props->createAndAddRenderNode(itemHash);
        }

        OH_ALWAYS_INLINE PictureRecorderUpdateInfo drawRenderNode(BaseRenderNode* renderNode) {
            initPropsIfNeeded();

            const OHNativeDrawingType drawingType = OHNativeDrawingType::DrawLayer;
            const uint64_t renderNodeUniqueHash = renderNode->getHash();

            currentDrawHash = hashMerge(currentDrawHash, renderNodeUniqueHash);

            props->currentDrawingItems.emplace_back(DrawingItem{
                    .itemHash = renderNodeUniqueHash,
                    .contentsHash = renderNodeUniqueHash,
                    .drawingType = drawingType
            });

            // 将已有的BaseRenderNode添加到缓存池
            props->renderNodePool[renderNodeUniqueHash] = renderNode;
            renderNode->setHostingHash(rootRenderNodeHash);
            const RenderNodeSaveState &saveState = topState();

            return PictureRecorderUpdateInfo{
                    .isDirty = true,
                    .itemHash = renderNodeUniqueHash,
                    .drawingType = drawingType,
                    .saveState = saveState
            };
        }

        OH_ALWAYS_INLINE PictureRecorderUpdateInfo clip(uint64_t drawingContentHash) {
            PictureRecorderUpdateInfo updateItem = draw(OHNativeDrawingType::Clip, drawingContentHash);
            pushClip();
            return updateItem;
        }

        PictureRecorderUpdateInfo draw(OHNativeDrawingType drawingType, uint64_t drawingContentHash);

        void prepareForReuse();

        BaseRenderNode* getOrCreateLayerForDrawing(OHNativeDrawingType type, uint64_t itemHash);

    private:
        struct PictureRecorderProps {
            PictureRecorderProps() noexcept;

            std::vector<DrawingItem> currentDrawingItems;
            std::vector<DrawingItem> finalDrawingItems;

            std::unordered_map<uint64_t, BaseRenderNode*> renderNodePool;
            std::unordered_map<uint64_t, BaseRenderNode*> clipPool;

            std::vector<std::unique_ptr<BaseRenderNode>> ownedNodes;

            void prepareForReuse();

            BaseRenderNode* findRenderNode(uint64_t hash) {
                auto it = renderNodePool.find(hash);
                return it != renderNodePool.end() ? it->second : nullptr;
            }

            BaseRenderNode* findClipNode(uint64_t hash) {
                auto it = clipPool.find(hash);
                return it != clipPool.end() ? it->second : nullptr;
            }

            BaseRenderNode* createAndAddRenderNode(uint64_t hash) {
                auto node = std::make_unique<BaseRenderNode>();
                BaseRenderNode* ptr = node.get();
                renderNodePool[hash] = ptr;
                ownedNodes.push_back(std::move(node));
                return ptr;
            }

            BaseRenderNode* createAndAddClipNode(uint64_t hash) {
                auto node = std::make_unique<BaseRenderNode>();
                BaseRenderNode* ptr = node.get();
                clipPool[hash] = ptr;
                ownedNodes.push_back(std::move(node));
                return ptr;
            }

        private:
            std::vector<BaseRenderNode*> newSubRenderNode;
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
            std::array<uint64_t, OHNativeDrawingTypeCount> table{};
            for (size_t i = 0; i < OHNativeDrawingTypeCount; ++i) {
                table[i] = 1ULL << (i + 29);
            }
            return table;
        }();

        std::unique_ptr<PictureRecorderProps> props = nullptr;
        std::array<SequenceTypeItem, OHNativeDrawingTypeCount> sequenceTable;

        uint64_t finishDrawHash = OHInitialHash;
        uint64_t currentDrawHash = OHInitialHash;

        std::vector<RenderNodeSaveState> saveStack;

        bool isFirstRender = true;
        uintptr_t rootRenderNodeHash = 0;
        int clipCountDuringOnceOperation = 0;

        void initPropsIfNeeded();
        constexpr void resetSequenceTableIndex();
        constexpr void resetSequenceTable();

        OH_ALWAYS_INLINE BaseRenderNode* getOrCreateClipRenderNode(uint64_t itemHash) {
            initPropsIfNeeded();

            if (auto* cachedNode = props->findClipNode(itemHash)) {
                return cachedNode;
            }

            return props->createAndAddClipNode(itemHash);
        }

        SequenceIdInfo allocSequenceIdInfo(OHNativeDrawingType type, uint64_t currentContentsHash);
        void detachRenderNode(BaseRenderNode& rootRenderNode, OHNativeDrawingType drawingType, uint64_t itemHash);

        void prepareForNextRecording(BaseRenderNode& rootRenderNode);
        void rebuildRenderNodeHierarchy(BaseRenderNode& rootRenderNode);
        void diffDrawingItems(BaseRenderNode& rootRenderNode);
        RenderNodeSaveState &topState();
        void pushSaveStack(RenderNodeSaveStateMakeType type);

        void pushClip();
        void popClip();
        void resetDrawingItemContentsHash(OHNativeDrawingType type, uint64_t itemHash);
    };

} // namespace OH

#endif // OH_PICTURE_RECORDER_H