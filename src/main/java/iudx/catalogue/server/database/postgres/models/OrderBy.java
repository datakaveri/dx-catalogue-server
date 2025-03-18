package iudx.catalogue.server.database.postgres.models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true, publicConverter = false)
public class OrderBy {
  private String column;
  private String direction;

  public OrderBy(JsonObject json) {
    OrderByConverter.fromJson(json, this);
  }
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    OrderByConverter.toJson(this, json);
    return json;
  }

  public OrderBy(String column, String direction) {
    if (!direction.equalsIgnoreCase("ASC") && !direction.equalsIgnoreCase("DESC")) {
      throw new IllegalArgumentException("Invalid order direction: " + direction);
    }
    this.column = column;
    this.direction = direction;
  }

  public String toSQL() {
    return "ORDER BY " + column + " " + direction;
  }

  public String getColumn() {
    return column;
  }

  public void setColumn(String column) {
    this.column = column;
  }

  public String getDirection() {
    return direction;
  }

  public void setDirection(String direction) {
    this.direction = direction;
  }
}
