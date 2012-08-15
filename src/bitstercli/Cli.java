package bitstercli;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import libbitster.Actor;
import libbitster.BencodingException;
import libbitster.ConcurrentReader;
import libbitster.Janitor;
import libbitster.Log;
import libbitster.Manager;
import libbitster.Memo;
import libbitster.TorrentInfo;
import libbitster.UserInterface;

/**
 * Singleton. Command line interface class.
 * @author Martin Miralles-Cordal
 */
public class Cli extends Actor implements UserInterface {
  
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
  
  public static boolean hasInstance() {
    return instance != null;
  }
  
  public void printProgress() {
    for(Manager manager : managers) {
      int percentDone = (int)(100*((1.0*manager.getDownloaded())/(manager.getDownloaded() + manager.getLeft())));
      String ratio = String.format("%.2f", (1.0f * manager.getUploaded() / manager.getDownloaded()));
      int numDots = percentDone/2;
      int i;
      
      System.out.print(manager.getFileName() + ": [");
      System.out.print(Log.color(Log.GREEN));
      for(i = 0; i < numDots; i++) System.out.print("=");
      System.out.print(Log.color(Log.RED));
      for( ; i < 50; i++) System.out.print("-");
      System.out.print(Log.color(Log.SANE) + "]" + percentDone + "%" + " [R: " + ratio + "]\n");
    }
  }
  
  public void addManager(Manager manager) { this.managers.add(manager); }
  
  public void openFile(File torrent, File dest) {
    String msg;
    
    if(!torrent.exists()) {
      msg = "Error: " + torrent.getName() + " is not a file.";
      Log.e(msg);
    }
    
    try {
      byte[] torrentBytes = new byte[(int) torrent.length()]; 
      DataInputStream dis;
      dis = new DataInputStream(new FileInputStream(torrent));
      dis.readFully(torrentBytes);
      dis.close();
      TorrentInfo metainfo = new TorrentInfo(torrentBytes);
      
      // if we are already downloading this torrent, skip
      for(Manager m : managers) {
        if (metainfo.info_hash.equals(m.getInfoHash())) {
          msg = "Error: torrent already being downloaded.";
          return;
        }
      }
      
      // validate metainfo.file_name
      dest = new File(metainfo.file_name);
      if(!dest.exists()) {
        try {
            // try to create file to validate target name
            dest.createNewFile();
            dest.delete();
        } catch (IOException e) {
          msg = "Error: invalid destination file.";
          Log.e(msg);
          return;
        }
      }
      
      Manager manager = new Manager(metainfo, dest, this);
      manager.start();
      
    } catch (IOException e) {
      msg = "Error: unable to read torrent file.";
      Log.e(msg);
      return;
    } catch (BencodingException e) {
      msg = "Error: invalid or corrupt torrent file.";
      Log.e(msg);
      return;
    }
  }
}
