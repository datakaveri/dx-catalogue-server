package iudx.catalogue.server.database.cache.models;

public class Filter {
  private final String field;
  private final String operator;
  private final Object value;

  public Filter(String field, String operator, Object value) {
    this.field = field;
    this.operator = operator;
    this.value = value;
  }

  public String toSQL() {
    return field + " " + operator + " ?";
  }

  public Object getValue() {
    return value;
  }
}
