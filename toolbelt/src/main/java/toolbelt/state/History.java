package toolbelt.state;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

public class History {

  private String name = "history";
  private int limit = 100;
  private UserState state;

  @Inject
  public History(UserState state) {
    this.state = state;
  }

  public void add(String token, String transcript) {
    state.addToStartOfList(name, token, transcript);
    state.trimList(name, token, limit);
  }

  public List<String> all(String token) {
    return state.getList(name, token);
  }

  public void clear(String token) {
    state.remove(name, token);
  }
}
