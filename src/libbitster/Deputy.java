package libbitster;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Deputy is the {@link Actor} that communicates with the Tracker.
 * It communicates the list of peers to the Manager upon request.
 * @author Martin Miralles-Cordal
 *
 */
public class Deputy extends Actor {
  
  private URL tracker;

  @Override
  protected void receive (Object memo) {
    if(memo instanceof URL)
    {
      tracker = (URL) memo;
      contactTracker();
    }
  }
  
  /**
   * Sends an HTTP GET request and gets fresh info from the tracker.
   */
  private void contactTracker()
  {
    if(tracker == null)
      return;
    else
    {
      try {
        BufferedReader in = new BufferedReader(
            new InputStreamReader(tracker.openStream()));
        
        // TODO: do the actual HTTP get request
        
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
  
}
