package iudx.catalogue.server.databroker.service;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import iudx.catalogue.server.databroker.model.QueryObject;

/**
 * The RabbitMQ Service.
 *
 * <h1>RabbitMQ Service</h1>
 *
 * <p>The RabbitMQ Service in the IUDX Catalogue Server defines the operations to be performed
 * with the RabbitMQ server.
 *
 * @version 1.0
 * @see io.vertx.codegen.annotations.ProxyGen
 * @see io.vertx.codegen.annotations.VertxGen
 * @since 2022-06-23
 */
@VertxGen
@ProxyGen
public interface RabbitMQService {

  static RabbitMQService createProxy(Vertx vertx, String address) {
    return new RabbitMQServiceVertxEBProxy(vertx, address);
  }

  Future<Void> publishMessage(QueryObject body, String toExchange, String routingKey);
}
