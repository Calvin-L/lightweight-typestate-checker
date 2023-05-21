package org.lwtsc.qual;

import org.checkerframework.framework.qual.InheritedAnnotation;
import org.checkerframework.framework.qual.PostconditionAnnotation;
import org.checkerframework.framework.qual.QualifierArgument;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@InheritedAnnotation
@Retention(RetentionPolicy.RUNTIME)
@PostconditionAnnotation(qualifier = State.class)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Repeatable(EnsuresState.List.class)
public @interface EnsuresState {

  // Postconditions must use "value" as the name (conditional postconditions use "expression").
  String[] value();

  @QualifierArgument("value")
  String[] state();

  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
  @InheritedAnnotation
  public static @interface List {
    /**
     * Return the repeatable annotations.
     *
     * @return the repeatable annotations
     */
    EnsuresState[] value();
  }

}
