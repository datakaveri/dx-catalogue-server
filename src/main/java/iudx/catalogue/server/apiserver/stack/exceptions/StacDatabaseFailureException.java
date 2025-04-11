package iudx.catalogue.server.apiserver.stack.exceptions;

import iudx.catalogue.server.exceptions.DatabaseFailureException;

public class StacDatabaseFailureException extends DatabaseFailureException {
  public StacDatabaseFailureException(String itemId, String message, String method) {
    super(itemId, message, method);
  }
}
