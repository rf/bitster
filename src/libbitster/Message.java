
import java.nio.*;

class Message {
  private int length = -1;
  private int type = -1;
  private int begin = -1;
  private int index = -1;
  ByteBuffer block;

  public static final int CHOKE = 0;
  public static final int UNCHOKE = 1;
  public static final int INTERESTED = 2;
  public static final int NOT_INTERESTED = 3;
  public static final int HAVE = 4;
  public static final int BITFIELD = 5;
  public static final int REQUEST = 6;
  public static final int PIECE = 7;

  public Message (ByteBuffer from) {
    block = from;
    length = from.getInt();
    if (length > 0) {
      type = (int) from.get();

      switch (type) {
        case HAVE:
          index = from.getInt();
        break;

        case BITFIELD:
          // TODO; how will we handle bitfields?
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
          from.get(bytes, 0, length - 9); // TODO: handle underflow
          block = ByteBuffer.wrap(bytes);
        break;
      }
    }
  }

  public Message (int type) {

  }

  public Message (int type, int begin, int index, int length, ByteBuffer block) {

  }

  public ByteBuffer serialize () {
    return null;
  }

  public String toString () {
    return "Message, length: " + length + " type: " + type;
  }

  public static Message createHave (int index) {
    return null;
  }

  public static Message createRequest (int index, int begin, int length) {
    return null;
  }

  public static Message createPiece (int index, int begin, int block) {
    return null;
  }

}
