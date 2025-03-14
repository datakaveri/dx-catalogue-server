package iudx.catalogue.server.auditing.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@ProxyGen
@VertxGen
public interface AuditingService {

  @GenIgnore
  static AuditingService createProxy(Vertx vertx, String address) {
    return new AuditingServiceVertxEBProxy(vertx, address);
  }

  Future<Void> insertAuditingValuesInRmq(JsonObject request);

}
