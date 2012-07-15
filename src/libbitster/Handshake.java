package libbitster;

import java.nio.*;

// Handles creation and parsing of Handshake messages
//
// author: Russ Frank

public class Handshake {
  public static boolean bufferEquals (ByteBuffer a, ByteBuffer b, int num) {
    try {
      for (int i = 0; i < num; i++) if (a.get() != b.get()) return false;
      return true;
    } catch (BufferUnderflowException e) { return false; }
  }

  // ## verify
  // Verify a handshake message and return a peerId, or throw an exception if
  // the handshake is invalid

  public static ByteBuffer verify (
    ByteBuffer infoHash, 
    ByteBuffer handshake
  ) throws Exception {

    handshake.position(0);

    ByteBuffer id = (ByteBuffer) handshake.slice().position(1).limit(20);
    // TODO: do the string stuff properly
    ByteBuffer correctid = Util.s("BitTorrent protocol");

    if (!bufferEquals(id, correctid, 19)) {         // verify the protocol id
      throw new Exception("handshake: incorrect protocol id");
    } 

    handshake.position(0);
    ByteBuffer receivedInfoHash =  // Parse out the info hash
      (ByteBuffer) handshake.slice().position(28).limit(48);

    if (!bufferEquals(receivedInfoHash, infoHash, 20)) { // verify the info hash
      throw new Exception("handshake: infohash doesnt match");
    }

    infoHash.position(0);
    handshake.position(48);
    byte[] peerIdBytes = new byte[20];
    handshake.get(peerIdBytes, 0, 20);
    ByteBuffer peerId = ByteBuffer.wrap(peerIdBytes);

    return peerId;
  }

  // ## create
  // Creates a handshake ByteBuffer

  public static ByteBuffer create (ByteBuffer infoHash, ByteBuffer peerId) {
    infoHash.position(0);
    peerId.position(0);

    ByteBuffer handshake = ByteBuffer.allocate(68);
    handshake.put((byte) 19);
    handshake.put("BitTorrent protocol".getBytes());
    handshake.putInt(0);  // 8 bytes with value 0
    handshake.putInt(0);  // TODO: do this in a non-shitty way

    handshake.put(infoHash);
    handshake.put(peerId);

    infoHash.position(0);
    peerId.position(0);

    return handshake;
  }
}
