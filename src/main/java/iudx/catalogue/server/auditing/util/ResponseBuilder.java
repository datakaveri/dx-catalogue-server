package iudx.catalogue.server.auditing.util;

import static iudx.catalogue.server.auditing.util.Constants.DETAIL;
import static iudx.catalogue.server.auditing.util.Constants.ERROR_TYPE;
import static iudx.catalogue.server.auditing.util.Constants.FAILED;
import static iudx.catalogue.server.auditing.util.Constants.RESULTS;
import static iudx.catalogue.server.auditing.util.Constants.SUCCESS;
import static iudx.catalogue.server.auditing.util.Constants.TITLE;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

public class ResponseBuilder {
  private String status;
  private JsonObject response;

  /** Initialise the object with Success or Failure. */
  public ResponseBuilder(String status) {
    this.status = status;
    response = new JsonObject();
  }

  public ResponseBuilder setTypeAndTitle(int statusCode) {
    response.put(ERROR_TYPE, statusCode);
    if (SUCCESS.equalsIgnoreCase(status)) {
      response.put(TITLE, SUCCESS);
    } else if (FAILED.equalsIgnoreCase(status)) {
      response.put(TITLE, FAILED);
    }
    return this;
  }

  public ResponseBuilder setJsonArray(JsonArray jsonArray) {
    response.put(RESULTS, jsonArray);
    return this;
  }
}
