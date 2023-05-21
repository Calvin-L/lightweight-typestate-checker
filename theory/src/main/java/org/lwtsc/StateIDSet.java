package org.lwtsc;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A possibly-infinite set of {@link StateID}s.
 *
 * <p>This class does not implement {@link Set} because
 * <ul>
 *   <li>It might represent an infinite set of states, so it might not be iterable and might not have a size</li>
 *   <li>It is immutable, and so cannot implement e.g. {@link Set#add(Object)}</li>
 * </ul>
 */
public interface StateIDSet {

  static StateIDSet empty() {
    return OneOfTheseStates.EMPTY;
  }

  class Caches {
    private static final ConcurrentMap<StateID, StateIDSet> SINGLETON_STATE_ID_SETS = new ConcurrentHashMap<>();
  }

  static StateIDSet singleton(StateID state) {
    return Caches.SINGLETON_STATE_ID_SETS.computeIfAbsent(state, st -> new OneOfTheseStates(Collections.singleton(st)));
  }

  static StateIDSet allStates() {
    return AllStates.INSTANCE;
  }

  static StateIDSet oneOfTheseStates(Set<StateID> states) {
    switch (states.size()) {
      case 0:
        return empty();
      case 1:
        return singleton(states.iterator().next());
      default:
        return new OneOfTheseStates(states);
    }
  }

  interface Visitor<T> {
    T onUnknown();
    T onOneOfTheseStates(Set<StateID> states);
  }

  <T> T match(Visitor<T> visitor);
  boolean contains(StateID state);
  StateIDSet union(StateIDSet other);
  StateIDSet intersection(StateIDSet other);

  default boolean containsAny(StateIDSet other) {
    return !this.intersection(other).equals(empty());
  }

  default boolean containsAll(StateIDSet other) {
    return this.union(other).equals(this);
  }

}
