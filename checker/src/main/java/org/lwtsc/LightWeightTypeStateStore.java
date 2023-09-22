package org.lwtsc;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.aliasing.AliasingAnnotatedTypeFactory;
import org.checkerframework.common.aliasing.AliasingChecker;
import org.checkerframework.common.aliasing.qual.Unique;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.expression.ArrayAccess;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeFactory;

import javax.lang.model.element.ExecutableElement;
import java.util.Collections;
import java.util.Map;

public class LightWeightTypeStateStore extends CFAbstractStore<CFValue, LightWeightTypeStateStore> {

  protected LightWeightTypeStateStore(CFAbstractAnalysis<CFValue, LightWeightTypeStateStore, ?> analysis, boolean sequentialSemantics) {
    super(analysis, sequentialSemantics);
  }

  protected LightWeightTypeStateStore(LightWeightTypeStateStore other) {
    super(other);
  }

  private boolean isDefinitelyPreservedByMethodCall(
      MethodInvocationNode methodCall,
      JavaExpression expr,
      @Nullable AnnotatedTypeFactory aliasTypeFactory,
      @Nullable CFAbstractValue<?> aliasInformationForExpr) {

    return expr.isUnmodifiableByOtherCode() ||
        (aliasInformationForExpr != null && aliasInformationForExpr.getAnnotations()
            .stream()
            .anyMatch(a -> aliasTypeFactory != null && aliasTypeFactory.areSameByClass(a, Unique.class)));

  }

  private static <V extends CFAbstractValue<V>> @Nullable V getValue(@Nullable CFAbstractStore<V, ?> store, JavaExpression e) {
    return store != null ? store.getValue(e) : null;
  }

  public Map<FieldAccess, CFValue> getFieldValues() {
    return Collections.unmodifiableMap(fieldValues);
  }

  public Map<ArrayAccess, CFValue> getArrayValues() {
    return Collections.unmodifiableMap(arrayValues);
  }

  @SuppressWarnings("assignment") // thisValue is not marked @Nullable, although it can be null
  protected void clearThisValue() {
    thisValue = null;
  }

  @Override
  public void updateForMethodCall(MethodInvocationNode methodInvocationNode, AnnotatedTypeFactory atypeFactory, CFValue val) {
    ExecutableElement method = methodInvocationNode.getTarget().getMethod();

    if (!atypeFactory.isSideEffectFree(method)) {
      AliasingAnnotatedTypeFactory aliasTypeFactory =
          atypeFactory.getChecker().getTypeFactoryOfSubchecker(AliasingChecker.class);
      CFStore aliasInformation = aliasTypeFactory != null
          ? aliasTypeFactory.getStoreBefore(methodInvocationNode)
          : null;

      localVariableValues.keySet().removeIf(e -> !isDefinitelyPreservedByMethodCall(
          methodInvocationNode,
          e,
          aliasTypeFactory,
          getValue(aliasInformation, e)));

      clearThisValue();

      fieldValues.keySet().removeIf(e -> !isDefinitelyPreservedByMethodCall(
          methodInvocationNode,
          e,
          aliasTypeFactory,
          getValue(aliasInformation, e)));

      arrayValues.keySet().removeIf(e -> !isDefinitelyPreservedByMethodCall(
          methodInvocationNode,
          e,
          aliasTypeFactory,
          getValue(aliasInformation, e)));

      // TODO: need to write some tests around this.  If I understand correctly, CF remembers types of
      //       @Deterministic methods in `methodValues`.  However, that isn't necessarily sound: even
      //       if the returned pointer is @Deterministic, the typestate of the pointed-to object might
      //       be modified by other method calls.  Things to check:
      //         - If the method returns an @State-annotated object, this should not clobber that info.
      //         - If we modify a pointer, the typestate of a @Deterministic getter is erased.
      methodValues.keySet().removeIf(e -> !isDefinitelyPreservedByMethodCall(
          methodInvocationNode,
          e,
          aliasTypeFactory,
          getValue(aliasInformation, e)));
    }

    // store information about method call if possible
    JavaExpression methodCall = JavaExpression.fromNode(methodInvocationNode);
    replaceValue(methodCall, val);
  }

}
