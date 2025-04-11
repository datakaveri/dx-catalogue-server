package iudx.catalogue.server.exceptions;

public class InternalServerException extends RuntimeException{
  private String id;
  private final String errorMessage;
  private final String method;
  public InternalServerException(String errorMessage, String method) {
    this.errorMessage = errorMessage;
    this.method = method;
  }
  public InternalServerException(String id, String errorMessage, String method) {
    this.id = id;
    this.errorMessage = errorMessage;
    this.method = method;
  }

  public int getStatusCode() {
    return 500;
  }
  public String getId() {
    return id;
  }
  public String getErrorMessage() {
    return errorMessage;
  }
  public String getMethod() {
    return method;
  }
}
