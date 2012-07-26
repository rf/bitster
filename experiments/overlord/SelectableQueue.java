import java.nio.*;
import java.nio.channels.*;
import java.nio.channels.Pipe.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;

public class SelectableQueue<E> extends ConcurrentLinkedQueue<E> {
  private static final long serialVersionUID = -4032523498234928L; // totes
  private SinkChannel pipe;

  public void register (SinkChannel pipe) {
    this.pipe = pipe;
  }
  
  @Override
  public boolean offer (E e) {
    try { 
      // Write a single byte to the pipe to inform it that the queue is 'hot'.
      if (pipe != null) pipe.write(ByteBuffer.wrap(new byte[] {0}));
    } catch (IOException exception) {
      // not really sure what to do with this 
    }

    return super.offer(e);
  }
}
