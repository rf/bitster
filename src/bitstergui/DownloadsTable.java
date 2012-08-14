package bitstergui;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

final class DownloadsTable extends JTable {
  private static final long serialVersionUID = 0xDeadFeel;
  private static String[] columns = {"File","Status","Size","Progress","Seeds","Leechers","Ratio"};
  DefaultTableModel mdl;
  
  DownloadsTable() {
    super(new DefaultTableModel(null, columns) {
      private static final long serialVersionUID = 0xDeadFeel;

      @Override
      public boolean isCellEditable(int row, int column) {
         return false;
      }
      
      @Override
      public int getColumnCount() {
        return 7;
      }
    });
    
    this.setAutoCreateRowSorter(true);
    this.setFillsViewportHeight(true);
    this.getColumnModel().getColumn(3).setCellRenderer(new ProgressBarCellRenderer());
    
    mdl = (DefaultTableModel)this.getModel();
  }
  
  int addRow(String file, String status, String size, int progress, int seed, int leech, double ratio) {
    Object[] row = {file, status, size, progress, seed, leech, ratio};
    mdl.addRow(row);
    return this.getRowCount() - 1;
  }
  
  void setRow(int row, String file, String status, String size, int progress, int seed, int leech, double ratio) {
    mdl.setValueAt(file, row, 0);
    mdl.setValueAt(status, row, 1);
    mdl.setValueAt(size, row, 2);
    mdl.setValueAt(progress, row, 3);
    mdl.setValueAt(seed, row, 4);
    mdl.setValueAt(leech, row, 5);
    mdl.setValueAt(ratio, row, 6);
  }
  
  String getFile(int row) {
    return (String)mdl.getValueAt(row, 0);
  }
  
  String getStatus(int row) {
    return (String)mdl.getValueAt(row, 1);
  }
  
  String getSize(int row) {
    return (String)mdl.getValueAt(row, 2);
  }
  
  int getProgress(int row) {
    return (Integer)mdl.getValueAt(row, 3);
  }
  
  int getSeed(int row) {
    return (Integer)mdl.getValueAt(row, 4);
  }
  
  int getLeech(int row) {
    return (Integer)mdl.getValueAt(row, 5);
  }
  
  double getRatio(int row) {
    return (Double)mdl.getValueAt(row, 6);
  }
}
