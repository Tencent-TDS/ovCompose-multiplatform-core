/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 Tencent. All rights reserved.
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

#ifndef ANDROIDX_COMPOSE_UI_ARKUI_LEFTOVER_C_ABI_NULL_GUARD_H
#define ANDROIDX_COMPOSE_UI_ARKUI_LEFTOVER_C_ABI_NULL_GUARD_H

/*
 * Leftover C ABI null policy for teardown / missing XComponent.
 *
 * Header-only and free of Harmony / NAPI includes so host tests can compile
 * the leftover `if (!p) return false` helper without a device SDK.
 *
 * Production C ABI uses leftover_c_abi_if_non_null before Harmony-only work.
 * Thin leftover_* wrappers expose that leftover policy to host tests.
 */

#ifdef __cplusplus
extern "C" {
#endif

/* Leftover helper: if (!p) return false. */
static inline int leftover_c_abi_if_non_null(const void *p) {
    if (!p) {
        return 0;
    }
    return 1;
}

/* leftover: prepareDraw(nullptr) / finishDraw(nullptr) return false. */
static inline int leftover_prepareDraw(void *render) {
    if (!leftover_c_abi_if_non_null(render)) {
        return 0;
    }
    return 1;
}

static inline int leftover_finishDraw(void *render) {
    if (!leftover_c_abi_if_non_null(render)) {
        return 0;
    }
    return 1;
}

/* leftover: register/unregister on nullptr are no-ops. */
static inline void leftover_registerFrameCallback(void *render) {
    if (!leftover_c_abi_if_non_null(render)) {
        return;
    }
}

static inline void leftover_unregisterFrameCallback(void *render) {
    if (!leftover_c_abi_if_non_null(render)) {
        return;
    }
}

/* leftover: sendMessage(nullptr, ...) returns nullptr. */
static inline const char *leftover_sendMessage(void *controller, const char *type, const char *message) {
    (void)type;
    (void)message;
    if (!leftover_c_abi_if_non_null(controller)) {
        return 0;
    }
    return 0;
}

#ifdef __cplusplus
}
#endif

#endif /* ANDROIDX_COMPOSE_UI_ARKUI_LEFTOVER_C_ABI_NULL_GUARD_H */
