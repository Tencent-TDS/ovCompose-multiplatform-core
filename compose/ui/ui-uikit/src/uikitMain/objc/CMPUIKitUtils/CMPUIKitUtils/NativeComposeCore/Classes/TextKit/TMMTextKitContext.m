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

#import "TMMTextKitContext.h"

@implementation TMMTextKitContext {
    NSLayoutManager *_layoutManager;
    NSTextStorage *_textStorage;
    NSTextContainer *_textContainer;
}

- (instancetype)initWithAttributedString:(nullable NSAttributedString *)attributedString constrainedSize:(CGSize)constrainedSize {
    if (self = [super init]) {
        // Create the TextKit component stack with our default configuration.
        _textStorage = (attributedString ? [[NSTextStorage alloc] initWithAttributedString:attributedString] : [[NSTextStorage alloc] init]);
        _layoutManager = [[NSLayoutManager alloc] init];
        _layoutManager.usesFontLeading = NO;
        [_textStorage addLayoutManager:_layoutManager];
        _textContainer = [[NSTextContainer alloc] initWithSize:constrainedSize];
        // We want the text laid out up to the very edges of the container.
        _textContainer.lineFragmentPadding = 0;
        [_layoutManager addTextContainer:_textContainer];
    }
    return self;
}

- (void)performBlockWithLockedTextKitComponents:(void (^)(NSLayoutManager *, NSTextStorage *, NSTextContainer *))block {
    block(_layoutManager, _textStorage, _textContainer);
}

@end
