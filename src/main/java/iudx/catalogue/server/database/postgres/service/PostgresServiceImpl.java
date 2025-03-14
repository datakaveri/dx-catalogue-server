package iudx.catalogue.server.database.postgres.service;


import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import iudx.catalogue.server.common.RespBuilder;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class PostgresServiceImpl implements PostgresService {
  private static final Logger LOGGER = LogManager.getLogger(PostgresServiceImpl.class);
  private final PgPool client;

  public PostgresServiceImpl(final PgPool pgclient) {
    this.client = pgclient;
  }

  @Override
  public Future<JsonObject> executeQuery(final String query) {
    Promise<JsonObject> promise = Promise.promise();

    Collector<Row, ?, List<JsonObject>> rowCollector =
        Collectors.mapping(row -> row.toJson(), Collectors.toList());

    client
        .withConnection(
            connection ->
                connection.query(query).collecting(rowCollector).execute().map(row -> row.value()))
        .onSuccess(
            successHandler -> {
              JsonArray result = new JsonArray(successHandler);
              RespBuilder respBuilder =
                  new RespBuilder().withType(TYPE_SUCCESS).withTitle(SUCCESS).withResult(result);

              promise.complete(respBuilder.getJsonResponse());
            })
        .onFailure(
            failureHandler -> {
              LOGGER.debug(failureHandler);
              RespBuilder respBuilder =
                  new RespBuilder()
                      .withType(TYPE_DB_ERROR)
                      .withTitle(TITLE_DB_ERROR)
                      .withDetail(failureHandler.getLocalizedMessage());

              promise.fail(respBuilder.getResponse());
            });
    return promise.future();
  }

  @Override
  public Future<JsonObject> executeCountQuery(final String query) {
    Promise<JsonObject> promise = Promise.promise();

    LOGGER.debug(query);
    client
        .withConnection(
            connection ->
                connection.query(query).execute().map(rows -> rows.iterator().next().getInteger(0)))
        .onSuccess(
            count -> {
              promise.complete(new JsonObject().put("totalHits", count));
            })
        .onFailure(
            failureHandler -> {
              LOGGER.debug(failureHandler);
              String response =
                  new RespBuilder()
                      .withType(TYPE_DB_ERROR)
                      .withTitle(TITLE_DB_ERROR)
                      .withDetail(failureHandler.getLocalizedMessage())
                      .getResponse();
              promise.fail(response);
            });
    return promise.future();
  }
}
