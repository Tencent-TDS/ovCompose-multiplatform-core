package androidx.compose.ui.platform.v2

import androidx.compose.ui.window.ApplicationStateListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import platform.Foundation.NSLog

/*
* 相关 Crash :
* https://bugly.woa.com/v2/exception/crash/issues/detail?productId=d591c6d8f7&pid=2&token=105d0f82cccebf87335239eb94153f5d&feature=FEEEE0858FB3BB483639B7628C6E30BD&cId=C45F85B2-5943-489F-A8A4-100126054C80
* 通过代码分析在 dispose 后，重新 addObserver ，此时 observer 的 state 会是 DESTROYED
* 再次触发 LifecycleRegistry.nonJvm.kt 289 行 sync 后 则可能出触发次错误
* kotlin.IllegalStateException no event down from INITIALIZED 或者 kotlin.IllegalStateException no event up from xxxx
*/
private class UIKitLifecycleRegistry(provider: LifecycleOwner) : LifecycleRegistry(provider) {

    private var isDisposed = false
    private val observerSet = HashSet<LifecycleObserver>()

    override fun handleLifecycleEvent(event: Event) {
        if (event == Lifecycle.Event.ON_DESTROY) {
            isDisposed = true
        }
        super.handleLifecycleEvent(event)
    }
    override fun addObserver(observer: LifecycleObserver) {
        if (!isDisposed) {
            if (observerSet.add(observer)) {
                // Adding the same observer again will RESET its state to INITIALIZED.
                // Thus causing the 'kotlin.IllegalStateException no event down from INITIALIZED'
                // with a following ON_DESTROY event.
                super.addObserver(observer)
            } else {
                NSLog("[UIKitLifecycleRegistry] addObserver called with duplicated observer: $observer")
            }
        } else {
            NSLog("[UIKitLifecycleRegistry] addObserver after ON_DESTROY is a bad case")
        }
    }

    override fun removeObserver(observer: LifecycleObserver) {
        super.removeObserver(observer)
        observerSet.remove(observer)
    }
}

/*
* 给 UIKit 使用的 Lifecycle
*/
internal class UIKitLifecycle : LifecycleOwner {

    override val lifecycle: LifecycleRegistry = UIKitLifecycleRegistry(this)

    /*
    字段的意义：标记当前 vc 是否在栈顶，如果不在栈顶，需要忽略 ApplicationStateListener 的回调，应该由栈顶的 vc 去处理
    如果是 view，则由业务侧自己主动触发 viewDidAppear 和 viewDidDisappear
    1. 如果仍有某一些 vc 通过 window 的方式覆盖在当前 vc 上层，则也不应该忽略，此种情况正常处理
    2. 目前在 vc 里面只能通过 viewDidAppear 和 viewDidDisappear 能判断是否在栈顶
    */
    private var isTopOfStack = false

    private var didNotSendOnCreateEvent = true

    private val applicationStateListener = ApplicationStateListener {
        // 闭包回调的值是当前应用是否回到前台，只需要处理栈顶 vc
        if (isTopOfStack) {
            viewIsVisible = it
        }
    }

    private var viewIsVisible = false
        set(value) {
            // 这里需要判断是否不一致，避免触发不必要的 Event，避免多余的 Compose 重组
            if (value != field) {
                field = value
                viewVisibilityChanged(value)
            }
        }

    private fun viewVisibilityChanged(visible: Boolean) {
        if (visible) {
            handleLifecycleEvent(Lifecycle.Event.ON_START)
            handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        } else {
            handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
    }


    private fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycle.handleLifecycleEvent(event)
    }

    fun didLoadView() {
        sendOnCreateIfNeed()
    }

    private fun sendOnCreateIfNeed() {
        if (didNotSendOnCreateEvent) {
            // 不发送直接 ON_DESTROY 会 crash
            handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            didNotSendOnCreateEvent = false
        }
    }

    fun viewDidAppear() {
        isTopOfStack = true
        viewIsVisible = true
    }

    fun viewDidDisappear() {
        isTopOfStack = false
        viewIsVisible = false
    }

    fun dispose() {
        sendOnCreateIfNeed()
        handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        applicationStateListener.dispose()
    }

}