package libbitster;
import java.util.concurrent.ConcurrentLinkedQueue;

// # Actor
// Simple actor class. Can receive messages. Once it is instantiated, it's a
// thread, spinning, calling its `receive()` method (which should be
// overridden) upon receiving messages, and its `idle()` method when there are
// no messages to handle.

public class Actor extends Thread {
  protected ConcurrentLinkedQueue<Object> queue;
  private boolean running;

  public Actor () {
    super();
    queue = new ConcurrentLinkedQueue<Object>();
    start();
  }

  // `post`s a message to this actor.
  public void post (Object memo) {
    queue.offer(memo);
  }

  // `receive` and `idle` are meant to be overridden.
  protected void receive (Object memo) {
    System.out.println(this + " received " + memo);
  }

  // Default `idle` method, just sleeps.
  protected void idle () {
    try { sleep(1000); } catch (Exception e) {} 
  }

  public final void run () {
    running = true;
    while (running) {
      Object memo = queue.poll();       // If there's a message, process it.
      if (memo != null) receive(memo);
                                        // If the queue is empty, call the idle
      if (queue.size() == 0) idle();    // function.
    }
  }

  // Sets `running` to false and stops the thread.
  public void shutdown () {
    running = false;
  }
}
