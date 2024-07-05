package iudx.catalogue.server.database.elastic.exception;


import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.elastic.ResponseUrn;
import org.apache.http.HttpStatus;

import static iudx.catalogue.server.util.Constants.*;

public class EsQueryException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final int statusCode;
  private final ResponseUrn urn;
  private final String message;

  public EsQueryException(final String message) {
    this(ResponseUrn.BAD_REQUEST_URN, message, HttpStatus.SC_BAD_REQUEST);
  }

  public EsQueryException(final ResponseUrn urn, final String message) {
    this(urn, message, HttpStatus.SC_BAD_REQUEST);
  }

  public EsQueryException(final ResponseUrn urn, final String message, final int statusCode) {
    super(message);
    this.urn = urn;
    this.message = message;
    this.statusCode = statusCode;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put(TYPE, urn.getUrn());
    json.put(TITLE, urn.getMessage());
    json.put(DETAIL, message);
    return json;
  }
}
