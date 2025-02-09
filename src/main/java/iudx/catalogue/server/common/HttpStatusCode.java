package iudx.catalogue.server.common;

public enum HttpStatusCode {

  // 4xx: Client Error
  BAD_REQUEST(400, "Bad Request", "urn:dx:cat:badRequest"),
  UNAUTHORIZED(401, "Not Authorized", "urn:dx:cat:notAuthorized"),
  INVALID_TOKEN_URN(401, "Token is invalid", "urn:dx:cat:InvalidAuthorizationToken"),
  // 5xx: Server Error
  INTERNAL_SERVER_ERROR(500, "Internal Server Error", "urn:dx:cat:internalServerError");
  private final int value;
  private final String description;
  private final String urn;

  HttpStatusCode(int value, String description, String urn) {
    this.value = value;
    this.description = description;
    this.urn = urn;
  }

  public static HttpStatusCode getByValue(int value) {
    for (HttpStatusCode status : values()) {
      if (status.value == value) {
        return status;
      }
    }
    throw new IllegalArgumentException("Invalid status code: " + value);
  }

  public int getValue() {
    return value;
  }

  public String getDescription() {
    return description;
  }

  public String getUrn() {
    return urn;
  }

  @Override
  public String toString() {
    return value + " " + description;
  }
}

