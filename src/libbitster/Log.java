package libbitster;

import java.util.Calendar;
import java.io.PrintStream;
import java.text.SimpleDateFormat;

public class Log {
  
  public static final String RED = "\u001B[0;31m";
  public static final String SANE = "\u001B[0m";
  public static final String BLUE = "\u001B[0;34m";
  public static final String YELLOW = "\u001B[0;33m";
  public static final String GREEN = "\u001B[0;32m";
  static final String DATE_FORMAT_NOW = "HH:mm:ss";
  static PrintStream output = System.out;
  
  public static void setOutput(PrintStream os) { output = os; }

  public static String time () {
    Calendar cal = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
    return sdf.format(cal.getTime()) + " ";
  }

  public static void d (String message) {
    p(time() + GREEN + message + SANE);
  }

  public static void debug (String message) { d(message); }

  public static void i (String message) {
    p(time() + BLUE + message + SANE);
  }

  public static void info (String message) { i(message); }

  public static void w (String message) {
    p(time() + YELLOW + message + SANE);
  }

  public static void warning (String message) { w(message); }

  public static void e (String message) {
    p(time() + RED + message + SANE);
  }

  public static void error (String message) { e(message); }

  public static void p (String message) {
    output.println(message);
  }
}

