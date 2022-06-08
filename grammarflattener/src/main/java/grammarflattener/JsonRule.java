package grammarflattener;

import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class JsonRule {

  public String type = "";
  public String name = "";
  public String value = "";
  public boolean named;
  public List<JsonRule> members = new ArrayList<>();
  public JsonRule content;

  JsonRule() {}

  @Override
  public String toString() {
    return MoreObjects
      .toStringHelper(this)
      .add("name", name)
      .add("type", type)
      .add("value", value)
      .add("members", members)
      .add("content", content)
      .toString();
  }

  @Override
  public boolean equals(Object other) {
    if (other.getClass() != getClass()) {
      return false;
    }

    var o = (JsonRule) other;
    return (
      Objects.equals(type, o.type) &&
      Objects.equals(name, o.name) &&
      Objects.equals(value, o.value) &&
      Objects.equals(named, o.named) &&
      Objects.equals(content, o.content) &&
      Objects.equals(members, o.members)
    );
  }
}
