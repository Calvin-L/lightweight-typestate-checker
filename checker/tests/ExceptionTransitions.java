package org.lwtsc;

import java.io.IOException;
import java.io.FileNotFoundException;
import org.lwtsc.qual.State;
import org.lwtsc.qual.RequiresState;
import org.lwtsc.qual.NewStateOnReturn;
import org.lwtsc.qual.NewStateOnException;
import org.checkerframework.common.aliasing.qual.Unique;

public class ExceptionTransitions {

  public void case0() throws IOException, Busy {
    try (Transaction tx = new Transaction()) {
      tx.commit();
    }
  }

  public void case1() throws IOException {
    try (@Unique Transaction tx = new Transaction()) {
      boolean done;
      for (;;) {
        try {
          tx.commit();
          break;
        } catch (Busy exception) {
          // NOTE: without @Unique, CF would conservatively assume that this
          // call could modify tx's state and this test would fail.
          Thread.yield();
        }
      }
    }
  }

  public void case2(@State("in-flight") Transaction tx) throws IOException, Busy {
    try {
      tx.commit();
    } catch (BusySubException exception) {
      // still legal: all subclasses of Busy mean "in-flight"
      tx.commit();
    } catch (IOException | Busy e) {
      // :: error: (contracts.precondition)
      tx.commit();
    }
  }

  public void case3(@Unique @State("in-flight") Transaction tx) throws IOException, Busy {
    try {
      tx.noCorruption();
    } finally {
      // always legal:
      //   - return keeps same state
      //   - Throwable (and by extension all exceptions) keeps same state
      tx.commit();
    }
  }

  public void case4(@Unique @State("in-flight") Transaction tx) throws IOException, Busy {
    try {
      tx.write();
    } catch (FileNotFoundException exception) {
      // Actually, this should work!
      // See underlying issue: https://github.com/typetools/checker-framework/issues/5936
      // :: error: (contracts.precondition)
      tx.commit();
    }
  }

  public static class Busy extends Exception {
  }

  public static class BusySubException extends Busy {
  }

  public static class Transaction implements AutoCloseable {
    @NewStateOnReturn("in-flight")
    public @Unique Transaction() {
    }

    @RequiresState(value="this", state="in-flight")
    @NewStateOnException(exception=IOException.class, state="fizzbuzz")
    @NewStateOnException(exception=FileNotFoundException.class, state="in-flight")
    public void write() throws Busy, IOException {
    }

    @RequiresState(value="this", state="in-flight")
    @NewStateOnReturn("committed")
    @NewStateOnException(exception=Busy.class, state="in-flight")
    @NewStateOnException(exception=IOException.class, state="ambiguous")
    public void commit() throws Busy, IOException {
    }

    @RequiresState(value="this", state="in-flight")
    @NewStateOnException(exception=Throwable.class, state="in-flight")
    public void noCorruption() {
    }

    @Override
    @NewStateOnReturn("closed")
    public void close() {
    }
  }

}
