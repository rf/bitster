package libbitster;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

// A class for scheduling memos to be delivered back to the sender after a 
// specified interval. Singleton.

public class Timeout extends Actor {
  private int last = 0;

  private ConcurrentLinkedQueue<TimeoutInfo> timeouts;

  private static Timeout instance = null;

  protected Timeout () {
    timeouts = new ConcurrentLinkedQueue<TimeoutInfo>();
    start();
  }

  protected void idle () {
    try { Thread.sleep(100); } catch (Exception e) {}

    int curr = (int) Calendar.getInstance().getTimeInMillis();

    if (last != 0) {
      int elapsed = curr - last;

      // Loop over each timeout we know about and reduce its time by the 
      // elapsed time.  When it's up, post the memo back to the sender.

      Iterator<TimeoutInfo> i = timeouts.iterator();
      while (i.hasNext()) {
        TimeoutInfo timeout = i.next();
        timeout.time -= elapsed;
        if (timeout.time < 0) {
          i.remove();
          timeout.payload.getSender().post(timeout.payload);
        }
      }
    }

    last = curr;
  }

  protected void setTimeout (int timeout, Memo memo) {
    TimeoutInfo info = new TimeoutInfo();
    info.time = timeout;
    info.payload = memo;
    timeouts.add(info);
  }

  // ## set
  // Set a timeout.  Takes a timeout and memo as arguments.
  public static synchronized void set (int timeout, Memo memo) {
    if (instance == null) instance = new Timeout();
    instance.setTimeout(timeout, memo);
  }

  public static void off () {
    if (instance != null) instance.shutdown();
  }
}

class TimeoutInfo {
  public int time;
  public Memo payload;
}
