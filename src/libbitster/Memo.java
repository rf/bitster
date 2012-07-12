package libbitster;

/**
 * The Memo class handles communications between {@link Actor}s. It
 * consists of a {@code String} type and {@code Object} payload.
 * @author Martin Miralles-Cordal
 *
 */
public class Memo {
  private String type;
  private Object payload;
  private Actor sender;
  
  public Memo(String type, Object payload, Actor sender)
  {
    this.type = type;
    this.payload = payload;
    this.sender = sender;
  }
  
  public Actor getSender()
  {
    return sender;
  }
  
  public String getType()
  {
    return type;
  }
  
  public Object getPayload()
  {
    return payload;
  }

  public String toString () {
    return "Memo type: " + type + " payload: " + payload;
  }
}
