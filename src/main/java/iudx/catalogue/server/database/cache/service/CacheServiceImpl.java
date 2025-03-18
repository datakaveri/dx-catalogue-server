package iudx.catalogue.server.database.cache.service;

import static iudx.catalogue.server.util.Constants.SUCCESS;
import static iudx.catalogue.server.util.Constants.TYPE_SUCCESS;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.common.RespBuilder;
import iudx.catalogue.server.database.postgres.refactor.PGService;
import iudx.catalogue.server.database.postgres.models.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CacheServiceImpl implements CacheService {
  private static final Logger LOGGER = LogManager.getLogger(CacheServiceImpl.class);
  private final PGService pgService;

  public CacheServiceImpl(PGService pgService) {
    this.pgService = pgService;
  }

  @Override
  public Future<JsonObject> getPopularDatasetCounts(String tableName) {
    Promise<JsonObject> promise = Promise.promise();

    // Build SQL query using query model
    List<String> columns = List.of("resource_group", "COUNT(id) AS totalhits");
    List<Filter> filters = List.of(new Filter("resource_group", "IS NOT", new JsonArray("NULL")));
    GroupBy groupBy = new GroupBy(List.of("resource_group"));
    OrderBy orderBy = new OrderBy("totalhits", "DESC");
    Limit limit = new Limit(6);

    SelectQuery selectQuery = new SelectQuery(tableName, columns, filters, List.of(), groupBy,
        orderBy, limit);

    // Execute query using PGService
    LOGGER.debug("Sending SelectQuery to PGService: {}", selectQuery.toJson());
    pgService.search(selectQuery).onComplete(dbHandler -> {
      if (dbHandler.succeeded()) {
        List<JsonObject> result = dbHandler.result().getRows();
        RespBuilder respBuilder =
            new RespBuilder().withType(TYPE_SUCCESS).withTitle(SUCCESS).withResult(result);
        promise.complete(respBuilder.getJsonResponse());
      } else {
        LOGGER.error("postgres query failed");
        promise.fail(dbHandler.cause());
      }
    });

    return promise.future();
  }

  @Override
  public Future<JsonObject> getSummaryCountSizeApi(String table) {
    Promise<JsonObject> promise = Promise.promise();
    LOGGER.info("Into get summary count size API");

    List<String> columns = List.of("*");
    SelectQuery selectQuery = new SelectQuery(table, columns, List.of(), List.of(),
        null, null, null);

    LOGGER.debug("Executing SelectQuery: {}", selectQuery.toJson());
    LOGGER.debug("Executing Query: {}", selectQuery.toSQL());
    pgService.search(selectQuery).onComplete(dbHandler -> {
      if (dbHandler.succeeded()) {
        List<JsonObject> result = dbHandler.result().getRows();
        RespBuilder respBuilder =
            new RespBuilder().withType(TYPE_SUCCESS).withTitle(SUCCESS).withResult(result);
        promise.complete(respBuilder.getJsonResponse());
      } else {
        LOGGER.error("Postgres query failed", dbHandler.cause());
        promise.fail(dbHandler.cause());
      }
    });
    return promise.future();
  }

  @Override
  public Future<JsonObject> getRealTimeDataSetApi(String databaseTable, JsonArray excludedIdsJson) {
    Promise<JsonObject> promise = Promise.promise();
    LOGGER.info("Into get real time dataset API");

    ZonedDateTime zonedDateTimeUtc = ZonedDateTime.now(ZoneId.of("UTC"));
    LocalDateTime utcTime = zonedDateTimeUtc.toLocalDateTime();
    String timeFrom12Am = utcTime.withHour(0).withMinute(0).withSecond(0).withNano(0).toString();
    LOGGER.debug("Time from 12 AM UTC: {}", timeFrom12Am);
    JsonArray filterValues = new JsonArray();
    filterValues.add(timeFrom12Am);
    filterValues.add(utcTime.toString());

    List<Filter> filters = new java.util.ArrayList<>(List.of(
        new Filter("time", "BETWEEN", filterValues)
    ));

    if (!excludedIdsJson.isEmpty()) {
      filters.add(new Filter("userid", "NOT IN", excludedIdsJson));
    }

    List<String> columns = List.of("COUNT(api) AS counts", "COALESCE(SUM(size), 0) AS size");
    SelectQuery selectQuery = new SelectQuery(databaseTable, columns, filters, List.of(), null,
        null, null);

    LOGGER.debug("Sending SelectQuery to PGService: {}", selectQuery.toJson());
    LOGGER.debug("Sending SelectQuery: {}", selectQuery.toSQL());
    pgService.search(selectQuery).onComplete(dbHandler -> {
      if (dbHandler.succeeded()) {
        List<JsonObject> result = dbHandler.result().getRows();
        RespBuilder respBuilder =
            new RespBuilder().withType(TYPE_SUCCESS).withTitle(SUCCESS).withResult(result);
        promise.complete(respBuilder.getJsonResponse());
      } else {
        LOGGER.error("Postgres query failed", dbHandler.cause());
        promise.fail(dbHandler.cause());
      }
    });

    return promise.future();
  }

  public Future<Integer> getAuditingInfo(String tableName, String userId, String resourceId) {
    Promise<Integer> promise = Promise.promise();

    List<Filter> filters = List.of(
        new Filter("userId", "=", new JsonArray().add(userId)),
        new Filter("resourceid", "=", new JsonArray().add(resourceId))
    );

    SelectQuery auditQuery = new SelectQuery(tableName, List.of("COUNT(*)"), filters,
        List.of(), null, null, null);

    pgService.count(auditQuery)
        .onSuccess(count -> {
          LOGGER.info("Audit count retrieved: {}", count);
          promise.complete(count);
        })
        .onFailure(error -> {
          LOGGER.error("Failed to fetch audit info: {}", error.getMessage());
          promise.fail(error);
        });

    return promise.future();
  }
}
