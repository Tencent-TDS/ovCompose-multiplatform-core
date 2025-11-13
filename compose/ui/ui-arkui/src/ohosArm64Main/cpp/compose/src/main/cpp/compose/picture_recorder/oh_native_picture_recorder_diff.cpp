#include "oh_native_picture_recorder_diff.h"

#include "compose/trace/oh_systrace_section.h"

#include <unordered_map>
#include <deque>
#include <cstdint>

namespace OH {

struct ListEntry {
    mutable int oldCounter = 0;
    mutable int newCounter = 0;
    mutable std::deque<size_t> oldIndexes;
    mutable bool updated = false;

    void reset() const {
        newCounter = 0;
        oldCounter = 0;
        updated = false;
        oldIndexes.clear();
    }
};

struct ListRecord {
    ListEntry* entry = nullptr;
    size_t index = static_cast<size_t>(-1);
};

static constexpr size_t kNotFound = static_cast<size_t>(-1);

DiffResult diffDrawCommands(const std::vector<DrawingItem>& oldArray,
                            const std::vector<DrawingItem>& newArray) {
    OH::SystraceSection trace("DiffDrawCommands");
    const size_t newCount = newArray.size();
    const size_t oldCount = oldArray.size();

    DiffResult diffResult;

    if (newCount == 0) {
        for (size_t i = 0; i < oldCount; i++) {
            diffResult.deletsItems.emplace_back(i);
        }
        return diffResult;
    }

    if (oldCount == 0) {
        for (size_t i = 0; i < newCount; i++) {
            diffResult.insertItems.emplace_back(i);
        }
        return diffResult;
    }

    std::unordered_map<uint64_t, ListEntry> table;

    for (auto& pair : table) {
        pair.second.reset();
    }

    std::vector<ListRecord> newResultsArray(newCount);
    for (size_t i = 0; i < newCount; i++) {
        uint64_t key = newArray[i].itemHash;
        ListEntry& entry = table[key];
        entry.newCounter++;

        entry.oldIndexes.push_back(kNotFound);

        newResultsArray[i].entry = &entry;
        newResultsArray[i].index = kNotFound;
    }

    std::vector<ListRecord> oldResultsArray(oldCount);
    for (size_t i = oldCount; i-- > 0;) {
        uint64_t key = oldArray[i].itemHash;
        ListEntry& entry = table[key];
        entry.oldCounter++;

        entry.oldIndexes.push_back(i);

        oldResultsArray[i].entry = &entry;
        oldResultsArray[i].index = kNotFound;
    }

    for (size_t i = 0; i < newCount; i++) {
        ListEntry* entry = newResultsArray[i].entry;

        if (entry->oldIndexes.empty()) {
            continue;
        }

        const size_t originalIndex = entry->oldIndexes.back();
        entry->oldIndexes.pop_back();

        if (originalIndex < oldCount) {
            if (newArray[i].contentsHash != oldArray[originalIndex].contentsHash) {
                entry->updated = true;
            }
        }

        if (originalIndex != kNotFound && entry->newCounter > 0 && entry->oldCounter > 0) {
            newResultsArray[i].index = originalIndex;
            oldResultsArray[originalIndex].index = i;
        }
    }

    std::vector<size_t> deleteOffsets(oldCount), insertOffsets(newCount);
    size_t runningOffset = 0;

    for (size_t i = 0; i < oldCount; i++) {
        deleteOffsets[i] = runningOffset;
        const ListRecord& record = oldResultsArray[i];
        if (record.index == kNotFound) {
            diffResult.deletsItems.emplace_back(i);
            runningOffset++;
        }
    }

    runningOffset = 0;
    for (size_t i = 0; i < newCount; i++) {
        insertOffsets[i] = runningOffset;
        const ListRecord& record = newResultsArray[i];
        const size_t oldIndex = record.index;

        if (oldIndex == kNotFound) {
            diffResult.insertItems.emplace_back(i);
            runningOffset++;
        } else {
            if (record.entry->updated) {
                diffResult.updatedItems.emplace_back(oldIndex);
            }

            const size_t insertOffset = insertOffsets[i];
            const size_t deleteOffset = deleteOffsets[oldIndex];

            if ((oldIndex - deleteOffset + insertOffset) != i) {
                diffResult.movedItems.emplace_back(oldIndex, i);
            }
        }
    }

    return diffResult;
}

} // namespace OH