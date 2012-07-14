package libbitster;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Funnel extends Actor {
  private int size;
  private int pieceSize;
  private AtomicInteger added;
  private List<Piece> pieces;
  
  public Funnel(int size, int pieceSize) {
    if(size < 0 || pieceSize < 0 || size < pieceSize)
      throw new IllegalArgumentException();
    
    this.size = size;
    this.pieceSize = pieceSize;
    this.added = new AtomicInteger(0);
    int numPieces = (int)Math.ceil((double)size / (double)pieceSize);
    pieces = Collections.synchronizedList(new ArrayList<Piece>(Collections.nCopies(numPieces, (Piece)null)));
  }
  
  protected void receive (Memo memo) {
    if(!(memo.getPayload() instanceof Piece))
      throw new IllegalArgumentException("Funnel expects a Piece");
    
    Piece piece = (Piece)memo.getPayload();
    if(!piece.finished())
      throw new IllegalArgumentException("The piece being received by Funnel is not finished");
    
    if(piece.getNumber() < pieces.size() - 1 && piece.getData().length != pieceSize)
      throw new IllegalArgumentException("Piece " + piece.getNumber() + " is the wrong size");
    //This is a little fancy around the part with the modulus operator
    //Basically it just gets the minimum number of bytes that the last piece should contain
    if(piece.getNumber() == pieces.size() - 1 && piece.getData().length < ((size - 1) % pieceSize) + 1)
      throw new IllegalArgumentException("Piece " + piece.getNumber() + " is too small");
  
    if(pieces.get(piece.getNumber()) == null)
    {
      added.incrementAndGet();
      pieces.set(piece.getNumber(), piece);
    }
  }
  
  public boolean finished() {
    return added.equals(pieces.size());
  }
  
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
  
  public void saveToFile(String filename) throws IOException {
    if(!finished())
      throw new IllegalStateException("File is not finished being downloaded");
    
    FileOutputStream fileOut = new FileOutputStream(filename);
    
    for(int piece = 0, numPieces = pieces.size(); piece < numPieces; ++piece)
      fileOut.write(pieces.get(piece).getData());
    
    fileOut.close();
  }
}
