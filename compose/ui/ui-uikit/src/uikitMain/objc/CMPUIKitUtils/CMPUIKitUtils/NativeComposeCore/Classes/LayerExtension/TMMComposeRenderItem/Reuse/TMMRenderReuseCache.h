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

/// 一个同种类型的简单的对象复用池，常见的是对 TMMCanvasViewProxy 的复用，注意这是非线程安全的，目前都是在 Compose 主线程运行
@interface TMMRenderReuseCache<ObjectType> : NSObject

/// 对象复用的最大数量
@property (nonatomic, assign, readonly) NSUInteger objectLimitCount;

/// 当前复用池中的对象数量
@property (nonatomic, assign, readonly) NSUInteger objectCount;

+ (instancetype)new NS_UNAVAILABLE;
+ (instancetype)init NS_UNAVAILABLE;

/// 通过 objectClass 和 objectLimitCount 创建一个对象复用池
/// - Parameters:
///   - objectLimitCount: 对象复用的最大数量
+ (instancetype)cacheWithObjectLimitCount:(NSUInteger)objectLimitCount;

/// 将对象放入复用池中，对象池满则返回 NO 表示放入失败
/// - Parameter object: ObjectType
- (BOOL)enqueObject:(ObjectType)object;

/// 从复用池中取出对象，可能为空
- (nullable ObjectType)dequeueObject;

/// 将复用池中的对象添加到当前复用池中
- (void)addObjectFromReuseCache:(TMMRenderReuseCache *)reuseCache;

@end

NS_ASSUME_NONNULL_END
