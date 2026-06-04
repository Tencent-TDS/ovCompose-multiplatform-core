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

NS_ASSUME_NONNULL_BEGIN

/**
 * TMM Compose 通用服务注入协议
 * 提供配置开关、灰度策略和日志记录等通用功能
 */
@protocol TMMComposeInjectCommonService <NSObject>

#pragma mark - Tab开关相关

/**
 * 获取Tab开关状态
 * @param key 开关配置的键名
 * @param defaultValue 默认值，当配置不存在时返回此值
 * @return 开关状态，YES表示开启，NO表示关闭
 */
- (BOOL)tmm_getTabToggleIsOnKey:(nonnull NSString *)key defaultValue:(BOOL)defaultValue;

/**
 * 获取Tab开关状态（支持上报）
 * @param key 开关配置的键名
 * @param defaultValue 默认值，当配置不存在时返回此值
 * @param needReport 是否需要上报开关使用情况
 * @return 开关状态，YES表示开启，NO表示关闭
 */
- (BOOL)tmm_getTabToggleIsOnKey:(nonnull NSString *)key defaultValue:(BOOL)defaultValue needReport:(BOOL)needReport;

/**
 * 获取Tab映射配置值
 * @param key 配置键名
 * @return 配置字典，包含键值对映射关系，如果配置不存在则返回nil
 */
- (NSDictionary<NSString *, id> * __nullable)tmm_getTabMapValueKey:(NSString *)key;

#pragma mark - 灰度策略相关

/**
 * 获取配置灰度策略ID
 * @param key 策略配置的键名
 * @param needReport 是否需要上报策略使用情况
 * @return 灰度策略ID，用于标识具体的灰度策略
 */
- (int32_t)tmm_getConfigGrayPolicyIdKey:(NSString *)key needReport:(BOOL)needReport;

/**
 * 获取开关灰度策略ID
 * @param key 策略配置的键名
 * @param needReport 是否需要上报策略使用情况
 * @return 灰度策略ID，用于标识具体的灰度策略
 */
- (int32_t)tmm_getToggleGrayPolicyIdKey:(NSString *)key needReport:(BOOL)needReport;

#pragma mark - 日志相关

/**
 * 记录日志消息
 * @param tag 日志标签，用于标识日志来源或类型
 * @param message 日志内容
 */
- (void)tmm_logMessage:(NSString *)tag message:(NSString *)message;

@end

/**
 * TMM Compose 通用服务注入实现类
 * 采用单例模式，支持依赖注入，作为通用服务的代理转发器
 */
@interface TMMComposeInjectCommonServiceImpl : NSObject <TMMComposeInjectCommonService>

/**
 * 获取单例实例
 * @return 单例对象
 */
+ (instancetype)sharedInstance;

/**
 * 注入通用服务实现
 * @param commonService 实现了TMMComposeInjectCommonService协议的服务对象
 */
- (void)injectCommonService:(id<TMMComposeInjectCommonService>)commonService;

@end


FOUNDATION_EXTERN void OVComposeLog(NSString *tag, NSString *message);

NS_ASSUME_NONNULL_END
