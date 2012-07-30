package libbitster;

import java.io.File;
import java.io.IOException;
import java.nio.channels.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.net.*;


/**
 * Coordinates actions of all the {@link Actor}s and manages
 * the application's operation. 
 * @author Martin Miralles-Cordal
 * @author Russell Frank
 * @author Theodore Surgent
 */

public class Manager extends Actor implements Communicator {

  private final int blockSize = 16384;
  private String state;

  // the contents of the metainfo file
  private TorrentInfo metainfo;
  
  // destination file
  @SuppressWarnings("unused")
  private File dest;

  // communicates with tracker
  private Deputy deputy;

  // Actually select()s on sockets
  private Overlord overlord;

  // Peer ID
  private final ByteBuffer peerId;

  // Listens for incoming peer connections
  private ServerSocketChannel listen;

  // current list of peers
  private ArrayList<Map<String, Object>> peers;
  private LinkedList<Broker> brokers; // broker objects for peer communication

  private ArrayList<Piece> pieces;

  private HashMap<ByteBuffer, Broker> peersById;

  // torrent info
  private int downloaded, uploaded, left;

  private Funnel funnel;

  /**
   * Instantiates the Manager and its Deputy, sending a memo containing the
   * tracker's announce URL.
   * @param metainfo A {@link TorrentInfo} object containing information about
   * a torrent.
   * @param dest The file to save the download as
   */
  public Manager(TorrentInfo metainfo, File dest)
  {
    super();

    state = "booting";

    Log.info("Manager init");

    this.metainfo = metainfo;
    this.dest = dest;
    this.downloaded = 0;
    this.uploaded = 0;
    this.setLeft(metainfo.file_length);

    overlord = new Overlord();

    brokers = new LinkedList<Broker>();
    pieces = new ArrayList<Piece>();
    try {
      funnel = new Funnel(metainfo, dest, this);
    } catch (IOException e1) {
      System.err.println("Error creating funnel");
      System.exit(1);
    }
    funnel.start();

    peersById = new HashMap<ByteBuffer, Broker>();

    // generate peer ID if we haven't already
    this.peerId = generatePeerID();

    // listen for connections, try ports 6881-6889, quite if all taken
    for(int i = 6881; i < 6890; ++i)
    {
      try {
        listen = ServerSocketChannel.open();
        listen.socket().bind(new InetSocketAddress("0.0.0.0", i));
        listen.configureBlocking(false);
        break;
      } 

      catch (IOException e) {
        if(i == 6890)
        {
          Log.warning("could not open a socket for listening");
          shutdown();
        }
      }
    }

    deputy = new Deputy(metainfo, listen.socket().getLocalPort(), this);
    deputy.start();

    overlord.register(listen, this);

    Log.info("Our peer id: " + Util.buff2str(peerId));

    int i, total = metainfo.file_length;
    for (i = 0; i < metainfo.piece_hashes.length; i++) {
      pieces.add(new Piece(
        metainfo.piece_hashes[i].array(), 
        i, 
        blockSize, 
        // If the last piece is truncated (which it probably is) total will
        // be less than piece_length and will be the last piece's length.
        Math.min(metainfo.piece_length, total)
      ));
      total -= metainfo.piece_length;
    }

    state = "downloading";
    Janitor.getInstance().register(this);
  }

  @SuppressWarnings("unchecked")
  protected void receive (Memo memo) {

    // Peer list received from Deputy.
    if(memo.getType().equals("peers") && memo.getSender() == deputy)
    {
      Log.info("Received peer list");
      peers = (ArrayList<Map<String, Object>>) memo.getPayload();
      if (peers.isEmpty()) Log.warning("Peer list empty!");

      for(int i = 0; i < peers.size(); i++)
      {
        // find the right peer for part one
        Map<String,Object> currPeer = peers.get(i);
        String ip = (String) currPeer.get("ip");

        if ((ip.equals("128.6.5.130") || ip.equals("128.6.5.131"))
            && peersById.get(currPeer.get("peerId")) == null)
        {
          try {
            InetAddress inetip = InetAddress.getByName(ip);

            // set up a broker
            Broker b = new Broker(
              inetip,
              (Integer) currPeer.get("port"),
              this
            );
            brokers.add(b);
            peersById.put((ByteBuffer) currPeer.get("peerId"), b);
          } 

          catch (UnknownHostException e) {
            // Malformed ip, just ignore it
          }
        }

      }
    }

    // Received from Brokers when they get a block.
    else if (memo.getType().equals("block")) {
      Message msg = (Message) memo.getPayload();
      Piece p = pieces.get(msg.getIndex());

      if (p.addBlock(msg.getBegin(), msg.getBlock())) {
        downloaded += msg.getBlockLength();
        left -= msg.getBlockLength();
      }

      if (p.finished()) {
        Log.info("Posting piece " + p.getNumber() + " to funnel");
        funnel.post(new Memo("piece", p, this));
      }

      Log.info("Got block, " + left + " left to download.");
    }
    
    // Received from Brokers when a block has been requested
    else if (memo.getType().equals("request")) {
      Message msg = (Message) memo.getPayload();
      Piece p = funnel.getPiece(msg.getIndex());
      try {
        ByteBuffer stoof = ByteBuffer.wrap(p.getBlock(msg.getBegin(), msg.getBlockLength()));
        Message response = Message.createPiece(msg.getIndex(), msg.getBegin(), stoof);
        memo.getSender().post(new Memo("block", response, this));
        this.addUploaded(msg.getBlockLength());
      } catch(IllegalArgumentException e) {
        Log.e("Invalid block request: " + e.getMessage());
      }
    }
    
    else if (memo.getType().equals("pieces")) {
      ArrayList<Piece> ps = (ArrayList<Piece>) memo.getPayload();
      
      for(int i = 0, l = ps.size(); i < l; ++i) {
        Piece p = ps.get(i);
        int length = p.getData().length;
        downloaded += length;
        left -= length;
        pieces.set(p.getNumber(), p);
        
        //Notify brokers
        for (Broker b : brokers) 
          b.post(new Memo("have", p, this));
      }

      Log.info("Resuming, " + left + " left to download.");
    }
    
    // Received from Funnel when we successfully verify and store some piece.
    // We forward the message off to each Broker so they can inform peers.
    else if (memo.getType().equals("have")) {
      for (Broker b : brokers) 
        b.post(new Memo("have", memo.getPayload(), this));
    }

    // Received from Brokers when they can't requested a block from a peer
    // anymore, ie when choked or when the connection is dropped.
    else if (memo.getType().equals("blockFail")) {
      Message m = (Message) memo.getPayload();
      Piece p = pieces.get(m.getIndex());
      p.blockFail(m.getBegin());
    }
    
    /* These three memos should be received in a chain, and are part of the
     * shutdown sequence. */
    
    // Part 1: halt message from Janitor
    else if (memo.getType().equals("halt"))
    {
      state = "shutdown";
      try { listen.close(); } catch (IOException e) { e.printStackTrace(); }
      deputy.post(new Memo("halt", null, this));
    }
    
    // Part 2: Deputy is done telling the tracker we're shutting down
    else if (memo.getType().equals("done") && memo.getSender().equals(deputy)) {
      funnel.post(new Memo("halt", null, this));
    }

    // Part 3: Received from Funnel when we're ready to shut down.
    else if (memo.getType().equals("done") && memo.getSender().equals(funnel)) {
      shutdown();
      Janitor.getInstance().post(new Memo("done", null, this));
    }
  }

  protected void idle () {
    // actually select() on sockets and do network io
    overlord.communicate(100);
    try { Thread.sleep(50); } catch (InterruptedException e) {}

    if (state == "downloading") {

      Iterator<Broker> i = brokers.iterator();
      Broker b;
      while (i.hasNext()) {
        b = i.next();
        b.tick();
        if (b.state() == "error") {
          i.remove();
          peersById.put(b.peerId(), null);
        }

        else {
          if (b.interested() && b.numQueued() < 5 && left > 0) {

            // We are interested in the peer, we have less than 5 requests
            // queued on the peer, and we have more shit to download.  We should
            // queue up a request on the peer.

            //log.info("We're interested in a peer. Finding something to req");

            // TODO: actually check if the peer has this piece
            Piece p = next();

            if (p != null) {
              if (!b.has(p.getNumber())) continue;
              int index = p.next();

              b.post(new Memo("request", Message.createRequest(
                p.getNumber(), index * blockSize, p.sizeOf(index)
              ), this));
            } 

          }
        }
      }

    }

    if (left == 0 && state != "shutdown" && state != "done") {
      Log.info("Download complete");
      state = "done";
      Iterator<Broker> i = brokers.iterator();
      Broker b;

      while (i.hasNext()) {
        b = i.next();
        b.close();
        i.remove();
      }

      funnel.post(new Memo("save", null, this));
      deputy.post(new Memo("done", null, this));
    }
  }

  public boolean onAcceptable () {
    try {
      SocketChannel newConnection = listen.accept();
      if (newConnection != null) {
        brokers.add(new Broker(newConnection, this));
      }
    } catch (IOException e) {
      // connection failed, ignore
    }

    return true;
  }

  public boolean onReadable () { return false; }
  public boolean onWritable () { return false; }
  public boolean onConnectable () { return false; }

  /**
   * Generates a 20 character {@code byte} array for use as a
   * peer ID
   * @return A randomly generated peer ID
   */
  private ByteBuffer generatePeerID()
  {
    byte[] id = new byte[20];
    // generating random peer ID. BTS- + 16 alphanums = 20 characters
    Random r = new Random(System.currentTimeMillis());
    id[0] = 'B';
    id[1] = 'T';
    id[2] = 'S';
    id[3] = '-';
    for(int i = 4; i < 20; i++)
    {
      int rand = r.nextInt(36);
      if(rand < 10)
        id[i] = (byte) ('0' + rand);
      else
      {
        rand -= 10;
        id[i] = (byte) ('A' + rand);
      }

    }

    return ByteBuffer.wrap(id);
  }

  // ## isInteresting
  // Returns true if the given bitset is interesting to us.  Run by Brokers.
  public boolean isInteresting (BitSet peer) {
    Iterator<Piece> i = pieces.iterator();
    Piece p;
    while (i.hasNext()) {
      p = i.next();
      if (!p.finished() && peer.get(p.getNumber())) return true;
    }

    return false;
  }

  // ## next
  // Get the next piece we need to download.
  // TODO: replace with a better algorithm for finding next piece.
  private Piece next () {
    Iterator<Piece> i = pieces.iterator();
    Piece p;
    while (i.hasNext()) {
      p = i.next();
      if (!p.requested()) return p;
    }

    return null;
  }

  /**
   * Add a peer to our internal list of peer ids
   */
  public boolean addPeer (ByteBuffer peerId, Broker b) {
    if (peersById.get(peerId) != null) return false;

    peersById.put(peerId, b);
    return true;
  }

  public int getDownloaded() {
    return downloaded;
  }

  public int getUploaded() {
    return uploaded;
  }

  public void addUploaded(int uploaded) {
    this.uploaded += uploaded;
  }

  public int getLeft() {
    return left;
  }

  private void setLeft(int left) {
    this.left = left;
  }

  public ByteBuffer getPeerId () {
    return peerId;
  }

  public ByteBuffer getInfoHash () {
    return metainfo.info_hash;
  }

  public String getState () { return state; }
  public Overlord getOverlord () { return overlord; }

}
