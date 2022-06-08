package core.util;

public class Comments {

  public static String strip(String source) {
    return source.replaceAll("\".*\"", "").replaceAll("'.*'", "").replaceAll("/\\*.*\\*/", "");
  }
}
