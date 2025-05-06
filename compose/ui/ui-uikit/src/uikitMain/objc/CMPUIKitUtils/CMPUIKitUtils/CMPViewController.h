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

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

// region Tencent Code
typedef NS_ENUM(NSInteger, CMPRenderBackend) {
    CMPRenderBackendSkia,
    CMPRenderBackendUIView,
};
// endregion

@interface CMPViewController : UIViewController

/// Indicates that view controller is considered alive in terms of structural containment.
/// Overriding classes should call super.
- (void)viewControllerDidEnterWindowHierarchy;

/// Indicates that view controller is considered alive in terms of structural containment
/// Overriding classes should call super.
- (void)viewControllerDidLeaveWindowHierarchy;

// region Tencent Code
/// setting the renderBackend for dispose
- (void)setRenderBackend:(CMPRenderBackend)renderBackend;

/// Indicates that view controller is considered for reuse
- (void)viewControllerPrepareForReuse;
// endregion
@end

NS_ASSUME_NONNULL_END
