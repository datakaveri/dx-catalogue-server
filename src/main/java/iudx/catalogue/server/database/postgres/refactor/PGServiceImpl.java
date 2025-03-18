package iudx.catalogue.server.database.postgres.refactor;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import iudx.catalogue.server.database.postgres.models.*;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PGServiceImpl implements PGService {

  private static final Logger LOGGER = LogManager.getLogger(PGServiceImpl.class);
  private final PgPool client;

  public PGServiceImpl(PgPool client) {
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
    client.preparedQuery(preparedQuery).execute()
        .onSuccess(result -> {
          LOGGER.debug("Success: " + result.rowCount() + " rows affected");
          if (isCountQuery) {
            int totalCount = result.iterator().hasNext() ? result.iterator().next().getInteger(0) : 0;
            promise.complete(new QueryResult(null, totalCount, false));
          } else {
            promise.complete(convertToQueryResult(result, result.size(), false));
          }
        })
        .onFailure(error -> {
          LOGGER.error("Error: " + error.getMessage());
            promise.complete(new QueryResult(error.getMessage()));});
    return promise.future();
  }

  private String prepareParameterizedQuery(String sql, List<Object> params) {
    for (Object param : params) {
      String value = (param instanceof String) ? "'" + param + "'" : param.toString();
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
    LOGGER.debug("Received SelectQuery in PGServiceImpl: {}", query.toJson());

    return executeQuery(query.toSQL(), query.getQueryParams(), false).map(result -> {
      LOGGER.debug("Success: " + result.toJson());
      int totalCount =result.getRows().size();
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
    LOGGER.debug("Received SelectQuery in PGServiceImpl: {}", query.toJson());

    return executeQuery(query.toSQL(), query.getQueryParams(), true).map(result -> {
      LOGGER.debug("Success: " + result.toJson());
      int totalCount =result.getTotalCount();
      LOGGER.info(totalCount);

      return totalCount;
    });
  }
}
