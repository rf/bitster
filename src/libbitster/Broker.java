package libbitster;

import java.net.*;
import java.util.*;
import java.util.logging.*;

import java.nio.*;
import java.nio.channels.*;

// The `Broker` class manages a connection with a peer.  It uses the
// `Protocol` class for the actual communication.  It accepts the following
// memos:
//
//  * `message`: payload is a message to be delivered to the peer
//  * `kepalive`: send a keepalive to the peer. This is scheduled with Timeout.
//
// author: Russ Frank

public class Broker extends Actor {
  private String state;
  // * `normal`   - communicating normally
  // * `check`    - peer needs to be checked to see if we're already connected
  // * `error`    - error has occurred

  public Exception exception;

  private Protocol peer;
  private Manager manager;

  // Choked and interesting refer to the peer's opinion of ous:
  private boolean choked = true;       // We are choked by the peer
  private boolean interesting = false; // We are not interesting to the peer.

  // Choking and interested are our opinions of the peer:
  private boolean choking = true;      // We are choking this peer
  private boolean interested = false;  // We are not interested in the peer

  private BitSet pieces;

  private int numReceived = 0; // # of recvd messages
  private int numQueued = 0;

  private LinkedList<Message> outbox;

  private final static Logger log = Logger.getLogger("Broker");

  public Broker (SocketChannel sc, Manager manager) {
    super();
    log.info("Broker: accepting");

    outbox = new LinkedList<Message>();
    peer = new Protocol(sc, manager.getInfoHash(), manager.getPeerId());
    this.manager = manager;
    state = "check";
    Util.setTimeout(120000, new Memo("keepalive", null, this));
  }

  public Broker (InetAddress host, int port, Manager manager) {
    super();
    log.info("Broker init for host: " + host);

    outbox = new LinkedList<Message>();

    peer = new Protocol(
      host, 
      port, 
      manager.getInfoHash(),
      manager.getPeerId()
    );

    this.manager = manager;
    state = "normal";
    Util.setTimeout(120000, new Memo("keepalive", null, this));
  }

  // ## receive
  // Receive a memo

  protected void receive (Memo memo) {
    if ("request".equals( memo.getType() )) {
      numQueued += 1;
      if (choked) {
        log.info("We're choked, queuing message");
        outbox.add((Message) memo.getPayload());
      } 
      
      else {
        log.info("Sending " + (Message) memo.getPayload());
        peer.send((Message) memo.getPayload());
      }
    }

    else if ("keepalive".equals(memo.getType()) && state.equals("normal")) {
      log.info("Sending keep alive");
      peer.send(Message.createKeepAlive());
      Util.setTimeout(120000, new Memo("keepalive", null, this));
    }

    else if ("have".equals(memo.getType())) {
      if (peer.getState().equals("normal")) {
        Piece p = (Piece) memo.getPayload();
        peer.send(Message.createHave(p.getNumber()));
        log.info("Informing peer " + Util.buff2str(peer.getPeerId()) + 
          " that we have piece " + p.getNumber());
      } else log.info("Peer not connected, not sending have.");
    }
  }

  private void error (Exception e) {
    state = "error";
    exception = e;
    peer.close();
  }

  public void close () { peer.close(); }

  // ## listen
  // Receive a message via tcp
  private void message (Message message) {
    if (numReceived > 0 && message.getType() == Message.BITFIELD) 
      error(new Exception("protocol error"));
    numReceived += 1;

    log.info(message.toString());

    switch (message.getType()) {

      // Handle basic messages
      case Message.CHOKE:          choked = true;                       break;
      case Message.UNCHOKE:        choked = false;                      break;
      case Message.INTERESTED:     interesting = true;                  break;
      case Message.NOT_INTERESTED: interesting = false;                 break;
      case Message.BITFIELD:       
        pieces = message.getBitfield();   
        checkInterested();
      break;

      case Message.HAVE:
        if (pieces == null) pieces = new BitSet();
        pieces.set(message.getIndex());
        checkInterested();
      break;

      // Send pieces to our `Manager`.
      case Message.PIECE:
        numQueued -= 1;
        manager.post(new Memo("block", message, this));
      break;

      // TODO: Handle Message.REQUEST
    }
  }

  protected void idle () {
    peer.communicate(); // pump that shit
    Message m;
    while ((m = peer.receive()) != null) message(m); // grab any available msgs

    if (state.equals("check") && peer.getPeerId() != null) {
      if (!manager.addPeer(peer.getPeerId(), this)) {
        // Peer has already been added
        log.severe("Dropping duplicate connection to " + 
          Util.buff2str(peer.getPeerId()));
        error(new Exception("duplicate"));
      } else {
        state = "normal";
      }
    }

    if (peer.getState().equals("error")) {
      if (state != "error") { // we haven't displayed the error msg yet
        log.warning("Peer " + Util.buff2str(peer.getPeerId()) + " protocol " +
          "error: " + peer.exception);
      }
      state = "error";
    }

    if (outbox.size() > 0 && !choked) {
      log.info("We're unchoked and there are messages in the queue, flushing");
      Iterator<Message> i = outbox.iterator();
      while (i.hasNext()) {
        Message msg = i.next();
        log.info("Sending " + msg);
        peer.send(msg);
        i.remove();
      }
    } 
  }

  private void checkInterested () {
    if (manager.isInteresting(pieces)) {
      log.info("We are interested in the peer");
      interested = true;
      choking = false;
      peer.send(Message.createUnchoke());
      peer.send(Message.createInterested());
    }
  }

  // Accessors.
  public boolean choked () { return choked; }
  public boolean choking () { return choking; }
  public boolean interested () { return interested; }
  public boolean interesting () { return interesting; }
  public String state () { return state; }
  public int numQueued () { return numQueued; }
  public ByteBuffer peerId () { return peer.getPeerId(); }
}
