package org.kie.openrewrite.recipe.jpmml;

import java.io.InputStream;
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

    private static final String JPMML_RECIPE_NAME = "org.kie.openrewrite.recipe.jpmml.JPMMLRecipe";

    @Override
    public void defaults(RecipeSpec spec) {
        List<Path> paths =JavaParser.runtimeClasspath();
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("META-INF/rewrite/rewrite.yml")) {
            spec.recipe(inputStream, JPMML_RECIPE_NAME);
            spec.parser(Java11Parser.builder()
                                .classpath(paths)
                                .logCompilationWarningsAndErrors(true));
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        @Language("java")
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
        @Language("java")
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

    @Test
    void removeFieldNameGetValue() {
        @Language("java")
        String before = "package com.yourorg;\n" +
                "import org.dmg.pmml.OutputField;\n" +
                "class FooBar {\n" +
                "static void method(OutputField toConvert) {\n" +
                "final String name = toConvert.getName() != null ? toConvert.getName().getValue() : null;\n" +
                "}\n" +
                "}";
        String after = "package com.yourorg;\n" +
                "import org.dmg.pmml.OutputField;\n" +
                "class FooBar {\n" +
                "static void method(OutputField toConvert) {\n" +
                "final String name = toConvert.getName() != null ?toConvert.getName() : null;\n" +
                "}\n" +
                "}";
        rewriteRun(
                Assertions.java(before, after)
        );
    }


    @Test
    void changeInstantiation_ScoreDistribution() {
        @Language("java")
        String before = "package com.yourorg;\n" +
                "import org.dmg.pmml.ScoreDistribution;\n" +
                "class FooBar {\n" +
                "static void method() {\n" +
                "ScoreDistribution toReturn = new ScoreDistribution();\n" +
                "}\n" +
                "}";
        @Language("java")
        String after = "package com.yourorg;\n" +
                "import org.dmg.pmml.ComplexScoreDistribution;\n" +
                "import org.dmg.pmml.ScoreDistribution;\n" +
                "\n" +
                "class FooBar {\n" +
                "static void method() {\n" +
                "ScoreDistribution toReturn = new ComplexScoreDistribution();\n" +
                "}\n" +
                "}";
        rewriteRun(
                Assertions.java(before, after)
        );
    }

}