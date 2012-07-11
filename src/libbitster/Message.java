package libbitster;

import java.nio.*;
import java.util.BitSet;

class Message {
  private int length = -1;
  private int type = -1;
  private int begin = -1;
  private int index = -1;
  private BitSet bitfield;
  //BitSet capacity() is a power of 2 it seems and length() is not what we want
  private int bitfieldByteLength;
  ByteBuffer block;

  public static final int CHOKE = 0;
  public static final int UNCHOKE = 1;
  public static final int INTERESTED = 2;
  public static final int NOT_INTERESTED = 3;
  public static final int HAVE = 4;
  public static final int BITFIELD = 5;
  public static final int REQUEST = 6;
  public static final int PIECE = 7;
  
  //toString()
  private static final String[] TYPES = { "CHOKE",
                                          "UNCHOKE",
                                          "INTERESTED", 
                                          "NOT_INTERESTED",
                                          "HAVE",
                                          "BITFIELD",
                                          "REQUEST", 
                                          "PIECE" };

  public Message (ByteBuffer from) {
    try { //Catch all BufferUnderflowExceptions
      block = from;
      length = from.getInt();
    
      if (length > 0) {
        type = (int) from.get();
  
        switch (type) {
          case HAVE:
            index = from.getInt();
          break;
  
          case BITFIELD:
            bitfieldByteLength = length - 1;
            bitfield = new BitSet( bitfieldByteLength * 8 );

            //The most significant bit of the most significant byte is piece 1 (big endian)
            for(int byteOffs = 0; byteOffs < bitfieldByteLength; ++byteOffs) {
              byte currByte = from.get();
              
              for(int bit = 0; bit < 8; ++bit) {
                boolean value = (currByte & (0x80 >> bit)) != 0;
                bitfield.set(byteOffs * 8 + bit, value);
              }
            }
          break;
  
          case REQUEST:
            index = from.getInt();
            begin = from.getInt();
            length = from.getInt();
          break;
  
          case PIECE:
            index = from.getInt();
            begin = from.getInt();
            byte[] bytes = new byte[length - 9];
            from.get(bytes, 0, length - 9);
            block = ByteBuffer.wrap(bytes);
          break;
        }
      }
    }
    catch(BufferUnderflowException ex) {
      throw new IllegalArgumentException("Parse error");
    }
  }

  // keepalive
  public Message () {
    this.type = -1;
  }

  public Message (int type) {
    this.type = type;
  }

  public Message (int type, int begin, int index, int length, ByteBuffer block) {
    this.type = type;
    this.begin = begin;
    this.index = index;
    this.length = length;
    this.block = block;
  }

  public ByteBuffer serialize () {
    ByteBuffer buff = null;
    
    switch (type) {
      // keepalive
      case -1:
        buff = ByteBuffer.allocate(4);
        buff.putInt(0);
      break;

      case CHOKE:
      case UNCHOKE:
      case INTERESTED:
      case NOT_INTERESTED:
        buff = ByteBuffer.allocate(5);
        buff.putInt(1);
        buff.put((byte) type);
      break;

      case HAVE:
        buff = ByteBuffer.allocate(9);
        buff.putInt(5);
        buff.put((byte) type);
        buff.putInt(index);
      break;

      case BITFIELD:
        buff = ByteBuffer.allocate(5 + bitfieldByteLength);
        buff.putInt(1 + bitfieldByteLength);
        buff.put((byte) type);
        
        //Convert bitfield to bytes and append to buff
        byte b;
        for(int bitOffs = 0, bitLen = bitfieldByteLength * 8; bitOffs < bitLen; bitOffs += 8) {
          
          b = 0;
          for(int bit = 0; bit < 8; ++bit) {
            b |= bitfield.get(bitOffs + bit) ? (0x80 >> bit):0;
          }
          
          buff.put(b);
        }
      break;

      case REQUEST:
        buff = ByteBuffer.allocate(17);
        buff.putInt(13);
        buff.put((byte) type);
        buff.putInt(index);
        buff.putInt(begin);
        buff.putInt(length);
      break;

      case PIECE:
        buff = ByteBuffer.allocate(13 + block.limit()); //Limit is, effectively, the size of the buffer
        buff.putInt(9 + block.limit());
        buff.put((byte) type);
        buff.putInt(index);
        buff.putInt(begin);
        buff.put(block);
      break;
    }
    
    buff.rewind();
    return buff;
  }

  public String toString () {
    
    if(type > 7) return "";
    if(type < 0) return "keepalive";
    
    String str = TYPES[type].toLowerCase();
    
    switch(type) {
      case HAVE:
        str += ":" + index;
      break;
  
      case BITFIELD:
        str += ":" + bitfield.toString();
      break;
  
      case REQUEST:
        str += ":" + index + ":" + begin + ":" + length;
      break;
  
      case PIECE:
        str += ":" + index + ":" + begin + ":" + block.toString();
      break;
    }
    
    return str;
  }

  public static Message createHave (int index) {
    Message msg = new Message(HAVE);
    msg.index = index;
    
    return msg;
  }
  
  public static Message createRequest (int index, int begin, int length) {
    Message msg = new Message(REQUEST);
    msg.index = index;
    msg.begin = begin;
    msg.length = length;
    
    return msg;
  }

  public static Message createPiece (int index, int begin, ByteBuffer block) {
    Message msg = new Message(PIECE);
    msg.index = index;
    msg.begin = begin;
    msg.block = block;
    
    return msg;
  }

  //TODO: Getters and setters, more create*(), testing, remove main()
  
  public static void main(String[] args) {
    Message msg = new Message(BITFIELD);
    msg.bitfield = new BitSet(8);
    msg.bitfieldByteLength = 1;
    msg.bitfield.set(7);
    msg.bitfield.set(2);
    msg = new Message(msg.serialize());
    
    System.err.println(msg);
  }
}
