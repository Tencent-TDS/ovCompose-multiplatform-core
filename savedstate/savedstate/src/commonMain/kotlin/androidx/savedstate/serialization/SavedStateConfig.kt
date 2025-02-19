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

package androidx.savedstate.serialization

import androidx.savedstate.serialization.SavedStateConfig.Builder
import androidx.savedstate.serialization.serializers.SavedStateSerializer
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlinx.serialization.Contextual
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.overwriteWith
import kotlinx.serialization.modules.plus

/**
 * Configuration of the current [SavedStateConfig] configured with [SavedStateConfig.Builder].
 *
 * Can be used via [encodeToSavedState] and [decodeFromSavedState].
 *
 * Standalone configuration object cannot be used outside the encode and decode functions provided
 * by SavedState.
 *
 * Detailed description of each property is available in [SavedStateConfig.Builder] class.
 *
 * @see SavedStateConfig.Builder
 */
public class SavedStateConfig
private constructor(
    public val serializersModule: SerializersModule = DEFAULT_SERIALIZERS_MODULE,
    @ClassDiscriminatorMode.Definition
    public val classDiscriminatorMode: Int = ClassDiscriminatorMode.POLYMORPHIC,
) {
    /**
     * Builder of the [SavedStateConfig] instance provided by `SavedStateConfig { ... }` factory
     * function.
     */
    @Suppress("MissingBuildMethod") // `build()` is internal, only used by the DSL.
    public class Builder internal constructor(config: SavedStateConfig) {

        /**
         * Module with contextual and polymorphic serializers to be used in the resulting
         * [SavedStateConfig] instance.
         *
         * @see SerializersModule
         * @see Contextual
         * @see Polymorphic
         */
        @get:Suppress("GetterOnBuilder") // Kotlin issue: KT-3110 (private get with public set).
        @set:Suppress("SetterReturnsThis") // DSL-like builder, no need to return this.
        public var serializersModule: SerializersModule = config.serializersModule

        /**
         * Defines which classes and objects should have class discriminator added to the output.
         * [ClassDiscriminatorMode.POLYMORPHIC] by default.
         *
         * @see ClassDiscriminatorMode
         */
        @get:Suppress("GetterOnBuilder") // Kotlin issue: KT-3110 (private get with public set).
        @set:Suppress("SetterReturnsThis") // DSL-like builder, no need to return this.
        @ClassDiscriminatorMode.Definition
        public var classDiscriminatorMode: Int = config.classDiscriminatorMode

        internal fun build(): SavedStateConfig {
            return SavedStateConfig(
                serializersModule = DEFAULT_SERIALIZERS_MODULE.overwriteWith(serializersModule),
                classDiscriminatorMode = classDiscriminatorMode,
            )
        }
    }

    public companion object {
        /**
         * The default instance of [SavedStateConfig] with default configuration.
         *
         * This configuration is used by [encodeToSavedState] and [decodeFromSavedState] unless an
         * alternative configuration is explicitly provided.
         *
         * @see encodeToSavedState
         * @see decodeFromSavedState
         */
        @JvmField public val DEFAULT: SavedStateConfig = SavedStateConfig()
    }
}

/**
 * Creates an instance of [SavedStateConfig] configured from the optionally given [from] and
 * adjusted with [builderAction].
 *
 * @sample androidx.savedstate.config
 * @param from An optional initial [SavedStateConfig] to start with. Defaults to
 *   [SavedStateConfig.DEFAULT].
 * @param builderAction A lambda function to configure the [Builder] for additional customization.
 * @return A new [SavedStateConfig] instance configured based on the provided parameters.
 */
@JvmOverloads
public fun SavedStateConfig(
    from: SavedStateConfig = SavedStateConfig.DEFAULT,
    builderAction: Builder.() -> Unit,
): SavedStateConfig {
    return Builder(from).apply(builderAction).build()
}

internal expect fun getDefaultSerializersModuleOnPlatform(): SerializersModule

private val DEFAULT_SERIALIZERS_MODULE: SerializersModule =
    SerializersModule { contextual(SavedStateSerializer) } + getDefaultSerializersModuleOnPlatform()
