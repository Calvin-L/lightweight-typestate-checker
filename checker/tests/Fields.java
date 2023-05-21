import org.lwtsc.qual.State;
import org.lwtsc.qual.NewStateOnReturn;
import org.lwtsc.qual.RequiresState;
import org.lwtsc.qual.EnsuresState;

public class Fields {

  // :: error: (lwtsc.invariant.unstable.field)
  private @State("in-use") Resource r = null;

  public void case1() {
    r = new Resource();
    // If `r` were legal, this would make its state annotation incorrect!
    r.use();
  }

  public void case2() {
    // If `r` were legal, this would make its state annotation incorrect!
    r.use();
  }

  public void case3() {
    Resource copy = r;
    r = null;
    copy.use();
    r = new Resource();
  }

  static class Resource {
    @NewStateOnReturn("in-use")
    public Resource() {
    }

    @RequiresState(value="this", state="in-use")
    @NewStateOnReturn("exhausted")
    public void use() {
    }
  }

}
