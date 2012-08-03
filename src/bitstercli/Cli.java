package bitstercli;

import java.util.ArrayList;

import libbitster.Actor;
import libbitster.ConcurrentReader;
import libbitster.Janitor;
import libbitster.Log;
import libbitster.Manager;
import libbitster.Memo;

/**
 * Singleton. Command line interface class.
 * @author Martin Miralles-Cordal
 */
public class Cli extends Actor {
  
  private static Cli instance = null;
  private ArrayList<Manager> managers;
  private String prompt = "bitster]> ";
  private String state;
  private ConcurrentReader s;
  
  private Cli() {
    super();
    state = "init";
    managers = new ArrayList<Manager>();
    s = new ConcurrentReader(System.in);
  }
  
  protected void receive (Memo memo) {
    if(memo.getType().equals("done") && memo.getSender() instanceof Manager) {
      Manager m = (Manager) memo.getSender();
      System.out.println("\n" + m.getFileName() + " complete!");
      System.out.print(prompt);
    }
  }
  
  public void idle() {
    if(state.equals("init")) {
      System.out.println("Welcome to Bitster! Type \"quit\" to quit.");
      System.out.println("------------------------------------------");
      System.out.print(prompt);
      state = "running";
    }
    String in = s.readLine();
    if(in != null) {
      if(in.equals("quit")) {
        Janitor.getInstance().start();
        return;
      }
      else if(in.equals("status")) {
        printProgress();
      }
      else {
        System.out.println("Usage instructions:");
        System.out.println("status - shows download status");
        System.out.println("quit - quits bitster");
      }
      System.out.print(prompt);
    }
    try { Thread.sleep(100); } catch (InterruptedException e) {}
  }
  
  public static Cli getInstance() {
    if(instance == null) {
      instance = new Cli();
    }
    
    return instance;
  }
  
  public void printProgress() {
    for(Manager manager : managers) {
      int percentDone = (int)(100*((1.0*manager.getDownloaded())/(manager.getDownloaded() + manager.getLeft())));
      String ratio = String.format("%.2f", (1.0f * manager.getUploaded() / manager.getDownloaded()));
      int numDots = percentDone/2;
      int i;
      
      System.out.print(manager.getFileName() + ": [");
      System.out.print(Log.green());
      for(i = 0; i < numDots; i++) System.out.print("=");
      System.out.print(Log.red());
      for( ; i < 50; i++) System.out.print("-");
      System.out.print(Log.sane() + "]" + percentDone + "%" + " [R: " + ratio + "]\n");
    }
  }
  
  public void addManager(Manager manager) { this.managers.add(manager); }
  
  public void shutdown() {
    super.shutdown();
  }
}
