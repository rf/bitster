package libbitster;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Reimplementation of TorrentInfo class. Adds multi-file torrent support.
 * @author Martin Miralles-Cordal
 */
public class Metainfo {
  
  private ByteBuffer[] pieceHashes;
  
  private int pieceLength;
  
  // raw file bytes
  private byte[] fileBytes;
  
  // info dictionary
  private HashMap<String, Object> info;
  
  // the whole metainfo file dictionary
  private HashMap<String, Object> metainfo;
  
  // list of trackers
  private ArrayList<String> announceUrls;
  
  // whether or not the torrent has multiple files
  private boolean multiFile = false;
  
  // file name/root directory as defined by the metainfo file
  private String rootName;
  
  // file length for single torrent file
  private int fileLength;
  
  @SuppressWarnings("unchecked")
  public Metainfo(byte[] fileBytes) {
    this.fileBytes = Arrays.copyOf(fileBytes, fileBytes.length);
    
    try {
      metainfo = (HashMap<String, Object>) BDecoder.decode(ByteBuffer.wrap(fileBytes));
      
      // Get our info dictionary
      if(metainfo.containsKey("info") && (metainfo.get("info") instanceof HashMap<?,?>)) {
        info = (HashMap<String, Object>) metainfo.get("info");
        
        if(info.containsKey("name")) {
          Object item = info.get("name");
          if(item instanceof ByteBuffer) {
            rootName = Util.buff2str((ByteBuffer) item);
          }
        }
        
        if(info.containsKey("files")) {
          multiFile = true;
        }
        else if(info.containsKey("length")) {
            fileLength = (Integer) info.get("length");
        }
        else {
          throw new Exception("Can't determine files to download.");
        }
        
        // grab the length of this torrent's pieces
        if(info.containsKey("piece length")) {
          pieceLength = (Integer) info.get("piece length");
        }
        else {
          pieceLength = 0;
          throw new Exception("Piece length field missing from info dictionary.");
        }
        
        // get our piece hashes
        if(info.containsKey("pieces")) {
          ByteBuffer pieces = (ByteBuffer) info.get("pieces");
          int numPieces = pieces.array().length/20;
          System.out.println(numPieces);
          pieceHashes = new ByteBuffer[numPieces];
          for(int i = 0; i < numPieces; i++) {
            byte[] dst = new byte[20];
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
      
    } catch (Exception e) { e.printStackTrace(); }
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

  public boolean isMultiFile() {
    return multiFile;
  }

  public String getRootName() {
    return rootName;
  }

  public int getFileLength() {
    return fileLength;
  }

  public int getPieceLength() {
    return pieceLength;
  }

  public byte[] getFileBytes() {
    return fileBytes;
  }

  public String getAnnounceUrl(int index) {
    return announceUrls.get(index);
  }
}
