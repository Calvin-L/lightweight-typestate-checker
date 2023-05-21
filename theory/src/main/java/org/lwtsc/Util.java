package org.lwtsc;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

class Util {

  public static <T> Set<T> immutableCopy(Set<T> set) {
    switch (set.size()) {
      case 0:
        return Collections.emptySet();
      case 1:
        return Collections.singleton(set.iterator().next());
      default:
        return Collections.unmodifiableSet(new HashSet<>(set));
    }
  }

  public static <T> Set<T> union(Set<T> s1, Set<T> s2) {
    if (s1.isEmpty()) {
      return s2;
    }
    if (s2.isEmpty()) {
      return s1;
    }
    Set<T> result = new HashSet<>(s1);
    result.addAll(s2);
    return result;
  }

  public static <T> Set<T> intersection(Set<T> s1, Set<T> s2) {
    if (s1.isEmpty() || s2.isEmpty()) {
      return Collections.emptySet();
    }
    Set<T> result = new HashSet<>(s1);
    result.retainAll(s2);
    return result;
  }
}
