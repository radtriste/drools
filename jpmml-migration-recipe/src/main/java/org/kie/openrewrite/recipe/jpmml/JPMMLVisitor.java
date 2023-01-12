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

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JPMMLVisitor extends JavaIsoVisitor<ExecutionContext> {

    private static final Logger logger = LoggerFactory.getLogger(JPMMLVisitor.class);
    static final String JPMML_MODEL_PACKAGE_BASE = "org.jpmml.model";
    static final String DMG_PMML_MODEL_PACKAGE_BASE = "org.dmg.pmml";
    static final String TO_MIGRATE_MESSAGE = "TO_MIGRATE";

    private final JavaType.Class originalInstantiatedType;
    private final JavaType targetInstantiatedType;

    private static final String fieldNameFQDN = "org.dmg.pmml.FieldName";


    public JPMMLVisitor(String oldInstantiatedFullyQualifiedTypeName, String newInstantiatedFullyQualifiedTypeName) {
        this.originalInstantiatedType = JavaType.ShallowClass.build(oldInstantiatedFullyQualifiedTypeName);
        this.targetInstantiatedType = JavaType.buildType(newInstantiatedFullyQualifiedTypeName);
    }

    @Override
    public @Nullable J postVisit(J tree, ExecutionContext executionContext) {
        maybeAddImport(targetInstantiatedType.toString());
        maybeRemoveImport(fieldNameFQDN);
        return super.postVisit(tree, executionContext);
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
        logger.debug("visitMethodInvocation {}", method);
        method.getArguments()
                .stream()
                .filter(argument -> argument instanceof J.MethodInvocation)
                .forEach(statement -> visitMethodInvocation((J.MethodInvocation) statement, executionContext));
        method = replaceFieldNameCreate(method);

        return super.visitMethodInvocation(method, executionContext);
    }

    @Override
    public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext executionContext) {
        logger.debug("visitNewClass {}", newClass);
        newClass = replaceInstantiation(newClass);
        return super.visitNewClass(newClass, executionContext);
    }

    @Override
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
        logger.debug("visitCompilationUnit {}", cu);
        boolean toMigrate = toMigrate(cu.getImports());
        executionContext.putMessage(TO_MIGRATE_MESSAGE, toMigrate);
        return super.visitCompilationUnit(cu, executionContext);
    }

    protected J.NewClass replaceInstantiation(J.NewClass newClass) {
        logger.debug("replaceInstantiation {}", newClass);
        if (newClass.getType().toString().equals(originalInstantiatedType.toString())) {
            JavaType.Method updatedMethod = updateType(newClass.getConstructorType());
            TypeTree typeTree = updateTypeTree(newClass);
            newClass = newClass.withConstructorType(updatedMethod)
                    .withClazz(typeTree);
        }
        return newClass;
    }

    /**
     * Returns a new <code>J.MethodInvocation</code> without the <code>org.dmg.pmml.FieldName.create</code>
     * invocation, if present.
     * Otherwise, returns the original method.
     * @param method
     * @return
     */
    protected J.MethodInvocation replaceFieldNameCreate(J.MethodInvocation method) {
        List<J.MethodInvocation> fieldNameInvocations = useFieldNameCreate(method);
        if (fieldNameInvocations.isEmpty()) {
            return method;
        }
        AtomicReference<J.MethodInvocation> toReturn = new AtomicReference<>(method);
        fieldNameInvocations.forEach(toReplace -> {
            List<Expression> replacement = toReplace.getArguments();
            toReturn.getAndUpdate(methodInvocation1 -> methodInvocation1.withArguments(replacement));
        });
        return toReturn.get();
    }

    protected List<J.MethodInvocation> useFieldNameCreate(J.MethodInvocation toCheck) {
        return toCheck.getArguments()
                .stream()
                .filter(argument -> argument instanceof J.MethodInvocation)
                .map(argument -> (J.MethodInvocation) argument)
                .filter(method -> method.getMethodType().getDeclaringType().getFullyQualifiedName().equals("org.dmg.pmml.FieldName")
                        && method.getMethodType().getName().equals("create"))
                .collect(Collectors.toList());
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

    TypeTree updateTypeTree(J.NewClass newClass) {
        return  ((J.Identifier) newClass.getClazz())
                .withSimpleName(((JavaType.ShallowClass) targetInstantiatedType).getClassName())
                .withType(targetInstantiatedType);
    }

}
