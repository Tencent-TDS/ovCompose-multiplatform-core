/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.xr.extensions;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import androidx.annotation.RequiresOptIn;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation denotes an experimental Extensions API.
 *
 * <p>There are no guarantees on stability of these APIs; they may change over time. By opting-in,
 * users of these APIs assume the risk of breakage as these changes occur.
 */
@Retention(CLASS)
@Target({TYPE, METHOD, CONSTRUCTOR, FIELD})
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public @interface ExperimentalExtensionApi {}
