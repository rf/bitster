package bitstercli;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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
    if(args.length != 2)
    {
      Log.e("Error: Invalid number of arguments.");
      return;
    }
    
    // validate argument 1
    File torrentFile = new File(args[0]);
    if(!torrentFile.exists() || torrentFile.isDirectory())
    {
      Log.e("Error: " + args[0] + " is not a file.");
      return;
    }
    
    // validate argument 2
    File dest = new File(args[1]);
    if(!dest.exists())
    {
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
      
      // attempt to gracefully shut down from term and interrupt signals
      Runtime.getRuntime().addShutdownHook(new Thread(Janitor.getInstance()));
      
      final Manager manager = new Manager(metainfo, dest);
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
