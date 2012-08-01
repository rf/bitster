package libbitster;

import java.nio.*; 
import java.net.*; 
import java.util.*; 
import java.nio.channels.*;

/** Handles communication with a peer.  Polls the socket, then writes and reads
 * if necessary.
 * @author: Russ Frank
 */
public class Protocol implements Communicator {
  private String state; // states:
  // 'init': just created, waiting to establish a connection
  // 'error': error occured, exception property will be populated
  // 'handshake': waiting for handshake message
  // 'connect': waiting for connect
  // 'normal': operating normally (may add more such states later)

  private InetAddress host;
  private int port;

  private LinkedList<Message> inbox;  // messages received from the client
  private LinkedList<Message> outbox; // messages being sent to the client

  private SocketChannel channel;      // select() abstraction garbage
  private Overlord overlord;

  public Exception exception;         // set to an exception if one occurs

  // big buffer
  ByteBuffer readBuffer = ByteBuffer.allocate(128000);
  private int numRead = 0;
  private int length = -1;

  ByteBuffer writeBuffer = null;
  private int numWritten = 0;

  private ByteBuffer infoHash;
  private ByteBuffer theirPeerId;
  private ByteBuffer ourPeerId;

  public Protocol (
    InetAddress host, 
    int port, 
    ByteBuffer infoHash, 
    ByteBuffer peerId,   // our peer id
    Overlord overlord
  ) {
    this.overlord = overlord;
    this.host = host;
    this.port = port;
    this.ourPeerId = peerId;
    this.infoHash = infoHash;
    this.state = "init";
    this.outbox = new LinkedList<Message>();
    this.inbox = new LinkedList<Message>();
  }

  public Protocol (
    SocketChannel sc, 
    ByteBuffer infoHash, 
    ByteBuffer peerId,
    Overlord overlord
  ) {
    this.overlord = overlord;
    this.ourPeerId = peerId;
    this.infoHash = infoHash;
    this.state = "init";
    this.outbox = new LinkedList<Message>();
    this.inbox = new LinkedList<Message>();
    this.channel = sc;
    try {
      this.port = ((InetSocketAddress) sc.getRemoteAddress()).getPort();
      this.host = ((InetSocketAddress) sc.getRemoteAddress()).getAddress();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /** handle errors */
  private void error (Exception e) {
    state = "error";
    exception = e;
    close();
  }

  public void close () {
    try { channel.close(); } catch (Exception e2) {} // close socket
  }

  /** Establish the connection */
  public void establish () {
    // Setup handshake
    ByteBuffer handshake = Handshake.create(infoHash, ourPeerId);
    writeBuffer = handshake;

    try {
      state = "handshake";

      // If there is no connection already (ie, we're making an outgoing
      // connection), we need to start a connection.
      if (channel == null) {
        channel = SocketChannel.open();
        channel.configureBlocking(false);

        // If the connection is established immediately, we go into the
        // handshake state
        if (channel.connect(new InetSocketAddress(host, port)))
          state = "handshake";
        else // Otherwise, we wait for the connection to happen
          state = "connect";
      }
      
      // Register this object for events on the channel with the overlord.
      if (overlord.register(channel, this) == false)
        throw new Exception("selector registration failed");

    } catch (Exception e) { error(e); }
  }

  public boolean onConnectable () {
    try {
      if (!channel.finishConnect()) {
        throw new Exception("connect failed");
      }
      state = "handshake";
      return true;
    } catch (Exception e) { error(e); return false; }
  }

  /** Send some data to the peer */
  public boolean onWritable () {
    try {
      // If we don't have a message in the writeBuffer, populate the writeBuffer
      if (writeBuffer == null && outbox.size() > 0)
        writeBuffer = outbox.poll().serialize();

      // If writeBuffer is still not populated, we have nothing to say
      if (writeBuffer == null) return true;

      numWritten += channel.write(writeBuffer); // try to write some bytes 
      writeBuffer.position(numWritten);         // set the buffer's new pos

      // If we sent the whole message, clear the buffer.
      if (writeBuffer.remaining() == 0) {
        writeBuffer = null;
        numWritten = 0;
      }

      return true;

    } catch (Exception e) { error(e); }
    return false;
  }

  // ## listen
  // Read data from the peer
  public boolean onReadable () {
    try {
      numRead += channel.read(readBuffer); // try to read some bytes from peer

      // EOF
      if (numRead == -1) throw new Exception("eof");
      readBuffer.position(numRead);        // advance buffer

      do { // Parse out messages while there are still messages to parse

        // If we have more data than the length of the message we're expecting,
        // parse messages out of the readBuffer.
        if (numRead >= length && length >= 0) parse();

        // Try to find the length of the message in the read buffer
        findLength();

      } while (numRead >= length && length >= 0);

      return true;

    } catch (Exception e) { error(e); }
    return false;
  }

  public boolean onAcceptable () { return false; }

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
        Log.i("Handshake successful, peer id: " + Util.buff2str(theirPeerId));
        state = "normal";
      } catch (Exception e) { error(e); }
    } else inbox.offer(new Message(readBuffer)); 

    // Amount of next message which has already been read.
    int nextMsgRead = numRead - length;

    // Copy the bits at the end of the message we just parsed to the beginning
    // of the read buffer.

    // A possible optimization here would be to use a ring buffer.

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
  public void findLength () throws Exception {
    // If we've read at least four bytes, we haven't gotten a length yet,
    // and we're not reading a handshake message, then grab the length out of
    // the readBuffer.
    if (length == -1 && numRead >= 4 && !state.equals("handshake"))
      // add 4 to account for the length of the integer specifying the length
      length = readBuffer.getInt(0) + 4;

    // If we expect a handshake and we don't have a length yet,
    else if (length == -1 && numRead >= 1 && state.equals("handshake")) 
      // `length` here is actually just the length of the protocol identifier
      // string.  We need to add 49 to account for the rest of the message.
      length = ((int) readBuffer.get(0)) + 49;

    // 32000 is arbitrary max message size
    if ((length < 0 || length > 32000) && length != -1) {  
      Log.error("Got invalid message length from peer: " + length);
      throw new Exception("invalid message length");
    }
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

  public String getState () { return state; }
  public ByteBuffer getPeerId () { return theirPeerId; }
  public String getAddress() { return host.getHostAddress() + ":" + port; }

  public String toString () {
    return "Protocol, state: " + state + " curr recv msg len: " + length + 
      " numRead: " + numRead + " numWritten: " + numWritten + " peerid: " + theirPeerId;
  }
}
