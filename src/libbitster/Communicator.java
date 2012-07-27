package libbitster;

import java.nio.*;
import java.nio.channels.*;

public interface Communicator {
  public boolean onReadable ();
  public boolean onWritable ();
  public boolean onAcceptable ();
}

