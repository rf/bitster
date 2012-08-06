package libbitster;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Clean room re-implementation of the decoding parts of Bencoder2.java.
 * @author Martin Miralles-Cordal
 */
public class BDecoder {
  
  /**
   * Decodes a bencoded ByteBuffer.
   * @param bytes the ByteBuffer to decode. 
   * WARNING: the ByteBuffer will be permanently altered by this procedure, so it
   * is recommended to pass a copy of your buffer to this function.
   * @return A Map, String, List, or Integer, depending on what was bencoded.
   */
  public static Object decode(ByteBuffer bytes) {
    bytes.compact().rewind();
    byte next = bytes.get();
    if(next == 'd') {
      return decodeDictionary(bytes);
    }
    else if(next == 'i') {
      return decodeInteger(bytes);
    }
    else if(Character.isDigit(next)) {
      bytes.rewind();
      return decodeString(bytes);
    }
    else if(next == 'l') {
      return decodeList(bytes);
    }
    else {
      Log.e("Error bdecoding bytes: " + Util.buff2str(bytes));
      return null;
    }
  }
  
  public static HashMap<String, Object> decodeDictionary(ByteBuffer bytes) {
    byte next;
    bytes.compact().rewind();
    HashMap<String, Object> dictionary = new HashMap<String, Object>();
    do {
      bytes.rewind();
      String key = decodeString(bytes);
      Object value = decode(bytes);
      dictionary.put(key, value);
      bytes.compact().rewind();
      next = bytes.get();
    } while(next != 'e');
    
    return dictionary;
  }
  
  public static String decodeString(ByteBuffer bytes) {
    byte next;
    bytes.compact().rewind();
    StringBuilder sb = new StringBuilder();
    while((next = bytes.get()) != ':') {
      sb.append((char) next);
    }
    int len = Integer.parseInt(sb.toString());
    byte[] keyBytes = new byte[len];
    bytes.get(keyBytes);
    return new String(keyBytes);
  }
  
  public static Integer decodeInteger(ByteBuffer bytes) {
    byte next;
    bytes.compact().rewind();
    StringBuilder sb = new StringBuilder();
    while((next = bytes.get()) != 'e') {
      sb.append((char) next);
    }
    return Integer.parseInt(sb.toString());    
  }
  
  public static ArrayList<Object> decodeList(ByteBuffer bytes) {
    ArrayList<Object> list = new ArrayList<Object>();
    bytes.compact().rewind();
    byte next;
    do {
      bytes.rewind();
      Object value = decode(bytes);
      list.add(value);
      bytes.compact().rewind();
      next = bytes.get();      
    } while(next != 'e');
    
    return list;
  }
  // static class
  private BDecoder() { }
}