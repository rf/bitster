package bitstergui;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

final class PeerTable extends JTable {
  private static final long serialVersionUID = 0xDeadFeel;
  private static String[] columns = {"Address", "State", "Progress", "Last Piece Sent", "Last Piece Received", "Choked", "Is Choking", "Interested", "Is Interesting"};
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
    this.getColumnModel().getColumn(2).setCellRenderer(new ProgressBarCellRenderer());
    
    mdl = (DefaultTableModel)this.getModel();
  }
  
  public int addRow(String address,
                    String state,
                    int progress,
                    String lastSent,
                    String lastReceived,
                    boolean choked,
                    boolean choking,
                    boolean interested,
                    boolean interesting) {
    Object[] row = {address, state, progress, lastSent, lastReceived, choked, choking, interested, interesting};
    mdl.addRow(row);
    return this.getRowCount() - 1;
  }
  
  public void setRow(int row, 
                     String address,
                     String state,
                     int progress,
                     String lastSent,
                     String lastReceived,
                     boolean choked,
                     boolean choking,
                     boolean interested,
                     boolean interesting) {
    mdl.setValueAt(address, row, 0);
    mdl.setValueAt(state, row, 1);
    mdl.setValueAt(progress, row, 2);
    mdl.setValueAt(lastSent, row, 3);
    mdl.setValueAt(lastReceived, row, 4);
    mdl.setValueAt(choked, row, 5);
    mdl.setValueAt(choking, row, 6);
    mdl.setValueAt(interested, row, 7);
    mdl.setValueAt(interesting, row, 8);
  }
}
