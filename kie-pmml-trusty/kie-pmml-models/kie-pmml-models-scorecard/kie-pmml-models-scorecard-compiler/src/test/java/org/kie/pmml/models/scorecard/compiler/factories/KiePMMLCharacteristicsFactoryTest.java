/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.pmml.models.scorecard.compiler.factories;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.PMML;
import org.dmg.pmml.TransformationDictionary;
import org.dmg.pmml.scorecard.Characteristic;
import org.dmg.pmml.scorecard.Characteristics;
import org.dmg.pmml.scorecard.Scorecard;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.memorycompiler.KieMemoryCompiler;
import org.kie.pmml.api.exceptions.KiePMMLException;
import org.kie.pmml.commons.model.expressions.KiePMMLApply;
import org.kie.pmml.commons.model.expressions.KiePMMLConstant;
import org.kie.pmml.commons.model.expressions.KiePMMLFieldRef;
import org.kie.pmml.commons.model.predicates.KiePMMLCompoundPredicate;
import org.kie.pmml.commons.model.predicates.KiePMMLSimplePredicate;
import org.kie.pmml.commons.model.predicates.KiePMMLSimpleSetPredicate;
import org.kie.pmml.commons.model.predicates.KiePMMLTruePredicate;
import org.kie.pmml.commons.utils.KiePMMLModelUtils;
import org.kie.pmml.compiler.commons.mocks.HasClassLoaderMock;
import org.kie.pmml.compiler.commons.utils.JavaParserUtils;
import org.kie.pmml.compiler.testutils.TestUtils;
import org.kie.pmml.models.scorecard.model.KiePMMLAttribute;
import org.kie.pmml.models.scorecard.model.KiePMMLCharacteristic;
import org.kie.pmml.models.scorecard.model.KiePMMLCharacteristics;
import org.kie.pmml.models.scorecard.model.KiePMMLComplexPartialScore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.kie.pmml.commons.Constants.PACKAGE_CLASS_TEMPLATE;
import static org.kie.pmml.compiler.commons.CommonTestingUtils.getFieldsFromDataDictionary;
import static org.kie.pmml.compiler.commons.testutils.CodegenTestUtils.commonValidateCompilationWithImports;
import static org.kie.pmml.compiler.commons.utils.JavaParserUtils.MAIN_CLASS_NOT_FOUND;
import static org.kie.pmml.compiler.commons.utils.ModelUtils.getDerivedFields;
import static org.kie.pmml.models.scorecard.compiler.factories.KiePMMLCharacteristicsFactory.KIE_PMML_CHARACTERISTICS_TEMPLATE;
import static org.kie.pmml.models.scorecard.compiler.factories.KiePMMLCharacteristicsFactory.KIE_PMML_CHARACTERISTICS_TEMPLATE_JAVA;
import static org.kie.test.util.filesystem.FileUtils.getFileContent;

public class KiePMMLCharacteristicsFactoryTest {

    private static final String BASIC_COMPLEX_PARTIAL_SCORE_SOURCE = "BasicComplexPartialScore.pmml";
    private static final String CONTAINER_CLASS_NAME = KiePMMLModelUtils.getGeneratedClassName("Scorecard");
    private static final String PACKAGE_NAME = "packagename";
    private static final String TEST_01_SOURCE = "KiePMMLCharacteristicsFactoryTest_01.txt";
    private static final CompilationUnit characteristicsCloneCU =
            JavaParserUtils.getKiePMMLModelCompilationUnit(CONTAINER_CLASS_NAME,
                                                           PACKAGE_NAME,
                                                           KIE_PMML_CHARACTERISTICS_TEMPLATE_JAVA,
                                                           KIE_PMML_CHARACTERISTICS_TEMPLATE);
    private static PMML basicComplexPartialScorePmml;
    private static DataDictionary basicComplexPartialScoreDataDictionary;
    private static TransformationDictionary basicComplexPartialScoreTransformationDictionary;
    private static Scorecard basicComplexPartialScore;
    private static List<DerivedField> basicComplexPartialScoreDerivedFields;
    private static Characteristics basicComplexPartialScoreCharacteristics;
    private static Characteristic basicComplexPartialScoreFirstCharacteristic;
    private ClassOrInterfaceDeclaration characteristicsTemplate;

    @BeforeClass
    public static void setupClass() throws Exception {
        basicComplexPartialScorePmml = TestUtils.loadFromFile(BASIC_COMPLEX_PARTIAL_SCORE_SOURCE);
        basicComplexPartialScoreDataDictionary = basicComplexPartialScorePmml.getDataDictionary();
        basicComplexPartialScoreTransformationDictionary = basicComplexPartialScorePmml.getTransformationDictionary();
        basicComplexPartialScore = ((Scorecard) basicComplexPartialScorePmml.getModels().get(0));
        basicComplexPartialScoreCharacteristics = basicComplexPartialScore.getCharacteristics();
        basicComplexPartialScoreFirstCharacteristic =
                basicComplexPartialScoreCharacteristics.getCharacteristics().get(0);
        basicComplexPartialScoreDerivedFields = getDerivedFields(basicComplexPartialScoreTransformationDictionary,
                                                                 basicComplexPartialScore.getLocalTransformations());
    }

    @Before
    public void setup() {
        characteristicsTemplate = characteristicsCloneCU.getClassByName(CONTAINER_CLASS_NAME)
                .orElseThrow(() -> new KiePMMLException(MAIN_CLASS_NOT_FOUND + ": " + CONTAINER_CLASS_NAME))
                .clone();
    }

    @Test
    public void getKiePMMLCharacteristics() {
        final KiePMMLCharacteristics retrieved =
                KiePMMLCharacteristicsFactory.getKiePMMLCharacteristics(basicComplexPartialScoreCharacteristics,
                                                                        getFieldsFromDataDictionary(basicComplexPartialScoreDataDictionary),
                                                                        PACKAGE_NAME,
                                                                        new HasClassLoaderMock());
        assertNotNull(retrieved);
    }

    @Test
    public void getKiePMMLCharacteristicsSourcesMap() {
        final Map<String, String> retrieved =
                KiePMMLCharacteristicsFactory.getKiePMMLCharacteristicsSourcesMap(basicComplexPartialScoreCharacteristics,
                                                                                  getFieldsFromDataDictionary(basicComplexPartialScoreDataDictionary),
                                                                                  CONTAINER_CLASS_NAME,
                                                                                  PACKAGE_NAME);
        assertNotNull(retrieved);
        assertEquals(1, retrieved.size());
        String expected = String.format(PACKAGE_CLASS_TEMPLATE, PACKAGE_NAME, CONTAINER_CLASS_NAME);
        assertTrue(retrieved.containsKey(expected));
        try {
            KieMemoryCompiler.compile(retrieved, Thread.currentThread().getContextClassLoader());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void setCharacteristicsVariableDeclaration() {
        KiePMMLCharacteristicsFactory.setCharacteristicsVariableDeclaration(CONTAINER_CLASS_NAME,
                                                                            basicComplexPartialScoreCharacteristics,
                                                                            getFieldsFromDataDictionary(basicComplexPartialScoreDataDictionary),
                                                                            characteristicsTemplate);
        List<Class<?>> imports = Arrays.asList(KiePMMLApply.class,
                                               KiePMMLAttribute.class,
                                               KiePMMLCharacteristic.class,
                                               KiePMMLCharacteristics.class,
                                               KiePMMLComplexPartialScore.class,
                                               KiePMMLCompoundPredicate.class,
                                               KiePMMLConstant.class,
                                               KiePMMLFieldRef.class,
                                               KiePMMLSimplePredicate.class,
                                               KiePMMLSimpleSetPredicate.class,
                                               KiePMMLTruePredicate.class,
                                               Arrays.class,
                                               Collections.class);
        commonValidateCompilationWithImports(characteristicsTemplate, imports);
    }

    @Test
    public void addGetCharacteristicMethod() throws IOException {
        final String characteristicName = "CharacteristicName";
        String expectedMethod = "get" + characteristicName;
        assertTrue(characteristicsTemplate.getMethodsByName(expectedMethod).isEmpty());
        KiePMMLCharacteristicsFactory.addGetCharacteristicMethod(characteristicName,
                                                                 basicComplexPartialScoreFirstCharacteristic,
                                                                 getFieldsFromDataDictionary(basicComplexPartialScoreDataDictionary),
                                                                 characteristicsTemplate);
        assertEquals(1, characteristicsTemplate.getMethodsByName(expectedMethod).size());
        MethodDeclaration retrieved = characteristicsTemplate.getMethodsByName(expectedMethod).get(0);
        String text = getFileContent(TEST_01_SOURCE);
        MethodDeclaration expected = JavaParserUtils
                .parseMethod(String.format(text, characteristicName));
        assertTrue(JavaParserUtils.equalsNode(expected, retrieved));
        List<Class<?>> imports = Arrays.asList(KiePMMLApply.class,
                                               KiePMMLAttribute.class,
                                               KiePMMLCharacteristic.class,
                                               KiePMMLCharacteristics.class,
                                               KiePMMLComplexPartialScore.class,
                                               KiePMMLCompoundPredicate.class,
                                               KiePMMLConstant.class,
                                               KiePMMLFieldRef.class,
                                               KiePMMLSimplePredicate.class,
                                               KiePMMLSimpleSetPredicate.class,
                                               KiePMMLTruePredicate.class,
                                               Arrays.class,
                                               Collections.class);
        commonValidateCompilationWithImports(retrieved, imports);
    }
}