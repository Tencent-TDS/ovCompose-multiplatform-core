/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.build.lint

import com.android.SdkConstants.ATTR_VALUE
import com.android.tools.lint.checks.TypedefDetector
import com.android.tools.lint.client.api.JavaEvaluator
import com.android.tools.lint.detector.api.AnnotationInfo
import com.android.tools.lint.detector.api.AnnotationOrigin
import com.android.tools.lint.detector.api.AnnotationUsageInfo
import com.android.tools.lint.detector.api.AnnotationUsageType
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.ConstantEvaluator
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.getMethodName
import com.android.tools.lint.detector.api.isUnconditionalReturn
import com.android.utils.SdkUtils.constantNameToCamelCase
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLiteralValue
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UIfExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParenthesizedExpression
import org.jetbrains.uast.UPolyadicExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UUnaryExpression
import org.jetbrains.uast.UastBinaryOperator
import org.jetbrains.uast.UastFacade
import org.jetbrains.uast.UastPrefixOperator
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.skipParenthesizedExprDown
import org.jetbrains.uast.tryResolve

/**
 * Enforced flag checking in the Android platform; see go/android-flagged-apis.
 *
 * **NOTE:** This file is a fork of the original sources in the Android lint code base, see
 * `lint-checks/src/main/java/com/android/tools/lint/checks/optional/FlaggedApiDetector.kt`
 */
class FlaggedApiDetector : Detector(), SourceCodeScanner {
    companion object Issues {
        private val IMPLEMENTATION =
            Implementation(FlaggedApiDetector::class.java, Scope.JAVA_FILE_SCOPE)

        /** Accessing flagged api without check. */
        @JvmField
        val ISSUE =
            Issue.create(
                id = "AndroidXFlaggedApi",
                explanation =
                    """
          This lint check looks for accesses of APIs marked with `@FlaggedApi(X)` without \
          a guarding `if (Flags.X)` check. See go/android-flagged-apis.
          """,
                briefDescription = "FlaggedApi access without check",
                category = Category.CORRECTNESS,
                priority = 6,
                severity = Severity.ERROR,
                androidSpecific = true,
                implementation = IMPLEMENTATION,
            )

        private const val FLAGGED_API_ANNOTATION = "android.annotation.FlaggedApi"
    }

    override fun applicableAnnotations(): List<String> {
        return listOf(FLAGGED_API_ANNOTATION)
    }

    override fun isApplicableAnnotationUsage(type: AnnotationUsageType): Boolean {
        return when (type) {
            AnnotationUsageType.METHOD_CALL,
            AnnotationUsageType.METHOD_REFERENCE,
            AnnotationUsageType.FIELD_REFERENCE,
            AnnotationUsageType.CLASS_REFERENCE,
            AnnotationUsageType.ANNOTATION_REFERENCE,
            AnnotationUsageType.EXTENDS,
            AnnotationUsageType.DEFINITION -> true
            else -> false
        }
    }

    override fun inheritAnnotation(annotation: String): Boolean {
        return false
    }

    override fun visitAnnotationUsage(
        context: JavaContext,
        element: UElement,
        annotationInfo: AnnotationInfo,
        usageInfo: AnnotationUsageInfo,
    ) {
        val compiled = usageInfo.referenced is PsiCompiledElement
        val annotation = annotationInfo.annotation
        val flag =
            if (compiled) {
                getFlaggedApiFromString(context, annotation)
            } else {
                getFlaggedApiFromSource(annotation)
            }

        if (flag == null) {
            // Raw string?
            val expression = annotation.attributeValues.firstOrNull()?.expression ?: return
            if (expression is ULiteralExpression) {
                val flagString = ConstantEvaluator.evaluateString(context, expression, false)
                if (flagString != null) {
                    if (flagString.indexOf('.') == -1) {
                        context.report(
                            ISSUE,
                            expression,
                            context.getLocation(expression),
                            "Invalid @FlaggedApi descriptor; should be `package.name`",
                        )
                        return
                    }
                }
                val incident =
                    Incident(
                        ISSUE,
                        expression,
                        context.getLocation(expression),
                        "@FlaggedApi should specify an actual flag constant; " +
                            "raw strings are discouraged (and more importantly, **not enforced**)",
                    )
                incident.overrideSeverity(Severity.WARNING)
                context.report(incident)
            }
            return
        }
        if (annotationInfo.origin == AnnotationOrigin.SELF) {
            if (annotationInfo.qualifiedName == FLAGGED_API_ANNOTATION) {
                return
            }
        } else if (isAlreadyAnnotated(context.evaluator, element, flag)) {
            return
        }

        val flagClass = flag.containingClass ?: return
        val flagName = flag.name
        val flagPresent = constantNameToCamelCase(flagName.removePrefix("FLAG_"))

        if (isFlagChecked(element, flagClass, flagPresent)) {
            return
        }

        val referenced = element.tryResolve()
        val description =
            when {
                referenced is PsiMethod -> "Method `${referenced.name}()`"
                element is UCallExpression -> "Method `${getMethodName(element)}()`"
                referenced is PsiField -> "Field `${referenced.name}`"
                referenced is PsiClass -> "Class `${referenced.name}`"
                element is UClassLiteralExpression ->
                    "Class `${element.expression?.sourcePsi?.text}`"
                referenced is PsiNamedElement -> "Reference `${referenced.name}`"
                else -> "This"
            }
        val name = element.getParentOfType<UMethod>()?.name ?: "?"
        val message =
            "$description is a flagged API and should be inside an `if (${flagClass.name}.$flagPresent())` check " +
                "(or annotate the surrounding method `$name` with `@FlaggedApi(${flagClass.name}.$flagName) to transfer requirement to caller`)"
        context.report(ISSUE, element, context.getLocation(element), message)
    }

    /** Given a `@FlaggedApi` annotation, returns the resolved field. */
    private fun getFlaggedApi(
        context: JavaContext,
        annotation: UAnnotation,
        usageInfo: AnnotationUsageInfo,
    ): PsiField? {
        if (usageInfo.referenced is PsiCompiledElement) {
            return getFlaggedApiFromString(context, annotation)
        }

        return getFlaggedApiFromSource(annotation)
    }

    /** Given a `@FlaggedApi` annotation, returns the resolved field. */
    private fun getFlaggedApiFromSource(annotation: UAnnotation): PsiField? {
        return annotation.attributeValues.firstOrNull()?.expression?.tryResolve() as? PsiField
    }

    /**
     * Given a `@FlaggedApi` annotation in bytecode, maps from the flag value back to the original
     * flagged API field (this process is deterministic).
     */
    private fun getFlaggedApiFromString(context: JavaContext, annotation: UAnnotation): PsiField? {
        val sourcePsi = annotation.sourcePsi
        if (sourcePsi is PsiAnnotation) {
            val value = sourcePsi.findAttributeValue(ATTR_VALUE) as? PsiLiteralValue
            val flag = value?.value as? String ?: return null

            val separator = flag.lastIndexOf('.')
            if (separator != -1) {
                val packageName = flag.substring(0, separator)
                val className = "$packageName.Flags"
                val cls = context.evaluator.findClass(className) ?: return null
                val fieldName = "FLAG_" + flag.substring(separator + 1).uppercase()
                return cls.findFieldByName(fieldName, true)
            }
        }

        return null
    }

    /**
     * Is the given [element] within a code block already annotated with the same flagged api as
     * [flag].
     */
    private fun isAlreadyAnnotated(
        evaluator: JavaEvaluator,
        element: UElement?,
        flag: PsiField,
    ): Boolean {
        var current = element
        while (current != null) {
            if (current is UAnnotated) {
                //noinspection AndroidLintExternalAnnotations
                for (annotation in current.uAnnotations) {
                    val api = getFlaggedApiFromSource(annotation) ?: continue
                    if (api.isEquivalentTo(flag)) {
                        return true
                    }
                }
            }
            if (current is UAnnotation) {
                if (TypedefDetector.isTypeDef(current.qualifiedName)) {
                    return true
                }
            } else if (current is UFile) {
                // Also consult any package annotations
                val pkg = evaluator.getPackage(current.javaPsi ?: current.sourcePsi)
                if (pkg != null) {
                    for (psiAnnotation in pkg.annotations) {
                        val annotation =
                            UastFacade.convertElement(psiAnnotation, null) as? UAnnotation
                                ?: continue
                        val api = getFlaggedApiFromSource(annotation) ?: continue
                        if (api.isEquivalentTo(flag)) {
                            return true
                        }
                    }
                }

                break
            }
            current = current.uastParent
        }

        return false
    }

    /**
     * Is the given [element] inside a flag check (where the class is [flagClass] and
     * [flagMethodName] is the flag checking method name), or after an early return of the flag not
     * being set?
     */
    private fun isFlagChecked(
        element: UElement,
        flagClass: PsiClass,
        flagMethodName: String,
    ): Boolean {
        var curr = element.uastParent ?: return false

        var prev = element
        while (curr !is UFile) {
            if (curr is UIfExpression) {
                val condition = curr.condition
                if (prev !== condition) {
                    val fromThen = prev == curr.thenExpression
                    if (fromThen) {
                        if (isFlagExpression(condition, flagClass, flagMethodName)) {
                            return true
                        }
                    } else {
                        // Handle "if (!Flags.X) else <CALL>"
                        val op = condition.skipParenthesizedExprDown()
                        if (
                            op is UUnaryExpression &&
                                op.operator == UastPrefixOperator.LOGICAL_NOT &&
                                isFlagExpression(op.operand, flagClass, flagMethodName)
                        ) {
                            return true
                        } else if (
                            op is UPolyadicExpression &&
                                op.operator == UastBinaryOperator.LOGICAL_OR &&
                                (op.operands.any {
                                    val nested = it.skipParenthesizedExprDown()
                                    nested is UUnaryExpression &&
                                        nested.operator == UastPrefixOperator.LOGICAL_NOT &&
                                        isFlagExpression(nested.operand, flagClass, flagMethodName)
                                })
                        ) {
                            return true
                        }
                    }
                }
            } else if (
                curr is UPolyadicExpression && curr.operator == UastBinaryOperator.LOGICAL_AND
            ) {
                for (operand in curr.operands) {
                    if (operand === curr) {
                        break
                    } else if (isFlagExpression(operand, flagClass, flagMethodName)) {
                        return true
                    }
                }
            } else if (curr is UMethod) {
                // See if there's an early return. We *only* handle a very simple canonical format
                // here;
                // must be first statement in method.
                val body = curr.uastBody
                if (body is UBlockExpression && body.expressions.size > 1) {
                    val first = body.expressions[0]
                    if (first is UIfExpression) {
                        val condition = first.condition.skipParenthesizedExprDown()
                        if (
                            condition is UUnaryExpression &&
                                condition.operator == UastPrefixOperator.LOGICAL_NOT &&
                                isFlagExpression(condition.operand, flagClass, flagMethodName)
                        ) {
                            // It's a flag check; make sure we just return
                            val then = first.thenExpression?.skipParenthesizedExprDown()
                            if (then != null && then.isUnconditionalReturn()) {
                                return true
                            }
                        }
                    }
                }
            }

            prev = curr
            curr = curr.uastParent ?: break
        }

        return false
    }

    /** Is the given [element] a flag expression (e.g. "Flags.set()") ? */
    private fun isFlagExpression(
        element: UElement,
        flagClass: PsiClass,
        flagMethodName: String,
    ): Boolean {
        if (element is UUnaryExpression && element.operator == UastPrefixOperator.LOGICAL_NOT) {
            return !isFlagExpression(element.operand, flagClass, flagMethodName)
        } else if (element is UReferenceExpression || element is UCallExpression) {
            val resolved = element.tryResolve()
            if (resolved is PsiMethod) {
                if (resolved.name == flagMethodName) {
                    val cls = resolved.containingClass
                    if (flagClass.isEquivalentTo(cls)) {
                        return true
                    }
                }
            } else if (resolved is PsiField) {
                // Arguably we should look for final fields here, but on the other hand
                // there may be cases where it's initialized later, so it's a bit like
                // Kotlin's "lateinit". Treat them all as constant.
                val initializer = UastFacade.getInitializerBody(resolved)
                if (initializer != null) {
                    return isFlagExpression(initializer, flagClass, flagMethodName)
                }
            }
        } else if (element is UParenthesizedExpression) {
            return isFlagExpression(element.expression, flagClass, flagMethodName)
        } else if (element is UPolyadicExpression) {
            if (element.operator == UastBinaryOperator.LOGICAL_AND) {
                for (operand in element.operands) {
                    if (isFlagExpression(operand, flagClass, flagMethodName)) {
                        return true
                    }
                }
            }
        }
        return false
    }
}
