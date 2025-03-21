package iudx.catalogue.server.exceptions;

public class InvalidSyntaxException extends RuntimeException {
  private final String message;

  public InvalidSyntaxException(String message) {
    this.message = message;
  }

  public int getStatusCode() {
    return 400;
  }
  public String getMessage() {
    return message;
  }
}
