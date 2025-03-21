package iudx.catalogue.server.exceptions;

public class DocNotFoundException extends RuntimeException {
  private final int statusCode = 404;
  private String id;
  private final String message;
  private final String method;

  public DocNotFoundException(String message, String method) {
    this.message = message;
    this.method = method;
  }
  public DocNotFoundException(String id, String message, String method) {
    this.id = id;
    this.message = message;
    this.method = method;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getId() {
    return id;
  }

  public String getMessage() {
    return message;
  }
  public String getMethod() {
    return method;
  }
}
