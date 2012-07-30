package test;

import libbitster.Util;
import libbitster.Actor;
import libbitster.Memo;

class Foo2 extends Actor {
  public Memo m;

  public Foo2 () {
    super();
    Util.setTimeout(100, new Memo("test", "100ms", this));
  }

  protected void receive (Memo memo) {
    System.out.println(memo);
    m = memo;
  }
}

public class TestTimeout {
  public static void main (String[] args) {
    Foo2 foo = new Foo2();
    foo.start();
    while (true) {
      try { Thread.sleep(100); } catch (Exception e) {}
      if (foo.m != null) {
        System.out.println("message was received");
        assert(foo.m.getType() == "test");
        foo.shutdown();
        Util.shutdown();
        break;
      }
    }
  }
}
