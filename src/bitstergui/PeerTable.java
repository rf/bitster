package bitstergui;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * Table of peers
 * @author Theodore Surgent
 */
final class PeerTable extends JTable {
  private static final long serialVersionUID = 0xDeadFeel;
  private static String[] columns = {"Address", "State", "Progress", "Last Sent", "Last Received", "Choked", "Is Choking", "Interested", "Is Interesting"};
  DefaultTableModel mdl;
  
  PeerTable() {
    super(new DefaultTableModel(null, columns) {
      private static final long serialVersionUID = 0xDeadFeel;

      @Override
      public boolean isCellEditable(int row, int column) {
         return false;
      }
      
      @Override
      public int getColumnCount() {
        return 9;
      }
    });

    this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    this.setAutoCreateRowSorter(true);
    this.setFillsViewportHeight(true);
    this.getColumnModel().getColumn(2).setCellRenderer(new ProgressBarCellRenderer());
    CheckCellRenderer ccr = new CheckCellRenderer();
    for(int i=5; i<=8; ++i)
      this.getColumnModel().getColumn(i).setCellRenderer(ccr);
    
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
  
  //Getters
  String getAddress(int row){
    return (String)mdl.getValueAt(row, 0);
  }
  
  String getState(int row){
    return (String)mdl.getValueAt(row, 1);
  }
  
  int getProgress(int row){
    return (Integer)mdl.getValueAt(row, 2);
  }
  
  String getLastSent(int row){
    return (String)mdl.getValueAt(row, 3);
  }
  
  String getLastReceived(int row){
    return (String)mdl.getValueAt(row, 4);
  }
  
  boolean getChoked(int row){
    return (Boolean)mdl.getValueAt(row, 5);
  }
  
  boolean getChoking(int row){
    return (Boolean)mdl.getValueAt(row, 6);
  }
  
  boolean getInterested(int row){
    return (Boolean)mdl.getValueAt(row, 7);
  }
  
  boolean getInteresting(int row){
    return (Boolean)mdl.getValueAt(row, 8);
  }
  
  //Setters
  void setAddress(int row, String address){
    mdl.setValueAt(address, row, 0);
  }
  
  void setState(int row, String state){
    mdl.setValueAt(state, row, 1);
  }
  
  void setProgress(int row, int progress){
    mdl.setValueAt(progress, row, 2);
  }
  
  void setLastSent(int row, String lastSent){
    mdl.setValueAt(lastSent, row, 3);
  }
  
  void setLastReceived(int row, String lastReceived){
    mdl.setValueAt(lastReceived, row, 4);
  }
  
  void setChoked(int row, boolean choked){
    mdl.setValueAt(choked, row, 5);
  }
  
  void setChoking(int row, boolean choking){
    mdl.setValueAt(choking, row, 6);
  }
  
  void setInterested(int row, boolean interested){
    mdl.setValueAt(interested, row, 7);
  }
  
  void setInteresting(int row, boolean interesting){
    mdl.setValueAt(interesting, row, 8);
  }
}
