package bitstergui;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import libbitster.Actor;
import libbitster.BencodingException;
import libbitster.Broker;
import libbitster.Log;
import libbitster.Manager;
import libbitster.Memo;
import libbitster.TorrentInfo;
import libbitster.UserInterface;

/**
 * Singleton. Graphical user interface class.
 * @author Theodore Surgent
 */
public class Gui extends Actor implements UserInterface {
  private ArrayList<Manager> managers; //Downloads
  private HashMap<Manager, Integer> downloadTblRows; //Manager (download) to table row index mapping
  private HashMap<Broker, Integer> peerInfoRows; //Broker (peer) to table row index mapping for selected download
  
  private MainWindow wnd;
  private static Gui instance = null;
  
  private Gui() {
    super();
    managers = new ArrayList<Manager>();
    downloadTblRows = new HashMap<Manager, Integer>();
    peerInfoRows = new HashMap<Broker, Integer>();
    nimbusLookAndFeel();
    wnd = new MainWindow(this);
  }
  
  protected void idle () {
    try { Thread.sleep(100); } catch (Exception e) {}
  }
  
  @Override
  public void addManager(Manager manager) {
    managers.add(manager);
    
    String file = manager.getFileName();
    String status = manager.getLeft() > 0 ? "downloading" : "seeding";
    String size = ((int)((manager.getSize()/1024.0/1024.0)*100))/100.0 + "MB";
    int progress = (100 * manager.getDownloaded()) / manager.getSize();
    int seed = manager.getSeeds();
    int leech = manager.getBrokerCount() - seed;
    double ratio = 0;
    if(manager.getDownloaded() != 0) {
      ratio = ((double) manager.getUploaded()) / manager.getDownloaded();
    }
    
    int index = wnd.tblDls.addRow(file, status, size, progress, seed, leech, ratio);
    downloadTblRows.put(manager, index);

    //Watch download progress
      //manager.watch("bitfield received", this);
      //manager.watch("block fail", this);
      manager.watch("block received", this);
      manager.watch("block sent", this);
      manager.watch("broker added", this);
      //manager.watch("broker choked", this);
      //manager.watch("broker choking", this);
      //manager.watch("broker interested", this);
      //manager.watch("broker interesting", this);
      //manager.watch("numQueued", this);
      //manager.watch("broker state", this);
      //manager.watch("have received", this);
      //manager.watch("piece received", this);
      //manager.watch("resume", this);
  }
  
  protected void receive (Memo memo) {
    if(memo.getType().equals("block sent")) {
      @SuppressWarnings("unchecked")
      HashMap<String, Object> payload = (HashMap<String, Object>)memo.getPayload();
      Manager manager = (Manager)memo.getSender();
      //Broker broker = (Broker)payload.get("broker");
      //int piece = (Integer)payload.get("piece number");
      int uploaded = (Integer)payload.get("uploaded");
      
      int row = downloadTblRows.get(manager);
      
      wnd.tblDls.setRatio(row, ((int)(((double)uploaded / (double)manager.getDownloaded())*100))/100.0);
    }
    else if(memo.getType() == "block received") {
      @SuppressWarnings("unchecked")
      HashMap<String, Object> payload = (HashMap<String, Object>)memo.getPayload();
      Manager manager = (Manager)memo.getSender();
      //Broker broker = (Broker)payload.get("broker");
      //int piece = (Integer)payload.get("piece number");
      int downloaded = (Integer)payload.get("downloaded");
      int left = (Integer)payload.get("left");

      int row = downloadTblRows.get(manager);

      wnd.tblDls.setStatus(row, left > 0 ? "downloading" : "seeding");
      wnd.tblDls.setProgress(row, (int)(((double)downloaded / (double)manager.getSize()) * 100));
      wnd.tblDls.setRatio(row, ((int)(((double)manager.getUploaded() / (double)downloaded)*100))/100.0);
    }
    else if(memo.getType() == "broker added") {
      Manager manager = (Manager)memo.getSender();
      //Broker broker = (Broker)memo.getPayload();
      
      int seed = manager.getSeeds();
      int leech = manager.getBrokerCount() - seed;

      int row = downloadTblRows.get(manager);
      
      wnd.tblDls.setSeed(row, seed);
      wnd.tblDls.setLeech(row, leech);
      
      buildPeerTabelRows(manager);
    }
  }
  
  public static Gui getInstance() {
    if(instance == null) {
      instance = new Gui();
    }
    
    return instance;
  }
  
  public static boolean hasInstance() {
    return instance != null;
  }
  
  //Helper method to build the peer table rows when a different Manager (download) is selected from the downloads list.
  private void buildPeerTabelRows(Manager manager) {
    //Clear out old info
    while(wnd.tblPeers.mdl.getRowCount() > 0)
      wnd.tblPeers.mdl.removeRow(0);
    peerInfoRows.clear();

    // peers list may change when this is called, so we need to clone the list
    @SuppressWarnings("unchecked")
    LinkedList<Broker> peers = (LinkedList<Broker>) manager.getBrokers().clone();
    
    if(peers != null) {
      for(Broker peer : peers) {
        String address = peer.address();
        String state = peer.state();
        int progress = 0;
        if(peer.bitfield() != null)
          progress = (peer.bitfield().cardinality()*100) / manager.getPieceCount();
        String lastSent = "";
        String lastReceived = "";
        boolean choked = peer.choked();
        boolean choking = peer.choking();
        boolean interested = peer.interested();
        boolean interesting = peer.interesting();
        
        int row = wnd.tblPeers.addRow(address, state, progress, lastSent, lastReceived, choked, choking, interested, interesting);

        peerInfoRows.put(peer, row);
      }
    }
  }
  
  public void openFile(File file) {
    String msg;
    
    if(!file.exists()) {
      msg = "Error: " + file.getName() + " is not a file.";
      Log.e(msg);
      JOptionPane.showMessageDialog(wnd, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    try {
      byte[] torrentBytes = new byte[(int) file.length()]; 
      DataInputStream dis;
      dis = new DataInputStream(new FileInputStream(file));
      dis.readFully(torrentBytes);
      dis.close();
      TorrentInfo metainfo = new TorrentInfo(torrentBytes);
      
      // validate metainfo.file_name
      File dest = new File(metainfo.file_name);
      if(!dest.exists()) {
        try {
            // try to create file to validate target name
            dest.createNewFile();
            dest.delete();
        } catch (IOException e) {
          msg = "Error: invalid destination file.";
          Log.e(msg);
          JOptionPane.showMessageDialog(wnd, msg, "Error", JOptionPane.ERROR_MESSAGE);
          return;
        }
      }
      
      Manager manager = new Manager(metainfo, dest, this);
      manager.start();
      
    } catch (IOException e) {
      msg = "Error: unable to read torrent file.";
      Log.e(msg);
      JOptionPane.showMessageDialog(wnd, msg, "Error", JOptionPane.ERROR_MESSAGE);
      return;
    } catch (BencodingException e) {
      msg = "Error: invalid or corrupt torrent file.";
      Log.e(msg);
      JOptionPane.showMessageDialog(wnd, msg, "Error", JOptionPane.ERROR_MESSAGE);
      return;
    }
  }
  
  private void nimbusLookAndFeel() {
    for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
      if ("Nimbus".equals(info.getName())) {
        try { UIManager.setLookAndFeel(info.getClassName()); } catch (Exception e) {} //Don't care
        return;
      }
    }
  }
  
  @Override
  public void shutdown() {
    wnd.dispose();
    super.shutdown();
  }
}
