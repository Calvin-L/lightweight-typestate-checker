import org.lwtsc.qual.NewStateOnReturn;
import org.lwtsc.qual.RequiresState;

class Basics {

  public void run() {

    Conn c = new Conn();

    c.connect();

    // :: error: (contracts.precondition)
    c.connect();

    c.use();

    c.close();

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
