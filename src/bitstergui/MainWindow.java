package bitstergui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import libbitster.Janitor;

public class MainWindow extends JFrame {

  private static final long serialVersionUID = 0xDEADBEEFL;
  
  JMenuBar mbTopMenu;
  JMenu mnuBitster;
  JMenuItem miAbout;
  JMenuItem miExit;
  
  DownloadsTable tblDls;
  JScrollPane spDls;
  JScrollPane spPeers;
  
  public MainWindow() {
    super("Bitster GUI");
    GridBagLayout gb = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    this.setLayout(gb);
    
    // Menu
      miExit = new JMenuItem("Exit");
      miExit.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent arg0) {
          Janitor.getInstance().start();
        }
      });
      
      miAbout = new JMenuItem("About");
      
      mnuBitster = new JMenu("Bitster");
      mnuBitster.add(miAbout);
      mnuBitster.add(miExit);
      
      mbTopMenu = new JMenuBar();
      mbTopMenu.add(mnuBitster);
      this.setJMenuBar(mbTopMenu);
      
    // Downloads
      tblDls = new DownloadsTable();
      for(int i=0; i < 100; ++i)
        tblDls.addRow("file_"+i+".out", "seeding", "12MB", 45, 2, 8, 0.2);
      
      spDls = new JScrollPane(tblDls);
      spDls.setAutoscrolls(true);
     
      spDls.setBorder(BorderFactory.createTitledBorder("Downloads"));
    
    // Peers
      spPeers = new JScrollPane(new JLabel("To do"));
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
      this.setPreferredSize(new Dimension(800, 600));
      this.pack();
      this.setLocationRelativeTo(null);
      this.setVisible(true);
  }
}
