package iudx.catalogue.server.apiserver;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import iudx.catalogue.server.rating.RatingService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import iudx.catalogue.server.apiserver.util.ExceptionHandler;
import iudx.catalogue.server.apiserver.util.RespBuilder;
import iudx.catalogue.server.authenticator.AuthenticationService;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.validator.ValidatorService;
import iudx.catalogue.server.geocoding.GeocodingService;
import iudx.catalogue.server.nlpsearch.NLPSearchService;
import iudx.catalogue.server.auditing.AuditingService;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

/**
 * The Catalogue Server API Verticle.
 *
 * <h1>Catalogue Server API Verticle</h1>
 *
 * <p>
 * The API Server verticle implements the IUDX Catalogue Server APIs. It handles the API requests
 * from the clients and interacts with the associated Service to respond.
 *
 * @see io.vertx.core.Vertx
 * @see io.vertx.core.AbstractVerticle
 * @see io.vertx.core.http.HttpServer
 * @see io.vertx.ext.web.Router
 * @see io.vertx.servicediscovery.ServiceDiscovery
 * @see io.vertx.servicediscovery.types.EventBusService
 * @see io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
 * @version 1.0
 * @since 2020-05-31
 */
public class ApiServerVerticle extends AbstractVerticle {

  private HttpServer server;
  private CrudApis crudApis;
  private SearchApis searchApis;
  private String keystore;
  private String keystorePassword;
  private ListApis listApis;
  private RelationshipApis relApis;
  private GeocodingApis geoApis;
  private RatingApis ratingApis;

  @SuppressWarnings("unused")
  private Router router;

  private String catAdmin;
  private boolean isSSL;
  private int port;
  public static String basePath;

  private static final Logger LOGGER = LogManager.getLogger(ApiServerVerticle.class);

  /**
   * This method is used to start the Verticle and joing a cluster
   *
   * @throws Exception which is a startup exception
   */
  @Override
  public void start() throws Exception {

    router = Router.router(vertx);

    /* Configure */
    catAdmin = config().getString(CAT_ADMIN);
    isSSL = config().getBoolean(IS_SSL);

    basePath = config().getString("basePath");

    HttpServerOptions serverOptions = new HttpServerOptions();


    if (isSSL) {
      LOGGER.debug("Info: Starting HTTPs server");

      /* Read the configuration and set the HTTPs server properties. */

      keystore = config().getString("keystore");
      keystorePassword = config().getString("keystorePassword");

      /*
       * Default port when ssl is enabled is 8443. If set through config, then that value is taken
       */
      port = config().getInteger(PORT) == null ? 8443
          : config().getInteger(PORT);

      /* Setup the HTTPs server properties, APIs and port. */

      serverOptions.setSsl(true)
          .setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword(keystorePassword));

    } else {
      LOGGER.debug("Info: Starting HTTP server");

      /* Setup the HTTP server properties, APIs and port. */

      serverOptions.setSsl(false);
      /*
       * Default port when ssl is disabled is 8080. If set through config, then that value is taken
       */
      port = config().getInteger(PORT) == null ? 8080
          : config().getInteger(PORT);
    }
    serverOptions.setCompressionSupported(true).setCompressionLevel(5);
    /** Instantiate this server */
    server = vertx.createHttpServer(serverOptions);


    /** API Callback managers */
    crudApis = new CrudApis();
    searchApis = new SearchApis();
    listApis = new ListApis();
    relApis = new RelationshipApis();
    geoApis = new GeocodingApis();
    ratingApis = new RatingApis();
    /**
     *
     * Get proxies and handlers
     *
     */


    /** Todo
     *    - Set service proxies based on availability?
     **/
    DatabaseService dbService
      = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);

    RatingService ratingService
      = RatingService.createProxy(vertx, RATING_SERVICE_ADDRESS);

    crudApis.setDbService(dbService);
    listApis.setDbService(dbService);
    relApis.setDbService(dbService);
    // TODO : set db service for Rating APIs
    crudApis.setHost(config().getString(HOST));
    ratingApis.setRatingService(ratingService);
    ratingApis.setHost(config().getString(HOST));

    AuthenticationService authService =
        AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
    crudApis.setAuthService(authService);
    ratingApis.setAuthService(authService);

    ValidatorService validationService =
        ValidatorService.createProxy(vertx, VALIDATION_SERVICE_ADDRESS);
    crudApis.setValidatorService(validationService);
    ratingApis.setValidatorService(validationService);

    GeocodingService geoService
      = GeocodingService.createProxy(vertx, GEOCODING_SERVICE_ADDRESS);
    geoApis.setGeoService(geoService);

    NLPSearchService nlpsearchService
      = NLPSearchService.createProxy(vertx, NLP_SERVICE_ADDRESS);

    searchApis.setService(dbService, geoService, nlpsearchService);

    AuditingService auditingService
      = AuditingService.createProxy(vertx, AUDITING_SERVICE_ADDRESS);
    crudApis.setAuditingService(auditingService);
    ratingApis.setAuditingService(auditingService);

    ExceptionHandler exceptionhandler = new ExceptionHandler();

    /**
     *
     * API Routes and Callbacks
     *
     */

    /**
     * Routes - Defines the routes and callbacks
     */
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router.route().handler(
        CorsHandler.create("*")
                   .allowedHeaders(ALLOWED_HEADERS)
                   .allowedMethods(ALLOWED_METHODS));

    router.route().handler(routingContext -> {
      routingContext.response()
                    .putHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate")
                    .putHeader("Pragma", "no-cache")
                    .putHeader("Expires", "0")
                    .putHeader("X-Content-Type-Options", "nosniff");
      routingContext.next();
    });

    /**
     * Documentation routes
     */
    /* Static Resource Handler */
    /* Get openapiv3 spec */
    router.get(ROUTE_STATIC_SPEC)
      .produces(MIME_APPLICATION_JSON)
      .handler( routingContext -> {
        HttpServerResponse response = routingContext.response();
        response.sendFile("docs/openapi.yaml");
      });
    /* Get redoc */
    router.get(ROUTE_DOC)
      .produces(MIME_TEXT_HTML)
      .handler( routingContext -> {
        HttpServerResponse response = routingContext.response();
        response.sendFile("docs/apidoc.html");
      });


    /**
     * UI routes
     */
    /* Static Resource Handler */
    router.route("/*").produces("text/html")
      .handler(StaticHandler.create("ui/dist/dk-customer-ui/"));

    router.route("/assets/*").produces("*/*")
      .handler(StaticHandler.create("ui/dist/dk-customer-ui/assets/"));

    router.route("/").produces("text/html")
      .handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response.sendFile("ui/dist/dk-customer-ui/index.html");
    });

    /**
     * Routes for item CRUD
     */
    /* Create Item - Body contains data */
    router.post(ROUTE_ITEMS)
      .consumes(MIME_APPLICATION_JSON)
      .produces(MIME_APPLICATION_JSON)
      .failureHandler(exceptionhandler)
      .handler( routingContext -> {
        /* checking auhthentication info in requests */
        if (routingContext.request().headers().contains(HEADER_TOKEN)) {
          crudApis.createItemHandler(routingContext);
        } else {
          LOGGER.warn("Fail: Unathorized CRUD operation");
          routingContext.response().setStatusCode(401).end();
        }
      });

    /* Get Item */
    router.get(ROUTE_ITEMS)
      .produces(MIME_APPLICATION_JSON)
      .handler( routingContext -> {
        crudApis.getItemHandler(routingContext);
      });

    /* Update Item - Body contains data */
    router.put(ROUTE_UPDATE_ITEMS)
      .consumes(MIME_APPLICATION_JSON)
      .produces(MIME_APPLICATION_JSON)
      .handler(routingContext -> {
        /* checking auhthentication info in requests */
        if (routingContext.request().headers().contains(HEADER_TOKEN)) {
          /** Update params checked in createItemHandler */
          crudApis.createItemHandler(routingContext);
        } else {
          LOGGER.warn("Unathorized CRUD operation");
          routingContext.response().setStatusCode(401).end();
        }
      });

    /* Delete Item - Query param contains id */
    router.delete(ROUTE_DELETE_ITEMS)
      .produces(MIME_APPLICATION_JSON)
      .handler(routingContext -> {
        /* checking auhthentication info in requests */
        if (routingContext.request().headers().contains(HEADER_TOKEN) &&
            routingContext.queryParams().contains(ID)) {
          /** Update params checked in createItemHandler */
          crudApis.deleteItemHandler(routingContext);
        } else {
          LOGGER.warn("Unathorized CRUD operation");
          routingContext.response().setStatusCode(401).end();
        }
      });

    /* Create instance - Instance name in query param */
    router.post(ROUTE_INSTANCE)
      .produces(MIME_APPLICATION_JSON)
      .handler(routingContext -> {
        /* checking auhthentication info in requests */
        if (routingContext.request().headers().contains(HEADER_TOKEN)) {
          crudApis.createInstanceHandler(routingContext, catAdmin);
        } else {
          LOGGER.warn("Fail: Unathorized CRUD operation");
          routingContext.response().setStatusCode(401).end();
        }
      });

    /* Delete instance - Instance name in query param */
    router.delete(ROUTE_INSTANCE)
      .produces(MIME_APPLICATION_JSON)
      .handler(routingContext -> {
        /* checking auhthentication info in requests */
        LOGGER.debug("Info: HIT instance");
        if (routingContext.request().headers().contains(HEADER_TOKEN)) {
          crudApis.deleteInstanceHandler(routingContext, catAdmin);
        } else {
          LOGGER.warn("Fail: Unathorized CRUD operation");
          routingContext.response().setStatusCode(401).end();
        }
      });

    /**
     * Routes for search and count
     */
    /* Search for an item */
    router.get(ROUTE_SEARCH)
      .produces(MIME_APPLICATION_JSON)
      .failureHandler(exceptionhandler)
      .handler( routingContext -> {
        searchApis.searchHandler(routingContext);
      });

    /* NLP Search */
    router.get(ROUTE_NLP_SEARCH)
      .produces(MIME_APPLICATION_JSON)
      .handler(routingContext -> {
        searchApis.nlpSearchHandler(routingContext);
      });

    /* Count the Cataloque server items */
    router.get(ROUTE_COUNT)
      .produces(MIME_APPLICATION_JSON)
      .handler( routingContext -> {
        searchApis.searchHandler(routingContext);
      });


    /**
     * Routes for list
     */
    /* list the item from database using itemId */
    router.get(ROUTE_LIST_ITEMS)
      .produces(MIME_APPLICATION_JSON)
      .handler(routingContext -> {
        listApis.listItemsHandler(routingContext);
      });

    /**
     * Routes for relationships
     */
    /* Relationship related search */
    router.get(ROUTE_REL_SEARCH)
      .handler( routingContext -> {
        relApis.relSearchHandler(routingContext);
      });

    /* Get all resources belonging to a resource group */
    router.get(ROUTE_RELATIONSHIP).handler(routingContext -> {
      relApis.listRelationshipHandler(routingContext);
    });

    /**
     * Routes for Geocoding
     */
    router.get(ROUTE_GEO_COORDINATES)
      .handler(routingContext -> {
        geoApis.getCoordinates(routingContext);
      });

    router.get(ROUTE_GEO_REVERSE)
      .handler(routingContext -> {
        geoApis.getLocation(routingContext);
      });

    /**
     * Routes for Rating APIs
     */
    /* Create Rating */
    router.post(ROUTE_RATING)
        .consumes(MIME_APPLICATION_JSON)
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(routingContext -> {
          if (routingContext.request().headers().contains(HEADER_TOKEN)) {
            ratingApis.createRatingHandler(routingContext);
          } else {
            LOGGER.error("Unauthorized Operation");
            routingContext.response().setStatusCode(401).end();
          }
        });

    /* Get Ratings */
    router
        .get(ROUTE_RATING)
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(
            routingContext -> {
              if (routingContext.request().params().contains("type")) {
                ratingApis.getRatingHandler(routingContext);
              } else {
                if (routingContext.request().headers().contains(HEADER_TOKEN)) {
                  ratingApis.getRatingHandler(routingContext);
                } else {
                  LOGGER.error("Unauthorized Operation");
                  routingContext.response().setStatusCode(401).end();
                }
              }
            });

    /* Update Rating */
    router.put(ROUTE_RATING)
        .consumes(MIME_APPLICATION_JSON)
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(routingContext -> {
          if (routingContext.request().headers().contains(HEADER_TOKEN)) {
            ratingApis.updateRatingHandler(routingContext);
          } else {
            LOGGER.error("Unauthorized Operation");
            routingContext.response().setStatusCode(401).end();
          }
        });

    /* Delete Rating */
    router.delete(ROUTE_RATING)
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(routingContext -> {
          if (routingContext.request().headers().contains(HEADER_TOKEN)) {
            ratingApis.deleteRatingHandler(routingContext);
          } else {
            LOGGER.error("Unauthorized Operation");
            routingContext.response().setStatusCode(401).end();
          }
        });

    /**
     * Start server
     */
    server.requestHandler(router).listen(port);

  }

  @Override
  public void stop() {
    LOGGER.info("Stopping the API server");
  }
}


