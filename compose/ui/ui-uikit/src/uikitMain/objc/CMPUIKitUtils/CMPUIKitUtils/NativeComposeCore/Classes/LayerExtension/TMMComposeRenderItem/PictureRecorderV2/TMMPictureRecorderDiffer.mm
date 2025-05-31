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

#import "TMMPictureRecorderDiffer.h"

namespace TMM {

struct ListEntry {
    mutable int oldCounter = 0;
    mutable int newCounter = 0;
    mutable std::deque<NSInteger> oldIndexes;
    mutable bool updated = false;

    void reset() const {
        newCounter = 0;
        oldCounter = 0;
        updated = false;
        oldIndexes.clear();
    }
};

struct ListRecord {
    ListEntry *entry = nullptr;
    NSInteger index = NSNotFound;
};

DiffResult diffDrawCommands(const std::vector<DrawingItem> &oldArray, const std::vector<DrawingItem> &newArray) {
    const NSInteger newCount = newArray.size();
    const NSInteger oldCount = oldArray.size();

    DiffResult diffResult;
    if (newCount == 0) {
        for (NSInteger i = 0; i < oldCount; i++) {
            diffResult.deletsItems.emplace_back(i);
        }
        return diffResult;
    }

    // if no old objects, everything from the newArray is inserted
    // take a shortcut and just build an insert-everything result
    if (oldCount == 0) {
        for (NSInteger i = 0; i < newCount; i++) {
            diffResult.insertItems.emplace_back(i);
        }
        return diffResult;
    }

    std::unordered_map<uint64_t, ListEntry> table;

    // symbol table uses the old/new array diffIdentifier as the key and IGListEntry as the value
    // using id<NSObject> as the key provided by https://lists.gnu.org/archive/html/discuss-gnustep/2011-07/msg00019.html
    for (const auto &pair : table) {
        const ListEntry &entry = pair.second;
        entry.reset();
    }

    // pass 1
    // create an entry for every item in the new array
    // increment its new count for each occurence
    std::vector<ListRecord> newResultsArray(newCount);
    for (NSInteger i = 0; i < newCount; i++) {
        uint64_t key = newArray[i].itemHash;
        ListEntry &entry = table[key];
        entry.newCounter++;

        // add NSNotFound for each occurence of the item in the new array
        entry.oldIndexes.push_back(NSNotFound);

        // note: the entry is just a pointer to the entry which is stack-allocated in the table
        newResultsArray[i].entry = &entry;
    }

    // pass 2
    // update or create an entry for every item in the old array
    // increment its old count for each occurence
    // record the original index of the item in the old array
    // MUST be done in descending order to respect the oldIndexes stack construction
    std::vector<ListRecord> oldResultsArray(oldCount);
    for (NSInteger i = oldCount - 1; i >= 0; i--) {
        uint64_t key = oldArray[i].itemHash;
        ListEntry &entry = table[key];
        entry.oldCounter++;

        // push the original indices where the item occurred onto the index stack
        entry.oldIndexes.push_back(i);

        // note: the entry is just a pointer to the entry which is stack-allocated in the table
        oldResultsArray[i].entry = &entry;
    }

    // pass 3
    // handle data that occurs in both arrays
    for (NSInteger i = 0; i < newCount; i++) {
        ListEntry *entry = newResultsArray[i].entry;
        const NSInteger originalIndex = entry->oldIndexes.back();
        entry->oldIndexes.pop_back();

        if (originalIndex < oldCount) {
            if (newArray[i].contentsHash != oldArray[originalIndex].contentsHash) {
                entry->updated = YES;
            }
        }
        if (originalIndex != NSNotFound && entry->newCounter > 0 && entry->oldCounter > 0) {
            // if an item occurs in the new and old array, it is unique
            // assign the index of new and old records to the opposite index (reverse lookup)
            newResultsArray[i].index = originalIndex;
            oldResultsArray[originalIndex].index = i;
        }
    }

    // track offsets from deleted items to calculate where items have moved
    std::vector<NSInteger> deleteOffsets(oldCount), insertOffsets(newCount);
    NSInteger runningOffset = 0;

    // iterate old array records checking for deletes
    // incremement offset for each delete
    for (NSInteger i = 0; i < oldCount; i++) {
        deleteOffsets[i] = runningOffset;
        const ListRecord &record = oldResultsArray[i];
        if (record.index == NSNotFound) {
            diffResult.deletsItems.emplace_back(i);
            runningOffset++;
        }
    }

    // reset and track offsets from inserted items to calculate where items have moved
    runningOffset = 0;
    for (NSInteger i = 0; i < newCount; i++) {
        insertOffsets[i] = runningOffset;
        const ListRecord &record = newResultsArray[i];
        const NSInteger oldIndex = record.index;
        // add to inserts if the opposing index is NSNotFound
        if (oldIndex == NSNotFound) {
            diffResult.insertItems.emplace_back(i);
            runningOffset++;
        } else {
            // note that an entry can be updated /and/ moved
            if (record.entry->updated) {
                diffResult.updatedItems.emplace_back(oldIndex);
            }

            // calculate the offset and determine if there was a move
            // if the indexes match, ignore the index
            const NSInteger insertOffset = insertOffsets[i];
            const NSInteger deleteOffset = deleteOffsets[oldIndex];
            if ((oldIndex - deleteOffset + insertOffset) != i) {
                diffResult.movedItems.emplace_back((RangeIndexs) {
                    .from = oldIndex,
                    .to = i,
                });
            }
        }
    }

    return diffResult;
}

}; // namespace TMM
