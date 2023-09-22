package org.lwtsc;

import com.sun.tools.javac.code.Type;
import org.lwtsc.qual.NewStateOnException;
import org.lwtsc.qual.NewStateOnReturn;
import org.lwtsc.qual.NewStateOnReturnIf;
import org.lwtsc.qual.RequiresState;
import org.lwtsc.qual.State;
import org.lwtsc.qual.StateBottom;
import org.lwtsc.qual.UnknownState;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implements conversion between javac types and the pure types {@link StateID}/{@link StateIDSet}.
 */
class TypeStateConverter {

  private final ProcessingEnvironment env;

  private final Map<StateID, AnnotationMirror> singletonStateAnnotationMirrors = new HashMap<>();

  /**
   * Cached field, equal to <code>TreeUtils.getMethod(State.class, "value", 0, env)</code>
   */
  private final ExecutableElement state_dot_value;
  private final ExecutableElement requiresState_dot_value;
  private final ExecutableElement requiresState_dot_state;
  private final ExecutableElement requiresState_dot_List_dot_value;
  private final ExecutableElement newStateOnReturn_dot_value;
  private final ExecutableElement newStateOnReturnIf_dot_result;
  private final ExecutableElement newStateOnReturnIf_dot_state;
  private final ExecutableElement newStateOnReturnIf_dot_List_dot_value;
  private final ExecutableElement newStateOnException_dot_exception;
  private final ExecutableElement newStateOnException_dot_state;
  private final ExecutableElement newStateOnException_dot_List_dot_value;
  private final TypeMirror throwableClass;
  private final AnnotationMirror unknownState;
  private final AnnotationMirror noState;
  private final StateIDSet.Visitor<AnnotationMirror> toAnnotationMirrorVisitor;

  public TypeStateConverter(ProcessingEnvironment env) {
    this.env = Objects.requireNonNull(env);
    this.state_dot_value = TreeUtils.getMethod(State.class, "value", 0, env);
    this.requiresState_dot_value = TreeUtils.getMethod(RequiresState.class, "value", 0, env);
    this.requiresState_dot_state = TreeUtils.getMethod(RequiresState.class, "state", 0, env);
    this.requiresState_dot_List_dot_value = TreeUtils.getMethod(RequiresState.List.class, "value", 0, env);
    this.newStateOnReturn_dot_value = TreeUtils.getMethod(NewStateOnReturn.class, "value", 0, env);
    this.newStateOnReturnIf_dot_result = TreeUtils.getMethod(NewStateOnReturnIf.class, "result", 0, env);
    this.newStateOnReturnIf_dot_state = TreeUtils.getMethod(NewStateOnReturnIf.class, "state", 0, env);
    this.newStateOnReturnIf_dot_List_dot_value = TreeUtils.getMethod(NewStateOnReturnIf.List.class, "value", 0, env);
    this.newStateOnException_dot_exception = TreeUtils.getMethod(NewStateOnException.class, "exception", 0, env);
    this.newStateOnException_dot_state = TreeUtils.getMethod(NewStateOnException.class, "state", 0, env);
    this.newStateOnException_dot_List_dot_value = TreeUtils.getMethod(NewStateOnException.List.class, "value", 0, env);
    this.throwableClass = env.getTypeUtils().getDeclaredType(ElementUtils.getTypeElement(env, Throwable.class));
    this.unknownState = AnnotationBuilder.fromClass(env.getElementUtils(), UnknownState.class);
    this.noState = AnnotationBuilder.fromClass(env.getElementUtils(), StateBottom.class);

    Function<StateID, AnnotationMirror> makeSingletonAnnotation = state -> makeAnnotation(env, state_dot_value, Collections.singleton(state));

    this.toAnnotationMirrorVisitor = new StateIDSet.Visitor<AnnotationMirror>() {
      @Override
      public AnnotationMirror onUnknown() {
        return unknownState;
      }

      @Override
      public AnnotationMirror onOneOfTheseStates(Set<StateID> states) {
        switch (states.size()) {
          case 0:
            return noState;
          case 1:
            return singletonStateAnnotationMirrors.computeIfAbsent(states.iterator().next(), makeSingletonAnnotation);
          default:
            return makeAnnotation(env, state_dot_value, states);
        }
      }
    };
  }

  private static AnnotationMirror makeAnnotation(ProcessingEnvironment env, ExecutableElement state_dot_value, Set<StateID> states) {
    return new AnnotationBuilder(env, State.class)
        .setValue(state_dot_value, states.stream().map(StateID::getID).collect(Collectors.toList()))
        .build();
  }

  StateIDSet fromAnnotationMirror(AnnotationMirror a) {
    Class<?> cls = AnnotationUtils.annotationMirrorToClass(a);
    if (cls == UnknownState.class) {
      return StateIDSet.allStates();
    } else if (cls == State.class) {
      return StateIDSet.oneOfTheseStates(
          AnnotationUtils.getElementValueArray(a, state_dot_value, String.class)
              .stream()
              .map(StateID::of)
              .collect(Collectors.toSet()));
    } else if (cls == StateBottom.class) {
      return StateIDSet.empty();
    } else {
      throw new IllegalArgumentException("Illegal annotation class: " + cls.getCanonicalName());
    }
  }

  AnnotationMirror toAnnotationMirror(StateIDSet t) {
    return t.match(toAnnotationMirrorVisitor);
  }

  public StateIDSet fromAnnotationMirrors(Iterable<? extends AnnotationMirror> annotations) {
    for (AnnotationMirror m : annotations) {
      try {
        return fromAnnotationMirror(m);
      } catch (IllegalArgumentException ignored) {
        // try the next one
      }
    }
    return StateIDSet.allStates();
  }

  private List<AnnotationMirror> getRequiresStateDeclarations(AnnotatedTypeFactory factory, ExecutableElement method) {
    List<AnnotationMirror> newStateOnExceptionAnnotations;

    @Nullable AnnotationMirror stateTransitionAnnotationList =
        factory.getDeclAnnotation(method, RequiresState.List.class);
    if (stateTransitionAnnotationList != null) {
      newStateOnExceptionAnnotations = AnnotationUtils.getElementValueArray(
          stateTransitionAnnotationList,
          requiresState_dot_List_dot_value,
          AnnotationMirror.class);
    } else {
      newStateOnExceptionAnnotations = new ArrayList<>(1);
    }

    @Nullable AnnotationMirror stateTransitionAnnotation =
        factory.getDeclAnnotation(method, RequiresState.class);
    if (stateTransitionAnnotation != null) {
      newStateOnExceptionAnnotations.add(stateTransitionAnnotation);
    }

    return newStateOnExceptionAnnotations;
  }

  public StateIDSet getRequiredStateForReceiver(AnnotatedTypeFactory factory, ExecutableElement method) {
    StateIDSet result = StateIDSet.allStates();

    // TODO: is it possible to put an @State annotation on the receiver?

    for (/* RequiresState */ AnnotationMirror requirement : getRequiresStateDeclarations(factory, method)) {
      List<String> exprs = AnnotationUtils.getElementValueArray(requirement, requiresState_dot_value, String.class);
      if (exprs.contains("this")) {
        List<String> states = AnnotationUtils.getElementValueArray(requirement, requiresState_dot_state, String.class);
        result = result.intersection(StateIDSet.oneOfTheseStates(states.stream().map(StateID::of).collect(Collectors.toSet())));
      }
    }

    return result;
  }

  public @Nullable StateID getDeclaredNewStateOnReturn(AnnotatedTypeFactory factory, ExecutableElement method) {
    @Nullable AnnotationMirror stateTransitionAnnotation =
        factory.getDeclAnnotation(method, NewStateOnReturn.class);
    if (stateTransitionAnnotation == null) {
      return null;
    }
    @Nullable String val = AnnotationUtils.getElementValue(stateTransitionAnnotation, newStateOnReturn_dot_value, String.class);
    if (val == null) {
      return null;
    }
    return StateID.of(val);
  }

  private List<AnnotationMirror> getRawNewStateOnReturnIfDeclarations(AnnotatedTypeFactory factory, ExecutableElement method) {
    List<AnnotationMirror> newStateOnReturnIfAnnotations;

    @Nullable AnnotationMirror stateTransitionAnnotationList =
        factory.getDeclAnnotation(method, NewStateOnReturnIf.List.class);

    if (stateTransitionAnnotationList != null) {
      newStateOnReturnIfAnnotations = AnnotationUtils.getElementValueArray(
          stateTransitionAnnotationList,
          newStateOnReturnIf_dot_List_dot_value,
          AnnotationMirror.class);
    } else {
      newStateOnReturnIfAnnotations = new ArrayList<>(1);
    }

    @Nullable AnnotationMirror stateTransitionAnnotation =
        factory.getDeclAnnotation(method, NewStateOnReturnIf.class);
    if (stateTransitionAnnotation != null) {
      newStateOnReturnIfAnnotations.add(stateTransitionAnnotation);
    }

    return newStateOnReturnIfAnnotations;
  }

  public @Nullable StateID getDeclaredNewStateOnReturnBool(AnnotatedTypeFactory factory, ExecutableElement method, boolean value) {
    List<AnnotationMirror> annotations = getRawNewStateOnReturnIfDeclarations(factory, method);
    StateID resultState = null;
    for (AnnotationMirror a : annotations) {
      boolean result = AnnotationUtils.getElementValueBoolean(a, newStateOnReturnIf_dot_result, true);
      @Nullable String newStateOnReturnIf_dot_state_value = AnnotationUtils.getElementValue(a, newStateOnReturnIf_dot_state, String.class);
      if (newStateOnReturnIf_dot_state_value != null) {
        StateID state = StateID.of(newStateOnReturnIf_dot_state_value);
        if (result == value) {
          if (resultState == null) {
            resultState = state;
          } else {
            // TODO: report error?
          }
        }
      }
    }
    return resultState;
  }

  private List<AnnotationMirror> getRawNewStateOnExceptionDeclarations(AnnotatedTypeFactory factory, ExecutableElement method) {
    List<AnnotationMirror> newStateOnExceptionAnnotations;

    @Nullable AnnotationMirror stateTransitionAnnotationList =
        factory.getDeclAnnotation(method, NewStateOnException.List.class);
    if (stateTransitionAnnotationList != null) {
      newStateOnExceptionAnnotations = AnnotationUtils.getElementValueArray(
          stateTransitionAnnotationList,
          newStateOnException_dot_List_dot_value,
          AnnotationMirror.class);
    } else {
      newStateOnExceptionAnnotations = new ArrayList<>(1);
    }

    @Nullable AnnotationMirror stateTransitionAnnotation =
        factory.getDeclAnnotation(method, NewStateOnException.class);
    if (stateTransitionAnnotation != null) {
      newStateOnExceptionAnnotations.add(stateTransitionAnnotation);
    }

    return newStateOnExceptionAnnotations;
  }

  public Map<Type.ClassType, StateID> getNewStateOnExceptionDeclarations(AnnotatedTypeFactory factory, ExecutableElement method) {
    Map<Type.ClassType, StateID> result = new LinkedHashMap<>();
    for (AnnotationMirror a : getRawNewStateOnExceptionDeclarations(factory, method)) {
      Type.ClassType exception = AnnotationUtils.getElementValue(a, newStateOnException_dot_exception, Type.ClassType.class);
      if (exception != null) {
        @Nullable String newStateOnException_dot_state_value = AnnotationUtils.getElementValue(a, newStateOnException_dot_state, String.class);
        if (newStateOnException_dot_state_value != null) {
          StateID state = StateID.of(newStateOnException_dot_state_value);
          result.put(exception, state);
        }
      }
    }
    return result;
  }

  public StateIDSet getNewStateOnException(Types types, Map<Type.ClassType, StateID> newStateOnExceptionDeclarations, TypeMirror caughtException) {
    // TODO: is this right...?
    //
    //   if I write `@NewStateOnException(exception=IOException.class, state="ambiguous")`
    //   and you catch `Exception`,
    //   then "corrupt" and "ambiguous" are both possible.
    //
    //   if I write `@NewStateOnException(exception=IOException.class, state="ambiguous")`
    //   and you catch `IOException`,
    //   then only "ambiguous" is possible.
    //
    //   if I write `@NewStateOnException(exception=IOException.class, state="ambiguous")`
    //   and you catch `FileNotFoundException`,
    //   then only "ambiguous" is possible.

    // find the most specific supertype of caughtException
    @Nullable TypeMirror mostSpecificException = null;
    StateID state = StateID.CORRUPT;
    for (Map.Entry<Type.ClassType, StateID> a : newStateOnExceptionDeclarations.entrySet()) {
      Type.ClassType exception = a.getKey();
      if (types.isSubtype(caughtException, exception) && (mostSpecificException == null || types.isSubtype(exception, mostSpecificException))) {
        state = a.getValue();
        mostSpecificException = exception;
      }
    }

    // anything that is a subtype of `mostSpecificException` is also possible
    // TODO: ... unless it is caught by a different block :/
    Set<StateID> allPossibleStates = new LinkedHashSet<>();
    allPossibleStates.add(state);
    for (Map.Entry<Type.ClassType, StateID> a : newStateOnExceptionDeclarations.entrySet()) {
      Type.ClassType exception = a.getKey();
      if (mostSpecificException == null || types.isSubtype(exception, mostSpecificException)) {
        allPossibleStates.add(a.getValue());
      }
    }

    return StateIDSet.oneOfTheseStates(allPossibleStates);
  }

  public StateIDGraph getGraph(AnnotatedTypeFactory factory, TypeMirror type) {
    StateIDGraph result = new StateIDGraph();
    switch (type.getKind()) {
      case DECLARED: {
        DeclaredType decl = (DeclaredType) type;
        Element e = decl.asElement();
        switch (e.getKind()) {
          case INTERFACE:
          case CLASS: {
            // TODO: inherited methods?
            for (Element subElement : e.getEnclosedElements()) {
              if (subElement.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) subElement;
                StateIDSet start = getRequiredStateForReceiver(factory, method);
                @Nullable StateID onReturn = getDeclaredNewStateOnReturn(factory, method);
                StateIDSet end = onReturn != null ? StateIDSet.singleton(onReturn) : StateIDSet.empty();
                Map<Type.ClassType, StateID> newStatesByException = getNewStateOnExceptionDeclarations(factory, method);
                for (Map.Entry<?, StateID> entry : newStatesByException.entrySet()) {
                  end = end.union(StateIDSet.singleton(entry.getValue()));
                }
                end = end.union(getNewStateOnException(env.getTypeUtils(), newStatesByException, throwableClass));
                result.addEdges(start, end);
              }
            }
            break;
          }
          // These types are logically immutable
          case ANNOTATION_TYPE:
          case ENUM:
//          case RECORD: // TODO: Java 15+
          default:
            return result;
        }
        break;
      }
      default:
        // conservatively assume everything is reachable from everything else
        result.addEdges(StateIDSet.allStates(), StateIDSet.allStates());
        break;
    }
    return result;
  }

}
