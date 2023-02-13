package org.kie.openrewrite.recipe.jpmml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import static org.assertj.core.api.Assertions.assertThat;
import static org.kie.openrewrite.recipe.jpmml.CommonTestingUtilities.*;
import static org.kie.openrewrite.recipe.jpmml.JPMMLVisitor.TO_MIGRATE_MESSAGE;

class JPMMLVisitorTest {

    private JPMMLVisitor jpmmlVisitor;

    @BeforeEach
    public void init() {
        jpmmlVisitor = new JPMMLVisitor("org.dmg.pmml.ScoreDistribution", "org.dmg.pmml.ComplexScoreDistribution");
    }

    @Test
    public void visitNewClass_ScoreDistribution() {
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
        J.NewClass toTest = getNewClassFromClassSource(classTested, classInstantiated)
                .orElseThrow(() -> new RuntimeException("Failed to find J.NewClass org.dmg.pmml.ScoreDistribution"));
        assertThat(toTest)
                .isNotNull();
        ExecutionContext executionContext = getExecutionContext(null);
        J retrieved = jpmmlVisitor.visitNewClass(toTest, executionContext);
        String expected = "new ComplexScoreDistribution()";
        assertThat(retrieved)
                .isNotNull()
                .isInstanceOf(J.NewClass.class)
                .hasToString(expected);
    }

    @Test
    public void visitNewClass_DataDictionary() {
        String classTested = "package com.yourorg;\n" +
                "\n" +
                "import java.util.List;\n" +
                "import org.dmg.pmml.DataDictionary;\n" +
                "import org.dmg.pmml.DataField;\n" +
                "\n" +
                "public class Stub {\n" +
                "\n" +
                "    public String hello(List<DataField> dataFields) {\n" +
                "        DataDictionary dataDictionary = new DataDictionary(dataFields);\n" +
                "        return \"Hello from com.yourorg.FooLol!\";\n" +
                "    }\n" +
                "\n" +
                "}";
        String classInstantiated = "org.dmg.pmml.DataDictionary";
        J.NewClass toTest = getNewClassFromClassSource(classTested, classInstantiated)
                .orElseThrow(() -> new RuntimeException("Failed to find J.NewClass org.dmg.pmml.DataDictionary"));
        assertThat(toTest)
                .isNotNull();
        ExecutionContext executionContext = getExecutionContext(null);
        J retrieved = jpmmlVisitor.visitNewClass(toTest, executionContext);
        String expected = "new DataDictionary().addStrings(dataFields.toArray(new String[0]))";
        assertThat(retrieved)
                .isNotNull()
                .isInstanceOf(J.MethodInvocation.class)
                .hasToString(expected);
    }

    @Test
    public void visitNewClass_FieldNameCreate() {
        String classTested = "package com.yourorg;\n" +
                "\n" +
                "import org.dmg.pmml.FieldName;\n" +
                "import org.dmg.pmml.MiningField;\n" +
                "\n" +
                "public class Stub {\n" +
                "\n" +
                "    public String hello() {\n" +
                "        MiningField toReturn = new MiningField(FieldName.create(new String(\"TestingField\")));\n" +
                "        return \"Hello from com.yourorg.FooLol!\";\n" +
                "    }\n" +
                "\n" +
                "}";
        String classInstantiated = "org.dmg.pmml.MiningField";
        J.NewClass toTest = getNewClassFromClassSource(classTested, classInstantiated)
                .orElseThrow(() -> new RuntimeException("Failed to find J.NewClass org.dmg.pmml.MiningField"));
        assertThat(toTest)
                .isNotNull();
        ExecutionContext executionContext = getExecutionContext(null);
        J retrieved = jpmmlVisitor.visitNewClass(toTest, executionContext);
        String expected = "new MiningField( String.valueOf(new String(\"TestingField\")))";
        assertThat(retrieved)
                .isNotNull()
                .isInstanceOf(J.NewClass.class)
                .hasToString(expected);
    }

    @Test
    public void visitFieldNameInstantiation_FieldName() {
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
        J.VariableDeclarations toTest = getVariableDeclarationsFromClassSource(classTested, variableDeclaration)
                .orElseThrow(() -> new RuntimeException("Failed to find J.VariableDeclarations FieldName fieldName = FieldName.create(\"OUTPUT_\")"));
        assertThat(toTest)
                .isNotNull();
        ExecutionContext executionContext = getExecutionContext(null);
        J.VariableDeclarations retrieved = jpmmlVisitor.visitVariableDeclarations(toTest, executionContext);
        String expected = "String fieldName =  String.valueOf(\"OUTPUT_\")";
        assertThat(retrieved)
                .isNotNull()
                .hasToString(expected);
    }

    @Test
    public void visitMethodInvocation_AccessFieldName() {
        String classTested = "package com.yourorg;\n" +
                "\n" +
                "import org.dmg.pmml.DataType;\n" +
                "import org.dmg.pmml.FieldName;\n" +
                "import org.dmg.pmml.mining.MiningModel;\n" +
                "import org.dmg.pmml.OpType;\n" +
                "import org.dmg.pmml.OutputField;\n" +
                "import org.dmg.pmml.Target;\n" +
                "\n" +
                "class Stub {\n" +
                "    public String bye() {\n" +
                "         MiningField toReturn = new MiningField(FieldName.create(new String(\"TestingFIeld\")));\n" +
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
        J.MethodInvocation toTest = getMethodInvocationFromClassSource(classTested, methodTested)
                .orElseThrow(() -> new RuntimeException("Failed to find J.MethodInvocation toConvert.getName().getValue"));
        assertThat(toTest).isNotNull();
        ExecutionContext executionContext = getExecutionContext(null);
        J retrieved = jpmmlVisitor.visitMethodInvocation(toTest, executionContext);
        String expected = "toConvert.getName()";
        assertThat(retrieved)
                .isNotNull()
                .isInstanceOf(J.MethodInvocation.class)
                .hasToString(expected);

        methodTested = "target.getField().getValue";
        toTest = getMethodInvocationFromClassSource(classTested, methodTested)
                .orElseThrow(() -> new RuntimeException("Failed to find J.MethodInvocation target.getField().getValue"));
        assertThat(toTest).isNotNull();
        retrieved = jpmmlVisitor.visitMethodInvocation(toTest, executionContext);
        expected = "target.getField()";
        assertThat(retrieved)
                .isNotNull()
                .isInstanceOf(J.MethodInvocation.class)
                .hasToString(expected);

        methodTested = "target.getKey().getValue";
        toTest = getMethodInvocationFromClassSource(classTested, methodTested)
                .orElseThrow(() -> new RuntimeException("Failed to find J.MethodInvocation target.getKey().getValue"));
        assertThat(toTest).isNotNull();
        retrieved = jpmmlVisitor.visitMethodInvocation(toTest, executionContext);
        expected = "target.getKey()";
        assertThat(retrieved)
                .isNotNull()
                .isInstanceOf(J.MethodInvocation.class)
                .hasToString(expected);
    }

    @Test
    public void visitMethodInvocation_AccessFieldNameInsideConstructor() {
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
        J.NewClass toTest = getNewClassFromClassSource(classTested, classInstantiated)
                .orElseThrow(() -> new RuntimeException("Failed to find J.NewClass java.lang.String"));
        assertThat(toTest).isNotNull();
        ExecutionContext executionContext = getExecutionContext(null);
        J retrieved = jpmmlVisitor.visitNewClass(toTest, executionContext);
        String expected = "new String(target.getKey())";
        assertThat(retrieved)
                .isNotNull()
                .isInstanceOf(J.NewClass.class)
                .hasToString(expected);
    }

    @Test
    public void visitMethodInvocation_AccessFieldNameAsSecondParameter() {
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
                "    public public void bye() {\n" +
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
        J.VariableDeclarations toTest = getVariableDeclarationsFromClassSource(classTested, variableDeclaration)
                .orElseThrow(() -> new RuntimeException("Failed to find J.VariableDeclarations DataField targetDataField = "));
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
    public void visitMethodInvocation_NumericPredictorGetName() {
        String classTested = "package com.yourorg;\n" +
                "\n" +
                "import org.dmg.pmml.FieldName;\n" +
                "import org.dmg.pmml.regression.NumericPredictor;\n" +
                "\n" +
                "class Stub {\n" +
                "    public String bye(NumericPredictor numericPredictor) {\n" +
                "        FieldName fieldName = numericPredictor.getName();\n" +
                "        return fieldName.getValue();\n" +
                "    }" +
                "}";
        String methodTested = "numericPredictor.getName";
        J.MethodInvocation toTest = getMethodInvocationFromClassSource(classTested, methodTested)
                .orElseThrow(() -> new RuntimeException("Failed to find J.MethodInvocation numericPredictor.getName()"));
        assertThat(toTest).isNotNull();
        ExecutionContext executionContext = getExecutionContext(null);
        J retrieved = jpmmlVisitor.visitMethodInvocation(toTest, executionContext);
        String expected = "numericPredictor.getField()";
        assertThat(retrieved)
                .isNotNull()
                .isInstanceOf(J.MethodInvocation.class)
                .hasToString(expected);
    }

    @Test
    public void visitFieldNameRetrieval__NumericPredictorGetName() {
        String classTested = "package com.yourorg;\n" +
                "\n" +
                "import org.dmg.pmml.FieldName;\n" +
                "import org.dmg.pmml.regression.NumericPredictor;\n" +
                "\n" +
                "class Stub {\n" +
                "    public String hello(NumericPredictor numericPredictor) {\n" +
                "        FieldName fieldName = numericPredictor.getName();\n" +
                "        return \"Hello from com.yourorg.FooBar!\";\n" +
                "    }\n" +
                "}";
        String variableDeclaration = "FieldName fieldName = numericPredictor.getName()";
        J.VariableDeclarations toTest = getVariableDeclarationsFromClassSource(classTested, variableDeclaration)
                .orElseThrow(() -> new RuntimeException("Failed to find J.VariableDeclarations FieldName fieldName = FieldName.create(\"OUTPUT_\")"));
        assertThat(toTest)
                .isNotNull();
        ExecutionContext executionContext = getExecutionContext(null);
        J.VariableDeclarations retrieved = jpmmlVisitor.visitVariableDeclarations(toTest, executionContext);
        String expected = "String fieldName = numericPredictor.getField()";
        assertThat(retrieved)
                .isNotNull()
                .hasToString(expected);
    }

    @Test
    public void visitMethodInvocation_CategoricalPredictorGetName() {
        String classTested = "package com.yourorg;\n" +
                "\n" +
                "import org.dmg.pmml.FieldName;\n" +
                "import org.dmg.pmml.regression.CategoricalPredictor;\n" +
                "\n" +
                "class Stub {\n" +
                "    public String bye(CategoricalPredictor categoricalPredictor) {\n" +
                "        FieldName fieldName = categoricalPredictor.getName();\n" +
                "        return fieldName.getValue();\n" +
                "    }" +
                "}";
        String methodTested = "categoricalPredictor.getName";
        J.MethodInvocation toTest = getMethodInvocationFromClassSource(classTested, methodTested)
                .orElseThrow(() -> new RuntimeException("Failed to find J.MethodInvocation categoricalPredictor.getName()"));
        assertThat(toTest).isNotNull();
        ExecutionContext executionContext = getExecutionContext(null);
        J retrieved = jpmmlVisitor.visitMethodInvocation(toTest, executionContext);
        String expected = "categoricalPredictor.getField()";
        assertThat(retrieved)
                .isNotNull()
                .isInstanceOf(J.MethodInvocation.class)
                .hasToString(expected);
    }

    @Test
    public void visitFieldNameRetrieval_CategoricalPredictorGetName() {
        String classTested = "package com.yourorg;\n" +
                "\n" +
                "import org.dmg.pmml.FieldName;\n" +
                "import org.dmg.pmml.regression.CategoricalPredictor;\n" +
                "\n" +
                "class Stub {\n" +
                "    public String hello(CategoricalPredictor categoricalPredictor) {\n" +
                "        FieldName fieldName = categoricalPredictor.getName();\n" +
                "        return \"Hello from com.yourorg.FooBar!\";\n" +
                "    }\n" +
                "}";
        String variableDeclaration = "FieldName fieldName = categoricalPredictor.getName()";
        J.VariableDeclarations toTest = getVariableDeclarationsFromClassSource(classTested, variableDeclaration)
                .orElseThrow(() -> new RuntimeException("Failed to find J.VariableDeclarations FieldName fieldName = categoricalPredictor.getName()"));
        assertThat(toTest)
                .isNotNull();
        ExecutionContext executionContext = getExecutionContext(null);
        J.VariableDeclarations retrieved = jpmmlVisitor.visitVariableDeclarations(toTest, executionContext);
        String expected = "String fieldName = categoricalPredictor.getField()";
        assertThat(retrieved)
                .isNotNull()
                .hasToString(expected);
    }

    @Test
    public void visitCompilationUnit_NotToMigrate() {
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
    public void visitCompilationUnit_ToMigrate() {
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
    public void visitExpression_FieldNameGetValue() {
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
                "    public public void bye() {\n" +
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
        String expressionTested = "field.getName().getValue()";
        Expression toTest = getExpressionFromClassSource(classTested, expressionTested)
                .orElseThrow(() -> new RuntimeException("Failed to find Expression FieldName.create(\"OUTPUT_\")"));
        assertThat(toTest)
                .isNotNull();
        ExecutionContext executionContext = getExecutionContext(null);
        Expression retrieved = jpmmlVisitor.visitExpression(toTest, executionContext);
        String expected = "field.getName()";
        assertThat(retrieved)
                .isNotNull()
                .isInstanceOf(J.MethodInvocation.class)
                .hasToString(expected);
    }

    @Test
    public void visitExpression_FieldNameCreate() {
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
        String expressionTested = "FieldName.create(\"OUTPUT_\")";
        Expression toTest = getExpressionFromClassSource(classTested, expressionTested)
                .orElseThrow(() -> new RuntimeException("Failed to find Expression FieldName.create(\"OUTPUT_\")"));
        assertThat(toTest)
                .isNotNull();
        ExecutionContext executionContext = getExecutionContext(null);
        Expression retrieved = jpmmlVisitor.visitExpression(toTest, executionContext);
        String expected = "String.valueOf(\"OUTPUT_\")";
        assertThat(retrieved)
                .isNotNull()
                .isInstanceOf(J.MethodInvocation.class)
                .hasToString(expected);
    }
    
    @Test
    public void replaceInstantiation_ScoreDistribution() {
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
        J.NewClass toTest = getNewClassFromClassSource(classTested, classInstantiated)
                .orElseThrow(() -> new RuntimeException("Failed to find J.NewClass org.dmg.pmml.ScoreDistribution"));
        assertThat(toTest)
                .isNotNull();
        Expression retrieved = jpmmlVisitor.replaceInstantiation(toTest, getExecutionContext(null));
        String expected = "new ComplexScoreDistribution()";
        assertThat(retrieved)
                .isNotNull()
                .isInstanceOf(J.NewClass.class)
                .hasToString(expected);
    }

    @Test
    public void toMigrate_False() {
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
    public void toMigrate_True() {
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