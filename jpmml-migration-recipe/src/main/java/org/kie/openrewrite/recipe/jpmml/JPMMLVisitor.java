/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.openrewrite.recipe.jpmml;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class JPMMLVisitor extends JavaIsoVisitor<ExecutionContext> {

    private static final Logger logger = LoggerFactory.getLogger(JPMMLVisitor.class);
    static final String JPMML_MODEL_PACKAGE_BASE = "org.jpmml.model";
    static final String DMG_PMML_MODEL_PACKAGE_BASE = "org.dmg.pmml";
    static final String TO_MIGRATE_MESSAGE = "TO_MIGRATE";
    private final JavaType.Class originalInstantiatedType;
    private final JavaType targetInstantiatedType;

    private static final JavaType STRING_JAVA_TYPE = JavaType.buildType(String.class.getCanonicalName());

    private static final String FIELD_NAME_FQDN = "org.dmg.pmml.FieldName";
    private static final String MODEL_NAME_FQDN = "org.dmg.pmml.Model";

    private static final String NUMERIC_PREDICTOR_FQDN = "org.dmg.pmml.regression.NumericPredictor";

    private static final J.Identifier STRING_IDENTIFIER = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, "String", STRING_JAVA_TYPE, null);
    private static final J.Identifier STRING_VALUE_OF_IDENTIFIER = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, "valueOf", STRING_JAVA_TYPE, null);

    private static final J.Identifier NUMERIC_PREDICTOR_GET_NAME_IDENTIFIER = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, "getField", STRING_JAVA_TYPE, null);

    private final JavaTemplate requireMiningFunctionTemplate = JavaTemplate.builder(this::getCursor,
                    "@Override\n" +
                            "    public MiningFunction requireMiningFunction() {\n" +
                            "        return null;\n" +
                            "    }\n")
            .build();

    private final JavaTemplate requireMiningSchemaTemplate = JavaTemplate.builder(this::getCursor,
                    "@Override\n" +
                            "    public MiningSchema requireMiningSchema() {\n" +
                            "        return null;\n" +
                            "    }\n")
            .build();


    public JPMMLVisitor(String oldInstantiatedFullyQualifiedTypeName, String newInstantiatedFullyQualifiedTypeName) {
        this.originalInstantiatedType = JavaType.ShallowClass.build(oldInstantiatedFullyQualifiedTypeName);
        this.targetInstantiatedType = JavaType.buildType(newInstantiatedFullyQualifiedTypeName);
    }

    @Override
    public @Nullable J postVisit(J tree, ExecutionContext executionContext) {
        logger.debug("postVisit {}", tree);
        if (tree instanceof J.CompilationUnit) {
            maybeAddImport(targetInstantiatedType.toString());
            if (Boolean.TRUE.equals(executionContext.getMessage(TO_MIGRATE_MESSAGE))) {
                tree = new ChangeType(FIELD_NAME_FQDN, STRING_JAVA_TYPE.toString(), false)
                        .getVisitor()
                        .visit(tree, executionContext);
            }
        }
        return super.postVisit(tree, executionContext);
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
        if (classDecl.getType() != null &&
                classDecl.getType().getSupertype() != null &&
                MODEL_NAME_FQDN.equals(classDecl.getType().getSupertype().getFullyQualifiedName())) {
            classDecl = addMissingMethod(classDecl, "requireMiningFunction", requireMiningFunctionTemplate);
            classDecl = addMissingMethod(classDecl, "requireMiningSchema", requireMiningSchemaTemplate);
        }
        return super.visitClassDeclaration(classDecl, executionContext);
    }

    @Override
    public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext executionContext) {
        logger.debug("visitNewClass {}", newClass);
        newClass = replaceInstantiation(newClass, executionContext);
        return super.visitNewClass(newClass, executionContext);
    }

    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable,
                                                            ExecutionContext executionContext) {
        logger.debug("visitVariableDeclarations {}", multiVariable);
        multiVariable = super.visitVariableDeclarations(multiVariable, executionContext);
        if (multiVariable.getTypeAsFullyQualified() != null &&
                multiVariable.getTypeAsFullyQualified().getFullyQualifiedName() != null &&
                multiVariable.getTypeAsFullyQualified().getFullyQualifiedName().equals(FIELD_NAME_FQDN)) {
            multiVariable = multiVariable.withType(STRING_JAVA_TYPE).withTypeExpression(STRING_IDENTIFIER);
        }
        return multiVariable;
    }

    @Override
    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext executionContext) {
        logger.debug("visitVariable {}", variable);
        if (variable.getType().toString().equals(FIELD_NAME_FQDN)) {
            variable = variable
                    .withType(STRING_JAVA_TYPE)
                    .withVariableType(variable.getVariableType().withType(STRING_JAVA_TYPE));
        }
        return super.visitVariable(variable, executionContext);
    }

    @Override
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
        logger.debug("visitCompilationUnit {}", cu);
        boolean toMigrate = toMigrate(cu.getImports());
        executionContext.putMessage(TO_MIGRATE_MESSAGE, toMigrate);
        return super.visitCompilationUnit(cu, executionContext);
    }

    @Override
    public Expression visitExpression(Expression expression, ExecutionContext executionContext) {
        logger.debug("visitExpression {}", expression);
        Optional<J.MethodInvocation> fieldNameCreate = getFieldNameCreate(expression);
        if (fieldNameCreate.isPresent()) {
            executionContext.putMessage(TO_MIGRATE_MESSAGE, true);
            J.MethodInvocation foundInvocation = fieldNameCreate.get();
            expression = foundInvocation
                    .withSelect(STRING_IDENTIFIER)
                    .withDeclaringType((JavaType.FullyQualified) STRING_JAVA_TYPE)
                    .withMethodType(updateTypeToString(foundInvocation.getMethodType()))
                    .withArguments(fieldNameCreate.get().getArguments())
                    .withName(STRING_VALUE_OF_IDENTIFIER);
            return expression;
        }
        Optional<J.MethodInvocation> fieldNameGetValue = getFieldNameGetValue(expression);
        if (fieldNameGetValue.isPresent()) {
            executionContext.putMessage(TO_MIGRATE_MESSAGE, true);
            return fieldNameGetValue.get().getSelect();
        }
        Optional<J.MethodInvocation> numericPredictorGetName = getNumericPredictorGetName(expression);
        if (numericPredictorGetName.isPresent()) {
            executionContext.putMessage(TO_MIGRATE_MESSAGE, true);
            JavaType.Method methodType = numericPredictorGetName.get()
                    .getMethodType()
                    .withReturnType(STRING_JAVA_TYPE);
            return numericPredictorGetName.get()
                    .withName(NUMERIC_PREDICTOR_GET_NAME_IDENTIFIER)
                    .withMethodType(methodType);
        }
        return super.visitExpression(expression, executionContext);
    }

    protected J.ClassDeclaration addMissingMethod(J.ClassDeclaration classDecl, String searchedMethod, JavaTemplate javaTemplate) {
        if (methodExists(classDecl, searchedMethod)) {
            return classDecl;
        }
        classDecl = classDecl.withBody(
                classDecl.getBody().withTemplate(
                        javaTemplate,
                        classDecl.getBody().getCoordinates().lastStatement()
                ));
        return classDecl;
    }

    protected boolean methodExists(J.ClassDeclaration classDecl, String searchedMethod) {
        return classDecl.getBody().getStatements().stream()
                .filter(statement -> statement instanceof J.MethodDeclaration)
                .map(J.MethodDeclaration.class::cast)
                .anyMatch(methodDeclaration -> methodDeclaration.getName().getSimpleName().equals(searchedMethod));
    }

    /**
     * Returns a new <code>J.NewClass</code> with the <code>originalInstantiatedType</code>
     * replaced by <code>targetInstantiatedType</code>, if present.
     * Otherwise, returns the original newClass.
     *
     * @param newClass
     * @param executionContext
     * @return
     */
    protected J.NewClass replaceInstantiation(J.NewClass newClass, ExecutionContext executionContext) {
        logger.debug("replaceInstantiation {}", newClass);
        if (newClass.getType().toString().equals(originalInstantiatedType.toString())) {
            JavaType.Method updatedMethod = updateType(newClass.getConstructorType());
            TypeTree typeTree = updateTypeTree(newClass);
            newClass = newClass.withConstructorType(updatedMethod)
                    .withClazz(typeTree);
            executionContext.putMessage(TO_MIGRATE_MESSAGE, true);
        }
        return newClass;
    }

    /**
     * Return an <code>Optional&lt;J.MethodInvocation&gt;</code> with <b>FieldName.create(...)</b>,
     * if present in the given <code>Expression</code>
     *
     * @param toCheck
     * @return
     */
    protected Optional<J.MethodInvocation> getFieldNameCreate(Expression toCheck) {
        return ((toCheck instanceof J.MethodInvocation) && (isFieldNameCreate((J.MethodInvocation) toCheck)))
                ? Optional.of((J.MethodInvocation) toCheck) : Optional.empty();
    }

    /**
     * Return an <code>Optional&lt;J.MethodInvocation&gt;</code> with <b>(_field_).getValue()</b>,
     * if present in the given <code>Expression</code>
     *
     * @param toCheck
     * @return
     */
    protected Optional<J.MethodInvocation> getFieldNameGetValue(Expression toCheck) {
        return ((toCheck instanceof J.MethodInvocation) && (useFieldNameGetValue((J.MethodInvocation) toCheck)))
                ? Optional.of((J.MethodInvocation) toCheck) : Optional.empty();
    }

    /**
     * Return <code>true</code> if the given <code>J.MethodInvocation</code> is <b>FieldName.create(...)</b>,
     * <code>false</code> otherwise
     *
     * @param toCheck
     * @return
     */
    protected boolean isFieldNameCreate(J.MethodInvocation toCheck) {
        return toCheck.getType() != null && toCheck.getType().toString().equals(FIELD_NAME_FQDN) && toCheck.getName().toString().equals("create");
    }


    /**
     * Return an <code>Optional&lt;J.MethodInvocation&gt;</code> with <b>NumericPredictor.getName(...)</b>,
     * if present in the given <code>Expression</code>
     *
     * @param toCheck
     * @return
     */
    protected Optional<J.MethodInvocation> getNumericPredictorGetName(Expression toCheck) {
        return ((toCheck instanceof J.MethodInvocation) && (isNumericPredictorGetName((J.MethodInvocation) toCheck)))
                ? Optional.of((J.MethodInvocation) toCheck) : Optional.empty();
    }

    /**
     * Return <code>true</code> if the given <code>J.MethodInvocation</code> is <b>NumericPredictor.getName(...)</b>,
     * <code>false</code> otherwise
     *
     * @param toCheck
     * @return
     */
    protected boolean isNumericPredictorGetName(J.MethodInvocation toCheck) {
        return toCheck.getMethodType() != null &&
                toCheck.getMethodType().getDeclaringType() != null &&
                toCheck.getMethodType().getDeclaringType().toString().equals(NUMERIC_PREDICTOR_FQDN) && toCheck.getName().toString().equals("getName");
    }

    /**
     * Return <code>true</code> if the given <code>J.MethodInvocation</code> invokes <b>(_field_).getValue()</b>,
     * <code>false</code> otherwise
     *
     * @param toCheck
     * @return
     */
    protected boolean useFieldNameGetValue(J.MethodInvocation toCheck) {
        return toCheck.getMethodType() != null &&
                toCheck.getMethodType().getDeclaringType() != null &&
                toCheck.getMethodType().getDeclaringType().getFullyQualifiedName() != null &&
                toCheck.getMethodType().getDeclaringType().getFullyQualifiedName().equals(FIELD_NAME_FQDN) && toCheck.getMethodType().getName().equals("getValue");
    }

    boolean toMigrate(List<J.Import> imports) {
        return imports.stream()
                .anyMatch(anImport -> anImport.getPackageName().startsWith(JPMML_MODEL_PACKAGE_BASE) ||
                        anImport.getPackageName().startsWith(DMG_PMML_MODEL_PACKAGE_BASE));
    }

    JavaType.Method updateType(JavaType.Method oldMethodType) {
        if (oldMethodType != null) {
            JavaType.Method method = oldMethodType;
            method = method.withDeclaringType((JavaType.FullyQualified) targetInstantiatedType)
                    .withReturnType(targetInstantiatedType);
            return method;
        }
        return null;
    }

    JavaType.Method updateTypeToString(JavaType.Method oldMethodType) {
        if (oldMethodType != null) {
            JavaType.Method method = oldMethodType;
            method = method.withDeclaringType((JavaType.FullyQualified) STRING_JAVA_TYPE)
                    .withReturnType(STRING_JAVA_TYPE);
            return method;
        }
        return null;
    }

    TypeTree updateTypeTree(J.NewClass newClass) {
        return ((J.Identifier) newClass.getClazz())
                .withSimpleName(((JavaType.ShallowClass) targetInstantiatedType).getClassName())
                .withType(targetInstantiatedType);
    }

}
