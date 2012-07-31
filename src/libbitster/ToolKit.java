/*
 *  RUBTClient is a BitTorrent client written at Rutgers University for 
 *  instructional use.
 *  Copyright (C) 2008  Robert Moore II
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

import java.util.*;
import java.nio.*;

/**
 * Contains a number of methods useful for debugging and developing programs
 * using Bencoding methods.&nbsp; This class is specifically designed to be used
 * with <code>edu.rutgers.cs.cs352.bt.util.Bencoder</code> and
 * <code>Bencoder2</code>.
 * 
 * @author Robert Moore II
 * 
 */
public class ToolKit
{
    /**
     * Prints out Java versions of Bencoding data types.
     * @param o the Object to print.&nbsp; It must be one of the following
     * types: @
     */
    @SuppressWarnings("rawtypes")
    public static void print(Object o)
    {
        if (o instanceof ByteBuffer)
            printString((ByteBuffer) o, true, 0);
        else if (o instanceof byte[])
            printString((byte[]) o, true, 0);
        else if (o instanceof Integer)
            printInteger((Integer) o, 0);
        else if (o instanceof ArrayList)
            printList((ArrayList) o, 0);
        else if (o instanceof HashMap)
            printMap((HashMap) o, 0);
        else
            System.err.println("Error: Unknown type");

    }

    /**
     * Prints the specified object with the provided depth.  The depth simply indicates how much to indent.
     * @param o the object to print
     * @param depth the depth of the object within another data type.
     */
	@SuppressWarnings("rawtypes")
  public static void print(Object o, int depth)
    {
        if (o instanceof ByteBuffer)
            printString((ByteBuffer) o, true, depth + 1);
        else if (o instanceof byte[])
            printString((byte[]) o, true, depth + 1);
        else if (o instanceof Integer)
            printInteger((Integer) o, depth + 1);
        else if (o instanceof ArrayList)
            printList((ArrayList) o, depth + 1);
        else if (o instanceof HashMap)
            printMap((HashMap) o, depth + 1);
        else
            System.err.println("Error: Unknown type");
    }

    /**
     * Helper method that prints out a byte string as a series of integer or ASCII
     * characters.
     * @param bytes the byte string to print.
     * @param as_text {@code true} if the byte string should be printed as ASCII characters
     * @param depth the depth of the object within other objects, used for indenting.
     */
    public static void printString(byte[] bytes, boolean as_text, int depth)
    {
        for (int k = 0; k < depth; k++)
            System.out.print("  ");
        System.out.print("String: ");
        for (int i = 0; i < bytes.length; i++)
        {
            System.out
                    .print(as_text ? (char) bytes[i] : (int) bytes[i] + " ");
        }
        System.out.println();
    }

    /**
     * Helper method that prints out a {@code ByteBuffer} as a series of integer or ASCII
     * characters.
     * @param byte_string the {@code ByteBuffer} to print.
     * @param as_text {@code true} if the {@code ByteBuffer} should be printed as ASCII characters
     * @param depth the depth of the object within other objects, used for indenting.
     */
    public static void printString(ByteBuffer byte_string, boolean as_text,
            int depth)
    {
        for (int k = 0; k < depth; k++)
            System.out.print("  ");
        System.out.print("String: ");
        byte[] bytes = byte_string.array();
        for (int i = 0; i < bytes.length; i++)
        {
            System.out
                    .print(as_text ? (char) bytes[i] : (int) bytes[i] + " ");
        }
        System.out.println();
    }

    /**
     * Helper method that prints out an integer.
     * @param i the integer to print.
     * @param depth the depth of the object within other objects, used for indenting.
     */
    public static void printInteger(Integer i, int depth)
    {
        for (int k = 0; k < depth; k++)
            System.out.print("  ");
        System.out.println("Integer: " + i);
    }

    /**
     * Helper method that prints out a list.
     * @param list the list to print.
     * @param depth the depth of the object within other objects, used for indenting.
     */
    @SuppressWarnings("rawtypes")
	public static void printList(AbstractList list, int depth)
    {
        final Iterator i = list.iterator();
        Object o = null;
        for (int k = 0; k < depth; k++)
            System.out.print("  ");
        System.out.println("List: ");
        while (i.hasNext() && (o = i.next()) != null)
        {
            for (int k = 0; k < depth; k++)
                System.out.print("  ");
            System.out.print(" +");
            print(o, depth);
        }
    }

    /**
     * Helper method that prints out a dictionary/map.
     * @param map the dictionary/map to print.
     * @param depth the depth of the object within other objects, used for indenting.
     */
    @SuppressWarnings("rawtypes")
	public static void printMap(Map map, int depth)
    {
        final Iterator i = map.keySet().iterator();
        Object key = null;
        for (int k = 0; k < depth; k++)
            System.out.print("  ");
        System.out.println("Dictionary:");
        while (i.hasNext() && (key = i.next()) != null)
        {
            for (int k = 0; k < depth; k++)
                System.out.print("  ");
            System.out.print("(K) ");
            print(key, depth);
            Object val = map.get(key);
            for (int k = 0; k < depth; k++)
                System.out.print("  ");
            System.out.print("(V) ");
            print(val, depth);
        }
    }
}
