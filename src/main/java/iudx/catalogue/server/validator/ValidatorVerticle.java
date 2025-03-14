package iudx.catalogue.server.validator;

import static iudx.catalogue.server.apiserver.util.Constants.UAC_DEPLOYMENT;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.validator.util.Constants.CONTEXT;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
import iudx.catalogue.server.validator.service.ValidatorService;
import iudx.catalogue.server.validator.service.ValidatorServiceImpl;


/**
 * The Validator Verticle.
 *
 * <h1>Validator Verticle</h1>
 *
 * <p>The Validator Verticle implementation in the the IUDX Catalogue Server exposes the {@link
 * ValidatorService} over the Vert.x Event Bus.
 *
 * @version 1.0
 * @since 2020-05-31
 */
public class ValidatorVerticle extends AbstractVerticle {

  private ValidatorService validator;
  private String docIndex;
  private ElasticsearchService elasticsearchService;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;
  private boolean isUacInstance;
  private String vocContext;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event bus against an address, publishes the service with the service discovery
   * interface.
   */
  @Override
  public void start() throws Exception {
    binder = new ServiceBinder(vertx);
    docIndex = config().getString(DOC_INDEX);
    isUacInstance = config().getBoolean(UAC_DEPLOYMENT);
    vocContext = config().getString(CONTEXT);
    /* Create a reference to HazelcastClusterManager. */

    elasticsearchService = ElasticsearchService.createProxy(vertx, ELASTIC_SERVICE_ADDRESS);

    /* Create or Join a Vert.x Cluster. */

    /* Publish the Validator service with the Event Bus against an address. */

    validator = new ValidatorServiceImpl(elasticsearchService, docIndex, isUacInstance, vocContext);
    consumer =
        binder.setAddress(VALIDATION_SERVICE_ADDRESS)
            .register(ValidatorService.class, validator);
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
