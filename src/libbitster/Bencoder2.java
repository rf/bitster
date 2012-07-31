/*
 *  RUBTClient is a BitTorrent client written at Rutgers University for 
 *  instructional use.
 *  Copyright (C) 2008-2009  Robert Moore II
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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * Parses a Bencoded byte array and returns a combination of {@code Map},
 * {@code List}, {@code ByteBuffer}, and {@code Integer} objects.
 * 
 * @author Robert Moore II
 *
 */
public final class Bencoder2 
{
	/**
	 * Indicates an invalid object type or bencoded object.
	 */
    private static final int INVALID = -1;
    
    /**
     * Indicates that an object is a dictionary.
     */
    private static final int DICTIONARY = 0;
    
    /**
     * Indicates that an object is an integer.
     */
    private static final int INTEGER = 1;
    
    /**
     * Indicates an object is a byte string.
     */
    private static final int STRING = 2;
    
    /**
     * Indicates an object is a list.
     */
    private static final int LIST = 3;
    
    /*
     ********************************************
     ************ CONVENIENCE METHODS ***********
     ********************************************
     */
    
    /**
     * Extracts the bencoded 'info' dictionary from a metainfo torrent file.
     * @param torrent_file_bytes the bencoded metainfo dictionary.
     * @return a {@code ByteBuffer} containing the bencoded 'info' dictionary from the metainfo file.
     * @throws BencodingException if the 'info' key is not contained in the decoded dictionary.
     */
    public static final ByteBuffer getInfoBytes(byte[] torrent_file_bytes) throws BencodingException
    {
        Object[] vals = decodeDictionary(torrent_file_bytes,0);
        if(vals.length != 3 || vals[2] == null)
            throw new BencodingException("Exception: No info bytes found!");
        return (ByteBuffer)vals[2];
    }
    
    /*
     ********************************************
     ************ BDECODING METHODS *************
     ********************************************
     */
    
    /**
     * Decodes a bencoded object represented by the byte array.
     * @param bencoded_bytes the bencoded data to decode.
     * @return either a {@code Map}, {@code List}, {@code ByteBuffer}, or {@code Integer}.
     * @throws BencodingException if the bencoded data was improperly formatted.
     */
    public static final Object decode(byte[] bencoded_bytes) throws BencodingException
    {
        return decode(bencoded_bytes, 0)[1];
    }
    
    /**
     * Decodes a bencoded object represented by the byte array, starting at the specified offset.
     * @param bencoded_bytes the bencoded data to decode.
     * @param offset the offset into {@code bencoded_bytes} at which to start decoding.
     * @return a {@code Map}, {@code List}, {@code ByteBuffer}, or {@code Integer}.
     * @throws BencodingException if the bencoded object in {@code bencoded_bytes} at offset {@code offset} is incorrectly encoded. 
     */
    private static final Object[] decode(byte[] bencoded_bytes, int offset) throws BencodingException
    {
        switch(nextObject(bencoded_bytes, offset))
        {
        case DICTIONARY:
            return decodeDictionary(bencoded_bytes, offset);
        case LIST:
            return decodeList(bencoded_bytes, offset);
        case INTEGER:
            return decodeInteger(bencoded_bytes, offset);
        case STRING:
            return decodeString(bencoded_bytes, offset);
        default:
            return null;
        }
    }
    
    /**
     * Decodes an integer from the byte array.
     * @param bencoded_bytes the byte array of the bencoded integer.
     * @param offset the position of the 'i' indicating the start of the
     *        bencoded integer to be bdecoded.
     * @return an <code>Object[]</code> containing an <code>Integer</code> offset and the decoded
     *          <code>Integer</code>, in positions 0 and 1, respectively
     * @throws BencodingException if the bencoded integer in {@code bencoded_bytes} at offset {@code offset} is incorrectly encoded.
     */
    private static final Object[] decodeInteger(byte[] bencoded_bytes, int offset) throws BencodingException
    {
        StringBuffer int_chars = new StringBuffer();
        offset++;
        for(; bencoded_bytes[offset] != (byte)'e' && bencoded_bytes.length > (offset); offset++)
        {
            if((bencoded_bytes[offset] < 48 || bencoded_bytes[offset] > 57) && bencoded_bytes[offset] != 45)
                throw new BencodingException("Expected an ASCII integer character, found " + (int)bencoded_bytes[offset]);
            int_chars.append((char)bencoded_bytes[offset]);
        }
        try 
        {
            offset++;   // Skip the 'e'
            return new Object[] {new Integer(offset),new Integer(Integer.parseInt(int_chars.toString()))};
        }
        catch(NumberFormatException nfe)
        {
            throw new BencodingException("Could not parse integer at position" + offset + ".\nInvalid character at position " + offset + ".");
        }
    }
    
    /**
     * Decodes a byte string from the byte array
     * @param bencoded_bytes the bencoded form of the byte string.
     * @param offset the offset into {@code bencoded_bytes} where the byte string begins.
     * @return an <code>Object[]</code> containing an <code>Integer</code> offset and the decoded
     *          byte string (as a {@code ByteBuffer}), in positions 0 and 1, respectively
     * @throws BencodingException if the bencoded object is incorrectly encoded.
     */
    private static final Object[] decodeString(byte[] bencoded_bytes, int offset) throws BencodingException
    {
        StringBuffer digits = new StringBuffer();
        while(bencoded_bytes[offset] > '/' && bencoded_bytes[offset] < ':')
        {
            digits.append((char)bencoded_bytes[offset++]);
        }
        if(bencoded_bytes[offset] != ':')
        {
            throw new BencodingException("Error: Invalid character at position " + offset + ".\nExpecting ':' but found '" + (char)bencoded_bytes[offset] + "'.");
        }
        offset++;
        int length = Integer.parseInt(digits.toString());
        byte[] byte_string = new byte[length];
        System.arraycopy(bencoded_bytes, offset, byte_string, 0, byte_string.length);
        return new Object[] {new Integer(offset+length), ByteBuffer.wrap(byte_string)};
    }
    
    /**
     * Decodes a list from the bencoded byte array.
     * @param bencoded_bytes the bencoded form of the list.
     * @param offset the offset into {@code bencoded_bytes} where the list begins.
     * @return an <code>Object[]</code> containing an <code>Integer</code> offset and the decoded
     *          list (as a {@code List}), in positions 0 and 1, respectively
     * @throws BencodingException if the bencoded object is incorrectly encoded.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	private static final Object[] decodeList(byte[] bencoded_bytes, int offset) throws BencodingException
    {
        ArrayList list = new ArrayList();
        offset++;
        Object[] vals;
        while(bencoded_bytes[offset] != (byte)'e')
        {
            vals = decode(bencoded_bytes,offset);
            offset = ((Integer)vals[0]).intValue();
            list.add(vals[1]);
        }
        offset++;
        return new Object[] {new Integer(offset), list};
    }
    
    /**
     * Decodes a dictionary from the bencoded byte array.
     * @param bencoded_bytes the bencoded form of the dictionary.
     * @param offset the offset into {@code bencoded_bytes} where the dictionary begins.
     * @return an <code>Object[]</code> containing an <code>Integer</code> offset and the decoded
     *          dictionary (as a {@code Map}, in positions 0 and 1, respectively
     * @throws BencodingException if the bencoded object is incorrectly encoded.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	private static final Object[] decodeDictionary(byte[] bencoded_bytes, int offset) throws BencodingException
    {
        HashMap map = new HashMap();
        ++offset;
        ByteBuffer info_hash_bytes = null;
        while(bencoded_bytes[offset] != (byte)'e')
        {

            // Decode the key, which must be a byte string
            Object[] vals = decodeString(bencoded_bytes, offset);
            ByteBuffer key = (ByteBuffer)vals[1];
            offset = ((Integer)vals[0]).intValue();
            boolean match = true;
            for(int i = 0; i < key.array().length && i < 4; i++)
            {
                if(!key.equals(ByteBuffer.wrap(new byte[]{'i', 'n','f','o'})))
                {
                    match = false;
                    break;
                }
            }
            int info_offset = -1;
            if(match)
                info_offset = offset;
            vals = decode(bencoded_bytes, offset);
            offset = ((Integer)vals[0]).intValue();
            if(match)
            {
                info_hash_bytes = ByteBuffer.wrap(new byte[offset - info_offset]);
                info_hash_bytes.put(bencoded_bytes,info_offset, info_hash_bytes.array().length);
            }
            else if(vals[1] instanceof HashMap)
            {
                info_hash_bytes = (ByteBuffer)vals[2];
            }
            if(vals[1] != null)
                map.put(key,vals[1]);
        }

        return new Object[] {new Integer(++offset), map, info_hash_bytes};
    }
    
    /**
     * Determines the bencoded data type at {@code bencoded_bytes[offset]}.
     * @param bencoded_bytes the bencoded data.
     * @param offset the offset into {@code bencoded_bytes} that contains a bencoded object.
     * @return the type of the bencoded object.
     * @see #DICTIONARY
     * @see #LIST
     * @see #INTEGER
     * @see #STRING
     * @see #INVALID
     */
    private static final int nextObject(byte[] bencoded_bytes, int offset)
    {
        switch(bencoded_bytes[offset])
        {
        case (byte)'d':
            return DICTIONARY;
        case (byte)'i':
            return INTEGER;
        case (byte)'l':
            return LIST;
        case (byte)'0':
        case (byte)'1':
        case (byte)'2':
        case (byte)'3':
        case (byte)'4':
        case (byte)'5':
        case (byte)'6':
        case (byte)'7':
        case (byte)'8':
        case (byte)'9':
            return STRING;
        default:
            return INVALID;
        }
    }
    
    /*
     ********************************************
     ************ BENCODING METHODS *************
     ********************************************
     */
    
    /**
     * Bencodes the specified object as a {@code byte[]}.
     * @param o the object to bencode.
     * @return the bencoded form of the object.
     * @throws BencodingException if {@code o} is not of type {@code HashMap}, {@code ArrayList},
     *  		{@code Integer}, or {@code ByteBuffer}.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public static final byte[] encode(Object o) throws BencodingException
    {
        if(o instanceof HashMap)
            return encodeDictionary((HashMap)o);
        else if(o instanceof ArrayList)
            return encodeList((ArrayList)o);
        else if(o instanceof Integer)
            return encodeInteger((Integer)o);
        else if(o instanceof ByteBuffer)
            return encodeString((ByteBuffer)o);
        else
            throw new BencodingException("Error: Object not of valid type for Bencoding.");
    }
    
    
    /**
     * Bencodes the specified byte string.
     * @param string the byte string to bencode.
     * @return a {@code byte[]} containing the bencoded form of the byte string.
     */
    private static final byte[] encodeString(ByteBuffer string)
    {
        int length = string.array().length;
        int num_digits = 1;
        while((length /= 10) > 0)
        {
            num_digits++;
        }
        byte[] bencoded_string = new byte[length+num_digits+1];
        bencoded_string[num_digits] = (byte)':';
        System.arraycopy(string.array(), 0, bencoded_string, num_digits+1, length);
        for(int i = num_digits-1; i >= 0; i--)
        {
            bencoded_string[i] = (byte)((length % 10)+48);
            length /= 10;
        }
        return bencoded_string;
    }
    
    /**
     * Bencodes the specified Integer.
     * @param integer the Integer to bencode.
     * @return a {@code byte[]} containing the bencoded form of the Integer.
     */
    private static final byte[] encodeInteger(Integer integer)
    {
        int num_digits = 1;
        int int_val = integer.intValue();
        while((int_val /= 10) > 0)
            ++num_digits;
        int_val = integer.intValue();
        byte[] bencoded_integer = new byte[num_digits+2];
        bencoded_integer[0] = (byte)'i';
        bencoded_integer[bencoded_integer.length - 1] = (byte)'e';
        for(int i = num_digits; i > 0; i--)
        {
            bencoded_integer[i] = (byte)((int_val % 10)+48);
            int_val /= 10;
        }
        return bencoded_integer;
    }
    
    /**
     * Bencodes the specified {@code ArrayList}.
     * @param list the {@code ArrayList} to bencode. 
     * @return a {@code byte[]} containing the bencoded form of the {@code ArrayList}.
     * @throws BencodingException if any of the objects in the list is not bencodable.
     */
    @SuppressWarnings({ "rawtypes" })
	private static final byte[] encodeList(ArrayList list) throws BencodingException
    {
        byte[][] list_segments = new byte[list.size()][];
        for(int i = 0; i < list_segments.length;i++)
        {
            list_segments[i] = encode(list.get(i));
        }
        int total_length = 2;
        for(int i = 0 ; i < list_segments.length; i++)
            total_length += list_segments[i].length;
        byte[] bencoded_list = new byte[total_length];
        bencoded_list[0] = 'l';
        bencoded_list[bencoded_list.length-1] = 'e';
        int offset = 1;
        for(int i = 0; i < list_segments.length; i++)
        {
            System.arraycopy(list_segments[i],0,bencoded_list,offset,list_segments[i].length);
            offset += list_segments[i].length;
        }
        return bencoded_list;
    }
    
    /**
     * Bencodes the specified {@code HashMap}.
     * @param dictionary the {@code HashMap} to bencode.
     * @return a {@code byte[]} containing the bnecoded form of the {@code HashMap}.
     * @throws BencodingException if any of the objecdts in the map is not bencodable.
     */
    private static final byte[] encodeDictionary(HashMap<ByteBuffer, Object> dictionary) throws BencodingException
    {
        TreeMap<ByteBuffer, Object> sorted_dictionary = new TreeMap<ByteBuffer, Object>();
        sorted_dictionary.putAll(dictionary);
        byte[][] dictionary_parts = new byte[sorted_dictionary.keySet().size()*2][];
        int k = 0;
        for(Iterator<ByteBuffer> i = sorted_dictionary.keySet().iterator(); i.hasNext();)
        {
            ByteBuffer key = i.next();
            dictionary_parts[k++] = encodeString(key);
            dictionary_parts[k++] = encode(sorted_dictionary.get(key));
        }
        
        int total_length = 2;
        for(int i = 0; i < dictionary_parts.length; i++)
        {
            total_length += dictionary_parts[i].length;
        }
        byte[] bencoded_dictionary = new byte[total_length];
        bencoded_dictionary[0] = 'd';
        bencoded_dictionary[bencoded_dictionary.length-1] = 'e';
        int offset = 1;
        for(int i = 0; i < dictionary_parts.length; i++)
        {
            System.arraycopy(dictionary_parts[i],0,bencoded_dictionary,offset,dictionary_parts[i].length);
            offset += dictionary_parts[i].length;
        }
        return bencoded_dictionary;
    }
}
