package org.kie.openrewrite.recipe.jpmml;

import java.nio.file.Path;
import java.util.List;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.Assertions;
import org.openrewrite.java.Java11Parser;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;


class JPMMLRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        List<Path> paths =JavaParser.runtimeClasspath();
        spec.recipe(new JPMMLRecipe());
        spec.parser(Java11Parser.builder()
                            .classpath(paths)
                            .logCompilationWarningsAndErrors(true));
    }

    @Test
    void changeImports() {
        @Language("java")
        String before = "package com.yourorg;\n" +
                "import org.jpmml.model.inlinetable.InputCell;\n" +
                "class FooBar {\n" +
                "static void method() {\n" +
                "InputCell input = null;\n" +
                "}\n" +
                "}";
        String after = "package com.yourorg;\n" +
                "\n" +
                "import org.jpmml.model.cells.InputCell;\n" +
                "\n" +
                "class FooBar {\n" +
                "static void method() {\n" +
                "InputCell input = null;\n" +
                "}\n" +
                "}";
        rewriteRun(
                Assertions.java(before, after)
        );
    }

    @Test
    void removeFieldNameCreate() {
        @Language("java")
        String before = "package com.yourorg;\n" +
                "import org.dmg.pmml.FieldName;\n" +
                "class FooBar {\n" +
                "static void method() {\n" +
                "System.out.println(FieldName.create(\"OUTPUT_\"));\n" +
                "}\n" +
                "}";
        String after = "package com.yourorg;\n" +
                "\n" +
                "class FooBar {\n" +
                "static void method() {\n" +
                "System.out.println(\"OUTPUT_\");\n" +
                "}\n" +
                "}";
        rewriteRun(
                Assertions.java(before, after)
        );
    }

}