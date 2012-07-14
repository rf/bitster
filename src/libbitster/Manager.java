package libbitster;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.util.*;
import java.net.*;
import java.util.logging.*;

/**
 * Coordinates actions of all the {@link Actor}s and manages
 * the application's operation. 
 * @author Martin Miralles-Cordal
 * @author Russell Frank
 * @author Theodore Surgent
 */
public class Manager extends Actor {

  // the contents of the metainfo file
  private TorrentInfo metainfo;
  
  // destination file
  @SuppressWarnings("unused")
  private File dest;

  // communicates with tracker
  private Deputy deputy;

  // Peer ID
  private final ByteBuffer peerId;

  //Listens for incoming peer connections
  private ServerSocket listen;

  // current list of peers
  private ArrayList<Map<String, Object>> peers;
  private LinkedList<Broker> brokers; // broker objects for peer communication

  private final static Logger log = Logger.getLogger("Manager");

  // torrent info
  private int downloaded, uploaded, left;

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

    log.setLevel(Level.FINEST);

    log.info("Manager init");

    this.metainfo = metainfo;
    this.dest = dest;
    this.setDownloaded(0);
    this.setUploaded(0);
    this.setLeft(metainfo.file_length);

    brokers = new LinkedList<Broker>();

    // generate peer ID if we haven't already
    this.peerId = generatePeerID();

    // listen for connections, try ports 6881-6889, quite if all taken
    for(int i = 6881; i < 6890; ++i)
    {
      try {
        this.listen = new ServerSocket(i);
        break;
      } catch (IOException e) {
        if(i == 6890)
        {
          System.err.println("Error establishing listening socket.");
          System.exit(1);
        }
      }
    }

    deputy = new Deputy(metainfo, listen.getLocalPort(), this);
    deputy.start();

    log.info("Our peer id: " + Util.buff2str(peerId));
  }

  @SuppressWarnings("unchecked")
  protected void receive (Memo memo) {

    if(memo.getType().equals("peers") && memo.getSender() == deputy)
    {
      log.info("Received peer list");
      peers = (ArrayList<Map<String, Object>>) memo.getPayload();
      if(peers.isEmpty())
        log.warning("Peer list empty!");
      else
        log.info("Peer list recieved!");

      // TODO: fix this to check against connected peers so we dont have
      // duplicates
      if (brokers.size() > 0) return;

      for(int i = 0; i < peers.size(); i++)
      {
        // find the right peer for part one
        Map<String,Object> currPeer = peers.get(i);
        ByteBuffer prefix = Util.s("RUBT11");
        ByteBuffer id = (ByteBuffer) currPeer.get("peerId");
        if(Util.bufferEquals(id, prefix, 6))
        {
          try {
            InetAddress ip = 
              InetAddress.getByName((String) currPeer.get("ip"));
            // set up a broker
            brokers.add(new Broker(
              ip,
              (Integer) currPeer.get("port"),
              this
            ));
          } catch (UnknownHostException e) { /*impossible*/ }
        }
      }
    }
    return;
  }

  protected void idle () {
    try { Thread.sleep(10); } catch (InterruptedException e) {}

    for (Broker broker : brokers) broker.tick();          // tick each broker
  }

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

  public int getDownloaded() {
    return downloaded;
  }

  private void setDownloaded(int downloaded) {
    this.downloaded = downloaded;
  }

  public int getUploaded() {
    return uploaded;
  }

  private void setUploaded(int uploaded) {
    this.uploaded = uploaded;
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

}
