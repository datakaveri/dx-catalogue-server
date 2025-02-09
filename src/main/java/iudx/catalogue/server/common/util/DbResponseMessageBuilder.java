package iudx.catalogue.server.common.util;

import static iudx.catalogue.server.geocoding.util.Constants.RESULTS;
import static iudx.catalogue.server.geocoding.util.Constants.TYPE;
import static iudx.catalogue.server.util.Constants.DETAIL;
import static iudx.catalogue.server.util.Constants.TITLE;
import static iudx.catalogue.server.util.Constants.TITLE_SUCCESS;
import static iudx.catalogue.server.util.Constants.TOTAL_HITS;
import static iudx.catalogue.server.util.Constants.TYPE_SUCCESS;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * DbResponseMessageBuilder Message builder for search APIs.
 */
public class DbResponseMessageBuilder {
  private final JsonObject response = new JsonObject();
  private final JsonArray results = new JsonArray();

  public DbResponseMessageBuilder() {
  }

  public DbResponseMessageBuilder statusSuccess() {
    response.put(TYPE, TYPE_SUCCESS);
    response.put(TITLE, TITLE_SUCCESS);
    return this;
  }

  public DbResponseMessageBuilder setTotalHits(int hits) {
    response.put(TOTAL_HITS, hits);
    return this;
  }

  public DbResponseMessageBuilder setDetail(String detail) {
    response.put(DETAIL, detail);
    return this;
  }

  /**
   * Overloaded for source only request.
   */
  public DbResponseMessageBuilder addResult(JsonObject obj) {
    response.put(RESULTS, results.add(obj));
    return this;
  }

  /**
   * Overloaded for doc-ids request.
   */
  public DbResponseMessageBuilder addResult(String value) {
    response.put(RESULTS, results.add(value));
    return this;
  }

  public DbResponseMessageBuilder addResult() {
    response.put(RESULTS, results);
    return this;
  }

  public JsonObject getResponse() {
    return response;
  }
}

