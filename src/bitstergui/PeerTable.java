package bitstergui;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

final class PeerTable extends JTable {
  private static final long serialVersionUID = 0xDeadFeel;
  private static String[] columns = {};
  DefaultTableModel mdl;
  
  PeerTable() {
    super(new DefaultTableModel(null, columns) {
      private static final long serialVersionUID = 0xDeadFeel;

      @Override
      public boolean isCellEditable(int row, int column) {
         return false;
      }
    });
    
    this.setFillsViewportHeight(true);
    
    mdl = (DefaultTableModel)this.getModel();
  }

}