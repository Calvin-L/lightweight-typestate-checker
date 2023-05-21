package org.lwtsc;

import org.lwtsc.qual.State;
import org.lwtsc.qual.RequiresState;
import org.lwtsc.qual.NewStateOnReturn;
import org.lwtsc.qual.NewStateOnReturnIf;
import org.checkerframework.common.aliasing.qual.Unique;
import org.checkerframework.common.aliasing.qual.NonLeaked;

public class ConditionalTransitions {

  public void case1(@Unique FizzBuzz x) {
    for (;;) {
      if (x.ok()) {
        try {
          Thread.yield();
          x.act();
        } catch (Throwable e) {
          e.printStackTrace();
        }
      } else {
        x.restore();
      }
    }
  }

  public static class FizzBuzz {
    @NewStateOnReturnIf(result=true, state="ok")
    @NewStateOnReturnIf(result=false, state="bad")
    public boolean ok(@NonLeaked FizzBuzz this) {
      return true;
    }

    @RequiresState(value="this", state="ok")
    @NewStateOnReturn("exhausted")
    public void act(@NonLeaked FizzBuzz this) {
    }

    @RequiresState(value="this", state="bad")
    @NewStateOnReturn("ok")
    public void restore(@NonLeaked FizzBuzz this) {
    }
  }

}
