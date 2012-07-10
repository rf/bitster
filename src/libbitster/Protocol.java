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

  ByteBuffer writeBuffer;
  private int numWritten = 0;

  private boolean handshakeSent = false;
  private boolean handshakeReceived = false;

  private ByteBuffer infoHash;
  private ByteBuffer peerId;

  public Protocol (InetAddress host, int port, ByteBuffer infoHash) {
    this.host = host;
    this.port = port;
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
      numRead += channel.read(readBuffer); // try to read some bytes from peer
      readBuffer.position(numRead);        // advance buffer

      // If we've read at least four bytes, we haven't gotten a length yet,
      // and we're not reading a handshake message, then grab the length out of
      // the readBuffer.
      if (length == -1 && numRead >= 4 && state != "handshake") 
        length = readBuffer.getInt(0); // grab length from msg
      else  // Otherwise it's a handshake
        length = ((int) readBuffer.get(0)) + 49;

      if (length == numRead) {                      // if we got a whole message
        readBuffer.position(0);                     // reset pos for parsing
        if (state == "handshake") parseHandshake(); // parse and handle it
        else inbox.offer(new Message(readBuffer)); 
        readBuffer.clear();                         // reset state
        length = -1;
        numRead = 0;
      }

    } catch (Exception e) { error(e); }
  }

  private boolean bufferEquals (ByteBuffer a, ByteBuffer b, int num) {
    for (int i = 0; i < num; i++) if (a.get() != b.get()) return false;
    return true;
  }

  private void parseHandshake () {
    byte[] bytes = new byte[68];
    readBuffer.get(bytes, 0, 68);  // Copy the handshake out of the readBuffer
    ByteBuffer handshake = ByteBuffer.wrap(bytes);

    ByteBuffer id = (ByteBuffer) handshake.slice().position(1).limit(20);
    // TODO: do the string stuff properly
    ByteBuffer correctid = 
      ByteBuffer.wrap(new String("BitTorrent Protocol").getBytes());

    if (!bufferEquals(id, correctid, 19)) {         // verify the protocol id
      error(new Exception("handshake"));
      return;
    } 

    handshake.position(0);
    ByteBuffer receivedInfoHash =  // Parse out the info hash
      (ByteBuffer) handshake.slice().position(28).limit(48);

    if (!bufferEquals(receivedInfoHash, infoHash, 20)) { // verify the info hash
      error(new Exception("handshake"));
      return;
    }

    infoHash.position(0);

    handshake.position(0);
    peerId = (ByteBuffer) handshake.slice().position(48).limit(68);
    state = "normal";
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
      " numRead: " + numRead + " numWritten: " + numWritten + " peerid: " + peerId;
  }

  public static void main (String[] args) {
    try {
      ByteBuffer infohash = ByteBuffer.wrap(new String("asdfasdfasdfasdfasdf").getBytes());
      Protocol p = new Protocol(InetAddress.getByName("localhost"), 4000, infohash);

      while (true) {
        p.communicate();
        System.out.println(p);
        Message m = p.receive();
        if (m != null) System.out.println(m);
        Thread.sleep(100);
      }

    } catch (Exception e) { e.printStackTrace(); return; }
  }
}
