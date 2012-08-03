package bitstercli;

import libbitster.Manager;
import libbitster.Memo;
import libbitster.UserInterface;

/**
 * Dummy interface, used when -no-cli flag is set
 * @author Martin Miralles-Cordal
 */
public class RawOutputUI implements UserInterface {
  
  private static RawOutputUI instance;
  
  private RawOutputUI() {}

  public static RawOutputUI getInstance() {
    if(instance == null) {
      instance = new RawOutputUI();
    }
    return instance;
  }
  
  @Override
  public void post(Memo memo) {}

  @Override
  public void start() { }

  @Override
  public void addManager(Manager m) { }

}
