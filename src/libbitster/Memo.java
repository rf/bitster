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
  
  public Memo(String type, Object payload)
  {
    this.type = type;
    this.payload = payload;
  }
  
  public String getType()
  {
    return type;
  }
  
  public Object getPayload()
  {
    return payload;
  }

}
