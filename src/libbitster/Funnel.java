package libbitster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Funnel extends Actor {
  private int size;
  private int pieceSize;
  private List<Piece> pieces;
  
  public Funnel(int size, int pieceSize) {
    if(size < 0 || pieceSize < 0 || size < pieceSize)
      throw new IllegalArgumentException();
    
    this.size = size;
    this.pieceSize = pieceSize;
    int numPieces = (int)Math.ceil((double)size / (double)pieceSize);
    pieces = Collections.synchronizedList(new ArrayList<Piece>(numPieces));
  }
  
  protected void receive (Memo memo) {
    if(!(memo.getPayload() instanceof Piece))
      throw new IllegalArgumentException("Funnel expects a Piece");
    
    Piece piece = (Piece)memo.getPayload();
    
    if(piece.getNumber() < pieces.size() - 1 && piece.getData().length != pieceSize)
      throw new IllegalArgumentException("Piece " + piece.getNumber() + " is the wrong size");
    //This is a little fancy around the part with the modulus operator
    //Basically it just gets the minimum number of bytes that the last piece should contain
    if(piece.getNumber() == pieces.size() - 1 && piece.getData().length < ((size - 1) % pieceSize) + 1)
      throw new IllegalArgumentException("Piece " + piece.getNumber() + " is too small");
    
    pieces.set(piece.getNumber(), piece);
  }
}
