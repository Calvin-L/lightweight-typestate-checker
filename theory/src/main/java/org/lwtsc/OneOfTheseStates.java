package org.lwtsc;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

class OneOfTheseStates implements StateIDSet {

  public static final OneOfTheseStates EMPTY = new OneOfTheseStates(Collections.emptySet());

  private final Set<StateID> states;

  public OneOfTheseStates(Set<StateID> states) {
    this.states = Util.immutableCopy(states);
  }

  @Override
  public String toString() {
    if (states.isEmpty()) {
      return "N/A";
    } else {
      return states.stream().map(Object::toString).collect(Collectors.joining(" or "));
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    OneOfTheseStates that = (OneOfTheseStates) o;
    return states.equals(that.states);
  }

  @Override
  public int hashCode() {
    return states.hashCode();
  }

  @Override
  public <T> T match(Visitor<T> visitor) {
    return visitor.onOneOfTheseStates(states);
  }

  @Override
  public boolean contains(StateID state) {
    return false;
  }

  @Override
  public StateIDSet union(StateIDSet other) {
    if (states.isEmpty()) {
      return other;
    }

    return other.match(new Visitor<StateIDSet>() {
      @Override
      public StateIDSet onUnknown() {
        return other;
      }

      @Override
      public StateIDSet onOneOfTheseStates(Set<StateID> states) {
        return StateIDSet.oneOfTheseStates(Util.union(OneOfTheseStates.this.states, states));
      }
    });
  }

  @Override
  public StateIDSet intersection(StateIDSet other) {
    if (states.isEmpty()) {
      return this;
    }

    return other.match(new Visitor<StateIDSet>() {
      @Override
      public StateIDSet onUnknown() {
        return OneOfTheseStates.this;
      }

      @Override
      public StateIDSet onOneOfTheseStates(Set<StateID> states) {
        return StateIDSet.oneOfTheseStates(Util.intersection(OneOfTheseStates.this.states, states));
      }
    });
  }

}
