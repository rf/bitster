
import java.nio.*;

class Message {
  private int length;
  private int type;
  private int begin;
  private int index;
  ByteBuffer block;

  public Message (ByteBuffer from) {

  }

  public Message (int type) {

  }

  public Message (int type, int begin, int index, int length, ByteBuffer block) {

  }

  public ByteBuffer serialize () {
    return null;
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
