import java.nio.*;
import java.nio.channels.*;
import java.nio.channels.Pipe.*;
import java.io.*;
import java.util.*; 
import java.util.logging.*; 

/** A reactor that selects on some stuff and then notifies some Communicators
 *  that things happened */

public class Overlord {
  private Selector selector;
  private final static Logger log = Logger.getLogger("Overlord");

  // This is just used to read the one byte off of pipes informing us that
  // there is data on some queue.
  ByteBuffer ignored = ByteBuffer.allocate(1);

  public Overlord () {
    try {
      selector = Selector.open();
    } catch (IOException e) { throw new RuntimeException("select() failed"); }
  }

  /** Selects on sockets and informs their Communicator when there is something
   *  to do. */
  public void communicate (int timeout) {

    try {
      if (selector.select(timeout) == 0) return; // nothing to do
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
      if (key.isReadable()) {

        if (key.channel() instanceof SourceChannel) {
          // It's a pipe, so we should read the byte off and notify the
          // Communicator that there's a memo.
          SourceChannel pipeSource = (SourceChannel) key.channel();
          ignored.clear();
          try { pipeSource.read(ignored); } catch (IOException e) { /*?*/ }

          communicator.onMemo();
        }
        
        else {
          communicator.onReadable();
        }
      }

      if (key.isWritable())   communicator.onWritable();
      if (key.isAcceptable()) communicator.onAcceptable();
    }
  }

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

  /** Registers a SelectableQueue */
  public boolean register (SelectableQueue sq, Communicator communicator) {
    try {
      // Make a new pipe and configure it for non blocking mode
      Pipe p = Pipe.open();
      p.sink().configureBlocking(false);
      p.source().configureBlocking(false);

      // Register the new pipe with the queue. It will write a byte to this
      // pipe when the queue is hot.
      sq.register(p.sink());

      // Register the new pipe to be selected on.
      p.source().register(
        selector,
        SelectionKey.OP_READ,
        communicator
      );

      return true;
    }

    catch (Exception e) { e.printStackTrace(); return false; }
  }
}
