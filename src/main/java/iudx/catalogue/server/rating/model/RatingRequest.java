package iudx.catalogue.server.rating.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.exceptions.InvalidSchemaException;

@DataObject
public class RatingRequest {
  private static final String UUID_REGEX =
      "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$";
  private String id;
  private String userID;
  private String ratingID;
  private Double rating;
  private String comment;
  private RatingStatus status;

  public RatingRequest(JsonObject json) throws InvalidSchemaException {
    this.id = json.getString("id");
    this.userID = json.getString("userID");
    this.rating = json.getDouble("rating");
    this.comment = json.getString("comment");
    this.status = parseStatus(json.getString("status"));
    this.ratingID = json.getString("ratingID");
    validateFields();
  }

  private void validateFields() {
    if (id == null || id.isBlank()) {
      throw new InvalidSchemaException("[object has missing required properties ([\"id\"])]");
    }
    if (!id.matches(UUID_REGEX)) {
      throw new InvalidSchemaException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]", UUID_REGEX, id));
    }
    if (userID == null || userID.isBlank()) {
      throw new InvalidSchemaException("[object has missing required properties ([\"userID\"])]");
    }
    if (rating == null) {
      throw new InvalidSchemaException("[object has missing required properties ([\"rating\"])]");
    }
    if (rating > 5.0) {
      throw new InvalidSchemaException(
          String.format(
              "numeric instance is greater than the required maximum (maximum: 5, found: %.1f)",
              rating));
    }
    if (rating < 0.0) {
      throw new InvalidSchemaException(
          String.format(
              "numeric instance is lower than the required minimum (minimum: 0, found: %.1f)",
              rating));
    }
    if (status == null) {
      throw new InvalidSchemaException("[object has missing required properties ([\"status\"])]");
    }
  }

  private RatingStatus parseStatus(String status) {
    if (status == null || status.isBlank()) {
      return RatingStatus.PENDING; // Default status
    }
    try {
      return RatingStatus.valueOf(status.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new InvalidSchemaException(
          "Invalid status value, must be 'pending', 'approved', " + "or 'denied'");
    }
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    if (id != null && !id.matches(UUID_REGEX)) {
      throw new InvalidSchemaException("Invalid ID format");
    }
    this.id = id;
  }

  public Double getRating() {
    return rating;
  }

  public void setRating(Double rating) {
    if (rating < 0.0 || rating > 5.0) {
      throw new InvalidSchemaException("RatingRequest must be between 0.0 and 5.0");
    }
    this.rating = rating;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public RatingStatus getStatus() {
    return status;
  }

  public void setStatus(RatingStatus status) {
    this.status = status;
  }

  public String getUserID() {
    return userID;
  }

  public void setUserID(String userID) {
    this.userID = userID;
  }

  public String getRatingID() {
    return ratingID;
  }

  public void setRatingID(String ratingID) {
    this.ratingID = ratingID;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put("id", id);
    json.put("userID", userID);
    json.put("rating", rating);
    json.put("status", status.name().toLowerCase());
    json.put("ratingID", ratingID);
    if (comment != null) {
      json.put("comment", comment);
    }

    return json;
  }

  public enum RatingStatus {
    PENDING,
    APPROVED,
    DENIED
  }
}
