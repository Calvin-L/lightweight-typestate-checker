package org.lwtsc;

import org.checkerframework.common.aliasing.AliasingChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;

import java.util.LinkedHashSet;

public class LightWeightTypeStateChecker extends BaseTypeChecker {

  @Override
  protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
    LinkedHashSet<Class<? extends BaseTypeChecker>> result = new LinkedHashSet<>(super.getImmediateSubcheckerClasses());
    result.add(AliasingChecker.class);
    return result;
  }

}
