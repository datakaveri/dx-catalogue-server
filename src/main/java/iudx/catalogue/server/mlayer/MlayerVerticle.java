package iudx.catalogue.server.mlayer;

import static iudx.catalogue.server.util.Constants.ELASTIC_SERVICE_ADDRESS;
import static iudx.catalogue.server.util.Constants.MLAYER_SERVICE_ADDRESS;
import static iudx.catalogue.server.util.Constants.PG_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
import iudx.catalogue.server.database.postgres.service.PostgresService;
import iudx.catalogue.server.mlayer.service.MlayerService;
import iudx.catalogue.server.mlayer.service.MlayerServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MlayerVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(MlayerVerticle.class);
  ElasticsearchService elasticsearchService;
  PostgresService postgresService;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;
  private MlayerService mlayer;

  /**
   * Helper function to create a WebClient to talk to the vocabulary server.
   *
   * @param vertx the vertx instance
   * @return a web client initialized with the relevant client certificate
   */
  static WebClient createWebClient(Vertx vertx) {
    WebClientOptions webClientOptions = new WebClientOptions();
    return WebClient.create(vertx, webClientOptions);
  }

  @Override
  public void start() throws Exception {
    elasticsearchService = ElasticsearchService.createProxy(vertx, ELASTIC_SERVICE_ADDRESS);
    postgresService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);
    binder = new ServiceBinder(vertx);

    mlayer = new MlayerServiceImpl(createWebClient(vertx), elasticsearchService, postgresService,
        config());
    consumer = binder.setAddress(MLAYER_SERVICE_ADDRESS).register(MlayerService.class, mlayer);
    LOGGER.info("Mlayer Service Started");
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
