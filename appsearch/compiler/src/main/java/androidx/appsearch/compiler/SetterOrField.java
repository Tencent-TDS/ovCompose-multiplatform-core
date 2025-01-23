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

package androidx.appsearch.compiler;

import com.google.auto.value.AutoValue;

import org.jspecify.annotations.NonNull;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

/**
 * A setter/field.
 */
@AutoValue
public abstract class SetterOrField {
    /**
     * The setter/field element.
     */
    public abstract @NonNull Element getElement();

    /**
     * The setter/field element's name.
     */
    public @NonNull String getJvmName() {
        return getElement().getSimpleName().toString();
    }

    /**
     * Whether it is a setter.
     */
    public boolean isSetter() {
        return getElement().getKind() == ElementKind.METHOD;
    }

    static @NonNull SetterOrField create(@NonNull Element element) {
        return new AutoValue_SetterOrField(element);
    }
}
