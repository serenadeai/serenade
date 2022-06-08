package core.snippet;

import core.formattedtext.FormattedTextConverter;
import core.formattedtext.FormattedTextOptions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SnippetTrigger {

  private static String sigil = ":";
  public static Pattern slotPattern = Pattern.compile("<%\\s*(.+?)\\s*%>");
  public static String wildcardPattern = ".+?";

  public List<String> slots;
  public String trigger;
  public Pattern pattern;
  public Optional<String> description = Optional.empty();

  public SnippetTrigger(String trigger) {
    this.trigger = trigger;
    List<String> slots = new ArrayList<>();
    Matcher match = slotPattern.matcher(trigger);
    StringBuffer buffer = new StringBuffer();
    while (match.find()) {
      String slot = match.group(1).trim();
      String pattern = slotToPattern(slot);
      String[] split = slot.split(sigil);
      String name = split[0];

      match.appendReplacement(buffer, "(?<" + name + ">" + pattern + ")");
      slots.add(slot);
    }
    match.appendTail(buffer);

    this.pattern = Pattern.compile("^" + buffer.toString() + "$");
    this.slots = slots;
  }

  public SnippetTrigger(String trigger, String description) {
    this(trigger);
    this.description = Optional.of(description);
  }

  private static String slotToPattern(String slot) {
    String[] split = slot.split(sigil);
    String name = split[0];
    String result = wildcardPattern;

    if (name.equals("modifiers")) {
      result =
        "(abstract|async|default|export|final|private|protected|pub|public|static|val|var|\\\\s)+";
    } else if (name.equals("primitive")) {
      result = "(int|float|double|long|short|char|byte|boolean)";
    }

    if (split.length > 1) {
      String joined = "";
      for (int i = 1; i < split.length; i++) {
        joined += split[i];
        if (i != split.length - 1) {
          joined += sigil;
        }
      }

      result = joined;
    }

    return result;
  }

  public Map<String, String> getSlotValuesFromTranscript(String transcript) {
    Map<String, String> result = new HashMap<>();
    Matcher transcriptMatcher = pattern.matcher(transcript);
    transcriptMatcher.find();

    // iterate over all of the slots defined in the trigger, and find the corresponding string in the transcript
    // by using the named capture group with the slot's name
    for (String slot : slots) {
      String name = slot.split(sigil)[0].trim();
      String match = transcriptMatcher.group(name);
      if (match != null && !match.equals("")) {
        result.put(name, match);
      }
    }

    return result;
  }

  public boolean matches(String transcript) {
    return pattern.matcher(transcript).matches();
  }

  public boolean wildcard() {
    return slots.size() > 0;
  }
}
