package org.lwtsc;

import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.DiagMessage;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import java.util.List;

public class LightWeightTypeStateVisitor extends BaseTypeVisitor<LightWeightTypeStateAnnotatedTypeFactory> {

  private final TypeStateConverter converter;

  public LightWeightTypeStateVisitor(BaseTypeChecker checker) {
    super(checker);
    this.converter = new TypeStateConverter(checker.getProcessingEnvironment());
  }

  private void debug(Tree where, String format, Object... args) {
    checker.report(where, new DiagMessage(
        Diagnostic.Kind.NOTE,
        format,
        args));
  }

  private void checkStability(String messageKey, Tree where, AnnotatedTypeMirror type) {
    StateIDSet allowed = converter.fromAnnotationMirrors(type.getAnnotations());
    StateIDSet reachable = converter.getGraph(atypeFactory, type.getUnderlyingType()).statesReachableFrom(allowed);
    if (!allowed.containsAll(reachable)) {
      checker.reportError(where, messageKey, allowed, reachable);
    }
  }

  private void checkType(Tree t, AnnotatedTypeMirror type) {
    switch (t.getKind()) {
      case ARRAY_TYPE: {
        AnnotatedTypeMirror elemType = ((AnnotatedTypeMirror.AnnotatedArrayType) type).getComponentType();
        checkType(((ArrayTypeTree) t).getType(), elemType);
        checkStability("lwtsc.invariant.unstable.array", t, elemType);
        break;
      }
      case PARAMETERIZED_TYPE: {
        ParameterizedTypeTree ptype = (ParameterizedTypeTree) t;
        AnnotatedTypeMirror.AnnotatedDeclaredType declaredType = (AnnotatedTypeMirror.AnnotatedDeclaredType) type;
        List<? extends Tree> tArgs = ptype.getTypeArguments();
        List<? extends AnnotatedTypeMirror> tMirrorArgs = declaredType.getTypeArguments();
        if (tArgs.size() == tMirrorArgs.size()) {
          for (int i = 0; i < tArgs.size(); ++i) {
            checkType(tArgs.get(i), tMirrorArgs.get(i));
            checkStability("lwtsc.invariant.unstable.typeargument", tArgs.get(i), tMirrorArgs.get(i));
          }
        }
        break;
      }
    }
  }

  @Override
  public Void visitVariable(VariableTree node, Void p) {
    super.visitVariable(node, p);

    @Nullable VariableElement elt = TreeUtils.elementFromDeclaration(node);
    if (elt != null) {
      AnnotatedTypeMirror declaredType = atypeFactory.fromElement(elt);
      checkType(node.getType(), declaredType);

      if (elt.getKind().isField()) {
        checkStability("lwtsc.invariant.unstable.field", node, declaredType);
      }
    }

    return null;
  }

}
