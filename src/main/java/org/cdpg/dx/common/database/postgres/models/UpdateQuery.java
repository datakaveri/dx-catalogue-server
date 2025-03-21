package org.cdpg.dx.common.database.postgres.models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@DataObject
@JsonGen
public class UpdateQuery implements QueryModel {
  private String table;
  private Map<String, Object> values;
  private List<Filter> filters;
  private List<JoinClause> joins;

  public UpdateQuery(JsonObject json) {
    UpdateQueryConverter.fromJson(json, this);
  }
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    UpdateQueryConverter.toJson(this, json);
    return json;
  }

  public UpdateQuery(String table, Map<String, Object> values, List<Filter> filters, List<JoinClause> joins) {
    this.table = table;
    this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    this.filters = List.copyOf(filters);
    this.joins = List.copyOf(joins);
  }

  @Override
  public String toSQL() {
    String setClause = values.keySet().stream().map(k -> k + " = ?").collect(Collectors.joining(", "));
    String joinClause = joins.stream().map(JoinClause::toSQL).collect(Collectors.joining(" "));
    String whereClause = filters.stream().map(Filter::toSQL).collect(Collectors.joining(" AND "));
    return "UPDATE " + table + " " + joinClause + " SET " + setClause + " WHERE " + whereClause;
  }

  @Override
  public List<Object> getQueryParams() {
    List<Object> params = new ArrayList<>(values.values());
    params.addAll(filters.stream()
        .map(Filter::getValue)
        .toList());
    return params;
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

  public List<Filter> getFilters() {
    return filters;
  }

  public void setFilters(List<Filter> filters) {
    this.filters = filters;
  }

  public List<JoinClause> getJoins() {
    return joins;
  }

  public void setJoins(List<JoinClause> joins) {
    this.joins = joins;
  }

}
