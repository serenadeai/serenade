package core.visitor;

import core.codeengine.CodeEngineBatchQueue;
import core.evaluator.ParsedTranscript;
import core.evaluator.TranscriptEvaluator;
import core.metadata.EditorStateWithMetadata;

public class CommandsVisitorContext {

  public EditorStateWithMetadata state;
  public ParsedTranscript parsed;
  public CodeEngineBatchQueue queue;
  public TranscriptEvaluator transcriptEvaluator;

  public CommandsVisitorContext(
    EditorStateWithMetadata state,
    ParsedTranscript parsed,
    CodeEngineBatchQueue queue,
    TranscriptEvaluator transcriptEvaluator
  ) {
    this.state = state;
    this.parsed = parsed;
    this.queue = queue;
    this.transcriptEvaluator = transcriptEvaluator;
  }
}
