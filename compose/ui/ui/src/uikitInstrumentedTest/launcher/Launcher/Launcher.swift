/*
 * Copyright 2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import XCTest
import InstrumentedTest

class TestLauncher: XCTestCase {
    override class var defaultTestSuite: XCTestSuite {
        ConfigurationKt.testSuite()
    }
}
