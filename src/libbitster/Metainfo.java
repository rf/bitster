package libbitster;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Reimplementation of TorrentInfo class.
 * @author martin
 *
 */
public class Metainfo {
  
  private ArrayList<ByteBuffer> announceUrls;
  
  public Metainfo(byte[] fileBytes) {
    this.announceUrls = new ArrayList<ByteBuffer>();
    
  }
}
