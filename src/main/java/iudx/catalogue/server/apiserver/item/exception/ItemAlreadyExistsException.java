package iudx.catalogue.server.apiserver.item.exception;

import iudx.catalogue.server.exceptions.DocAlreadyExistsException;

public class ItemAlreadyExistsException extends DocAlreadyExistsException {
  public ItemAlreadyExistsException(String itemId) {
    super(itemId, " Fail: Doc Already Exists");
  }
}
