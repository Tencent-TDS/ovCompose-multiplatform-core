import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.RemoteWebDriver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

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

class SeleniumTests {

    private val path = "file:///${System.getenv("HOME")}/compose-web-tmp/index.html"

    private fun openTestPage(test: String) {
        driver.get("$path?test=$test")
    }

    private val options = ChromeOptions().apply {
        addArguments("--headless")
    }

    private lateinit var driver: RemoteWebDriver

    @BeforeTest
    fun before() {
       driver = ChromeDriver(options)
    }

    @AfterTest
    fun after() {
        driver.close()
        driver.quit()
    }

    @Test
    fun test() {
        openTestPage("testCase1")
        assertEquals(
            expected = "Hello World!",
            actual = driver.findElementByTagName("div").text
        )
    }

    @Test
    fun test2() {
        openTestPage("testCase1")
        assertEquals(
            expected = "Hello World!",
            actual = driver.findElementByTagName("div").text
        )
    }
}