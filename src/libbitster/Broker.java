package libbitster;

import java.net.*;
import java.util.*;

import java.nio.*;
import java.nio.channels.*;

/**
 * The `Broker` class manages a connection with a peer.  It uses the
 * `Protocol` class for the actual communication.
 * @author Russ Frank
 * @author Martin Miralles-Cordal
 */

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

  private int numReceived = 0; // # of received messages
  private int numQueued = 0;

  public int piecesReceived = 0;
  public float speed = 0;


  private LinkedList<Message> outbox;

  // Pieces we've requested from the peer. Heinous, I know, but it works.
  private HashMap<String, Message> requests;

  public Broker (SocketChannel sc, Manager manager, Message bitfield) {
    super();
    Log.i("Broker: accepting");

    requests = new HashMap<String, Message>();
    outbox = new LinkedList<Message>();
    peer = new Protocol(
      sc, 
      manager.getInfoHash(), 
      manager.getPeerId(), 
      manager.getOverlord()
    );
    peer.establish();
    peer.send(bitfield);
    this.manager = manager;
    state = "check";
    Util.setTimeout(120000, new Memo("keepalive", null, this));

    Util.setTimeout(20000, new Memo("calcSpeed", null, this));
    forwardWatches(manager);
  }

  public Broker (InetAddress host, int port, Manager manager, Message bitfield) {
    super();
    Log.info("Broker init for host: " + host);

    requests = new HashMap<String, Message>();
    outbox = new LinkedList<Message>();

    peer = new Protocol(
      host, 
      port, 
      manager.getInfoHash(),
      manager.getPeerId(),
      manager.getOverlord()
    );
    peer.establish();
    peer.send(bitfield);

    this.manager = manager;
    state = "normal";
    Util.setTimeout(120000, new Memo("keepalive", null, this));
    Util.setTimeout(20000, new Memo("calcSpeed", null, this));
    forwardWatches(manager);
  }

  /** Receive a memo */
  protected void receive (Memo memo) {
    if (memo.getType().equals("request")) {
      numQueued += 1;
      this.signal("broker numQueued", numQueued, this);
      Message m = (Message) memo.getPayload();
      requests.put(m.getIndex() + ":" + m.getBegin(), m);
      if (choked) {
        Log.info("We're choked, queuing message");
        outbox.add(m);
      } 

      else {
        //Log.info("Sending " + m);
        peer.send(m);
      }
    }

    else if (memo.getType().equals("unchoke")) {
      Log.info("unchoking peer " + Util.buff2str(peer.getPeerId()));
      peer.send(Message.createUnchoke());
      choking = false;
      this.signal("broker choking", choking, this);
    }

    else if (memo.getType().equals("choke")) {
      Log.info("choking peer " + Util.buff2str(peer.getPeerId()));
      peer.send(Message.createChoke());
      choking = true;
      this.signal("broker choking", choking, this);
    }

    // Get block back from funnel
    else if (memo.getType().equals("block")) {
      Message msg = (Message) ((Object[])memo.getPayload())[0];
      ByteBuffer stoof = (ByteBuffer) ((Object[])memo.getPayload())[1];
      Message response = Message.createPiece(msg.getIndex(), msg.getBegin(), stoof);
      Log.d("Sending to " + new String(this.peerId().array()) + ": " + response);
      peer.send(response);
    }

    else if (memo.getType().equals("keepalive") && state.equals("normal")) {
      //Log.info("Sending keep alive");
      peer.send(Message.createKeepAlive());
      Util.setTimeout(120000, new Memo("keepalive", null, this));
    }

    // received from Manager when we've finished downloading a piece
    else if (memo.getType().equals("have")) {
      if (peer.getState().equals("normal")) {
        Piece p = (Piece) memo.getPayload();

        /* Only send have message if peer doesn't have said piece. This lowers overhead
         * about 35% on average, but it could consequently make pieces seem rarer than 
         * they are to seeders.
         * See http://wiki.theory.org/BitTorrentSpecification#have:_.3Clen.3D0005.3E.3Cid.3D4.3E.3Cpiece_index.3E
         */
        if(pieces == null || !pieces.get(p.getNumber())) {
          peer.send(Message.createHave(p.getNumber()));
          //Log.info("Informing peer " + Util.buff2str(peer.getPeerId()) + 
              //" that we have piece " + p.getNumber());
        }

      } else Log.info("Peer not connected, not sending have.");
    }

    else if (memo.getType().equals("calcSpeed")) {
      // Rough speed calculation
      speed = (float) piecesReceived / 20.0f;
      piecesReceived = 0;
    }
  }

  private void error (Exception e) {
    state = "error";
    this.signal("broker state", state, this);
    exception = e;
    peer.close();

    if (requests.size() > 0) {
      for (Message m : requests.values()) {
        manager.post(new Memo("blockFail", m, this));
      }
    }
  }

  /** Send a state update to manager **/
  private void updateManager () {
    manager.post(new Memo("stateChanged", null, this));
  }

  /** Close the connection **/
  public void close () { peer.close(); }

  /** Receive a message via tcp */
  private void message (Message message) {
    if (numReceived > 0 && message.getType() == Message.BITFIELD) 
      error(new Exception("protocol error"));
    numReceived += 1;

    switch (message.getType()) {

      // Handle basic messages
      case Message.CHOKE:
        choked = true;
        updateManager();
        this.signal("broker choked", choked, this);
      break;
      
      case Message.UNCHOKE:
        choked = false;
        updateManager();
        this.signal("broker choked", choked, this);
      break;
      
      case Message.INTERESTED:
        interesting = true;
        updateManager();
        this.signal("broker interesting", interesting, this);
        peer.send(Message.createUnchoke());
        choking = false;
        this.signal("broker choking", choking, this);
      break;
      
      case Message.NOT_INTERESTED:
        interesting = false;
        updateManager();
        this.signal("broker interesting", interesting, this);
      break;

      case Message.BITFIELD:       
        pieces = message.getBitfield();
        manager.post(new Memo("bitfield", pieces, this));
        checkInterested();
      break;

      case Message.HAVE:
        if (pieces == null) pieces = new BitSet();
        pieces.set(message.getIndex());
        manager.post(new Memo("have-message", message.getIndex(), this));
        checkInterested();
      break;

      // Send pieces to our `Manager`.
      case Message.PIECE:
        numQueued -= 1;
        piecesReceived += 1;
        this.signal("broker numQueued", numQueued, this);
        requests.remove(message.getIndex() + ":" + message.getBegin());
        manager.post(new Memo("block", message, this));
      break;

      case Message.REQUEST:
        // Post a "request" memo to Manager, which passes it on as
        // a "block" memo to Funnel, who grabs the block and forwards
        // it to the requesting Broker
        if (!choking) manager.post(new Memo("request", message, this));

        // Be an asshole and drop peers who attempt to request from us when
        // we're choking them.  They should know better.
        else error(new Exception("protocol error"));
      break;
    }

    if (choked) {
      // If we're choked, assume any pending requests have been discarded by
      // the peer.
      if (requests.size() > 0) {
        Iterator <Message> i = requests.values().iterator();
        while (i.hasNext()) {
          Message item = i.next();
          manager.post(new Memo("blockFail", item, this));
          i.remove();
        }
      }
    }
  }

  protected void idle () {
    Message m;
    while ((m = peer.receive()) != null) message(m); // grab any available msgs

    if (state.equals("check") && peer.getPeerId() != null) {
      if (!manager.addPeer(peer.getAddress(), this)) {
        // Peer has already been added
        Log.error("Dropping duplicate connection to " + 
          Util.buff2str(peer.getPeerId()));
        error(new Exception("duplicate"));
      } else {
        state = "normal";
        this.signal("broker state", state, this);
      }
    }

    if (peer.getState().equals("error")) {
      if (state != "error") { // we haven't displayed the error msg yet
        Log.error("Peer " + Util.buff2str(peer.getPeerId()) + " protocol " +
          "error: " + peer.exception);
      }
      state = "error";
      updateManager();
      this.signal("broker state", state, this);
    }

    if (outbox.size() > 0 && !choked) {
      Log.debug("We're unchoked and there are messages in the queue, flushing");
      Iterator<Message> i = outbox.iterator();
      while (i.hasNext()) {
        Message msg = i.next();
        Log.debug("Sending " + msg);
        peer.send(msg);
        i.remove();
      }
    } 
  }

  private void checkInterested () {
    if (manager.isInteresting(pieces) && !interested) {
      Log.debug("We are interested in " + Util.buff2str(peer.getPeerId()));
      interested = true;
      this.signal("broker interested", interested, this);
      choking = false;
      this.signal("broker choking", choking, this);
      //peer.send(Message.createUnchoke());
      peer.send(Message.createInterested());
    }
  }
  
  private void forwardWatches(Manager manager) {
    this.watch("broker state", manager);
    this.watch("broker numQueued", manager);
    this.watch("broker choked", manager);
    this.watch("broker choking", manager);
    this.watch("broker interested", manager);
    this.watch("broker interesting", manager);
  }

  /** Checks to see if the peer has this piece. */
  public boolean has (int number) {
    return pieces.get(number);
  }

  // Accessors.
  public boolean choked () { return choked; }
  public boolean choking () { return choking; }
  public boolean interested () { return interested; }
  public boolean interesting () { return interesting; }
  public String state () { return state; }
  public int numQueued () { return numQueued; }
  public ByteBuffer peerId () { return peer.getPeerId(); }
  public String address() { return peer.getAddress(); }
  public BitSet bitfield() { return this.pieces; }
  
  /**
   * Checks to see if an object is equal to this.
   * @return false if not a broker or peer IDs are not equal
   */
  public boolean equals(Object o) {
    if(o == null || !(o instanceof Broker)) {
      return false;
    }
    else {
      Broker b = (Broker) o;
      return b.peerId().equals(this.peerId());
    }
  }

  /**
   * @return a String representation of this object.
   */
  public String toString () {
    return "Broker [" + Util.buff2str(peer.getPeerId()) + "] choked: " + choked + " choking: " + choking +" interested: " + interested +" interesting: " + interesting + " speed: " + speed;
  }
}
