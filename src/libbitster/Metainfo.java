package libbitster;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Reimplementation of TorrentInfo class.
 * @author Martin Miralles-Cordal
 */
public class Metainfo {
  
  private ByteBuffer[] pieceHashes;
  private int pieceLength;
  
  // info dictionary
  private HashMap<String, Object> info;
  // the whole metainfo file dictionary
  private HashMap<String, Object> metainfo;
  
  private ArrayList<String> announceUrls;
  
  @SuppressWarnings("unchecked")
  public Metainfo(byte[] fileBytes) {
    try {
      metainfo = (HashMap<String, Object>) BDecoder.decode(ByteBuffer.wrap(fileBytes));
      
      // Get our info dictionary
      if(metainfo.containsKey("info") && (metainfo.get("info") instanceof HashMap<?,?>)) {
        info = (HashMap<String, Object>) metainfo.get("info");
        
        // grab the length of this torrent's pieces
        if(info.containsKey("piece length")) {
          pieceLength = (Integer) metainfo.get("piece length");
        }
        else {
          pieceLength = 0;
          throw new Exception("Piece length field missing from info dictionary.");
        }
        
        // get our piece hashes
        if(info.containsKey("pieces")) {
          ByteBuffer pieces = (ByteBuffer) metainfo.get("pieces");
          int numPieces = pieces.remaining()/pieceLength; 
          pieceHashes = new ByteBuffer[numPieces];
          for(int i = 0; i < numPieces; i++) {
            byte[] dst = new byte[10];
            pieces.get(dst);
            pieceHashes[i] = ByteBuffer.wrap(dst);
          }
        }
      }
      
      // get our announce URL list. This field is not required.
      if(metainfo.containsKey("announce-list")) {
        ArrayList<Object> announceList = (ArrayList<Object>) metainfo.get("announce-list");
        for(Object item : announceList) {
          if(item instanceof ByteBuffer) {
            announceUrls.add(Util.buff2str((ByteBuffer) item));
          }
        }
      }
      
      // get our announce URL. This is the one that's required.
      if(metainfo.containsKey("announce")){
        if(announceUrls == null) {
          announceUrls = new ArrayList<String>();
        }
        announceUrls.add(Util.buff2str((ByteBuffer) metainfo.get("announce")));
      }
      else {
        throw new Exception("Missing announce URL.");
      }
      
    } catch (Exception e) { /* TBD */ }
  }

  /**
   * @return the piece_hashes
   */
  public ByteBuffer[] getPieceHashes() {
    return pieceHashes;
  }
  
  public ByteBuffer getPieceHash(int index) throws ArrayIndexOutOfBoundsException {
    if(index >= 0 && index < pieceHashes.length) {
      return pieceHashes[index];
    }
    else {
      throw new ArrayIndexOutOfBoundsException("Invalid piece number " + index);
    }
  }
}
