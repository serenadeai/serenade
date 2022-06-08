package grammarflattener;

import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonRoot {

  // Represents the root level of the grammar.json files.
  // We only add the json fields we need.
  public String name = "";
  public Map<String, JsonRule> rules = new HashMap<>();
  public List<String> inline = new ArrayList<>();
  public List<String> supertypes = new ArrayList<>();

  JsonRoot() {}

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("name", name).add("rules", rules).toString();
  }
}
