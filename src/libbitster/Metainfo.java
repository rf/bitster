package libbitster;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Reimplementation of TorrentInfo class.
 * @author Martin Miralles-Cordal
 */
public class Metainfo {
  
  public final ByteBuffer[] piece_hashes;
  
  private ArrayList<ByteBuffer> announceUrls;
  
  public Metainfo(byte[] fileBytes) {
    this.announceUrls = new ArrayList<ByteBuffer>();
    
  }
}
