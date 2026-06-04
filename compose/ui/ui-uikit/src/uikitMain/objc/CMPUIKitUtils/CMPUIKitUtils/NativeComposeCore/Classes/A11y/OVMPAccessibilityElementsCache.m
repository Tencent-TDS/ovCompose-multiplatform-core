/*
 * Copyright 2023 The Android Open Source Project
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

#import "OVMPAccessibilityElementsCache.h"
#import "OVMPAccessibilityElement.h"

@interface OVMPAccessibilityElementsCache()
@property (nonatomic, strong, readonly) NSMutableDictionary <NSNumber *, OVMPAccessibilityElement *> *backendCache;
@end

@implementation OVMPAccessibilityElementsCache

- (instancetype)init {
    self = [super init];
    if (self) {
        // 盲人模式节点在实际过程中会非常多
        _backendCache = [NSMutableDictionary dictionaryWithCapacity:100];
    }
    return self;
}

- (OVMPAccessibilityElement *)getOrCreateElementWithProxy:(OVMPAccessibilityElementProxy *)proxy {
    NSNumber *key = @(proxy.identifierKey);
    NSMutableDictionary <NSNumber *, OVMPAccessibilityElement *> *backendCache = self.backendCache;
    OVMPAccessibilityElement *element = [backendCache objectForKey:key];
    if (!element) {
        element = [[OVMPAccessibilityElement alloc] initWithElementProxy:proxy];
        [backendCache setObject:element forKey:key];
    }
    return element;
}

- (void)removeElementWithProxy:(OVMPAccessibilityElementProxy *)proxy {
    [self.backendCache removeObjectForKey:@(proxy.identifierKey)];
}

- (void)dispose {
    [_backendCache enumerateKeysAndObjectsUsingBlock:^(NSNumber * _Nonnull key, OVMPAccessibilityElement * _Nonnull obj, BOOL * _Nonnull stop) {
        [obj dispose];
    }];
    _backendCache = nil;
}

#pragma mark - debug
/// 给到通过无障碍模式的单测使用，仅限于 debug 环境
- (nullable id)accessibilityElementWithIdentifier:(NSString *)identifier {
    NSMutableDictionary <NSNumber *, OVMPAccessibilityElement *> *backendCache = self.backendCache;
    __block OVMPAccessibilityElement *result = nil;
    [backendCache enumerateKeysAndObjectsUsingBlock:^(NSNumber * _Nonnull key, OVMPAccessibilityElement * _Nonnull obj, BOOL * _Nonnull stop) {
        if ([obj.accessibilityIdentifier isEqualToString:identifier]) {
            result = obj;
            *stop = YES;
        } else if ([obj.accessibilityLabel isEqualToString:identifier]) {
            result = obj;
            *stop = YES;
        }
    }];
    return result;
}

@end
