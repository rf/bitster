class Bar extends Actor {
  private Actor friend;

  public Bar (Actor f) {
    f.post(new String("test"));
    friend = f;
  }

  protected void receive (Object memo) {
    String m = (String) memo;
    System.out.println("bar received memo: " + m);
    friend.post(m.toLowerCase() + " bar is the MAN ");
  }
}

class Foo extends Actor {
  private Actor friend;

  public void setFriend (Actor f) { friend = f; }

  protected void receive (Object memo) {
    System.out.println("foo received memo: " + memo);
    friend.post(((String) memo).toUpperCase());
  }
}

class Test {

  public static void main (String[] arguments) {
    Bar bar;
    Foo foo;
    foo = new Foo();
    bar = new Bar(foo);
    foo.setFriend(bar);
  }
}

