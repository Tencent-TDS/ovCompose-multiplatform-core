package androidx.compose.ui.unit

import androidx.compose.runtime.ComposeTabService
import androidx.compose.ui.uikit.utils.TMMComposeInjectCommonServiceImpl
import androidx.compose.ui.uikit.utils.TMMComposeInjectCommonServiceProtocol
import platform.darwin.NSObject
import platform.posix.int32_t


class ComposeNativeTabServiceWrapper : NSObject(), TMMComposeInjectCommonServiceProtocol {

    fun injectTabAndLogService() {
        TMMComposeInjectCommonServiceImpl.sharedInstance().injectCommonService(this)
    }

    override fun tmm_getConfigGrayPolicyIdKey(
        key: String,
        needReport: Boolean
    ): int32_t {
        return ComposeTabService.tabService?.getConfigGrayPolicyId(key, needReport) ?: 0
    }

    override fun tmm_getToggleGrayPolicyIdKey(
        key: String,
        needReport: Boolean
    ): int32_t {
        return ComposeTabService.tabService?.getToggleGrayPolicyId(key, needReport) ?: 0
    }

    override fun tmm_getTabMapValueKey(key: String): Map<Any?, *>? {
        return (ComposeTabService.tabService?.getConfigMap(key) as Map<Any?, *>) ?: null
    }

    override fun tmm_getTabToggleIsOnKey(key: String, defaultValue: Boolean): Boolean {
        return ComposeTabService.tabService?.isOn(key, defaultValue) ?: false
    }

    override fun tmm_getTabToggleIsOnKey(
        key: String,
        defaultValue: Boolean,
        needReport: Boolean
    ): Boolean {
        return ComposeTabService.tabService?.isOn(key, defaultValue, needReport) ?: false
    }

    override fun tmm_logMessage(tag: String, message: String) {
        ComposeTabService.logService?.invoke(tag, message)
    }
}

