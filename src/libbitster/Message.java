package libbitster;

import java.nio.*;
import java.util.BitSet;

public class Message {
  private int length = -1;
  private int blockLength = -1;
  private int type = -1;
  private int begin = -1;
  private int index = -1;
  private BitSet bitfield;
  //BitSet capacity() is a power of 2 it seems and length() is not what we want
  private int bitfieldByteLength;
  ByteBuffer block;

  public static final int KEEP_ALIVE = -1;
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

  /*
   * Creates a message from a ByteBuffer filled with serialized message data
   * @param from The ByteBuffer containing the message data
   */
  public Message (ByteBuffer from) {
    try { //Catch all BufferUnderflowExceptions
      block = from;
      length = from.getInt();
    
      if (length > 0) {
        type = (int) from.get();
  
        switch (type) {
          case HAVE:
            //Good enough for CHOKE, UNCHOKE, INTERESTED, and NOT_INTERTESTED
            index = from.getInt();
          break;
  
          case BITFIELD:
            //Subtract size of message header to get bitfield length in bytes
            bitfieldByteLength = length - 1;
            //Constructor want the number of bits
            bitfield = new BitSet( bitfieldByteLength * 8 );

            //Check each bit of each byte in the buffer and set the bitfield bits appropriately
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
            blockLength = from.getInt();
          break;
  
          case PIECE:
            index = from.getInt();
            begin = from.getInt();
            //Subtract size of message header to get the number of bytes in this piece
            byte[] bytes = new byte[length - 9];
            from.get(bytes, 0, length - 9);
            block = ByteBuffer.wrap(bytes);
          break;
        }
      }
      else
        type = KEEP_ALIVE;
    }
    catch(BufferUnderflowException ex) {
      throw new IllegalArgumentException("Parse error");
    }
  }

  //Use create*() factory methods instead
  private Message (int type) {
    this.type = type;
  }

  /*
   * Serializes this message into a binary format described by
   * http://wiki.theory.org/BitTorrentSpecification#Messages
   * @return A ByteBuffer containing the serialized form of this message
   */
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
        //For each group of 8-bits
        for(int bitOffs = 0, bitLen = bitfieldByteLength * 8; bitOffs < bitLen; bitOffs += 8) {
          
          b = 0;
          //For each bit in this byte
          for(int bit = 0; bit < 8; ++bit) {
            //Set bit in current byte at index bit if set in the bitfield
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
        buff.putInt(blockLength);
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

  /*
   * Returns a string describing this message
   * @return a string describing this message
   */
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

  /*
   * Creates a KEEP_ALIVE message
   */
  public static Message createKeepAlive() {
    return new Message(KEEP_ALIVE);
  }
  
  /*
   * Creates a CHOKE message
   */
  public static Message createChoke() {
    return new Message(CHOKE);
  }

  /*
   * Creates a UNCHOKE message
   */
  public static Message createUnchoke() {
    return new Message(UNCHOKE);
  }

  /*
   * Creates an INTERESTED message
   */
  public static Message createInterested() {
    return new Message(INTERESTED);
  }

  /*
   * Creates a NOT_INTERESTED message
   */
  public static Message createNotInterested() {
    return new Message(NOT_INTERESTED);
  }
  
  /*
   * Creates a HAVE message
   * @param index The piece index
   */
  public static Message createHave (int index) {
    Message msg = new Message(HAVE);
    msg.index = index;
    
    return msg;
  }
  
  /*
   * Creates a BITFIELD message
   * Size is needed because Java appears to allocate memory for a BitSet in powers of 2 (which makes sense BTW)
   * @param bitfield A BitSet where each bit represents whether the corresponding piece is available
   * @param size The size of the bitfield in bytes, NOT in bits (note that all unused bits should not be set)
   */
  public static Message createBitfield(final BitSet bitfield, int size) {
    Message msg = new Message(BITFIELD);
    msg.bitfield = bitfield;
    msg.bitfieldByteLength = size;
    return msg;
  }
  
  /*
   * Creates a REQUEST message
   * @param index The piece index
   * @param begin The byte offset within the piece
   * @param length The number of bytes
   */
  public static Message createRequest (int index, int begin, int length) {
    Message msg = new Message(REQUEST);
    msg.index = index;
    msg.begin = begin;
    msg.blockLength = length;
    
    return msg;
  }

  /*
   * Creates a PIECE message
   * @param index The piece index
   * @param begin The byte offset within the piece
   * @param block The data associated with the piece part
   */
  public static Message createPiece (int index, int begin, final ByteBuffer block) {
    Message msg = new Message(PIECE);
    msg.index = index;
    msg.begin = begin;
    msg.block = block;
    
    return msg;
  }

  //TODO: more create*(), testing
  
  /*
   * Gets the type of this message
   * @return The message type
   */
  public int getType() {
    return type;
  }
  
  /*
   * Gets the length of this entire message (when serialized) in bytes
   * WARNING - This is only valid when a message has been constructed from a ByteBuffer
   * @return The message length
   */
  public int getLength() {
    return length;
  }
  
  /*
   * Returns the length of a block in bytes for a part of piece for REQUEST messages, otherwise -1
   * Note that this method will return -1 for PIECE messages
   * @return The block length within a piece
   * @see getBegin()
   */
  public int getBlockLength() {
    return blockLength;
  }
  
  /*
   * Returns the piece number if this message is of type HAVE, REQUEST, or PIECE, otherwise -1
   * @return The piece index
   */
  public int getIndex() {
    return index;
  }
  
  /*
   * Returns The offset in bytes to the beginning of a block within a piece for REQUEST and PIECE messages, otherwise -1
   * @return The offset to the beginning of a block within a piece
   * @see getBlockLength()
   */
  public int getBegin() {
    return begin;
  }
  
  /*
   * Returns a block of data which can be thought of as a piece of a piece for PIECE messages
   * @return A block of data
   */
  public final ByteBuffer getBlock() {
    return block;
  }
  
  /*
   * Returns the bitfield for BITFIELD messages, otherwise null
   * @return The bitfield
   * @see getBitfieldLength()
   */
  public final BitSet getBitfield() {
    return bitfield;
  }
  
  /*
   * Returns the length of a bitfield for BITFIELD messages in bytes, NOT in bits
   * Unfortunately, a message itself does not know the number of bits
   * This number is equivalent to the following formula: ceiling(bitfieldLengthInBits / 8.0)
   * @return The length of a bitfield in bytes
   * @see getBitfield()
   */
  public int getBitfieldLength() {
    return bitfieldByteLength;
  }
}
