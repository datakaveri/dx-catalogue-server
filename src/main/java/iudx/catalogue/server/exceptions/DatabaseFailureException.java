package iudx.catalogue.server.exceptions;

import iudx.catalogue.server.apiserver.item.util.ItemCategory;
public class DatabaseFailureException extends RuntimeException {
  private final int statusCode;
  private ItemCategory mode;
  private String id;
  private final String message;
  private String method;

  public DatabaseFailureException(ItemCategory mode, String id, String message) {
    this.id = id;
    this.message = message;
    this.statusCode = 500; // HTTP 500 Internal Server Error
    this.mode = mode;
  }
  public DatabaseFailureException(String id, String message) {
    this.id = id;
    this.message = message;
    this.statusCode = 500; // HTTP 500 Internal Server Error
  }
  public DatabaseFailureException(String id, String message, String method) {
    this.id = id;
    this.message = message;
    this.statusCode = 500; // HTTP 500 Internal Server Error
    this.method = method;
  }
  public DatabaseFailureException(String message) {
    this.message = message;
    this.statusCode = 500; // HTTP 500 Internal Server Error
  }

  public int getStatusCode() {
    return statusCode;
  }
  public ItemCategory getMode() {
    return mode;
  }

  public String getMessage() {
    return message;
  }

  public String getId() {
    return id;
  }
  public String getMethod() {
    return method;
  }
}
