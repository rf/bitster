package bitstergui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;

public class InfoPanel extends JPanel {
  private GridBagLayout gb;
  private GridBagConstraints c;
  
  public InfoPanel() {
    gb = new GridBagLayout();
    c = new GridBagConstraints();
    
    this.setLayout(gb);
  }
}
