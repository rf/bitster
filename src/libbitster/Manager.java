package libbitster;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Map;

public class Manager extends Actor {
  
  // the contents of the metainfo file
  @SuppressWarnings("unused")
  private TorrentInfo metainfo;
  
  // communicates with tracker
  private Deputy deputy;
  
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
  }
  
  @SuppressWarnings("unchecked")
  protected void receive (Memo memo) {
    
    if(memo.getType().equals("peers") && memo.getSender() == deputy)
    {
      System.out.println("Manager: peers received!");
      peers = (ArrayList<Map<String, String>>) memo.getPayload();
      for(int i = 0; i < peers.size(); i++)
      {
        // find the right peer for part one
        if(peers.get(i).get("peer id").startsWith("RUBT11"))
        {
          // set up a broker
        }
      }
    }
    return;
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
 
}
