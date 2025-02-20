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

package androidx.savedstate.serialization.utils

import androidx.savedstate.RobolectricTest
import androidx.savedstate.SavedState
import androidx.savedstate.platformEncodeDecode
import androidx.savedstate.read
import androidx.savedstate.serialization.SavedStateConfig
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.overwriteWith

/**
 * Base class for testing [SavedState] serialization and deserialization.
 *
 * This class provides a shared setup for testing the encoding and decoding of objects into
 * [SavedState]. It extends [RobolectricTest] to enable proper functionality of `android.os.Bundle`
 * in unit tests.
 */
internal abstract class SavedStateSerializationBaseTest(
    config: SavedStateConfig = SavedStateConfig.DEFAULT,
) : RobolectricTest() {

    private val config =
        SavedStateConfig(config) {
            val modules = SerializersModule {
                include(polymorphicTestModule)
                include(contextualTestModule)
            }
            serializersModule = modules.overwriteWith(config.serializersModule)
        }

    protected fun doTestNullData(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<NullData>? = null,
        assertion: SavedStateAssertionScope<NullData>.() -> Unit,
    ) {
        doTest(NullData(null), config, serializer, assertion)
    }

    protected fun doTestInt(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<Int>? = null,
        assertion: SavedStateAssertionScope<Int>.() -> Unit,
    ) {
        doTest(7, config, serializer, assertion)
    }

    protected fun doTestIntData(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<IntData>? = null,
        assertion: SavedStateAssertionScope<IntData>.() -> Unit,
    ) {
        doTest(IntData(value = 7), config, serializer, assertion)
    }

    protected fun doTestLong(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<Long>? = null,
        assertion: SavedStateAssertionScope<Long>.() -> Unit,
    ) {
        doTest(7L, config, serializer, assertion)
    }

    protected fun doTestLongData(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<LongData>? = null,
        assertion: SavedStateAssertionScope<LongData>.() -> Unit,
    ) {
        doTest(LongData(value = 7L), config, serializer, assertion)
    }

    protected fun doTestShort(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<Short>? = null,
        assertion: SavedStateAssertionScope<Short>.() -> Unit,
    ) {
        doTest(7.toShort(), config, serializer, assertion)
    }

    protected fun doTestShortData(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<ShortData>? = null,
        assertion: SavedStateAssertionScope<ShortData>.() -> Unit,
    ) {
        doTest(ShortData(value = 7.toShort()), config, serializer, assertion)
    }

    protected fun doTestByte(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<Byte>? = null,
        assertion: SavedStateAssertionScope<Byte>.() -> Unit,
    ) {
        doTest(7.toByte(), config, serializer, assertion)
    }

    protected fun doTestByteData(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<ByteData>? = null,
        assertion: SavedStateAssertionScope<ByteData>.() -> Unit,
    ) {
        doTest(ByteData(value = 7.toByte()), config, serializer, assertion)
    }

    protected fun doTestBoolean(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<Boolean>? = null,
        assertion: SavedStateAssertionScope<Boolean>.() -> Unit,
    ) {
        doTest(true, config, serializer, assertion) // Or false
    }

    protected fun doTestBooleanData(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<BooleanData>? = null,
        assertion: SavedStateAssertionScope<BooleanData>.() -> Unit,
    ) {
        doTest(BooleanData(value = true), config, serializer, assertion) // Or false
    }

    protected fun doTestChar(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<Char>? = null,
        assertion: SavedStateAssertionScope<Char>.() -> Unit,
    ) {
        doTest('a', config, serializer, assertion) // Or any other char
    }

    protected fun doTestCharData(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<CharData>? = null,
        assertion: SavedStateAssertionScope<CharData>.() -> Unit,
    ) {
        doTest(CharData(value = 'a'), config, serializer, assertion) // Or any other char
    }

    protected fun doTestFloat(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<Float>? = null,
        assertion: SavedStateAssertionScope<Float>.() -> Unit,
    ) {
        doTest(7.0f, config, serializer, assertion)
    }

    protected fun doTestFloatData(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<FloatData>? = null,
        assertion: SavedStateAssertionScope<FloatData>.() -> Unit,
    ) {
        doTest(FloatData(value = 7.0f), config, serializer, assertion)
    }

    protected fun doTestDouble(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<Double>? = null,
        assertion: SavedStateAssertionScope<Double>.() -> Unit,
    ) {
        doTest(7.0, config, serializer, assertion)
    }

    protected fun doTestDoubleData(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<DoubleData>? = null,
        assertion: SavedStateAssertionScope<DoubleData>.() -> Unit,
    ) {
        doTest(DoubleData(value = 7.0), config, serializer, assertion)
    }

    protected fun doTestIntList(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<List<Int>>? = null,
        assertion: SavedStateAssertionScope<List<Int>>.() -> Unit,
    ) {
        doTest(listOf(1, 2, 3), config, serializer, assertion)
    }

    protected fun doTestListIntData(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<ListIntData>? = null,
        assertion: SavedStateAssertionScope<ListIntData>.() -> Unit,
    ) {
        doTest(ListIntData(listOf(1, 2, 3)), config, serializer, assertion)
    }

    protected fun doTestStringList(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<List<String>>? = null,
        assertion: SavedStateAssertionScope<List<String>>.() -> Unit,
    ) {
        doTest(listOf("a", "b", "c"), config, serializer, assertion)
    }

    protected fun doTestListStringData(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<ListStringData>? = null,
        assertion: SavedStateAssertionScope<ListStringData>.() -> Unit,
    ) {
        doTest(ListStringData(listOf("a", "b", "c")), config, serializer, assertion)
    }

    protected fun doTestBooleanArray(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<BooleanArray>? = null,
        assertion: SavedStateAssertionScope<BooleanArray>.() -> Unit,
    ) {
        doTest(booleanArrayOf(true, false, true), config, serializer, assertion)
    }

    protected fun doTestBooleanArrayData(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<BooleanArrayData>? = null,
        assertion: SavedStateAssertionScope<BooleanArrayData>.() -> Unit,
    ) {
        doTest(BooleanArrayData(booleanArrayOf(true, false, true)), config, serializer, assertion)
    }

    protected fun doTestCharArray(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<CharArray>? = null,
        assertion: SavedStateAssertionScope<CharArray>.() -> Unit,
    ) {
        doTest(charArrayOf('a', 'b', 'c'), config, serializer, assertion)
    }

    protected fun doTestCharArrayData(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<CharArrayData>? = null,
        assertion: SavedStateAssertionScope<CharArrayData>.() -> Unit,
    ) {
        doTest(CharArrayData(charArrayOf('a', 'b', 'c')), config, serializer, assertion)
    }

    protected fun doTestDoubleArray(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<DoubleArray>? = null,
        assertion: SavedStateAssertionScope<DoubleArray>.() -> Unit,
    ) {
        doTest(doubleArrayOf(1.0, 2.0, 3.0), config, serializer, assertion)
    }

    protected fun doTestDoubleArrayData(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<DoubleArrayData>? = null,
        assertion: SavedStateAssertionScope<DoubleArrayData>.() -> Unit,
    ) {
        doTest(DoubleArrayData(doubleArrayOf(1.0, 2.0, 3.0)), config, serializer, assertion)
    }

    protected fun doTestFloatArray(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<FloatArray>? = null,
        assertion: SavedStateAssertionScope<FloatArray>.() -> Unit,
    ) {
        doTest(floatArrayOf(1.0f, 2.0f, 3.0f), config, serializer, assertion)
    }

    protected fun doTestFloatArrayData(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<FloatArrayData>? = null,
        assertion: SavedStateAssertionScope<FloatArrayData>.() -> Unit,
    ) {
        doTest(FloatArrayData(floatArrayOf(1.0f, 2.0f, 3.0f)), config, serializer, assertion)
    }

    protected fun doTestIntArray(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<IntArray>? = null,
        assertion: SavedStateAssertionScope<IntArray>.() -> Unit,
    ) {
        doTest(intArrayOf(1, 2, 3), config, serializer, assertion)
    }

    protected fun doTestIntArrayData(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<IntArrayData>? = null,
        assertion: SavedStateAssertionScope<IntArrayData>.() -> Unit,
    ) {
        doTest(IntArrayData(intArrayOf(1, 2, 3)), config, serializer, assertion)
    }

    protected fun doTestLongArray(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<LongArray>? = null,
        assertion: SavedStateAssertionScope<LongArray>.() -> Unit,
    ) {
        doTest(longArrayOf(1L, 2L, 3L), config, serializer, assertion)
    }

    protected fun doTestLongArrayData(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<LongArrayData>? = null,
        assertion: SavedStateAssertionScope<LongArrayData>.() -> Unit,
    ) {
        doTest(LongArrayData(longArrayOf(1L, 2L, 3L)), config, serializer, assertion)
    }

    protected fun doTestStringArray(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<Array<String>>? = null,
        assertion: SavedStateAssertionScope<Array<String>>.() -> Unit,
    ) {
        doTest(arrayOf("a", "b", "c"), config, serializer, assertion)
    }

    protected fun doTestStringArrayData(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<StringArrayData>? = null,
        assertion: SavedStateAssertionScope<StringArrayData>.() -> Unit,
    ) {
        doTest(StringArrayData(arrayOf("a", "b", "c")), config, serializer, assertion)
    }

    protected fun doTestBoxData(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<BoxData<String>>? = null,
        assertion: SavedStateAssertionScope<BoxData<String>>.() -> Unit,
    ) {
        doTest(BoxData("abc"), config, serializer, assertion)
    }

    protected fun doTestObject(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<ObjectData>? = null,
        assertion: SavedStateAssertionScope<ObjectData>.() -> Unit,
    ) {
        doTest(ObjectData, config, serializer, assertion)
    }

    protected fun doTestClassDiscriminatorConflict(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<ClassDiscriminatorConflict>? = null,
    ) {
        doEncodeToSavedState(ClassDiscriminatorConflict(123), serializer, config)
    }

    protected fun doTestSerialName(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<SerialNameType>? = null,
        assertion: SavedStateAssertionScope<SerialNameType>.() -> Unit,
    ) {
        doTest(SerialNameType(123), config, serializer, assertion)
    }

    protected fun doTestSerialNameData(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<SerialNameData>? = null,
        assertion: SavedStateAssertionScope<SerialNameData>.() -> Unit,
    ) {
        doTest(SerialNameData(SerialNameType(456)), config, serializer, assertion)
    }

    protected fun doTestSealed(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<Sealed>? = null,
        assertion: SavedStateAssertionScope<Sealed>.() -> Unit,
    ) {
        doTest(SealedImpl1(1), config, serializer, assertion)
    }

    protected fun doTestSealedData(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<SealedData>? = null,
        assertion: SavedStateAssertionScope<SealedData>.() -> Unit,
    ) {
        doTest(SealedData(SealedImpl1(7), SealedImpl2("a")), config, serializer, assertion)
    }

    protected fun doTestEnum(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<Enum>? = null,
        assertion: SavedStateAssertionScope<Enum>.() -> Unit,
    ) {
        doTest(Enum.OptionB, config, serializer, assertion)
    }

    protected fun doTestEnumData(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<EnumData>? = null,
        assertion: SavedStateAssertionScope<EnumData>.() -> Unit,
    ) {
        doTest(EnumData(Enum.OptionA, Enum.OptionB), config, serializer, assertion)
    }

    protected fun doTestPolymorphicInterface(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<PolymorphicInterface>? =
            PolymorphicSerializer(PolymorphicInterface::class), // Required in Kotlin/Native.
        assertion: SavedStateAssertionScope<PolymorphicInterface>.() -> Unit,
    ) {
        doTest(PolymorphicInterfaceImpl1(7), config, serializer, assertion)
    }

    protected fun doTestPolymorphicInterfaceData(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<PolymorphicInterfaceData>? = null,
        assertion: SavedStateAssertionScope<PolymorphicInterfaceData>.() -> Unit,
    ) {
        val data =
            PolymorphicInterfaceData(PolymorphicInterfaceImpl1(7), PolymorphicInterfaceImpl2("a"))
        doTest(data, config, serializer, assertion)
    }

    protected fun doTestPolymorphicClass(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<PolymorphicClass>? =
            PolymorphicSerializer(PolymorphicClass::class), // Required in Kotlin/Native.
        assertion: SavedStateAssertionScope<PolymorphicClass>.() -> Unit,
    ) {
        doTest(PolymorphicClassImpl1(7), config, serializer, assertion)
    }

    protected fun doTestPolymorphicClassData(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<PolymorphicClassData>? = null,
        assertion: SavedStateAssertionScope<PolymorphicClassData>.() -> Unit,
    ) {
        val data = PolymorphicClassData(PolymorphicClassImpl1(7), PolymorphicClassImpl2("a"))
        doTest(data, config, serializer, assertion)
    }

    protected fun doTestPolymorphicMixedData(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<PolymorphicMixedData>? = null,
        assertion: SavedStateAssertionScope<PolymorphicMixedData>.() -> Unit,
    ) {
        val data =
            PolymorphicMixedData(
                base1 = PolymorphicClassImpl1(2),
                base2 = PolymorphicInterfaceImpl1(3),
            )
        doTest(data, config, serializer, assertion)
    }

    protected fun doTestPolymorphicNullMixedData(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<PolymorphicNullMixedData>? = null,
        assertion: SavedStateAssertionScope<PolymorphicNullMixedData>.() -> Unit,
    ) {
        val data =
            PolymorphicNullMixedData(
                base1 = null,
                base2 = null,
            )
        doTest(data, config, serializer, assertion)
    }

    protected fun doTestContextual(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<ContextualType>? = null,
        assertion: SavedStateAssertionScope<ContextualType>.() -> Unit,
    ) {
        doTest(ContextualType("a", "b"), config, serializer, assertion)
    }

    protected fun doTestContextualData(
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<ContextualData>? = null,
        assertion: SavedStateAssertionScope<ContextualData>.() -> Unit,
    ) {
        doTest(ContextualData(ContextualType("a", "b")), config, serializer, assertion)
    }

    /**
     * Executes the full encode-decode test for an instance of `T`.
     *
     * @param original The original instance to be tested.
     * @param serializer Optional `KSerializer<T>` for custom serialization.
     * @param config Optional `SavedStateConfig` for encoding configuration.
     * @param assertion A block to perform additional assertions on the test results.
     */
    protected inline fun <reified T : Any> doTest(
        original: T,
        config: SavedStateConfig? = this.config,
        serializer: KSerializer<T>? = null,
        noinline assertion: SavedStateAssertionScope<T>.() -> Unit,
    ) {
        val platformAgnosticSerialized = doEncodeToSavedState(original, serializer, config)
        val platformSpecificSerialized = platformEncodeDecode(platformAgnosticSerialized)
        val deserialized = doDecodeFromSavedState<T>(platformSpecificSerialized, serializer, config)
        val scope =
            SavedStateAssertionScope(
                original = original,
                deserialized = deserialized,
                serialized = platformSpecificSerialized,
                platformRepresentation = platformAgnosticSerialized.read { contentDeepToString() },
                representation = platformSpecificSerialized.read { contentDeepToString() },
            )
        assertion(scope)
    }

    /**
     * Encodes an instance of `T` into a `SavedState` using optional serialization and
     * configuration.
     */
    protected inline fun <reified T : Any> doEncodeToSavedState(
        original: T,
        serializer: KSerializer<T>? = null,
        config: SavedStateConfig? = this.config,
    ): SavedState {
        return when {
            serializer != null && config != null -> encodeToSavedState(serializer, original, config)
            serializer != null -> encodeToSavedState(serializer, original)
            config != null -> encodeToSavedState(original, config)
            else -> encodeToSavedState(original)
        }
    }

    /** Decodes a `SavedState` back into an instance of `T`. */
    protected inline fun <reified T : Any> doDecodeFromSavedState(
        serialized: SavedState,
        strategy: DeserializationStrategy<T>? = null,
        config: SavedStateConfig? = this.config,
    ): T {
        return when {
            strategy != null && config != null -> decodeFromSavedState(strategy, serialized, config)
            strategy != null -> decodeFromSavedState(strategy, serialized)
            config != null -> decodeFromSavedState(serialized, config)
            else -> decodeFromSavedState(serialized)
        }
    }

    /**
     * A helper class that encapsulates the state of an encoding-decoding test for `SavedState`.
     *
     * This class provides a structured way to assert the correctness of the serialization process
     * by storing the original object, its encoded representation, and the final decoded instance.
     * It helps verify that the state remains consistent throughout the encoding-decoding cycle.
     *
     * @param T The type of the object being tested.
     * @param original The original [T] instance before encoding.
     * @param deserialized The [T] instance after decoding, expected to match the original.
     * @param serialized The [SavedState] representation of the object after encoding.
     * @param platformRepresentation A [String] representation tied to platform-specific
     *   serialization behavior (e.g., **after** parceling on Android).
     * @param representation A [String] representation of the encoded state, independent of
     *   platform-specific serialization formats (e.g., **before** parceling on Android).
     */
    protected class SavedStateAssertionScope<T>(
        val original: T,
        val deserialized: T,
        val serialized: SavedState,
        val platformRepresentation: String,
        val representation: String,
    )
}
