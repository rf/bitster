package libbitster;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

public class Manager extends Actor {
  
  // the contents of the metainfo file
  @SuppressWarnings("unused")
  private TorrentInfo metainfo;
  
  // communicates with tracker
  private Deputy deputy;
  
  // Peer ID
  private final ByteBuffer peerID;
  
  //Listens for incoming peer connections
  private ServerSocket listen;
  
  // current list of peers
  private ArrayList<Map<String, String>> peers;
  
  // torrent info
  private int downloaded, uploaded, left;
  
  /**
   * Instantiates the Manager and its Deputy, sending a memo containing the 
   * tracker's announce URL.
   * @param metainfo A {@link TorrentInfo} object containing information about
   * a torrent.
   */
  public Manager(TorrentInfo metainfo)
  {
    super();
    this.metainfo = metainfo;
    
    this.setDownloaded(0);
    this.setUploaded(0);
    this.setLeft(metainfo.file_length);
    
    // generate peer ID if we haven't already
    this.peerID = generatePeerID();
    System.out.println(new String(this.peerID.array()));
    
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
  }
  
  @SuppressWarnings("unchecked")
  protected void receive (Memo memo) {
    
    if(memo.getType().equals("peers") && memo.getSender() == deputy)
    {
      peers = (ArrayList<Map<String, String>>) memo.getPayload();
      if(peers.isEmpty())
        System.out.println("Manager: peer list is empty!");
      else
        System.out.println("Manager: peers received!");
      
      for(int i = 0; i < peers.size(); i++)
      {
        // find the right peer for part one
        Map<String,String> currPeer = peers.get(i);
        System.out.println(currPeer);
        if(peers.get(i).get("peer id").startsWith("RUBT11"))
        {
          // set up a broker
        }
      }
    }
    return;
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

  public ByteBuffer getPeerID () {
    return peerID;
  }

  public ByteBuffer getInfoHash () {
    return metainfo.info_hash;
  }
 
}
