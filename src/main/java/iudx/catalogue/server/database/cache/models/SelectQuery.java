package iudx.catalogue.server.database.cache.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SelectQuery implements QueryModel {
  private final String table;
  private final List<String> columns;
  private final List<Filter> filters;
  private final List<JoinClause> joins;

  public SelectQuery(String table, List<String> columns, List<Filter> filters, List<JoinClause> joins) {
    this.table = table;
    this.columns = Collections.unmodifiableList(new ArrayList<>(columns));
    this.filters = Collections.unmodifiableList(new ArrayList<>(filters));
    this.joins = Collections.unmodifiableList(new ArrayList<>(joins));
  }

  @Override
  public String toSQL() {
    String colString = columns.isEmpty() ? "*" : String.join(", ", columns);
    String joinClause = joins.stream().map(JoinClause::toSQL).collect(Collectors.joining(" "));
    String query = "SELECT " + colString + " FROM " + table + " " + joinClause;
    if (!filters.isEmpty()) {
      query += " WHERE " + filters.stream().map(Filter::toSQL).collect(Collectors.joining(" AND "));
    }
    return query;
  }

  @Override
  public List<Object> getQueryParams() {
    return filters.stream().map(Filter::getValue).toList();
  }
}

