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
  private HashMap<String, Integer> uploadStats;
  
  @SuppressWarnings("unchecked")
  private BitsterInfo() {
    this.info = new File("metadata.bitster.dat");
    if(info.exists() && info.isFile()) {
      ObjectInputStream ois;
      try {
        ois = new ObjectInputStream(new FileInputStream(info));
        Object readIn = ois.readObject();
        if(readIn instanceof HashMap<?,?>) {
          uploadStats = (HashMap<String, Integer>) readIn;
        }
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
    else if(info.isDirectory()) {
      Log.e("Error: metadata.bitster.dat is a directory.");
    }
    else {
      Log.w("No metadata.bitster.dat found.");
      uploadStats = new HashMap<String, Integer>();
    }
  }
  
  public Integer getUploadData(ByteBuffer infoHash) {
    if(!uploadStats.containsKey(Util.buff2str(infoHash))) {
      Log.d("New infohash. Inserting...");
      uploadStats.put(Util.buff2str(infoHash), 0);
    }
    return uploadStats.get(Util.buff2str(infoHash));
  }
  
  public void setUploadData(ByteBuffer infoHash, Integer upload) {
    uploadStats.put(Util.buff2str(infoHash), upload);
    Log.i("Updated upload info for info hash " + Util.buff2str(infoHash));
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
      e.printStackTrace();
    }
  }
}
