package replayer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MetricsPrinter {

  private List<String[]> analyzeDatapoints(List<Datapoint> datapoints) {
    return Arrays.asList(
      getOneAlternativeAndCorrect(datapoints),
      getRecallAnalysis(datapoints, 1),
      getRecallAnalysis(datapoints, 5),
      getRecallAnalysis(datapoints, 10)
    );
  }

  private String normalizeTranscript(String transcript) {
    return transcript
      .replaceAll("\\bpage up\\b", "pageup")
      .replaceAll("\\bpage down\\b", "pagedown")
      .replaceAll("\\bnewline\\b", "new line")
      .replaceAll("\\bpascalcase\\b", "pascal case")
      .replaceAll("\\bcamelcase\\b", "camel case");
  }

  private boolean transcriptsMatch(String first, String second) {
    return normalizeTranscript(first).equals(normalizeTranscript(second));
  }

  public String[] getRecallAnalysis(List<Datapoint> datapoints, int k) {
    List<Datapoint> errors = datapoints
      .stream()
      .filter(
        d -> d.prediction.transcripts.stream().limit(k).allMatch(t -> !transcriptsMatch(d.label, t))
      )
      .collect(Collectors.toList());

    int numRecalled = datapoints.size() - errors.size();
    String summary = String.format(
      "#### Recall@%d = %s / %s = %s",
      k,
      numRecalled,
      datapoints.size(),
      ((float) numRecalled) / datapoints.size()
    );

    return new String[] { summary, summary + "\n" + getErrors(errors) };
  }

  public String[] getOneAlternativeAndCorrect(List<Datapoint> datapoints) {
    List<Datapoint> firstCorrect = datapoints
      .stream()
      .filter(
        d ->
          d.prediction.transcripts.size() > 0 &&
          transcriptsMatch(d.label, d.prediction.transcripts.get(0))
      )
      .collect(Collectors.toList());

    List<Datapoint> notAutoExecuted = firstCorrect
      .stream()
      .filter(d -> d.prediction.transcripts.size() != 1)
      .collect(Collectors.toList());

    int numAutoExecuted = firstCorrect.size() - notAutoExecuted.size();
    String summary = String.format(
      "#### One alternative and correct = %s / %s = %s, %s / %s = %s",
      numAutoExecuted,
      firstCorrect.size(),
      ((float) numAutoExecuted) / firstCorrect.size(),
      numAutoExecuted,
      datapoints.size(),
      ((float) numAutoExecuted) / datapoints.size()
    );

    return new String[] { summary, summary };
  }

  public String getErrors(List<Datapoint> datapoints) {
    String result = "\n### Errors\n";
    for (Datapoint datapoint : datapoints) {
      if (
        datapoint.prediction.transcripts.size() == 0 ||
        !transcriptsMatch(datapoint.label, datapoint.prediction.transcripts.get(0))
      ) {
        result +=
          "* " +
          String.join(", ", datapoint.chunk_ids) +
          " " +
          datapoint.label +
          ": " +
          IntStream
            .range(0, datapoint.prediction.transcripts.size())
            .mapToObj(
              i ->
                datapoint.prediction.transcripts.get(i) +
                " (" +
                datapoint.prediction.acousticCosts.get(i) +
                ", " +
                datapoint.prediction.languageModelCosts.get(i) +
                ")"
            )
            .collect(Collectors.joining(", "));
      }
    }

    return result;
  }

  public void printAnalysis(List<Datapoint> datapoints, String outputPath) {
    Map<String, List<Datapoint>> byTag = new HashMap<>();
    for (Datapoint datapoint : datapoints) {
      for (String tag : datapoint.tags) {
        if (byTag.get(tag) == null) {
          byTag.put(tag, new ArrayList<>());
        }
        byTag.get(tag).add(datapoint);
      }
    }

    String result = "";
    result += "# Test set analysis results\n\n";
    List<String[]> overallResults = analyzeDatapoints(datapoints);

    result += "## Overall Summary\n";
    result += overallResults.stream().map(e -> e[0]).collect(Collectors.joining("\n")) + "\n";

    for (Map.Entry<String, List<Datapoint>> t : byTag.entrySet()) {
      List<String[]> tagResults = analyzeDatapoints(t.getValue());
      result += "## Summary for " + t.getKey() + "\n";
      result += tagResults.stream().map(e -> e[0]).collect(Collectors.joining("\n")) + "\n";
    }

    for (Map.Entry<String, List<Datapoint>> t : byTag.entrySet()) {
      List<String[]> tagResults = analyzeDatapoints(t.getValue());
      result += "## " + t.getKey() + "\n\n";
      result += tagResults.stream().map(e -> e[1]).collect(Collectors.joining("\n"));
    }

    result += "## Overall\n";
    result += overallResults.stream().map(e -> e[1]).collect(Collectors.joining("\n")) + "\n";

    try {
      File f = new File(outputPath);
      f.delete();
      Files.write(Paths.get(outputPath), (result + "\n").getBytes(), StandardOpenOption.CREATE);
      System.out.println(result);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
