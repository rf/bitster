package bitstercli;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.*;

import libbitster.BencodingException;
import libbitster.Manager;
import libbitster.TorrentInfo;

public class RUBTClient {

  /**
   * @param args Takes in a torrent file and a destination file name as arguments 
   */
  public static void main(String[] args) {
    Logger log = Logger.getLogger("bitster");

    try {
      File f = new File(args[0]);
      byte[] torrentBytes = new byte[(int) f.length()]; 
      DataInputStream dis;
      dis = new DataInputStream(new FileInputStream(f));
      dis.readFully(torrentBytes);
      dis.close();
      TorrentInfo metainfo = new TorrentInfo(torrentBytes);
      Manager manager = new Manager(metainfo);
      manager.start();
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (BencodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

}
