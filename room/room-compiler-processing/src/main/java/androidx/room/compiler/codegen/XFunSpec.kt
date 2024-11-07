/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.compiler.codegen

import androidx.room.compiler.codegen.impl.XFunSpecImpl
import androidx.room.compiler.codegen.java.JavaFunSpec
import androidx.room.compiler.codegen.java.toJavaVisibilityModifier
import androidx.room.compiler.codegen.kotlin.KotlinFunSpec
import androidx.room.compiler.codegen.kotlin.toKotlinVisibilityModifier
import androidx.room.compiler.processing.FunSpecHelper
import androidx.room.compiler.processing.MethodSpecHelper
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XType
import com.squareup.kotlinpoet.KModifier

interface XFunSpec {

    val name: XName

    interface Builder {

        fun addAnnotation(annotation: XAnnotationSpec): Builder

        fun addAbstractModifier(): Builder

        // TODO(b/247247442): Maybe make a XParameterSpec ?
        fun addParameter(
            typeName: XTypeName,
            name: String,
            annotations: List<XAnnotationSpec> = emptyList()
        ): Builder = addParameter(typeName, XName.of(name), annotations)

        fun addParameter(
            typeName: XTypeName,
            name: XName,
            annotations: List<XAnnotationSpec> = emptyList()
        ): Builder

        fun addCode(code: XCodeBlock): Builder

        fun addStatement(format: String, vararg args: Any?) =
            addCode(XCodeBlock.builder().addStatement(format, *args).build())

        fun callSuperConstructor(vararg args: XCodeBlock): Builder

        fun returns(typeName: XTypeName): Builder

        fun build(): XFunSpec

        companion object {
            fun Builder.applyTo(block: Builder.(CodeLanguage) -> Unit) = apply {
                when (this) {
                    is XFunSpecImpl.Builder -> {
                        this.java.block(CodeLanguage.JAVA)
                        this.kotlin.block(CodeLanguage.KOTLIN)
                    }
                    is JavaFunSpec.Builder -> block(CodeLanguage.JAVA)
                    is KotlinFunSpec.Builder -> block(CodeLanguage.KOTLIN)
                }
            }

            fun Builder.applyTo(language: CodeLanguage, block: Builder.() -> Unit) =
                applyTo { codeLanguage ->
                    if (codeLanguage == language) {
                        block()
                    }
                }
        }
    }

    companion object {
        @JvmStatic
        fun builder(
            name: String,
            visibility: VisibilityModifier,
            isOpen: Boolean = false,
            isOverride: Boolean = false
        ) = builder(XName.of(name), visibility, isOpen, isOverride)

        @JvmStatic
        fun builder(
            name: XName,
            visibility: VisibilityModifier,
            isOpen: Boolean = false,
            isOverride: Boolean = false
        ): Builder =
            XFunSpecImpl.Builder(
                JavaFunSpec.Builder(
                    JFunSpec.methodBuilder(name.java).apply {
                        addModifiers(visibility.toJavaVisibilityModifier())
                        // TODO(b/247242374) Add nullability annotations for non-private params
                        // if (!isOpen) {
                        //    addModifiers(Modifier.FINAL)
                        // }
                        if (isOverride) {
                            addAnnotation(Override::class.java)
                        }
                    }
                ),
                KotlinFunSpec.Builder(
                    KFunSpec.builder(name.kotlin).apply {
                        addModifiers(visibility.toKotlinVisibilityModifier())
                        if (isOpen) {
                            addModifiers(KModifier.OPEN)
                        }
                        if (isOverride) {
                            addModifiers(KModifier.OVERRIDE)
                        }
                    }
                )
            )

        @JvmStatic
        fun constructorBuilder(visibility: VisibilityModifier): Builder =
            XFunSpecImpl.Builder(
                JavaFunSpec.Builder(
                    JFunSpec.constructorBuilder().apply {
                        addModifiers(visibility.toJavaVisibilityModifier())
                    }
                ),
                KotlinFunSpec.Builder(
                    KFunSpec.constructorBuilder().apply {
                        // Workaround for the unreleased fix in
                        // https://github.com/square/kotlinpoet/pull/1342
                        if (visibility != VisibilityModifier.PUBLIC) {
                            addModifiers(visibility.toKotlinVisibilityModifier())
                        }
                    }
                )
            )

        @JvmStatic
        fun overridingBuilder(element: XMethodElement, owner: XType): Builder =
            XFunSpecImpl.Builder(
                JavaFunSpec.Builder(MethodSpecHelper.overridingWithFinalParams(element, owner)),
                KotlinFunSpec.Builder(FunSpecHelper.overriding(element, owner))
            )
    }
}
