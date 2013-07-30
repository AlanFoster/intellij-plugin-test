package me.alanfoster.camelus.blueprint.dom;

import me.alanfoster.camelus.CamelryTestSupport;
import me.alanfoster.camelus.TestHelper;

import java.util.Arrays;
import java.util.List;

import static me.alanfoster.camelus.CamelryProjectDescriptorBuilder.CreateCamelusProject;
import static me.alanfoster.camelus.CamelryProjectDescriptorBuilder.blueprintFiles;
import static me.alanfoster.camelus.CamelryProjectDescriptorBuilder.javaFiles;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;

/**
 * Tests for the Blueprint service DomElement support.
 */
public class Service extends CamelryTestSupport {

    @Override
    public String getTestDataPath() {
        return TestHelper.getTestRoot() + "/blueprint/dom/service";
    }

    public void testServiceRefVariants() {
        CreateCamelusProject(myFixture)
                .with(blueprintFiles("ServiceRefVariants.xml"))
                .withOpenedFile("ServiceRefVariants.xml")
                .with(javaFiles("me.alanfoster.camelus.blueprint.camel.dom.common", commonFile("Person.java")));

        List<String> completionVariants = myFixture.getCompletionVariants("ServiceRefVariants.xml");
        assertReflectionEquals(
                Arrays.asList("customString", "personService"),
                completionVariants);
    }

}
