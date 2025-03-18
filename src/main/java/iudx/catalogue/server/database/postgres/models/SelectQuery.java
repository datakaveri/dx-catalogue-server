package iudx.catalogue.server.database.postgres.models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@DataObject(generateConverter = true, publicConverter = false)
public class SelectQuery implements QueryModel {
  private static final Logger LOGGER = LogManager.getLogger(SelectQuery.class);
  private String table;
  private List<String> columns;
  private List<Filter> filters;
  private List<JoinClause> joins;
  private GroupBy groupBy;
  private OrderBy orderBy;
  private Limit limit;
  private List<Object> queryParams;
  public SelectQuery(JsonObject json) {
    LOGGER.debug("Constructing SelectQuery from JSON: {}", json);
    SelectQueryConverter.fromJson(json, this);
    LOGGER.debug("Constructed SelectQuery: {}", this.toJson());
  }
  public SelectQuery(String table, List<String> columns, List<Filter> filters, List<JoinClause> joins,
                     GroupBy groupBy, OrderBy orderBy, Limit limit) {
    this.table = table;
    this.columns = columns != null ? columns : List.of("*");
    this.filters = filters != null ? filters : List.of();
    this.joins = joins != null ? joins : List.of();
    this.groupBy = groupBy;
    this.orderBy = orderBy;
    this.limit = limit;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    SelectQueryConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toSQL() {
    String colString = String.join(", ", columns);
    String joinClause = joins.isEmpty() ? "" : " " + joins.stream().map(JoinClause::toSQL).collect(Collectors.joining(" "));
    String whereClause = filters.isEmpty() ? "" : " WHERE " + filters.stream()
        .map(filter -> {
          if (filter.getOperator().equalsIgnoreCase("IS NOT") && filter.getValue() == null) {
            return filter.getField() + " IS NOT NULL";
          } else {
            return filter.toSQL();
          }
        })
        .collect(Collectors.joining(" AND "));

    String groupByClause = groupBy != null ? " " + groupBy.toSQL() : "";
    String orderByClause = orderBy != null ? " " + orderBy.toSQL() : "";
    String limitClause = limit != null ? " " + limit.toSQL() : "";

    return "SELECT " + colString + " FROM " + table + joinClause + whereClause + groupByClause + orderByClause + limitClause;
  }

  @Override
  public List<Object> getQueryParams() {
    if (queryParams == null) { // Lazy initialization
      queryParams = new ArrayList<>();
      for (Filter filter : filters) {
        if (!(filter.getOperator().equalsIgnoreCase("IS NOT") && filter.getValue() == null)) {
          JsonArray value = filter.getValue();
          if (value != null) {
            for (int i = 0; i < value.size(); i++) {
              queryParams.add(value.getString(i)); // Ensure values are strings
            }
          } else {
            queryParams.add(value);
          }
        }
      }
    }
    return queryParams;
  }

  public void setQueryParams(List<Object> queryParams) {
    this.queryParams = queryParams;
  }

  public List<Filter> getFilters() {
    return filters;
  }

  public void setFilters(List<Filter> filters) {
    this.filters = filters;
  }

  public String getTable() {
    return table;
  }

  public void setTable(String table) {
    this.table = table;
  }

  public List<String> getColumns() {
    return columns;
  }

  public void setColumns(List<String> columns) {
    this.columns = columns;
  }

  public List<JoinClause> getJoins() {
    return joins;
  }

  public void setJoins(List<JoinClause> joins) {
    this.joins = joins;
  }

  public GroupBy getGroupBy() {
    return groupBy;
  }

  public void setGroupBy(GroupBy groupBy) {
    this.groupBy = groupBy;
  }

  public OrderBy getOrderBy() {
    return orderBy;
  }

  public void setOrderBy(OrderBy orderBy) {
    this.orderBy = orderBy;
  }

  public Limit getLimit() {
    return limit;
  }

  public void setLimit(Limit limit) {
    this.limit = limit;
  }

}

