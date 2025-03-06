/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.navigation

import android.os.Bundle
import androidx.kruth.assertThat
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NavigatorProviderAndroidTest {
    @Test
    fun addWithMissingAnnotationName() {
        val provider = NavigatorProvider()
        val navigator = NoNameNavigator()
        try {
            provider.addNavigator(navigator)
            fail(
                "Adding a provider with no @Navigator.Name should cause an " +
                    "IllegalArgumentException"
            )
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun addWithMissingAnnotationNameGetWithExplicitName() {
        val provider = NavigatorProvider()
        val navigator = NoNameNavigator()
        provider.addNavigator("name", navigator)
        assertThat(provider.getNavigator<NoNameNavigator>("name")).isEqualTo(navigator)
    }

    @Test
    fun addWithExplicitNameGetWithMissingAnnotationName() {
        val provider = NavigatorProvider()
        val navigator = NoNameNavigator()
        provider.addNavigator("name", navigator)
        try {
            provider.getNavigator(NoNameNavigator::class.java)
            fail(
                "getNavigator(Class) with no @Navigator.Name should cause an " +
                    "IllegalArgumentException"
            )
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun addWithAnnotationNameGetWithAnnotationName() {
        val provider = NavigatorProvider()
        val navigator = EmptyNavigator()
        provider.addNavigator(navigator)
        assertThat(provider.getNavigator(EmptyNavigator::class.java)).isEqualTo(navigator)
    }
}

internal actual class NoNameNavigator
actual constructor() : Navigator<NavDestination>() {
    actual override fun createDestination(): NavDestination {
        throw IllegalStateException("createDestination is not supported")
    }

    actual override fun navigate(
        destination: NavDestination,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Extras?
    ): NavDestination? {
        throw IllegalStateException("navigate is not supported")
    }

    actual override fun popBackStack(): Boolean {
        throw IllegalStateException("popBackStack is not supported")
    }
}

/** An empty [Navigator] used to test [NavigatorProvider]. */
@Navigator.Name(EmptyNavigator.NAME)
internal actual open class EmptyNavigator
actual constructor() : Navigator<NavDestination>() {

    actual companion object {
        actual const val NAME = "empty"
    }

    actual override fun createDestination(): NavDestination {
        throw IllegalStateException("createDestination is not supported")
    }

    actual override fun navigate(
        destination: NavDestination,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Extras?
    ): NavDestination? {
        throw IllegalStateException("navigate is not supported")
    }

    actual override fun popBackStack(): Boolean {
        throw IllegalStateException("popBackStack is not supported")
    }
}
