package libbitster;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.io.*;

// Assembles pieces together into a file, actually runs the piece verification,
// and can write the completed data to a file.
// author: Theodore Surgent

public class Funnel extends Actor {
  private static final int defaultBlockSize = 16384;
  private int size;
  private int pieceSize;
  private int pieceCount;
  private ByteBuffer[] hashes;
  private RandomAccessFile file;
  private MappedByteBuffer dest;
  
  /**
   * Creates Funnel representing a single file being downloaded
   * @param size The size of the expected file
   * @param pieceSize The size of each piece being received except possibly the last (usually 2^14 or 16KB)
   * @throws IOException 
   */
  public Funnel(TorrentInfo info, File dest, Actor creator) throws IOException {
    size = info.file_length;
    pieceSize = info.piece_length;
    pieceCount =  (int)Math.ceil((double)size / (double)pieceSize);
    hashes = info.piece_hashes;
    
    if(size < 0 || pieceSize < 0 || size < pieceSize) {
      String msg = "Bad size arguments in Funnel constructor";
      Log.error(msg);
      throw new IllegalArgumentException(msg);
    }
    
    if(!dest.exists())
        dest.createNewFile();
    
    this.file = new RandomAccessFile(dest, "rw");
    this.dest = file.getChannel().map(MapMode.READ_WRITE, 0, size);

    ArrayList<Piece> donePieces = new ArrayList<Piece>();
    
    for(int i = 0; i < pieceCount; ++i) {
      Piece p = getPieceNoValidate(i); //Avoid spitting hash fails to the log
      
      if(p.isValid())
        donePieces.add(p);
    }
    Log.info("Funnel initialized.");
    creator.post(new Memo("pieces", donePieces, this));
  }

  /**
   * Currently expects a memo containing a Piece as its payload (will change in future implementation)
   * @see libbitster.Actor#receive(libbitster.Memo)
   */
  protected void receive (Memo memo) {
    if(memo.getType().equals("piece")) {

      if(!(memo.getPayload() instanceof Piece))
        throw new IllegalArgumentException("Funnel expects a Piece");

      Piece piece = (Piece)memo.getPayload();
      if(!piece.isValid()) {
        //throw new IllegalArgumentException("The piece being recieved by Funnel is not valid");
        Log.error("Piece " + piece.getNumber() + " failed hash check");
        
        //Notify the sender
        memo.getSender().post(new Memo("hash_fail", Integer.valueOf(piece.getNumber()), this));
        
        return;
      }
      if(!piece.finished())
        throw new IllegalArgumentException("The piece being received by Funnel is not finished");

      if(piece.getNumber() < pieceCount - 1 && piece.getData().length != pieceSize)
        throw new IllegalArgumentException("Piece " + piece.getNumber() + " is the wrong size");
      
      //This is a little fancy around the part with the modulus operator
      //Basically it just gets the minimum number of bytes that the last piece should contain
      if(piece.getNumber() == pieceCount - 1 && piece.getData().length < ((size - 1) % pieceSize) + 1)
        throw new IllegalArgumentException("Piece " + piece.getNumber() + " is too small");

      // Send a memo back to the Manager so it can forward it to each broker
      memo.getSender().post(new Memo("have", memo.getPayload(), this));
      setPiece(piece);

    }
    else if(memo.getType().equals("save")) {
      dest.force();
      Log.info("Funnel saved data");
    }
    else if(memo.getType().equals("halt")) {
      Log.info("Funnel shutting down");
      dest.force();
      try { file.close(); } catch (IOException e) { e.printStackTrace(); }
      shutdown();
      memo.getSender().post(new Memo("done", null, this));
    }
    else if(memo.getType().equals("request")) {
      if(!(memo.getPayload() instanceof Integer)) {
        String msg = "Integer payload expected for request message in Funnel";
        Log.error(msg);
        throw new IllegalArgumentException(msg);
      }
      
      Integer index = (Integer) memo.getPayload();
      
      memo.getSender().post(new Memo("piece", getPiece(index.intValue()), this));
    }
  }

  protected void idle () { 
    try { Thread.sleep(100); } catch (InterruptedException e) {} 
  }

  /**
   * Gets a part of a piece, or a block within a piece
   * @param pieceNumber The index of the desired piece
   * @param start The byte offset from the start of the piece
   * @param length The number of bytes to get
   */
  public ByteBuffer get(int pieceNumber, int start, int length) {
    if(pieceNumber < 0 || pieceNumber >= pieceCount)
      throw new IndexOutOfBoundsException("pieceNumber is out of bounds");

    Piece piece = getPiece(pieceNumber);

    if(piece == null)
      throw new IllegalStateException("Piece " + pieceNumber + "has not been recieved yet");

    byte[] data = piece.getData();

    if(start < 0 || start > data.length)
      throw new IndexOutOfBoundsException("start is out of bounds");
    if(length < 0 || (start + length) > data.length)
      throw new IndexOutOfBoundsException("length is either < 0 or too large");

    ByteBuffer buff = ByteBuffer.allocate(length);

    for(int i = start, l = start + length; i < l; ++i)
      buff.put(data[i]);

    buff.rewind();

    return buff;
  }
  
  /**
   * Returns the requested piece
   * @param pieceNumber The index of the Piece to get
   * @return A Piece
   */
  public Piece getPiece(int pieceNumber) {
    if(pieceNumber < 0 || pieceNumber >= pieceCount) {
      String msg = "The Piece index is out of bounds";
      Log.error(msg);
      throw new IndexOutOfBoundsException(msg);
    }

  
    Piece piece = getPieceNoValidate(pieceNumber);
    
    if(!piece.isValid()) {
      String msg = "The request piece is not available";
      Log.error(msg);
      throw new IllegalArgumentException(msg);
    }
    
    return piece;
  }
  
  //Used by getPiece and in constructor
  private Piece getPieceNoValidate(int pieceNumber) {
    if(pieceNumber < 0 || pieceNumber >= pieceCount) {
      String msg = "The Piece index is out of bounds";
      Log.error(msg);
      throw new IndexOutOfBoundsException(msg);
    }
    
    int requestedPieceSize = (pieceNumber < pieceCount - 1) ? pieceSize : ((size - 1) % pieceSize) + 1;
    
    byte[] data = new byte[requestedPieceSize];
    dest.position(pieceNumber*pieceSize);
    dest.get(data, 0, data.length);
    Piece piece = new Piece(data, hashes[pieceNumber].array(), pieceNumber, defaultBlockSize);
    
    return piece;
  }
  
  private void setPiece(Piece p) {
    byte[] data = p.getData();
    dest.position(p.getNumber() * pieceSize);
    dest.put(data, 0, data.length);
  }
}
