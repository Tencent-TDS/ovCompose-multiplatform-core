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
package androidx.wear.protolayout.material3

import android.graphics.Color
import android.os.Build.VERSION_CODES
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.material3.tokens.ColorTokens
import androidx.wear.protolayout.types.argb
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(AndroidJUnit4::class)
@DoNotInstrument
class MaterialScopeTest {
    @Test
    @Config(minSdk = VERSION_CODES.VANILLA_ICE_CREAM)
    fun testDynamicThemeEnabled_returnsTrue() {
        enableDynamicTheme()

        assertThat(isDynamicColorSchemeEnabled(getApplicationContext())).isTrue()
    }

    @Test
    @Config(minSdk = VERSION_CODES.VANILLA_ICE_CREAM)
    fun scopeWithDefaultTheme_defaultOptInDynamicColor_dynamicThemeEnabled_api35() {
        enableDynamicTheme()

        val scopeWithDefaultTheme =
            MaterialScope(
                context = getApplicationContext(),
                deviceConfiguration = DEVICE_PARAMETERS,
                allowDynamicTheme = true,
                theme =
                    MaterialTheme(
                        colorScheme = dynamicColorScheme(context = getApplicationContext())
                    ),
                defaultTextElementStyle = TextElementStyle(),
                defaultIconStyle = IconStyle(),
                defaultBackgroundImageStyle = BackgroundImageStyle(),
                defaultAvatarImageStyle = AvatarImageStyle(),
                layoutSlotsPresence = LayoutSlotsPresence()
            )

        assertThat(scopeWithDefaultTheme.deviceConfiguration).isEqualTo(DEVICE_PARAMETERS)
        assertThat(scopeWithDefaultTheme.allowDynamicTheme).isTrue()
        assertThat(isDynamicColorSchemeEnabled(scopeWithDefaultTheme.context)).isTrue()
        // It doesn't use default static theme
        assertThat(scopeWithDefaultTheme.theme.colorScheme.primary.staticArgb)
            .isNotEqualTo(ColorTokens.PRIMARY)
    }

    @Test
    fun scopeWithCustomThemeAndOptOutDynamicColor() {
        val customErrorColor = Color.MAGENTA
        val customTertiaryColor = Color.CYAN

        val materialScope =
            MaterialScope(
                context = getApplicationContext(),
                deviceConfiguration = DEVICE_PARAMETERS,
                theme =
                    MaterialTheme(
                        colorScheme =
                            ColorScheme(
                                error = customErrorColor.argb,
                                tertiary = customTertiaryColor.argb
                            )
                    ),
                allowDynamicTheme = false,
                defaultTextElementStyle = TextElementStyle(),
                defaultIconStyle = IconStyle(),
                defaultBackgroundImageStyle = BackgroundImageStyle(),
                defaultAvatarImageStyle = AvatarImageStyle(),
                layoutSlotsPresence = LayoutSlotsPresence()
            )

        assertThat(materialScope.deviceConfiguration).isEqualTo(DEVICE_PARAMETERS)
        assertThat(materialScope.allowDynamicTheme).isFalse()

        // Overridden
        assertThat(materialScope.theme.colorScheme.error.staticArgb).isEqualTo(customErrorColor)
        assertThat(materialScope.theme.colorScheme.tertiary.staticArgb)
            .isEqualTo(customTertiaryColor)
        // Not overridden
        assertThat(materialScope.theme.colorScheme.primary.staticArgb)
            .isEqualTo(ColorTokens.PRIMARY)
    }

    @Test
    fun scopeWithCustomTheme_optInToDynamicColor_dynamicThemingDisabled() {
        val customErrorColor = Color.MAGENTA
        val customTertiaryColor = Color.CYAN

        val materialScope =
            MaterialScope(
                context = getApplicationContext(),
                deviceConfiguration = DEVICE_PARAMETERS,
                allowDynamicTheme = true,
                theme =
                    MaterialTheme(
                        colorScheme =
                            ColorScheme(
                                error = customErrorColor.argb,
                                tertiary = customTertiaryColor.argb
                            )
                    ),
                defaultTextElementStyle = TextElementStyle(),
                defaultIconStyle = IconStyle(),
                defaultBackgroundImageStyle = BackgroundImageStyle(),
                defaultAvatarImageStyle = AvatarImageStyle(),
                layoutSlotsPresence = LayoutSlotsPresence()
            )

        assertThat(isDynamicColorSchemeEnabled(materialScope.context)).isFalse()
        assertThat(materialScope.deviceConfiguration).isEqualTo(DEVICE_PARAMETERS)
        assertThat(materialScope.allowDynamicTheme).isTrue()
        // Overridden
        assertThat(materialScope.theme.colorScheme.error.staticArgb).isEqualTo(customErrorColor)
        assertThat(materialScope.theme.colorScheme.tertiary.staticArgb)
            .isEqualTo(customTertiaryColor)
        // Not overridden
        assertThat(materialScope.theme.colorScheme.primary.staticArgb)
            .isEqualTo(ColorTokens.PRIMARY)
    }

    companion object {
        internal val DEVICE_PARAMETERS =
            DeviceParametersBuilders.DeviceParameters.Builder()
                .setScreenWidthDp(192)
                .setScreenHeightDp(192)
                .build()
    }
}
