package org.lwtsc;

import com.sun.source.tree.NewClassTree;
import org.lwtsc.qual.State;
import org.lwtsc.qual.StateBottom;
import org.lwtsc.qual.UnknownState;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.TreeUtils;

import javax.lang.model.element.ExecutableElement;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class LightWeightTypeStateAnnotatedTypeFactory extends GenericAnnotatedTypeFactory<CFValue, LightWeightTypeStateStore, LightWeightTypeStateTransfer, LightWeightTypeStateAnalysis> {

  private final TypeStateConverter converter;

  @SuppressWarnings("method.invocation") // postInit() call not allowed
  public LightWeightTypeStateAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);

    // NOTE: This is a *CRITICAL* part of this checker.  It is a little insane to me
    // that this is defined in the "AnnotatedTypeFactory" and not as part of the
    // transfer rules, but c'est la vie...
    super.sideEffectsUnrefineAliases = true;

    this.converter = new TypeStateConverter(checker.getProcessingEnvironment());
    postInit();
  }

  public TypeStateConverter getConverter() {
    return converter;
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return new LinkedHashSet<>(Arrays.asList(
        UnknownState.class,
        State.class,
        StateBottom.class));
  }

  @Override
  protected QualifierHierarchy createQualifierHierarchy() {
    return new LightWeightTypeStateQualifierHierarchy(
        getSupportedTypeQualifiers(),
        getElementUtils(),
        new TypeStateConverter(getProcessingEnv()));
  }

  @Override
  protected TreeAnnotator createTreeAnnotator() {
    return new ListTreeAnnotator(super.createTreeAnnotator(), new LightWeightTypeStateTreeAnnotator());
  }

  private class LightWeightTypeStateTreeAnnotator extends TreeAnnotator {
    protected LightWeightTypeStateTreeAnnotator() {
      super(LightWeightTypeStateAnnotatedTypeFactory.this);
    }

    @Override
    public Void visitNewClass(NewClassTree node, AnnotatedTypeMirror annotatedTypeMirror) {
      ExecutableElement element = TreeUtils.elementFromUse(node);
      @Nullable StateID initialState = converter.getDeclaredNewStateOnReturn(LightWeightTypeStateAnnotatedTypeFactory.this, element);
      if (initialState != null) {
        annotatedTypeMirror.replaceAnnotation(converter.toAnnotationMirror(StateIDSet.singleton(initialState)));
      }
      return null;
    }
  }

  @Override
  protected LightWeightTypeStateAnalysis createFlowAnalysis() {
    return new LightWeightTypeStateAnalysis(getChecker(), this);
  }

  @Override
  public LightWeightTypeStateTransfer createFlowTransferFunction(CFAbstractAnalysis<CFValue, LightWeightTypeStateStore, LightWeightTypeStateTransfer> analysis) {
    return new LightWeightTypeStateTransfer(analysis);
  }

}
