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

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue

class FlaggedApiDetectorTest : LintDetectorTest() {
    override fun getIssues(): List<Issue> = listOf(FlaggedApiDetector.ISSUE)

    override fun getDetector(): Detector {
        return FlaggedApiDetector()
    }

    override fun lint(): TestLintTask {
        return super.lint().allowMissingSdk()
    }

    fun testDocumentationExample() {
        lint()
            .files(
                java(
                        """
            package test.api;
            import android.annotation.FlaggedApi;
            import com.example.foobar.Flags;

            @FlaggedApi(Flags.FLAG_FOOBAR)
            public class MyApi {
              public void apiMethod() { }
              public int apiField = 42;
            }
            """
                    )
                    .indented(),
                java(
                        """
            package test.pkg;
            import test.api.MyApi;
            import com.example.foobar.Flags;

            public class Test {
              public void test(MyApi api) {
                if (Flags.foobar()) {
                  api.apiMethod(); // OK
                  int val = api.apiField; // OK
                }
                api.apiMethod(); // ERROR 1
                int val = api.apiField; // ERROR 2
                Object o = MyApi.class; // ERROR 3
              }
            }
            """
                    )
                    .indented(),
                // Generated
                java(
                        """
            package com.example.foobar;

            public class Flags {
                public static final String FLAG_FOOBAR = "com.example.foobar.foobar";
                public static boolean foobar() { return true; }
            }
            """
                    )
                    .indented(),
                flaggedApiAnnotationStub,
            )
            .run()
            .expect(
                """
        src/test/pkg/Test.java:11: Error: Method apiMethod() is a flagged API and should be inside an if (Flags.foobar()) check (or annotate the surrounding method test with @FlaggedApi(Flags.FLAG_FOOBAR) to transfer requirement to caller) [AndroidXFlaggedApi]
            api.apiMethod(); // ERROR 1
            ~~~~~~~~~~~~~~~
        src/test/pkg/Test.java:12: Error: Field apiField is a flagged API and should be inside an if (Flags.foobar()) check (or annotate the surrounding method test with @FlaggedApi(Flags.FLAG_FOOBAR) to transfer requirement to caller) [AndroidXFlaggedApi]
            int val = api.apiField; // ERROR 2
                          ~~~~~~~~
        src/test/pkg/Test.java:13: Error: Class MyApi is a flagged API and should be inside an if (Flags.foobar()) check (or annotate the surrounding method test with @FlaggedApi(Flags.FLAG_FOOBAR) to transfer requirement to caller) [AndroidXFlaggedApi]
            Object o = MyApi.class; // ERROR 3
                       ~~~~~~~~~~~
        3 errors, 0 warnings
        """
            )
    }

    fun testCompiled() {
        lint()
            .files(
                compiled(
                    "libs/annotation.jar",
                    flaggedApiAnnotationStub,
                    0x81415584,
                    """
          android/annotation/FlaggedApi.class:
          H4sIAAAAAAAA/4WRwU4CMRCG/yLLKqigookHo/FA9OIePXja4BJJcJfsVhPj
          wRRoNiWlS5ZCwqt58AF8KOOsJsKBxEP/Tjrf/O1MP7/ePwDc4sTFkYumi2MG
          ZyH0XDI0r657Y7EQnhYm9RKbK5PeMVSTbJ4PZUdpYuodLdJUjvypuilYhtN4
          bqyayGc1UwMtfWMyK6zKzIzhbM1P/CU8LvJUWrK+3JwPtJxIY/lyKgkq85d+
          wFB5DPhDdM9Qa0dhwuOnNo9ienynG/TotO6HYcR93o3Ct9+Ci83msbTkTRFZ
          t/5B+plWwyWBTrvnJwk1JMwoz9RonV5NhKGx8osGYzm0DOcbr1iNqcXAUKK1
          RR/DytQvHIpKqPyoi23aPYp2iCm/wpGoolbIbiF7hewXUkejICQOcPgNDPA2
          HOgBAAA=
          """,
                ),
                compiled(
                    "libs/api.jar",
                    // Generated
                    java(
                            """
              package com.android.aconfig.test;

              public class Flags {
                  public static final String FLAG_DISABLED_RO = "com.android.aconfig.test.disabled_ro";
                  public static boolean disabledRo() {
                      return true; // not the real implementation
                  }
              }
              """
                        )
                        .indented(),
                    0xc07ff6ad,
                    """
          com/android/aconfig/test/Flags.class:
          H4sIAAAAAAAA/11PPUsDQRScl29jYmK0UVAQLNTirkyhCDExIhwGEklhEzZ3
          67Hhsgt3e/4qGyvBwh/gjxLfLRHBYue9nZ158/br++MTQB/7TZTRrWO3jh6h
          Ow4Gd4vR/WxwE9yOFtMJoResxIvwE6Fjf2ZTpeNLQntodGaFtnOR5LKBPULt
          Smllrwnls/M5oTI0kSR0AqXlQ75eyvRRLBNmmpHKii6aGqd9Ympm8jSUY+Xe
          x4mIM68IbaGBLcJxaNa+0FFqVOSL0OhnFftWZtZ3Ut76b8PJciVDSzhlj7fx
          eBuPV3i83/hFanCCEv8eIByggirXGt9KqPOhIpyxycwRV+JavXgHvTnDNmPN
          kWWWtdDeSA8dx0Mqr/90Be648Z0fKhia7H4BAAA=
          """,
                ),
                compiled(
                    "libs/api.jar",
                    java(
                            """
              package test.api;
              import android.annotation.FlaggedApi;
              import com.android.aconfig.test.Flags;

              @FlaggedApi(Flags.FLAG_DISABLED_RO)
              public class MyApi {
                public void apiMethod() { }
                public int apiField = 42;
              }
              """
                        )
                        .indented(),
                    0x6f573c19,
                    """
          test/api/MyApi.class:
          H4sIAAAAAAAA/0VQTUvDQBB926ZNm9a2foJ4EEFQc0iOHhShCIVCq6DiVbbZ
          NW5Jd0uyKfizPIjgwR/gjxIni9TDDDNv3ns7O98/n18AzrEXwMNWG3Vs+9jx
          scvQ4ks1UjITDGzM0LxUWtkrhvrp2SODd22EZOhPlJY35WIm8wc+ywhpk2wq
          7YshXXBvyjyRI1UNgunrcKmiOV9xhoO7Ulu1kGO9UoUi4VBrY7lVRhcMhxOu
          RW6UiPkajkcZT1MpyOOCobHiWUmmx4lZRH/kiCdGP6s0srKwkVBFtZB4yk0X
          PlpdNNBk6FXDmHaM3ToMg2qhOOM6jW9nc5lYHNEVPLoK/bvSUFWjiiwot6k7
          cT3QCT/AwkH4jtqboweUAxqCRJ6jd9aifceg+Kc2HeCToOve2ECvciW0TzEo
          sPkLyuJbCJ8BAAA=
          """,
                ),
                java(
                        """
            package test.pkg;
            import test.api.MyApi;
            import com.android.aconfig.test.Flags;

            public class Test {
              public void test(MyApi api) {
                if (Flags.disabledRo()) {
                  api.apiMethod(); // OK
                  int val = api.apiField; // OK
                }
                api.apiMethod(); // ERROR 1
                int val = api.apiField; // ERROR 2
                Object o = MyApi.class; // ERROR 3
              }
            }
            """
                    )
                    .indented(),
            )
            .skipTestModes(TestMode.SOURCE_ONLY)
            .run()
            .expect(
                """
        src/test/pkg/Test.java:11: Error: Method apiMethod() is a flagged API and should be inside an if (Flags.disabledRo()) check (or annotate the surrounding method test with @FlaggedApi(Flags.FLAG_DISABLED_RO) to transfer requirement to caller) [AndroidXFlaggedApi]
            api.apiMethod(); // ERROR 1
            ~~~~~~~~~~~~~~~
        src/test/pkg/Test.java:12: Error: Field apiField is a flagged API and should be inside an if (Flags.disabledRo()) check (or annotate the surrounding method test with @FlaggedApi(Flags.FLAG_DISABLED_RO) to transfer requirement to caller) [AndroidXFlaggedApi]
            int val = api.apiField; // ERROR 2
                          ~~~~~~~~
        src/test/pkg/Test.java:13: Error: Class MyApi is a flagged API and should be inside an if (Flags.disabledRo()) check (or annotate the surrounding method test with @FlaggedApi(Flags.FLAG_DISABLED_RO) to transfer requirement to caller) [AndroidXFlaggedApi]
            Object o = MyApi.class; // ERROR 3
                       ~~~~~~~~~~~
        3 errors, 0 warnings
        """
            )
    }

    fun testCamelCaseFlagName() {
        lint()
            .files(
                java(
                        """
            package test.pkg;
            import test.api.MyApi;
            import com.android.aconfig.test.Flags;

            public class Test {
              public void test(MyApi api) {
                if (Flags.enabledFixedRo()) {
                  api.apiMethod(); // OK
                }
                api.apiMethod(); // ERROR 1
              }
            }
            """
                    )
                    .indented(),
                java(
                        """
            package test.api;
            import android.annotation.FlaggedApi;
            import com.android.aconfig.test.Flags;

            public class MyApi {
              @FlaggedApi(Flags.FLAG_ENABLED_FIXED_RO)
              public void apiMethod() { }
            }
            """
                    )
                    .indented(),
                // Generated code:
                java(
                        """
            package com.android.aconfig.test;
            public final class Flags {
                public static final String FLAG_DISABLED_RO = "com.android.aconfig.test.disabled_ro";
                public static final String FLAG_DISABLED_RW = "com.android.aconfig.test.disabled_rw";
                public static final String FLAG_ENABLED_FIXED_RO = "com.android.aconfig.test.enabled_fixed_ro";
                public static final String FLAG_ENABLED_RO = "com.android.aconfig.test.enabled_ro";
                public static final String FLAG_ENABLED_RW = "com.android.aconfig.test.enabled_rw";

                public static boolean disabledRo() {
                    return FEATURE_FLAGS.disabledRo();
                }

                public static boolean disabledRw() {
                    return FEATURE_FLAGS.disabledRw();
                }

                public static boolean enabledFixedRo() {
                    return FEATURE_FLAGS.enabledFixedRo();
                }

                public static boolean enabledRo() {
                    return FEATURE_FLAGS.enabledRo();
                }
                public static boolean enabledRw() {
                    return FEATURE_FLAGS.enabledRw();
                }
            }
            """
                    )
                    .indented(),
                flaggedApiAnnotationStub,
            )
            .run()
            .expect(
                """
        src/test/pkg/Test.java:10: Error: Method apiMethod() is a flagged API and should be inside an if (Flags.enabledFixedRo()) check (or annotate the surrounding method test with @FlaggedApi(Flags.FLAG_ENABLED_FIXED_RO) to transfer requirement to caller) [AndroidXFlaggedApi]
            api.apiMethod(); // ERROR 1
            ~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """
            )
    }

    fun testPartOfApi() {
        // Make sure we don't flag calls to APIs from within other parts of the
        // same API (e.g. also annotated with the same annotation)
        lint()
            .files(
                java(
                        """
            package test.api;
            import android.annotation.FlaggedApi;
            import com.example.foobar.Flags;

            @FlaggedApi(Flags.FLAG_FOOBAR)
            public class MyApi {
              public void apiMethod() { }
            }
            """
                    )
                    .indented(),
                java(
                        """
            package test.api;
            import android.annotation.FlaggedApi;
            import com.example.foobar.Flags;

            @FlaggedApi(Flags.FLAG_FOOBAR)
            public class MyApi2 {
              public void apiMethod(MyApi api) {
                  api.apiMethod(); // OK
              }
            }
            """
                    )
                    .indented(),
                java(
                        """
            package test.api;
            import android.annotation.FlaggedApi;
            import com.example.foobar.Flags;

            @FlaggedApi(Flags.FLAG_UNRELATED)
            public class Test {
              public void apiMethod(MyApi api) {
                  api.apiMethod(); // ERROR: Flagged, but different API so still an error
              }
            }
            """
                    )
                    .indented(),
                // Generated
                java(
                        """
            package com.example.foobar;

            public class Flags {
                public static final String FLAG_FOOBAR = "foobar";
                public static final String FLAG_UNRELATED = "unrelated";
                public static boolean foobar() { return true; }
                public static boolean unrelated() { return true; }
            }
            """
                    )
                    .indented(),
                flaggedApiAnnotationStub,
            )
            .run()
            .expect(
                """
        src/test/api/Test.java:8: Error: Method apiMethod() is a flagged API and should be inside an if (Flags.foobar()) check (or annotate the surrounding method apiMethod with @FlaggedApi(Flags.FLAG_FOOBAR) to transfer requirement to caller) [AndroidXFlaggedApi]
              api.apiMethod(); // ERROR: Flagged, but different API so still an error
              ~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """
            )
    }

    fun testBasic() {
        // Test case from b/303434307#comment2
        lint()
            .files(
                java(
                    """
          package test.pkg;

          public final class Flags {
              public static final String FLAG_MY_FLAG = "myFlag";
              public static boolean myFlag() { return true; }
          }
          """
                ),
                java(
                        """
            package test.pkg;

            import android.annotation.FlaggedApi;

            public class JavaTest {
                @FlaggedApi(Flags.FLAG_MY_FLAG)
                class Foo {
                    public void someMethod() { }
                }

                public void testValid1() {
                    if (Flags.myFlag()) {
                        Foo f = new Foo(); // OK 1
                        f.someMethod();    // OK 2
                    }
                }
            }
            """
                    )
                    .indented(),
                flaggedApiAnnotationStub,
            )
            .run()
            .expectClean()
    }

    fun testInterprocedural() {
        // Test case from b/303434307#comment2
        lint()
            .files(
                java(
                    """
          package test.pkg;

          public final class Flags {
              public static final String FLAG_MY_FLAG = "myFlag";
              public static boolean myFlag() { return true; }
          }
          """
                ),
                java(
                        """
            package test.pkg;

            import android.annotation.FlaggedApi;

            public class JavaTest {
                static class Foo {
                    @FlaggedApi(Flags.FLAG_MY_FLAG)
                    static void flaggedApi() {
                    }
                }

                void outer() {
                    if (Flags.myFlag()) {
                        inner();
                    }
                }

                void inner() {
                    // In theory valid because FLAG_MY_FLAG was checked earlier in the call-chain,
                    // but we don't do inter procedural analysis
                    Foo.flaggedApi(); // ERROR
                }
            }
            """
                    )
                    .indented(),
                flaggedApiAnnotationStub,
            )
            .run()
            .expect(
                """
        src/test/pkg/JavaTest.java:21: Error: Method flaggedApi() is a flagged API and should be inside an if (Flags.myFlag()) check (or annotate the surrounding method inner with @FlaggedApi(Flags.FLAG_MY_FLAG) to transfer requirement to caller) [AndroidXFlaggedApi]
                Foo.flaggedApi(); // ERROR
                ~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """
            )
    }

    fun testApiGating() {
        // Test case from b/303434307#comment2
        lint()
            .files(
                java(
                    """
          package test.pkg;

          public final class Flags {
              public static final String FLAG_MY_FLAG = "myFlag";
              public static boolean myFlag() { return true; }
          }
          """
                ),
                java(
                        """
            package test.pkg;

            import android.annotation.FlaggedApi;

            public class JavaTest {
                interface MyInterface {
                    void bar();
                }

                static class OldImpl implements MyInterface {
                    @Override
                    public void bar() {
                    }
                }

                @FlaggedApi(Flags.FLAG_MY_FLAG)
                static class NewImpl implements MyInterface {
                    @Override
                    public void bar() {
                    }
                 }

                 void test(MyInterface f) {
                     MyInterface obj = null;
                     if (Flags.myFlag()) {
                         obj = new NewImpl();
                     } else {
                         obj = new OldImpl();
                     }
                     f.bar();
                 }
            }
            """
                    )
                    .indented(),
                flaggedApiAnnotationStub,
            )
            .run()
            .expectClean()
    }

    fun testFinalFields() {
        // Test case from b/303434307#comment2
        lint()
            .files(
                java(
                    """
          package test.pkg;

          public final class Flags {
              public static final String FLAG_MY_FLAG = "myFlag";
              public static boolean myFlag() { return true; }
          }
          """
                ),
                java(
                        """
            package test.pkg;

            import android.annotation.FlaggedApi;

            public class JavaTest {
                static class Bar {
                    @FlaggedApi(Flags.FLAG_MY_FLAG)
                    public void bar() { }
                }
                static class Foo {
                    private static final boolean useNewStuff = Flags.myFlag();
                    private final Bar mBar = new Bar();

                    void someMethod() {
                        if (useNewStuff) {
                            // OK because flags can't change value without a reboot, though this might change in
                            // the future and in that case caching the flag value would be an error. We can restart
                            // apps due to a server push of new flag values but restarting the framework would be
                            // too disruptive
                            mBar.bar(); // OK
                        }
                    }
                }
            }
            """
                    )
                    .indented(),
                flaggedApiAnnotationStub,
            )
            .run()
            .expectClean()
    }

    fun testInverseLogic() {
        lint()
            .files(
                java(
                    """
          package test.pkg;

          public final class Flags {
              public static final String FLAG_MY_FLAG = "myFlag";
              public static boolean myFlag() { return true; }
          }
          """
                ),
                java(
                        """
            package test.pkg;

            import android.annotation.FlaggedApi;

            public class JavaTest {
                @FlaggedApi(Flags.FLAG_MY_FLAG)
                class Foo {
                    public void someMethod() { }
                }

                public void testInverse() {
                    if (!Flags.myFlag()) {
                        // ...
                    } else {
                        Foo f = new Foo(); // OK 1
                        f.someMethod();    // OK 2
                    }
                }
            }
            """
                    )
                    .indented(),
                flaggedApiAnnotationStub,
            )
            .run()
            .expectClean()
    }

    fun testAnded() {
        lint()
            .files(
                java(
                    """
          package test.pkg;

          public final class Flags {
              public static final String FLAG_MY_FLAG = "myFlag";
              public static boolean myFlag() { return true; }
          }
          """
                ),
                java(
                        """
            package test.pkg;

            import android.annotation.FlaggedApi;
            import static test.pkg.Flags.myFlag;

            /** @noinspection InstantiationOfUtilityClass, AccessStaticViaInstance , ResultOfMethodCallIgnored , StatementWithEmptyBody */
            public class JavaTest {
                @FlaggedApi(Flags.FLAG_MY_FLAG)
                public static class Foo {
                    public static boolean someMethod() { return true; }
                }

                public void testValid1(boolean something) {
                    if (true && something && Flags.myFlag()) {
                        Foo f = new Foo(); // OK 1
                        f.someMethod();    // OK 2
                    }
                }

                public void testValid2(boolean something) {
                    if (something || !Flags.myFlag()) {
                    } else {
                        Foo f = new Foo(); // OK 3
                        f.someMethod();    // OK 4
                    }
                }

                public void testValid3(Foo f, boolean something) {
                    // b/b/383061307
                    if (Flags.myFlag() && f.someMethod()) { // OK 5
                    }
                    if (myFlag() && f.someMethod()) { // OK 6
                    }
                    if (Flags.myFlag() && something && f.someMethod()) { // OK 7
                    }
                }
            }
            """
                    )
                    .indented(),
                flaggedApiAnnotationStub,
            )
            .run()
            .expectClean()
    }

    fun testEarlyReturns() {
        lint()
            .files(
                java(
                    """
          package test.pkg;

          public final class Flags {
              public static final String FLAG_MY_FLAG = "myFlag";
              public static boolean myFlag() { return true; }
          }
          """
                ),
                java(
                        """
            package test.pkg;

            import android.annotation.FlaggedApi;

            public class JavaTest {
                @FlaggedApi(Flags.FLAG_MY_FLAG)
                class Foo {
                    public void someMethod() { }
                }

                public void testSimpleEarlyReturn() {
                    if (!Flags.myFlag()) {
                        return;
                    }
                    Foo f = new Foo(); // OK 1
                    f.someMethod();    // OK 2
                }

                public void testEarlyReturn() {
                    int log;
                    {
                        if (!Flags.myFlag()) {
                            return;
                        }
                    }
                    // These are fine -- but we don't do more complex
                    // flow analysis here as in the SDK_INT version checker
                    // here, we only check very simple scenarios
                    Foo f = new Foo(); // ERROR 1
                    f.someMethod();    // ERROR 2
                }
            }
            """
                    )
                    .indented(),
                flaggedApiAnnotationStub,
            )
            .run()
            .expect(
                """
        src/test/pkg/JavaTest.java:29: Error: Method null() is a flagged API and should be inside an if (Flags.myFlag()) check (or annotate the surrounding method testEarlyReturn with @FlaggedApi(Flags.FLAG_MY_FLAG) to transfer requirement to caller) [AndroidXFlaggedApi]
                Foo f = new Foo(); // ERROR 1
                        ~~~~~~~~~
        src/test/pkg/JavaTest.java:30: Error: Method someMethod() is a flagged API and should be inside an if (Flags.myFlag()) check (or annotate the surrounding method testEarlyReturn with @FlaggedApi(Flags.FLAG_MY_FLAG) to transfer requirement to caller) [AndroidXFlaggedApi]
                f.someMethod();    // ERROR 2
                ~~~~~~~~~~~~~~
        2 errors, 0 warnings
        """
            )
    }

    fun testAnnotations() {
        lint()
            .files(
                java(
                        """
            package test.pkg;

            import android.annotation.FlaggedApi;

            @FlaggedApi("test.pkg.FLAG_MY_FLAG")
            public class JavaTest {
                @FlaggedApi("FLAG_MY_FLAG")
                class Foo {
                    public void someMethod() { }
                }
            }
            """
                    )
                    .indented(),
                flaggedApiAnnotationStub,
            )
            .run()
            .expect(
                """
        src/test/pkg/JavaTest.java:7: Error: Invalid @FlaggedApi descriptor; should be package.name [AndroidXFlaggedApi]
            @FlaggedApi("FLAG_MY_FLAG")
                        ~~~~~~~~~~~~~~
        src/test/pkg/JavaTest.java:5: Warning: @FlaggedApi should specify an actual flag constant; raw strings are discouraged (and more importantly, not enforced) [AndroidXFlaggedApi]
        @FlaggedApi("test.pkg.FLAG_MY_FLAG")
                    ~~~~~~~~~~~~~~~~~~~~~~~
        1 errors, 1 warnings
        """
            )
    }
}

private val flaggedApiAnnotationStub: TestFile =
    java(
            """
      package android.annotation; // HIDE-FROM-DOCUMENTATION

      import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
      import static java.lang.annotation.ElementType.CONSTRUCTOR;
      import static java.lang.annotation.ElementType.FIELD;
      import static java.lang.annotation.ElementType.METHOD;
      import static java.lang.annotation.ElementType.TYPE;

      import java.lang.annotation.Retention;
      import java.lang.annotation.RetentionPolicy;
      import java.lang.annotation.Target;

      @Target({TYPE, METHOD, CONSTRUCTOR, FIELD, ANNOTATION_TYPE})
      @Retention(RetentionPolicy.CLASS)
      public @interface FlaggedApi {
          String value();
      }
      """
        )
        .indented()
