package libbitster;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * Singleton. Stores information external to any given instance of Bitster.
 * Reads info from metadata.bitster.dat, which it expects to find in the
 * pwd.
 * @author Martin Miralles-Cordal
 */
public final class BitsterInfo {
  
  private static BitsterInfo instance = null;
  private File info;
  private HashMap<String, Integer> uploadStats;
  
  @SuppressWarnings("unchecked")
  private BitsterInfo() {
    this.info = new File("metadata.bitster.dat");
    
    if(info.exists() && info.isFile()) {
      ObjectInputStream ois;
      try {
        ois = new ObjectInputStream(new FileInputStream(info));
        Object readIn = ois.readObject();
        
        // If file is valid
        if(readIn instanceof HashMap<?,?>) {
          uploadStats = (HashMap<String, Integer>) readIn;
        }
        
        // File exists but is invalid
        else {
          Log.e("Invalid metadata.bitster.dat");
          ois.close();
          throw new Exception();
        }
        
        ois.close();
      } catch (Exception e) {
        uploadStats = new HashMap<String, Integer>();
        info.delete();
      }
    }
    
    // if metadata.bitster.dat is a directory, we're just plain fucked.
    else if(info.isDirectory()) {
      Log.e("Error: metadata.bitster.dat is a directory.");
    }
    
    // but if it's not there, we're a-okay!
    else {
      Log.w("No metadata.bitster.dat found.");
      uploadStats = new HashMap<String, Integer>();
    }
  }
  
  /** Takes an infohash, turns it into a string to use as a key
   *  because ByteBuffers aren't serializable :|
   * @param infoHash the torrent info hash
   * @return the amount the user has uploaded across instances
   */
  public Integer getUploadData(ByteBuffer infoHash) {
    if(!uploadStats.containsKey(Util.buff2str(infoHash))) {
      Log.d("New infohash. Inserting...");
      uploadStats.put(Util.buff2str(infoHash), 0);
    }
    return uploadStats.get(Util.buff2str(infoHash));
  }
  
  /**
   * Sets upload data!
   * @param infoHash The info hash to update
   * @param upload the new upload amount, in bytes
   */
  public void setUploadData(ByteBuffer infoHash, Integer upload) {
    uploadStats.put(Util.buff2str(infoHash), upload);
  }
  
  /** returns instance and instantiates it if called for the first time */ 
  public static BitsterInfo getInstance() {
    if(instance == null) {
      instance = new BitsterInfo();
    }
    return instance;
  }
  
  /** Save and quit */
  public void shutdown() {
    try {
      ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(info));
      oos.writeObject(uploadStats);
      oos.close();
    } catch (IOException e) {
      Log.e("Couldn't save upload data.");
      e.printStackTrace();
    }
  }
}
