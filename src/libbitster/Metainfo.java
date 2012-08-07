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
  
  private ArrayList<String> announceUrls;
  
  @SuppressWarnings("unchecked")
  public Metainfo(byte[] fileBytes) {
    HashMap<String, Object> metainfo;
    try {
      metainfo = (HashMap<String, Object>) BDecoder.decode(ByteBuffer.wrap(fileBytes));
      
      // get our announce URLs
      if(metainfo.containsKey("announce-list")) {
        ArrayList<Object> announceList = (ArrayList<Object>) metainfo.get("announce-list");
        for(Object item : announceList) {
          if(item instanceof ByteBuffer) {
            announceUrls.add(Util.buff2str((ByteBuffer) item));
          }
        }
      }
      
      if(metainfo.containsKey("announce")){
        if(announceUrls == null) {
          announceUrls = new ArrayList<String>();
        }
        announceUrls.add(Util.buff2str((ByteBuffer) metainfo.get("announce")));
      }
      else {
        throw new Exception("Invalid metainfo file.");
      }
      
      if(metainfo.containsKey("piece length")) {
        pieceLength = (Integer) metainfo.get("piece length");
      }
      else {
        pieceLength = 0;
        throw new Exception("Invalid metainfo file.");
      }
      
      if(metainfo.containsKey("pieces")) {
        ByteBuffer pieces = (ByteBuffer) metainfo.get("pieces");
        int numPieces = pieces.remaining()/pieceLength; 
        pieceHashes = new ByteBuffer[numPieces];
        for(int i = 0; i < numPieces; i++) {
          byte[] dst = new byte[10];
          pieces.get(dst);
          pieceHashes[i] = ByteBuffer.wrap(dst);
        }
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
