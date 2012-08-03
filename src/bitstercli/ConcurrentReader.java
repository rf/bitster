package bitstercli;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Non-blocking {@link InputStream} reader.
 * @author Martin Miralles-Cordal
 */
public class ConcurrentReader {
  private BufferedReader s;
  private InputStream is;
  
  public ConcurrentReader(InputStream is) {
    this.is = is;
    s = new BufferedReader(new InputStreamReader(is));
  }
  
  /**
   * Returns the next input scanned in, or null if buffer is empty.
   * @return The next scanned token, or null if no token exists.
   */
  public String next() {    
    try {
      if(is.available() > 0) {
        return s.readLine();
      }
    } catch(Exception e) { /* Don't care */ }
    return null;
  }
  
}
