package core.visitor;

import core.parser.ParseTree;
import javax.inject.Inject;

public class KeyConverter {

  @Inject
  public KeyConverter() {}

  public String convert(ParseTree node) {
    if (node.getTerminal("control").isPresent()) {
      return "control";
    } else if (node.getTerminal("ctrl").isPresent()) {
      return "control";
    } else if (node.getTerminal("command").isPresent()) {
      return "command";
    } else if (node.getTerminal("alt").isPresent()) {
      return "alt";
    } else if (node.getTerminal("option").isPresent()) {
      return "option";
    } else if (node.getTerminal("shift").isPresent()) {
      return "shift";
    } else if (node.getTerminal("function").isPresent()) {
      return "function";
    } else if (node.getTerminal("windows").isPresent()) {
      return "windows";
    } else if (node.getTerminal("win").isPresent()) {
      return "win";
    } else if (node.getTerminal("meta").isPresent()) {
      return "meta";
    }

    return "";
  }

  public String convert(ParseTree node, boolean isMac) {
    if (node.getTerminal("f").isPresent() && node.getTerminal("one").isPresent()) {
      return "f1";
    } else if (node.getTerminal("f").isPresent() && node.getTerminal("two").isPresent()) {
      return "f2";
    } else if (node.getTerminal("f").isPresent() && node.getTerminal("three").isPresent()) {
      return "f3";
    } else if (node.getTerminal("f").isPresent() && node.getTerminal("four").isPresent()) {
      return "f4";
    } else if (node.getTerminal("f").isPresent() && node.getTerminal("five").isPresent()) {
      return "f5";
    } else if (node.getTerminal("f").isPresent() && node.getTerminal("six").isPresent()) {
      return "f6";
    } else if (node.getTerminal("f").isPresent() && node.getTerminal("seven").isPresent()) {
      return "f7";
    } else if (node.getTerminal("f").isPresent() && node.getTerminal("eight").isPresent()) {
      return "f8";
    } else if (node.getTerminal("f").isPresent() && node.getTerminal("nine").isPresent()) {
      return "f9";
    } else if (node.getTerminal("f").isPresent() && node.getTerminal("ten").isPresent()) {
      return "f10";
    } else if (node.getTerminal("f").isPresent() && node.getTerminal("eleven").isPresent()) {
      return "f11";
    } else if (node.getTerminal("f").isPresent() && node.getTerminal("twelve").isPresent()) {
      return "f12";
    } else if (node.getTerminal("one").isPresent()) {
      return "1";
    } else if (node.getTerminal("two").isPresent()) {
      return "2";
    } else if (node.getTerminal("three").isPresent()) {
      return "3";
    } else if (node.getTerminal("four").isPresent()) {
      return "4";
    } else if (node.getTerminal("five").isPresent()) {
      return "5";
    } else if (node.getTerminal("six").isPresent()) {
      return "6";
    } else if (node.getTerminal("seven").isPresent()) {
      return "7";
    } else if (node.getTerminal("eight").isPresent()) {
      return "8";
    } else if (node.getTerminal("nine").isPresent()) {
      return "9";
    } else if (node.getTerminal("zero").isPresent()) {
      return "0";
    } else if (node.getTerminal("a").isPresent()) {
      return "a";
    } else if (node.getTerminal("b").isPresent()) {
      return "b";
    } else if (node.getTerminal("c").isPresent()) {
      return "c";
    } else if (node.getTerminal("d").isPresent()) {
      return "d";
    } else if (node.getTerminal("e").isPresent()) {
      return "e";
    } else if (node.getTerminal("f").isPresent()) {
      return "f";
    } else if (node.getTerminal("g").isPresent()) {
      return "g";
    } else if (node.getTerminal("h").isPresent()) {
      return "h";
    } else if (node.getTerminal("i").isPresent()) {
      return "i";
    } else if (node.getTerminal("j").isPresent()) {
      return "j";
    } else if (node.getTerminal("k").isPresent()) {
      return "k";
    } else if (node.getTerminal("l").isPresent()) {
      return "l";
    } else if (node.getTerminal("m").isPresent()) {
      return "m";
    } else if (node.getTerminal("n").isPresent()) {
      return "n";
    } else if (node.getTerminal("o").isPresent()) {
      return "o";
    } else if (node.getTerminal("p").isPresent()) {
      return "p";
    } else if (node.getTerminal("q").isPresent()) {
      return "q";
    } else if (node.getTerminal("r").isPresent()) {
      return "r";
    } else if (node.getTerminal("s").isPresent()) {
      return "s";
    } else if (node.getTerminal("t").isPresent()) {
      return "t";
    } else if (node.getTerminal("u").isPresent()) {
      return "u";
    } else if (node.getTerminal("v").isPresent()) {
      return "v";
    } else if (node.getTerminal("w").isPresent()) {
      return "w";
    } else if (node.getTerminal("x").isPresent()) {
      return "x";
    } else if (node.getTerminal("y").isPresent()) {
      return "y";
    } else if (node.getTerminal("z").isPresent()) {
      return "z";
    } else if (node.getTerminal("semicolon").isPresent()) {
      return ";";
    } else if (node.getTerminal("colon").isPresent()) {
      return ":";
    } else if (node.getTerminal("quote").isPresent()) {
      return "'";
    } else if (node.getTerminal("right").isPresent() && node.getTerminal("bracket").isPresent()) {
      return "]";
    } else if (node.getTerminal("bracket").isPresent()) {
      return "[";
    } else if (node.getTerminal("right").isPresent() && node.getTerminal("brace").isPresent()) {
      return "}";
    } else if (node.getTerminal("brace").isPresent()) {
      return "{";
    } else if (node.getTerminal("forward").isPresent() && node.getTerminal("slash").isPresent()) {
      return "\\";
    } else if (node.getTerminal("pipe").isPresent()) {
      return "|";
    } else if (node.getTerminal("comma").isPresent()) {
      return ",";
    } else if (node.getTerminal("period").isPresent()) {
      return ".";
    } else if (node.getTerminal("dot").isPresent()) {
      return ".";
    } else if (node.getTerminal("slash").isPresent()) {
      return "/";
    } else if (node.getTerminal("question").isPresent()) {
      return "?";
    } else if (node.getTerminal("escape").isPresent()) {
      return "escape";
    } else if (node.getTerminal("dash").isPresent()) {
      return "-";
    } else if (node.getTerminal("minus").isPresent()) {
      return "-";
    } else if (node.getTerminal("underscore").isPresent()) {
      return "_";
    } else if (node.getTerminal("equal").isPresent()) {
      return "=";
    } else if (node.getTerminal("equals").isPresent()) {
      return "=";
    } else if (node.getTerminal("plus").isPresent()) {
      return "+";
    } else if (node.getTerminal("tick").isPresent() || node.getTerminal("backtick").isPresent()) {
      return "`";
    } else if (node.getTerminal("tilde").isPresent()) {
      return "~";
    } else if (node.getTerminal("bang").isPresent()) {
      return "!";
    } else if (node.getTerminal("exclamation").isPresent()) {
      return "!";
    } else if (node.getTerminal("at").isPresent()) {
      return "@";
    } else if (node.getTerminal("hash").isPresent() || node.getTerminal("pound").isPresent()) {
      return "#";
    } else if (node.getTerminal("dollar").isPresent()) {
      return "$";
    } else if (node.getTerminal("percent").isPresent()) {
      return "%";
    } else if (node.getTerminal("caret").isPresent()) {
      return "^";
    } else if (node.getTerminal("ampersand").isPresent()) {
      return "&";
    } else if (node.getTerminal("star").isPresent()) {
      return "*";
    } else if (node.getTerminal("right").isPresent() && node.getTerminal("paren").isPresent()) {
      return ")";
    } else if (node.getTerminal("paren").isPresent()) {
      return "(";
    } else if (node.getTerminal("up").isPresent() && !node.getTerminal("page").isPresent()) {
      return "up";
    } else if (node.getTerminal("down").isPresent() && !node.getTerminal("page").isPresent()) {
      return "down";
    } else if (node.getTerminal("left").isPresent()) {
      return "left";
    } else if (node.getTerminal("right").isPresent()) {
      return "right";
    } else if (node.getTerminal("return").isPresent()) {
      return "enter";
    } else if (node.getTerminal("home").isPresent()) {
      return "home";
    } else if (node.getTerminal("end").isPresent()) {
      return "end";
    } else if (node.getTerminal("delete").isPresent()) {
      if (node.getTerminal("forward").isPresent() && isMac) return "forwarddelete";
      return "delete";
    } else if (node.getTerminal("enter").isPresent()) {
      return "enter";
    } else if (node.getTerminal("escape").isPresent()) {
      return "escape";
    } else if (node.getTerminal("tab").isPresent()) {
      return "tab";
    } else if (node.getTerminal("backspace").isPresent()) {
      return "backspace";
    } else if (node.getTerminal("pageup").isPresent()) {
      return "pageup";
    } else if (node.getTerminal("pagedown").isPresent()) {
      return "pagedown";
    } else if (node.getTerminal("page").isPresent() && node.getTerminal("up").isPresent()) {
      return "pageup";
    } else if (node.getTerminal("page").isPresent() && node.getTerminal("down").isPresent()) {
      return "pagedown";
    } else if (node.getTerminal("space").isPresent()) {
      return "space";
    } else if (node.getChild("implicitKey").isPresent()) {
      return convert(node.getChild("implicitKey").get(), isMac);
    }
    return "";
  }
}
