package org.cdpg.dx.common.database.postgres.models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen
public class JoinClause {
  private String joinType;
  private String table;
  private String onCondition;

  public JoinClause(JsonObject json) {
    JoinClauseConverter.fromJson(json, this);
  }
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    JoinClauseConverter.toJson(this, json);
    return json;
  }

  public JoinClause(String joinType, String table, String onCondition) {
    this.joinType = joinType;
    this.table = table;
    this.onCondition = onCondition;
  }

  public String toSQL() {
    return joinType + " JOIN " + table + " ON " + onCondition;
  }

  public String getJoinType() {
    return joinType;
  }

  public void setJoinType(String joinType) {
    this.joinType = joinType;
  }

  public String getTable() {
    return table;
  }

  public void setTable(String table) {
    this.table = table;
  }

  public String getOnCondition() {
    return onCondition;
  }

  public void setOnCondition(String onCondition) {
    this.onCondition = onCondition;
  }
}
