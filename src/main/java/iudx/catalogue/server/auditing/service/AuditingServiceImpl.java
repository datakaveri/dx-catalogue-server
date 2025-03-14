package iudx.catalogue.server.auditing.service;

import static iudx.catalogue.server.auditing.util.Constants.DATABASE_TABLE_NAME;
import static iudx.catalogue.server.auditing.util.Constants.EXCHANGE_NAME;
import static iudx.catalogue.server.auditing.util.Constants.ROUTING_KEY;
import static iudx.catalogue.server.util.Constants.RMQ_SERVICE_ADDRESS;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.auditing.util.QueryBuilder;
import iudx.catalogue.server.databroker.model.QueryObject;
import iudx.catalogue.server.databroker.service.RabbitMQService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class AuditingServiceImpl implements AuditingService {

  private static final Logger LOGGER = LogManager.getLogger(AuditingServiceImpl.class);
  public static RabbitMQService rmqService;
  private String databaseTableName;

  /**
   * Constructs an instance of the AuditingServiceImpl class with the given property
   * object and Vert.x instance.
   * Initializes the class members with values from the property object.
   *
   * @param databaseTableName The String containing the database table name.
   * @param vertxInstance     The Vert.x instance to use for database connections.
   */
  public AuditingServiceImpl(String databaseTableName, Vertx vertxInstance) {
    this.databaseTableName = databaseTableName;
    rmqService = RabbitMQService.createProxy(vertxInstance, RMQ_SERVICE_ADDRESS);

  }

  @Override
  public Future<Void> insertAuditingValuesInRmq(
      JsonObject request) {
    request.put(DATABASE_TABLE_NAME, databaseTableName);
    QueryObject rmqMessage;

    rmqMessage = QueryBuilder.buildMessageForRmq(request);

    Promise<Void> promise = Promise.promise();
    LOGGER.debug("audit rmq Message body: " + rmqMessage.toJson());
    rmqService.publishMessage(
            rmqMessage,
            EXCHANGE_NAME,
            ROUTING_KEY)
        .onSuccess(result -> {
          LOGGER.info("inserted into rmq");
          promise.complete();
        })
        .onFailure(err -> {
          LOGGER.debug("failed to insert into rmq");
          LOGGER.error(err);
          promise.fail(err);
        });
    return promise.future();
  }
}
