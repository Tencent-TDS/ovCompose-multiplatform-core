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

package androidx.navigation.compose

import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.net.toUri
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.navDeepLink
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@LargeTest
@RunWith(AndroidJUnit4::class)
class NavGraphBuilderAndroidTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testDeepLink() {
        lateinit var navController: TestNavHostController
        val uriString = "https://www.example.com"
        val deeplink = NavDeepLinkRequest.Builder.fromUri(Uri.parse(uriString)).build()
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                composable(
                    secondRoute,
                    deepLinks = listOf(navDeepLink { uriPattern = uriString })
                ) {}
            }
        }

        composeTestRule.runOnUiThread {
            navController.navigate(uriString.toUri())
            assertThat(navController.currentBackStackEntry!!.destination.hasDeepLink(deeplink))
                .isTrue()
        }
    }

    @Test
    fun testNestedNavigationDeepLink() {
        lateinit var navController: TestNavHostController
        val uriString = "https://www.example.com"
        val deeplink = NavDeepLinkRequest.Builder.fromUri(Uri.parse(uriString)).build()
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                navigation(
                    startDestination = thirdRoute,
                    route = secondRoute,
                    deepLinks = listOf(navDeepLink { uriPattern = uriString })
                ) {
                    composable(thirdRoute) {}
                }
            }
        }

        composeTestRule.runOnUiThread {
            navController.navigate(uriString.toUri())
            assertThat(
                navController.getBackStackEntry(secondRoute).destination.hasDeepLink(deeplink)
            )
                .isTrue()
        }
    }
}

private const val firstRoute = "first"
private const val secondRoute = "second"
private const val thirdRoute = "third"