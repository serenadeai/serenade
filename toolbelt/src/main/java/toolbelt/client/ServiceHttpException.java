package toolbelt.client;

public class ServiceHttpException extends RuntimeException {

  public ServiceHttpException(String url, int status) {
    super(url + " returned " + status);
  }
}
