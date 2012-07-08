package libbitster;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
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
  private String infoHash;
  private byte[] peerID;
  private TorrentInfo metainfo;

  @Override
  protected void receive (Memo memo)
  {
    if(memo.getType().equals("info"))
    {
      metainfo = (TorrentInfo) memo.getPayload();
      announceURL = metainfo.announce_url.getProtocol() + "://" +
          metainfo.announce_url.getHost() + ":" + metainfo.announce_url.getPort()
          + metainfo.announce_url.getPath();
      
      ByteBuffer rawInfoHash = metainfo.info_hash;
      StringBuffer infoHashSB = new StringBuffer();
      while(rawInfoHash.hasRemaining())
      {
        infoHashSB.append("%");
        String hexEncode = Integer.toHexString(0xFF & rawInfoHash.get());
        if(hexEncode.length() == 1)
          infoHashSB.append("0");
        infoHashSB.append(hexEncode);
      }
      infoHash = infoHashSB.toString();
      
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
    id[0] = 'B';
    id[1] = 'I';
    id[2] = 'T';
    id[3] = 'S';
    for(int i = 4; i < 20; i++)
    {
      id[i] = (byte) ('A' +  r.nextInt(26));
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
      StringBuffer finalURL = new StringBuffer();
      // add announce URL
      finalURL.append(announceURL);
      
      // add info hash
      finalURL.append("?info_hash=");
      finalURL.append(infoHash.toString());
      
      // add peer ID
      finalURL.append("&peer_id=");
      String s = "";
      finalURL.append(new String(peerID));
      
      // add port
      finalURL.append("&port=6881"); // TODO: don't hardcode this
      
      // add rest
      // TODO: split this all up and figure it out programmatically
      finalURL.append("&uploaded=0&downloaded=0");
      
      // add amount left
      // TODO: do this right
      finalURL.append("&left=");
      finalURL.append(metainfo.file_length);
      
      System.out.println(finalURL.toString());
      try {
        URL tracker = new URL(finalURL.toString());
        InputStream is = tracker.openStream();
        byte b;
        byte[] bytes = new byte[is.available()];
        is.read(bytes);
        System.out.println(new String(bytes));
      } catch (MalformedURLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
  
}
