package libbitster;

import java.nio.*;
import java.nio.channels.*;
import java.io.*;
import java.util.*; 
import java.util.logging.*; 

public class Overlord {
  private Selector selector;
  private final static Logger log = Logger.getLogger("Overlord");

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
      if (key.isReadable())   communicator.onReadable();
      if (key.isWritable())   communicator.onWritable();
      if (key.isAcceptable()) communicator.onAcceptable();
    }
  }

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
}
