package iudx.catalogue.server.rating.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject
public class FilterRatingRequest {
  private final String id;
  private final String type;
  private final String userID;
  private String ratingID;

  public FilterRatingRequest(JsonObject json) {
    this.id = json.getString("id");
    this.type = json.getString("type");
    this.userID = json.getString("userID");
    this.ratingID = json.getString("ratingID");
  }

  public String getId() {
    return id;
  }

  public String getUserID() {
    return userID;
  }

  public String getType() {
    return type;
  }

  public String getRatingID() {
    return ratingID;
  }

  public void setRatingID(String ratingID) {
    this.ratingID = ratingID;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    if (id != null) json.put("id", id);
    if (type != null) json.put("type", type);
    if (userID != null) json.put("userID", userID);
    if (ratingID != null) json.put("ratingID", ratingID);
    return json;
  }
}
