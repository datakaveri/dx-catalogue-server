package iudx.catalogue.server.apiserver.item.exception;

import iudx.catalogue.server.exceptions.DatabaseFailureException;

public class ItemDatabaseFailureException extends DatabaseFailureException {
  public ItemDatabaseFailureException(String itemId, String message) {
    super(itemId, message);
  }
}
