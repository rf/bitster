package libbitster;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import java.io.*;
import java.util.logging.Logger;

public class Funnel extends Actor {
  private int size;
  private int pieceSize;
  private List<Piece> pieces;
  private File dest;
  private final static Logger log = Logger.getLogger("Funnel");
  /*
   * Creates Funnel representing a single file being downloaded
   * @param size The size of the expected file
   * @param pieceSize The size of each piece being received except possibly the last (usually 2^14 or 16KB)
   */
  public Funnel(File dest, int size, int pieceSize) {
    if(size < 0 || pieceSize < 0 || size < pieceSize)
      throw new IllegalArgumentException();

    this.dest = dest;
    this.size = size;
    this.pieceSize = pieceSize;
    int numPieces = (int)Math.ceil((double)size / (double)pieceSize);
    pieces = new ArrayList<Piece>(Collections.nCopies(numPieces, (Piece)null));
  }

  /*
   * Currently expects a memo containing a Piece as its payload (will change in future implementation)
   * @see libbitster.Actor#receive(libbitster.Memo)
   */
  protected void receive (Memo memo) {
    if (memo.getType() == "piece") {

      if(!(memo.getPayload() instanceof Piece))
        throw new IllegalArgumentException("Funnel expects a Piece");

      Piece piece = (Piece)memo.getPayload();
      if(!piece.isValid()) {
        //throw new IllegalArgumentException("The piece being recieved by Funnel is not valid");
        log.severe("Piece " + piece.getNumber() + " failed hash check");
        return;
      }
      if(!piece.finished())
        throw new IllegalArgumentException("The piece being received by Funnel is not finished");

      if(piece.getNumber() < pieces.size() - 1 && piece.getData().length != pieceSize)
        throw new IllegalArgumentException("Piece " + piece.getNumber() + " is the wrong size");
      //This is a little fancy around the part with the modulus operator
      //Basically it just gets the minimum number of bytes that the last piece should contain
      if(piece.getNumber() == pieces.size() - 1 && piece.getData().length < ((size - 1) % pieceSize) + 1)
        throw new IllegalArgumentException("Piece " + piece.getNumber() + " is too small");

      pieces.set(piece.getNumber(), piece);

    }

    else if (memo.getType() == "save") {
      try { saveToFile(); } catch (IOException e) { e.printStackTrace(); }
      log.info("Funnel shutting down");
      shutdown();
      memo.getSender().post(new Memo("done", null, this));
    }
  }

  protected void idle () { 
    try { Thread.sleep(100); } catch (InterruptedException e) {} 
  }

  /*
   * Returns true when all the pieces have been received
   * @return true when finished
   */
  public boolean finished() {
    return true;
  }

  /*
   * Gets a part of a piece, or a block within a piece
   * @param pieceNumber The index of the desired piece
   * @param start The byte offset from the start of the piece
   * @param length The number of bytes to get
   */
  public ByteBuffer get(int pieceNumber, int start, int length) {
    if(pieceNumber < 0 || pieceNumber >= pieces.size())
      throw new IndexOutOfBoundsException("pieceNumber is out of bounds");

    Piece piece = pieces.get(pieceNumber);

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

  /*
   * Saves the data to a file if finished
   * @param filename The name of the file to use when saving the data
   */
  public void saveToFile() throws IOException {
    if(!finished())
      throw new IllegalStateException("File is not finished being downloaded");

    FileOutputStream fileOut = new FileOutputStream(dest);

    for (Piece p : pieces) {
      if (p != null) fileOut.write(p.getData());
    }

    fileOut.close();
    log.info("Finished writing file.");
  }
}
