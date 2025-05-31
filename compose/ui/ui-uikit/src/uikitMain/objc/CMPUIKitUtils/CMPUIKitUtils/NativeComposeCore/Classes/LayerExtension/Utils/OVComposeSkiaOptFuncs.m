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

#import <CoreText/CoreText.h>
#import <UIKit/UIKit.h>
#include <dlfcn.h>
 
/**
 * CTFontCopyVariationAxes provides the localized name of all axes, making it very slow.
 * This is unfortunate, its result is needed just to see if there are any axes at all.
 * To avoid calling internal APIs cache the result of CTFontCopyVariationAxes.
 * https://github.com/WebKit/WebKit/commit/1842365d413ed87868e7d33d4fad1691fa3a8129
 * https://bugs.webkit.org/show_bug.cgi?id=232690
 */

typedef CFArrayRef (*CTFontCopyVariationAxesInternalFunc)(CTFontRef font);

extern CFArrayRef OVMPCTFontCopyVariationAxesInternal(CTFontRef font) {
    static dispatch_once_t onceToken;
    static CTFontCopyVariationAxesInternalFunc _CTFontCopyVariationAxesInternalFunc = NULL;
    dispatch_once(&onceToken, ^{
        void *handle = dlopen("/System/Library/Frameworks/CoreText.framework/CoreText", RTLD_LAZY);
        if (handle) {
            _CTFontCopyVariationAxesInternalFunc = (CTFontCopyVariationAxesInternalFunc)dlsym(handle, "CTFontCopyVariationAxesInternal");
            dlclose(handle);
        }
    });
    
    if (_CTFontCopyVariationAxesInternalFunc != NULL) {
        return _CTFontCopyVariationAxesInternalFunc(font);
    }
    return CTFontCopyVariationAxes(font);
}
