package iudx.catalogue.server.apiserver.stack.exceptions;

import iudx.catalogue.server.exceptions.DocNotFoundException;

public class StacNotFoundException extends DocNotFoundException {
  public StacNotFoundException(String itemId, String message, String method) {
    super(itemId, message, method);
  }
}