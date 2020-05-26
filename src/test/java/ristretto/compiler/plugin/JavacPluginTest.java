package ristretto.compiler.plugin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ristretto.compiler.plugin.TestCompilerMatchers.hasOutput;

class JavacPluginTest extends JavacPluginBaseTest {

    @Nested
    class output {

        TestCompiler.Result compilerResult;

        @BeforeEach
        void beforeEach() {
            var code = TestCompiler.SourceCode.of("ristretto.test", "TestSample", "",
                "package ristretto.test;",
                "",
                "import ristretto.PackagePrivate;",
                "",
                "public class TestSample {",
                "",
                "  String field1;",
                "  final String field2;",
                "",
                "  public void sampleMethod(String parameter) {",
                "    String s = parameter;",
                "  }",
                "",
                "  @PackagePrivate void anotherMethod() {",
                "  }",
                "",
                "}"
            );

            compilerResult = compile(code);
        }

        @Test
        void indicates_when_it_is_loaded() {
            assertThat(compilerResult, hasOutput("ristretto plugin loaded"));
        }

        @Test
        void prints_warn_messages() {
            assertThat(compilerResult, hasOutput("/test/TestSample.java:9 variable field2 has unnecessary final modifier"));
        }

        @Test
        void prints_summary() {
            assertThat(compilerResult, hasOutput(
                "summary:",
                "| rule                                     | inspected   | final   | skipped | annotated |",
                "|------------------------------------------|-------------|---------|---------|-----------|",
                "| DefaultFieldImmutabilityRule             |           2 |  50.00% |  50.00% |     0.00% |",
                "| DefaultParameterImmutabilityRule         |           1 | 100.00% |   0.00% |     0.00% |",
                "| DefaultLocalVariableImmutabilityRule     |           1 | 100.00% |   0.00% |     0.00% |",
                "| DefaultFieldAccessRule                   |           2 | 100.00% |   0.00% |     0.00% |"
            ));
        }
    }

    @Test
    void ignores_specified_package() {
        var code = TestCompiler.SourceCode.of("ristretto.test", "TestSample", "",
            "package ristretto.test;",
            "",
            "public class TestSample {",
            "",
            "  public static String test(String parameter) {",
            "    parameter += \":value\";",
            "    return parameter;",
            "  }",
            "",
            "}"
        );

        var result = compile(code, "--ignore-packages=ristretto.test").invoke("ristretto.test.TestSample", "test", "value");

        assertThat(result, is("value:value"));
    }
}
