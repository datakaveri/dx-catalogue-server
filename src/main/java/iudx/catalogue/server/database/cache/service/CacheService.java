package iudx.catalogue.server.database.cache.service;

import io.vertx.core.Future;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface CacheService {
  Future<JsonObject> getPopularDatasetCounts(String table);

  Future<JsonObject> getSummaryCountSizeApi(String table);

  Future<JsonObject> getRealTimeDataSetApi(String databaseTable, JsonArray excludedIdsJson);
  Future<Integer> getAuditingInfo(String tableName, String userId, String resourceId);
}

