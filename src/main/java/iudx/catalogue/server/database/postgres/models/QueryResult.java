package iudx.catalogue.server.database.postgres.models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.List;

@DataObject(generateConverter = true, publicConverter = false)
public class QueryResult {
  private List<JsonObject> rows;
  private String error;
  private int totalCount;
  private boolean hasMore;

  public QueryResult(JsonObject json) {
    QueryResultConverter.fromJson(json, this);
  }
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    QueryResultConverter.toJson(this, json);
    return json;
  }
  public QueryResult(List<JsonObject> rows, int totalCount, boolean hasMore) {
    this.rows = rows;
    this.error = null;
    this.totalCount = totalCount;
    this.hasMore = hasMore;
  }

  public QueryResult(List<JsonObject> rows, int totalCount) {
    this.rows = rows;
    this.error = null;
    this.totalCount = totalCount;
  }
  public QueryResult(String error) {
    this.rows = null;
    this.error = error;
    this.totalCount = 0;
    this.hasMore = false;
  }

  public List<JsonObject> getRows() {
    return rows;
  }

  public String getError() {
    return error;
  }

  public int getTotalCount() {
    return totalCount;
  }

  public boolean hasMore() {
    return hasMore;
  }

  public void setRows(List<JsonObject> rows) {
    this.rows = rows;
  }

  public void setError(String error) {
    this.error = error;
  }

  public void setTotalCount(int totalCount) {
    this.totalCount = totalCount;
  }

  public boolean isHasMore() {
    return hasMore;
  }

  public void setHasMore(boolean hasMore) {
    this.hasMore = hasMore;
  }
}
