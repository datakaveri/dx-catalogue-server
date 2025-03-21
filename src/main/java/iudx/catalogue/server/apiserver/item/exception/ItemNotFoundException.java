package iudx.catalogue.server.apiserver.item.exception;

import iudx.catalogue.server.exceptions.DocNotFoundException;

public class ItemNotFoundException extends DocNotFoundException {
  public ItemNotFoundException(String itemId, String method) {
    super(itemId, "Fail: Doc doesn't exist", method);
  }
}
