package iudx.catalogue.server.rating;

import static iudx.catalogue.server.util.Constants.ELASTIC_SERVICE_ADDRESS;
import static iudx.catalogue.server.util.Constants.PG_SERVICE_ADDRESS;
import static iudx.catalogue.server.util.Constants.RATING_SERVICE_ADDRESS;
import static iudx.catalogue.server.util.Constants.RMQ_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
import iudx.catalogue.server.database.postgres.service.PostgresService;
import iudx.catalogue.server.databroker.service.RabbitMQService;
import iudx.catalogue.server.rating.service.RatingService;
import iudx.catalogue.server.rating.service.RatingServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <h1>Rating Verticle</h1>
 *
 * <p>The Rating Verticle implementation in the IUDX Catalogue Server exposes the {@link
 * RatingService} over the Vert.x Event Bus
 *
 * @version 1.0
 * @since 2022-05-30
 */
public class RatingVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(RatingVerticle.class);

  ElasticsearchService elasticsearchService;
  RabbitMQService rmqService;
  PostgresService postgresService;
  private String ratingExchangeName;
  private String rsauditingtable;
  private int minReadNumber;
  private String ratingIndex;
  private String docIndex;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;
  private RatingService rating;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event bus against an address, publishes the service with the service discovery
   * interface.
   *
   * @throws Exception which is a start up exception.
   */
  @Override
  public void start() throws Exception {

    ratingExchangeName = config().getString("ratingExchangeName");
    rsauditingtable = config().getString("rsAuditingTableName");
    minReadNumber = config().getInteger("minReadNumber");
    ratingIndex = config().getString("ratingIndex");
    docIndex = config().getString("docIndex");

    elasticsearchService = ElasticsearchService.createProxy(vertx, ELASTIC_SERVICE_ADDRESS);
    rmqService = RabbitMQService.createProxy(vertx, RMQ_SERVICE_ADDRESS);
    postgresService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);

    binder = new ServiceBinder(vertx);
    rating = new RatingServiceImpl(ratingExchangeName,
        rsauditingtable, minReadNumber, ratingIndex, elasticsearchService, docIndex,
        rmqService, postgresService);
    consumer = binder.setAddress(RATING_SERVICE_ADDRESS).register(RatingService.class, rating);
    LOGGER.info("Rating Service Started");
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
