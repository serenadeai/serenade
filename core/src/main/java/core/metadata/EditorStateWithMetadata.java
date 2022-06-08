package core.metadata;

import core.gen.rpc.CustomCommand;
import core.gen.rpc.EditorState;
import core.gen.rpc.Language;
import core.gen.rpc.StylerType;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditorStateWithMetadata {

  private String source = "";
  private int cursor = 0;
  private String filename = "";
  private String clientIdentifier = "";
  private String application = "";
  private List<CustomCommand> customCommands = Arrays.asList();
  private List<String> files = Arrays.asList();
  private List<String> roots = Arrays.asList();
  private boolean pluginInstalled = false;
  private List<String> tabs = Arrays.asList();
  private boolean nux = false;
  private Map<String, String> customWords = new HashMap<>();
  private String clipboard = "";
  private Map<Language, StylerType> stylers = new HashMap<>();
  private boolean autocomplete = false;
  private Language language = Language.LANGUAGE_DEFAULT;
  private boolean canGetState = false;
  private boolean canSetState = false;
  private boolean dictateMode = false;
  private String url = "";
  private List<String> customHints = Arrays.asList();
  private boolean logAudio = false;
  private boolean logSource = false;
  private String token = "";

  public EditorStateWithMetadata() {}

  public EditorStateWithMetadata(
    String source,
    int cursor,
    String filename,
    String clientIdentifier,
    String application,
    List<CustomCommand> customCommands,
    List<String> files,
    List<String> roots,
    boolean pluginInstalled,
    List<String> tabs,
    boolean nux,
    Map<String, String> customWords,
    String clipboard,
    Map<Language, StylerType> stylers,
    boolean autocomplete,
    Language language,
    boolean dictateMode,
    boolean canGetState,
    boolean canSetState,
    String url,
    List<String> customHints,
    boolean logAudio,
    boolean logSource,
    String token
  ) {
    this.source = source;
    this.cursor = cursor;
    this.filename = filename;
    this.clientIdentifier = clientIdentifier;
    this.application = application;
    this.customCommands = customCommands;
    this.files = files;
    this.roots = roots;
    this.pluginInstalled = pluginInstalled;
    this.tabs = tabs;
    this.nux = nux;
    this.customWords = customWords;
    this.clipboard = clipboard;
    this.stylers = stylers;
    this.autocomplete = autocomplete;
    this.language = language;
    this.dictateMode = dictateMode;
    this.canGetState = canGetState;
    this.canSetState = canSetState;
    this.url = url;
    this.customHints = customHints;
    this.logAudio = logAudio;
    this.logSource = logSource;
    this.token = token;
  }

  public EditorStateWithMetadata(EditorState state) {
    this(
      new String(state.getSource().toByteArray(), StandardCharsets.UTF_8),
      state.getCursor(),
      state.getFilename(),
      state.getClientIdentifier(),
      state.getApplication(),
      state.getCustomCommandsList(),
      state.getFilesList(),
      state.getRootsList(),
      state.getPluginInstalled(),
      state.getTabsList(),
      state.getNux(),
      state.getCustomWordsMap(),
      state.getClipboard(),
      Map.of(),
      state.getAutocomplete(),
      state.getLanguage(),
      state.getDictateMode(),
      state.getCanGetState(),
      state.getCanSetState(),
      state.getUrl(),
      state.getCustomHintsList(),
      state.getLogAudio(),
      state.getLogSource(),
      state.getToken()
    );
    this.stylers = new HashMap<>();
    for (Map.Entry<Integer, StylerType> e : state.getStylerMap().entrySet()) {
      this.stylers.put(Language.forNumber(e.getKey()), e.getValue());
    }
  }

  public EditorStateWithMetadata(EditorStateWithMetadata state) {
    this(
      state.source,
      state.cursor,
      state.filename,
      state.clientIdentifier,
      state.application,
      state.customCommands,
      state.files,
      state.roots,
      state.pluginInstalled,
      state.tabs,
      state.nux,
      state.customWords,
      state.clipboard,
      state.stylers,
      state.autocomplete,
      state.language,
      state.dictateMode,
      state.canGetState,
      state.canSetState,
      state.url,
      state.customHints,
      state.logAudio,
      state.logSource,
      state.token
    );
  }

  public String getSource() {
    return source;
  }

  public int getCursor() {
    return cursor;
  }

  public String getFilename() {
    return filename;
  }

  public String getClientIdentifier() {
    return clientIdentifier;
  }

  public String getApplication() {
    return application;
  }

  public List<CustomCommand> getCustomCommandsList() {
    return customCommands;
  }

  public List<String> getFilesList() {
    return files;
  }

  public List<String> getRootsList() {
    return roots;
  }

  public boolean getPluginInstalled() {
    return pluginInstalled;
  }

  public List<String> getTabsList() {
    return tabs;
  }

  public boolean getNux() {
    return nux;
  }

  public Map<String, String> getCustomWords() {
    return customWords;
  }

  public String getClipboard() {
    return clipboard;
  }

  public Map<Language, StylerType> getStylers() {
    return stylers;
  }

  public boolean getAutocomplete() {
    return autocomplete;
  }

  public Language getLanguage() {
    return language;
  }

  public boolean getCanGetState() {
    return canGetState;
  }

  public boolean getCanSetState() {
    return canSetState;
  }

  public boolean getDictateMode() {
    return dictateMode;
  }

  public String getUrl() {
    return url;
  }

  public List<String> getCustomHints() {
    return customHints;
  }

  public boolean getLogAudio() {
    return logAudio;
  }

  public boolean getLogSource() {
    return logSource;
  }

  public String getToken() {
    return token;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public void setCursor(int cursor) {
    this.cursor = cursor;
  }

  public void setDictateMode(boolean mode) {
    this.dictateMode = mode;
  }
}
