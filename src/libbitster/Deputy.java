package libbitster;

import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Deputy is the {@link Actor} that communicates with the Tracker.
 * It communicates the list of peers to the Manager upon request.
 * @author Martin Miralles-Cordal
 *
 */
public class Deputy extends Actor {
  
  private String announceURL;
  private ByteBuffer infoHash;
  private byte[] peerID;

  @Override
  protected void receive (Memo memo)
  {
    if(memo.getType().equals("info"))
    {
      TorrentInfo metainfo = (TorrentInfo) memo.getPayload();
      announceURL = metainfo.announce_url.getPath();
      infoHash = metainfo.info_hash;
      
      if(peerID == null)
        peerID = generatePeerID();
      
      contactTracker();
    }
  }
  
  /**
   * Generates a 20 character {@code byte} array for use as a 
   * peer ID
   * @return A randomly generated peer ID
   */
  private byte[] generatePeerID()
  {
    byte[] id = new byte[20];
    // generating random peer ID. BITS + 16 digits = 20 characters
    Random r = new Random(System.currentTimeMillis());
    peerID = new byte[20];
    peerID[0] = 'B';
    peerID[1] = 'I';
    peerID[2] = 'T';
    peerID[3] = 'S';
    for(int i = 4; i < 20; i++)
    {
      peerID[i] = (byte) ('A' +  r.nextInt(26));
    }
    
    return id;
  }
  
  /**
   * Sends an HTTP GET request and gets fresh info from the tracker.
   */
  private void contactTracker()
  {
    if(announceURL == null)
      return;
    else
    {
      
    }
  }
  
}
