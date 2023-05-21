package org.lwtsc;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class StateID {

  static final ConcurrentMap<String, StateID> STATE_IDS = new ConcurrentHashMap<>();

  public static StateID of(String id) {
    return STATE_IDS.computeIfAbsent(id, StateID::new);
  }

  public static final StateID CORRUPT = of("corrupt");

  private final String id;

  private StateID(String id) {
    this.id = Objects.requireNonNull(id);
  }

  @Override
  public String toString() {
    return "state '" + id + '\'';
  }

  public String getID() {
    return id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    StateID stateID = (StateID) o;
    return id.equals(stateID.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

}
