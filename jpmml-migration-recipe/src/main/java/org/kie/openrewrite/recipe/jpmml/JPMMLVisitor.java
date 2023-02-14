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
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
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

    private static final Map<String, RemovedListTupla> REMOVED_LIST_FROM_INSTANTIATION = Map.of(DATADICTIONARY_FQDN,
            new RemovedListTupla("addDataFields", JavaType.buildType("org.dmg.pmml.DataField")));


    private static final J.Identifier STRING_IDENTIFIER = new J.Identifier(Tree.randomId(), Space.build(" ", Collections.emptyList()), Markers.EMPTY, "String", JavaType.buildType(String.class.getCanonicalName()), null);

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
        String cuName = cu.getSourcePath().toString();
        boolean toMigrate = toMigrate(cu.getImports());
        if (!toMigrate) {
            logger.info("Skipping {}", cuName);
            return cu;
        } else {
            logger.info("Going to migrate {}", cuName);
        }
        executionContext.putMessage(TO_MIGRATE_MESSAGE, toMigrate);
        try {
            cu = (J.CompilationUnit) super.visitCompilationUnit(cu, executionContext);

            maybeAddImport(targetInstantiatedType.toString());
                if (Boolean.TRUE.equals(executionContext.getMessage(TO_MIGRATE_MESSAGE))) {
                    cu = (J.CompilationUnit) new ChangeType(FIELD_NAME_FQDN, String.class.getCanonicalName(), false)
                            .getVisitor()
                            .visitCompilationUnit(cu, executionContext);
                }
                removeFieldNameImport(cu);
            return cu;
        } catch (Throwable t) {
            logger.error("Failed to visit {}", cu, t);
            //logger.error(TreeVisitingPrinter.printTree(cu));
            return cu;
        }
    }

    @Override
    public Expression visitExpression(Expression expression, ExecutionContext executionContext) {
        logger.trace("visitExpression {}", expression);
        Optional<J.MethodInvocation> fieldNameCreate = getFieldNameCreate(expression);
        if (fieldNameCreate.isPresent()) {
            executionContext.putMessage(TO_MIGRATE_MESSAGE, true);
            Expression createArgument = fieldNameCreate.get().getArguments().get(0);
            createArgument = visitExpression(createArgument, executionContext);
            return createArgument;
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
        Optional<J.MethodInvocation> hasFieldNameParameter = getHasFieldNameParameter(expression);
        if (hasFieldNameParameter.isPresent()) {
            executionContext.putMessage(TO_MIGRATE_MESSAGE, true);
            JavaType.Method methodType = hasFieldNameParameter.get().getMethodType()
                    .withParameterTypes(Collections.singletonList(JavaType.Primitive.String));
            return hasFieldNameParameter.get()
                    .withMethodType(methodType);
        }
        if (expression instanceof J.Binary) {
            Expression left = visitExpression(((J.Binary)expression).getLeft(), executionContext);
            Expression right = visitExpression(((J.Binary)expression).getRight(), executionContext);
            expression = ((J.Binary)expression)
                    .withLeft(left)
                    .withRight(right);
        }
        if (expression instanceof J.NewClass) {
            expression = replaceInstantiation((J.NewClass) expression, executionContext);
        }
        return (Expression) super.visitExpression(expression, executionContext);
    }

    @Override
    public J visitParameterizedType(J.ParameterizedType type, ExecutionContext executionContext) {
        return super.visitParameterizedType(type, executionContext);
    }

    @Override
    public @Nullable JavaType visitType(@Nullable JavaType javaType, ExecutionContext executionContext) {
        return super.visitType(javaType, executionContext);
    }

    protected J.CompilationUnit removeFieldNameImport(J.CompilationUnit cu) {
        if (hasFieldNameImport(cu)) {
            List<J.Import> cleanedImports = new ArrayList<>();
            for (J.Import imported : cu.getImports()) {
                if (! isFieldNameImport(imported)) {
                    cleanedImports.add(imported);
                }
            }
            return cu.withImports(cleanedImports);
        } else {
            return cu;
        }

    }

    protected boolean hasFieldNameImport(J.CompilationUnit cu) {
        return cu.getImports().stream().anyMatch(this::isFieldNameImport);
    }

    protected boolean isFieldNameImport(J.Import anImport) {
        return  (anImport.getQualid().getType() instanceof JavaType.Class) &&  ((JavaType.Class) anImport.getQualid().getType()).getFullyQualifiedName().equals(FIELD_NAME_FQDN);
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
        Optional<RemovedListTupla> optionalRetrieved = getRemovedListTupla(newClass);
        if (optionalRetrieved.isPresent()) {
            RemovedListTupla removedListTupla =optionalRetrieved.get();
            return removedListTupla.getJMethod(newClass);
        } else {
            return newClass;
        }
    }

    /**
     * Return <code>Optional&lt;RemovedListTupla&gt;</code> if the given <code>J.NewClass</code> constructor has not the <b>List</b> anymore
     * <code>Optional.empty()</code> otherwise
     *
     * @param toCheck
     * @return
     */
    protected Optional<RemovedListTupla> getRemovedListTupla(J.NewClass toCheck) {
        return toCheck.getType() != null &&
                REMOVED_LIST_FROM_INSTANTIATION.containsKey(toCheck.getType().toString()) &&
                toCheck.getArguments() != null &&
                !toCheck.getArguments().isEmpty()
                && (toCheck.getArguments().get(0) instanceof J.Identifier) ? Optional.of(REMOVED_LIST_FROM_INSTANTIATION.get(toCheck.getType().toString())) : Optional.empty();
    }

    /**
     * Return an <code>Optional&lt;J.MethodInvocation&gt;</code> with <b>FieldName.create(...)</b>,
     * if present in the given <code>Expression</code>
     *
     * @param toCheck
     * @return
     */
    protected Optional<J.MethodInvocation> getHasFieldNameParameter(Expression toCheck) {
        return ((toCheck instanceof J.MethodInvocation) && (hasFieldNameParameter((J.MethodInvocation) toCheck)))
                ? Optional.of((J.MethodInvocation) toCheck) : Optional.empty();
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
     * Return <code>true</code> if the given <code>J.MethodInvocation</code> is <b>FieldName.create(...)</b>,
     * <code>false</code> otherwise
     *
     * @param toCheck
     * @return
     */
    protected boolean hasFieldNameParameter(J.MethodInvocation toCheck) {
        return toCheck.getMethodType() != null &&
                toCheck.getMethodType().getParameterTypes() != null &&
                toCheck.getMethodType().getParameterTypes().stream().anyMatch(javaType -> javaType != null && javaType.toString().equals(FIELD_NAME_FQDN));
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

    private static class RemovedListTupla {

        private final String addMethodName;

        private final J.Identifier elementIdentifier;

        private final JavaType.Array elementArray;

        private final J.Identifier elementToArrayIdentifier;
        private final J.Identifier addMethodIdentifier;
        public RemovedListTupla(String addMethodName, JavaType elementJavaType) {
            this.addMethodName = addMethodName;
            elementIdentifier = new J.Identifier(Tree.randomId(), Space.build(" ", Collections.emptyList()), Markers.EMPTY, elementJavaType.toString(), elementJavaType, null);
            elementArray = new JavaType.Array(null, elementJavaType);
            JavaType.Parameterized elementListJavaType = new JavaType.Parameterized(null, (JavaType.FullyQualified) LIST_JAVA_TYPE, List.of(elementJavaType));
            elementToArrayIdentifier = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, "toArray", elementListJavaType, null);
            addMethodIdentifier = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, addMethodName, elementListJavaType, null);
        }

        public J.MethodInvocation getJMethod(J.NewClass newClass) {
            J.Identifier originalListIdentifier = (J.Identifier) newClass.getArguments().get(0);
            J.Literal literal = new J.Literal(Tree.randomId(), Space.EMPTY, Markers.EMPTY, 0, "0", null, JavaType.Primitive.Int);
            J.ArrayDimension arrayDimension = new J.ArrayDimension(Tree.randomId(), Space.EMPTY, Markers.EMPTY, JRightPadded.build(literal));
            J.NewArray newArray = new J.NewArray(Tree.randomId(), Space.EMPTY, Markers.EMPTY, elementIdentifier, Collections.singletonList(arrayDimension), null, elementArray);
            JavaType.Method methodType = new JavaType.Method(null, 1025, LIST_GENERIC_JAVA_TYPE, "toArray",
                    elementArray,
                    Collections.singletonList("arg0"),
                    Collections.singletonList(elementArray), null, null);

            J.MethodInvocation toArrayInvocation = new J.MethodInvocation(Tree.randomId(), Space.EMPTY, Markers.EMPTY, null, null,
                    elementToArrayIdentifier,
                    JContainer.build(Collections.emptyList()),
                    methodType)
                    .withSelect(originalListIdentifier)
                    .withArguments(Collections.singletonList(newArray));
            JavaType.Method constructorType = newClass.getConstructorType()
                    .withParameterTypes(Collections.emptyList())
                    .withParameterNames(Collections.emptyList());
            J.NewClass noArgClass = newClass.withArguments(Collections.emptyList())
                    .withConstructorType(constructorType);

            JavaType.Method addMethodInvocation =  new JavaType.Method(null, 1025,
                    (JavaType.FullyQualified) JavaType.buildType(noArgClass.getType().toString()),
                    addMethodName,
                    JavaType.Primitive.Void,
                    Collections.singletonList("toAdd"),
                    Collections.singletonList(elementArray), null, null);

            return new J.MethodInvocation(Tree.randomId(), Space.EMPTY, Markers.EMPTY, null, null,
                    addMethodIdentifier,
                    JContainer.build(Collections.emptyList()),
                    addMethodInvocation)
                    .withSelect(noArgClass)
                    .withArguments(Collections.singletonList(toArrayInvocation));
        }
    }

}