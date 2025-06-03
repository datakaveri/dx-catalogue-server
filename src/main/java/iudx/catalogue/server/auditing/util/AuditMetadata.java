package iudx.catalogue.server.auditing.util;

import static iudx.catalogue.server.auditing.util.Constants.CONSUMER;
import static iudx.catalogue.server.auditing.util.Constants.CREATE;
import static iudx.catalogue.server.auditing.util.Constants.DELETE;
import static iudx.catalogue.server.auditing.util.Constants.UPDATE;
import static iudx.catalogue.server.auditing.util.Constants.VIEW;
import static iudx.catalogue.server.util.Constants.PROVIDER;
import static iudx.catalogue.server.util.Constants.REQUEST_DELETE;
import static iudx.catalogue.server.util.Constants.REQUEST_GET;
import static iudx.catalogue.server.util.Constants.REQUEST_POST;
import static iudx.catalogue.server.util.Constants.REQUEST_PUT;

public class AuditMetadata {
  public final String itemId;
  public final String apiEndpoint;
  public final String httpMethod;
  public final String itemType;
  public final String itemName;
  public final String userId;
  public final String userRole;

  public AuditMetadata(String itemId, String apiEndpoint, String httpMethod, String itemType,
                       String itemName, String userId, String userRole) {
    this.itemId = itemId;
    this.apiEndpoint = apiEndpoint;
    this.httpMethod = httpMethod;
    this.itemType = itemType;
    this.itemName = itemName;
    this.userId = userId;
    this.userRole = userRole;
  }

  public String getOperation() {
    switch (httpMethod.toUpperCase()) {
      case REQUEST_POST:
        return CREATE;
      case REQUEST_PUT:
        return UPDATE;
      case REQUEST_DELETE:
        return DELETE;
      case REQUEST_GET:
      default:
        return VIEW;
    }
  }

  public String getRole() {
    if (httpMethod.equalsIgnoreCase(REQUEST_GET)) {
      return CONSUMER;
    } else {
      return userRole != null ? userRole : PROVIDER;
    }
  }
}


