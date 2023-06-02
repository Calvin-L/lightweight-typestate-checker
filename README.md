# Lightweight Typestate Checker

An easy-to-use typestate checker for Java.

Features:
 - Lightweight: no complicated alias analysis or ownership annotations.
   (Instead, certain annotations have to be "stable"---more on this when I get
   around to writing it up.)
 - Easy-to-use: no need to learn a new language.  Typestate transitions are
   specified using simple annotations: `@RequiresState`, `@NewStateOnReturn`,
   `@NewStateOnReturnIf`, and `@NewStateOnException`.
 - Sound: you are never allowed to call a method when an object could be in the
   wrong state.

## Example

```java
public class Example {

  public static void main(String[] args) throws IOException {
    try (Conn c = new Conn()) {

      // Attempting to use `c` before calling `connect()` is bad!
      //   > error: [contracts.precondition] precondition of c.use is not satisfied.
      //   >   found   : c is @State("idle")
      //   >   required: c is @State("connected")
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
```

## Quickstart

Add `-processor org.lwtsc.LightWeightTypeStateChecker` to your `javac` flags.
(More on adding this to your existing build system when I get around to writing
it up---but you can start by reading
[the official Checker Framework docs](https://checkerframework.org/manual/#external-tools)
and studying
[the example project](/example/)).
