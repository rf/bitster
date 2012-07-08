/*
 *  RUBTClient is a BitTorrent client written at Rutgers University for 
 *  instructional use.
 *  Copyright (C) 2009  Robert Moore II
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package libbitster;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * This is a data structure class that extracts basic information from a bencoded torrent metainfo
 * file and stores it in public fields.&nbsp; Note that this class only works for torrent metainfo files
 * for a single-file torrent.
 * 
 * @author Robert Moore II
 *
 */
public class TorrentInfo
{
	/**
     * Key used to retrieve the info dictionary from the torrent metainfo file.
     */
    public final static ByteBuffer KEY_INFO = ByteBuffer.wrap(new byte[]
    { 'i', 'n', 'f', 'o' });
	
	/**
     * Key used to retrieve the length of the torrent.
     */
    public final static ByteBuffer KEY_LENGTH = ByteBuffer.wrap(new byte[]
    { 'l', 'e', 'n', 'g', 't', 'h' });

    /**
     * Key used to retrieve the piece hashes.
     */
    public final static ByteBuffer KEY_PIECES = ByteBuffer.wrap(new byte[]
    { 'p', 'i', 'e', 'c', 'e', 's' });

    /**
     * Key used to retrieve the file name.
     */
    public final static ByteBuffer KEY_NAME = ByteBuffer.wrap(new byte[]
    { 'n', 'a', 'm', 'e' });

    /**
     * Key used to retrieve the default piece length.
     */
    public final static ByteBuffer KEY_PIECE_LENGTH = ByteBuffer.wrap(new byte[]
    { 'p', 'i', 'e', 'c', 'e', ' ', 'l', 'e', 'n', 'g', 't', 'h' });
	
	/**
	 * ByteBuffer to retrieve the announce URL from the metainfo dictionary.
	 */
	public static final ByteBuffer KEY_ANNOUNCE = ByteBuffer.wrap(new byte[] {'a','n','n','o','u','n','c','e'});
	
	/**
	 * A byte array containing the raw bytes of the torrent metainfo file.
	 */
	public final byte[] torrent_file_bytes;
	
	/**
	 * The base dictionary of the torrent metainfo file.&nbsp; 
     * See <a href="http://www.bittorrent.org/beps/bep_0003.html">http://www.bittorrent.org/beps/bep_0003.html</a>
     * for an explanation of what keys are available and how they map.
	 */
	public final Map<ByteBuffer,Object> torrent_file_map;
	
	/**
	 * The unbencoded info dictionary of the torrent metainfo file.&nbsp; 
     * See <a href="http://www.bittorrent.org/beps/bep_0003.html">http://www.bittorrent.org/beps/bep_0003.html</a> for 
	 * an explanation of what keys are available and how they map.
	 */
	public final Map<ByteBuffer,Object> info_map;
	
	/**
	 * The SHA-1 hash of the bencoded form of the info dictionary from the torrent metainfo file.
	 */
	public final ByteBuffer info_hash;
	
	/**
	 * The base URL of the tracker for client scrapes.
	 */
	public final URL announce_url;
	
	/**
	 * The default length of each piece in bytes.&nbsp; Note that the last piece may be irregularly-sized (less than the value of piece_length)
	 * if the file size is not a multiple of the piece size.
	 */
	public final int piece_length;
	
	/**
	 * The name of the file referenced in the torrent metainfo file.
	 */
	public final String file_name;
	
	/**
	 * The length of the file in bytes.
	 */
	public final int file_length;
	
	/**
	 * The SHA-1 hashes of each piece of the file.
	 */
	public final ByteBuffer[] piece_hashes;
	
	/**
	 * Creates a new TorrentInfo object from the specified byte array.  If the byte array is {@code null} or
	 * has a length of 0(zero), then an {@code IllegalArgumentException} is thrown.
	 * @param torrent_file_bytes
	 * @throws BencodingException
	 */
	@SuppressWarnings("unchecked")
	public TorrentInfo(byte[] torrent_file_bytes) throws BencodingException
	{ 	
		// Make sure the input is valid
		if(torrent_file_bytes == null || torrent_file_bytes.length == 0)
			throw new IllegalArgumentException("Torrent file bytes must be non-null and have at least 1 byte.");
		
		// Assign the byte array
		this.torrent_file_bytes = torrent_file_bytes;
		
		// Assign the metainfo map
		this.torrent_file_map = (Map<ByteBuffer,Object>)Bencoder2.decode(torrent_file_bytes);
		
		// Try to extract the announce URL
		ByteBuffer url_buff = (ByteBuffer)this.torrent_file_map.get(TorrentInfo.KEY_ANNOUNCE);
		if(url_buff == null)
			throw new BencodingException("Could not retrieve anounce URL from torrent metainfo.  Corrupt file?");
		
		try {
			String url_string = new String(url_buff.array(), "ASCII");
			URL announce_url = new URL(url_string);
			this.announce_url = announce_url;
		}
		catch(UnsupportedEncodingException uee)
		{
			throw new BencodingException(uee.getLocalizedMessage());
		}
		catch(MalformedURLException murle)
		{
			throw new BencodingException(murle.getLocalizedMessage());
		}
		
		// Try to extract the info dictionary
		ByteBuffer info_bytes = Bencoder2.getInfoBytes(torrent_file_bytes);
		Map<ByteBuffer,Object> info_map = (Map<ByteBuffer,Object>)this.torrent_file_map.get(TorrentInfo.KEY_INFO);
		
		if(info_map == null)
			throw new BencodingException("Could not extract info dictionary from torrent metainfo dictionary.  Corrupt file?");
		this.info_map = info_map;
		
		// Try to generate the info hash value
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			digest.update(info_bytes.array());
			byte[] info_hash = digest.digest();
			this.info_hash = ByteBuffer.wrap(info_hash);
		}
		catch(NoSuchAlgorithmException nsae)
		{
			throw new BencodingException(nsae.getLocalizedMessage());
		}
		
		// Extract the piece length from the info dictionary
		Integer piece_length = (Integer)this.info_map.get(TorrentInfo.KEY_PIECE_LENGTH);
		if(piece_length == null)
			throw new BencodingException("Could not extract piece length from info dictionary.  Corrupt file?");
		this.piece_length = piece_length.intValue();
		
		// Extract the file name from the info dictionary
		ByteBuffer name_bytes = (ByteBuffer)this.info_map.get(TorrentInfo.KEY_NAME);
		if(name_bytes == null)
			throw new BencodingException("Could not retrieve file name from info dictionary.  Corrupt file?");
		try {
			this.file_name = new String(name_bytes.array(),"ASCII");
		}
		catch(UnsupportedEncodingException uee)
		{
			throw new BencodingException(uee.getLocalizedMessage());
		}
		
		// Extract the file length from the info dictionary
		Integer file_length = (Integer)this.info_map.get(TorrentInfo.KEY_LENGTH);
		if(file_length == null)
			throw new BencodingException("Could not extract file length from info dictionary.  Corrupt file?");
		this.file_length = file_length.intValue();
		
		// Extract the piece hashes from the info dictionary
		ByteBuffer all_hashes = (ByteBuffer)this.info_map.get(TorrentInfo.KEY_PIECES);
		if(all_hashes == null)
			throw new BencodingException("Could not extract piece hashes from info dictionary.  Corrupt file?");
		byte[] all_hashes_array = all_hashes.array();
		
		// Verify that the length of the array is a multiple of 20 bytes (160 bits)
		if(all_hashes_array.length % 20 != 0)
			throw new BencodingException("Piece hashes length is not a multiple of 20.  Corrupt file?");
		int num_pieces = all_hashes_array.length / 20;
		
		// Copy the values of the piece hashes into the local field
		this.piece_hashes = new ByteBuffer[num_pieces];
		for(int i = 0; i < num_pieces; i++)
		{
			byte[] temp_buff = new byte[20];
			System.arraycopy(all_hashes_array,i*20,temp_buff,0,20);
			this.piece_hashes[i] = ByteBuffer.wrap(temp_buff);
		}
	}
}
