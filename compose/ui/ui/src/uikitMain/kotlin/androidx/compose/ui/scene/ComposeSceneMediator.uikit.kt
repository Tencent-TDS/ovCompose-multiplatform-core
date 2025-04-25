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

package androidx.compose.ui.scene

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.animation.withAnimationProgress
import androidx.compose.ui.backhandler.UIKitBackGestureDispatcher
import androidx.compose.ui.draganddrop.UIKitDragAndDropManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.PointerKeyboardModifiers
import androidx.compose.ui.input.key.toComposeEvent
import androidx.compose.ui.input.pointer.HistoricalChange
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.layout.OffsetToFocusedRect
import androidx.compose.ui.platform.AccessibilityMediator
import androidx.compose.ui.platform.CUPERTINO_TOUCH_SLOP
import androidx.compose.ui.platform.DefaultInputModeManager
import androidx.compose.ui.platform.EmptyViewConfiguration
import androidx.compose.ui.platform.LocalLayoutMargins
import androidx.compose.ui.platform.LocalSafeArea
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.PlatformScreenReader
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.platform.UIKitTextInputService
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.lerp
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.uikit.LocalKeyboardOverlapHeight
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.uikit.density
import androidx.compose.ui.uikit.embedSubview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.asDpOffset
import androidx.compose.ui.unit.asDpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toPlatformInsets
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.viewinterop.LocalInteropContainer
import androidx.compose.ui.viewinterop.TrackInteropPlacementContainer
import androidx.compose.ui.viewinterop.UIKitInteropContainer
import androidx.compose.ui.viewinterop.UIKitInteropTransaction
import androidx.compose.ui.window.ApplicationForegroundStateListener
import androidx.compose.ui.window.ComposeSceneKeyboardOffsetManager
import androidx.compose.ui.window.FocusStack
import androidx.compose.ui.window.KeyboardVisibilityListener
import androidx.compose.ui.window.MetalRedrawer
import androidx.compose.ui.window.TouchesEventKind
import androidx.compose.ui.window.UserInputView
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available
import platform.CoreGraphics.CGPoint
import platform.QuartzCore.CACurrentMediaTime
import platform.QuartzCore.CATransaction
import platform.UIKit.UIEvent
import platform.UIKit.UIEventButtonMaskPrimary
import platform.UIKit.UIEventButtonMaskSecondary
import platform.UIKit.UIPress
import platform.UIKit.UITouch
import platform.UIKit.UITouchPhase
import platform.UIKit.UITouchTypeDirect
import platform.UIKit.UITouchTypeIndirect
import platform.UIKit.UITouchTypeIndirectPointer
import platform.UIKit.UITouchTypePencil
import platform.UIKit.UIView

/**
 * iOS specific-implementation of [PlatformContext.SemanticsOwnerListener] used to track changes in [SemanticsOwner].
 *
 * @property view The UI container associated with the semantics owner.
 * @property coroutineContext The coroutine context to use for handling semantics changes.
 * @property performEscape A lambda to delegate accessibility escape operation. Returns true if the escape was handled, false otherwise.
 */
private class SemanticsOwnerListenerImpl(
    private val view: UIView,
    private val coroutineContext: CoroutineContext,
    private val performEscape: () -> Boolean,
    private val onKeyboardPresses: (Set<*>) -> Unit,
    private val onScreenReaderActive: (Boolean) -> Unit,
) : PlatformContext.SemanticsOwnerListener {

    private var accessibilityMediator: AccessibilityMediator? = null

    var isEnabled: Boolean = false
        set(value) {
            field = value
            accessibilityMediator?.isEnabled = value
        }

    override fun onSemanticsOwnerAppended(semanticsOwner: SemanticsOwner) {
        if (accessibilityMediator == null) {
            accessibilityMediator = AccessibilityMediator(
                view,
                semanticsOwner,
                coroutineContext,
                performEscape,
                onKeyboardPresses,
                onScreenReaderActive
            ).also {
                it.isEnabled = isEnabled
            }
        }
    }

    override fun onSemanticsOwnerRemoved(semanticsOwner: SemanticsOwner) {
        if (accessibilityMediator?.owner == semanticsOwner) {
            accessibilityMediator?.dispose()
            accessibilityMediator = null
            onScreenReaderActive(false)
        }
    }

    override fun onSemanticsChange(semanticsOwner: SemanticsOwner) {
        if (accessibilityMediator?.owner == semanticsOwner) {
            accessibilityMediator?.onSemanticsChange()
        }
    }

    override fun onLayoutChange(semanticsOwner: SemanticsOwner, semanticsNodeId: Int) {
        if (accessibilityMediator?.owner == semanticsOwner) {
            accessibilityMediator?.onLayoutChange(nodeId = semanticsNodeId)
        }
    }

    val hasInvalidations: Boolean get() = accessibilityMediator?.hasPendingInvalidations ?: false

    fun dispose() {
        accessibilityMediator?.dispose()
        accessibilityMediator = null
    }
}

internal class ComposeSceneMediator(
    parentView: UIView,
    interopContainerView: UIView,
    private val onFocusBehavior: OnFocusBehavior,
    private val focusStack: FocusStack?,
    private val windowContext: PlatformWindowContext,
    private val coroutineContext: CoroutineContext,
    private val redrawer: MetalRedrawer,
    private val backGestureDispatcher: UIKitBackGestureDispatcher,
    composeSceneFactory: (
        invalidate: () -> Unit,
        platformContext: PlatformContext
    ) -> ComposeScene
) {
    private var onPreviewKeyEvent: (KeyEvent) -> Boolean = { false }

    private var onKeyEvent: (KeyEvent) -> Boolean = { false }

    private var keyboardOverlapHeight by mutableStateOf(0.dp)
    private var animateKeyboardOffsetChanges by mutableStateOf(false)
    private var platformScreenReader = object : PlatformScreenReader {
        override var isActive by mutableStateOf(false)
    }

    private var disposed = false

    private val viewConfiguration: ViewConfiguration =
        object : ViewConfiguration by EmptyViewConfiguration {
            override val touchSlop: Float
                get() = with(density) {
                    // this value is originating from iOS 16 drag behavior reverse engineering
                    CUPERTINO_TOUCH_SLOP.dp.toPx()
                }
        }

    private val scene: ComposeScene by lazy {
        composeSceneFactory(
            redrawer::setNeedsRedraw,
            PlatformContextImpl()
        )
    }

    private var size: IntSize?
        get() = scene.size
        set(value) {
            if (!disposed) {
                scene.size = value
            }
        }

    var density: Density
        get() = scene.density
        set(value) {
            if (!disposed) {
                scene.density = value
            }
        }

    var layoutDirection: LayoutDirection
        get() = scene.layoutDirection
        set(value) {
            if (!disposed) {
                scene.layoutDirection = value
            }
        }

    var compositionLocalContext: CompositionLocalContext?
        get() = scene.compositionLocalContext
        set(value) {
            if (!disposed) {
                scene.compositionLocalContext = value
            }
        }

    private val applicationForegroundStateListener =
        ApplicationForegroundStateListener { _ ->
            // Sometimes the application can trigger animation and go background before the animation is
            // finished. The scheduled GPU work is performed, but no presentation can be done, causing
            // mismatch between visual state and application state. This can be fixed by forcing
            // a redraw when app returns to foreground, which will ensure that the visual state is in
            // sync with the application state even if such sequence of events took a place.
            redrawer.setNeedsRedraw()
        }

    /**
     * View wrapping the hierarchy managed by this Mediator.
     */
    private val view = UIKitTransparentContainerView(
        onLayoutSubviews = ::updateLayout
    )

    /**
     * View that handles the user input events and hosts interop views.
     */
    private val userInputView = UserInputView(
        ::hitTestInteropView,
        ::isPointInsideInteractionBounds,
        ::onTouchesEvent,
        ::onCancelAllTouches,
        ::onScrollEvent,
        ::onCancelScroll,
        ::onHoverEvent,
        ::onKeyboardPresses,
        backGestureDispatcher::isBackGestureActive
    )

    /**
     * Container for managing UIKitView and UIKitViewController
     */
    private val interopContainer = UIKitInteropContainer(
        root = userInputView,
        requestRedraw = redrawer::setNeedsRedraw
    )

    var interactionBounds = IntRect.Zero

    private val dragAndDropManager = UIKitDragAndDropManager(
        view = userInputView,
        getComposeRootDragAndDropNode = { scene.rootDragAndDropNode },
    )

    /**
     * A callback to define whether the precondition for the user input view hit test is met.
     *
     * @param point Point in the interaction view coordinate space.
     */
    private fun isPointInsideInteractionBounds(point: CValue<CGPoint>) =
        interactionBounds.contains(point.asDpOffset().toOffset(view.density).round())

    private val semanticsOwnerListener by lazy {
        SemanticsOwnerListenerImpl(
            view = view,
            coroutineContext = coroutineContext,
            performEscape = {
                val down = onKeyboardEvent(KeyEvent(Key.Escape, KeyEventType.KeyDown))
                val up = onKeyboardEvent(KeyEvent(Key.Escape, KeyEventType.KeyUp))

                down || up
            },
            onKeyboardPresses = ::onKeyboardPresses,
            onScreenReaderActive = { platformScreenReader.isActive = it }
        )
    }

    var isAccessibilityEnabled by semanticsOwnerListener::isEnabled

    private val keyboardManager by lazy {
        ComposeSceneKeyboardOffsetManager(
            view = view,
            keyboardOverlapHeightChanged = { height ->
                if (keyboardOverlapHeight != height) {
                    animateKeyboardOffsetChanges = false
                    keyboardOverlapHeight = height
                }
            }
        )
    }

    private val textInputService: UIKitTextInputService by lazy {
        UIKitTextInputService(
            updateView = {
                redrawer.setNeedsRedraw()
                CATransaction.flush() // clear all animations
            },
            view = view,
            viewConfiguration = viewConfiguration,
            focusStack = focusStack,
            onInputStarted = {
                animateKeyboardOffsetChanges = true
            },
            onKeyboardPresses = ::onKeyboardPresses,
            focusManager = { scene.focusManager }
        ).also {
            KeyboardVisibilityListener.initialize()
        }
    }

    val hasInvalidations: Boolean
        get() = scene.hasInvalidations() ||
            keyboardManager.isAnimating ||
            isLayoutTransitionAnimating ||
            semanticsOwnerListener.hasInvalidations ||
            textInputService.hasInvalidations

    private fun hitTestInteropView(point: CValue<CGPoint>): UIView? =
        point.useContents {
            val position = asDpOffset().toOffset(density)
            val interopView = scene.hitTestInteropView(position)

            // Find a group of a holder associated with a given interop view or view controller
            interopView?.let {
                interopContainer.groupForInteropView(it)
            }
        }

    fun onScrollEvent(
        position: DpOffset,
        delta: DpOffset,
        event: UIEvent?,
        eventKind: TouchesEventKind
    ) {
        when (eventKind) {
            TouchesEventKind.BEGAN -> redrawer.ongoingInteractionEventsCount += 1
            TouchesEventKind.MOVED -> {}
            TouchesEventKind.ENDED -> redrawer.ongoingInteractionEventsCount -= 1
        }

        scene.sendPointerEvent(
            eventType = PointerEventType.Scroll,
            pointers = listOf(
                ComposeScenePointer(
                    id = PointerId(0),
                    position = position.toOffset(density),
                    pressed = false,
                    type = PointerType.Mouse,
                )
            ),
            scrollDelta = delta.toOffset(density) * SCROLL_DELTA_MULTIPLIER,
            timeMillis = event.timeMillis,
            nativeEvent = event,
            keyboardModifiers = PointerKeyboardModifiers(event.modifierFlagsOrZero)
        )
    }

    fun onHoverEvent(
        position: DpOffset,
        event: UIEvent?,
        eventKind: TouchesEventKind
    ) {
        val eventType = when (eventKind) {
            TouchesEventKind.BEGAN -> PointerEventType.Enter
            TouchesEventKind.MOVED -> PointerEventType.Move
            TouchesEventKind.ENDED -> PointerEventType.Exit
        }

        scene.sendPointerEvent(
            eventType = eventType,
            pointers = listOf(
                ComposeScenePointer(
                    id = PointerId(0),
                    position = position.toOffset(density),
                    pressed = false,
                    type = PointerType.Mouse,
                )
            ),
            timeMillis = event.timeMillis,
            nativeEvent = event,
            keyboardModifiers = PointerKeyboardModifiers(event.modifierFlagsOrZero)
        )
    }

    fun onCancelScroll() {
        redrawer.ongoingInteractionEventsCount -= 1
        scene.cancelPointerInput()
    }

    private fun onCancelAllTouches(touches: Set<*>) {
        redrawer.ongoingInteractionEventsCount -= touches.count()
        scene.cancelPointerInput()
    }

    /**
     * Converts [UITouch] objects from [touches] to [ComposeScenePointer] and dispatches them to the appropriate handlers.
     * @param touches a [Set] of [UITouch] objects. Erasure happens due to K/N not supporting Obj-C lightweight generics.
     * @param event the [UIEvent] associated with the touches
     * @param eventKind the [TouchesEventKind] of the touches
     */
    private fun onTouchesEvent(
        touches: Set<*>,
        event: UIEvent?,
        eventKind: TouchesEventKind
    ): PointerEventResult {
        when (eventKind) {
            TouchesEventKind.BEGAN -> redrawer.ongoingInteractionEventsCount += touches.count()
            TouchesEventKind.ENDED -> redrawer.ongoingInteractionEventsCount -= touches.count()
            TouchesEventKind.MOVED -> {}
        }

        val pointers = touches.mapIndexed { index, touch ->
            touch as UITouch
            val position = touch.offsetInView(userInputView, density.density)
            val pointerType = when (touch.type) {
                UITouchTypeDirect -> PointerType.Touch
                UITouchTypeIndirect, UITouchTypeIndirectPointer -> PointerType.Mouse
                UITouchTypePencil -> PointerType.Stylus
                else -> PointerType.Touch
            }
            val id = touch.hashCode().toLong().takeIf {
                pointerType != PointerType.Mouse
            } ?: index.toLong()
            ComposeScenePointer(
                id = PointerId(id),
                position = position,
                pressed = touch.isPressed,
                type = pointerType,
                pressure = touch.force.toFloat(),
                historical = event?.historicalChangesForTouch(
                    touch,
                    view,
                    density.density
                ) ?: emptyList()
            )
        }

        // UIKit sends buttonMask that was before the release action. It should be empty if no
        // pressed pointers left.
        val pointerButtonsMask = event.buttonMaskOrZero.takeIf {
            pointers.any { it.pressed }
        } ?: 0L

        return scene.sendPointerEvent(
            eventType = eventKind.toPointerEventType(),
            pointers = pointers,
            timeMillis = event.timeMillis,
            nativeEvent = event,
            button = event?.getButton(previousButtonMask, eventKind, previousTouchEventKind),
            buttons = PointerButtons(pointerButtonsMask),
            keyboardModifiers = PointerKeyboardModifiers(event.modifierFlagsOrZero)
        ).also {
            previousButtonMask = event.buttonMaskOrZero
            if (eventKind != TouchesEventKind.MOVED) {
                previousTouchEventKind = eventKind
            }
        }
    }
    private var previousButtonMask: Long = 0L
    private var previousTouchEventKind: TouchesEventKind? = null

    init {
        parentView.embedSubview(view)
        interopContainerView.embedSubview(userInputView)
    }

    private var lastFocusedRect: Rect? = null
    private fun getFocusedRect(): Rect? {
        return scene.focusManager.getFocusRect()?.also {
            lastFocusedRect = it
        } ?: lastFocusedRect
    }

    fun setContent(content: @Composable () -> Unit) {
        view.runOnceOnAppeared {
            focusStack?.pushAndFocus(userInputView)

            scene.setContent {
                ProvideComposeSceneMediatorCompositionLocals {
                    FocusAboveKeyboardIfNeeded {
                        interopContainer.TrackInteropPlacementContainer(content = content)
                    }
                }
            }
        }
    }

    private var isLayoutTransitionAnimating = false
    fun prepareAndGetSizeTransitionAnimation(): suspend (Duration) -> Unit {
        isLayoutTransitionAnimating = true

        val initialLayoutMargins = layoutMargins
        val initialSafeArea = safeArea
        val initialSize = scene.size?.toSize() ?: return {}

        return { duration ->
            try {
                if (initialSize != currentViewSize) {
                    withAnimationProgress(duration) { progress ->
                        layoutMargins = lerp(
                            start = initialLayoutMargins,
                            stop = view.layoutMargins.toPlatformInsets(),
                            fraction = progress
                        )
                        safeArea = lerp(
                            start = initialSafeArea,
                            stop = view.safeAreaInsets.toPlatformInsets(),
                            fraction = progress
                        )
                        size = lerp(
                            start = initialSize,
                            stop = currentViewSize,
                            fraction = progress
                        ).roundToIntSize()
                    }
                }
            } finally {
                isLayoutTransitionAnimating = false
                updateLayout()
            }
        }
    }

    fun render(canvas: Canvas, nanoTime: Long) {
        textInputService.flushEditCommandsIfNeeded(force = true)
        scene.render(canvas, nanoTime)
    }

    fun retrieveInteropTransaction(): UIKitInteropTransaction =
        interopContainer.retrieveTransaction()

    private var safeArea by mutableStateOf(PlatformInsets.Zero)

    private var layoutMargins by mutableStateOf(PlatformInsets.Zero)

    @Composable
    private fun ProvideComposeSceneMediatorCompositionLocals(content: @Composable () -> Unit) =
        CompositionLocalProvider(
            LocalInteropContainer provides interopContainer,
            LocalKeyboardOverlapHeight provides keyboardOverlapHeight,
            LocalSafeArea provides safeArea,
            LocalLayoutMargins provides layoutMargins,
            content = content
        )

    @Composable
    private fun FocusAboveKeyboardIfNeeded(content: @Composable () -> Unit) {
        if (onFocusBehavior == OnFocusBehavior.FocusableAboveKeyboard) {
            OffsetToFocusedRect(
                insets = PlatformInsets(bottom = keyboardOverlapHeight),
                getFocusedRect = ::getFocusedRect,
                size = scene.size,
                animationDuration = if (animateKeyboardOffsetChanges) {
                    FOCUS_CHANGE_ANIMATION_DURATION
                } else {
                    0.seconds
                },
                animationCompletion = {
                    animateKeyboardOffsetChanges = false
                },
                content = content
            )
        } else {
            content()
        }
    }

    fun dispose() {
        disposed = true
        onPreviewKeyEvent = { false }
        onKeyEvent = { false }

        view.dispose()
        textInputService.stopInput()
        applicationForegroundStateListener.dispose()
        focusStack?.popUntilNext(userInputView)
        keyboardManager.dispose()
        userInputView.dispose()

        view.removeFromSuperview()
        userInputView.removeFromSuperview()

        scene.close()
        interopContainer.dispose()
        semanticsOwnerListener.dispose()
    }

    /**
     * Updates the [ComposeScene] with the properties derived from the [view].
     */
    private fun updateLayout() {
        density = view.density

        if (isLayoutTransitionAnimating) {
            return
        }
        layoutMargins = view.layoutMargins.toPlatformInsets()
        safeArea = view.safeAreaInsets.toPlatformInsets()

        size = currentViewSize.roundToIntSize()
    }

    private val currentViewSize: Size get() {
        return with(density) {
            view.frame.useContents { size.asDpSize() }.toSize()
        }
    }

    fun sceneDidAppear() {
        redrawer.setNeedsRedraw()
        keyboardManager.start()
    }

    fun sceneWillDisappear() {
        keyboardManager.stop()
    }

    fun setKeyEventListener(
        onPreviewKeyEvent: ((KeyEvent) -> Boolean)?,
        onKeyEvent: ((KeyEvent) -> Boolean)?
    ) {
        this.onPreviewKeyEvent = onPreviewKeyEvent ?: { false }
        this.onKeyEvent = onKeyEvent ?: { false }
    }

    /**
     * Converts [UIPress] objects to [KeyEvent] and dispatches them to the appropriate handlers.
     * @param presses a [Set] of [UIPress] objects. Erasure happens due to K/N not supporting Obj-C lightweight generics.
     */
    private fun onKeyboardPresses(presses: Set<*>) {
        presses.forEach {
            val press = it as UIPress
            onKeyboardEvent(press.toComposeEvent())
        }
    }

    private fun onKeyboardEvent(keyEvent: KeyEvent): Boolean =
        textInputService.onPreviewKeyEvent(keyEvent) // TODO: fix redundant call
            || onPreviewKeyEvent(keyEvent)
            || scene.sendKeyEvent(keyEvent)
            || backGestureDispatcher.onKeyEvent(keyEvent)
            || onKeyEvent(keyEvent)

    private inner class PlatformContextImpl : PlatformContext {
        override val windowInfo: WindowInfo get() = windowContext.windowInfo
        override val screenReader: PlatformScreenReader get() = platformScreenReader

        override fun convertLocalToWindowPosition(localPosition: Offset): Offset =
            windowContext.convertLocalToWindowPosition(view, localPosition)

        override fun convertWindowToLocalPosition(positionInWindow: Offset): Offset =
            windowContext.convertWindowToLocalPosition(view, positionInWindow)

        override fun convertLocalToScreenPosition(localPosition: Offset): Offset =
            windowContext.convertLocalToScreenPosition(view, localPosition)

        override fun convertScreenToLocalPosition(positionOnScreen: Offset): Offset =
            windowContext.convertScreenToLocalPosition(view, positionOnScreen)

        override val viewConfiguration get() = this@ComposeSceneMediator.viewConfiguration
        override val inputModeManager = DefaultInputModeManager(InputMode.Touch)
        override val textInputService get() = this@ComposeSceneMediator.textInputService
        override val textToolbar get() = this@ComposeSceneMediator.textInputService
        override val semanticsOwnerListener get() = this@ComposeSceneMediator.semanticsOwnerListener
        override val dragAndDropManager get() = this@ComposeSceneMediator.dragAndDropManager

        override suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing {
            // TODO: Adopt PlatformTextInputService2 (https://youtrack.jetbrains.com/issue/CMP-7832/iOS-Adopt-PlatformTextInputService2)
            coroutineScope {
                launch {
                    request.outputValue.collect {
                        textInputService.updateState(oldValue = null, newValue = it)
                    }
                }
                launch {
                    request.textLayoutResult.collect {
                        textInputService.updateTextLayoutResult(it)
                    }
                }
                launch {
                    request.textFieldRectInRoot.collect {
                        textInputService.updateTextFrame(it)
                    }
                }
                suspendCancellableCoroutine<Nothing> { continuation ->
                    textInputService.startInput(
                        value = request.value(),
                        imeOptions = request.imeOptions,
                        onEditCommand = request.onEditCommand,
                        onImeActionPerformed = request.onImeAction ?: {}
                    )

                    continuation.invokeOnCancellation {
                        textInputService.stopInput()
                    }
                }
            }
        }
    }
}

private fun UIEvent.getButton(
    previousButtonMask: Long,
    eventKind: TouchesEventKind,
    previousEventKind: TouchesEventKind?
): PointerButton? =
    if (eventKind == TouchesEventKind.MOVED) {
        null
    } else if (buttonMaskOrZero and UIEventButtonMaskPrimary != 0L &&
        (previousButtonMask and UIEventButtonMaskPrimary == 0L ||
            eventKind != previousEventKind)) {
        PointerButton.Primary
    } else if (buttonMaskOrZero and UIEventButtonMaskSecondary != 0L &&
        (previousButtonMask and UIEventButtonMaskSecondary == 0L ||
            eventKind != previousEventKind)) {
        PointerButton.Secondary
    } else {
        null
    }

private val UIEvent?.timeMillis: Long get() {
    // If the touches were cancelled due to gesture failure, the timestamp is not available,
    // because no actual event with touch updates happened. We just use the current time in
    // this case.
    val timestamp = this?.timestamp ?: CACurrentMediaTime()
    return (timestamp * 1e3).toLong()
}

private val FOCUS_CHANGE_ANIMATION_DURATION = 0.15.seconds
private val SCROLL_DELTA_MULTIPLIER = 0.01f

private fun TouchesEventKind.toPointerEventType(): PointerEventType =
    when (this) {
        TouchesEventKind.BEGAN -> PointerEventType.Press
        TouchesEventKind.MOVED -> PointerEventType.Move
        TouchesEventKind.ENDED -> PointerEventType.Release
    }

private fun UIEvent.historicalChangesForTouch(
    touch: UITouch,
    view: UIView,
    density: Float
): List<HistoricalChange> {
    val touches = coalescedTouchesForTouch(touch) ?: return emptyList()

    return if (touches.size > 1) {
        // the last touch is not included because it is the actual touch reported by the event
        touches.dropLast(1).map {
            val historicalTouch = it as UITouch
            val position = historicalTouch.offsetInView(view, density)
            HistoricalChange(
                uptimeMillis = (historicalTouch.timestamp * 1e3).toLong(),
                position = position,
                originalEventPosition = position
            )
        }
    } else {
        emptyList()
    }
}

private val UIEvent?.buttonMaskOrZero: Long get() =
    if (available(OS.Ios to OSVersion(13, 4))) {
        this?.buttonMask ?: 0L
    } else {
        0L
    }

private val UIEvent?.modifierFlagsOrZero: Long get() =
    if (available(OS.Ios to OSVersion(13, 4))) {
        this?.modifierFlags ?: 0L
    } else {
        0L
    }

private val UITouch.isPressed
    get() = when (phase) {
        UITouchPhase.UITouchPhaseEnded, UITouchPhase.UITouchPhaseCancelled -> false
        else -> true
    }

private fun UITouch.offsetInView(view: UIView, density: Float): Offset =
    locationInView(view).useContents {
        Offset(x.toFloat() * density, y.toFloat() * density)
    }
