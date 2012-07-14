package libbitster;

import java.net.*;
import java.util.*;
import java.util.logging.*;

// The `Broker` class manages a connection with a peer.  It uses the
// `Protocol` class for the actual communication.  It accepts the following
// memos:
//
//  * `message`: payload is a message to be delivered to the peer

public class Broker extends Actor {
  private String state;
  public Exception exception;

  private Protocol peer;
  private Manager manager;

  // Choked and interesting refer to the local state:
  private boolean choked;      // We are choked by the peer.
  private boolean interesting; // We are interesting to the peer.

  // Choking and interested refer to the remote state:
  private boolean choking;     // We are choking this peer.
  private boolean interested;  // We are interested in the peer.

  private BitSet pieces;

  private int numReceived = 0; // # of recvd messages

  private final static Logger log = Logger.getLogger("Broker");

  public Broker (InetAddress host, int port, Manager manager) {
    super();
    log.setLevel(Level.FINEST);
    log.info("Broker init for host: " + host);

    peer = new Protocol(
      host, 
      port, 
      manager.getInfoHash(),
      manager.getPeerId()
    );

    this.manager = manager;

    // When we start..
    choked = true;        // We assume we are choked by the peer.
    interesting = false;  // We assume we are not interesting to the peer.

    choking = true;       // We are choking the peer.
    interested = false;   // We are not interested in the peer.

    state = "normal";

    Util.setTimeout(120000, new Memo("keepalive", null, this));
  }

  // ## receive
  // Receive a memo
  protected void receive (Memo memo) {
    if (memo.getType() == "message") {
      peer.send((Message) memo.getPayload());
    }

    else if (memo.getType() == "keepalive") {
      peer.send(Message.createKeepAlive());
      Util.setTimeout(120000, new Memo("keepalive", null, this));
    }
  }

  private void error (Exception e) {
    state = "error";
    exception = e;
    peer.close();
  }

  // ## listen
  // Receive a message via tcp
  private void message (Message message) {
    if (numReceived > 0 && message.getType() == Message.BITFIELD) 
      error(new Exception("protocol error"));
    numReceived += 1;

    switch (message.getType()) {

      // Handle basic messages
      case Message.CHOKE:          choked = true;                    break;
      case Message.UNCHOKE:        choked = false;                   break;
      case Message.INTERESTED:     interesting = true;               break;
      case Message.NOT_INTERESTED: interesting = false;              break;
      case Message.BITFIELD:       pieces = message.getBitfield();   break;

      case Message.HAVE:
        if (pieces == null) pieces = new BitSet();
        pieces.flip(message.getIndex());
      break;

      // Send pieces to our `Manager`.
      case Message.PIECE:
        manager.post(new Memo("block", message, this));
      break;

      // TODO: Handle Message.REQUEST
    }
  }

  protected void idle () {
    peer.communicate(); // pump that shit
    Message m;
    while ((m = peer.receive()) != null) message(m); // grab any available msgs

    if (peer.getState() == "error") {
      if (state != "error") { // we haven't displayed the error msg yet
        log.warning("Peer " + Util.buff2str(peer.getPeerId()) + " protocol " +
          "error: " + peer.exception);
      }
      state = "error";
    }
  }

  // Accessors.
  public boolean choked () { return choked; }
  public boolean choking () { return choking; }
  public boolean interested () { return interested; }
  public boolean interesting () { return interesting; }
}
