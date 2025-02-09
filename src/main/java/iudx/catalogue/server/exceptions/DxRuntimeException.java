package iudx.catalogue.server.exceptions;

import iudx.catalogue.server.common.ResponseUrn;

public class DxRuntimeException extends RuntimeException {
  private final int statusCode;
  private final ResponseUrn responseUrn;
  private final String message;

  public DxRuntimeException(final int statusCode, final ResponseUrn urn) {
    super();
    this.responseUrn = urn;
    this.statusCode = statusCode;
    this.message = urn.getMessage();
  }

  public DxRuntimeException(final int statusCode, final ResponseUrn urn, String message) {
    super(message);
    this.responseUrn = urn;
    this.statusCode = statusCode;
    this.message = message;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public ResponseUrn getUrn() {
    return this.responseUrn;
  }

  public String getMessage() {
    return message;
  }
}
