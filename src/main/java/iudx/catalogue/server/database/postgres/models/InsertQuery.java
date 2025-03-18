package iudx.catalogue.server.database.postgres.models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@DataObject(generateConverter = true, publicConverter = false)
public class InsertQuery implements QueryModel {
  private String table;
  private Map<String, Object> values;

  public InsertQuery(JsonObject json) {
    InsertQueryConverter.fromJson(json, this);
  }
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    InsertQueryConverter.toJson(this, json);
    return json;
  }

  public InsertQuery(String table, Map<String, Object> values) {
    this.table = table;
    this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
  }

  @Override
  public String toSQL() {
    String columns = String.join(", ", values.keySet());
    String placeholders = values.keySet().stream().map(k -> "?").collect(Collectors.joining(", "));
    return "INSERT INTO " + table + " (" + columns + ") VALUES (" + placeholders + ")";
  }

  @Override
  public List<Object> getQueryParams() {
    return new ArrayList<>(values.values());
  }

  public String getTable() {
    return table;
  }

  public void setTable(String table) {
    this.table = table;
  }

  public Map<String, Object> getValues() {
    return values;
  }

  public void setValues(Map<String, Object> values) {
    this.values = values;
  }
}
