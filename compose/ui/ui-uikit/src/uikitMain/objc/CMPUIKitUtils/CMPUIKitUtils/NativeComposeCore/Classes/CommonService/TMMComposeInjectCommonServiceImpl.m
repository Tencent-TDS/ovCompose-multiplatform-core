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

#import "TMMComposeInjectCommonServiceImpl.h"

@interface TMMComposeInjectCommonServiceImpl()
@property (nonatomic, strong) id<TMMComposeInjectCommonService> commonService;
@end

@implementation TMMComposeInjectCommonServiceImpl

+ (instancetype)sharedInstance {
    static dispatch_once_t onceToken;
    static TMMComposeInjectCommonServiceImpl *instance = nil;
    dispatch_once(&onceToken, ^{
        instance = [[self alloc] init];
    });
    return instance;
}

- (void)injectCommonService:(nonnull id<TMMComposeInjectCommonService>)commonService {
    _commonService = commonService;
}

- (BOOL)tmm_getTabToggleIsOnKey:(nonnull NSString *)key defaultValue:(BOOL)defaultValue {
    return [_commonService tmm_getTabToggleIsOnKey:key defaultValue:defaultValue];
}

- (NSDictionary<NSString *,id> *)tmm_getTabMapValueKey:(NSString *)key {
    return [_commonService tmm_getTabMapValueKey:key];
}

- (int32_t)tmm_getConfigGrayPolicyIdKey:(NSString *)key needReport:(BOOL)needReport {
    return [_commonService tmm_getConfigGrayPolicyIdKey:key needReport:needReport];
}

- (int32_t)tmm_getToggleGrayPolicyIdKey:(NSString *)key needReport:(BOOL)needReport {
    return [_commonService tmm_getToggleGrayPolicyIdKey:key needReport:needReport];
}

- (BOOL)tmm_getTabToggleIsOnKey:(nonnull NSString *)key defaultValue:(BOOL)defaultValue needReport:(BOOL)needReport { 
    return [_commonService tmm_getTabToggleIsOnKey:key defaultValue:defaultValue needReport:needReport];
}

- (void)tmm_logMessage:(NSString*)tag message:(NSString *)message {
    return [_commonService tmm_logMessage:tag message:message];
}

@end


void OVComposeLog(NSString *tag, NSString *message) {
    [[TMMComposeInjectCommonServiceImpl sharedInstance] tmm_logMessage:tag message:message];
}
