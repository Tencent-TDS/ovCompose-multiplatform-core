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

/*
 * Host leftover C ABI tests. Compiles without Harmony / NAPI: the leftover
 * helper and leftover wrappers live in leftover_c_abi_null_guard.h.
 */

#include "leftover_c_abi_null_guard.h"

#include <cstdio>
#include <cstdlib>

static void leftover_fail(const char *what) {
    std::fprintf(stderr, "leftover_c_abi_null_guard_test failed: %s\n", what);
    std::abort();
}

int main() {
    /* leftover: prepareDraw(nullptr) / finishDraw(nullptr) return false, do not crash */
    if (leftover_prepareDraw(nullptr) != 0) {
        leftover_fail("leftover_prepareDraw(nullptr) must return false");
    }
    if (leftover_finishDraw(nullptr) != 0) {
        leftover_fail("leftover_finishDraw(nullptr) must return false");
    }

    /* leftover: register/unregister on nullptr are no-ops */
    leftover_registerFrameCallback(nullptr);
    leftover_unregisterFrameCallback(nullptr);

    /* leftover: sendMessage(nullptr, ...) returns nullptr */
    if (leftover_sendMessage(nullptr, "type", "message") != nullptr) {
        leftover_fail("leftover_sendMessage(nullptr, ...) must return nullptr");
    }

    if (leftover_c_abi_if_non_null(nullptr) != 0) {
        leftover_fail("leftover_c_abi_if_non_null(nullptr) must return false");
    }

    std::printf("leftover_c_abi_null_guard_test: ok\n");
    return 0;
}
