package androidx.compose.ui.window

import androidx.compose.runtime.ComposeTabService
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.interop.UIKitInteropTransaction
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSRunLoop
import platform.Foundation.NSRunLoopCommonModes
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSTimeInterval
import platform.QuartzCore.CACurrentMediaTime
import platform.QuartzCore.CADisplayLink
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationState
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.darwin.NSInteger
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

internal abstract class Redrawer(protected val callbacks: RedrawerCallbacks) {

    var isForcedToPresentWithTransactionEveryFrame: Boolean = false

    private var isDisposed = false

    /**
     * null after [dispose] call
     */
    protected var caDisplayLink: CADisplayLink? = CADisplayLink.displayLinkWithTarget(
        target = DisplayLinkProxy {
            val targetTimestamp = currentTargetTimestamp ?: return@DisplayLinkProxy

            displayLinkConditions.onDisplayLinkTick {
                if (ComposeTabService.sendTouchesAsynchronously) {
                    // call draw asynchronously to avoid blocking display link callback.
                    dispatch_async(dispatch_get_main_queue()) {
                        if (!isDisposed) {
                            // Do not call draw after disposed.
                            draw(waitUntilCompletion = false, targetTimestamp)
                        }
                    }
                } else {
                    draw(waitUntilCompletion = false, targetTimestamp)
                }
            }
        },
        selector = NSSelectorFromString(DisplayLinkProxy::handleDisplayLinkTick.name)
    )

    private val currentTargetTimestamp: NSTimeInterval?
        get() = caDisplayLink?.targetTimestamp

    protected val displayLinkConditions = DisplayLinkConditions { paused ->
        caDisplayLink?.paused = paused
    }

    /**
     * Set to `true` if need always running invalidation-independent displayLink for forcing UITouch events to come at the fastest possible cadence.
     * Otherwise, touch events can come at rate lower than actual display refresh rate.
     */
    var needsProactiveDisplayLink: Boolean
        get() = displayLinkConditions.needsToBeProactive
        set(value) {
            displayLinkConditions.needsToBeProactive = value
        }

    var maximumFramesPerSecond: NSInteger
        get() = caDisplayLink?.preferredFramesPerSecond ?: 0
        set(value) {
            caDisplayLink?.preferredFramesPerSecond = value
        }

    open var opaque: Boolean = true

    private val applicationStateListener = ApplicationStateListener { isApplicationActive ->
        displayLinkConditions.isApplicationActive = isApplicationActive

        onApplicationStateChanged(isApplicationActive)
    }

    init {
        val caDisplayLink = caDisplayLink
            ?: throw IllegalStateException("caDisplayLink is null during redrawer init")

        // UIApplication can be in UIApplicationStateInactive state (during app launch before it gives control back to run loop)
        // and won't receive UIApplicationWillEnterForegroundNotification
        // so we compare the state with UIApplicationStateBackground instead of UIApplicationStateActive
        displayLinkConditions.isApplicationActive =
            UIApplication.sharedApplication.applicationState != UIApplicationState.UIApplicationStateBackground

        // Wait for View to be laid out.
        caDisplayLink.paused = true
        caDisplayLink.addToRunLoop(NSRunLoop.mainRunLoop, NSRunLoopCommonModes)
    }

    /**
     * Marks current state as dirty and unpauses display link if needed and enables draw dispatch operation on
     * next vsync
     */
    fun needRedraw() = displayLinkConditions.needRedraw()

    protected abstract fun draw(waitUntilCompletion: Boolean, targetTimestamp: NSTimeInterval)

    protected open fun onApplicationStateChanged(isApplicationActive: Boolean) {}

    open fun dispose() {
        check(caDisplayLink != null) { "MetalRedrawer.dispose() was called more than once" }

        isDisposed = true
        caDisplayLink?.invalidate()
        caDisplayLink = null

        applicationStateListener.dispose()
    }

    /**
     * Immediately dispatch draw and block the thread until it's finished and presented on the screen.
     */
    fun drawSynchronously() {
        if (caDisplayLink == null) {
            return
        }
        displayLinkConditions.isViewActive = true
        // Consume a draw request, shouldn't we?
        if (displayLinkConditions.scheduledRedrawsCount > 0) {
            displayLinkConditions.scheduledRedrawsCount -= 1
        }
        draw(waitUntilCompletion = true, CACurrentMediaTime())
    }
}

class DisplayLinkConditions(
    val setPausedCallback: (Boolean) -> Unit
) {
    /**
     * see [MetalRedrawer.needsProactiveDisplayLink]
     */
    var needsToBeProactive: Boolean = false
        set(value) {
            field = value

            update()
        }

    var isViewActive: Boolean = false
        set(value) {
            field = value

            update()
        }

    /**
     * Indicates that application is running foreground now
     */
    var isApplicationActive: Boolean = false
        set(value) {
            field = value

            update()
        }

    /**
     * Number of subsequent vsync that will issue a draw
     */
    var scheduledRedrawsCount = 0
        set(value) {
            field = value

            update()
        }

    /**
     * Handle display link callback by updating internal state and dispatching the draw, if needed.
     */
    inline fun onDisplayLinkTick(draw: () -> Unit) {
        if (scheduledRedrawsCount > 0) {
            scheduledRedrawsCount -= 1
            draw()
        }
    }

    /**
     * Mark next [FRAMES_COUNT_TO_SCHEDULE_ON_NEED_REDRAW] frames to issue a draw dispatch and unpause displayLink if needed.
     */
    fun needRedraw() {
        scheduledRedrawsCount = FRAMES_COUNT_TO_SCHEDULE_ON_NEED_REDRAW
    }

    private fun update() {
        val isUnpaused = isViewActive && isApplicationActive && (needsToBeProactive || scheduledRedrawsCount > 0)
        setPausedCallback(!isUnpaused)
    }

    companion object {
        /**
         * Right now `needRedraw` doesn't reentry from within `draw` callback during animation which leads to a situation where CADisplayLink is first paused
         * and then asynchronously unpaused. This effectively makes Pro Motion display lose a frame before running on highest possible frequency again.
         * To avoid this, we need to render at least two frames (instead of just one) after each `needRedraw` assuming that invalidation comes inbetween them and
         * displayLink is not paused by the end of RuntimeLoop tick.
         */
        const val FRAMES_COUNT_TO_SCHEDULE_ON_NEED_REDRAW = 2
    }
}

internal class ApplicationStateListener(
    /**
     * Callback which will be called with `true` when the app becomes active, and `false` when the app goes background
     */
    private val callback: (Boolean) -> Unit
) : NSObject() {
    init {
        val notificationCenter = NSNotificationCenter.defaultCenter

        notificationCenter.addObserver(
            this,
            NSSelectorFromString(::applicationWillEnterForeground.name),
            UIApplicationWillEnterForegroundNotification,
            null
        )

        notificationCenter.addObserver(
            this,
            NSSelectorFromString(::applicationDidEnterBackground.name),
            UIApplicationDidEnterBackgroundNotification,
            null
        )
    }

    @ObjCAction
    fun applicationWillEnterForeground() {
        callback(true)
    }

    @ObjCAction
    fun applicationDidEnterBackground() {
        callback(false)
    }

    /**
     * Deregister from [NSNotificationCenter]
     */
    fun dispose() {
        val notificationCenter = NSNotificationCenter.defaultCenter

        notificationCenter.removeObserver(this, UIApplicationWillEnterForegroundNotification, null)
        notificationCenter.removeObserver(this, UIApplicationDidEnterBackgroundNotification, null)
    }
}

internal interface RedrawerCallbacks {
    /**
     * Perform time step and encode draw operations into canvas.
     *
     * @param canvas Canvas to encode draw operations into.
     * @param targetTimestamp Timestamp indicating the expected draw result presentation time. Implementation should forward its internal time clock to this targetTimestamp to achieve smooth visual change cadence.
     */
    fun render(canvas: Canvas, targetTimestamp: NSTimeInterval)

    /**
     * Retrieve a transaction object, containing a list of pending actions
     * that need to be synchronized with Metal rendering using CATransaction mechanism.
     */
    fun retrieveInteropTransaction(): UIKitInteropTransaction
}

private class DisplayLinkProxy(
    private val callback: () -> Unit
) : NSObject() {
    @ObjCAction
    fun handleDisplayLinkTick() {
        callback()
    }
}