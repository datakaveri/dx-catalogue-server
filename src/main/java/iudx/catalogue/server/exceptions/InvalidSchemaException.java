package iudx.catalogue.server.exceptions;

import iudx.catalogue.server.common.ResponseUrn;

public class InvalidSchemaException extends RuntimeException {
  private final int statusCode;
  private final ResponseUrn responseUrn;
  private final String message;
  private final String errorMessage;
  private boolean isStacInstance = false;


  public InvalidSchemaException(String errorMessage) {
    super();
    this.errorMessage = errorMessage;
    this.statusCode = 400;
    this.responseUrn = ResponseUrn.INVALID_SCHEMA_URN;
    this.message = responseUrn.getMessage();
  }
  public InvalidSchemaException(String message, boolean isStacInstance) {
    super();
    this.errorMessage = message;
    this.statusCode = 400;
    this.responseUrn = ResponseUrn.INVALID_SCHEMA_URN;
    this.message = responseUrn.getMessage();
    this.isStacInstance = isStacInstance;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public ResponseUrn getUrn() {
    return responseUrn;
  }
  public boolean isStacInstance() {
    return isStacInstance;
  }
  @Override
  public String getMessage() {
    return message;
  }
}
