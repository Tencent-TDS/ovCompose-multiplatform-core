/*
 * Copyright 2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.ui

import androidx.compose.xctest.setupXCTestSuite
import kotlinx.cinterop.ExperimentalForeignApi
import platform.XCTest.XCTestSuite

@OptIn(ExperimentalForeignApi::class)
fun testSuite(): XCTestSuite = setupXCTestSuite_ERROR_()
