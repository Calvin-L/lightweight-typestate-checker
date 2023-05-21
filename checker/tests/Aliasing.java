import java.util.concurrent.atomic.AtomicReference;

import org.lwtsc.qual.State;
import org.lwtsc.qual.RequiresState;
import org.lwtsc.qual.NewStateOnReturn;

public class Aliasing {

  public void case1() {
    Conn c = new Conn();

    // :: error: (lwtsc.invariant.unstable.typeargument)
    AtomicReference<@State("idle") Conn> ref = new AtomicReference<>(c);

    // This line changes the state of ref.get()
    c.connect();

    @State("idle") Conn cAlias = ref.get();
  }

  public void case2() {
    Conn c = new Conn();

    // :: error: (lwtsc.invariant.unstable.array)
    @State("idle") Conn[] a = new Conn[] { c };

    // Due to aliasing, this changes the state of a[0] and violates the invariant
    // that \A x \in a: x is in state "idle".
    c.connect();

    // These lines are OK; the violation of a's invariant is the problem.
    @State("connected") Conn c1 = c;
    @State("idle") Conn c2 = a[0];
  }

  static class Conn implements AutoCloseable {

    @NewStateOnReturn("idle")
    public Conn() {
    }

    @RequiresState(value="this", state="idle")
    @NewStateOnReturn("connected")
    public void connect() {
    }

    @RequiresState(value="this", state="connected")
    public void use() {
    }

    @Override
    @NewStateOnReturn("idle")
    public void close() {
    }

  }

}
