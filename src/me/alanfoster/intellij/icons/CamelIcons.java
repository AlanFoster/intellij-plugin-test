package me.alanfoster.intellij.icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

/**
 * Stores all of the Icons used by this plugin
 *
 * public static access to such data seems to be the common way
 * of doing this in IntelliJ plugins...
 *
 * @author Alan Foster
 * @version 1.0.0-SNAPSHOT
 */
public class CamelIcons {
    public static final String CAMEL_STRING = "/me/alanfoster/intellij/icons/camel.png";
    public static final Icon CAMEL = IconLoader.getIcon(CAMEL_STRING);
}