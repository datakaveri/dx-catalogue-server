package iudx.catalogue.server.databroker;

import static iudx.catalogue.server.util.Constants.RMQ_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.catalogue.server.databroker.service.RabbitMQService;
import iudx.catalogue.server.databroker.service.RabbitMQServiceImpl;

/**
 * The RabbitMQ Verticle.
 *
 * <h1>RabbitMQ Verticle</h1>
 *
 * <p>The RabbitMQ Verticle implementation in the IUDX Catalogue Server exposes the {@link
 * RabbitMQService} over the Vert.x Event Bus.
 *
 * @version 1.0
 * @since 2024-08-02
 */
public class RabbitMQVerticle extends AbstractVerticle {
  private RabbitMQService rabbitMQService;
  private RabbitMQOptions options;
  private RabbitMQClient client;
  private String rmqIP;
  private int rmqPort;
  private String rmqVhost;
  private String rmqUserName;
  private String rmqPassword;
  private int connectionTimeout;
  private int requestedHeartbeat;
  private int handshakeTimeout;
  private int requestedChannelMax;
  private int networkRecoveryInterval;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event bus against an address, publishes the service with the service discovery
   * interface.
   */
  @Override
  public void start() throws Exception {
    rmqIP = config().getString("dataBrokerIP");
    rmqPort = config().getInteger("dataBrokerPort");
    rmqVhost = config().getString("dataBrokerVhost");
    rmqUserName = config().getString("dataBrokerUserName");
    rmqPassword = config().getString("dataBrokerPassword");
    connectionTimeout = config().getInteger("connectionTimeout");
    requestedHeartbeat = config().getInteger("requestedHeartbeat");
    handshakeTimeout = config().getInteger("handshakeTimeout");
    requestedChannelMax = config().getInteger("requestedChannelMax");
    networkRecoveryInterval = config().getInteger("networkRecoveryInterval");

    options = new RabbitMQOptions();
    options.setUser(rmqUserName);
    options.setPassword(rmqPassword);
    options.setHost(rmqIP);
    options.setPort(rmqPort);
    options.setVirtualHost(rmqVhost);
    options.setConnectionTimeout(connectionTimeout);
    options.setRequestedHeartbeat(requestedHeartbeat);
    options.setHandshakeTimeout(handshakeTimeout);
    options.setRequestedChannelMax(requestedChannelMax);
    options.setNetworkRecoveryInterval(networkRecoveryInterval);
    options.setAutomaticRecoveryEnabled(true);

    client = RabbitMQClient.create(vertx, options);

    binder = new ServiceBinder(vertx);
    rabbitMQService = new RabbitMQServiceImpl(client);

    consumer =
        binder
            .setAddress(RMQ_SERVICE_ADDRESS)
            .register(RabbitMQService.class, rabbitMQService);
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
