package iudx.catalogue.server.database.postgres.refactor;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import iudx.catalogue.server.database.postgres.models.*;

@VertxGen
@ProxyGen
public interface PGService {

  Future<QueryResult> insert(InsertQuery query);
  Future<QueryResult> update(UpdateQuery query);
  Future<QueryResult> search(SelectQuery query);
  Future<QueryResult> delete(DeleteQuery query);
  Future<Integer> count(SelectQuery query);

  @GenIgnore
  static PGService createProxy(Vertx vertx, String address) {
    return new PGServiceVertxEBProxy(vertx, address);
  }
}
