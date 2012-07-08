import java.nio.*;
import java.net.*;
import java.util.*;

import java.nio.channels.*;

class Protocol {
  private String state; // states:
  // 'init': just created, waiting to establish a connection
  // 'error': error occured, exception property will be populated
  // 'normal': operating normally (may add more such states later)

  private InetAddress host;
  private int port;

  private LinkedList<Message> inbox;  // messages received from the client
  private LinkedList<Message> outbox; // messages being sent to the client

  private SocketChannel channel;      // select() abstraction garbage
  private Selector selector;

  public Exception exception;         // set to an exception if one occurs

  // big buffer
  ByteBuffer readBuffer = ByteBuffer.allocateDirect(32000);
  private int numRead;

  ByteBuffer writeBuffer = ByteBuffer.allocateDirect(32000);
  private int numWritten;

  public Protocol (InetAddress host, int port) {
    this.host = host;
    this.port = port;
    this.state = "init";
  }

  // select() on sockets, call talk() or listen() to perform io if necessary
  public void communicate () {
    if (this.state == "init") this.establish();
    else if (this.state != "error") {
      try {

        // Select on the socket. "Is there stuff to do?"
        if (this.selector.select(0) == 0) return; // nothing to do
        Iterator<SelectionKey> keys = this.selector.selectedKeys().iterator();

        while (keys.hasNext()) {
          SelectionKey key = keys.next();
          keys.remove();
          if (!key.isValid()) continue; // WHY
          if (key.isReadable()) this.listen(); // call talk if we can talk
          if (key.isWritable()) this.talk();   // call listen if we can listen
        }

      } catch (Exception e) { this.error(e); }
    }
  }

  // handle errors
  private void error (Exception e) {
    e.printStackTrace();
    this.state = "error";
    this.exception = e;
  }

  // Establish the connection
  public void establish () {
    try {
      channel = SocketChannel.open(new InetSocketAddress(host, port));
      channel.configureBlocking(false);
      channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    } catch (Exception e) {
      this.state = "error";
      this.exception = e;
    }
  }
  
  // Send some data to the peer
  private void talk () {
    // try to write some bytes to the peer
    // record the number of bytes sent
    // if we sent the whole message
      // pop it off of the queue
      // serialize the next message to be sent
  }

  // Read data from the peer
  private void listen () {
    // try to read some bytes from the peer
    // record the number of bytes received
    // if we received a whole message
      // de-serialize it
      // push it onto the queue
  }

  // called by the Broker to send messages
  public void send (Message message) {
    outbox.offer(message);
  }

  // called by the Broker to receive messages
  public Message receive () {
    if (inbox.size() > 0) return inbox.poll();
    else return null;
  }
}
