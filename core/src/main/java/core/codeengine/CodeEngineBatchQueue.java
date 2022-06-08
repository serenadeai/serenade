package core.codeengine;

import codeengine.gen.rpc.RescoringAlternative;
import codeengine.gen.rpc.TranslationAlternative;
import core.gen.rpc.Language;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import toolbelt.logging.Logs;

public class CodeEngineBatchQueue {

  private static Logger logger = LoggerFactory.getLogger(CodeEngineBatchQueue.class);

  private List<SlotContext> translateInput = new ArrayList<>();
  private List<CompletableFuture<List<TranslationAlternative>>> translateOutput = new ArrayList<>();

  private List<SlotContext> rescoreInput = new ArrayList<>();
  private List<CompletableFuture<RescoringAlternative>> rescoreOutput = new ArrayList<>();

  private CodeEngineClient codeEngineClient;
  public Language language;

  @AssistedInject
  public CodeEngineBatchQueue(CodeEngineClient codeEngineClient, @Assisted Language language) {
    this.codeEngineClient = codeEngineClient;
    this.language = language;
  }

  @AssistedFactory
  public interface Factory {
    CodeEngineBatchQueue create(Language language);
  }

  public CompletableFuture<List<TranslationAlternative>> translate(SlotContext slotContext) {
    CompletableFuture<List<TranslationAlternative>> result = new CompletableFuture<>();
    translateInput.add(slotContext);
    translateOutput.add(result);
    return result;
  }

  public CompletableFuture<RescoringAlternative> rescore(SlotContext slotContext) {
    CompletableFuture<RescoringAlternative> result = new CompletableFuture<>();
    rescoreInput.add(slotContext);
    rescoreOutput.add(result);
    return result;
  }

  public void flush() {
    while (!translateInput.isEmpty() || !rescoreInput.isEmpty()) {
      List<SlotContext> translateInputCopy = new ArrayList<>(translateInput);
      List<CompletableFuture<List<TranslationAlternative>>> translateOutputCopy = new ArrayList<>(
        translateOutput
      );
      translateInput.clear();
      translateOutput.clear();

      List<SlotContext> rescoreInputCopy = new ArrayList<>(rescoreInput);
      List<CompletableFuture<RescoringAlternative>> rescoreOutputCopy = new ArrayList<>(
        rescoreOutput
      );
      rescoreInput.clear();
      rescoreOutput.clear();

      try {
        CompletableFuture<List<List<TranslationAlternative>>> translateFuture = codeEngineClient.translate(
          language,
          translateInputCopy
        );

        CompletableFuture<List<RescoringAlternative>> rescoreFuture = codeEngineClient.rescore(
          language,
          rescoreInputCopy
        );

        List<List<TranslationAlternative>> translateResult = translateFuture.get();
        List<RescoringAlternative> rescoreResult = rescoreFuture.get();

        for (int i = 0; i < translateResult.size(); i++) {
          translateOutputCopy.get(i).complete(translateResult.get(i));
        }

        for (int i = 0; i < rescoreResult.size(); i++) {
          rescoreOutputCopy.get(i).complete(rescoreResult.get(i));
        }
      } catch (InterruptedException | ExecutionException e) {
        Logs.logError(logger, "Server interrupted", e);

        for (int i = 0; i < translateInputCopy.size(); i++) {
          translateOutputCopy.get(i).completeExceptionally(e);
        }

        for (int i = 0; i < rescoreInputCopy.size(); i++) {
          rescoreOutputCopy.get(i).completeExceptionally(e);
        }
      }
    }
  }
}
