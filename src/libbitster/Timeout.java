package libbitster;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Timeout extends Actor {
  private int last = 0;

  private ConcurrentLinkedQueue<TimeoutInfo> timeouts;

  private static Timeout instance = null;

  protected Timeout () {
    timeouts = new ConcurrentLinkedQueue<TimeoutInfo>();
    start();
  }

  protected void idle () {
    try { Thread.sleep(1000); } catch (Exception e) {}

    int curr = (int) Calendar.getInstance().getTimeInMillis();

    if (last != 0) {
      int elapsed = curr - last;

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

  public static synchronized void set (int timeout, Memo memo) {
    if (instance == null) instance = new Timeout();
    instance.setTimeout(timeout, memo);
  }
}

class TimeoutInfo {
  public int time;
  public Memo payload;
}
