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

#ifndef ANDROIDX_COMPOSE_UI_ARKUI_UTILS_XCOMPONENT_CONSTANT_H
#define ANDROIDX_COMPOSE_UI_ARKUI_UTILS_XCOMPONENT_CONSTANT_H

#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <EGL/eglplatform.h>

/**
 * Egl red size default.
 */
const int EGL_RED_SIZE_DEFAULT = 8;

/**
 * Egl green size default.
 */
const int EGL_GREEN_SIZE_DEFAULT = 8;

/**
 * Egl blue size default.
 */
const int EGL_BLUE_SIZE_DEFAULT = 8;

/**
 * Egl alpha size default.
 */
const int EGL_ALPHA_SIZE_DEFAULT = 8;

/**
 * Config attribute list.
 */
const EGLint ATTRIB_LIST[] = {EGL_SURFACE_TYPE, EGL_WINDOW_BIT,         EGL_RED_SIZE,        EGL_RED_SIZE_DEFAULT,
                              EGL_GREEN_SIZE,   EGL_GREEN_SIZE_DEFAULT, EGL_BLUE_SIZE,       EGL_BLUE_SIZE_DEFAULT,
                              EGL_ALPHA_SIZE,   EGL_ALPHA_SIZE_DEFAULT, EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                              EGL_NONE};

/**
 * Context attributes.
 */
const EGLint CONTEXT_ATTRIBS[] = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE};

#endif // ANDROIDX_COMPOSE_UI_ARKUI_UTILS_XCOMPONENT_CONSTANT_H
