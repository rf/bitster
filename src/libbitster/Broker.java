package libbitster;

import java.net.*;
import java.util.*;

public class Broker extends Actor {
  private Protocol peer;
  private Manager manager;

  // choked and interesting refer to the local state:
  public boolean choked;      // We are choked by the peer.
  public boolean interesting; // We are interesting to the peer.

  // choking and interested refer to the remote state:
  public boolean choking;     // We are choking this peer.
  public boolean interested;  // We are interested in the peer.

  private BitSet pieces;

  public Broker (InetAddress host, int port, Manager manager) {
    peer = new Protocol(
      host, 
      port, 
      manager.getInfoHash(),
      manager.getPeerID()
    );

    this.manager = manager;

    // When we start..
    choked = true;        // We assume we are choked by the peer.
    interesting = false;  // We assume we are not interesting to the peer.

    choking = true;       // We are choking the peer.
    interested = false;   // We are not interested in the peer.
  }

  // ## receive
  // Receive a memo
  protected void receive (Memo memo) {
    if (memo.getType() == "request") {
      peer.send((Message) memo.getPayload());
    }
  }

  // ## listen
  // Receive a message via tcp
  private void message (Message message) {
    switch (message.getType()) {

      // Handle basic messages
      case Message.CHOKE:          choked = true;                    break;
      case Message.UNCHOKE:        choked = false;                   break;
      case Message.INTERESTED:     interesting = true;               break;
      case Message.NOT_INTERESTED: interesting = false;              break;
      case Message.HAVE:           pieces.flip(message.getIndex());  break;
      case Message.BITFIELD:       pieces = message.getBitfield();   break;

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
  }
}
