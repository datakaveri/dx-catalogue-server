package iudx.catalogue.server.exceptions;

import iudx.catalogue.server.common.ResponseUrn;

public class DocAlreadyExistsException extends RuntimeException {
  private final int statusCode;
  private final ResponseUrn responseUrn;
  private final String itemId;
  private final String message;
  private final String detail;

  public DocAlreadyExistsException(ResponseUrn urn, String itemId, String message, String detail) {
    super(message);
    this.detail = detail;
    this.statusCode = 409; // HTTP 409 Conflict
    this.responseUrn = urn;
    this.itemId = itemId;
    this.message = urn.getMessage();
  }

  public DocAlreadyExistsException(String itemId, String detail) {
    super();
    this.detail = detail;
    this.statusCode = 409; // HTTP 409 Conflict
    this.responseUrn = ResponseUrn.ITEM_ALREADY_EXISTS;
    this.itemId = itemId;
    this.message = responseUrn.getMessage();
  }

  public DocAlreadyExistsException(String itemId) {
    this(itemId, "Item with id " + itemId + " already exists");
  }

  public int getStatusCode() {
    return statusCode;
  }

  public ResponseUrn getUrn() {
    return responseUrn;
  }
  public String getMessage() {
    return message;
  }

  public String getItemId() {
    return itemId;
  }

  public String getDetail() {
    return detail;
  }
}

