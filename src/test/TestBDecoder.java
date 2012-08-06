package test;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Set;

import libbitster.BDecoder;

public class TestBDecoder {

  /**
   * @param args
   */
  @SuppressWarnings("unchecked")
  public static void main(String[] args) {
    try {
      File torrentFile = new File("project2.torrent");
      byte[] torrentBytes = new byte[(int) torrentFile.length()]; 
      DataInputStream dis;
      dis = new DataInputStream(new FileInputStream(torrentFile));
      dis.readFully(torrentBytes);
      dis.close();
      HashMap<String,Object> data = (HashMap<String, Object>) BDecoder.decode(ByteBuffer.wrap(torrentBytes));
      Set<String> keys = data.keySet();
      for(String key : keys) {
        System.out.println("[" + key + " => " + data.get(key) + "]");
        
      }
    } catch (Exception e) { e.printStackTrace(); }
  }

}
