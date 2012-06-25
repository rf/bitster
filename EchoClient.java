

import java.io.InputStream;
import java.net.Socket;

/**
 * A simple network application that sends a single line of text to a listening
 * server and reads the response.
 * 
 * @author Robert Moore
 * 
 */
public class EchoClient {
	/**
	 * Starts this echo client by connecting to a remote echo server, reading a
	 * line from the user, sending it to the server, and then printing the
	 * server's response.
	 * 
	 * @param args
	 *            the echo server's hostname/IP address and the port number.
	 */
	public static void main(String[] args) {
		if (args.length != 2) {
			System.err.println("Invalid number of arguments.");
			return;
		}
	}

	/**
	 * Creates a TCP socket connected to the specified host/port.
	 * 
	 * @param hostname
	 *            the hostname or IP address of the remote server.
	 * @param port
	 *            the port number of the remote server.
	 * @return a new {@code Socket} object connected to the specified host/port,
	 *         or {@code null} if a connection could not be established.
	 */
	public static Socket createSocket(final String hostname, final int port) {
		// TODO: Create the socket, and return it. If an exception is caught,
		// return null.
		return null;
	}

	/**
	 * Reads a line of text from {@code System.in}.
	 * 
	 * @return a single line of text from {@code System.in} or {@code null} if
	 *         the line was empty (contained only whitespace).
	 */
	public static String readLineFromStdIn() {
		// TODO: Wrap a BufferedReader around System.in and return a single
		// line.
		return null;
	}

	/**
	 * Reads a single line of text from an {@code InputStream}.
	 * 
	 * @param input
	 *            the {@code InputStream} from which to read a line of text.
	 * @return the {@code String} read from the {@code InputStream}, or
	 *         {@code null} if the line could not be read for some reason.
	 */
	public static String readLineFromInputStream(final InputStream input) {
		// TODO: Wrap a BufferedReader around input and return a single line.
		// Return null if an exception is caught.
		return null;
	}
}
