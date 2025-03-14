package iudx.catalogue.server.databroker.service;

import static iudx.catalogue.server.util.Constants.TITLE_INTERNAL_SERVER_ERROR;
import static iudx.catalogue.server.util.Constants.TYPE_INTERNAL_SERVER_ERROR;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.RabbitMQClient;
import iudx.catalogue.server.apiserver.util.RespBuilder;
import iudx.catalogue.server.databroker.model.QueryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RabbitMQServiceImpl implements RabbitMQService {
  private static final Logger LOGGER = LogManager.getLogger(RabbitMQServiceImpl.class);
  private RabbitMQClient client;

  /**
   * Constructs a new instance of RabbitMQServiceImpl with the specified RabbitMQClient.
   *
   * @param client the RabbitMQClient to use for communication with the RabbitMQ server
   */
  public RabbitMQServiceImpl(RabbitMQClient client) {
    this.client = client;
    this.client.start(
        startHandler -> {
          if (startHandler.succeeded()) {
            LOGGER.info("RMQ started");
          } else {
            LOGGER.error("RMQ startup failed");
          }
        });
  }

  /**
   * This method will only publish messages to internal-communication exchanges.
   */
  @Override
  public Future<Void> publishMessage(QueryObject body, String toExchange, String routingKey) {
    Promise<Void> promise = Promise.promise();
    Buffer buffer = Buffer.buffer(body.toJson().toString());

    if (!client.isConnected()) {
      client.start();
    }

    client.basicPublish(toExchange, routingKey, buffer, publishHandler -> {
      if (publishHandler.succeeded()) {
        //JsonObject result = new JsonObject().put("type", "success");
        promise.complete();
      } else {
        RespBuilder respBuilder =
            new RespBuilder()
                .withType(TYPE_INTERNAL_SERVER_ERROR)
                .withTitle(TITLE_INTERNAL_SERVER_ERROR)
                .withDetail(publishHandler.cause().getLocalizedMessage());
        promise.fail(respBuilder.getResponse());
      }
    });
    return promise.future();
  }
}
