package corpusgen.mapping;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import toolbelt.env.Env;

@Singleton
public class CompositeWords {

  private final Set<String> transcriptLexicon;
  private Map<String, List<List<String>>> sequenceCache = new HashMap<>();

  @Inject
  public CompositeWords(Env env) {
    try {
      BufferedReader inputReader = new BufferedReader(
        new FileReader(
          env.sourceRoot() + "/scripts/serenade/speech_engine/lexicon/cmudict-0.7b-utf-8.txt"
        )
      );
      transcriptLexicon = new HashSet<>();
      String inputLine = new String();
      while ((inputLine = inputReader.readLine()) != null) {
        transcriptLexicon.add(inputLine.split("\\s+")[0].toLowerCase());
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    assert transcriptLexicon.size() > 3000;
  }

  private String removePluralPostfix(String word) {
    String postfix;
    if (word.endsWith("es")) {
      postfix = "es";
    } else if (word.endsWith("s")) {
      postfix = "s";
    } else {
      return word;
    }
    return word.substring(0, word.length() - postfix.length());
  }

  public List<List<String>> sequences(String word) {
    if (sequenceCache.containsKey(word)) {
      return sequenceCache.get(word);
    }
    List<List<String>> result = new ArrayList<>();
    for (int i = 1; i < word.length() - 1; i++) {
      String firstWord = word.substring(0, i);
      String secondWord = word.substring(i, word.length());
      if (
        transcriptLexicon.contains(firstWord.toLowerCase()) &&
        transcriptLexicon.contains(secondWord.toLowerCase())
      ) {
        result.add(Arrays.asList(firstWord, secondWord));
      }
    }
    sequenceCache.put(word, result);
    return result;
  }

  public boolean isCompositeWord(String word, boolean includeLexicon) {
    word = word.toLowerCase();
    if (!includeLexicon && transcriptLexicon.contains(word.toLowerCase())) {
      return false;
    }
    return sequences(word).size() != 0;
  }

  public boolean isPlural(String word) {
    return !removePluralization(word).equals(word);
  }

  public String removePluralization(String word) {
    String postfixRemoved = removePluralPostfix(word);
    if (transcriptLexicon.contains(postfixRemoved)) {
      return postfixRemoved;
    }
    return word;
  }
}
