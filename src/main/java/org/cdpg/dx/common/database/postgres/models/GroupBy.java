package org.cdpg.dx.common.database.postgres.models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import java.util.List;

@DataObject
@JsonGen
public class GroupBy {
  private List<String> columns;

  public GroupBy(JsonObject json) {
    GroupByConverter.fromJson(json, this);
  }
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    GroupByConverter.toJson(this, json);
    return json;
  }

  public GroupBy(List<String> columns) {
    this.columns = columns;
  }

  public String toSQL() {
    return "GROUP BY " + String.join(", ", columns);
  }

  public List<String> getColumns() {
    return columns;
  }

  public void setColumns(List<String> columns) {
    this.columns = columns;
  }
}

