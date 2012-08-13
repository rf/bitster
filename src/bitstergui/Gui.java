package bitstergui;

import java.util.ArrayList;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;

import libbitster.Actor;
import libbitster.Manager;
import libbitster.UserInterface;

/**
 * Singleton. Graphical user interface class.
 * @author Theodore Surgent
 */
public class Gui extends Actor implements UserInterface {
  private ArrayList<Manager> managers;
  private MainWindow wnd;
  private static Gui instance = null;
  
  private Gui() {
    super();
    managers = new ArrayList<Manager>();
    nimbusLookAndFeel();
    wnd = new MainWindow();
  }
  
  protected void idle () {
    try { Thread.sleep(100); } catch (Exception e) {}
  }
  
  @Override
  public void addManager(Manager manager) {
    managers.add(manager);
  }
  

  public static Gui getInstance() {
    if(instance == null) {
      instance = new Gui();
    }
    
    return instance;
  }
  
  public static boolean hasInstance() {
    return instance != null;
  }
  
  private void nimbusLookAndFeel() {
    for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
      if ("Nimbus".equals(info.getName())) {
        try { UIManager.setLookAndFeel(info.getClassName()); } catch (Exception e) {} //Don't care
        return;
      }
    }
  }
}
