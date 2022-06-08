package corpusgen.mapping;

import core.ast.Ast;
import core.ast.AstFactory;
import core.ast.api.AstNode;
import core.ast.api.AstParent;
import core.ast.api.AstSyntaxError;
import core.ast.api.DefaultAstParent;
import core.gen.rpc.Language;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import toolbelt.languages.LanguageDeterminer;

@Singleton
public class FileFilter {

  @Inject
  AstFactory astFactory;

  @Inject
  LanguageDeterminer languageDeterminer;

  @Inject
  public FileFilter() {}

  public List<String> unprocessableReasons(String filename, String source, Language language) {
    String[] lines = source.split("\n");

    boolean tooShort = false;
    if (lines.length < 10) {
      tooShort = true;
    }
    boolean lineTooLong = false;
    for (String line : lines) {
      if (line.length() > 200) {
        lineTooLong = true;
        break;
      }
    }
    boolean tooManyLines = lines.length > 5000;
    boolean averageLineLengthTooLong = source.length() / (lines.length + 1) > 70;
    if (language == Language.LANGUAGE_DEFAULT) {
      lineTooLong = false;
      averageLineLengthTooLong = false;
    }

    List<String> reasons = new ArrayList<>();
    if (tooShort) {
      reasons.add("Skipped because not enough lines");
    }
    if (lineTooLong) {
      reasons.add("Skipped because contains a line that's too long");
    }
    if (tooManyLines) {
      reasons.add("Skipped because contains too many lines");
    }
    if (averageLineLengthTooLong) {
      reasons.add("Skipped because average line length too long.");
    }

    // Only check bad formatting if all the above checks pass, since Ast parsing may blow up.
    if (reasons.size() != 0) {
      return reasons;
    }

    if (reasons.size() == 0) {
      // Don't run parsing if file fails sanity checks.
      AstParent root;
      try {
        root = astFactory.createFileRoot(source, language, true, false);
      } catch (Exception e) {
        return Arrays.asList("Skipped because: Parsing error -- treesitter or postprocessor.");
      }

      if (language == Language.LANGUAGE_CSHARP) {
        // We want to skip files where function calls or definitions look like
        // int Foo (int a, int b);
        // int x = Foo (2, 3);
        long badCalls = Stream
          .concat(
            root
              .filter(e -> e instanceof Ast.Method || e instanceof Ast.Function)
              .filter(e -> e.name().isPresent())
              .map(e -> e.name().get()),
            root.find(Ast.Call.class).map(e -> e.identifier())
          )
          .map(node -> (Ast.Identifier) node)
          .filter(
            node -> node.tokenRangeWithCommentsAndWhitespace().stop > node.tokenRange().get().stop
          )
          .count();
        if (badCalls > 0) {
          return Arrays.asList(
            "Skipped because " + badCalls + " function calls have spaces between name and parens."
          );
        }
      }

      if (language == Language.LANGUAGE_CPLUSPLUS) {
        // We skip C files due to different styling:
        if (filename.endsWith(".c")) {
          return Arrays.asList("Skipped because file extension is a C file not C++");
        }

        // We want to skip files where the braces fall on the next line, e.g.:
        // int factorial()
        // {
        // }
        long badCases = root
          .filter(
            e ->
              e instanceof Ast.Method ||
              e instanceof Ast.Function ||
              e instanceof Ast.ForClause ||
              e instanceof Ast.IfClause
          )
          .map(e -> (DefaultAstParent) e)
          .flatMap(e -> e.child(Ast.EnclosedBody.class).stream())
          .filter(
            e ->
              e
                .tokensWithCommentsAndWhitespace()
                .stream()
                .findFirst()
                .filter(t -> root.tree().whitespace.isWhitespace(t))
                .filter(t -> t.code().contains("\n"))
                .isPresent()
          )
          .count();

        if (badCases > 0) {
          return Arrays.asList("Skipped because " + badCases + " braces were found on a new line.");
        }
      }
    }

    return Collections.emptyList();
  }

  public boolean processableFile(Path path, Language language) {
    String source;
    List<String> unprocessableReasons = Arrays.asList();
    try {
      source = new String(Files.readAllBytes(path));
      unprocessableReasons = unprocessableReasons(path.toString(), source, language);
    } catch (IOException e) {
      return false;
    }
    return unprocessableReasons.size() == 0;
  }
}
