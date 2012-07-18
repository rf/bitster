package libbitster;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Deputy is the {@link Actor} that communicates with the Tracker.
 * It communicates the list of peers to the Manager upon request.
 * @author Martin Miralles-Cordal
 *
 */
public class Deputy extends Actor {

  private final static Logger log = Logger.getLogger("Deputy");

  private String state; // states:
  // 'init': just created, waiting to establish a connection
  // 'error': error occurred, exception property will be populated
  // 'normal': operating normally (may add more such states later)

  private String announceURL;
  private String infoHash;
  private int listenPort;
  private int announceInterval = 180; // 3 minutes is a safe enough default
  private Manager manager;
  boolean timeoutQueued;

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
      
      this.timeoutQueued = false;
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
    // special force reannounce request from Manager.
    // payload = null
    if (memo.getType().equals("list")) {
      announce();
    }
    
    // periodic reannounce request sent from the Timeout from itself
    // calls announce(payload)
    else if (memo.getType().equals("announce") && memo.getSender() == this)
    {
      timeoutQueued = false;
      if(memo.getPayload() instanceof ByteBuffer)
        announce((ByteBuffer) memo.getPayload());
      else
        announce();
    }

    else if (memo.getType().equals("done")) {
      announce(Util.s("&event=completed"));
    }
    
    else if (memo.getType() == "halt") {
      announce(Util.s("&event=stopped"));
      shutdown();
    }
  }

  /**
   * Announce at regular intervals
   */
  @Override
  protected void idle () {
    // send started event to tracker if we're just starting up
    if(this.getState().equals("init")) {
      announce(Util.s("&event=started"));
    }
    
    // queue our next announce
    if(timeoutQueued == false) {
      Util.setTimeout(announceInterval * 1000, new Memo("announce", null, this));
      timeoutQueued = true;
    }
    
    try { Thread.sleep(1000); } catch (Exception e) {}
  }

  /**
   * Sends an HTTP GET request and gets fresh info from the tracker.
   */
  
  private void announce()
  {
    announce(null);
  }
  @SuppressWarnings("unchecked")
  /**
   * Sends an HTTP GET request and gets fresh info from the tracker.
   * @param args extra parameters for the HTTP GET request. Must start with "&".
   */
  private void announce(ByteBuffer args)
  {
    if(announceURL == null)
      return;
    else
    {
      log.info("Contacting tracker...");
      
      // no longer in init state, may switch to error later
      this.setState("normal");

      StringBuffer finalURL = new StringBuffer();
      // add announce URL
      finalURL.append(announceURL);

      // add info hash
      finalURL.append("?info_hash=");
      finalURL.append(infoHash);

      // add peer ID
      finalURL.append("&peer_id=");
      finalURL.append(escapeURL(Util.buff2str(manager.getPeerId())));

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
      
      if(args != null)
      {
        finalURL.append(Util.buff2str(args));
      }

      try {
        // send request to tracker
        log.finer("Announce URL = " + finalURL.toString());
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
            (ArrayList<Map>) response.get(Util.s("peers"));
        ArrayList<Map<String,Object>> peers = parsePeers(rawPeers);

        // send updated peer list to manager
        manager.post(new Memo("peers", peers, this));

        // get our announce interval
        announceInterval = (Integer) response.get(Util.s("interval"));        
      } catch (MalformedURLException e) {
        error(e, "Error: malformed announce URL " + finalURL.toString());
      } catch (IOException e) {
        log.warning("Warning: Unable to communicate with tracker. Retrying in 60 seconds...");
        
        // Try again in a minute
        Util.setTimeout(60000, new Memo("announce", args, this));
        timeoutQueued = true;
      } catch (BencodingException e) {
        error(e, "Error: invalid tracker response.");
      }
    }
  }

  /**
   * Takes the raw peer list from the tracker response and processes it into something
   * that's nicer to work with
   * @param rawPeerList The {@code ArrayList<Map>} of peers sent from announce()
   * @return An {@code ArrayList<Map<String, Object>>} of peers and their information
   */
  private ArrayList<Map<String, Object>> parsePeers(@SuppressWarnings("rawtypes") ArrayList<Map> rawPeerList)
  {
    ArrayList<Map<String, Object>> processedPeerList = new ArrayList<Map<String, Object>>();
    for(int i = 0; i < rawPeerList.size(); ++i)
    {
      HashMap<String,Object> peerInfo = new HashMap<String,Object>();

      // get this peer's peer ID
      ByteBuffer peer_id_bytes =
          (ByteBuffer) rawPeerList.get(i).get(ByteBuffer.wrap(new byte[]{'p','e','e','r',' ','i','d'}));
      peerInfo.put("peerId", peer_id_bytes);

      // get this peer's ip
      ByteBuffer ip_bytes = (ByteBuffer) rawPeerList.get(i).get(ByteBuffer.wrap(new byte[]{'i','p'}));
      String ip = new String(ip_bytes.array());
      peerInfo.put("ip", ip);

      // get this peer's port
      Integer port = (Integer) rawPeerList.get(i).get(ByteBuffer.wrap(new byte[]{'p','o','r','t'}));
      peerInfo.put("port", port);

      // add it to our peer list
      processedPeerList.add(peerInfo);
    }
    return processedPeerList;
  }
  
  /**
   * Processes an exception and sets the error state
   * @param e the exception that was thrown
   */
  private void error(Exception e, String logMessage)
  {
    this.exception = e;
    this.setState("error");
    log.severe(logMessage);
  }

  /**
   * Get's the deputy's current state
   * @return the state
   */
  public String getState() {
    return state;
  }

  /**
   * Validates the input, and if okay, sets the state to it
   * @param state the state to set
   */
  public void setState(String state) {
    if(state.equals("init") || state.equals("error") || state.equals("normal")) {
      this.state = state;
    }
  }
}
