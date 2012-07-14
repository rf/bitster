package libbitster;

import java.nio.charset.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Util {
  public static ByteBuffer s (String input) {
    try {
      Charset utf8 = Charset.forName("UTF-8");
      byte[] ret = input.getBytes(utf8);
      return ByteBuffer.wrap(ret);
    } catch (UnsupportedCharsetException e) {
      throw new RuntimeException("Your jvm doesn't support UTF-8, which is impossible");
    }
  }

  public static String buff2str (ByteBuffer input) {
    int oldpos = input.position();
    StringBuilder ss = new StringBuilder();
    for (int i = 0; i < input.array().length; i++) 
      ss.append((char) input.array()[i]);
    return ss.toString();
  }

  public static void setTimeout (int timeout, Memo memo) {
    Timeout.set(timeout, memo);
  }

  public static void shutdown () {
    Timeout.off();
  }
}

// A class for scheduling memos to be delivered back to the sender after a 
// specified interval. Singleton.

class Timeout extends Actor {
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
