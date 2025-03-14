package iudx.catalogue.server.database.cache.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DeleteQuery implements QueryModel {
  private final String table;
  private final List<Filter> filters;

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
    return filters.stream().map(Filter::getValue).toList();
  }
}
