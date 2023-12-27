/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UITextLoupeSession
import platform.UIKit.UIView

@Stable
internal interface PlatformMagnifierFactory {

    /**
     * If true, passing a different zoom level to [PlatformMagnifier.update] on the
     * [PlatformMagnifier] returned from [create] will actually update the magnifier.
     * If false, a new [PlatformMagnifier] must be created to use a different zoom level.
     */
    val canUpdateZoom: Boolean

    @OptIn(ExperimentalFoundationApi::class)
    fun create(
        style: MagnifierStyle,
        view: UIView,
        density: Density,
        initialZoom: Float
    ): PlatformMagnifier

    companion object {
        @Stable
        fun getForCurrentPlatform(): PlatformMagnifierFactory =
            when {
                !isPlatformMagnifierSupported() -> {
                    throw UnsupportedOperationException(
                        "Magnifier is only supported on API level 28 and higher."
                    )
                }
                else -> PlatformMagnifierFactoryIos17Impl
            }
    }
}

/**
 * Abstraction around the framework [Magnifier] class, for testing.
 */
internal interface PlatformMagnifier {

    /** Returns the actual size of the magnifier widget, even if not specified at creation. */
    val size: IntSize

    /** Causes the magnifier to re-copy the magnified pixels. Wraps [Magnifier.update]. */
    fun updateContent()

    /**
     * Sets the properties on a [Magnifier] instance that can be updated without recreating the
     * magnifier (e.g. [Magnifier.setZoom]) and [shows][Magnifier.show] it.
     */
    fun update(
        sourceCenter: Offset,
        magnifierCenter: Offset,
        zoom: Float
    )

    /** Wraps [Magnifier.dismiss]. */
    fun dismiss()
}

internal object PlatformMagnifierFactoryIos17Impl : PlatformMagnifierFactory {

    override val canUpdateZoom: Boolean = false

    @OptIn(ExperimentalFoundationApi::class)
    override fun create(
        style: MagnifierStyle,
        view: UIView,
        density: Density,
        initialZoom: Float
    ): PlatformMagnifier {
        return PlatformMagnifierImpl(
            density = density.density,
            sessionFactory = {
                requireNotNull(
                    UITextLoupeSession.beginLoupeSessionAtPoint(
                        point = CGPointMake(it.x.toDouble(), it.y.toDouble()),
                        fromSelectionWidgetView = null,
                        inView = view
                    )
                )
            }
        )
    }

    class PlatformMagnifierImpl(
        val density: Float,
        val sessionFactory: (Offset) -> UITextLoupeSession
    ) : PlatformMagnifier {

        // TODO: find exact size of iOS 17 loupe
        override val size: IntSize = IntSize(
            (115 * density).roundToInt(),
            (80 * density).roundToInt()
        )

        override fun updateContent() {
        }

        private var loupeSession : UITextLoupeSession? = null

        override fun update(sourceCenter: Offset, magnifierCenter: Offset, zoom: Float) {

            if (sourceCenter.isUnspecified)
                return
            val pos = (sourceCenter + if (magnifierCenter.isSpecified) magnifierCenter else Offset.Zero) / density
            val session = loupeSession ?: sessionFactory(pos).also {
                loupeSession = it
            }

            val magnifierCenterPoint = CGPointMake(pos.x.toDouble(), pos.y.toDouble())

            session.moveToPoint(
                point = magnifierCenterPoint,
                withCaretRect = CGRectMake(
                    x = pos.x.toDouble() - CarretWidth/2,
                    y = pos.y.toDouble()- CarretHeight/2,
                    width = CarretWidth,
                    height = CarretHeight
                ),
                trackingCaret = true
            )
        }

        override fun dismiss() {
            loupeSession?.invalidate()
            loupeSession = null
        }
    }
}

private val CarretWidth = 1.0
private val CarretHeight = 5.0
