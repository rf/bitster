import java.nio.*;
import java.nio.channels.*;
import java.nio.channels.Pipe.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;

public class SelectableQueue<E> extends ConcurrentLinkedQueue<E> {
  private static final long serialVersionUID = -4032523498234928L; // totes
  private SinkChannel pipe;
  private Communicator handler;
  private Overlord overlord;

  public void register (SinkChannel pipe, Overlord o) {
    this.pipe = pipe;
    this.overlord = o;
  }

  public void register (Communicator handler) { this.handler = handler; }

  @Override
  public boolean offer (E e) {
    try { 
      // Write a single byte to the pipe to inform it that the queue is 'hot'.
      if (pipe != null) {
        pipe.write(ByteBuffer.wrap(new byte[] {0}));
        overlord.offer(handler);
      }
    } catch (IOException exception) {
      // not really sure what to do with this 
    }

    return super.offer(e);
  }
}
