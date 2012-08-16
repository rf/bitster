package bitstergui;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import libbitster.Actor;
import libbitster.BencodingException;
import libbitster.BitsterInfo;
import libbitster.Broker;
import libbitster.Janitor;
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
  private HashMap<Manager, Integer> managerToRowIndex;
  private HashMap<Integer, Manager> rowIndexToManager;
  private HashMap<Broker, Integer> brokerToRowIndex;
  private HashMap<Integer, Broker> rowIndexToBroker;
  
  private MainWindow wnd;
  private static Gui instance = null;
  
  private Gui() {
    super();
    managers = new ArrayList<Manager>();
    managerToRowIndex = new HashMap<Manager, Integer>();
    rowIndexToManager = new HashMap<Integer, Manager>();
    brokerToRowIndex = new HashMap<Broker, Integer>();
    rowIndexToBroker = new HashMap<Integer, Broker>();
    nimbusLookAndFeel();
    wnd = new MainWindow(this);
    
    Set<Entry<File, File>> downloads = BitsterInfo.getInstance().getDownloads();
    Entry<File, File> download;
    synchronized (downloads) {
      Iterator<Entry<File, File>> it = downloads.iterator();
      while(it.hasNext()) {
        download = it.next();
        openFile(download.getValue(), download.getKey());
      }
    }
  }
  
  protected void idle () {
    try { Thread.sleep(50); } catch (Exception e) {}
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
    managerToRowIndex.put(manager, index);
    rowIndexToManager.put(index, manager);

    //Watch download progress
      manager.watch("bitfield received", this);
      manager.watch("block received", this);
      manager.watch("block sent", this);
      manager.watch("broker added", this);
      manager.watch("broker choked", this);
      manager.watch("broker choking", this);
      manager.watch("broker interested", this);
      manager.watch("broker interesting", this);
      manager.watch("broker state", this);
      manager.watch("have received", this);
  }
  
  protected void receive (Memo memo) {
    if(memo.getType().equals("block sent")) {
      @SuppressWarnings("unchecked")
      HashMap<String, Object> payload = (HashMap<String, Object>)memo.getPayload();
      Manager manager = (Manager)memo.getSender();
      int uploaded = (Integer)payload.get("uploaded");
      
      int row = managerToRowIndex.get(manager);
      if(row >= 0)
        wnd.tblDls.setRatio(row, ((int)(((double)uploaded / (double)manager.getDownloaded())*100))/100.0);
    }
    else if(memo.getType() == "block received") {
      @SuppressWarnings("unchecked")
      HashMap<String, Object> payload = (HashMap<String, Object>)memo.getPayload();
      Manager manager = (Manager)memo.getSender();
      int downloaded = (Integer)payload.get("downloaded");
      int left = (Integer)payload.get("left");

      int row = managerToRowIndex.get(manager);

      if(row >= 0) {
        wnd.tblDls.setStatus(row, left > 0 ? "downloading" : "seeding");
        wnd.tblDls.setProgress(row, (int)(((double)downloaded / (double)manager.getSize()) * 100));
        wnd.tblDls.setRatio(row, ((int)(((double)manager.getUploaded() / (double)downloaded)*100))/100.0);
      }
    }
    else if(memo.getType() == "broker added") {
      Manager manager = (Manager)memo.getSender();
      Broker broker = (Broker)memo.getPayload();
      
      int seed = manager.getSeeds();
      int leech = manager.getBrokerCount() - seed;

      int row = managerToRowIndex.get(manager);

      if(row >= 0) {
        wnd.tblDls.setSeed(row, seed);
        wnd.tblDls.setLeech(row, leech);
      
        if(managerSelected(manager))
          addPeer(manager, broker);
      }
    }
    else if(memo.getType().equals("broker choked")) {
      Manager manager = (Manager)memo.getSender();
      @SuppressWarnings("unchecked")
      HashMap<String, Object> info = (HashMap<String, Object>) memo.getPayload();
        Broker broker = (Broker)info.get("broker");
        boolean choked = (Boolean)info.get("choked");
        
      if(managerSelected(manager)) {
        Integer row = brokerToRowIndex.get(broker);
        if(row != null) { //Should not be the case though
          wnd.tblPeers.setChoked(row, choked);
        }
      }
    }
    else if(memo.getType().equals("broker choking")) {
      Manager manager = (Manager)memo.getSender();
      @SuppressWarnings("unchecked")
      HashMap<String, Object> info = (HashMap<String, Object>) memo.getPayload();
        Broker broker = (Broker)info.get("broker");
        boolean choking = (Boolean)info.get("choking");
        
      if(managerSelected(manager)) {
        Integer row = brokerToRowIndex.get(broker);
        if(row != null) { //Should not be the case though
          wnd.tblPeers.setChoking(row, choking);
        }
      }
    }
    else if(memo.getType().equals("broker interested")) {
      Manager manager = (Manager)memo.getSender();
      @SuppressWarnings("unchecked")
      HashMap<String, Object> info = (HashMap<String, Object>) memo.getPayload();
        Broker broker = (Broker)info.get("broker");
        boolean interested = (Boolean)info.get("interested");
        
      if(managerSelected(manager)) {
        Integer row = brokerToRowIndex.get(broker);
        if(row != null) { //Should not be the case though
          wnd.tblPeers.setInterested(row, interested);
        }
      }
    }
    else if(memo.getType().equals("broker interesting")) {
      Manager manager = (Manager)memo.getSender();
      @SuppressWarnings("unchecked")
      HashMap<String, Object> info = (HashMap<String, Object>) memo.getPayload();
        Broker broker = (Broker)info.get("broker");
        boolean interesting = (Boolean)info.get("interesting");
        
      if(managerSelected(manager)) {
        Integer row = brokerToRowIndex.get(broker);
        if(row != null) { //Should not be the case though
          wnd.tblPeers.setInteresting(row, interesting);
        }
      }
    }
    else if(memo.getType().equals("bitfield received")) {
      Manager manager = (Manager)memo.getSender();
      @SuppressWarnings("unchecked")
      HashMap<String, Object> info = (HashMap<String, Object>) memo.getPayload();
        Broker broker = (Broker)info.get("broker");
        BitSet field = (BitSet)info.get("field");
        
      if(managerSelected(manager)) {
        Integer row = brokerToRowIndex.get(broker);
        if(row != null) { //Should not be the case though
          wnd.tblPeers.setAvailable(row, (100*field.cardinality())/manager.getPieceCount());
        }
      }
    }
    else if(memo.getType().equals("have received")) {
      Manager manager = (Manager)memo.getSender();
      @SuppressWarnings("unchecked")
      HashMap<String, Object> info = (HashMap<String, Object>) memo.getPayload();
        Broker broker = (Broker)info.get("broker");

      int seed = manager.getSeeds();
      int leech = manager.getBrokerCount() - seed;
      wnd.tblDls.setSeed(managerToRowIndex.get(manager),seed);
      wnd.tblDls.setLeech(managerToRowIndex.get(manager),leech);
        
      if(managerSelected(manager)) {
        Integer row = brokerToRowIndex.get(broker);
        if(row != null) { //Should not be the case though
          wnd.tblPeers.setAvailable(row, (100*broker.bitfield().cardinality())/manager.getPieceCount());
        }
      }
    }
    else if(memo.getType().equals("broker state")) {
      Manager manager = (Manager)memo.getSender();
      @SuppressWarnings("unchecked")
      HashMap<String, Object> info = (HashMap<String, Object>) memo.getPayload();
        Broker broker = (Broker)info.get("broker");
        String state = (String)info.get("state");

        int seed = manager.getSeeds();
        int leech = manager.getBrokerCount() - seed;
        wnd.tblDls.setSeed(managerToRowIndex.get(manager),seed);
        wnd.tblDls.setLeech(managerToRowIndex.get(manager),leech);
        
      if(managerSelected(manager)) {
        Integer row = brokerToRowIndex.get(broker);
        if(row != null) { //Should not be the case though
          wnd.tblPeers.setState(row, state);
        }
      }
    }
  }

  public boolean managerSelected(Manager manager) {
    return managerToRowIndex.containsKey(manager) &&
    managerToRowIndex.get(manager) == wnd.getSelectedDownloadRowIndex();
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
  
  public void addPeer(Manager manager, Broker peer) {
    String address = peer.address();
    String state = peer.state();
    int progress = 0;
    if(peer.bitfield() != null)
      progress = (peer.bitfield().cardinality()*100) / manager.getPieceCount();
    boolean choked = peer.choked();
    boolean choking = peer.choking();
    boolean interested = peer.interested();
    boolean interesting = peer.interesting();
    
    int row = wnd.tblPeers.addRow(address, state, progress, choked, choking, interested, interesting);
  
    brokerToRowIndex.put(peer, row);
    rowIndexToBroker.put(row, peer);
  }
  
  public void buildPeerTableRowsBySelected() {
    int row = wnd.getSelectedDownloadRowIndex();
    if(row < 0 || row >= rowIndexToManager.size()) {
      buildPeerTableRows(null);
      return;
    }
    
    Manager manager = rowIndexToManager.get(row);
    buildPeerTableRows(manager);
  }
  
  //Helper method to build the peer table rows when a different Manager (download) is selected from the downloads list.
  public void buildPeerTableRows(Manager manager) {
    //Clear out old info
    while(wnd.tblPeers.mdl.getRowCount() > 0)
      wnd.tblPeers.mdl.removeRow(0);
    brokerToRowIndex.clear();
    rowIndexToBroker.clear();
    
    if(manager == null)
      return;
    
    // peers list may change when this is called, so we need to clone the list
    @SuppressWarnings("unchecked")
    LinkedList<Broker> peers = (LinkedList<Broker>) manager.getBrokers().clone();
    
    if(peers != null) {
      for(Broker peer : peers) {
        addPeer(manager, peer);
      }
    }
  }
  
  public void openFile(File file, File dest) {
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
      
   // if we are already downloading this torrent, skip
      for(Manager m : managers) {
        if (metainfo.info_hash.equals(m.getInfoHash())) {
          msg = "Error: torrent already being downloaded.";
          JOptionPane.showMessageDialog(wnd, msg, "Error", JOptionPane.ERROR_MESSAGE);
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
          JOptionPane.showMessageDialog(wnd, msg, "Error", JOptionPane.ERROR_MESSAGE);
          return;
        }
      }
      
      Manager manager = new Manager(metainfo, dest, this);
      manager.start();
      
      BitsterInfo.getInstance().addDownload(dest, file);
      
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
  
  public void removeDownload(int row) {
    Manager manager = rowIndexToManager.get(row);
    
    if(manager != null) {
      BitsterInfo.getInstance().removeDownload(new File(manager.getFileName()));
      Janitor.getInstance().unregister(manager);
      wnd.tblDls.mdl.removeRow(row);
      rowIndexToManager.remove(row);
      managerToRowIndex.remove(manager);
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
