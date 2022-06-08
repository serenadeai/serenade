package core.util;

public class LinePositionConverter {

  int[] newlineCounts;

  public LinePositionConverter(String source) {
    newlineCounts = new int[source.length() + 1];
    newlineCounts[0] = 0;
    for (int i = 1; i <= source.length(); i++) {
      newlineCounts[i] = newlineCounts[i - 1];
      if (source.charAt(i - 1) == '\n') {
        newlineCounts[i]++;
      }
    }
  }

  public int position(int position) {
    // part of hack to handle end of file list add.
    if (position == newlineCounts.length) {
      return newlineCounts[position - 1] + 1;
    }
    return newlineCounts[position];
  }

  public Range range(Range range) {
    return new Range(position(range.start), position(range.stop) + 1);
  }
}
