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

/**
 * Deputy is the {@link Actor} that communicates with the Tracker.
 * It communicates the list of peers to the Manager upon request.
 * @author Martin Miralles-Cordal
 *
 */
public class Deputy extends Actor {

  private String state; // states:
  // 'error': error occurred, exception property will be populated
  // 'normal': operating normally (may add more such states later)

  private String announceURL;
  private String infoHash;
  private int listenPort;
  private int announceInterval = -1;
  private Manager manager;

  public Exception exception;         // set to an exception if one occurs

  /**
   * Constructs a Deputy object
   * @param metainfo The data from the metainfo file
   * @param port The port the manager is listening for incoming connections on
   */
  public Deputy(String announceURL, int port, Manager manager)
  {
      this.listenPort = port;
      this.manager = manager;

      // assemble our announce URL from metainfo
      this.announceURL = announceURL;

      // encode our info hash
      infoHash = escapeURL(manager.getInfoHash());
      
      // posts a memo to itself to announce when thread starts
      this.post(new Memo("announce", Util.s("&event=started"), this));
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
      boolean result = false;
      if(memo.getPayload() instanceof ByteBuffer)
        result = announce((ByteBuffer) memo.getPayload());
      else
        result = announce();
      
      if(result)
      {
        Util.setTimeout(announceInterval * 1000, new Memo("announce", null, this));
      }
    }

    else if (memo.getType().equals("done")) {
      announce(Util.s("&event=completed"));
    }
    
    else if (memo.getType().equals("halt")) {
      announce(Util.s("&event=stopped"));
      manager.post(new Memo("done", null, this));
      shutdown();
    }
  }

  /**
   * Sends an HTTP GET request and gets fresh info from the tracker.
   */
  private boolean announce()
  {
    return announce(null);
  }

  @SuppressWarnings({ "unchecked" })
  /**
   * Sends an HTTP GET request and gets fresh info from the tracker.
   * @param args extra parameters for the HTTP GET request. Must start with "&".
   */
  private boolean announce(ByteBuffer args)
  {
    if(announceURL == null)
      return false;
    else
    {
      Log.info("Contacting tracker...");

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
        Log.info("Announce URL = " + finalURL.toString());
        URL tracker = new URL(finalURL.toString());
        URLConnection trackerConn = tracker.openConnection();

        // read response
        byte[] bytes = new byte[trackerConn.getContentLength()];
        DataInputStream dis = new DataInputStream(trackerConn.getInputStream());
        dis.readFully(bytes);
        
        System.err.println(new String(bytes));
        // bdecode response
        Map<String, Object> response = (Map<String, Object>) BDecoder.decode(ByteBuffer.wrap(bytes));

        // get our peer list and work it into something nicer
        Object rawPeers = response.get("peers");
        ArrayList<Map<String,Object>> peers = null;
        if(rawPeers instanceof ArrayList<?>)
          peers = (ArrayList<Map<String,Object>>)rawPeers;
        else if(rawPeers instanceof ByteBuffer)
          peers = parsePeers((ByteBuffer) rawPeers);
        
        // send updated peer list to manager
        if(!Util.buff2str(args).equals("&event=stopped"))
          manager.post(new Memo("peers", peers, this));

        // get our announce interval
        announceInterval = (Integer) response.get("interval");
        return true;
      } catch (MalformedURLException e) {
        error(e, "Error: malformed announce URL " + finalURL.toString());
      } catch (IOException e) {
        Log.w("Warning: Unable to communicate with tracker. Retrying in 60 seconds...");
        
        // Try again in a minute
        Util.setTimeout(60000, new Memo("announce", args, this));
      } catch (Exception e) {
        e.printStackTrace();
        Log.e("Error: Invalid torrent response.");
      }
      return false;
    }
  }

  private ArrayList<Map<String, Object>> parsePeers(ByteBuffer rawPeers) {
    if(rawPeers.remaining() % 6 != 0) {
      throw new IllegalArgumentException("Invalid binary peer list");
    }
    ArrayList<Map<String, Object>> processedPeerList = new ArrayList<Map<String, Object>>();
    while(rawPeers.hasRemaining()) {
      HashMap<String,Object> peerInfo = new HashMap<String,Object>();
      
      // get this peer's ip
      StringBuilder sb = new StringBuilder();
      for(int i = 0; i < 4; i++) {
        sb.append(0xFF & rawPeers.get());
        if(i != 3) sb.append(".");
      }
      String ip = sb.toString();
      peerInfo.put("ip", ip);
      

      // get this peer's port
      int port = 0xFFFF & rawPeers.getShort();
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
    Log.error(logMessage);
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
    if(state.equals("error") || state.equals("normal")) {
      this.state = state;
    }
  }
}
