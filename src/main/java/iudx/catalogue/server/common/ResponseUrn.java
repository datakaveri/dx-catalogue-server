package iudx.catalogue.server.common;

import static iudx.catalogue.server.util.Constants.*;

import java.util.stream.Stream;

public enum ResponseUrn {
  BAD_REQUEST_URN("urn:dx:cat:badRequest", "bad request parameter"),
  INTERNAL_SERVER_ERROR("urn:dx:cat:internalServerError", "Internal Server Error"),
  INVALID_TOKEN_URN(TYPE_TOKEN_INVALID, "Token is invalid");


  private final String urn;
  private final String message;

  ResponseUrn(String urn, String message) {
    this.urn = urn;
    this.message = message;
  }

  public String getUrn() {
    return urn;
  }

  public String getMessage() {
    return message;
  }

  public String toString() {
    return "[" + urn + " : " + message + " ]";
  }
}
