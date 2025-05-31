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

typedef NS_OPTIONS(NSUInteger, TMMComposeAspectOptions) {
    TMMComposeAspectPositionAfter = 0,   /// Called after the original implementation (default)
    TMMComposeAspectPositionInstead = 1, /// Will replace the original implementation.
    TMMComposeAspectPositionBefore = 2,  /// Called before the original implementation.

    TMMComposeAspectOptionAutomaticRemoval = 1 << 3 /// Will remove the hook after the first execution.
};

/// Opaque Aspect Token that allows to deregister the hook.
@protocol TMMComposeAspectToken <NSObject>

/// Deregisters an aspect.
/// @return YES if deregistration is successful, otherwise NO.
- (BOOL)remove;

@end

/// The AspectInfo protocol is the first parameter of our block syntax.
@protocol TMMComposeAspectInfo <NSObject>

/// The instance that is currently hooked.
- (id)instance;

/// The original invocation of the hooked method.
- (NSInvocation *)originalInvocation;

/// All method arguments, boxed. This is lazily evaluated.
- (NSArray *)arguments;

@end

/**
 Aspects uses Objective-C message forwarding to hook into messages. This will create some overhead. Don't add aspects to methods that are called a
 lot. Aspects is meant for view/controller code that is not called a 1000 times per second.

 Adding aspects returns an opaque token which can be used to deregister again. All calls are thread safe.
 */
@interface NSObject (TMMComposeAspects)

/// Adds a block of code before/instead/after the current `selector` for a specific class.
///
/// @param block Aspects replicates the type signature of the method being hooked.
/// The first parameter will be `id<AspectInfo>`, followed by all parameters of the method.
/// These parameters are optional and will be filled to match the block signature.
/// You can even use an empty block, or one that simple gets `id<AspectInfo>`.
///
/// @note Hooking static methods is not supported.
/// @return A token which allows to later deregister the aspect.
+ (id<TMMComposeAspectToken>)tmm_compose_aspect_hookSelector:(SEL)selector
                                                 withOptions:(TMMComposeAspectOptions)options
                                                  usingBlock:(id)block
                                                       error:(NSError **)error;

/// Adds a block of code before/instead/after the current `selector` for a specific instance.
- (id<TMMComposeAspectToken>)tmm_compose_aspect_hookSelector:(SEL)selector
                                                 withOptions:(TMMComposeAspectOptions)options
                                                  usingBlock:(id)block
                                                       error:(NSError **)error;

@end

typedef NS_ENUM(NSUInteger, TMMComposeAspectErrorCode) {
    TMMComposeAspectErrorSelectorBlacklisted,                   /// Selectors like release, retain, autorelease are blacklisted.
    TMMComposeAspectErrorDoesNotRespondToSelector,              /// Selector could not be found.
    TMMComposeAspectErrorSelectorDeallocPosition,               /// When hooking dealloc, only AspectPositionBefore is allowed.
    TMMComposeAspectErrorSelectorAlreadyHookedInClassHierarchy, /// Statically hooking the same method in subclasses is not allowed.
    TMMComposeAspectErrorFailedToAllocateClassPair,             /// The runtime failed creating a class pair.
    TMMComposeAspectErrorMissingBlockSignature,                 /// The block misses compile time signature info and can't be called.
    TMMComposeAspectErrorIncompatibleBlockSignature,            /// The block signature does not match the method or is too large.

    TMMComposeAspectErrorRemoveObjectAlreadyDeallocated = 100 /// (for removing) The object hooked is already deallocated.
};

extern NSString *const TMMComposeAspectErrorDomain;
