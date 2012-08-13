package bitstergui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;

import libbitster.Janitor;

public class MainWindow extends JFrame {

  private static final long serialVersionUID = 0xDEADBEEFL;
  
  private JMenuBar menuBar;
  private JMenu bitsterMenu;
  private JMenuItem exitMenuItem;
  
  private JScrollPane spDls;
  private JTable tblDls;
  
  public MainWindow() {
    super("Bitster GUI");
    
    exitMenuItem = new JMenuItem("Exit");
    exitMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent arg0) {
        Janitor.getInstance().start();
      }
    });
    bitsterMenu = new JMenu("Bitster");
    bitsterMenu.add(exitMenuItem);
    menuBar = new JMenuBar();
    menuBar.add(bitsterMenu);
    this.setJMenuBar(menuBar);
    
    GridBagLayout gb = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    this.setLayout(gb);
    
    JPanel panel = new JPanel();
    panel.add(new JLabel("test"));
    
    String[] columns = {"File","Status","Size","Progress","Seeds","Leechers","Ratio"};
    Object[][] data = {
        {"File.out", "Leeching", "1.1MB", "33%", 2, 8, 0.5}
     };
    tblDls = new JTable(data, columns);
    tblDls.setFillsViewportHeight(true);
    spDls = new JScrollPane(tblDls);
    c.weightx = 1.0;
    c.weighty = 1.0;
    gb.setConstraints(spDls, c);
    spDls.setPreferredSize(new Dimension(400, 200));
    this.add(spDls);
    
    this.setPreferredSize(new Dimension(800, 600));
    this.pack();
    this.setLocationRelativeTo(null);
    this.setVisible(true);
  }
}
