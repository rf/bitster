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
  private int numRead = 0;
  private int length = -1;

  ByteBuffer writeBuffer;
  private int numWritten = 0;

  private boolean handshakeSent = false;
  private boolean handshakeReceived = false;

  public Protocol (InetAddress host, int port) {
    this.host = host;
    this.port = port;
    this.state = "init";
  }

  // select() on sockets, call talk() or listen() to perform io if necessary
  public void communicate () {
    if (state == "init") establish();
    else if (state != "error") {
      try {

        // Select on the socket. "Is there stuff to do?"
        if (selector.select(0) == 0) return; // nothing to do
        Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

        while (keys.hasNext()) {
          SelectionKey key = keys.next();
          keys.remove();
          if (!key.isValid()) continue;   // WHY
          if (key.isReadable() && !handshakeReceived) listenHandshake();
          else if (key.isReadable()) listen(); // call listen if we can listen
          if (key.isWritable()) talk();        // call talk if we can talk
        }

      } catch (Exception e) { error(e); }
    }
  }

  // handle errors
  private void error (Exception e) {
    e.printStackTrace();
    state = "error";
    exception = e;
  }

  // Establish the connection
  public void establish () {
    try {
      channel = SocketChannel.open(new InetSocketAddress(host, port));
      channel.configureBlocking(false);
      channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
      state = "handshake";
      length = 68;  // handshake message is 68 bytes long
    } catch (Exception e) { error(e); }
  }

  // ## talk
  // Send some data to the peer
  private void talk () {
    try {
      // If we dont have a message in the writeBuffer, populate the writeBuffer
      if (writeBuffer == null && outbox.size() > 0)
        writeBuffer = outbox.poll().serialize();
      else return; // we have nothing to say

      numWritten += channel.write(writeBuffer); // try to write some bytes 
      writeBuffer.position(numWritten);         // set the buffer's new pos

      // If we sent the whole message, clear the buffer.
      if (writeBuffer.remaining() == 0) writeBuffer = null;
    } catch (Exception e) { error(e); }
  }

  // ## listen
  // Read data from the peer
  private void listen () {
    try {
      if (length == readBuffer.position()) {  // if we received a whole message
        inbox.offer(new Message(readBuffer)); // de-serialize it
        readBuffer.clear();                   // push it onto the queue
        length = -1;
      }

      numRead += channel.read(readBuffer); // try to read some bytes from peer
      readBuffer.position(numRead);        // advance buffer

      if (numRead >= 4 && state != "handshake") 
        length = readBuffer.getInt(0); // grab length from msg

    } catch (Exception e) { error(e); }
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
