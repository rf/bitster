package libbitster;

import java.util.Calendar;
import java.io.PrintStream;
import java.text.SimpleDateFormat;

public class Log {
  
  private static final String RED = "\u001B[0;31m";
  private static final String SANE = "\u001B[0m";
  private static final String BLUE = "\u001B[0;34m";
  private static final String YELLOW = "\u001B[0;33m";
  private static final String GREEN = "\u001B[0;32m";
  private static final String DATE_FORMAT_NOW = "HH:mm:ss";
  private static PrintStream output = System.out;
  private static boolean colors = false;
  
  public static void setOutput(PrintStream os) { output = os; }
  
  /** @return the ANSI code for red, or blank string if colors are disabled */
  public static String red() { if(colors) { return RED; } else { return ""; } }
  
  /** @return the ANSI code for default terminal color, or blank string if colors are disabled */
  public static String sane() { if(colors) { return SANE; } else { return ""; } }
  
  /** @return the ANSI code for blue, or blank string if colors are disabled */
  public static String blue() { if(colors) { return BLUE; } else { return ""; } }
  
  /** @return the ANSI code for yellow, or blank string if colors are disabled */
  public static String yellow() { if(colors) { return YELLOW; } else { return ""; } }
  
  /** @return the ANSI code for green, or blank string if colors are disabled */
  public static String green() { if(colors) { return GREEN; } else { return ""; } }

  public static String time () {
    Calendar cal = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
    return sdf.format(cal.getTime()) + " ";
  }

  public static void d (String message) {
    p(time() + green() + message + sane());
  }

  public static void debug (String message) { d(message); }

  public static void i (String message) {
    p(time() + blue() + message + sane());
  }

  public static void info (String message) { i(message); }

  public static void w (String message) {
    p(time() + yellow() + message + sane());
  }

  public static void warning (String message) { w(message); }

  public static void e (String message) {
    p(time() + red() + message + sane());
  }

  public static void error (String message) { e(message); }

  public static void p (String message) {
    output.println(message);
  }

  /**
   * Sets or unsets the printing of ANSI color codes in log messages.
   * @param colors Boolean setting whether or not color is shown. 
   */
  public static void setColor(boolean colors) { Log.colors = colors; }
}

