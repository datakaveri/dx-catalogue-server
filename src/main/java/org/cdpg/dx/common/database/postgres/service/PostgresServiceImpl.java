package org.cdpg.dx.common.database.postgres.service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.common.database.postgres.models.DeleteQuery;
import org.cdpg.dx.common.database.postgres.models.InsertQuery;
import org.cdpg.dx.common.database.postgres.models.QueryResult;
import org.cdpg.dx.common.database.postgres.models.SelectQuery;
import org.cdpg.dx.common.database.postgres.models.UpdateQuery;

public class PostgresServiceImpl implements PostgresService {

  private static final Logger LOGGER = LogManager.getLogger(PostgresServiceImpl.class);
  private final PgPool client;

  public PostgresServiceImpl(PgPool client) {
    this.client = client;
  }

  private QueryResult convertToQueryResult(RowSet<Row> rowSet, int totalCount, boolean hasMore) {
    List<JsonObject> rows = new ArrayList<>();
    for (Row row : rowSet) {
      JsonObject json = new JsonObject();
      for (int i = 0; i < row.size(); i++) {
        json.put(row.getColumnName(i), row.getValue(i));
      }
      rows.add(json);
    }
    return new QueryResult(rows, totalCount, hasMore);
  }

  private Future<QueryResult> executeQuery(String sql, List<Object> params, boolean isCountQuery) {
    Promise<QueryResult> promise = Promise.promise();

    String preparedQuery = prepareParameterizedQuery(sql, params);
    LOGGER.debug("prepared query: " + preparedQuery);
    client
        .preparedQuery(preparedQuery)
        .execute()
        .onSuccess(
            result -> {
              LOGGER.debug("Success: " + result.rowCount() + " rows affected");
              if (isCountQuery) {
                int totalCount =
                    result.iterator().hasNext() ? result.iterator().next().getInteger(0) : 0;
                promise.complete(new QueryResult(null, totalCount, false));
              } else {
                promise.complete(convertToQueryResult(result, result.size(), false));
              }
            })
        .onFailure(
            error -> {
              LOGGER.error("Error: " + error.getMessage());
              promise.complete(new QueryResult(error.getMessage()));
            });
    return promise.future();
  }

  private String prepareParameterizedQuery(String sql, List<Object> params) {
    for (Object param : params) {
      String value;

      if (param == null || "NULL".equalsIgnoreCase(String.valueOf(param))) {
        value = "NULL";
      } else if (param instanceof JsonArray jsonArray) { // Handle JsonArray as list
        if (jsonArray.isEmpty()) {
          throw new IllegalArgumentException("IN/NOT IN clause requires at least one value.");
        }
        value = jsonArray.stream()
            .map(item -> item instanceof String ? "'" + item + "'" : item.toString())
            .collect(Collectors.joining(", ", "(", ")"));
      } else if (param instanceof String) {
        value = "'" + param + "'";
      } else {
        value = param.toString();
      }

      sql = sql.replaceFirst("\\?", value);
    }
    return sql;
  }



  @Override
  public Future<QueryResult> insert(InsertQuery query) {
    return executeQuery(query.toSQL(), query.getQueryParams(), false);
  }

  @Override
  public Future<QueryResult> update(UpdateQuery query) {
    return executeQuery(query.toSQL(), query.getQueryParams(), false);
  }

  @Override
  public Future<QueryResult> search(SelectQuery query) {
    LOGGER.debug("Received SelectQuery in PostgresServiceImpl: {}", query.toSQL());

    return executeQuery(query.toSQL(), query.getQueryParams(), false)
        .map(
            result -> {
              LOGGER.debug("Success: " + result.toJson());
              int totalCount = result.getRows().size();
              LOGGER.info(totalCount);
              List<JsonObject> rows = new ArrayList<>(result.getRows());
              return new QueryResult(rows, totalCount);
            });
  }

  @Override
  public Future<QueryResult> delete(DeleteQuery query) {
    return executeQuery(query.toSQL(), query.getQueryParams(), false);
  }

  @Override
  public Future<Integer> count(SelectQuery query) {
    LOGGER.debug("Received SelectQuery in PostgresServiceImpl: {}", query.toJson());

    return executeQuery(query.toSQL(), query.getQueryParams(), true)
        .map(
            result -> {
              LOGGER.debug("Success: " + result.toJson());
              int totalCount = result.getTotalCount();
              LOGGER.info(totalCount);

              return totalCount;
            });
  }
}
