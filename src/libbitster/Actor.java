package libbitster;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Simple Actor class capable of sending and receiving `Memo`s. Can be run in
 * its own thread.
 * @author Russ Frank
 */
public class Actor extends Beacon implements Runnable {
  protected ConcurrentLinkedQueue<Memo> queue;
  private boolean running;
  private Thread thread = null;

  public Actor () {
    super();
    queue = new ConcurrentLinkedQueue<Memo>();
  }

  /** `post`s a message to this actor.*/
  public void post (Memo memo) {
    queue.offer(memo);
  }

  /** `receive` and `idle` are meant to be overridden.*/
  protected void receive (Memo memo) {
    System.out.println(this + " received " + memo);
  }

  /** Default `idle` method, just sleeps. */
  protected void idle () {
    try { Thread.sleep(1000); } catch (Exception e) {} 
  }

  public void tick () {
    Memo memo = queue.poll();       // If there's a message, process it.
    if (memo != null) receive(memo);
                                      // If the queue is empty, call the idle
    if (queue.size() == 0) idle();    // function.
  }

  public final void run () {
    running = true;
    while (running) tick();
  }

  public synchronized void start () {
    if (thread == null) {
      thread = new Thread(this);
      thread.start();
    }
  }

  /** Sets `running` to false and stops the thread. */
  public void shutdown () {
    running = false;
  }
}
