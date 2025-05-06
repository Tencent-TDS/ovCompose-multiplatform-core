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

#import "TMMRenderReuseCache.h"

@interface TMMRenderReuseCache ()

/// 对象池
@property (nonatomic, strong) NSMutableSet<id> *objectPool;

@end

@implementation TMMRenderReuseCache

+ (instancetype)cacheWithObjectLimitCount:(NSUInteger)objectLimitCount;
{
    TMMRenderReuseCache *cache = [[TMMRenderReuseCache alloc] init];
    cache->_objectLimitCount = objectLimitCount;
    return cache;
}

- (instancetype)init {
    if (self = [super init]) {
        // 添加内存告警的通知，用于在内存告警的时候，淘汰掉对象
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(handleMemoryWarning)
                                                     name:UIApplicationDidReceiveMemoryWarningNotification
                                                   object:nil];
    }
    return self;
}

#pragma mark - public methods
- (BOOL)enqueObject:(id)object {
    NSMutableSet<id> *objectPool = self.objectPool;
    if (objectPool.count < self.objectLimitCount) {
        [objectPool addObject:object];
        return YES;
    }
    return NO;
}

- (id)dequeueObject {
    id object = [self.objectPool anyObject];
    if (object) {
        [self.objectPool removeObject:object];
    }
    return object;
}

- (NSUInteger)objectCount {
    return self.objectPool.count;
}

- (void)addObjectFromReuseCache:(TMMRenderReuseCache *)reuseCache {
    NSMutableSet<id> *currentObjectPool = self.objectPool;
    NSInteger currentCount = currentObjectPool.count;
    if (currentCount == 0) {
        [currentObjectPool setSet:reuseCache.objectPool];
    } else {
        NSInteger count = currentCount - self.objectLimitCount;
        for (NSInteger i = 0; i < count; i++) {
            [currentObjectPool addObject:reuseCache.objectPool.anyObject];
        }
    }
    [reuseCache.objectPool removeAllObjects];
}

#pragma mark - private methods
- (void)handleMemoryWarning {
    [self.objectPool removeAllObjects];
}

- (NSMutableSet<id> *)objectPool {
    if (_objectPool) {
        return _objectPool;
    }
    _objectPool = [NSMutableSet setWithCapacity:self.objectLimitCount];
    return _objectPool;
}

- (void)dealloc {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

@end
