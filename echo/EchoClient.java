import java.net.Socket;
import java.io.*;

// # EchoClient
// Simple echo client class, written by Russ Frank for IT.
public class EchoClient {
  public static void main (String[] args) {
    if (args.length != 2) {
      System.out.println("Invalid arguments");
    }

    try {
      // Parse arguments.
      int port = Integer.parseInt(args[1]);
      String host = args[0];

      // Setup the socket.
      Socket sock = new Socket(host, port);

      // Read a string of input from the user and write it to the socket.
      String input = readln(System.in);
      writeln(input, sock.getOutputStream());

      // Read a string of output from the socket and write it to the screen.
      String response = readln(sock.getInputStream());
      System.out.println(response);
    }

    catch (Exception e) {
      // Catch all errors and print them.
      System.out.println(e.toString());
    }
  }

  // ## readln
  // Read a single line of input from some `InputStream`.
  public static String readln (InputStream stream) throws Exception {
    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
    return reader.readLine();
  }

  // ## writeln
  // Write a single line of input to some `OutputStream`.
  public static void writeln (String line, OutputStream stream) throws Exception {
    PrintWriter out = new PrintWriter(stream, true);
    out.println(line);
  }
}
