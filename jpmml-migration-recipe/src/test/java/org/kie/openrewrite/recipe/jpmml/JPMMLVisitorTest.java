package org.kie.openrewrite.recipe.jpmml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;

import static org.assertj.core.api.Assertions.assertThat;
import static org.kie.openrewrite.recipe.jpmml.CommonTestingUtilities.getCompilationUnitFromClassSource;
import static org.kie.openrewrite.recipe.jpmml.CommonTestingUtilities.getExecutionContext;
import static org.kie.openrewrite.recipe.jpmml.CommonTestingUtilities.getMethodInvocationFromClassSource;
import static org.kie.openrewrite.recipe.jpmml.CommonTestingUtilities.getNewClassFromClassSource;
import static org.kie.openrewrite.recipe.jpmml.JPMMLVisitor.TO_MIGRATE_MESSAGE;

class JPMMLVisitorTest {

    private JPMMLVisitor jpmmlVisitor;
    //private static Properties CHANGED_INSTANTIATIONS;

    /*@BeforeAll
    public static void setup() throws IOException {
        try (InputStream input = JPMMLRecipe.class.getResourceAsStream ("/changed_instantiation.properties")) {
            CHANGED_INSTANTIATIONS = new Properties();
            // load a properties file
            CHANGED_INSTANTIATIONS.load(input);
        }
    }*/

    @BeforeEach
    public void init() {
        jpmmlVisitor = new JPMMLVisitor("org.dmg.pmml.ScoreDistribution", "org.dmg.pmml.ComplexScoreDistribution");
    }

    @Test
    void visitNewClass_ScoreDistribution() {
        String classTested = "package com.yourorg;\n" +
                "\n" +
                "import org.dmg.pmml.ScoreDistribution;\n" +
                "\n" +
                "public class Stub {\n" +
                "\n" +
                "    public String hello() {\n" +
                "        ScoreDistribution scoreDistribution = new ScoreDistribution();\n" +
                "        return \"Hello from com.yourorg.FooLol!\";\n" +
                "    }\n" +
                "\n" +
                "}";
        String classInstantiated = "org.dmg.pmml.ScoreDistribution";
        J.NewClass toTest = getNewClassFromClassSource(classTested, classInstantiated).iterator().next();
        assertThat(toTest)
                .isNotNull();
        ExecutionContext executionContext = getExecutionContext(null);
        J.NewClass retrieved = jpmmlVisitor.visitNewClass(toTest, executionContext);
        String expected = "new ComplexScoreDistribution()";
        assertThat(retrieved)
                .isNotNull()
                .hasToString(expected);
    }

    @Test
    void visitMethodInvocation_FieldName() {
        String classTested = "package com.yourorg;\n" +
                "\n" +
                "import org.dmg.pmml.FieldName;\n" +
                "\n" +
                "class Stub {\n" +
                "    public String hello() {\n" +
                "        System.out.println(FieldName.create(\"OUTPUT_\"));\n" +
                "        return \"Hello from com.yourorg.FooBar!\";\n" +
                "    }\n" +
                "}";
        String methodTested = "System.out.println";
        J.MethodInvocation toTest = getMethodInvocationFromClassSource(classTested, methodTested).iterator().next();
        assertThat(toTest)
                .isNotNull();
        ExecutionContext executionContext = getExecutionContext(null);
        J.MethodInvocation retrieved = jpmmlVisitor.visitMethodInvocation(toTest, executionContext);
        String expected = "System.out.println(\"OUTPUT_\")";
        assertThat(retrieved)
                .isNotNull()
                .hasToString(expected);
    }

    @Test
    void visitCompilationUnit_NotToMigrate() {
        String classTested = "package com.yourorg;\n" +
                "import java.util.List;\n" +
                "import java.util.Map;\n" +
                "class FooBar {\n" +
                "};";
        J.CompilationUnit cu = getCompilationUnitFromClassSource(classTested);
        ExecutionContext executionContext = getExecutionContext(null);
        jpmmlVisitor.visitCompilationUnit(cu, executionContext);
        Object retrieved = executionContext.getMessage(TO_MIGRATE_MESSAGE);
        assertThat(retrieved)
                .isNotNull()
                .isInstanceOf(Boolean.class)
                .isEqualTo(false);
    }

    @Test
    void visitCompilationUnit_ToMigrate() {
        String classTested = "package com.yourorg;\n" +
                "import java.util.List;\n" +
                "import org.dmg.pmml.FieldName;\n" +
                "class FooBar {\n" +
                "};";
        J.CompilationUnit cu = getCompilationUnitFromClassSource(classTested);
        ExecutionContext executionContext = getExecutionContext(null);
        jpmmlVisitor.visitCompilationUnit(cu, executionContext);
        Object retrieved = executionContext.getMessage(TO_MIGRATE_MESSAGE);
        assertThat(retrieved)
                .isNotNull()
                .isInstanceOf(Boolean.class)
                .isEqualTo(true);
    }

    @Test
    void toMigrate_False() {
        String classTested = "package com.yourorg;\n" +
                "import java.util.List;\n" +
                "import java.util.Map;\n" +
                "class FooBar {\n" +
                "};";
        J.CompilationUnit cu = getCompilationUnitFromClassSource(classTested);
        assertThat(jpmmlVisitor.toMigrate(cu.getImports()))
                .isFalse();
    }

    @Test
    void toMigrate_True() {
        String classTested = "package com.yourorg;\n" +
                "import java.util.List;\n" +
                "import org.dmg.pmml.FieldName;\n" +
                "class FooBar {\n" +
                "};";
        J.CompilationUnit cu = getCompilationUnitFromClassSource(classTested);
        assertThat(jpmmlVisitor.toMigrate(cu.getImports()))
                .isTrue();
        classTested = "package com.yourorg;\n" +
                "import java.util.List;\n" +
                "import org.jpmml.model.cells.InputCell;\n" +
                "class FooBar {\n" +
                "};";
        cu = getCompilationUnitFromClassSource(classTested);
        assertThat(jpmmlVisitor.toMigrate(cu.getImports()))
                .isTrue();

        classTested = "package com.yourorg;\n" +
                "import java.util.List;\n" +
                "import org.jpmml.model.cells.InputCell;\n" +
                "import org.jpmml.model.cells.InputCell;\n" +
                "class FooBar {\n" +
                "};";
        cu = getCompilationUnitFromClassSource(classTested);
        assertThat(jpmmlVisitor.toMigrate(cu.getImports()))
                .isTrue();
    }
}