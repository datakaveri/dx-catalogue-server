package iudx.catalogue.server.apiserver;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;
import iudx.catalogue.server.apiserver.crud.CrudController;
import iudx.catalogue.server.apiserver.crud.CrudService;
import iudx.catalogue.server.apiserver.item.service.ItemService;
import iudx.catalogue.server.apiserver.item.service.ItemServiceImpl;
import iudx.catalogue.server.apiserver.stack.controller.StacController;
import iudx.catalogue.server.auditing.handler.AuditHandler;
import iudx.catalogue.server.auditing.service.AuditingService;
import iudx.catalogue.server.authenticator.handler.authentication.AuthHandler;
import iudx.catalogue.server.authenticator.handler.authorization.AuthValidationHandler;
import iudx.catalogue.server.authenticator.handler.authorization.AuthorizationHandler;
import iudx.catalogue.server.authenticator.service.AuthenticationService;
import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
import iudx.catalogue.server.exceptions.FailureHandler;
import iudx.catalogue.server.geocoding.controller.GeocodingController;
import iudx.catalogue.server.geocoding.service.GeocodingService;
import iudx.catalogue.server.mlayer.controller.MlayerController;
import iudx.catalogue.server.mlayer.service.MlayerService;
import iudx.catalogue.server.nlpsearch.service.NLPSearchService;
import iudx.catalogue.server.rating.controller.RatingController;
import iudx.catalogue.server.rating.service.RatingService;
import iudx.catalogue.server.relationship.controller.RelationshipController;
import iudx.catalogue.server.relationship.service.RelationshipService;
import iudx.catalogue.server.relationship.service.RelationshipServiceImpl;
import iudx.catalogue.server.util.Api;
import iudx.catalogue.server.validator.service.ValidatorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Catalogue Server API Verticle.
 *
 * <h1>Catalogue Server API Verticle</h1>
 *
 * <p>The API Server verticle implements the IUDX Catalogue Server APIs. It handles the API requests
 * from the clients and interacts with the associated Service to respond.
 *
 * @version 1.0
 * @see io.vertx.core.Vertx
 * @see io.vertx.core.AbstractVerticle
 * @see io.vertx.core.http.HttpServer
 * @see io.vertx.ext.web.Router
 * @see io.vertx.servicediscovery.ServiceDiscovery
 * @see io.vertx.servicediscovery.types.EventBusService
 * @see io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
 * @since 2020-05-31
 */
public class ApiServerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(ApiServerVerticle.class);
  private AuthHandler authHandler;
  private AuthorizationHandler authorizationHandler;
  private CrudController crudController;
  private ListController listController;
  private RatingController ratingController;
  private GeocodingController geocodingController;
  private SearchController searchController;
  private RelationshipController relationshipController;
  private MlayerController mlayerController;
  private HttpServer server;
  private String keystore;
  private String keystorePassword;
  private String catAdmin;
  private boolean isSsL;
  private int port;
  private String dxApiBasePath;
  private String host;
  private String docIndex;
  private Api api;
  private JsonArray optionalModules;

  /**
   * This method is used to start the Verticle and joing a cluster.
   *
   * @throws Exception which is a startup exception
   */
  @Override
  public void start() throws Exception {
    Router router = Router.router(vertx);
    // API Routes and Callbacks
    // Routes - Defines the routes and callbacks
    router.route().handler(BodyHandler.create());
    router.route().handler(CorsHandler.create("*")
        .allowedHeaders(ALLOWED_HEADERS)
        .allowedMethods(ALLOWED_METHODS));

    router.route().handler(
        routingContext -> {
          routingContext
              .response()
              .putHeader("Cache-Control",
                  "no-cache, no-store, max-age=0, must-revalidate")
              .putHeader("Pragma", "no-cache")
              .putHeader("Expires", "0")
              .putHeader("X-Content-Type-Options", "nosniff");
          routingContext.next();
        });

    dxApiBasePath = config().getString("dxApiBasePath");
    host = config().getString(HOST);
    docIndex = config().getString("docIndex");
    api = Api.getInstance(dxApiBasePath);

    /* Configure */
    catAdmin = config().getString(CAT_ADMIN);
    isSsL = config().getBoolean(IS_SSL);

    HttpServerOptions serverOptions = new HttpServerOptions();

    if (isSsL) {
      LOGGER.debug("Info: Starting HTTPs server");

      startHttpsServer(serverOptions);

    } else {
      LOGGER.debug("Info: Starting HTTP server");

      startHttpServer(serverOptions);
    }
    LOGGER.debug("Started HTTP server at port : " + port);

    serverOptions.setCompressionSupported(true).setCompressionLevel(5);
    // Instantiate this server
    server = vertx.createHttpServer(serverOptions);
    // API Callback managers

    // Todo - Set service proxies based on availability?
    GeocodingService geoService = GeocodingService.createProxy(vertx, GEOCODING_SERVICE_ADDRESS);
    geocodingController = new GeocodingController(geoService);

    NLPSearchService nlpsearchService = NLPSearchService.createProxy(vertx, NLP_SERVICE_ADDRESS);
    ElasticsearchService elasticsearchService =
        ElasticsearchService.createProxy(vertx, ELASTIC_SERVICE_ADDRESS);
    ItemService itemService;
    optionalModules = config().getJsonArray(OPTIONAL_MODULES);
    if (optionalModules.contains(NLPSEARCH_PACKAGE_NAME)
        && optionalModules.contains(GEOCODING_PACKAGE_NAME)) {
      itemService =
          new ItemServiceImpl(elasticsearchService, geoService, nlpsearchService, config());
    } else {
      itemService = new ItemServiceImpl(elasticsearchService, config());
    }
    AuditingService auditingService = AuditingService.createProxy(vertx, AUDITING_SERVICE_ADDRESS);
    AuditHandler auditHandler = new AuditHandler(auditingService);
    FailureHandler failureHandler = new FailureHandler();
    CrudService crudService = new CrudService(itemService);
    AuthenticationService authService =
        AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
    authHandler = new AuthHandler(authService);
    AuthValidationHandler validateToken = new AuthValidationHandler(authService);
    authorizationHandler = new AuthorizationHandler();
    ValidatorService validationService =
        ValidatorService.createProxy(vertx, VALIDATION_SERVICE_ADDRESS);
    boolean isUac = config().getBoolean(UAC_DEPLOYMENT);
    crudController = new CrudController(isUac, host, crudService,
        validationService, authHandler, validateToken, authorizationHandler, auditHandler,
        failureHandler);
    RatingService ratingService = RatingService.createProxy(vertx, RATING_SERVICE_ADDRESS);
    ratingController = new RatingController(validationService, ratingService,
        host, authHandler, validateToken, authorizationHandler, auditHandler,
        failureHandler);
    listController = new ListController(elasticsearchService, docIndex);
    searchController = new SearchController(elasticsearchService, geoService, nlpsearchService,
        failureHandler, dxApiBasePath, docIndex);
    MlayerService mlayerService = MlayerService.createProxy(vertx, MLAYER_SERVICE_ADDRESS);
    mlayerController = new MlayerController(host, validationService,
        mlayerService, failureHandler, authHandler, validateToken);
    RelationshipService relService = new RelationshipServiceImpl(elasticsearchService, docIndex);
    relationshipController = new RelationshipController(relService);

    //  Documentation routes

    /* Static Resource Handler */
    /* Get openapiv3 spec */
    router
        .get(ROUTE_STATIC_SPEC)
        .produces(MIME_APPLICATION_JSON)
        .handler(
            routingContext -> {
              HttpServerResponse response = routingContext.response();
              response.sendFile("docs/openapi.yaml");
            });
    /* Get redoc */
    router
        .get(ROUTE_DOC)
        .produces(MIME_TEXT_HTML)
        .handler(
            routingContext -> {
              HttpServerResponse response = routingContext.response();
              response.sendFile("docs/apidoc.html");
            });

    // UI routes

    /* Static Resource Handler */
    router
        .route("/*")
        .produces("text/html")
        .handler(StaticHandler.create("ui/dist/dk-customer-ui/"));

    router
        .route("/assets/*")
        .produces("*/*")
        .handler(StaticHandler.create("ui/dist/dk-customer-ui/assets/"));

    router
        .route("/")
        .produces("text/html")
        .handler(
            routingContext -> {
              HttpServerResponse response = routingContext.response();
              response.sendFile("ui/dist/dk-customer-ui/index.html");
            });

    StacController stacController = new StacController(api, config(), validationService,
        auditHandler, elasticsearchService, authHandler, validateToken, failureHandler);

    // Initialize controllers and register their routes
    router.route(api.getStackRestApis() + "/*").subRouter(stacController.init(router));
    router.route(dxApiBasePath + "/*").subRouter(geocodingController.init(router));
    router.route(dxApiBasePath + "/*").subRouter(crudController.init(router));
    router.route(dxApiBasePath + "/*").subRouter(ratingController.init(router));
    router.route(dxApiBasePath + "/*").subRouter(listController.init(router));
    router.route(dxApiBasePath + "/*").subRouter(searchController.init(router));
    router.route(dxApiBasePath + "/*").subRouter(relationshipController.init(router));
    router.route(dxApiBasePath + "/*").subRouter(mlayerController.init(router));

    // Start server
    server.requestHandler(router).listen(port);

    /* Print the deployed endpoints */
    printDeployedEndpoints(router);
    LOGGER.info("API server deployed on :" + serverOptions.getPort());
  }

  private void startHttpServer(HttpServerOptions serverOptions) {
    /* Set up the HTTP server properties, APIs and port. */

    serverOptions.setSsl(false);
    /*
     * Default port when ssl is disabled is 8080. If set through config, then that value is taken
     */
    port = config().getInteger(PORT) == null ? 8080 : config().getInteger(PORT);
  }

  private void startHttpsServer(HttpServerOptions serverOptions) {
    /* Read the configuration and set the HTTPs server properties. */

    keystore = config().getString("keystore");
    keystorePassword = config().getString("keystorePassword");

    /*
     * Default port when ssl is enabled is 8443. If set through config, then that value is taken
     */
    port = config().getInteger(PORT) == null ? 8443 : config().getInteger(PORT);

    /* Set up the HTTPs server properties, APIs and port. */

    serverOptions
        .setSsl(true)
        .setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword(keystorePassword));
  }

  private void printDeployedEndpoints(Router router) {
    for (Route route : router.getRoutes()) {
      if (route.getPath() != null) {
        LOGGER.info("API Endpoints deployed : " + route.methods() + " : " + route.getPath());
      }
    }
  }

  @Override
  public void stop() {
    LOGGER.info("Stopping the API server");
  }
}
