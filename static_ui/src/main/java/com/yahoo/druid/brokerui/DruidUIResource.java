package com.yahoo.druid.brokerui;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.inject.Inject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URL;

@Path("/druid/v1/ui")
public class DruidUIResource
{
  private static final String BASE_DIR = "static_ui/";

  private final ContentTypeDetector contentTypeDetector;
  private final ClassLoader classLoader;

  @Inject
  public DruidUIResource(ContentTypeDetector contentTypeDetector)
  {
    this.contentTypeDetector = contentTypeDetector;

    classLoader = DruidUIResource.class.getClassLoader();
  }

  @GET
  public Response doGet()
  {
    return processPath("index.html");
  }

  @GET
  @Path("/{path:.*}")
  public Response doGet(@PathParam("path") String path)
  {
    return Strings.isNullOrEmpty(path) ? processPath("index.html") : processPath(path);
  }

  private Response processPath(String path)
  {
    URL url = classLoader.getResource(BASE_DIR + path);
    if (url == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    // Returns a canonical file path
    String filePath = url.getFile();

    // Verify that it didn't somehow magically "cd" up through the path hierarchy and start serving stuff
    // outside of our base dir
    String prefix = filePath.substring(0, filePath.length() - path.length());
    if (prefix.endsWith(BASE_DIR)) {
      try {
        String contentType = contentTypeDetector.detect(path);

        // InputStream is closed by Jersey
        return Response.ok(url.openStream(), contentType).build();
      }
      catch (IOException e) {
        return Response.ok(null).build();
      }
    } else {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
  }
}