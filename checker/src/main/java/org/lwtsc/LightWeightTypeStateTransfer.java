package org.lwtsc;

import com.sun.tools.javac.code.Type;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeFactory;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class LightWeightTypeStateTransfer extends CFAbstractTransfer<CFValue, LightWeightTypeStateStore, LightWeightTypeStateTransfer> {

  private final TypeStateConverter converter;

  public LightWeightTypeStateTransfer(CFAbstractAnalysis<CFValue, LightWeightTypeStateStore, LightWeightTypeStateTransfer> analysis) {
    super(analysis);
    AnnotatedTypeFactory atypeFactory = analysis.getTypeFactory();
    this.converter = atypeFactory instanceof LightWeightTypeStateAnnotatedTypeFactory
        ? ((LightWeightTypeStateAnnotatedTypeFactory) atypeFactory).getConverter()
        : new TypeStateConverter(atypeFactory.getProcessingEnv());
  }

  public StateIDSet getNewStateOnException(ExecutableElement method, TypeMirror caughtException) {
    AnnotatedTypeFactory factory = analysis.getTypeFactory();
    Types types = factory.getProcessingEnv().getTypeUtils();
    Map<Type.ClassType, StateID> newStateOnExceptionDeclarations = converter.getNewStateOnExceptionDeclarations(factory, method);
    return converter.getNewStateOnException(types, newStateOnExceptionDeclarations, caughtException);
  }

  @Override
  public TransferResult<CFValue, LightWeightTypeStateStore> visitMethodInvocation(
      MethodInvocationNode n,
      TransferInput<CFValue, LightWeightTypeStateStore> in) {
    TransferResult<CFValue, LightWeightTypeStateStore> result = super.visitMethodInvocation(n, in);

    AnnotatedTypeFactory atypeFactory = analysis.getTypeFactory();
    JavaExpression receiver = JavaExpression.fromNode(n.getTarget().getReceiver());
    ExecutableElement method = n.getTarget().getMethod();

    // Update the receiver's typestate
    @Nullable StateID maybeNewState = converter.getDeclaredNewStateOnReturn(atypeFactory, method);
    if (maybeNewState != null) {
      replaceIntoStores(result, receiver, converter.toAnnotationMirror(StateIDSet.singleton(maybeNewState)));
    }

    @Nullable StateID ifTrue = converter.getDeclaredNewStateOnReturnBool(atypeFactory, method, true);
    @Nullable StateID ifFalse = converter.getDeclaredNewStateOnReturnBool(atypeFactory, method, false);

    if (ifTrue != null || ifFalse != null) {
      result = new ConditionalTransferResult<>(
          result.getResultValue(),
          result.getThenStore(),
          result.getElseStore(),
          result.getExceptionalStores(),
          result.storeChanged());

      if (ifTrue != null) {
        replaceIntoStore(result.getThenStore(), receiver, converter.toAnnotationMirror(StateIDSet.singleton(ifTrue)));
      }

      if (ifFalse != null) {
        replaceIntoStore(result.getElseStore(), receiver, converter.toAnnotationMirror(StateIDSet.singleton(ifFalse)));
      }
    }

    // Typestate is "corrupt" on exception (or whatever was declared in the annotations)
    Block block = n.getBlock();
    Map<TypeMirror, Set<Block>> exceptionalSuccessors =
        block != null // TODO: is block ever actually null?
          ? getExceptionalSuccessors(block)
          : Collections.emptyMap();
    Map<TypeMirror, LightWeightTypeStateStore> exceptionalStores = new LinkedHashMap<>(exceptionalSuccessors.size());

    @Nullable Map<TypeMirror, LightWeightTypeStateStore> oldExceptionalStores = result.getExceptionalStores();
    if (oldExceptionalStores != null) {
      exceptionalStores.putAll(oldExceptionalStores);
    }

    for (TypeMirror exnType : exceptionalSuccessors.keySet()) {
      LightWeightTypeStateStore store = in.getRegularStore().copy();
      AnnotationMirror newState = converter.toAnnotationMirror(
          getNewStateOnException(
              method,
              exnType));
      replaceIntoStore(store, receiver, newState);
      exceptionalStores.put(exnType, store);
    }

    return attachExceptionalStores(result, exceptionalStores);
  }

  private <V extends AbstractValue<V>, S extends Store<S>> TransferResult<V, S> attachExceptionalStores(
      TransferResult<V, S> result,
      Map<TypeMirror, S> exceptionalStores) {
    return result.containsTwoStores()
        ? new ConditionalTransferResult<>(
            result.getResultValue(),
            result.getThenStore(),
            result.getElseStore(),
            exceptionalStores,
            result.storeChanged())
        : new RegularTransferResult<>(
            result.getResultValue(),
            result.getRegularStore(),
            exceptionalStores,
            result.storeChanged());
  }

  /**
   * TODO: the result is missing some information; see
   *       <a href="https://github.com/typetools/checker-framework/issues/5936">CF issue #5936</a>
   */
  private Map<TypeMirror, Set<Block>> getExceptionalSuccessors(Block block) {
    return block instanceof ExceptionBlock
        ? ((ExceptionBlock) block).getExceptionalSuccessors()
        : Collections.emptyMap();
  }

  protected <V extends CFAbstractValue<V>, S extends CFAbstractStore<V, S>> void replaceIntoStores(TransferResult<V, S> result, JavaExpression e, AnnotationMirror newAnnotation) {
    if (result.containsTwoStores()) {
      replaceIntoStore(result.getThenStore(), e, newAnnotation);
      replaceIntoStore(result.getElseStore(), e, newAnnotation);
    } else {
      replaceIntoStore(result.getRegularStore(), e, newAnnotation);
    }
  }

  protected void replaceIntoStore(CFAbstractStore<?, ?> store, JavaExpression e, AnnotationMirror newAnnotation) {
    store.clearValue(e);
    store.insertValue(e, newAnnotation);
  }

}
