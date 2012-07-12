package test;

import libbitster.Timeout;
import libbitster.Actor;
import libbitster.Memo;

class Foo extends Actor {
  public Foo () {
    super();
    Timeout.set(1000, new Memo("test", "1 second", this));
    Timeout.set(4000, new Memo("test", "4 second", this));
    Timeout.set(10000, new Memo("test", "10 second", this));
  }
}

public class TestTimeout {
  public static void main (String[] args) {
    Actor foo = new Foo();
    foo.start();
  }
}
