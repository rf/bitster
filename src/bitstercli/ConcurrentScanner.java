package bitstercli;

import java.io.InputStream;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ConcurrentScanner implements Runnable {
  private ConcurrentLinkedQueue<String> input;
  private Scanner s;
  private Thread t = null;
  private boolean running = false;
  
  public ConcurrentScanner(InputStream is) {
    s = new Scanner(is);
    input = new ConcurrentLinkedQueue<String>();
  }
  
  private void start() {
    running = true;
    if(t == null) {
      t = new Thread(this);
    }
    t.start();
  }
  
  public void run() {
    while(running) {
      input.offer(s.next());
      try { Thread.sleep(2000); } catch (InterruptedException e) { /* Who gives a shit */ }
    }
  }
  
  /**
   * Returns the next input scanned in, or null if buffer is empty.
   * @return The next scanned token, or null if no token exists.
   */
  public String next() {
    if(!running) {
      start();
    }
    
    return input.poll();
  }
  
  public void close() { shutdown(); }
  public void shutdown() {
    running = false;
    s.close();
  }
  
}
