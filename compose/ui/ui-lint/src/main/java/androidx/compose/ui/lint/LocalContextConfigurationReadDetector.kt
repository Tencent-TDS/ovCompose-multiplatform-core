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

package androidx.compose.ui.lint

import androidx.compose.lint.Names.Ui.Platform.LocalConfiguration
import androidx.compose.lint.Package
import androidx.compose.lint.isInPackageName
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.UastLintUtils.Companion.tryResolveUDeclaration
import com.intellij.psi.PsiMethod
import java.util.EnumSet
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.matchesQualified
import org.jetbrains.uast.skipParenthesizedExprDown
import org.jetbrains.uast.tryResolve

/**
 * Detector that warns for calls to LocalContext.current.resources.configuration - changes to the
 * configuration object will not cause this to recompose, so callers of this API will not be
 * notified of the new configuration. LocalConfiguration.current should be used instead.
 */
class LocalContextConfigurationReadDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf(UQualifiedReferenceExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitQualifiedReferenceExpression(node: UQualifiedReferenceExpression) {
                // Fast path for whole string - note the logic below would catch this (and variants
                // that use method calls such as getResources() instead), but we want a fast path
                // so we can suggest a replacement.
                if (node.matchesQualified(LocalContextCurrentResourcesConfiguration)) {
                    context.report(
                        LocalContextConfigurationRead,
                        node,
                        context.getNameLocation(node),
                        "Reading Configuration using $LocalContextCurrentResourcesConfiguration",
                        LintFix.create()
                            .replace()
                            .name("Replace with $LocalConfigurationCurrent")
                            .all()
                            .with(LocalConfigurationCurrent)
                            .imports(LocalConfiguration.javaFqn)
                            .autoFix()
                            .build()
                    )
                    return
                }
                // Simple logic to try and match a few specific cases (there are many cases that
                // this won't warn for) where the chain is split up
                // E.g. val context = LocalContext.current, val resources = context.resources,
                // val configuration = resources.configuration
                // A future improvement would be to catch receiver scope cases, such as
                // `with(LocalContext.current.resources) { configuration... }`, but this is more
                // complicated and error prone

                // See if this is a resources.configuration call
                val selector = node.selector.skipParenthesizedExprDown()
                if (!selector.isCallToGetConfiguration()) return
                // Try and find out where this resources came from.
                val resources = node.receiver.skipParenthesizedExprDown()
                val contextExpression: UExpression? =
                    when (resources) {
                        // Still part of a qualified expression, e.g. context.resources
                        is UQualifiedReferenceExpression -> {
                            if (!resources.isCallToGetResources()) return
                            // Return the receiver, e.g. `context` in the case of
                            // `context.resources`
                            resources.receiver.skipParenthesizedExprDown()
                        }
                        // Possible reference to a variable, e.g. val resources = context.resources,
                        // and this USimpleNameReferenceExpression is `resources`
                        is USimpleNameReferenceExpression -> {
                            // If it is a property such as val resources = context.resources, find
                            // the initializer
                            val initializer =
                                (resources.tryResolveUDeclaration() as? UVariable)?.uastInitializer
                                    ?: return
                            if (initializer !is UQualifiedReferenceExpression) return
                            if (!initializer.isCallToGetResources()) return
                            // Return the receiver, e.g. `context` in the case of
                            // `context.resources`
                            initializer.receiver.skipParenthesizedExprDown()
                        }
                        else -> return
                    }

                // Try and find out where this context came from
                val contextSource =
                    when (contextExpression) {
                        // Still part of a qualified expression, e.g. LocalContext.current
                        is UQualifiedReferenceExpression -> contextExpression
                        // Possible reference to a variable, e.g. val context =
                        // LocalContext.current,
                        // and this USimpleNameReferenceExpression is `context`
                        is USimpleNameReferenceExpression -> {
                            // If it is a property such as val context = LocalContext.current, find
                            // the initializer
                            val initializer =
                                (contextExpression.tryResolveUDeclaration() as? UVariable)
                                    ?.uastInitializer ?: return
                            if (initializer !is UQualifiedReferenceExpression) return
                            initializer
                        }
                        else -> return
                    }

                if (contextSource.matchesQualified("LocalContext.current")) {
                    context.report(
                        LocalContextConfigurationRead,
                        node,
                        context.getNameLocation(node),
                        "Reading Configuration using $LocalContextCurrentResourcesConfiguration"
                    )
                }
            }
        }

    private fun UElement.isCallToGetConfiguration(): Boolean {
        val resolved = tryResolve() as? PsiMethod ?: return false
        return resolved.name == "getConfiguration" && resolved.isInPackageName(ResPackage)
    }

    private fun UElement.isCallToGetResources(): Boolean {
        val resolved = tryResolve() as? PsiMethod ?: return false
        return resolved.name == "getResources" && resolved.isInPackageName(ContentPackage)
    }

    companion object {
        private const val LocalContextCurrentResourcesConfiguration =
            "LocalContext.current.resources.configuration"
        private const val LocalConfigurationCurrent = "LocalConfiguration.current"
        private val ContentPackage = Package("android.content")
        private val ResPackage = Package("android.content.res")

        val LocalContextConfigurationRead =
            Issue.create(
                "LocalContextConfigurationRead",
                "Reading Configuration using $LocalContextCurrentResourcesConfiguration",
                "Changes to the Configuration object will not cause LocalContext reads to be " +
                    "invalidated, so you may end up with stale values when the Configuration " +
                    "changes. Instead, use $LocalConfigurationCurrent to retrieve the " +
                    "Configuration - this will recompose callers when the Configuration object " +
                    "changes.",
                Category.CORRECTNESS,
                3,
                Severity.ERROR,
                Implementation(
                    LocalContextConfigurationReadDetector::class.java,
                    EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
                )
            )
    }
}
