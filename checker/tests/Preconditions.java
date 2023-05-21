import org.lwtsc.qual.RequiresState;
import org.lwtsc.qual.NewStateOnReturn;

public class Preconditions {

  public static void main(String[] args) {
    Conn c = new Conn();
    try {
      c.connect();
    } catch (Exception e) {
      e.printStackTrace();
    }

    // :: error: (contracts.precondition)
    c.use();
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
