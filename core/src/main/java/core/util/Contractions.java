package core.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Contractions {

  public Map<String, String> contractionMatchToFormatted;
  public Pattern contractionPattern;

  public final List<String> all = Arrays.asList(
    "'cause",
    "I'd",
    "I'd've",
    "I'll",
    "I'll've",
    "I'm",
    "I've",
    "ain't",
    "aren't",
    "can't",
    "can't've",
    "could've",
    "couldn't",
    "couldn't've",
    "didn't",
    "doesn't",
    "don't",
    "hadn't",
    "hadn't've",
    "hasn't",
    "haven't",
    "he'd",
    "he'd've",
    "he'll",
    "he'll've",
    "he's",
    "here's",
    "how'd",
    "how'd'y",
    "how'll",
    "how's",
    "isn't",
    "it'd",
    "it'd've",
    "it'll",
    "it'll've",
    "it's",
    "let's",
    "ma'am",
    "mayn't",
    "might've",
    "mightn't",
    "mightn't've",
    "must've",
    "mustn't",
    "mustn't've",
    "needn't",
    "needn't've",
    "o'clock",
    "oughtn't",
    "oughtn't've",
    "sha'n't",
    "shan't",
    "shan't've",
    "she'd",
    "she'd've",
    "she'll",
    "she'll've",
    "she's",
    "should've",
    "shouldn't",
    "shouldn't've",
    "so's",
    "so've",
    "that'd",
    "that'd've",
    "that's",
    "there'd",
    "there'd've",
    "there's",
    "they'd",
    "they'd've",
    "they'll",
    "they'll've",
    "they're",
    "they've",
    "to've",
    "wasn't",
    "we'd",
    "we'd've",
    "we'll",
    "we'll've",
    "we're",
    "we've",
    "weren't",
    "what'll",
    "what'll've",
    "what're",
    "what's",
    "what've",
    "when's",
    "when've",
    "where'd",
    "where's",
    "where've",
    "who'll",
    "who'll've",
    "who's",
    "who've",
    "why's",
    "why've",
    "will've",
    "won't",
    "won't've",
    "would've",
    "wouldn't",
    "wouldn't've",
    "y'all",
    "y'all'd",
    "y'all'd've",
    "y'all're",
    "y'all've",
    "y'alls",
    "you'd",
    "you'd've",
    "you'll",
    "you'll've",
    "you're",
    "you've"
  );

  @Inject
  public Contractions() {
    this.contractionMatchToFormatted = new HashMap<String, String>();
    for (String c : this.all) {
      this.contractionMatchToFormatted.put(c.toLowerCase().replaceAll("'", " ' "), c);
    }

    this.contractionPattern =
      Pattern.compile(
        contractionMatchToFormatted
          .keySet()
          .stream()
          .map(c -> "\\b" + c + "\\b")
          .collect(Collectors.joining("|"))
      );
  }
}
