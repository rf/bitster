import java.nio.*;
import java.nio.channels.*;
import java.nio.channels.Pipe.*;
import java.io.*;
import java.util.*; 
import java.util.logging.*; 
import java.util.concurrent.*;

/** A reactor that selects on some stuff and then notifies some Communicators
 *  that things happened */

public class Overlord {
  private Selector selector;
  private Pipe pipe;
  private final static Logger log = Logger.getLogger("Overlord");
  private ConcurrentLinkedQueue<Communicator> queue;

  // This is just used to read the one byte off of pipes informing us that
  // there is data on some queue.
  ByteBuffer ignored = ByteBuffer.allocate(10);

  public Overlord () {
    try {
      selector = Selector.open();
      queue = new ConcurrentLinkedQueue<Communicator>();

      // open the pipe and register it with our selector
      pipe = Pipe.open();
      pipe.sink().configureBlocking(false);
      pipe.source().configureBlocking(false);
      pipe.source().register(selector, SelectionKey.OP_READ);
    } catch (IOException e) { throw new RuntimeException("select() failed"); }
  }

  /** Selects on sockets and informs their Communicator when there is something
   *  to do. */
  public void communicate (int timeout) {

    try {
      selector.select(timeout);
    } catch (IOException e) {
      // Not really sure why/when this happens yet
      return;
    }

    Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

    while (keys.hasNext()) {
      SelectionKey key = keys.next();
      keys.remove();
      if (!key.isValid()) continue;      // WHY
      Communicator communicator = (Communicator) key.attachment();

      if (key.isReadable())   communicator.onReadable();
      if (key.isWritable())   communicator.onWritable();
      if (key.isAcceptable()) communicator.onAcceptable();
    }

    // Go through the queue and handle each communicator
    while (!queue.isEmpty()) {
      Communicator c = queue.poll();
      c.onMemo();
    }
  }
  
  public void offer (Communicator c) { queue.offer(c); }

  /** Registers a SelectableChannel */
  public boolean register (SelectableChannel sc, Communicator communicator) {
    try {
      sc.register(
        selector, 
        sc.validOps(),
        communicator
      );

      return true;
    } catch (Exception e) { return false; }
  }

  /** If the selector is waiting, wake it up */
  public void interrupt () { selector.wakeup(); }

  /** Registers a SelectableQueue */
  public boolean register (SelectableQueue sq, Communicator communicator) {
    try {
      // Register the new pipe with the queue. It will write a byte to this
      // pipe when the queue is hot, and it will offer its communicator to our
      // queue.
      sq.register(this, communicator);

      return true;
    }

    catch (Exception e) { e.printStackTrace(); return false; }
  }
}
