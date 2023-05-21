import org.lwtsc.qual.NewStateOnReturn;
import org.lwtsc.qual.RequiresState;
import org.lwtsc.qual.State;

public class Arrays {

  // :: error: (lwtsc.invariant.unstable.array)
  private final @State("in-use") Resource[] r = new Resource[10];

  public void case1() {
    r[0] = new Resource();
    // If `r` were legal, this would make its state annotation incorrect!
    r[0].use();
  }

  public void case2() {
    // If `r` were legal, this would make its state annotation incorrect!
    r[5].use();
  }

  public void case3() {
    Resource copy = r[5];
    r[5] = null;
    copy.use();
    copy.refresh();
    r[5] = copy;
  }

  static class Resource {
    @NewStateOnReturn("in-use")
    public Resource() {
    }

    @RequiresState(value="this", state="in-use")
    @NewStateOnReturn("exhausted")
    public void use() {
    }

    @RequiresState(value="this", state="exhausted")
    @NewStateOnReturn("in-use")
    public void refresh() {
    }
  }

}
