package toolbelt.server;

import java.io.IOException;
import java.io.Writer;
import javax.servlet.http.HttpServletRequest;
import org.eclipse.jetty.server.handler.ErrorHandler;

public class ApiErrorHandler extends ErrorHandler {

  @Override
  protected void writeErrorPage(
    HttpServletRequest request,
    Writer writer,
    int code,
    String message,
    boolean showStacks
  ) throws IOException {}
}
