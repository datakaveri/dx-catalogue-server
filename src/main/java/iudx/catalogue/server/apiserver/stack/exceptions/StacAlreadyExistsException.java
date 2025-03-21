package iudx.catalogue.server.apiserver.stack.exceptions;

import iudx.catalogue.server.common.ResponseUrn;
import iudx.catalogue.server.exceptions.DocAlreadyExistsException;

// Exception for STAC
public class StacAlreadyExistsException extends DocAlreadyExistsException {
  public StacAlreadyExistsException(String itemId, String message) {
    super(ResponseUrn.CONFLICT, itemId, message, message);
  }
}
