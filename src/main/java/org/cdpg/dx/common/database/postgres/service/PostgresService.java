package org.cdpg.dx.common.database.postgres.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.cdpg.dx.common.database.postgres.models.DeleteQuery;
import org.cdpg.dx.common.database.postgres.models.InsertQuery;
import org.cdpg.dx.common.database.postgres.models.QueryResult;
import org.cdpg.dx.common.database.postgres.models.SelectQuery;
import org.cdpg.dx.common.database.postgres.models.UpdateQuery;

@VertxGen
@ProxyGen
public interface PostgresService {

  Future<QueryResult> insert(InsertQuery query);
  Future<QueryResult> update(UpdateQuery query);
  Future<QueryResult> search(SelectQuery query);
  Future<QueryResult> delete(DeleteQuery query);
  Future<Integer> count(SelectQuery query);

  @GenIgnore
  static PostgresService createProxy(Vertx vertx, String address) {
    return new PostgresServiceVertxEBProxy(vertx, address);
  }
}
