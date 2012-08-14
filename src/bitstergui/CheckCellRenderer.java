package bitstergui;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class CheckCellRenderer extends DefaultTableCellRenderer {
  private static final long serialVersionUID = 0xDeadDeaL;

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    this.setValue((Boolean)value ? "\u2713" : "");
    return this;
  }
}