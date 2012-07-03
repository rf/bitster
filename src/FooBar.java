import java.util.Random;

class FooBar {
  
  public static void main (String[] args) {
    System.out.println("derp");
  }

  public static String strfry(String s)
  {
	  String result = "";
	  Random r = new Random();
	  while(!s.isEmpty())
	  {
		  int id = r.nextInt(s.length());
		  result += s.charAt(id);
		  s = s.substring(0,id) + s.substring(id+1);
	  }
	  return result;
  }
}
