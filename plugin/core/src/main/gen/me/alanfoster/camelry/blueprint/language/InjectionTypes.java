// This is a generated file. Not intended for manual editing.
package me.alanfoster.camelry.blueprint.language;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import me.alanfoster.camelry.blueprint.language.psi.InjectionElementType;
import me.alanfoster.camelry.blueprint.language.psi.InjectionTokenType;
import me.alanfoster.camelry.blueprint.language.impl.*;

public interface InjectionTypes {

  IElementType PROPERTY_DEFINITION = new InjectionElementType("PROPERTY_DEFINITION");

  IElementType CRLF = new InjectionTokenType("CRLF");
  IElementType FUNCTION_END = new InjectionTokenType("FUNCTION_END");
  IElementType FUNCTION_START = new InjectionTokenType("FUNCTION_START");
  IElementType PROPERTY_NAME = new InjectionTokenType("PROPERTY_NAME");
  IElementType TEXT = new InjectionTokenType("TEXT");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
       if (type == PROPERTY_DEFINITION) {
        return new InjectionPropertyDefinitionImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
