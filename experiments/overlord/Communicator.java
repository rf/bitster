import java.nio.*;
import java.nio.channels.*;

public interface Communicator {
  public void onReadable ();
  public void onWritable ();
  public void onAcceptable ();
  public void onMemo ();
}

