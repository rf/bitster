public class Test {
  public static void main (String[] args) {
    Overlord o = new Overlord();

    Foo foo = new Foo(o);
    Bar bar = new Bar(o);

    bar.setFriend(foo);
    foo.setFriend(bar);

    while (true) {
      o.communicate(1000);
      System.out.println("end of selection!");
    }
  }
}

class Foo implements Communicator {
  public SelectableQueue<String> queue;
  Bar friend;
  int times = 0;

  public void onMemo () {
    String memo = queue.poll();
    System.out.println("foo got a message: " + memo);
    times++;
    if (times < 10) friend.queue.offer(memo.toLowerCase() + " FOOOO");
  }

  public Foo (Overlord o) {
    queue = new SelectableQueue<String>();
    o.register(queue, this);
  }

  public void setFriend (Bar f) { 
    friend = f;
    f.queue.offer("hi its foo");
  }

  public void onReadable () {}
  public void onWritable () {}
  public void onAcceptable () {}
}

class Bar implements Communicator {
  public SelectableQueue<String> queue;

  Foo friend;

  public Bar (Overlord o) {
    queue = new SelectableQueue<String>();
    o.register(queue, this);
  }

  public void setFriend (Foo f) { friend = f; }

  public void onMemo () {
    String memo = queue.poll();
    System.out.println("bar got a message: " + memo);

    friend.queue.offer(memo.toUpperCase() + " BAR");
  }

  public void onReadable () {}
  public void onWritable () {}
  public void onAcceptable () {}
}

