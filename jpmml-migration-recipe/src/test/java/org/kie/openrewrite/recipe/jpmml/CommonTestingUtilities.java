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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.Java11Parser;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

public class CommonTestingUtilities {

    private static final List<Path> paths = JavaParser.runtimeClasspath();

    private CommonTestingUtilities() {
    }

    public static J.CompilationUnit getCompilationUnitFromClassSource(String classSource) {
        JavaParser parser = Java11Parser.builder()
                .classpath(paths)
                .logCompilationWarningsAndErrors(true)
                .build();
        return parser.parse(classSource).get(0);
    }

    public static Collection<J.MethodInvocation> getMethodInvocationFromClassSource(String classSource,
                                                                                    String methodInvocation) {
        J.CompilationUnit compilationUnit = getCompilationUnitFromClassSource(classSource);
        Collection<J.MethodInvocation> toReturn = new ArrayList<>();
        compilationUnit.getClasses().forEach(classDeclaration -> populateWithMethodInvocation(toReturn, classDeclaration.getBody(), methodInvocation));
        return toReturn;
    }

    public static Collection<J.NewClass> getNewClassFromClassSource(String classSource,
                                                                                    String fqdnInstantiatedClass) {
        J.CompilationUnit compilationUnit = getCompilationUnitFromClassSource(classSource);
        Collection<J.NewClass> toReturn = new ArrayList<>();
        compilationUnit.getClasses().forEach(classDeclaration -> populateNewClass(toReturn, classDeclaration.getBody(), fqdnInstantiatedClass));
        return toReturn;
    }

    public static ExecutionContext getExecutionContext(Throwable expected) {
        return new InMemoryExecutionContext(throwable -> org.assertj.core.api.Assertions.assertThat(throwable).isEqualTo(expected));
    }

    private static void populateWithMethodInvocation(final Collection<J.MethodInvocation> toPopulate, J.Block body, String methodInvocation) {
        body.getStatements().forEach(statement -> {
            if (statement instanceof J.MethodInvocation && statement.toString().startsWith(methodInvocation + "(")) {
                toPopulate.add((J.MethodInvocation) statement);
            }
            if (statement instanceof J.Block) {
                populateWithMethodInvocation(toPopulate, (J.Block) statement, methodInvocation);
            }
            if (statement instanceof J.MethodDeclaration) {
                populateWithMethodInvocation(toPopulate, ((J.MethodDeclaration) statement).getBody(), methodInvocation);
            }
        });
    }

    private static void populateNewClass(final Collection<J.NewClass> toPopulate, J.Block body, String fqdnInstantiatedClass) {
        body.getStatements().forEach(statement -> {
            if (statement instanceof J.NewClass && ((J.NewClass) statement).getType().toString().equals(fqdnInstantiatedClass)) {
                toPopulate.add((J.NewClass) statement);
            }
            if (statement instanceof J.Block) {
                populateNewClass(toPopulate, (J.Block) statement, fqdnInstantiatedClass);
            }
            if (statement instanceof J.MethodDeclaration) {
                populateNewClass(toPopulate, ((J.MethodDeclaration) statement).getBody(), fqdnInstantiatedClass);
            }
            if (statement instanceof J.VariableDeclarations) {
                ((J.VariableDeclarations)statement).getVariables().forEach(namedVariable -> {
                    Expression initializer = namedVariable.getInitializer();
                    if (initializer instanceof J.NewClass && (initializer).getType().toString().equals(fqdnInstantiatedClass)) {
                        toPopulate.add((J.NewClass) initializer);
                    }

                });
            }
        });
    }
}
