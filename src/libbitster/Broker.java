package libbitster;

import java.net.*;

public class Broker extends Actor {
  private Protocol peer;
  private Manager manager;

  // choked and interesting refer to the local state:
  public boolean choked;      // We are choked by the peer.
  public boolean interesting; // We are interesting to the peer.

  // choking and interested refer to the remote state:
  public boolean choking;     // We are choking this peer.
  public boolean interested;  // We are interested in the peer.
  
  public Broker (InetAddress host, int port, Manager manager) {
    peer = new Protocol(
      host, 
      port, 
      manager.getInfoHash(),
      manager.getPeerID()
    );

    this.manager = manager;

    start(); // launch thread
  }

  protected void receive (Memo memo) {

  }

  private void listen (Message message) {

  }

  protected void idle () {
    peer.communicate(); // pump that shit
    Message m;
    while ((m = peer.receive()) != null) listen(m); // grab any available msgs
  }
}
