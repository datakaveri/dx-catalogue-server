package iudx.catalogue.server.database.cache.models;

import io.vertx.core.json.JsonObject;
import java.util.List;

public class QueryResult {
  private final List<JsonObject> rows;
  private final String error;
  private final int totalCount;
  private final boolean hasMore;

  public QueryResult(List<JsonObject> rows, int totalCount, boolean hasMore) {
    this.rows = rows;
    this.error = null;
    this.totalCount = totalCount;
    this.hasMore = hasMore;
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
}
