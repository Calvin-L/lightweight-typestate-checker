package org.lwtsc;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.AnnotationMirrorSet;

import javax.lang.model.type.TypeMirror;

public class LightWeightTypeStateAnalysis extends CFAbstractAnalysis<CFValue, LightWeightTypeStateStore, LightWeightTypeStateTransfer> {

  public LightWeightTypeStateAnalysis(BaseTypeChecker checker, LightWeightTypeStateAnnotatedTypeFactory factory) {
    super(checker, factory);
  }

  @Override
  public LightWeightTypeStateStore createEmptyStore(boolean sequentialSemantics) {
    return new LightWeightTypeStateStore(this, sequentialSemantics);
  }

  @Override
  public LightWeightTypeStateStore createCopiedStore(LightWeightTypeStateStore lightWeightTypeStateStore) {
    return new LightWeightTypeStateStore(lightWeightTypeStateStore);
  }

  @Override
  public @Nullable CFValue createAbstractValue(AnnotationMirrorSet annotations, TypeMirror underlyingType) {
    return defaultCreateAbstractValue(this, annotations, underlyingType);
  }

}
