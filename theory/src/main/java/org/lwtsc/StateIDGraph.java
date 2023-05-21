package org.lwtsc;

import java.util.ArrayList;
import java.util.List;

public class StateIDGraph {

  private final List<ManyToManyEdges> edges = new ArrayList<>();

  public void addEdges(StateIDSet sources, StateIDSet targets) {
    edges.add(new ManyToManyEdges(sources, targets));
  }

  public StateIDSet statesReachableFrom(StateIDSet starts) {
    StateIDSet result = starts;
    boolean changed;
    do {
      changed = false;
      for (ManyToManyEdges edgeSet : edges) {
        if (edgeSet.sources.containsAny(result)) {
          StateIDSet newResult = result.union(edgeSet.targets);
          changed = changed || !newResult.equals(result);
          result = newResult;
        }
      }
    } while (changed);
    return result;
  }

  private static class ManyToManyEdges {
    final StateIDSet sources;
    final StateIDSet targets;
    public ManyToManyEdges(StateIDSet sources, StateIDSet targets) {
      this.sources = sources;
      this.targets = targets;
    }
  }
}
