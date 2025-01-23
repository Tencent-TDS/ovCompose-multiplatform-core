/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions

/**
 * A factory for instantiate the enclosing class of AppFunctions. The factory is invoked every time
 * a function call to one of the AppFunction within the class is made.
 *
 * Implement a custom [AppFunctionFactory] if your AppFunction's enclosing class require constructor
 * parameter or custom instantiation logic beyond the default no-argument constructor. This allows
 * you to inject dependencies or handle more complex object creation scenarios.
 *
 * @param T The specific type of AppFunction class this factory creates.
 */
public interface AppFunctionFactory<T : Any> {
    /**
     * Overrides this method to provide your custom creation logic for enclosing class of
     * AppFunctions.
     */
    public fun createEnclosingClass(enclosingClass: Class<T>): T
}
