package bitstercli;

import java.util.ArrayList;
import java.util.Scanner;

import libbitster.Janitor;
import libbitster.Log;
import libbitster.Manager;

/**
 * Singleton. Command line interface class.
 * @author Martin Miralles-Cordal
 */
public class Cli extends Thread {
  
  private boolean running;
  private static Cli instance = null;
  private ArrayList<Manager> managers;
  
  private Cli() {
    running = true;
    managers = new ArrayList<Manager>();
    }
  
  public void run() {
    System.out.println("Welcome to Bitster! Press Ctrl+C to quit.");
    System.out.println("-----------------------------------------");
    Scanner s = new Scanner(System.in);
    while(running) {
      System.out.print("bitster]> ");
      String in = s.next();
      if(in.equals("quit")) {
        Janitor.getInstance().start();
        shutdown();
      }
      else if(in.equals("status")) {
        printProgress();
      }
      else {
        System.out.println("Usage instructions:");
        System.out.println("status - shows download status");
        System.out.println("quit - quits bitster");
      }
      try { Thread.sleep(100); } catch (InterruptedException e) {}
    }
    s.close();
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
  
  public void shutdown() { this.running = false; }
}
