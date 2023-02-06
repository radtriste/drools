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
import static org.kie.openrewrite.recipe.jpmml.CommonTestingUtilities.getVariableDeclarationsFromClassSource;
import static org.kie.openrewrite.recipe.jpmml.JPMMLVisitor.TO_MIGRATE_MESSAGE;

class JPMMLVisitorTest {

    private JPMMLVisitor jpmmlVisitor;

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
    void visitFieldNameInstantiation_FieldName() {
        String classTested = "package com.yourorg;\n" +
                "\n" +
                "import org.dmg.pmml.FieldName;\n" +
                "\n" +
                "class Stub {\n" +
                "    public String hello() {\n" +
                "        FieldName fieldName = FieldName.create(\"OUTPUT_\");\n" +
                "        return \"Hello from com.yourorg.FooBar!\";\n" +
                "    }\n" +
                "}";
        String variableDeclaration = "FieldName fieldName = FieldName.create(\"OUTPUT_\")";
        J.VariableDeclarations toTest = getVariableDeclarationsFromClassSource(classTested, variableDeclaration).iterator().next();
        assertThat(toTest)
                .isNotNull();
        ExecutionContext executionContext = getExecutionContext(null);
        J.VariableDeclarations retrieved = jpmmlVisitor.visitVariableDeclarations(toTest, executionContext);
        String expected = "String fieldName =\"OUTPUT_\"";
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
    void visitMethodInvocation_AccessFieldName() {
        String classTested = "package com.yourorg;\n" +
                "\n" +
                "import org.dmg.pmml.DataType;\n" +
                "import org.dmg.pmml.FieldName;\n" +
                "import org.dmg.pmml.OpType;\n" +
                "import org.dmg.pmml.OutputField;\n" +
                "import org.dmg.pmml.Target;\n" +
                "\n" +
                "class Stub {\n" +
                "    public String bye() {\n" +
                "        OutputField toConvert = new OutputField(FieldName.create(\"FIELDNAME\"), OpType.CATEGORICAL," +
                " DataType.BOOLEAN);\n" +
                "        final String name = toConvert.getName() != null ? toConvert.getName().getValue() : null;\n" +
                "        Target target = new Target();\n" +
                "        String field = target.getField().getValue();\n" +
                "        String key = target.getKey().getValue();\n" +
                "        return name;\n" +
                "    }" +
                "}";
        String methodTested = "toConvert.getName().getValue";
        J.MethodInvocation toTest = getMethodInvocationFromClassSource(classTested, methodTested).iterator().next();
        assertThat(toTest).isNotNull();
        ExecutionContext executionContext = getExecutionContext(null);
        J.MethodInvocation retrieved = jpmmlVisitor.visitMethodInvocation(toTest, executionContext);
        String expected = "toConvert.getName()";
        assertThat(retrieved).isNotNull()
                .hasToString(expected);

        methodTested = "target.getField().getValue";
        toTest = getMethodInvocationFromClassSource(classTested, methodTested).iterator().next();
        assertThat(toTest).isNotNull();
        retrieved = jpmmlVisitor.visitMethodInvocation(toTest, executionContext);
        expected = "target.getField()";
        assertThat(retrieved).isNotNull()
                .hasToString(expected);

        methodTested = "target.getKey().getValue";
        toTest = getMethodInvocationFromClassSource(classTested, methodTested).iterator().next();
        assertThat(toTest).isNotNull();
        retrieved = jpmmlVisitor.visitMethodInvocation(toTest, executionContext);
        expected = "target.getKey()";
        assertThat(retrieved).isNotNull()
                .hasToString(expected);
    }

    @Test
    void visitMethodInvocation_AccessFieldNameInsideConstructor() {
        String classTested = "package com.yourorg;\n" +
                "\n" +
                "import org.dmg.pmml.Target;\n" +
                "\n" +
                "class Stub {\n" +
                "    public String bye() {\n" +
                "        Target target = new Target();\n" +
                "        String name = new String(target.getKey().getValue());\n" +
                "        return name;\n" +
                "    }" +
                "}";
        String classInstantiated = "java.lang.String";
        J.NewClass toTest = getNewClassFromClassSource(classTested, classInstantiated).iterator().next();
        assertThat(toTest).isNotNull();
        ExecutionContext executionContext = getExecutionContext(null);
        J.NewClass retrieved = jpmmlVisitor.visitNewClass(toTest, executionContext);
        String expected = "new String(target.getKey())";
        assertThat(retrieved).isNotNull()
                .hasToString(expected);
    }

    @Test
    void visitMethodInvocation_AccessFieldNameAsSecondParameter() {
        String classTested = "package com.yourorg;\n" +
                "\n" +
                "import java.util.List;\n" +
                "import java.util.Objects;\n" +
                "\n" +
                "import org.dmg.pmml.DataField;\n" +
                "import org.dmg.pmml.Field;\n" +
                "\n" +
                "public class Stub {\n" +
                "\n" +
                "    private List<Field<?>> fields;\n" +
                "\n" +
                "    public void bye() {\n" +
                "        DataField targetDataField = this.fields.stream()\n" +
                "                .filter(DataField.class::isInstance)\n" +
                "                .map(DataField.class::cast)\n" +
                "                .filter(field -> Objects.equals(getTargetFieldName(), field.getName().getValue()))\n" +
                "                .findFirst().orElse(null);\n" +
                "    }\n" +
                "    public String getTargetFieldName() {\n" +
                "        return \"targetDataFieldName\";\n" +
                "    }\n" +
                "}";
        String variableDeclaration = "DataField targetDataField = ";
        J.VariableDeclarations toTest = getVariableDeclarationsFromClassSource(classTested, variableDeclaration).iterator().next();
        assertThat(toTest).isNotNull();
        ExecutionContext executionContext = getExecutionContext(null);
        J.VariableDeclarations retrieved = jpmmlVisitor.visitVariableDeclarations(toTest, executionContext);
        String expected = "DataField targetDataField = this.fields.stream()\n" +
                "                .filter(DataField.class::isInstance)\n" +
                "                .map(DataField.class::cast)\n" +
                "                .filter(field -> Objects.equals(getTargetFieldName(),field.getName()))\n" +
                "                .findFirst().orElse(null)";
        assertThat(retrieved).isNotNull()
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
    void removeUnusedImports() {
        String classTested = "package com.yourorg;\n" +
                "import java.util.List;\n" +
                "import org.dmg.pmml.FieldName;\n" +
                "import static org.assertj.core.api.Assertions.assertThat;\n" +
                "import static org.dmg.pmml.regression.RegressionModel.NormalizationMethod.CAUCHIT;\n" +
                "class FooBar {\n" +
                "    public String hello() {\n" +
                "        assertThat(\"\").isNotNull();\n" +
                "        assertThat(CAUCHIT).isNotNull();\n" +
                "    }\n" +
                "};";
        J.CompilationUnit cu = getCompilationUnitFromClassSource(classTested);
        assertThat(cu.getImports()).hasSize(4);
        ExecutionContext executionContext = getExecutionContext(null);
        cu = jpmmlVisitor.removeUnusedImports(cu, executionContext);
        assertThat(cu.getImports()).hasSize(2);
    }

    @Test
    void replaceInstantiation_ScoreDistribution() {
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
        J.NewClass retrieved = jpmmlVisitor.replaceInstantiation(toTest, getExecutionContext(null));
        String expected = "new ComplexScoreDistribution()";
        assertThat(retrieved)
                .isNotNull()
                .hasToString(expected);
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
        assertThat(cu.getImports()).hasSize(2);
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