/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.kie.dmn.core;

import java.util.Arrays;
import java.util.Map;

import org.junit.Test;
import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNMessage;
import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNResult;
import org.kie.dmn.api.core.DMNRuntime;
import org.kie.dmn.api.core.DMNType;
import org.kie.dmn.api.core.FEELPropertyAccessible;
import org.kie.dmn.api.core.ast.ItemDefNode;
import org.kie.dmn.core.api.DMNFactory;
import org.kie.dmn.core.compiler.DMNTypeRegistry;
import org.kie.dmn.core.impl.CompositeTypeImpl;
import org.kie.dmn.core.impl.DMNContextFPAImpl;
import org.kie.dmn.core.impl.DMNModelImpl;
import org.kie.dmn.core.impl.SimpleTypeImpl;
import org.kie.dmn.core.util.DMNRuntimeUtil;
import org.kie.dmn.feel.lang.EvaluationContext;
import org.kie.dmn.feel.lang.impl.EvaluationContextImpl;
import org.kie.dmn.feel.lang.types.AliasFEELType;
import org.kie.dmn.feel.lang.types.BuiltInType;
import org.kie.dmn.feel.util.ClassLoaderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.kie.dmn.core.util.DynamicTypeUtils.entry;
import static org.kie.dmn.core.util.DynamicTypeUtils.mapOf;

public class DMNCompilerTest extends BaseVariantTest {

    public static final Logger LOG = LoggerFactory.getLogger(DMNCompilerTest.class);

    public DMNCompilerTest(VariantTestConf testConfig) {
        super(testConfig);
    }

    @Test
    public void testJavadocSimple() {
        final DMNRuntime runtime = createRuntime("javadocSimple.dmn", this.getClass());
        final DMNModel dmnModel = runtime.getModel("https://kiegroup.org/dmn/_55F8F74F-3E9F-4FAA-BBF4-E6F9534B6B19", "new-file");
        assertThat(dmnModel).isNotNull();

        final DMNType tVowel = dmnModel.getItemDefinitionByName("tVowel").getType();
        assertThat(tVowel).isNotNull();
        assertThat(tVowel.isComposite()).isFalse();
        assertThat(tVowel).isInstanceOf(SimpleTypeImpl.class);
        assertThat(tVowel.getBaseType()).isNotNull();
        assertThat(tVowel.getFields()).hasSize(0);

        final DMNType tNumbers = dmnModel.getItemDefinitionByName("tNumbers").getType();
        assertThat(tNumbers).isNotNull();
        assertThat(tNumbers.isComposite()).isFalse();
        assertThat(tNumbers).isInstanceOf(SimpleTypeImpl.class);
        assertThat(tNumbers.getBaseType()).isNotNull();
        assertThat(tNumbers.getFields()).hasSize(0);

        final DMNContext context = runtime.newContext();
        context.set("a vowel", "a");
        context.set("a list", Arrays.asList(1, 2, 3));
        context.set("vowels", Arrays.asList("a", "a", "e"));

        final DMNResult evaluateAll = evaluateModel(runtime, dmnModel, context);
        LOG.debug("{}", evaluateAll);
        assertThat(evaluateAll.hasErrors()).isFalse();
    }

    @Test
    public void testJavadocComposite() {
        final DMNRuntime runtime = createRuntime("javadocComposite.dmn", this.getClass());
        final DMNModel dmnModel = runtime.getModel("https://kiegroup.org/dmn/_7EC096B1-878B-4E85-8334-58B440BB6AD9", "new-file");
        assertThat(dmnModel).isNotNull();

        final DMNType tPerson = dmnModel.getItemDefinitionByName("tPerson").getType();
        assertThat(tPerson).isNotNull();
        assertThat(tPerson.isComposite()).isTrue();
        assertThat(tPerson).isInstanceOf(CompositeTypeImpl.class);
        assertThat(tPerson.getBaseType()).isNull();
        assertThat(tPerson.getFields()).hasSize(2);

        final DMNContext context = runtime.newContext();
        context.set("a person", mapOf(entry("full name", "John Doe"), entry("age", 47)));

        final DMNResult evaluateAll = evaluateModel(runtime, dmnModel, context);
        LOG.debug("{}", evaluateAll);
        assertThat(evaluateAll.hasErrors()).as(DMNRuntimeUtil.formatMessages(evaluateAll.getMessages())).isFalse();
    }

    @Test
    public void testJavadocInnerComposite() {
        final DMNRuntime runtime = createRuntime("javadocInnerComposite.dmn", this.getClass());
        final DMNModel dmnModel = runtime.getModel("https://kiegroup.org/dmn/_7EC096B1-878B-4E85-8334-58B440BB6AD9bis", "new-file");
        assertThat(dmnModel).isNotNull();
        
        DMNTypeRegistry typeRegistry = ((DMNModelImpl) dmnModel).getTypeRegistry();

        final DMNType tPerson = dmnModel.getItemDefinitionByName("tPerson").getType();
        assertThat(tPerson).isNotNull();
        assertThat(tPerson.isComposite()).isTrue();
        assertThat(tPerson).isInstanceOf(CompositeTypeImpl.class);
        assertThat(tPerson.getBaseType()).isNull();
        assertThat(tPerson.getFields()).hasSize(2);
        assertThat(typeRegistry.resolveType(tPerson.getNamespace(), tPerson.getName())).isNotNull();
        final DMNType addressType = tPerson.getFields().get("address");
        assertThat(addressType).isInstanceOf(CompositeTypeImpl.class);
        assertThat(addressType.getName()).isEqualTo("address");
        assertThat(addressType.getFields()).hasSize(2);
        assertThat(typeRegistry.resolveType(addressType.getNamespace(), addressType.getName())).isNull();

        final DMNType tPart = dmnModel.getItemDefinitionByName("tPart").getType();
        assertThat(tPart).isNotNull();
        assertThat(tPart.isComposite()).isTrue();
        assertThat(tPart).isInstanceOf(CompositeTypeImpl.class);
        assertThat(typeRegistry.resolveType(tPart.getNamespace(), tPart.getName())).isNotNull();
        final DMNType gradeType = tPart.getFields().get("grade");
        assertThat(gradeType).isInstanceOf(SimpleTypeImpl.class);
        assertThat(gradeType.getName()).isEqualTo("grade");
        assertThat(typeRegistry.resolveType(gradeType.getNamespace(), gradeType.getName())).isNull();

        final DMNContext context = runtime.newContext();
        context.set("a person", mapOf(entry("full name", "John Doe"), entry("address", mapOf(entry("country", "IT"), entry("zip", "abcde")))));
        context.set("a part", mapOf(entry("name", "Part 1"), entry("grade", "B")));
        
        final DMNResult evaluateAll = evaluateModel(runtime, dmnModel, context);
        LOG.debug("{}", evaluateAll);
        assertThat(evaluateAll.hasErrors()).as(DMNRuntimeUtil.formatMessages(evaluateAll.getMessages())).isFalse();
    }

    @Test
    public void testItemDefAllowedValuesString() {
        final DMNRuntime runtime = createRuntime("0003-input-data-string-allowed-values.dmn", this.getClass());
        final DMNModel dmnModel = runtime.getModel("https://github.com/kiegroup/kie-dmn", "0003-input-data-string-allowed-values" );
        assertThat(dmnModel).isNotNull();

        final ItemDefNode itemDef = dmnModel.getItemDefinitionByName("tEmploymentStatus" );

        assertThat(itemDef.getName()).isEqualTo("tEmploymentStatus");
        assertThat(itemDef.getId()).isNull();

        final DMNType type = itemDef.getType();

        assertThat(type).isNotNull();
        assertThat(type.getName()).isEqualTo("tEmploymentStatus");
        assertThat(type.getId()).isNull();
        assertThat(type).isInstanceOf(SimpleTypeImpl.class);

        final SimpleTypeImpl feelType = (SimpleTypeImpl) type;

        final EvaluationContext ctx = new EvaluationContextImpl(ClassLoaderUtil.findDefaultClassLoader(), null);
        assertThat(feelType.getFeelType()).isInstanceOf(AliasFEELType.class);
        assertThat(feelType.getFeelType().getName()).isEqualTo("tEmploymentStatus");
        assertThat(feelType.getAllowedValuesFEEL()).hasSize(4);
        assertThat(feelType.getAllowedValuesFEEL().get(0).apply(ctx, "UNEMPLOYED")).isTrue();
        assertThat(feelType.getAllowedValuesFEEL().get(1).apply(ctx, "EMPLOYED")).isTrue();
        assertThat(feelType.getAllowedValuesFEEL().get(2).apply(ctx, "SELF-EMPLOYED")).isTrue();
        assertThat(feelType.getAllowedValuesFEEL().get(3).apply(ctx, "STUDENT")).isTrue();
    }

    @Test
    public void testCompositeItemDefinition() {
        final DMNRuntime runtime = createRuntime("0008-LX-arithmetic.dmn", this.getClass());
        final DMNModel dmnModel = runtime.getModel("https://github.com/kiegroup/kie-dmn", "0008-LX-arithmetic" );
        assertThat(dmnModel).isNotNull();

        final ItemDefNode itemDef = dmnModel.getItemDefinitionByName("tLoan" );

        assertThat(itemDef.getName()).isEqualTo("tLoan");
        assertThat(itemDef.getId()).isEqualTo("tLoan");

        final DMNType type = itemDef.getType();

        assertThat(type).isNotNull();
        assertThat(type.getName()).isEqualTo("tLoan");
        assertThat(type.getId()).isEqualTo("tLoan");
        assertThat(type).isInstanceOf(CompositeTypeImpl.class);

        final CompositeTypeImpl compType = (CompositeTypeImpl) type;

        assertThat(compType.getFields()).hasSize(3);
        final DMNType principal = compType.getFields().get("principal" );
        assertThat(principal).isNotNull();
        assertThat(principal.getName()).isEqualTo("number");
        assertThat(((SimpleTypeImpl)principal).getFeelType()).isEqualTo(BuiltInType.NUMBER);

        final DMNType rate = compType.getFields().get("rate" );
        assertThat(rate).isNotNull();
        assertThat(rate.getName()).isEqualTo("number");
        assertThat(((SimpleTypeImpl)rate).getFeelType()).isEqualTo(BuiltInType.NUMBER);

        final DMNType termMonths = compType.getFields().get("termMonths" );
        assertThat(termMonths).isNotNull();
        assertThat(termMonths.getName()).isEqualTo("number");
        assertThat(((SimpleTypeImpl)termMonths).getFeelType()).isEqualTo(BuiltInType.NUMBER);
    }

    @Test
    public void testCompilationThrowsNPE() {
        try {
            createRuntime("compilationThrowsNPE.dmn", this.getClass());
            fail("shouldn't have reached here.");
        } catch (final Exception ex) {
            assertThat(ex.getMessage()).containsSequence("Unable to compile DMN model for the resource");
        }
    }

    @Test
    public void testRecursiveFunctions() {
        // DROOLS-2161
        final DMNRuntime runtime = createRuntime("Recursive.dmn", this.getClass());
        final DMNModel dmnModel = runtime.getModel("https://github.com/kiegroup/kie-dmn", "Recursive" );
        assertThat(dmnModel).isNotNull();
        assertThat(evaluateModel(runtime, dmnModel, DMNFactory.newContext()).hasErrors()).isFalse();
    }

    @Test
    public void testImport() {
        final DMNRuntime runtime = createRuntimeWithAdditionalResources("Importing_Model.dmn",
                                                                                       this.getClass(),
                                                                                       "Imported_Model.dmn");

        final DMNModel importedModel = runtime.getModel("http://www.trisotech.com/dmn/definitions/_f27bb64b-6fc7-4e1f-9848-11ba35e0df36",
                                                        "Imported Model");
        assertThat(importedModel).isNotNull();
        for (final DMNMessage message : importedModel.getMessages()) {
            LOG.debug("{}", message);
        }

        final DMNModel dmnModel = runtime.getModel("http://www.trisotech.com/dmn/definitions/_f79aa7a4-f9a3-410a-ac95-bea496edab52",
                                                   "Importing Model");
        assertThat(dmnModel).isNotNull();
        for (final DMNMessage message : dmnModel.getMessages()) {
            LOG.debug("{}", message);
        }

        final DMNContext context = runtime.newContext();
        context.set("A Person", mapOf(entry("name", "John"), entry("age", 47)));

        final DMNResult evaluateAll = evaluateModel(runtime, dmnModel, context);
        for (final DMNMessage message : evaluateAll.getMessages()) {
            LOG.debug("{}", message);
        }
        LOG.debug("{}", evaluateAll);
        assertThat(evaluateAll.getDecisionResultByName("Greeting").getResult()).isEqualTo("Hello John!");

        if (isTypeSafe()) {
            FEELPropertyAccessible outputSet = ((DMNContextFPAImpl)evaluateAll.getContext()).getFpa();
            Map<String, Object> allProperties = outputSet.allFEELProperties();
            assertThat(allProperties.get("Greeting")).isEqualTo("Hello John!");
        }
    }

    @Test
    public void testEmptyNamedModelImport() {
        final DMNRuntime runtime = createRuntimeWithAdditionalResources("Importing_EmptyNamed_Model.dmn",
                                                                        this.getClass(),
                                                                        "Imported_Model_Unamed.dmn");

        final DMNModel importedModel = runtime.getModel("http://www.trisotech.com/dmn/definitions/_f27bb64b-6fc7-4e1f-9848-11ba35e0df36",
                                                        "Imported Model");
        assertThat(importedModel).isNotNull();
        for (final DMNMessage message : importedModel.getMessages()) {
            LOG.debug("{}", message);
        }

        final DMNModel importingModel = runtime.getModel("http://www.trisotech.com/dmn/definitions/_f79aa7a4-f9a3-410a-ac95-bea496edabgc",
                                                   "Importing empty-named Model");
        assertThat(importingModel).isNotNull();
        for (final DMNMessage message : importingModel.getMessages()) {
            LOG.debug("{}", message);
        }

        final DMNContext context = runtime.newContext();
        context.set("A Person", mapOf(entry("name", "John"), entry("age", 47)));

        final DMNResult evaluateAll = evaluateModel(runtime, importingModel, context);
        for (final DMNMessage message : evaluateAll.getMessages()) {
            LOG.debug("{}", message);
        }
        LOG.debug("{}", evaluateAll);
        // Verify locally-defined BusinessKnowledgeModel
        assertThat(evaluateAll.getDecisionResultByName("Local Greeting").getResult()).isEqualTo("Local Hello John!");

        if (isTypeSafe()) {
            FEELPropertyAccessible outputSet = ((DMNContextFPAImpl)evaluateAll.getContext()).getFpa();
            Map<String, Object> allProperties = outputSet.allFEELProperties();
            assertThat(allProperties).containsEntry("Local Greeting", "Local Hello John!");
        }
        // Verify unnamed-imported BusinessKnowledgeModel
        assertThat(evaluateAll.getDecisionResultByName("Imported Greeting").getResult()).isEqualTo("Hello John!");

        if (isTypeSafe()) {
            FEELPropertyAccessible outputSet = ((DMNContextFPAImpl)evaluateAll.getContext()).getFpa();
            Map<String, Object> allProperties = outputSet.allFEELProperties();
            assertThat(allProperties).containsEntry("Imported Greeting", "Hello John!");
        }
    }

    @Test
    public void testOverridingEmptyNamedModelImport() {
        final DMNRuntime runtime = createRuntimeWithAdditionalResources("Importing_OverridingEmptyNamed_Model.dmn",
                                                                        this.getClass(),
                                                                        "Imported_Model_Unamed.dmn");

        final DMNModel importedModel = runtime.getModel("http://www.trisotech.com/dmn/definitions/_f27bb64b-6fc7-4e1f-9848-11ba35e0df36",
                                                        "Imported Model");
        assertThat(importedModel).isNotNull();
        for (final DMNMessage message : importedModel.getMessages()) {
            LOG.debug("{}", message);
        }

        final DMNModel importingModel = runtime.getModel("http://www.trisotech.com/dmn/definitions/_f79aa7a4-f9a3-410a-ac95-bea496edabgc",
                                                         "Importing empty-named Model");
        assertThat(importingModel).isNotNull();
        for (final DMNMessage message : importingModel.getMessages()) {
            LOG.debug("{}", message);
        }

        final DMNContext context = runtime.newContext();
        context.set("A Person", mapOf(entry("name", "John"), entry("age", 47)));

        final DMNResult evaluateAll = evaluateModel(runtime, importingModel, context);
        for (final DMNMessage message : evaluateAll.getMessages()) {
            LOG.debug("{}", message);
        }
        LOG.debug("{}", evaluateAll);
        // Verify locally-defined BusinessKnowledgeModel
        assertThat(evaluateAll.getDecisionResultByName("Local Greeting").getResult()).isEqualTo("Local Hello John!");

        if (isTypeSafe()) {
            FEELPropertyAccessible outputSet = ((DMNContextFPAImpl)evaluateAll.getContext()).getFpa();
            Map<String, Object> allProperties = outputSet.allFEELProperties();
            assertThat(allProperties).containsEntry("Local Greeting", "Local Hello John!");
        }
        // Verify unnamed-imported BusinessKnowledgeModel
        assertThat(evaluateAll.getDecisionResultByName("Imported Greeting").getResult()).isEqualTo("Hello John!");

        if (isTypeSafe()) {
            FEELPropertyAccessible outputSet = ((DMNContextFPAImpl)evaluateAll.getContext()).getFpa();
            Map<String, Object> allProperties = outputSet.allFEELProperties();
            assertThat(allProperties).containsEntry("Imported Greeting", "Hello John!");
        }
    }

    @Test
    public void testWrongComparisonOps() {
        final DMNRuntime runtime = createRuntime("WrongComparisonOps.dmn", this.getClass());
        final DMNModel dmnModel = runtime.getModel("http://www.trisotech.com/definitions/_a937d093-86d3-4306-8db8-1e7a33588b68", "Drawing 1");
        assertThat(dmnModel).isNotNull();
        assertThat(dmnModel.hasErrors()).as(DMNRuntimeUtil.formatMessages(dmnModel.getMessages())).isFalse();
        assertThat(dmnModel.getMessages()).as(DMNRuntimeUtil.formatMessages(dmnModel.getMessages())).hasSize(4);
        assertThat(dmnModel.getMessages(DMNMessage.Severity.WARN)).as(DMNRuntimeUtil.formatMessages(dmnModel.getMessages())).hasSize(4);
        assertThat(dmnModel.getMessages(DMNMessage.Severity.WARN)
                           .stream()
                           .filter(m -> m.getSourceId().equals("_d72d6fab-1e67-4fe7-9c12-54800d6fe294") ||
                                        m.getSourceId().equals("_2390dd99-094d-4f97-aecc-9cccb697ce05") ||
                                        m.getSourceId().equals("_0c292d34-498e-4b08-ae99-3c694197b69f") ||
                                        m.getSourceId().equals("_21c7d800-b806-4b2e-9a10-00828de7f2d2"))
                           .count()).as(DMNRuntimeUtil.formatMessages(dmnModel.getMessages())).isEqualTo(4L);
    }
}
