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

NS_ASSUME_NONNULL_BEGIN

/// TextKit 上下文
@interface TMMTextKitContext : NSObject

/**
 Initializes a context and its associated TextKit components.

 Initialization of TextKit components is a globally locking operation so be careful of bottlenecks with this class.
 */
- (instancetype)initWithAttributedString:(nullable NSAttributedString *)attributedString constrainedSize:(CGSize)constrainedSize;

/**
 All operations on TextKit values MUST occur within this locked context.  Simultaneous access (even non-mutative) to
 TextKit components may cause crashes.

 The block provided MUST not call out to client code from within its scope or it is possible for this to cause deadlocks
 in your application.  Use with EXTREME care.

 Callers MUST NOT keep a ref to these internal objects and use them later.  This WILL cause crashes in your application.
 */
- (void)performBlockWithLockedTextKitComponents:(void (^)(NSLayoutManager *layoutManager, NSTextStorage *textStorage,
                                                          NSTextContainer *textContainer))block;

@end

NS_ASSUME_NONNULL_END
