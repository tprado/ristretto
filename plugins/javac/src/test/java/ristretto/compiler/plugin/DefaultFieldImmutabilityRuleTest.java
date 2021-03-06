package ristretto.compiler.plugin;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static ristretto.compiler.plugin.TestCompilerMatchers.hasOutput;

class DefaultFieldImmutabilityRuleTest extends JavacPluginBaseTest {

  @Nested
  class classes {

    @Test
    void enforces_fields_to_be_final() {
      var code = TestCompiler.SourceCode.of("",
        "package ristretto.test;",
        "",
        "public class TestSample {",
        "  ",
        "  String msg;",
        "  ",
        "  public static String hello(String value) {",
        "    TestSample sample = new TestSample();",
        "    sample.msg = value;",
        "    return sample.getMsg();",
        "  }",
        "  ",
        "  String getMsg() {",
        "    return msg;",
        "  }",
        "}"
      );

      TestCompiler.Result result = compile(code);

      assertThat(result.diagnostics(), containsString("TestSample.java:10: error: cannot assign a value to final variable msg"));
    }

    @Test
    void skips_annotated_fields() {
      var code = TestCompiler.SourceCode.of("",
        "package ristretto.test;",
        "",
        "import ristretto.Mutable;",
        "",
        "public class TestSample {",
        "  ",
        "  @Mutable",
        "  String msg;",
        "  ",
        "  public static String hello(String value) {",
        "    TestSample sample = new TestSample();",
        "    sample.msg = value;",
        "    return sample.getMsg();",
        "  }",
        "  ",
        "  String getMsg() {",
        "    return msg;",
        "  }",
        "}"
      );

      var result = compile(code).invoke("ristretto.test.TestSample", "hello", "hello world");

      assertThat(result, is("hello world"));
    }

    @Test
    void skips_volatile_fields() {
      var code = TestCompiler.SourceCode.of("",
        "package ristretto.test;",
        "",
        "import ristretto.Mutable;",
        "",
        "public class TestSample {",
        "  ",
        "  volatile long counter;",
        "  ",
        "  public static String hello(String value) {",
        "    TestSample sample = new TestSample();",
        "    sample.counter = Long.parseLong(value) + 1;",
        "    return String.valueOf(sample.get());",
        "  }",
        "  ",
        "  long get() {",
        "    return counter;",
        "  }",
        "}"
      );

      var result = compile(code).invoke("ristretto.test.TestSample", "hello", "10");

      assertThat(result, is("11"));
    }
  }

  @Nested
  class enums {

    @Test
    void enforces_fields_to_be_final() {
      var code = TestCompiler.SourceCode.of(
        "package ristretto.test;",
        "",
        "public enum TestSample {",
        "  ",
        "  INSTANCE;",
        "  ",
        "  String value;",
        "  ",
        "}"
      );

      var result = compile(code);

      assertThat(result.diagnostics(), containsString("TestSample.java:7: error: variable value not initialized in the default constructor"));
    }

    @Test
    void warns_about_final_fields() {
      var code = TestCompiler.SourceCode.of(
        "package ristretto.test;",
        "",
        "public enum TestSample {",
        "  ",
        "  INSTANCE(\"value\");",
        "  ",
        "  final String value;",
        "}"
      );

      var result = compile(code);

      assertThat(result, hasOutput("DefaultFieldImmutabilityRule /test/TestSample.java:7 MODIFIER_ALREADY_PRESENT"));
    }

    @Test
    void does_not_warn_about_static_final_fields() {
      var code = TestCompiler.SourceCode.of(
        "package ristretto.test;",
        "",
        "public enum TestSample {",
        "  ",
        "  INSTANCE",
        "  ",
        "}"
      );

      var result = compile(code);

      assertThat(result.additionalOutput, not(containsString("warn")));
    }
  }
}
