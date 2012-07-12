package libbitster;

import java.io.DataInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Deputy is the {@link Actor} that communicates with the Tracker.
 * It communicates the list of peers to the Manager upon request.
 * @author Martin Miralles-Cordal
 *
 */
public class Deputy extends Actor {
  
  private String state; // states:
  // 'init': just created, waiting to establish a connection
  // 'error': error occured, exception property will be populated
  // 'normal': operating normally (may add more such states later)
  
  private String announceURL;
  private String infoHash;
  private int listenPort;
  private int announceInterval;
  private Manager manager;
  Calendar lastAnnounce;
  
  public Exception exception;         // set to an exception if one occurs

  /**
   * Constructs a Deputy object
   * @param metainfo The data from the metainfo file
   * @param port The port the manager is listening for incoming connections on
   */
  public Deputy(TorrentInfo metainfo, int port, Manager manager)
  {
      this.listenPort = port;
      this.manager = manager;
      
      // assemble our announce URL from metainfo
      announceURL = metainfo.announce_url.getProtocol() + "://" +
        metainfo.announce_url.getHost() + ":" + metainfo.announce_url.getPort()
        + metainfo.announce_url.getPath();
      
      // encode our info hash
      infoHash = escapeURL(metainfo.info_hash);
      
      this.state = "init";
      
      // we're done setting up variables, now connect
      announce();
  }
  
  /**
   * Encode all characters in a string using URL escaping
   * @param s The string to encode
   * @return The US-ASCII encoded string
   */
  public static String escapeURL(String s)
  {
    try {
      return escapeURL(ByteBuffer.wrap(s.getBytes("UTF-8")));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Your computer somehow doesn't support UTF-8. Hang your head in shame.");
    }
  }
  
  /**
   * Encode all characters in a ByteBuffer using URL escaping
   * @param b The string ByteBuffer to encode
   * @return The US-ASCII encoded string
   */
  public static String escapeURL(ByteBuffer bb)
  {
    final char[] HEX_CHARS = 
      { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
    StringBuffer sb = new StringBuffer();
    while(bb.hasRemaining())
    {
      byte b = bb.get();
      sb.append('%');
      sb.append(HEX_CHARS[( 0x0F & (b >> 4) )]);
      sb.append(HEX_CHARS[(0x0F & b)]);
    }
    return sb.toString();
  }

  @Override
  protected void receive (Memo memo)
  {
    if(memo.getType().equals("list"))
    {
        announce(); // get updated peer list and send it to manager
    }
  }
  
  /**
   * Announce at regular intervals
   */
  @Override
  protected void idle () {
    try { sleep(1000); } catch (Exception e) {}
    
    if(Calendar.getInstance().getTimeInMillis() - this.lastAnnounce.getTimeInMillis()
        > 1000*this.announceInterval)
    {
      announce();
    }
  }
  
  /**
   * Sends an HTTP GET request and gets fresh info from the tracker.
   */
  @SuppressWarnings("unchecked")
  private void announce()
  {
    if(announceURL == null)
      return;
    else
    {
      // reset our timer
      this.lastAnnounce = Calendar.getInstance();
      System.out.println("Announcing...");
      
      StringBuffer finalURL = new StringBuffer();
      // add announce URL
      finalURL.append(announceURL);
      
      // add info hash
      finalURL.append("?info_hash=");
      finalURL.append(infoHash);
      
      // add peer ID
      finalURL.append("&peer_id=");
      finalURL.append(new String(manager.getPeerID().array()));
      
      // add port
      finalURL.append("&port=");
      finalURL.append(this.listenPort);
      
      // add uploaded
      finalURL.append("&uploaded=");
      finalURL.append(manager.getUploaded());
      
      // add downloaded
      finalURL.append("&downloaded=");
      finalURL.append(manager.getDownloaded());
      
      // add amount left
      finalURL.append("&left=");
      finalURL.append(manager.getLeft());
      
      try {
        // send request to tracker
        URL tracker = new URL(finalURL.toString());
        URLConnection trackerConn = tracker.openConnection();
        
        // read response
        byte[] bytes = new byte[trackerConn.getContentLength()];
        DataInputStream dis = new DataInputStream(trackerConn.getInputStream());    
        dis.readFully(bytes);
        
        // bdecode response
        @SuppressWarnings("rawtypes")
        Map response = (Map) Bencoder2.decode(bytes);
        
        // get our peer list and work it into something nicer
        @SuppressWarnings("rawtypes")
        ArrayList<Map> rawPeers =
            (ArrayList<Map>) response.get(
                ByteBuffer.wrap(new byte[]{'p','e','e','r','s'}));
        ArrayList<Map<String,String>> peers = parsePeers(rawPeers);
        
        // send updated peer list to manager
        manager.post(new Memo("peers", peers, this));
        
        // get our announce interval
        announceInterval = (Integer) response.get(
            ByteBuffer.wrap(new byte[]{'i','n','t','e','r','v','a','l'}));
        
        this.state = "normal";
               
      } catch (Exception e) {
        this.exception = e;
        this.state = "error";
        e.printStackTrace();
      }
    }
  }
  
  /**
   * Takes the raw peer list from the tracker response and processes it into something
   * that's nicer to work with
   * @param rawPeerList The {@code ArrayList<{@code Map}>} of peers sent from announce()
   * @return An ArrayList<Map<String, String>> of peers and their information
   */
  private ArrayList<Map<String, String>> parsePeers(@SuppressWarnings("rawtypes") ArrayList<Map> rawPeerList)
  {
    ArrayList<Map<String, String>> processedPeerList = new ArrayList<Map<String, String>>();
    for(int i = 0; i < rawPeerList.size(); ++i)
    {
      HashMap<String,String> peerInfo = new HashMap<String,String>();
      
      // get this peer's peer ID
      ByteBuffer peer_id_bytes = 
          (ByteBuffer) rawPeerList.get(i).get(ByteBuffer.wrap(new byte[]{'p','e','e','r',' ','i','d'}));
      String peer_id = new String(peer_id_bytes.array());
      peerInfo.put("peer id", peer_id);
      
      // get this peer's ip
      ByteBuffer ip_bytes = (ByteBuffer) rawPeerList.get(i).get(ByteBuffer.wrap(new byte[]{'i','p'}));
      String ip = new String(ip_bytes.array());
      peerInfo.put("ip", ip);
      
      // get this peer's port
      Integer port = (Integer) rawPeerList.get(i).get(ByteBuffer.wrap(new byte[]{'p','o','r','t'}));
      peerInfo.put("port", port.toString());
      
      // add it to our peer list
      processedPeerList.add(peerInfo);
    }
    return processedPeerList;
  }

  /**
   * @return the state
   */
  public String getState() {
    return state;
  }
  
}
