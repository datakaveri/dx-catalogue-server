package iudx.catalogue.server.exceptions;

public class OperationNotAllowedException extends RuntimeException {
  private String id;
  private final String errorMessage;
  private final String method;
  public OperationNotAllowedException(String errorMessage, String method) {
    this.errorMessage = errorMessage;
    this.method = method;
  }
  public OperationNotAllowedException(String id, String errorMessage, String method) {
    this.id = id;
    this.errorMessage = errorMessage;
    this.method = method;
  }

  public int getStatusCode() {
    return 400;
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
