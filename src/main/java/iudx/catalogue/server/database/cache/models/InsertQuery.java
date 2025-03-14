package iudx.catalogue.server.database.cache.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InsertQuery implements QueryModel {
  private final String table;
  private final Map<String, Object> values;

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
}
