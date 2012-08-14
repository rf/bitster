package bitstercli;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import bitstergui.Gui;

import libbitster.BencodingException;
import libbitster.Janitor;
import libbitster.Log;
import libbitster.Manager;
import libbitster.TorrentInfo;
import libbitster.UserInterface;

/**
 * Driver class for Bitster
 * @author Martin Miralles-Cordal
 */
public class RUBTClient {
  
  private static boolean cli = true;
  private static boolean gui = false;
  
  private static boolean processSwitch(String arg) {
    if(arg.equals("-color")) {
      Log.setColor(true);
    }
    else if(arg.equals("-gui")) {
      gui = true;
      cli = false;
    }
    else if(arg.equals("-no-cli")) {
      cli = false;
    }
    else {
      return false;
    }
    return true;
  }
  
  /**
   * @param args Takes in a torrent file, a destination file name, and switches as arguments 
   */
  public static void main(String[] args) {
    // initialize to minimalist debug output UI by default
    UserInterface ui = RawOutputUI.getInstance();
    
    String argTorrent = null, argDest = null;
    for(int i = 0; i < args.length; i++) {
      if(!processSwitch(args[i])) {
        if(argTorrent == null) {
          argTorrent = args[i];
        }
        else if(argDest == null) {
          argDest = args[i];
        }
      }
    }
    
    // check if we have a valid number of arguments
    if(!gui && argTorrent == null) {
      Log.e("Error: Invalid number of arguments.");
      return;
    }

    // attempt to gracefully shut down from term and interrupt signals
    Runtime.getRuntime().addShutdownHook(new Thread(Janitor.getInstance()));
    
    if(gui && argTorrent == null) {
      try {
        Log.setOutput(new PrintStream(new FileOutputStream("bitster.log")));
      } catch (FileNotFoundException e) {}
      
      Gui.getInstance().start();
    }
    else {
      // validate argTorrent
      File torrentFile = new File(argTorrent);
      if(!torrentFile.exists() || torrentFile.isDirectory()) {
        Log.e("Error: " + argTorrent + " is not a file.");
        return;
      }
      
      try {
        byte[] torrentBytes = new byte[(int) torrentFile.length()]; 
        DataInputStream dis;
        dis = new DataInputStream(new FileInputStream(torrentFile));
        dis.readFully(torrentBytes);
        dis.close();
        TorrentInfo metainfo = new TorrentInfo(torrentBytes);
        
        if(argDest == null) {
          argDest = metainfo.file_name;
        }
        // validate argDest
        File dest = new File(argDest);
        if(!dest.exists()) {
          try {
              // try to create file to validate target name
              dest.createNewFile();
              dest.delete();
          } catch (IOException e) {
            Log.e("Error: invalid destination file.");
            return;
          }
        }
        
        if(cli || gui) {
          if(cli) {
            ui = Cli.getInstance();
          }
          else if(gui) {
            ui = Gui.getInstance();
          }
          Log.setOutput(new PrintStream(new FileOutputStream("bitster.log")));
        }
        
        ui.start();
        
        final Manager manager = new Manager(metainfo, dest, ui);
        manager.start();
      } catch (IOException e) {
        Log.e("Error: unable to read torrent file.");
        return;
      } catch (BencodingException e) {
        Log.e("Error: invalid or corrupt torrent file.");
        return;
      }
    }
  }

}
