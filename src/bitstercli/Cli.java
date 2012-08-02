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
      int numDots = (int)(50*((1.0*manager.getDownloaded())/(manager.getDownloaded() + manager.getLeft())));
      int i;
      System.out.print(manager.getFileName() + ": |");
      System.out.print(Log.GREEN);
      for(i = 0; i < numDots; i++) System.out.print("=");
      System.out.print(Log.RED);
      for( ; i < 50; i++) System.out.print("-");
      System.out.print(Log.SANE + "|" + numDots*2 + "%\r");
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
