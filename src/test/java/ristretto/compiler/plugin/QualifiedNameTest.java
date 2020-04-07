package ristretto.compiler.plugin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

class QualifiedNameTest {

    @Nested
    class when_package_is_present {

        QualifiedName name;

        @BeforeEach
        void beforeEach() {
            name = QualifiedName.parse("some.package.ClassName");
        }

        @Test
        void parses_package_name() {
            assertThat(name.packageName(), is(Optional.of("some.package")));
        }

        @Test
        void parses_simple_name() {
            assertThat(name.simpleName(), is("ClassName"));
        }
    }

    @Nested
    class when_package_is_not_present {

        QualifiedName name;

        @BeforeEach
        void beforeEach() {
            name = QualifiedName.parse("ClassName");
        }

        @Test
        void parses_package_name() {
            assertThat(name.packageName(), is(Optional.empty()));
        }

        @Test
        void parses_simple_name() {
            assertThat(name.simpleName(), is("ClassName"));
        }
    }

    @Nested
    class when_based_on_class {

        QualifiedName name;

        @BeforeEach
        void beforeEach() {
            name = QualifiedName.of(java.util.List.class);
        }

        @Test
        void parses_package_name() {
            assertThat(name.packageName(), is(Optional.of("java.util")));
        }

        @Test
        void parses_simple_name() {
            assertThat(name.simpleName(), is("List"));
        }
    }

    @Test
    void has_a_string_representation() {
        var name = QualifiedName.parse("some.package.ClassName");

        assertThat(name.toString(), is("some.package.ClassName"));
    }

    @Test
    void can_be_used_as_key() {
        var name = QualifiedName.parse("some.package.ClassName");
        var sameName = QualifiedName.parse("some.package.ClassName");
        var anotherName = QualifiedName.parse("some.package.AnotherClassName");

        Map<QualifiedName, String> map = Map.of(name, "value");

        assertThat(map, hasKey(sameName));
        assertThat(map, not(hasKey(anotherName)));
    }
}