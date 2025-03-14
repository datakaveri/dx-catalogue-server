package iudx.catalogue.server.database.cache.models;

public class JoinClause {
  private final String joinType;
  private final String table;
  private final String onCondition;

  public JoinClause(String joinType, String table, String onCondition) {
    this.joinType = joinType;
    this.table = table;
    this.onCondition = onCondition;
  }

  public String toSQL() {
    return joinType + " JOIN " + table + " ON " + onCondition;
  }
}
