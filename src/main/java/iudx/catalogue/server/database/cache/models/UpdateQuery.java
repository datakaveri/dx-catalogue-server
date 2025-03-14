package iudx.catalogue.server.database.cache.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UpdateQuery implements QueryModel {
  private final String table;
  private final Map<String, Object> values;
  private final List<Filter> filters;
  private final List<JoinClause> joins;

  public UpdateQuery(String table, Map<String, Object> values, List<Filter> filters, List<JoinClause> joins) {
    this.table = table;
    this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    this.filters = Collections.unmodifiableList(new ArrayList<>(filters));
    this.joins = Collections.unmodifiableList(new ArrayList<>(joins));
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
    params.addAll(filters.stream().map(Filter::getValue).toList());
    return params;
  }
}
