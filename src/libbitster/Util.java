import java.nio.charset.*;
import java.nio.*;

public class Util {
  public static ByteBuffer s (String input) {
    try {
      Charset utf8 = Charset.forName("UTF-8");
      byte[] ret = input.getBytes(utf8);
      return ByteBuffer.wrap(ret);
    } catch (UnsupportedCharsetException e) {
      throw new RuntimeException("Your jvm doesn't support UTF-8, which is impossible");
    }
  }
}

