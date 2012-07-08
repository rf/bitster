/*
 *  RUBTClient is a BitTorrent client written at Rutgers University for 
 *  instructional use.
 *  Copyright (C) 2008-2011  Robert Moore II
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

/**
 * @author Robert Moore II
 *
 */
public final class BencodingException extends Exception
{
    /**
     * Automatically generated and unused.
     */
    private static final long serialVersionUID = -4829433031030292728L;
    
    /**
     * The message to display regarding the exception.
     */
    private final String message;
    
    /**
     * Creates a new BencodingException with a blank message.
     */
    public BencodingException()
    {
        this.message = null;
    }
    
    /**
     * Creates a new BencodingException with the message provided.
     * @param message the message to display to the user.
     */
    public BencodingException(final String message)
    {
        this.message = message;
    }
    
    /**
     * Returns a string containing the exception message specified during
     * creation.
     */
    @Override
    public final String toString()
    {
        return "Bencoding Exception:\n"+(this.message == null ? "" : this.message);
    }
}
