package org.lwtsc;

import org.lwtsc.qual.NewStateOnException;
import org.lwtsc.qual.NewStateOnReturn;
import org.lwtsc.qual.RequiresState;

import java.io.IOException;

public class Example {

  public static void main(String[] args) throws IOException {
    try (Conn c = new Conn()) {
      c.connect();
      c.use();
    }
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
    @NewStateOnReturn("closed")
    @NewStateOnException(exception=Throwable.class, state="closed")
    public void close() {
    }

  }

}
