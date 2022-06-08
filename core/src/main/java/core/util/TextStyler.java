package core.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TextStyler {

  private Pattern allCaps = Pattern.compile("^[A-Z]+(_[A-Z]+)*");
  private Pattern camelCase = Pattern.compile("^[a-z][a-zA-Z]*");
  private Pattern capitalized = Pattern.compile("^[A-Z][a-z]*");
  private Pattern dashes = Pattern.compile("^[a-z]+(-[a-z]+)*");
  private Pattern lowerCase = Pattern.compile("^[a-z ]+");
  private Pattern pascalCase = Pattern.compile("^[A-Z][a-zA-Z]*");
  private Pattern replaceCamelCase = Pattern.compile("([a-z])([A-Z])");
  private Pattern replacePascalCase = Pattern.compile("([A-Z])([A-Z][a-z])");
  private Pattern underscores = Pattern.compile("^[a-z]+(_[a-z]+)*");

  @Inject
  public TextStyler() {}

  private boolean isAlphanumeric(char c) {
    return Character.isDigit(c) || Character.isLetter(c);
  }

  public Set<TextStyle> getStyle(String s) {
    Set<TextStyle> ret = new HashSet<>();
    if (lowerCase.matcher(s).matches()) {
      ret.add(TextStyle.LOWERCASE);
    }
    if (underscores.matcher(s).matches()) {
      ret.add(TextStyle.UNDERSCORES);
    }
    if (camelCase.matcher(s).matches()) {
      ret.add(TextStyle.CAMEL_CASE);
    }
    if (pascalCase.matcher(s).matches()) {
      ret.add(TextStyle.PASCAL_CASE);
    }
    if (capitalized.matcher(s).matches()) {
      ret.add(TextStyle.CAPITALIZED);
    }
    if (allCaps.matcher(s).matches()) {
      ret.add(TextStyle.ALL_CAPS);
    }
    if (dashes.matcher(s).matches()) {
      ret.add(TextStyle.DASHES);
    }
    if (ret.size() == 0) {
      ret.add(TextStyle.UNKNOWN);
    }

    return ret;
  }

  public String style(String s, TextStyle style) {
    if (style == TextStyle.ALL_CAPS) {
      return toAllCaps(s);
    } else if (style == TextStyle.CAMEL_CASE) {
      return toCamelCase(s);
    } else if (style == TextStyle.CAPITALIZED) {
      return toCapitalized(s);
    } else if (style == TextStyle.DASHES) {
      return toDashes(s);
    } else if (style == TextStyle.PASCAL_CASE) {
      return toPascalCase(s);
    } else if (style == TextStyle.UNDERSCORES) {
      return toUnderscores(s);
    } else if (style == TextStyle.LOWERCASE) {
      return toLowerCase(s);
    } else if (style == TextStyle.TITLE_CASE) {
      return toTitleCase(s);
    }

    return s;
  }

  public String removeStyle(String s, boolean removeContractionQuotes) {
    s = replaceCamelCase.matcher(s).replaceAll("$1 $2");
    s = replacePascalCase.matcher(s).replaceAll("$1 $2");
    s = s.replace("_", " ");
    s = s.replace("-", " ");
    s = s.toLowerCase();
    if (removeContractionQuotes) {
      s = s.replace("'", "");
    }

    return s;
  }

  public String toAllCaps(String string) {
    string = removeStyle(string, true);
    return toUnderscores(string).toUpperCase();
  }

  public String toCapitalized(String string) {
    string = removeStyle(string, false);
    return Character.toUpperCase(string.charAt(0)) + string.substring(1);
  }

  public String toCamelCase(String string) {
    String s = removeStyle(string, true).trim().replaceAll(" +", " ");
    String result = "";
    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i) == ' ') {
        result += Character.toUpperCase(s.charAt(i + 1));
        i++;
      } else {
        result += s.charAt(i);
      }
    }

    return result;
  }

  public String toPascalCase(String string) {
    String s = removeStyle(string, true).trim().replaceAll(" +", " ");
    String result = "";
    for (int i = 0; i < s.length(); i++) {
      if (i == 0) {
        result += Character.toUpperCase(s.charAt(i));
      } else if (s.charAt(i) == ' ') {
        result += Character.toUpperCase(s.charAt(i + 1));
        i++;
      } else {
        result += s.charAt(i);
      }
    }
    return result;
  }

  public String toTitleCase(String string) {
    String s = removeStyle(string, false);
    String result = "";
    for (int i = 0; i < s.length(); i++) {
      if (i == 0) {
        result += Character.toUpperCase(s.charAt(i));
      } else if (s.charAt(i) == ' ') {
        result += s.charAt(i);
        result += Character.toUpperCase(s.charAt(i + 1));
        i++;
      } else {
        result += s.charAt(i);
      }
    }
    return result;
  }

  public String toUnderscores(String string) {
    return removeStyle(string, true).trim().replaceAll(" +", "_");
  }

  public String toDashes(String string) {
    return removeStyle(string, true).trim().replaceAll(" +", "-");
  }

  public String toLowerCase(String string) {
    string = removeStyle(string, false);
    return string.toLowerCase();
  }
}
