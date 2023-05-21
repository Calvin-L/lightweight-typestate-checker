package org.lwtsc;

import java.io.IOException;
import org.lwtsc.qual.RequiresState;
import org.lwtsc.qual.NewStateOnReturn;

public class ExceptionLeadsToCorruptState {

  public static void main(String[] args) {
    Service s = new Service();
    try {
      s.init();
    } catch (IOException ignored) {
    }

    // :: error: (contracts.precondition)
    s.run();
  }

  public static class Service {
    @NewStateOnReturn("init")
    public Service() {
    }

    @RequiresState(value="this", state="init")
    @NewStateOnReturn("ready")
    public void init() throws IOException {
    }

    @RequiresState(value="this", state="ready")
    public void run() {
    }
  }

}
