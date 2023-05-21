import org.lwtsc.qual.State;
import org.lwtsc.qual.NewStateOnReturn;
import org.lwtsc.qual.NewStateOnException;
import org.lwtsc.qual.RequiresState;
import org.lwtsc.qual.EnsuresState;

import java.util.List;
import java.util.ArrayList;

public class Collections {

  public void case1() {
    // :: error: (lwtsc.invariant.unstable.typeargument)
    List<@State("in-use") Resource> l1 = new ArrayList<>();
  }

  public void case2() {
    // OK: resource type is stable
    List<@State({"in-use", "exhausted", "ambiguous"}) Resource> l1 = new ArrayList<>();
  }

  static class Resource {
    @NewStateOnReturn("in-use")
    public Resource() {
    }

    @RequiresState(value="this", state="in-use")
    @NewStateOnReturn("exhausted")
    @NewStateOnException(exception=Throwable.class, state="ambiguous")
    public void use() {
    }
  }

}
