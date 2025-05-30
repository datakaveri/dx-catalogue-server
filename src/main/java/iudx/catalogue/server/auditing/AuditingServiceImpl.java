package iudx.catalogue.server.auditing;

import static iudx.catalogue.server.auditing.util.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import iudx.catalogue.server.auditing.util.QueryBuilder;
import iudx.catalogue.server.databroker.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class AuditingServiceImpl implements AuditingService {

  private static final Logger LOGGER = LogManager.getLogger(AuditingServiceImpl.class);
  PgConnectOptions connectOptions;
  PoolOptions poolOptions;
  PgPool pool;
  private final QueryBuilder queryBuilder = new QueryBuilder();

  private String databaseIp;
  private int databasePort;
  private String databaseName;
  private String databaseUserName;
  private String databasePassword;
  private int databasePoolSize;
  private String databaseTableName;
  public static DataBrokerService rmqService;

  /**
   * Constructs an instance of the AuditingServiceImpl class with the given property
   * object and Vert.x instance.
   * Initializes the class members with values from the property object.
   * @param propObj The property object containing the database configuration parameters.
   * @param vertxInstance The Vert.x instance to use for database connections.
   */
  public AuditingServiceImpl(JsonObject propObj, Vertx vertxInstance) {
    if (propObj != null && !propObj.isEmpty()) {
      databaseIp = propObj.getString("auditingDatabaseIP");
      databasePort = propObj.getInteger("auditingDatabasePort");
      databaseName = propObj.getString("auditingDatabaseName");
      databaseUserName = propObj.getString("auditingDatabaseUserName");
      databasePassword = propObj.getString("auditingDatabasePassword");
      databaseTableName = propObj.getString("auditingDatabaseTableName");
      databasePoolSize = propObj.getInteger("auditingPoolSize");
    }

    this.connectOptions =
        new PgConnectOptions()
            .setPort(databasePort)
            .setHost(databaseIp)
            .setDatabase(databaseName)
            .setUser(databaseUserName)
            .setPassword(databasePassword)
            .setReconnectAttempts(2)
            .setReconnectInterval(1000);

    this.poolOptions = new PoolOptions().setMaxSize(databasePoolSize);
    this.pool = PgPool.pool(vertxInstance, connectOptions, poolOptions);
    this.rmqService = DataBrokerService.createProxy(vertxInstance, BROKER_SERVICE_ADDRESS);

  }

  @Override
  public AuditingService insertAuditngValuesInRmq(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    JsonObject rmqMessage = new JsonObject();

    rmqMessage = queryBuilder.buildMessageForRmq(request);

    LOGGER.debug("audit rmq Message body: " + rmqMessage);
    rmqService.publishMessage(
        rmqMessage,
        EXCHANGE_NAME,
        ROUTING_KEY,
        rmqHandler -> {
          if (rmqHandler.succeeded()) {
            handler.handle(Future.succeededFuture());
            LOGGER.info("inserted into rmq");
          } else {
            LOGGER.debug("failed to insert into rmq");
            LOGGER.error(rmqHandler.cause());
            handler.handle(Future.failedFuture(rmqHandler.cause().getMessage()));
          }
        });
    return this;
  }
}
