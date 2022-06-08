package toolbelt.server;

import java.io.IOException;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class StatusServlet extends HttpServlet {

  @Inject
  public StatusServlet() {}

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    String containerId = "";
    if (System.getenv("CONTAINER_ID") != null) {
      containerId = System.getenv("CONTAINER_ID");
    }
    String commitId = "";
    if (System.getenv("GIT_COMMIT") != null) {
      commitId = System.getenv("GIT_COMMIT");
    }

    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("text/plain");
    response
      .getWriter()
      .println("{\"status\":\"ok\",\"c\":\"" + containerId + "\",\"g\":\"" + commitId + "\"}");
  }
}
