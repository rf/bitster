package libbitster;

import java.util.HashSet;
import java.util.Iterator;

/**
 * Singleton. Cleans up the program when shutting down.
 * @author Martin Miralles-Cordal
 */
public class Janitor extends Actor {
  
  private static Janitor instance;
  private String state;
  
  // Hash set of all running managers
  private HashSet<Manager> managers;
  
  protected Janitor() {
    super();
    this.state = "init";
    managers = new HashSet<Manager>();
  }
  
  @Override
  protected void receive (Memo memo) {
    
    // Memo sent when manager is done shutting down
    if(memo.getType().equals("done")) {
      Log.info("Manager shut down.");
      managers.remove(memo.getSender());
    }        
  }
  
  /**
   * Register a {@link Manager} with the {@link Janitor}
   * @param m the {@link Manager} to register
   */
  public void register(Manager m)
  {
    managers.add(m);
  }
  /**
   * When thread starts, send halt message to Managers
   */
  protected void idle () {
    if(state.equals("init")) {
      state = "normal";
      Iterator<Manager> it = managers.iterator();
      while(it.hasNext()) {
        Log.info("Sending halt memo to manager.");
        it.next().post(new Memo("halt", null, this));
      }
    }
    
    if(managers.isEmpty()) {
      Log.info("All managers report done. Shutting down...");
      shutdown();
    }
    try { Thread.sleep(50); } catch (InterruptedException e) { /* don't care */ }
  }
  
  public static Janitor getInstance() {
    if(instance == null) {
      instance = new Janitor();
    }
    
    return instance;
  }

}
