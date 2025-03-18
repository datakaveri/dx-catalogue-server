package iudx.catalogue.server.database.postgres.models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@DataObject(generateConverter = true, publicConverter = false)
public class DeleteQuery implements QueryModel {
  private String table;
  private List<Filter> filters;
  public DeleteQuery(JsonObject json) {
    DeleteQueryConverter.fromJson(json, this);
  }
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    DeleteQueryConverter.toJson(this, json);
    return json;
  }

  public DeleteQuery(String table, List<Filter> filters) {
    this.table = table;
    this.filters = Collections.unmodifiableList(new ArrayList<>(filters));
  }

  @Override
  public String toSQL() {
    String whereClause = filters.stream().map(Filter::toSQL).collect(Collectors.joining(" AND "));
    return "DELETE FROM " + table + " WHERE " + whereClause;
  }

  @Override
  public List<Object> getQueryParams() {
    return filters.stream()
        .map(Filter::getValue)
        .collect(Collectors.toList());
  }

  public String getTable() {
    return table;
  }

  public void setTable(String table) {
    this.table = table;
  }

  public List<Filter> getFilters() {
    return filters;
  }

  public void setFilters(List<Filter> filters) {
    this.filters = filters;
  }
}
