package libbitster;

import java.util.HashSet;
import java.util.Iterator;

import bitstercli.Cli;
import bitstergui.Gui;

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
    else if(memo.getType().equals("kill")) {
      managers.clear();
      Log.error("Timeout reached. Force quitting...");
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
  
  public void unregister(Manager m)
  {
    managers.remove(m);
    Log.info("Sending halt memo to manager.");
    m.post(new Memo("halt", null, this));
  }
  
  /**
   * When thread starts, send halt message to Managers
   */
  protected void idle () {
    // if we've already shut down
    if(state.equals("done")) {
      shutdown();
      return;
    }
    
    if(state.equals("init")) {
      state = "normal";
      if(Cli.hasInstance())
        Cli.getInstance().shutdown();
      if(Gui.hasInstance())
        Gui.getInstance().shutdown();
      synchronized (managers) {
        Iterator<Manager> it = managers.iterator();
        while(it.hasNext()) {
          try { //Fix this sometimes get exception
            unregister(it.next());
          } catch (Exception e) {}
        }
      }
      Util.setTimeout(10000, new Memo("kill", null, this));
    }
    
    if(managers.isEmpty()) {
      Log.info("All managers report done. Shutting down...");
      this.state = "done";
      Util.shutdown();
      BitsterInfo.getInstance().shutdown();
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
