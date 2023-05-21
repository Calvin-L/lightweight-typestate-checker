package org.lwtsc;

class AllStates implements StateIDSet {

  public static final AllStates INSTANCE = new AllStates();

  private AllStates() {
  }

  @Override
  public String toString() {
    return "an unknown state";
  }

  @Override
  public int hashCode() {
    return -1276941173;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof AllStates;
  }

  @Override
  public <T> T match(Visitor<T> visitor) {
    return visitor.onUnknown();
  }

  @Override
  public boolean contains(StateID state) {
    return false;
  }

  @Override
  public StateIDSet union(StateIDSet other) {
    return this;
  }

  @Override
  public StateIDSet intersection(StateIDSet other) {
    return other;
  }

}
