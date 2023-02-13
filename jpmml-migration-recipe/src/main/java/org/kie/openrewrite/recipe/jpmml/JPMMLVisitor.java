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
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.TreeVisitingPrinter;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class JPMMLVisitor extends JavaVisitor<ExecutionContext> {

    private static final Logger logger = LoggerFactory.getLogger(JPMMLVisitor.class);
    static final String JPMML_MODEL_PACKAGE_BASE = "org.jpmml.model";
    static final String DMG_PMML_MODEL_PACKAGE_BASE = "org.dmg.pmml";
    static final String TO_MIGRATE_MESSAGE = "TO_MIGRATE";
    private final JavaType.Class originalInstantiatedType;
    private final JavaType targetInstantiatedType;

    private static final String FIELD_NAME_FQDN = "org.dmg.pmml.FieldName";
    private static final String MODEL_NAME_FQDN = "org.dmg.pmml.Model";

    private static final String NUMERIC_PREDICTOR_FQDN = "org.dmg.pmml.regression.NumericPredictor";

    private static final String CATEGORICAL_PREDICTOR_FQDN = "org.dmg.pmml.regression.CategoricalPredictor";

    private static final List<String> GET_NAME_TO_GET_FIELD_CLASSES = Arrays.asList(NUMERIC_PREDICTOR_FQDN,
            CATEGORICAL_PREDICTOR_FQDN);

    private static final String DATADICTIONARY_FQDN = "org.dmg.pmml.DataDictionary";

    private static final Map<String, String> REMOVED_LIST_FROM_INSTANTIATION = Map.of(DATADICTIONARY_FQDN,
            "addDataFields");


    private static final J.Identifier STRING_IDENTIFIER = new J.Identifier(Tree.randomId(), Space.build(" ", Collections.emptyList()), Markers.EMPTY, "String", JavaType.buildType(String.class.getCanonicalName()), null);

    private static final J.Identifier STRING_VALUE_OF_IDENTIFIER = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, "valueOf", JavaType.Primitive.String, null);

    private static final J.Identifier NUMERIC_PREDICTOR_GET_NAME_IDENTIFIER = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, "getField", JavaType.Primitive.String, null);


    //**********
    private static final JavaType LIST_JAVA_TYPE = JavaType.buildType(List.class.getCanonicalName());

    private static final JavaType.Parameterized LIST_STRING_JAVA_TYPE = new JavaType.Parameterized(null, (JavaType.FullyQualified) LIST_JAVA_TYPE, List.of(JavaType.Primitive.String));

    private static final JavaType.Parameterized LIST_GENERIC_JAVA_TYPE = new JavaType.Parameterized(null, (JavaType.FullyQualified) LIST_JAVA_TYPE, List.of(JavaType.GenericTypeVariable.Primitive.String));

    private static final JavaType.Array ARRAY_STRING_JAVA_TYPE = new JavaType.Array(null, JavaType.buildType(String.class.getCanonicalName()));

    private static final J.Identifier ADD_STRINGS_IDENTIFIER = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, "addStrings", LIST_STRING_JAVA_TYPE, null);

    private static final J.Identifier TO_ARRAY_STRING_IDENTIFIER = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, "toArray", LIST_STRING_JAVA_TYPE, null);

    //*******

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
        logger.trace("postVisit {}", tree);
        if (tree instanceof J.CompilationUnit) {
            maybeAddImport(targetInstantiatedType.toString());
/*            if (Boolean.TRUE.equals(executionContext.getMessage(TO_MIGRATE_MESSAGE))) {
                tree = new ChangeType(FIELD_NAME_FQDN, STRING_JAVA_TYPE.toString(), false)
                        .getVisitor()
                        .visit(tree, executionContext);
            }*/
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
        return (J.ClassDeclaration) super.visitClassDeclaration(classDecl, executionContext);
    }

    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable,
                                                            ExecutionContext executionContext) {
        logger.trace("visitVariableDeclarations {}", multiVariable);
        logger.trace(TreeVisitingPrinter.printTree(multiVariable));
        multiVariable = (J.VariableDeclarations) super.visitVariableDeclarations(multiVariable, executionContext);
        if (multiVariable.getTypeAsFullyQualified() != null &&
                multiVariable.getTypeAsFullyQualified().getFullyQualifiedName() != null &&
                multiVariable.getTypeAsFullyQualified().getFullyQualifiedName().equals(FIELD_NAME_FQDN)) {
            multiVariable = multiVariable.withType(JavaType.Primitive.String).withTypeExpression(STRING_IDENTIFIER);
        }
        return multiVariable;
    }

    @Override
    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext executionContext) {
        logger.trace("visitVariable {}", variable);
        if (variable.getType() != null && variable.getType().toString().equals(FIELD_NAME_FQDN)) {
            variable = variable
                    .withType(JavaType.Primitive.String)
                    .withVariableType(variable.getVariableType().withType(JavaType.Primitive.String));
        }
        return (J.VariableDeclarations.NamedVariable) super.visitVariable(variable, executionContext);
    }

    @Override
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
        logger.trace("visitCompilationUnit {}", cu);
        boolean toMigrate = toMigrate(cu.getImports());
        executionContext.putMessage(TO_MIGRATE_MESSAGE, toMigrate);
        try {
            return (J.CompilationUnit) super.visitCompilationUnit(cu, executionContext);
        } catch (Throwable t) {
            logger.error("Failed to visit {}", cu, t);
            logger.error(TreeVisitingPrinter.printTree(cu));
            return cu;
        }
    }

    @Override
    public Expression visitExpression(Expression expression, ExecutionContext executionContext) {
        logger.trace("visitExpression {}", expression);
        logger.trace(TreeVisitingPrinter.printTree(expression));
        Optional<J.MethodInvocation> fieldNameCreate = getFieldNameCreate(expression);
        if (fieldNameCreate.isPresent()) {
            executionContext.putMessage(TO_MIGRATE_MESSAGE, true);
            J.MethodInvocation foundInvocation = fieldNameCreate.get();
            expression = foundInvocation
                    .withSelect(STRING_IDENTIFIER)
                    .withDeclaringType((JavaType.FullyQualified) JavaType.buildType(String.class.getCanonicalName()))
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
        Optional<J.MethodInvocation> fieldNameGetNameToGetField = getFieldNameGetNameToGetField(expression);
        if (fieldNameGetNameToGetField.isPresent()) {
            executionContext.putMessage(TO_MIGRATE_MESSAGE, true);
            JavaType.Method methodType = fieldNameGetNameToGetField.get()
                    .getMethodType()
                    .withReturnType(JavaType.Primitive.String);
            return fieldNameGetNameToGetField.get()
                    .withName(NUMERIC_PREDICTOR_GET_NAME_IDENTIFIER)
                    .withMethodType(methodType);
        }
        if (expression instanceof J.NewClass) {
            expression = replaceInstantiation((J.NewClass) expression, executionContext);
        }
        return (Expression) super.visitExpression(expression, executionContext);
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
     *
     * @param newClass
     * @param executionContext
     * @return
     */
    protected Expression replaceInstantiation(J.NewClass newClass, ExecutionContext executionContext) {
        logger.trace("replaceInstantiation {}", newClass);
        newClass = replaceOriginalToTargetInstantiation(newClass, executionContext);
        return replaceInstantiationListRemoved(newClass);
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
    protected J.NewClass replaceOriginalToTargetInstantiation(J.NewClass newClass, ExecutionContext executionContext) {
        logger.trace("replaceOriginalToTargetInstantiation {}", newClass);
        if (newClass.getType() != null && newClass.getType().toString().equals(originalInstantiatedType.toString())) {
            JavaType.Method updatedMethod = updateType(newClass.getConstructorType());
            TypeTree typeTree = updateTypeTree(newClass);
            newClass = newClass.withConstructorType(updatedMethod)
                    .withClazz(typeTree);
            executionContext.putMessage(TO_MIGRATE_MESSAGE, true);
        }
        return newClass;
    }

    /**
     * Returns a new <code>J.NewClass</code> with the <code>originalInstantiatedType</code>
     * replaced by <code>targetInstantiatedType</code>, if present.
     * Otherwise, returns the original newClass.
     *
     * @param newClass
     * @return
     */
    protected Expression replaceInstantiationListRemoved(J.NewClass newClass) {
        logger.trace("replaceInstantiationListRemoved {}", newClass);
        if (isInstantiationListRemoved(newClass)) {
            J.Identifier stringsIdentifier = (J.Identifier) newClass.getArguments().get(0);
            J.Literal literal = new J.Literal(Tree.randomId(), Space.EMPTY, Markers.EMPTY, 0, "0", null, JavaType.Primitive.Int);
            J.ArrayDimension arrayDimension = new J.ArrayDimension(Tree.randomId(), Space.EMPTY, Markers.EMPTY, JRightPadded.build(literal));
            J.NewArray newArray = new J.NewArray(Tree.randomId(), Space.EMPTY, Markers.EMPTY, STRING_IDENTIFIER, Collections.singletonList(arrayDimension), null, ARRAY_STRING_JAVA_TYPE);
            JavaType.Method methodType = new JavaType.Method(null, 1025, LIST_GENERIC_JAVA_TYPE, "toArray",
                    ARRAY_STRING_JAVA_TYPE,
                    Collections.singletonList("arg0"),
                    Collections.singletonList(ARRAY_STRING_JAVA_TYPE), null, null);
            J.MethodInvocation toArrayInvocation = new J.MethodInvocation(Tree.randomId(), Space.EMPTY, Markers.EMPTY, null, null,
                    TO_ARRAY_STRING_IDENTIFIER,
                    JContainer.build(Collections.emptyList()),
                    methodType)
                    .withSelect(stringsIdentifier)
                    .withArguments(Collections.singletonList(newArray));
            JavaType.Method constructorType = newClass.getConstructorType()
                    .withParameterTypes(Collections.emptyList())
                    .withParameterNames(Collections.emptyList());
            J.NewClass noArgClass = newClass.withArguments(Collections.emptyList())
                    .withConstructorType(constructorType);
            JavaType.Method addStringInvocationMethodType =  new JavaType.Method(null, 1025,
                    (JavaType.FullyQualified) JavaType.buildType(noArgClass.getType().toString()),
                    "addStrings",
                    JavaType.Primitive.Void,
                    Collections.singletonList("toAdd"),
                    Collections.singletonList(ARRAY_STRING_JAVA_TYPE), null, null);

            J.MethodInvocation toReturn = new J.MethodInvocation(Tree.randomId(), Space.EMPTY, Markers.EMPTY, null, null,
                    ADD_STRINGS_IDENTIFIER,
                    JContainer.build(Collections.emptyList()),
                    addStringInvocationMethodType)
                    .withSelect(noArgClass)
                    .withArguments(Collections.singletonList(toArrayInvocation));
            logger.trace(TreeVisitingPrinter.printTree(toReturn));
            return  toReturn;
        } else {
            return newClass;
        }
    }

    /**
     * Return <code>true</code> if the given <code>J.NewClass</code> constructor has not the <b>List</b> anymore
     * <code>false</code> otherwise
     *
     * @param toCheck
     * @return
     */
    protected boolean isInstantiationListRemoved(J.NewClass toCheck) {
        return toCheck.getType() != null &&
                REMOVED_LIST_FROM_INSTANTIATION.containsKey(toCheck.getType().toString()) &&
                !toCheck.getArguments().isEmpty();
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
     * Return an <code>Optional&lt;J.MethodInvocation&gt;</code> with <b>#FieldName(_any_).getName(...)</b>,
     * if present in the given <code>Expression</code>
     *
     * @param toCheck
     * @return
     */
    protected Optional<J.MethodInvocation> getFieldNameGetNameToGetField(Expression toCheck) {
        return ((toCheck instanceof J.MethodInvocation) && (isFieldNameGetNameToGetField((J.MethodInvocation) toCheck)))
                ? Optional.of((J.MethodInvocation) toCheck) : Optional.empty();
    }

    /**
     * Return <code>true</code> if the given <code>J.MethodInvocation</code> is <b>#FieldName(_any_).getName(...)</b>,
     * and the modified method is <b>String(_any_).getField(...)</b>
     * <code>false</code> otherwise
     *
     * @param toCheck
     * @return
     */
    protected boolean isFieldNameGetNameToGetField(J.MethodInvocation toCheck) {
        return toCheck.getMethodType() != null &&
                toCheck.getMethodType().getDeclaringType() != null &&
                GET_NAME_TO_GET_FIELD_CLASSES.contains(toCheck.getMethodType().getDeclaringType().toString()) &&
                toCheck.getName().toString().equals("getName");
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
            method = method
                    .withDeclaringType((JavaType.FullyQualified) JavaType.buildType(String.class.getCanonicalName()))
                    .withReturnType(JavaType.Primitive.String);
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
