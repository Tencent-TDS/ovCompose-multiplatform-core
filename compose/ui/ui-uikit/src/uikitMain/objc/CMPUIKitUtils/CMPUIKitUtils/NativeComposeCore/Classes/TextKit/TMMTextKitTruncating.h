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

#import <Foundation/Foundation.h>

@class TMMTextKitContext;
NS_ASSUME_NONNULL_BEGIN

@protocol TMMTextKitTruncating <NSObject>

/// visibleRanges of  the string to fit the constraints of the TextKit components.
@property (nonatomic, assign, readonly) NSArray *visibleRanges;

/// truncation string rect
@property (nonatomic, assign, readonly) CGRect truncationStringRect;

/**
 A truncater object is initialized with the full state of the text.  It is a Single Responsibility Object that is
 mutative.  It configures the state of the TextKit components (layout manager, text container, text storage) to achieve
 the intended truncation, then it stores the resulting state for later fetching.

 The truncater may mutate the state of the text storage such that only the drawn string is actually present in the
 text storage itself.

 The truncater should not store a strong reference to the context to prevent retain cycles.
 */
- (instancetype)initWithContext:(TMMTextKitContext *)context
     truncationAttributedString:(NSAttributedString *)truncationAttributedString
         avoidTailTruncationSet:(NSCharacterSet *)avoidTailTruncationSet
                constrainedSize:(CGSize)constrainedSize;

@end

NS_ASSUME_NONNULL_END
