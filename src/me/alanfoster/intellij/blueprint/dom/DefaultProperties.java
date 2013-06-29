package me.alanfoster.intellij.blueprint.dom;

import com.intellij.util.xml.*;

import java.util.List;

/**
 * @author Alan Foster
 * @version 1.0.0-SNAPSHOT
 */
@NameStrategy(value = HyphenNameStrategy.class)
public interface DefaultProperties extends DomElement {
    @SubTagList("property")
    List<Property> getProperties();
}
