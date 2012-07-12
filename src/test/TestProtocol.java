package test;

import libbitster.*;
import java.nio.*;
import java.net.*;
import java.io.*;

class Foo extends Actor {
  public Protocol peer;
  public String peerState;
  public Message last;

  public Foo () throws Exception {
    super();
    ByteBuffer infohash, peerid;
    infohash = ByteBuffer.wrap("asdf1234asdf1234asdf".getBytes());
    peerid = ByteBuffer.wrap("BIT-1234asdf1234asdf".getBytes());

    peer = new Protocol(
      InetAddress.getByName("localhost"), 4000, infohash, peerid
    );

    start();
  }

  protected void idle () {
    try { Thread.sleep(10); } catch (Exception e) {}
    peer.communicate();
    peerState = peer.getState();
    Message m = peer.receive();
    if (m != null) {
      peer.send(m);
      last = m;
    }
  }
}

public class TestProtocol {
  public static void main (String[] args) {
    try {
      ServerSocket server = new ServerSocket(4000);
      l("listening on port 4000");
      Foo foo = new Foo();
      Socket socket = server.accept();
      l("Received connection");
      Conn conn = new Conn(socket);
      ByteBuffer peerHandshake = conn.get(68);

      ByteBuffer peerId = Handshake.verify(
        ByteBuffer.wrap("asdf1234asdf1234asdf".getBytes()),
        peerHandshake
      );

      l("Checking peer id");
      assert(Handshake.bufferEquals(
        peerId,
        ByteBuffer.wrap("BIT-1234asdf1234asdf".getBytes()),
        20
      ));

      l("Protocol should be in handshake state");
      assert(foo.peerState == "handshake");

      l("Sending handshake");
      ByteBuffer handshake = Handshake.create(
        ByteBuffer.wrap("asdf1234asdf1234asdf".getBytes()),
        ByteBuffer.wrap("BIT-4321asdf1234asdf".getBytes())
      );
      conn.send(handshake);

      // let it tick a couple times
      Thread.sleep(50);

      l("Protocol should be in normal state");
      assert(foo.peerState == "normal");

      l("Sending a choke really slowly");

      conn.send((byte) 0);
      Thread.sleep(20);
      l(foo.peer.toString());

      conn.send((byte) 0);
      Thread.sleep(20);
      l(foo.peer.toString());

      conn.send((byte) 0);
      Thread.sleep(20);
      l(foo.peer.toString());

      conn.send((byte) 1);
      Thread.sleep(20);
      l(foo.peer.toString());

      conn.send((byte) 0);
      Thread.sleep(20);
      l(foo.peer.toString());

      l("Protocol received choke");
      assert(foo.last.getType() == Message.CHOKE);

      ByteBuffer received = conn.get(5);
      Message m = new Message(received);

      l("Protocol responded with a choke");
      assert(m.getType() == Message.CHOKE);

      foo.shutdown();
      Thread.sleep(20);
      socket.close();

    } catch (Exception e) { e.printStackTrace(); }
  }

  private static void l (String arg) { System.out.println(arg); }
}


class Conn {
  Socket sock;
  DataInputStream in = null;
  DataOutputStream out = null;
 
  Conn(Socket sock) throws Exception {
    this.sock = sock;
    in = new DataInputStream(sock.getInputStream());
    out = new DataOutputStream(sock.getOutputStream());
  }

  ByteBuffer get (int num) throws Exception {
    byte[] bytes = new byte[num];
    in.readFully(bytes);
    return ByteBuffer.wrap(bytes);
  }

  void send (ByteBuffer data) throws Exception {
    out.write(data.array(), 0, data.limit());
  }

  void send (byte b) throws Exception {
    out.write(b);
  }
}
