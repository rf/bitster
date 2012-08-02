package bitstercli;

import libbitster.Log;
import libbitster.Manager;

public class Cli extends Thread {
  
  private boolean running;
  private static Cli instance = null;
  private Manager manager;
  
  private Cli() { running = true; }
  
  public void run() {
    System.out.println("Welcome to Bitster! Press Ctrl+C to quit.");
    System.out.println("-----------------------------------------");
    while(running) {
      int percentDone = (int)(100*((1.0*manager.getDownloaded())/(manager.getDownloaded() + manager.getLeft())));
      String ratio = String.format("%.2f", (1.0f * manager.getUploaded() / manager.getDownloaded()));
      int numDots = percentDone/2;
      int i;
      
      System.out.print(manager.getFileName() + ": [");
      System.out.print(Log.GREEN);
      for(i = 0; i < numDots; i++) System.out.print("=");
      System.out.print(Log.RED);
      for( ; i < 50; i++) System.out.print("-");
      System.out.print(Log.SANE + "]" + percentDone + "%" + " [R: " + ratio + "]\r");
      
      try { Thread.sleep(100); } catch (InterruptedException e) {}
    }
    System.out.println();
  }
  
  public static Cli getInstance() {
    if(instance == null) {
      instance = new Cli();
    }
    
    return instance;
  }

  public void setManager(Manager manager) { this.manager = manager; }
  
  public void shutdown() { this.running = false; }
}
