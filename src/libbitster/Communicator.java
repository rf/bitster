package libbitster;

public interface Communicator {
  public boolean onReadable ();
  public boolean onWritable ();
  public boolean onAcceptable ();
  public boolean onConnectable ();
}

