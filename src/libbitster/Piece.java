package libbitster;

import java.nio.ByteBuffer;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.BitSet;

public class Piece {
  private int number;
  private int blockSize;
  private byte[] data;
  private BitSet completed;
  
  /*
   * Creates an empty piece
   * @param number The piece index
   * @param blockSize The number of bytes to add to the piece at a time (generally 2^14 or 16KB)
   * @param size The size of this piece, must be >= blockSize
   */
  public Piece(int number, int blockSize, int size) {
    //Sanity checks
    if(number < 0 || blockSize <= 0 || size <= 0)
      throw new IllegalArgumentException("Arguments must be > 0 (except number which may = 0)");
    if(blockSize > size)
      throw new IllegalArgumentException("blockSize must be < size");
    
    this.number = number;
    this.blockSize = blockSize;
    
    data = new byte[size];
    
    //One bit for each block
    completed = new BitSet( (int)Math.ceil((double)size / (double)blockSize) );
  }
  
  /*
   * Adds a block of bytes to the piece
   * @param begin The byte offset within the piece, must be aligned to a blockSize boundary
   * @param block The block of bytes to add
   */
  public void addBlock(int begin, ByteBuffer block) {
    if(block == null || block.position() != 0)
      throw new IllegalArgumentException("block is either null or is not at the beginning of the buffer");
    
    //Make sure we are on correct boundaries
    if(begin % blockSize != 0)
      throw new IllegalArgumentException("begin must be aligned on a " + blockSize + " byte boundry");
    if(begin + block.limit() > data.length || begin < 0)
      throw new IllegalArgumentException("block under/overflows the buffer for this piece");
    
    //If this piece was already downloaded then there is nothing to to
    if(completed.get(begin % blockSize))
      return;
    
    //Check to make sure not special case where final block would be smaller than the rest
    //Also check to make sure 'block' is of length 'blockSize'
    if( (begin < (data.length / blockSize) * blockSize) && (block.limit() != blockSize) )
      throw new IllegalArgumentException("block is of not " + blockSize + " bytes long");
    else if(block.limit() != data.length % blockSize) //Last block which is smaller
      throw new IllegalArgumentException("block is not " + (data.length % blockSize) + " bytes long for final block");
    
    //Copy block over to this piece
    for(int i = begin, l = begin + block.limit(); i < l; ++i)
      data[i] = block.get();
    
    completed.set(begin % blockSize);
  }
  
  /*
   * Returns true when all blocks have been added to this piece
   * @return true when finished, otherwise false
   */
  public boolean finished() {
    int blocks = (int)Math.ceil((double)data.length / (double)blockSize);
    
    //If any block is not completed than the piece is not finished
    for(int i=0; i<blocks; ++i)
      if(!completed.get(i))
        return false;
    
    return true;
  }
  
  /*
   * Gets the piece index
   * @return The piece number
   */
  public int getNumber() {
    return number;
  }
  
  /*
   * Gets the data associated with this piece after being finished
   * If the piece is not finished you get a lovely IllegalStateException instead ;)
   * @return The data associated with this piece
   */
  public final byte[] getData() {
    if(!finished())
      throw new IllegalStateException("Piece is not finished");
      
    return data;
  }
  
  /*
   * Returns true if the data associated with this piece matches the expected hash
   * @return true if this piece is valid
   */
  public boolean isValid(byte[] expectedHash) {
    MessageDigest sha1;
    byte[] hash;
        
    try {
      sha1 = MessageDigest.getInstance("SHA-1");
    }
    catch (NoSuchAlgorithmException e) {
      throw new UnsupportedOperationException("JVM does not support SHA-1?");
    }
    
    hash = sha1.digest(data);
    
    return Arrays.equals(hash, expectedHash);
  }
}