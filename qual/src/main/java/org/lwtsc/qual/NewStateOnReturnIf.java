package org.lwtsc.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Repeatable(NewStateOnReturnIf.List.class)
public @interface NewStateOnReturnIf {
  boolean result();
  String state();

  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD})
  public static @interface List {
    /**
     * Return the repeatable annotations.
     *
     * @return the repeatable annotations
     */
    NewStateOnReturnIf[] value();
  }

}
