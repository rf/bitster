package libbitster;

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
