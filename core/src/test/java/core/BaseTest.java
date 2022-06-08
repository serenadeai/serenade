package core;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;

public class BaseTest {

  protected static CoreTestComponent component = DaggerCoreTestComponent.builder().build();
  protected static String token = "7ac7fcb3-ca55-4768-98e4-5a04fe9c0e92";

  protected String fileAsString(String path) {
    try {
      return Resources.toString(Resources.getResource("cases/" + path), Charsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
