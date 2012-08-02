import java.nio.*;
import java.nio.channels.*;
import java.nio.channels.Pipe.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;

public class SelectableQueue<E> extends ConcurrentLinkedQueue<E> {
  private static final long serialVersionUID = -4032523498234928L; // totes
  private Communicator handler;
  private Overlord overlord;

  public void register (Overlord o, Communicator handler) {
    this.overlord = o;
    this.handler = handler;
  }

  @Override
  public boolean offer (E e) {
    // Interrupt the overlord's selection and give it our handler as he needs
    // to be notified
    if (overlord != null) {
      overlord.interrupt();
      overlord.offer(handler);
    }

    return super.offer(e);
  }
}
