package org.cdpg.dx.common.database.postgres.models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.List;

@DataObject
@JsonGen()
public class Filter {
  private String field;
  private String operator;
  private JsonArray value;
  private static final List<String> ALLOWED_OPERATORS = List.of(
      "=", "!=", "<>", ">", "<", ">=", "<=", "LIKE", "ILIKE", "IN", "NOT IN",
      "IS", "IS NOT", "BETWEEN", "NOT BETWEEN", "EXISTS", "NOT EXISTS"
  );

  public Filter(JsonObject json) {
    FilterConverter.fromJson(json, this);
  }
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    FilterConverter.toJson(this, json);
    return json;
  }

  public Filter(String field, String operator, JsonArray value) {
    this.field = field;
    this.operator = validateOperator(operator);
    this.value = value;
  }

  private String validateOperator(String operator) {
    if (!ALLOWED_OPERATORS.contains(operator.toUpperCase())) {
      throw new IllegalArgumentException("Invalid SQL operator: " + operator);
    }
    return operator;
  }

  public String toSQL() {
    if ("NULL".equalsIgnoreCase(String.valueOf(value))) {
      if (operator.equalsIgnoreCase("IS") || operator.equalsIgnoreCase("IS NOT")) {
        return field + " " + operator + " NULL";
      }
      throw new IllegalArgumentException("NULL can only be used with IS or IS NOT");
    } else if (operator.equalsIgnoreCase("IN") || operator.equalsIgnoreCase("NOT IN")) {
      if (value == null || value.isEmpty()) {
        throw new IllegalArgumentException(operator + " clause requires at least one value.");
      }
      String placeholders = String.join(", ", Collections.nCopies(value.size(), "?"));
      return field + " " + operator + " (" + placeholders + ")";
    } else if (operator.equalsIgnoreCase("BETWEEN") || operator.equalsIgnoreCase("NOT BETWEEN")) {
      if (value == null || value.size() != 2) {
        throw new IllegalArgumentException(operator + " operator requires exactly two values.");
      }
      return field + " " + operator + " ? AND ?";
    }
    return field + " " + operator + " ?";
  }



  public void setValue(JsonArray value) {
    this.value = value;
  }

  public String getField() {
    return field;
  }

  public JsonArray getValue() {
    if ("NULL".equalsIgnoreCase(String.valueOf(value))) {
      return null; // ✅ No parameter for IS NULL
    }
    return value;
  }


  public void setField(String field) {
    this.field = field;
  }

  public String getOperator() {
    return operator;
  }

  public void setOperator(String operator) {
    this.operator = operator;
  }
}
