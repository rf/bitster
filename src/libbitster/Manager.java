package libbitster;

import java.io.IOException;
import java.net.ServerSocket;

public class Manager extends Actor {
  
  // the contents of the metainfo file
  @SuppressWarnings("unused")
  private TorrentInfo metainfo;
  
  // communicates with tracker
  private Deputy deputy;
  
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
    deputy = new Deputy();
    deputy.post(new Memo("info", metainfo));   
  }
  
  protected void receive (Memo memo) {
    
    // TODO: get messages
    return;
  }
 
}
