/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.scenecore

import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.concurrent.futures.ResolvableFuture
import androidx.xr.runtime.math.Vector4
import androidx.xr.scenecore.JxrPlatformAdapter.MaterialResource as RtMaterialResource
import com.google.common.util.concurrent.ListenableFuture

/** A Material which implements a water effect. */
// TODO(b/396201066): Add unit tests for this class if we end up making it public.
@Suppress("NotCloseable")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class WaterMaterial
internal constructor(public val material: RtMaterialResource, public val session: Session) {

    /**
     * Disposes the given water material resource.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * Currently, a glTF model (which this material will be used with) can't be disposed. This means
     * that calling dispose on the material will lead to a crash if the call is made out of order,
     * that is, if the material is disposed before the glTF model that uses it.
     */
    // TODO(b/376277201): Provide Session.GltfModel.dispose().
    @MainThread
    public fun dispose() {
        session.platformAdapter.destroyWaterMaterial(material)
    }

    /**
     * Sets the reflection cube texture for the water material.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param reflectionCube The [CubeMapTexture] to be used as the reflection cube.
     */
    @MainThread
    public fun setReflectionCube(reflectionCube: CubeMapTexture) {
        session.platformAdapter.setReflectionCube(material, reflectionCube.texture)
    }

    /**
     * Sets the normal map texture for the water material.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param normalMap The [Texture] to be used as the normal map.
     */
    @MainThread
    public fun setNormalMap(normalMap: Texture) {
        session.platformAdapter.setNormalMap(material, normalMap.texture)
    }

    /**
     * Sets the normal tiling for the water material.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param normalTiling The tiling of the normal map.
     */
    @MainThread
    public fun setNormalTiling(normalTiling: Float) {
        session.platformAdapter.setNormalTiling(material, normalTiling)
    }

    /**
     * Sets the normal speed for the water material.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param normalSpeed The speed of the normal map.
     */
    @MainThread
    public fun setNormalSpeed(normalSpeed: Float) {
        session.platformAdapter.setNormalSpeed(material, normalSpeed)
    }

    /**
     * Sets the alpha step U for the water material.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param alphaStepU The alpha step U.
     */
    @MainThread
    public fun setAlphaStepU(alphaStepU: Vector4) {
        session.platformAdapter.setAlphaStepU(material, alphaStepU)
    }

    /**
     * Sets the alpha step V for the water material.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param alphaStepV The alpha step V.
     */
    @MainThread
    public fun setAlphaStepV(alphaStepV: Vector4) {
        session.platformAdapter.setAlphaStepV(material, alphaStepV)
    }

    /**
     * Sets the alpha step multiplier for the water material.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param alphaStepMultiplier The alpha step multiplier.
     */
    @MainThread
    public fun setAlphaStepMultiplier(alphaStepMultiplier: Float) {
        session.platformAdapter.setAlphaStepMultiplier(material, alphaStepMultiplier)
    }

    public companion object {
        // ResolvableFuture is marked as RestrictTo(LIBRARY_GROUP_PREFIX), which is intended for
        // classes
        // within AndroidX. We're in the process of migrating to AndroidX. Without suppressing this
        // warning, however, we get a build error - go/bugpattern/RestrictTo.
        @SuppressWarnings("RestrictTo")
        internal fun createAsync(
            platformAdapter: JxrPlatformAdapter,
            transparent: Boolean,
            session: Session,
        ): ListenableFuture<WaterMaterial> {
            val materialResourceFuture = platformAdapter.createWaterMaterial(transparent)
            val materialFuture = ResolvableFuture.create<WaterMaterial>()

            // TODO: b/375070346 - remove this `!!` when we're sure the future is non-null.
            materialResourceFuture!!.addListener(
                {
                    try {
                        val material = materialResourceFuture.get()
                        materialFuture.set(WaterMaterial(material, session))
                    } catch (e: Exception) {
                        if (e is InterruptedException) {
                            Thread.currentThread().interrupt()
                        }
                        materialFuture.setException(e)
                    }
                },
                Runnable::run,
            )
            return materialFuture
        }

        /**
         * Public factory function for a [WaterMaterial], where the material is asynchronously
         * loaded.
         *
         * This method must be called from the main thread.
         * https://developer.android.com/guide/components/processes-and-threads
         *
         * Currently, only URLs and relative paths from the android_assets/ directory are supported.
         *
         * @param session The [Session] to use for loading the model.
         * @param transparent If the water material should have transparency or not.
         * @return a ListenableFuture<WaterMaterial>. Listeners will be called on the main thread if
         *   Runnable::run is supplied.
         */
        @MainThread
        @JvmStatic
        public fun create(session: Session, transparent: Boolean): ListenableFuture<WaterMaterial> {
            return WaterMaterial.createAsync(session.platformAdapter, transparent, session)
        }
    }
}
