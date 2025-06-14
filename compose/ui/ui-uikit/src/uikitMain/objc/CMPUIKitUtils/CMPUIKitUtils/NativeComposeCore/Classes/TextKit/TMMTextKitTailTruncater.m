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

#import "TMMTextKitTailTruncater.h"
#import "TMMTextKitContext.h"

@implementation TMMTextKitTailTruncater {
    __weak TMMTextKitContext *_context;
    NSAttributedString *_truncationAttributedString;
    NSCharacterSet *_avoidTailTruncationSet;
    CGSize _constrainedSize;
}
@synthesize visibleRanges = _visibleRanges;
@synthesize truncationStringRect = _truncationStringRect;

- (instancetype)initWithContext:(TMMTextKitContext *)context
     truncationAttributedString:(NSAttributedString *)truncationAttributedString
         avoidTailTruncationSet:(NSCharacterSet *)avoidTailTruncationSet
                constrainedSize:(CGSize)constrainedSize {
    if (self = [super init]) {
        _context = context;
        _truncationAttributedString = truncationAttributedString;
        _avoidTailTruncationSet = avoidTailTruncationSet;
        _constrainedSize = constrainedSize;

        [self _truncate];
    }
    return self;
}

/**
 Calculates the intersection of the truncation message within the end of the last line.
 */
- (NSUInteger)_calculateCharacterIndexBeforeTruncationMessage:(NSLayoutManager *)layoutManager
                                                  textStorage:(NSTextStorage *)textStorage
                                                textContainer:(NSTextContainer *)textContainer {
    CGRect constrainedRect = (CGRect) { .size = textContainer.size };

    NSRange visibleGlyphRange = [layoutManager glyphRangeForBoundingRect:constrainedRect inTextContainer:textContainer];
    NSInteger lastVisibleGlyphIndex = (NSMaxRange(visibleGlyphRange) - 1);

    if (lastVisibleGlyphIndex < 0) {
        return NSNotFound;
    }

    CGRect lastLineRect = [layoutManager lineFragmentRectForGlyphAtIndex:lastVisibleGlyphIndex effectiveRange:NULL];
    CGRect lastLineUsedRect = [layoutManager lineFragmentUsedRectForGlyphAtIndex:lastVisibleGlyphIndex effectiveRange:NULL];
    NSParagraphStyle *paragraphStyle = [textStorage attributesAtIndex:[layoutManager characterIndexForGlyphAtIndex:lastVisibleGlyphIndex]
                                                       effectiveRange:NULL][NSParagraphStyleAttributeName];
    // We assume LTR so long as the writing direction is not
    BOOL rtlWritingDirection = paragraphStyle ? paragraphStyle.baseWritingDirection == NSWritingDirectionRightToLeft : NO;
    // We only want to treat the trunction rect as left-aligned in the case that we are right-aligned and our writing
    // direction is RTL.
    BOOL leftAligned = CGRectGetMinX(lastLineRect) == CGRectGetMinX(lastLineUsedRect) || !rtlWritingDirection;

    // Calculate the bounding rectangle for the truncation message
    TMMTextKitContext *truncationContext = [[TMMTextKitContext alloc] initWithAttributedString:_truncationAttributedString
                                                                               constrainedSize:constrainedRect.size];

    __block CGRect truncationUsedRect;

    [truncationContext performBlockWithLockedTextKitComponents:^(NSLayoutManager *truncationLayoutManager, NSTextStorage *truncationTextStorage,
                                                                 NSTextContainer *truncationTextContainer) {
        // Size the truncation message
        [truncationLayoutManager ensureLayoutForTextContainer:truncationTextContainer];
        NSRange truncationGlyphRange = [truncationLayoutManager glyphRangeForTextContainer:truncationTextContainer];
        truncationUsedRect = [truncationLayoutManager boundingRectForGlyphRange:truncationGlyphRange inTextContainer:truncationTextContainer];
    }];
    CGFloat truncationOriginX = (leftAligned ? CGRectGetMaxX(constrainedRect) - truncationUsedRect.size.width : CGRectGetMinX(constrainedRect));
    CGRect translatedTruncationRect
        = CGRectMake(truncationOriginX, CGRectGetMinY(lastLineRect), truncationUsedRect.size.width, truncationUsedRect.size.height);

    // Determine which glyph is the first to be clipped / overlaps the truncation message.
    CGFloat truncationMessageX = (leftAligned ? CGRectGetMinX(translatedTruncationRect) : CGRectGetMaxX(translatedTruncationRect));
    CGPoint beginningOfTruncationMessage = CGPointMake(truncationMessageX, CGRectGetMidY(translatedTruncationRect));
    NSUInteger firstClippedGlyphIndex = [layoutManager glyphIndexForPoint:beginningOfTruncationMessage
                                                          inTextContainer:textContainer
                                           fractionOfDistanceThroughGlyph:NULL];
    // If it didn't intersect with any text then it should just return the last visible character index, since the
    // truncation rect can fully fit on the line without clipping any other text.
    if (firstClippedGlyphIndex == NSNotFound) {
        return [layoutManager characterIndexForGlyphAtIndex:lastVisibleGlyphIndex];
    }
    NSUInteger firstCharacterIndexToReplace = [layoutManager characterIndexForGlyphAtIndex:firstClippedGlyphIndex];
    if (firstCharacterIndexToReplace == NSNotFound) {
        return NSNotFound;
    }

    // Break on word boundaries
    return [self _findTruncationInsertionPointAtOrBeforeCharacterIndex:firstCharacterIndexToReplace
                                                         layoutManager:layoutManager
                                                           textStorage:textStorage];
}

/**
 Finds the first whitespace at or before the character index do we don't truncate in the middle of words
 If there are multiple whitespaces together (say a space and a newline), this will backtrack to the first one
 */
- (NSUInteger)_findTruncationInsertionPointAtOrBeforeCharacterIndex:(NSUInteger)firstCharacterIndexToReplace
                                                      layoutManager:(NSLayoutManager *)layoutManager
                                                        textStorage:(NSTextStorage *)textStorage {
    // Don't attempt to truncate beyond the end of the string
    if (firstCharacterIndexToReplace >= textStorage.length) {
        return 0;
    }

    // Find the glyph range of the line fragment containing the first character to replace.
    NSRange lineGlyphRange;
    [layoutManager lineFragmentRectForGlyphAtIndex:[layoutManager glyphIndexForCharacterAtIndex:firstCharacterIndexToReplace]
                                    effectiveRange:&lineGlyphRange];

    // Look for the first whitespace from the end of the line, starting from the truncation point
    NSUInteger startingSearchIndex = [layoutManager characterIndexForGlyphAtIndex:lineGlyphRange.location];
    NSUInteger endingSearchIndex = firstCharacterIndexToReplace;
    NSRange rangeToSearch = NSMakeRange(startingSearchIndex, (endingSearchIndex - startingSearchIndex));

    NSRange rangeOfLastVisibleAvoidedChars = { .location = NSNotFound };
    if (_avoidTailTruncationSet) {
        rangeOfLastVisibleAvoidedChars = [textStorage.string rangeOfCharacterFromSet:_avoidTailTruncationSet
                                                                             options:NSBackwardsSearch
                                                                               range:rangeToSearch];
    }

    // Couldn't find a good place to truncate. Might be because there is no whitespace in the text, or we're dealing
    // with a foreign language encoding. Settle for truncating at the original place, which may be mid-word.
    if (rangeOfLastVisibleAvoidedChars.location == NSNotFound) {
        return firstCharacterIndexToReplace;
    } else {
        return rangeOfLastVisibleAvoidedChars.location;
    }
}

- (void)_truncate {
    [_context performBlockWithLockedTextKitComponents:^(NSLayoutManager *layoutManager, NSTextStorage *textStorage, NSTextContainer *textContainer) {
        NSUInteger originalStringLength = textStorage.length;

        [layoutManager ensureLayoutForTextContainer:textContainer];

        NSRange visibleGlyphRange = [layoutManager glyphRangeForBoundingRect:CGRectMake(0, 0, textContainer.size.width, textContainer.size.height)
                                                             inTextContainer:textContainer];
        NSRange visibleCharacterRange = [layoutManager characterRangeForGlyphRange:visibleGlyphRange actualGlyphRange:NULL];

        // Check if text is truncated, and if so apply our truncation string
        if (visibleCharacterRange.length < originalStringLength && self->_truncationAttributedString.length > 0) {
            NSInteger firstCharacterIndexToReplace = [self _calculateCharacterIndexBeforeTruncationMessage:layoutManager
                                                                                               textStorage:textStorage
                                                                                             textContainer:textContainer];
            if (firstCharacterIndexToReplace == 0 || firstCharacterIndexToReplace >= textStorage.length) {
                // Either nothing is visible, or everything is; nothing for us to do here
                return;
            }

            // Update/truncate the visible range of text
            visibleCharacterRange = NSMakeRange(0, firstCharacterIndexToReplace);
            NSRange truncationReplacementRange = NSMakeRange(firstCharacterIndexToReplace, textStorage.length - firstCharacterIndexToReplace);
            // Replace the end of the visible message with the truncation string
            [textStorage replaceCharactersInRange:truncationReplacementRange withAttributedString:self->_truncationAttributedString];
        }

        self->_visibleRanges = @[ [NSValue valueWithRange:visibleCharacterRange] ];
    }];
}

@end
