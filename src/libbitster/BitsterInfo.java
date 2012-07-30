package libbitster;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;

public final class BitsterInfo {
  
  private static BitsterInfo instance = null;
  private File info;
  private HashMap<ByteBuffer, Integer> uploadStats;
  
  @SuppressWarnings("unchecked")
  private BitsterInfo() {
    this.info = new File("metadata.bitster.dat");
    if(info.exists() && info.isFile()) {
      ObjectInputStream ois;
      try {
        ois = new ObjectInputStream(new FileInputStream(info));
        Object readIn = ois.readObject();
        if(readIn instanceof HashMap<?,?>) {
          uploadStats = (HashMap<ByteBuffer, Integer>) readIn;
        }
        ois.close();
      } catch (Exception e) {
        
      }
    }
    else {
      try {
        info.createNewFile();
        uploadStats = new HashMap<ByteBuffer, Integer>();
      } catch (IOException e) {
        Log.e("Unable to create metadata file.");
        System.exit(1);
      }
    }
  }
  
  public Integer getUploadData(ByteBuffer infoHash) {
    if(!uploadStats.containsKey(infoHash)) {
      uploadStats.put(infoHash, 0);
    }
    return uploadStats.get(infoHash);
  }
  
  public void setUploadData(ByteBuffer infoHash, Integer upload) {
    uploadStats.put(infoHash, upload);
  }
  
  public static BitsterInfo getInstance() {
    if(instance == null) {
      instance = new BitsterInfo();
    }
    return instance;
  }
  
  public void shutdown() {
    try {
      ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(info));
      oos.writeObject(uploadStats);
      oos.close();
    } catch (IOException e) {
      Log.e("Couldn't save upload data.");
    }
  }
}
