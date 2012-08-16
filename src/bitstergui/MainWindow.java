package bitstergui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import libbitster.Janitor;

/**
 * Main JFrame for bitster GUI
 * @author Theodore Surgent
 * @author Martin Miralles-Cordal
 */
public class MainWindow extends JFrame {

  private static final long serialVersionUID = 0xDEADBEEFL;
  private Gui gui;
  
  JMenuBar mbTopMenu;
    JMenu mnuBitster;
      JMenuItem miAbout;
      JMenuItem miQuit;
    JMenu mnuFile;
      JMenuItem miOpen;
  
  DownloadTable tblDls;
  JScrollPane spDls;
  PeerTable tblPeers;
  JScrollPane spPeers;
  JPopupMenu pmDelete;
  JMenuItem miDelete;
  
  public MainWindow(final Gui gui) {
    super("bitster");
    this.gui = gui;
    GridBagLayout gb = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    this.setLayout(gb);
    
    // Menu
      miQuit = new JMenuItem("Quit");
      miQuit.setAccelerator(KeyStroke.getKeyStroke('q'));
      miQuit.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Janitor.getInstance().start();
        }
      });
      
      miAbout = new JMenuItem("About");
      miAbout.setAccelerator(KeyStroke.getKeyStroke('a'));
      miAbout.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          JOptionPane.showMessageDialog(null, "bitster - a lightweight bittorrent client\n\nCreds:\n\tMartin Miralles-Cordal\n\tRussell Frank\n\tTheodore Surgent", "About", JOptionPane.INFORMATION_MESSAGE);
        }
      });
      mnuBitster = new JMenu("bitster");
      mnuBitster.add(miAbout);
      mnuBitster.add(miQuit);
      
      miOpen = new JMenuItem("Open");
      miOpen.setAccelerator(KeyStroke.getKeyStroke('o'));
      miOpen.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          doOpen();
        }
      });
      mnuFile = new JMenu("File");
      mnuFile.add(miOpen);
      
      mbTopMenu = new JMenuBar();
      mbTopMenu.add(mnuBitster);
      mbTopMenu.add(mnuFile);
      this.setJMenuBar(mbTopMenu);
      
    // Downloads
      tblDls = new DownloadTable();
      spDls = new JScrollPane(tblDls);
      spDls.setBorder(BorderFactory.createTitledBorder("Downloads"));
      tblDls.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
        @Override
        public void valueChanged(ListSelectionEvent e) {
          gui.buildPeerTableRowsBySelected();
        }
      });
      
    //Downloads context menu
      pmDelete = new JPopupMenu();
      miDelete = new JMenuItem("Stop download and remove from list");
      pmDelete.add(miDelete);
      miDelete.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          gui.removeDownload(getSelectedDownloadRowIndex());
          gui.buildPeerTableRowsBySelected();
        }
      });
      
      tblDls.addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          if(SwingUtilities.isRightMouseButton(e))
          {
            pmDelete.show(tblDls, e.getX(), e.getY());
            Point point = e.getPoint();
            int row = tblDls.rowAtPoint(point);
            tblDls.getSelectionModel().setSelectionInterval(row, row);
          }
        }
      });
    
    // Peers
      tblPeers = new PeerTable();
      spPeers = new JScrollPane(tblPeers);
      spPeers.setBorder(BorderFactory.createTitledBorder("Peers"));
      
    // DL/Peer Splitter
      JSplitPane splt = new JSplitPane(JSplitPane.VERTICAL_SPLIT, spDls, spPeers);
      c.weightx = 1;
      c.weighty = 1;
      c.fill = GridBagConstraints.BOTH;
      gb.setConstraints(splt, c);
      this.add(splt);
      splt.setDividerLocation(160);
      
    // Window
      this.setPreferredSize(new Dimension(1000, 600));
      this.pack();
      this.setLocationRelativeTo(null);
      this.setVisible(true);
      this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
      this.addWindowListener(new WindowAdapter(){
        //Window is disposed in Gui.java shutdown()
        @Override
        public void windowClosing(WindowEvent e) {
          Janitor.getInstance().start();
        }
      });
  }
  
  private void doOpen() {
    JFileChooser fc = new JFileChooser();
    FileFilter filter = new FileNameExtensionFilter("Torrent metainfo file", "torrent");
    fc.setFileFilter(filter);
    int result = fc.showOpenDialog(this);
    
    if(result == JFileChooser.APPROVE_OPTION) {
      File file = fc.getSelectedFile();
      gui.openFile(file, null);
    }
  }
  
  public int getSelectedDownloadRowIndex() {
    int idx = tblDls.getSelectedRow();
    
    if(idx < 0) return 0;
    
    return tblDls.convertRowIndexToModel(idx);
  }
}
