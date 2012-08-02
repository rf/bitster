package bitstercli;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import libbitster.BencodingException;
import libbitster.Janitor;
import libbitster.Log;
import libbitster.Manager;
import libbitster.TorrentInfo;

/**
 * Driver class for Bitster
 * @author Martin Miralles-Cordal
 */
public class RUBTClient {

  /**
   * @param args Takes in a torrent file and a destination file name as arguments 
   */
  public static void main(String[] args) {    
    // check if we have a valid number of arguments
    if(args.length < 2) {
      Log.e("Error: Invalid number of arguments.");
      return;
    }
    
    // get any switches and parse them
    if(args.length > 2) {
      List<String> switches = Arrays.asList(args);
      if(switches.contains("-color")) {
        Log.setColor(true);
      }
    }
    
    // get the torrent file and destination file
    final String argTorrent = args[args.length-2];
    final String argDest = args[args.length-1];
    
    // validate argument 1
    File torrentFile = new File(argTorrent);
    if(!torrentFile.exists() || torrentFile.isDirectory()) {
      Log.e("Error: " + argTorrent + " is not a file.");
      return;
    }
    
    // validate argument 2
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
    
    try {
      byte[] torrentBytes = new byte[(int) torrentFile.length()]; 
      DataInputStream dis;
      dis = new DataInputStream(new FileInputStream(torrentFile));
      dis.readFully(torrentBytes);
      dis.close();
      TorrentInfo metainfo = new TorrentInfo(torrentBytes);
      
      Log.setOutput(new PrintStream(new FileOutputStream("bitster.log")));
            
      // attempt to gracefully shut down from term and interrupt signals
      Runtime.getRuntime().addShutdownHook(new Thread(Janitor.getInstance()));
      
      final Manager manager = new Manager(metainfo, dest);
      Cli.getInstance().setManager(manager);
      manager.start();
      Cli.getInstance().start();
    } catch (IOException e) {
      Log.e("Error: unable to read torrent file.");
      return;
    } catch (BencodingException e) {
      Log.e("Error: invalid or corrupt torrent file.");
      return;
    }
  }

}
