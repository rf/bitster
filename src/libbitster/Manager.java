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
  private Object[] piecesByAvailability;
  private BitSet           received;

  private HashMap<String, Broker> peersByAddress;

  // torrent info
  private int downloaded, left;
  private UserInterface ui;

  private Funnel funnel;
  
  // true if the torrent was already done when we started.
  // This is for suppressing "completed" messages when we're already seeding.
  boolean startedSeeding = false;

  /**
   * Instantiates the Manager and its Deputy, sending a memo containing the
   * tracker's announce URL.
   * @param metainfo A {@link TorrentInfo} object containing information about
   * a torrent.
   * @param dest The file to save the download as
   */
  public Manager(TorrentInfo metainfo, File dest, UserInterface ui)
  {
    super();

    state = "booting";

    Log.info("Manager init");

    this.metainfo = metainfo;
    this.dest = dest;
    this.downloaded = 0;
    this.ui = ui;
    
    this.setLeft(metainfo.file_length);

    overlord = new Overlord();

    brokers = new LinkedList<Broker>();
    pieces = new ArrayList<Piece>();
    received = new BitSet(metainfo.piece_hashes.length);
    try {
      funnel = new Funnel(metainfo, dest, this);
    } catch (IOException e1) {
      System.err.println("Error creating funnel");
      System.exit(1);
    }
    funnel.start();
    
    // generate peer ID if we haven't already
    this.peerId = generatePeerID();
    peersByAddress = new HashMap<String, Broker>();
    
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
  }

  private void initialize() {
  	if(left == 0) {
      this.startedSeeding = true;
    }
    // We have all the piece objects, now populate our RPF array
    piecesByAvailability = pieces.toArray();
    
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

    overlord.register(listen, this);

    state = "downloading";
    Janitor.getInstance().register(this);
    
    ui.addManager(this);    
    deputy = new Deputy(metainfo, listen.socket().getLocalPort(), this);
    deputy.start();
  }
  
  @SuppressWarnings("unchecked")
  protected void receive (Memo memo) {
    
    /*
     * Messages received from our Deputy.
     */
    if(memo.getSender() == deputy) {
      // Peer list received from Deputy.
      if(memo.getType().equals("peers"))
      { 
        Log.info("Received peer list");
        peers = (ArrayList<Map<String, Object>>) memo.getPayload();
        if (peers.isEmpty()) Log.warning("Peer list empty!");
        
        Message bitfield = Message.createBitfield(received, metainfo.piece_hashes.length);
        
        for(int i = 0; i < peers.size(); i++)
        {
          // find the right peer for part one
          Map<String,Object> currPeer = peers.get(i);
          String ip = (String) currPeer.get("ip");
          String address = ip + ":" + currPeer.get("port");
  
          if ((ip.equals("128.6.5.130") || ip.equals("128.6.5.131"))
              && peersByAddress.get(address) == null)
          {
            try {
              InetAddress inetip = InetAddress.getByName(ip);
  
              // set up a broker
              Broker b = new Broker(
                inetip,
                (Integer) currPeer.get("port"),
                this,
                bitfield
              );
              brokers.add(b);
              peersByAddress.put(b.address(), b);
            } 
  
            catch (UnknownHostException e) {
              // Malformed ip, just ignore it
            }
          }
        }
      }
      
      // Part 2: Deputy is done telling the tracker we're shutting down
      else if (memo.getType().equals("done")) {
        funnel.post(new Memo("halt", null, this));
      }
    }
    
    /*
     * Messages received from our Brokers.
     */
    else if (memo.getSender() instanceof Broker) {
      // Received from Brokers when they get a block.
      if (memo.getType().equals("block")) {
        Message msg = (Message) memo.getPayload();
        Piece p = pieces.get(msg.getIndex());
  
        if (p.addBlock(msg.getBegin(), msg.getBlock())) {
          downloaded += msg.getBlockLength();
          left -= msg.getBlockLength();
        }
  
        if (p.finished()) {
          Log.info("Posting piece " + p.getNumber() + " to funnel");
          funnel.post(new Memo("piece", p, this));
          received.set(p.getNumber());
        }
  
        Log.info("Got block, " + left + " left to download.");
        
        // request more shit
        request((Broker)memo.getSender());
      }
      
      // sent when a Broker gets a bitfield message
      else if(memo.getType().equals("bitfield")) {
        BitSet field = (BitSet) memo.getPayload();
        for(int i = 0; i < field.length(); i++) {
          if(field.get(i)) {
            Piece p = pieces.get(i);
            p.incAvailable();
          }
        }
        Arrays.sort(piecesByAvailability);
        // Git dem peecazzz
        request((Broker)memo.getSender());
      }
      
      // sent when a Broker gets a have message
      else if(memo.getType().equals("have-message")) {
        int piece = (Integer) memo.getPayload();
        Piece p = pieces.get(piece);
        p.incAvailable();
        Arrays.sort(piecesByAvailability);
        request((Broker)memo.getSender());
      }
      
      // Received from Brokers when a block has been requested
      else if (memo.getType().equals("request")) {
        Message msg = (Message) memo.getPayload();
        funnel.post(new Memo("block", memo.getPayload(), memo.getSender()));
        this.addUploaded(msg.getBlockLength());
      }
      
      // Received from Brokers when they can't requested a block from a peer
      // anymore, ie when choked or when the connection is dropped.
      else if (memo.getType().equals("blockFail")) {
        Message m = (Message) memo.getPayload();
        Piece p = pieces.get(m.getIndex());
        p.blockFail(m.getBegin());
      }
    }
    
    /*
     * Messages sent from the Funnel.
     */
    else if (memo.getSender() == funnel) {
      if (memo.getType().equals("pieces")) {
        ArrayList<Piece> ps = (ArrayList<Piece>) memo.getPayload();
        
        for(int i = 0, l = ps.size(); i < l; ++i) {
          Piece p = ps.get(i);
          int length = p.getData().length;
          downloaded += length;
          left -= length;
          pieces.set(p.getNumber(), p);
          received.set(p.getNumber());
        }
        Log.info("Resuming, " + left + " left to download.");
        initialize();
      }
      
      // Received from Funnel when we successfully verify and store some piece.
      // We forward the message off to each Broker so they can inform peers.
      else if (memo.getType().equals("have")) {
        for (Broker b : brokers) 
          b.post(new Memo("have", memo.getPayload(), this));
      }
      
      // Part 3: Received from Funnel when we're ready to shut down.
      else if (memo.getType().equals("done") && memo.getSender().equals(funnel)) {
        shutdown();
        Janitor.getInstance().post(new Memo("done", null, this));
      }
    }
    
    else if (memo.getSender() instanceof Janitor) {
      // Part 1: halt message from Janitor
      if (memo.getType().equals("halt"))
      {
        state = "shutdown";
        try { listen.close(); } catch (IOException e) { e.printStackTrace(); }
        deputy.post(new Memo("halt", null, this));
      }
    }
  }
  
  private void request(Broker b) {
    if (b.interested() && b.numQueued() < 5 && left > 0) {

      // We are interested in the peer, we have less than 5 requests
      // queued on the peer, and we have more shit to download.  We should
      // queue up a request on the peer.
      
      Piece p = next(b.bitfield());

      if (p != null) {
        int index = p.next();

        b.post(new Memo("request", Message.createRequest(
          p.getNumber(), index * blockSize, p.sizeOf(index)
        ), this));
      }
    }
  }

  protected void idle () {
    // actually select() on sockets and do network io
    overlord.communicate(100);
    try { Thread.sleep(50); } catch (InterruptedException e) {}

    if (state.equals("downloading") || state.equals("seeding")) {
      Iterator<Broker> i = brokers.iterator();
      Broker b;
      
      while (i.hasNext()) {
        b = i.next();
        b.tick();
        if (b.state().equals("error")) {
          i.remove();

          // Updating our availability
          BitSet field = b.bitfield();
          if(field != null) {
            for(int j = 0; j < field.length(); j++) {
              if(!field.get(j)) {
                Piece p = pieces.get(j);
                p.decAvailable();
              }
            }
          }
          peersByAddress.put(b.address(), null);
        }
      }
    }

    if (left == 0 && !state.equals("shutdown") && !state.equals("seeding")) {
      Log.info("Download complete");
      state = "seeding";

      funnel.post(new Memo("save", null, this));      
      if(!startedSeeding) {
        deputy.post(new Memo("done", null, this));  
      }
      ui.post(new Memo("done", null, this));
    }
  }

  public boolean onAcceptable () {
    try {
      Message bitfield = Message.createBitfield(received, metainfo.piece_hashes.length);
      
      SocketChannel newConnection = listen.accept();
      newConnection.configureBlocking(false);
      if (newConnection != null) {
        brokers.add(new Broker(newConnection, this, bitfield));
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

  /** Returns true if the given bitset is interesting to us.  Run by Brokers. */
  public boolean isInteresting (BitSet peer) {
    Iterator<Piece> i = pieces.iterator();
    Piece p;
    while (i.hasNext()) {
      p = i.next();
      if (!p.finished() && peer.get(p.getNumber())) return true;
    }

    return false;
  }

  /**
   * Get the next piece we need to download. Uses rarest-piece-first.
   */
  private Piece next (BitSet b) {
    // list of rarest pieces. We return a random value in this.
    ArrayList<Piece> rarestPieces = new ArrayList<Piece>();
    
    for(int i = 0; i < piecesByAvailability.length; i++) {
      Piece p = (Piece) piecesByAvailability[i];
      if (b.get(p.getNumber()) && !p.requested()) {
        rarestPieces.add(p);
      }
      if(rarestPieces.size() == 5) {
        break;
      }
    }
    
    if(!rarestPieces.isEmpty()) {
      Random r = new Random();
      return rarestPieces.get(r.nextInt(rarestPieces.size()));
    }
    else {
      return null;
    }
  }

  /** Add a peer to our internal list of peer ids */
  public boolean addPeer (String address, Broker b) {
    if (peersByAddress.get(address) != null) return false;

    peersByAddress.put(address, b);
    return true;
  }

  public int getDownloaded() {
    return downloaded;
  }

  public int getUploaded() {
    return BitsterInfo.getInstance().getUploadData(getInfoHash());
  }

  public void addUploaded(int uploaded) {
    BitsterInfo.getInstance().setUploadData(getInfoHash(), getUploaded() + uploaded);    
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
  
  public String getFileName() { return dest.getName(); }

}
