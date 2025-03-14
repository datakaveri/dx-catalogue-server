package iudx.catalogue.server.database.cache.service;

import io.vertx.core.Future;
import iudx.catalogue.server.database.cache.models.DeleteQuery;
import iudx.catalogue.server.database.cache.models.InsertQuery;
import iudx.catalogue.server.database.cache.models.QueryResult;
import iudx.catalogue.server.database.cache.models.SelectQuery;
import iudx.catalogue.server.database.cache.models.UpdateQuery;

public interface CacheService {
  Future<QueryResult> insert(InsertQuery query);
  Future<QueryResult> update(UpdateQuery query);
  Future<QueryResult> search(SelectQuery query, int limit, int offset);
  Future<QueryResult> delete(DeleteQuery query);
  Future<Integer> count(SelectQuery query);
}

