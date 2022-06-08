package replayer;

import java.util.List;

public class Datapoint {

  public String label;
  public List<String> chunk_ids;
  public Prediction prediction;
  public String user_id;
  public List<String> tags;
  public String state;
}
