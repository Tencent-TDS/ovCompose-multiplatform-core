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

package androidx.appfunctions.compiler.testings

import androidx.room.compiler.processing.util.DiagnosticMessage
import androidx.room.compiler.processing.util.Resource
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.compiler.TestCompilationArguments
import androidx.room.compiler.processing.util.compiler.TestCompilationResult
import androidx.room.compiler.processing.util.compiler.compile
import com.google.common.truth.Truth
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.tools.Diagnostic
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.pathString
import kotlin.io.path.writeText

/** A helper to test compilation. */
class CompilationTestHelper(
    /** The root directory containing the source test files. */
    private val testFileSrcDir: File,
    /** The root directory containing the source golden files. */
    private val goldenFileSrcDir: File,
    /** A list of [com.google.devtools.ksp.processing.SymbolProcessorProvider] under test. */
    private val symbolProcessorProviders: List<SymbolProcessorProvider>,
) {

    init {
        check(testFileSrcDir.exists()) {
            "Test file source directory [${testFileSrcDir.path}] does not exist"
        }
        check(testFileSrcDir.isDirectory) { "[$testFileSrcDir] is not a directory." }

        check(goldenFileSrcDir.exists()) {
            "Golden file source directory [${goldenFileSrcDir.path}] does not exist"
        }
        check(goldenFileSrcDir.isDirectory) { "[$goldenFileSrcDir] is not a directory." }
    }

    private val outputDir: Path by lazy {
        requireNotNull(System.getProperty("test_output_dir")) {
                "test_output_dir not set for diff test."
            }
            .let { Path(it) }
    }

    /** Compiles all [sourceFileNames] with additional [processorOptions]. */
    fun compileAll(
        sourceFileNames: List<String>,
        processorOptions: Map<String, String> = emptyMap<String, String>(),
    ): CompilationReport {
        val sources =
            sourceFileNames.map { sourceFileName ->
                val sourceFile = getTestSourceFile(sourceFileName)
                Source.Companion.kotlin(
                    ensureKotlinFileNameFormat(sourceFileName),
                    sourceFile.readText()
                )
            }

        val workingDir =
            Files.createTempDirectory("compile").toFile().also { file -> file.deleteOnExit() }
        val result =
            compile(
                workingDir,
                TestCompilationArguments(
                    sources = sources,
                    symbolProcessorProviders = symbolProcessorProviders,
                    processorOptions = processorOptions,
                )
            )

        return CompilationReport.create(result, outputDir)
    }

    /**
     * Asserts that the compilation succeeds and contains [expectGeneratedSourceFileName] in
     * generated sources that is identical to the content of [goldenFileName].
     */
    fun assertSuccessWithSourceContent(
        report: CompilationReport,
        expectGeneratedSourceFileName: String,
        goldenFileName: String,
    ) {
        val generatedSourceFile =
            report.generatedSourceFiles.firstOrNull { sourceFile ->
                sourceFile.source.relativePath.contains(expectGeneratedSourceFileName)
            }
        check(generatedSourceFile != null) { "Unable to find [$expectGeneratedSourceFileName]" }
        Truth.assertWithMessage(
                """
                    Compile failed with error:
                    ${report.printDiagnostics(Diagnostic.Kind.ERROR)}

                    Generated content:
                    ${generatedSourceFile.source.contents}
                """
                    .trimIndent()
            )
            .that(report.isSuccess)
            .isTrue()

        val goldenFile = getGoldenFile(goldenFileName)
        val generatedFilePath = generatedSourceFile.sourceFilePath.pathString.sanitizeFilePath()
        val goldenFilePath = goldenFile.absolutePath.sanitizeFilePath()
        Truth.assertWithMessage(
                """
              Content of generated file [${generatedSourceFile.source.relativePath}] does not match
              the content of golden file [${goldenFile.path}].

              To update the golden file,
              run `cp $generatedFilePath ${goldenFilePath}`
            """
                    .trimIndent()
            )
            .that(generatedSourceFile.source.contents)
            .isEqualTo(goldenFile.readText())
    }

    /**
     * Asserts that the compilation succeeds and contains [expectGeneratedResourceFileName] in
     * generated resources that is identical to the content of [goldenFileName].
     */
    fun assertSuccessWithResourceContent(
        report: CompilationReport,
        expectGeneratedResourceFileName: String,
        goldenFileName: String,
    ) {
        Truth.assertWithMessage(
                """
                    Compile failed with error:
                    ${report.printDiagnostics(Diagnostic.Kind.ERROR)}
                """
                    .trimIndent()
            )
            .that(report.isSuccess)
            .isTrue()

        val goldenFile = getGoldenFile(goldenFileName)
        val generatedResourceFile =
            report.generatedResourceFiles.single { resourceFile ->
                resourceFile.resource.relativePath.contains(expectGeneratedResourceFileName)
            }
        val generatedFilePath = generatedResourceFile.resourceFilePath.pathString.sanitizeFilePath()
        val goldenFilePath = goldenFile.absolutePath.sanitizeFilePath()
        Truth.assertWithMessage(
                """
              Content of generated file [${generatedResourceFile.resource.relativePath}] does not match
              the content of golden file [${goldenFile.path}].

              To update the golden file,
              run `cp $generatedFilePath ${goldenFilePath}`
            """
                    .trimIndent()
            )
            .that(generatedResourceFile.resource.getContents())
            .isEqualTo(goldenFile.readText())
    }

    fun assertErrorWithMessage(report: CompilationReport, expectedErrorMessage: String) {
        Truth.assertWithMessage("Compile succeed").that(report.isSuccess).isFalse()

        val errorDiagnostics = report.diagnostics[Diagnostic.Kind.ERROR] ?: emptyList()
        var foundError = false
        for (errorDiagnostic in errorDiagnostics) {
            if (errorDiagnostic.msg.contains(expectedErrorMessage)) {
                foundError = true
                break
            }
        }
        Truth.assertWithMessage(
                """
                Unable to find the expected error message [$expectedErrorMessage] from the
                diagnostics results:

                ${report.printDiagnostics(Diagnostic.Kind.ERROR)}
            """
                    .trimIndent()
            )
            .that(foundError)
            .isTrue()
    }

    private fun ensureKotlinFileNameFormat(sourceFileName: String): String {
        val nameParts = sourceFileName.split(".")
        require(nameParts.last().lowercase() == "kt") {
            "Source file $sourceFileName is not a Kotlin file"
        }
        val fileNameWithoutExtension =
            nameParts.joinToString(separator = ".", limit = nameParts.size - 1)
        return "${fileNameWithoutExtension}.kt"
    }

    private fun getTestSourceFile(fileName: String): File {
        return File(
                testFileSrcDir,
                /** child= */
                fileName
            )
            .also { file -> check(file.exists()) { "Source file [${file.path}] does not exist" } }
    }

    private fun getGoldenFile(fileName: String): File {
        return File(
                goldenFileSrcDir,
                /** child= */
                fileName
            )
            .also { file -> check(file.exists()) { "Golden file [${file.path}] does not exist" } }
    }

    private fun String.sanitizeFilePath(): String {
        return this.replace("$", "\\$")
    }

    /** The compilation report. */
    data class CompilationReport(
        /** Indicates whether the compilation succeed or not. */
        val isSuccess: Boolean,
        /** A list of generated source files. */
        val generatedSourceFiles: List<GeneratedSourceFile>,
        /** A map of diagnostics results. */
        val diagnostics: Map<Diagnostic.Kind, List<DiagnosticMessage>>,
        /** A list of generated source files. */
        val generatedResourceFiles: List<GeneratedResourceFile>,
    ) {
        /** Print the diagnostics result of type [kind]. */
        fun printDiagnostics(kind: Diagnostic.Kind): String {
            val errorDiagnostics = diagnostics[kind] ?: return "No ${kind.name} diagnostic message"
            return buildString {
                append("${kind.name} diagnostic messages:\n\n")
                for (diagnostic in errorDiagnostics) {
                    append("$diagnostic\n\n")
                }
            }
        }

        companion object {
            internal fun create(result: TestCompilationResult, outputDir: Path): CompilationReport {
                return CompilationReport(
                    isSuccess = result.success,
                    generatedSourceFiles =
                        result.generatedSources.map { source ->
                            GeneratedSourceFile.create(source, outputDir)
                        },
                    diagnostics = result.diagnostics,
                    generatedResourceFiles =
                        result.generatedResources.map { resource ->
                            GeneratedResourceFile.create(resource, outputDir)
                        }
                )
            }
        }
    }

    /** A wrapper class contains [source] with its file path. */
    data class GeneratedSourceFile(val source: Source, val sourceFilePath: Path) {
        companion object {
            internal fun create(source: Source, outputDir: Path): GeneratedSourceFile {
                val filePath =
                    outputDir.resolve(source.relativePath).apply {
                        parent?.createDirectories()
                        deleteIfExists()
                        createFile()
                        writeText(source.contents)
                    }
                return GeneratedSourceFile(source, filePath)
            }
        }
    }

    /** A wrapper class contains [Resource] with its file path. */
    data class GeneratedResourceFile(val resource: Resource, val resourceFilePath: Path) {
        companion object {
            internal fun create(resource: Resource, outputDir: Path): GeneratedResourceFile {
                val filePath =
                    outputDir.resolve(resource.relativePath).apply {
                        parent?.createDirectories()
                        deleteIfExists()
                        createFile()
                        writeText(resource.getContents())
                    }
                return GeneratedResourceFile(resource, filePath)
            }
        }
    }

    companion object {
        private fun Resource.getContents(): String =
            openInputStream().bufferedReader().use { it.readText() }
    }
}
