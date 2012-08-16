package libbitster;

import java.io.File;

/**
 * Stub interface where the implementing class declares itself
 * a user interface.
 * @author Martin Miralles-Cordal
 */
public interface UserInterface {
  public void post (Memo memo);
  public void start();
  public void addManager(Manager m);
  public void openFile(File torrent, File dest);
}
