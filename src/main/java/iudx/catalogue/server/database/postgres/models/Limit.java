package iudx.catalogue.server.database.postgres.models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true, publicConverter = false)
public class Limit {
  private int limit;

  public Limit(JsonObject json) {
    LimitConverter.fromJson(json, this);
  }
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    LimitConverter.toJson(this, json);
    return json;
  }

  public Limit(int limit) {
    if (limit < 1) {
      throw new IllegalArgumentException("Limit must be greater than 0");
    }
    this.limit = limit;
  }

  public String toSQL() {
    return "LIMIT " + limit;
  }

  public int getLimit() {
    return limit;
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }
}
