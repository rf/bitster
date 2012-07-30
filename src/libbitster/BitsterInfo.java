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
        else {
          Log.e("Invalid metadata.bitster.dat");
          ois.close();
          throw new Exception();
        }
        ois.close();
      } catch (Exception e) {
        uploadStats = new HashMap<ByteBuffer, Integer>();
        info.delete();
      }
    }
    else if(info.isDirectory()) {
      Log.e("Error: metadata.bitster.dat is a directory.");
    }
    else {
      Log.w("No metadata.bitster.dat found.");
      uploadStats = new HashMap<ByteBuffer, Integer>();
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
    Log.i("Updated upload info for info hash " + Util.buff2str(infoHash));
    write();
  }
  
  public static BitsterInfo getInstance() {
    if(instance == null) {
      instance = new BitsterInfo();
    }
    return instance;
  }
  
  public void write() {
    try {
      if(!info.exists())
        info.createNewFile();
      ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(info));
      oos.writeObject(uploadStats);
      oos.close();
    } catch (IOException e) {
      Log.e("Couldn't save upload data.");
    }
  }
}
