package core.commands;

import core.closeness.ClosestObjectFinder;
import core.exception.CannotStyleInvalidFile;
import core.exception.InvalidStyler;
import core.formattedtext.ConversionMapFactory;
import core.gen.rpc.Language;
import core.gen.rpc.StylerType;
import core.util.Comments;
import core.util.Diff;
import core.util.Range;
import core.util.Whitespace;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;
import javax.inject.Inject;
import toolbelt.env.Env;
import toolbelt.languages.LanguageDeterminer;

public class Styler {

  @Inject
  ClosestObjectFinder closestObjectFinder;

  @Inject
  Env env;

  @Inject
  LanguageDeterminer languageDeterminer;

  @Inject
  ConversionMapFactory conversionMapFactory;

  @Inject
  Whitespace whitespace;

  @Inject
  public Styler() {}

  private int applyWhitespaceOffset(String source, int cursor, int offset) {
    // Apply the whitespace offset, but make sure we don't cross into non-whitespace.
    Range whitespaceRange = whitespace.expandRight(
      source,
      whitespace.expandLeft(source, new Range(cursor, cursor))
    );

    cursor += offset;
    cursor = Math.max(whitespaceRange.start, cursor);
    cursor = Math.min(whitespaceRange.stop, cursor);
    return cursor;
  }

  // needed only for older clients, can be deprecated after clients update
  private StylerType getDefaultStyler(Language language) {
    switch (language) {
      case LANGUAGE_PYTHON:
        return StylerType.STYLER_TYPE_BLACK;
      case LANGUAGE_KOTLIN:
        return StylerType.STYLER_TYPE_KTLINT;
      case LANGUAGE_CPLUSPLUS:
        return StylerType.STYLER_TYPE_CLANG_GOOGLE;
      case LANGUAGE_CSHARP:
        return StylerType.STYLER_TYPE_CLANG_MICROSOFT;
      case LANGUAGE_GO:
        return StylerType.STYLER_TYPE_GOFMT;
      case LANGUAGE_RUST:
        return StylerType.STYLER_TYPE_RUSTFMT;
      default:
        return StylerType.STYLER_TYPE_PRETTIER;
    }
  }

  private String executeStyler(String source, Language language, StylerType styler) {
    try {
      if (styler == null || styler == StylerType.STYLER_TYPE_NONE) {
        styler = getDefaultStyler(language);
      }

      Process process = getStylerProcess(source, language, styler).start();
      OutputStream stdin = process.getOutputStream();
      InputStream stdout = process.getInputStream();
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));

      // send code to stdin
      writer.write(source);
      writer.flush();
      writer.close();

      // read stdout
      Scanner scanner = new Scanner(stdout);
      scanner.useDelimiter("\\A");
      if (scanner.hasNext()) {
        source = scanner.next();
      }

      scanner.close();
      if (process.waitFor() != 0) {
        throw new CannotStyleInvalidFile();
      }
    } catch (InterruptedException | IOException e) {
      throw new RuntimeException("Styler error", e);
    }

    return source;
  }

  private ProcessBuilder getStylerProcess(String source, Language language, StylerType styler) {
    if (languageDeterminer.extensions(language).size() == 0) {
      throw new InvalidStyler();
    }

    String path = env.libraryRoot() + "/stylers";
    switch (styler) {
      case STYLER_TYPE_AUTOPEP8:
        return new ProcessBuilder(
          "autopep8",
          "--aggressive",
          "--max-line-length",
          Integer.toString(lineWidth(source)),
          "-"
        );
      case STYLER_TYPE_BLACK:
        return new ProcessBuilder("black", "--quiet", "--fast", "-");
      case STYLER_TYPE_CLANG_GOOGLE:
        return new ProcessBuilder(
          "clang-format-9",
          "--assume-filename",
          "file." + languageDeterminer.extensions(language).get(0),
          "--style",
          "Google"
        );
      case STYLER_TYPE_CLANG_LLVM:
        return new ProcessBuilder(
          "clang-format-9",
          "--assume-filename",
          "file." + languageDeterminer.extensions(language).get(0),
          "--style",
          "LLVM"
        );
      case STYLER_TYPE_CLANG_MICROSOFT:
        return new ProcessBuilder(
          "clang-format-9",
          "--assume-filename",
          "file." + languageDeterminer.extensions(language).get(0),
          "--style",
          "Microsoft"
        );
      case STYLER_TYPE_CLANG_WEBKIT:
        return new ProcessBuilder(
          "clang-format-9",
          "--assume-filename",
          "file." + languageDeterminer.extensions(language).get(0),
          "--style",
          "WebKit"
        );
      case STYLER_TYPE_GOFMT:
        return new ProcessBuilder("gofmt");
      case STYLER_TYPE_GOOGLE_JAVA_FORMAT:
        return new ProcessBuilder("java", "-jar", path + "/google-java-format.jar", "-");
      case STYLER_TYPE_KTLINT:
        return new ProcessBuilder("ktlint", "--format", "--stdin");
      case STYLER_TYPE_PRETTIER:
        return new ProcessBuilder(
          "prettier",
          "--print-width=" + lineWidth(source),
          "--tab-width=" + indentation(source, language),
          "--stdin-filepath",
          "file." + languageDeterminer.extensions(language).get(0)
        );
      case STYLER_TYPE_STANDARD:
        return new ProcessBuilder("standard", "--stdin", "--fix");
      case STYLER_TYPE_YAPF:
        return new ProcessBuilder("yapf");
      case STYLER_TYPE_RUSTFMT:
        return new ProcessBuilder("rustfmt");
    }

    throw new InvalidStyler();
  }

  private int indentation(String source, Language language) {
    return whitespace
      .indentationToken(source, conversionMapFactory.create(language).indentation())
      .length();
  }

  // as a heuristic, count the number of line widths greater than the default and if that number is
  // greater than 5% of total lines then use the larger (max) line width
  private int lineWidth(String source) {
    int defaultWidth = 80;
    int maxWidth = 100;
    source = Comments.strip(source);

    int width = defaultWidth;
    String[] lines = source.split("\n");
    int linesCount = 0;
    int linesGreaterThanDefault = 0;
    for (String line : lines) {
      if (line.length() == 0) {
        continue;
      }

      if (line.length() > defaultWidth) {
        linesGreaterThanDefault++;
      }

      linesCount++;
    }

    return linesCount > 0 && (float) linesGreaterThanDefault / (float) linesCount > 0.05
      ? maxWidth
      : defaultWidth;
  }

  private int sourcePosition(String source, int nonWhitespacePosition, boolean start) {
    // Convert the non-whitespace index back to a regular position in the styled source.
    List<Range> ranges = whitespace.nonWhitespaceRanges(source);
    int rangeNonWhitespacePosition = 0;
    Range range = new Range(0, 0);
    for (int i = 0; i < ranges.size(); i++) {
      range = ranges.get(i);
      int size = range.stop - range.start;

      // We need to know if we're at the start of a range to disambiguate. The end
      // of one range has the same non-whitespace index as the start of the next.
      if (rangeNonWhitespacePosition + size + (start ? 0 : 1) > nonWhitespacePosition) {
        break;
      }

      rangeNonWhitespacePosition += size;
    }

    return nonWhitespacePosition - rangeNonWhitespacePosition + range.start;
  }

  public Diff style(Diff diff, Language language, StylerType styler) {
    if (styler == StylerType.STYLER_TYPE_EDITOR) {
      return diff;
    }

    String source = diff.getSource();
    int cursor = diff.getCursor();
    String styledSource = executeStyler(source, language, styler);

    // Convert cursor to a position counting only non-whitespace, and an offset into
    // whitespace. Since styling mostly just changes whitespace, we'll use this below to
    // recover our cursor position. We do this without a parser/lexer because it's
    // faster/generalized, respectively.
    List<Range> ranges = whitespace.nonWhitespaceRanges(source);
    if (ranges.size() == 0) {
      return diff.replaceSource(styledSource);
    }

    int whitespaceOffset;
    int nonWhitespacePosition;
    boolean start;
    Range range = closestObjectFinder.closestRange(source, cursor, ranges);
    int index = ranges.indexOf(range);
    int startNonWhitespacePosition = ranges
      .stream()
      .limit(index)
      .map(r -> r.stop - r.start)
      .mapToInt(i -> i)
      .sum();
    if (cursor <= range.start) {
      nonWhitespacePosition = startNonWhitespacePosition;
      whitespaceOffset = cursor - range.start;
      start = true;
    } else if (cursor >= range.stop) {
      nonWhitespacePosition = startNonWhitespacePosition + range.stop - range.start;
      whitespaceOffset = cursor - range.stop;
      start = false;
    } else {
      nonWhitespacePosition = startNonWhitespacePosition + cursor - range.start;
      whitespaceOffset = 0;
      start = true; // prioritize the right character if we get split up at this point.
    }
    int newCursor = sourcePosition(styledSource, nonWhitespacePosition, start);
    newCursor = applyWhitespaceOffset(styledSource, newCursor, whitespaceOffset);

    return diff.replaceSource(styledSource).moveCursor(newCursor);
  }
}
