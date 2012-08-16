package bitstergui;

import java.awt.Component;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

@SuppressWarnings("serial")
class ProgressBarCellRenderer extends JProgressBar implements TableCellRenderer {
  ProgressBarCellRenderer() {
    this.setMinimum(0);
    this.setMaximum(100);
    this.setBorderPainted(false);
    this.setStringPainted(true);
  }
  
  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    if(value instanceof Integer) {
      this.setValue((Integer)value);
      this.setBackground(table.getBackground());
    }
    return this;
  }
}