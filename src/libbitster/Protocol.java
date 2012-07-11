package libbitster;

import java.nio.*; 
import java.net.*; 
import java.util.*; 
import java.nio.channels.*;

class Protocol {
  private String state; // states:
  // 'init': just created, waiting to establish a connection
  // 'error': error occured, exception property will be populated
  // 'handshake': waiting for handshake message
  // 'normal': operating normally (may add more such states later)

  private InetAddress host;
  private int port;

  private LinkedList<Message> inbox;  // messages received from the client
  private LinkedList<Message> outbox; // messages being sent to the client

  private SocketChannel channel;      // select() abstraction garbage
  private Selector selector;

  public Exception exception;         // set to an exception if one occurs

  // big buffer
  ByteBuffer readBuffer = ByteBuffer.allocate(32000);
  private int numRead = 0;
  private int length = -1;

  ByteBuffer writeBuffer = null;
  private int numWritten = 0;

  private boolean handshakeSent = false;
  private boolean handshakeReceived = false;

  private ByteBuffer infoHash;
  private ByteBuffer theirPeerId;
  private ByteBuffer ourPeerId;

  public Protocol (
    InetAddress host, 
    int port, 
    ByteBuffer infoHash, 
    ByteBuffer peerId       // our peer id
  ) {
    this.host = host;
    this.port = port;
    this.ourPeerId = peerId;
    this.infoHash = infoHash;
    this.state = "init";
    this.outbox = new LinkedList<Message>();
    this.inbox = new LinkedList<Message>();
    try { this.selector = Selector.open(); } catch (Exception e) { error(e); }
  }

  // select() on sockets, call talk() or listen() to perform io if necessary
  public void communicate () {
    if (state != "error") {
      try {
        if (state == "init") establish();

        // Select on the socket. "Is there stuff to do?"
        if (selector.select(0) == 0) return; // nothing to do
        Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

        while (keys.hasNext()) {
          SelectionKey key = keys.next();
          keys.remove();
          if (!key.isValid())   continue;      // WHY
          if (key.isReadable()) listen();      // call listen if we can listen
          if (key.isWritable()) talk();        // call talk if we can talk
        }

        // If we have more data than the length of the message we're expecting,
        // parse messages out of the readBuffer.
        if (numRead >= length && length != -1) parse();

        // Try to find the length of the message in the read buffer
        findLength();

      } catch (Exception e) { error(e); }
    }
  }

  // handle errors
  private void error (Exception e) {
    e.printStackTrace();
    state = "error";
    exception = e;

    try { channel.close(); } catch (Exception e2) {} // close socket
  }

  // Establish the connection
  public void establish () {
    try {
      channel = SocketChannel.open(new InetSocketAddress(host, port));
      channel.configureBlocking(false);
      channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

      ByteBuffer handshake = Handshake.create(infoHash, ourPeerId);
      writeBuffer = handshake;

      state = "handshake";
    } catch (Exception e) { error(e); }
  }

  // ## talk
  // Send some data to the peer
  private void talk () {
    try {
      // If we dont have a message in the writeBuffer, populate the writeBuffer
      if (writeBuffer == null && outbox.size() > 0)
        writeBuffer = outbox.poll().serialize();

      // If writeBuffer is still not populated, we have nothing to say
      if (writeBuffer == null) return;

      numWritten += channel.write(writeBuffer); // try to write some bytes 
      writeBuffer.position(numWritten);         // set the buffer's new pos

      // If we sent the whole message, clear the buffer.
      if (writeBuffer.remaining() == 0) {
        writeBuffer = null;
        numWritten = 0;
      }
    } catch (Exception e) { error(e); }
  }

  // ## listen
  // Read data from the peer
  private void listen () {
    try {
      numRead += channel.read(readBuffer); // try to read some bytes from peer
      readBuffer.position(numRead);        // advance buffer

      // Messages parsing is actually in the communicate loop.  We call `parse`
      // if we've read 'enough' data to have a whole message.

    } catch (Exception e) { error(e); }
  }

  // ## parse
  // Parse the message and reset the state of the listen logic.
  private void parse () {
    readBuffer.position(0);
    if (state == "handshake") {
      try {
        byte[] bytes = new byte[length];
        // Copy the handshake out of the readBuffer
        readBuffer.get(bytes, 0, length);  
        ByteBuffer handshake = ByteBuffer.wrap(bytes);
        theirPeerId = Handshake.verify(infoHash, handshake);
        state = "normal";
      } catch (Exception e) { error(e); }
    } else inbox.offer(new Message(readBuffer)); 

    // Amount of next message which has already been read.
    int nextMsgRead = numRead - length;

    // Copy the bits at the end of the message we just parsed to the beginning
    // of the read buffer.

    // A possible optimizaion here would be to use a ring buffer.

    byte[] nextMsgPart = new byte[nextMsgRead];
    readBuffer.position(length);
    readBuffer.get(nextMsgPart, 0, nextMsgRead);

    readBuffer.position(0);
    readBuffer.put(nextMsgPart);

    length = -1;
    numRead = nextMsgRead;
  }

  // ## findLength
  // Grab the length of the next message out of the read buffer if possible
  public void findLength () {
    // If we've read at least four bytes, we haven't gotten a length yet,
    // and we're not reading a handshake message, then grab the length out of
    // the readBuffer.
    if (length == -1 && numRead >= 4 && state != "handshake") 
      // add 4 to account for the length of the integer specifying the length
      length = readBuffer.getInt(0) + 4;

    // If we expect a handshake and we don't have a length yet,
    else if (length == -1 && numRead >= 1 && state == "handshake") 
      // `length` here is actually just the length of the protocol identifier
      // string.  We need to add 49 to account for the rest of the message.
      length = ((int) readBuffer.get(0)) + 49;
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

  public String toString () {
    return "Protocol, state: " + state + " curr recv msg len: " + length + 
      " numRead: " + numRead + " numWritten: " + numWritten + " peerid: " + theirPeerId;
  }

  public static void main (String[] args) {
    try {
      ByteBuffer infohash = ByteBuffer.wrap(new String("asdfasdfasdfasdfasdf").getBytes());
      ByteBuffer ourPeerId = ByteBuffer.wrap(new String("asdfasdfasdfasdfasdf").getBytes());
      Protocol p = new Protocol(InetAddress.getByName("localhost"), 4000, infohash, ourPeerId);

      while (true) {
        p.communicate();
        System.out.println(p);
        Message m = p.receive();
        while (m != null) {
          System.out.println("message received: " + m);
          m = p.receive();
        }
        Thread.sleep(100);
      }

    } catch (Exception e) { e.printStackTrace(); return; }
  }
}
