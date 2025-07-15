package iudx.catalogue.server.apiserver;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;
import iudx.catalogue.server.apiserver.util.ExceptionHandler;
import iudx.catalogue.server.auditing.AuditingService;
import iudx.catalogue.server.authenticator.AuthenticationService;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.geocoding.GeocodingService;
import iudx.catalogue.server.mlayer.MlayerService;
import iudx.catalogue.server.nlpsearch.NLPSearchService;
import iudx.catalogue.server.rating.RatingService;
import iudx.catalogue.server.util.Api;
import iudx.catalogue.server.validator.ValidatorService;
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
  private HttpServer server;
  private CrudApis crudApis;
  private SearchApis searchApis;
  private MyAssetsApis myAssetsApis;
  private OwnershipApis ownershipApis;
  private OrganisationApis organisationApis;
  private String keystore;
  private String keystorePassword;
  private ListApis listApis;
  private RelationshipApis relApis;
  private GeocodingApis geoApis;
  private RatingApis ratingApis;
  private MlayerApis mlayerApis;

  @SuppressWarnings("unused")
  private Router router;

  private String catAdmin;
  private boolean isSsL;
  private int port;
  private String dxApiBasePath;
  private Api api;

  /**
   * This method is used to start the Verticle and joing a cluster.
   *
   * @throws Exception which is a startup exception
   */
  @Override
  public void start() throws Exception {

    router = Router.router(vertx);

    dxApiBasePath = config().getString("dxApiBasePath");
    api = Api.getInstance(dxApiBasePath);

    /* Configure */
    catAdmin = config().getString(CAT_ADMIN);
    isSsL = config().getBoolean(IS_SSL);

    HttpServerOptions serverOptions = new HttpServerOptions();

    if (isSsL) {
      LOGGER.debug("Info: Starting HTTPs server");

      /* Read the configuration and set the HTTPs server properties. */

      keystore = config().getString("keystore");
      keystorePassword = config().getString("keystorePassword");

      /*
       * Default port when ssl is enabled is 8443. If set through config, then that value is taken
       */
      port = config().getInteger(PORT) == null ? 8443 : config().getInteger(PORT);

      /* Setup the HTTPs server properties, APIs and port. */

      serverOptions
          .setSsl(true)
          .setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword(keystorePassword));

    } else {
      LOGGER.debug("Info: Starting HTTP server");

      /* Setup the HTTP server properties, APIs and port. */

      serverOptions.setSsl(false);
      /*
       * Default port when ssl is disabled is 8080. If set through config, then that value is taken
       */
      port = config().getInteger(PORT) == null ? 8080 : config().getInteger(PORT);
    }
    LOGGER.debug("Started HTTP server at port : " + port);

    serverOptions.setCompressionSupported(true).setCompressionLevel(5);
    // Instantiate this server
    server = vertx.createHttpServer(serverOptions);

    boolean isUac = config().getBoolean(UAC_DEPLOYMENT);
    // API Callback managers
    crudApis = new CrudApis(api, isUac);
    searchApis = new SearchApis(api);
    myAssetsApis = new MyAssetsApis(api);
    ownershipApis = new OwnershipApis(api);
    organisationApis = new OrganisationApis(api);
    listApis = new ListApis(api);
    relApis = new RelationshipApis();
    geoApis = new GeocodingApis();
    ratingApis = new RatingApis();
    mlayerApis = new MlayerApis(api);

    // Todo - Set service proxies based on availability?
    DatabaseService dbService = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);

    RatingService ratingService = RatingService.createProxy(vertx, RATING_SERVICE_ADDRESS);
    ratingApis.setRatingService(ratingService);

    MlayerService mlayerService = MlayerService.createProxy(vertx, MLAYER_SERVICE_ADDRESSS);
    mlayerApis.setMlayerService(mlayerService);

    crudApis.setDbService(dbService);
    listApis.setDbService(dbService);
    ownershipApis.setDbService(dbService);
    relApis.setDbService(dbService);
    // TODO : set db service for Rating APIs
    crudApis.setHost(config().getString(HOST));
    ratingApis.setHost(config().getString(HOST));
    mlayerApis.setHost(config().getString(HOST));

    AuthenticationService authService =
        AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
    crudApis.setAuthService(authService);
    ownershipApis.setAuthService(authService);
    ratingApis.setAuthService(authService);
    mlayerApis.setAuthService(authService);
    listApis.setAuthService(authService);

    ValidatorService validationService =
        ValidatorService.createProxy(vertx, VALIDATION_SERVICE_ADDRESS);
    crudApis.setValidatorService(validationService);
    ratingApis.setValidatorService(validationService);
    mlayerApis.setValidatorService(validationService);

    GeocodingService geoService = GeocodingService.createProxy(vertx, GEOCODING_SERVICE_ADDRESS);
    geoApis.setGeoService(geoService);

    NLPSearchService nlpsearchService = NLPSearchService.createProxy(vertx, NLP_SERVICE_ADDRESS);

    searchApis.setService(dbService, geoService, nlpsearchService, validationService, authService);
    myAssetsApis.setService(dbService, validationService, authService);
    organisationApis.setService(dbService, authService);

    AuditingService auditingService = AuditingService.createProxy(vertx, AUDITING_SERVICE_ADDRESS);
    crudApis.setAuditingService(auditingService);
    ratingApis.setAuditingService(auditingService);
    ExceptionHandler exceptionhandler = new ExceptionHandler();

    // API Routes and Callbacks

    // Routes - Defines the routes and callbacks
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router
        .route()
        .handler(
            CorsHandler.create("*")
                .allowedHeaders(ALLOWED_HEADERS)
                .allowedMethods(ALLOWED_METHODS));

    router
        .route()
        .handler(
            routingContext -> {
              routingContext
                  .response()
                  .putHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate")
                  .putHeader("Pragma", "no-cache")
                  .putHeader("Expires", "0")
                  .putHeader("X-Content-Type-Options", "nosniff");
              routingContext.next();
            });

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

    // Routes for item CRUD

    /* Create Item - Body contains data */
    router
        .post(api.getRouteItems())
        .consumes(MIME_APPLICATION_JSON)
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(
            routingContext -> {
              /* checking auhthentication info in requests */
              if (routingContext.request().headers().contains(HEADER_AUTHORIZATION)) {
                String authHeader = routingContext.request().getHeader(HEADER_AUTHORIZATION);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                  String token = authHeader.substring("Bearer ".length()).trim();
                  routingContext.put(HEADER_TOKEN, token);
                }
                crudApis.createItemHandler(routingContext);
              } else {
                LOGGER.warn("Fail: Unathorized CRUD operation");
                routingContext.response().setStatusCode(401).end();
              }
            });

    /* Get Item */
    router
        .get(api.getRouteItems())
        .produces(MIME_APPLICATION_JSON)
        .handler(
            routingContext -> {
              String authHeader = routingContext.request().getHeader(HEADER_AUTHORIZATION);
              if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring("Bearer ".length()).trim();
                routingContext.put(HEADER_TOKEN, token);
              }
              crudApis.getItemHandler(routingContext);
            });

    /* Update Item - Body contains data */
    router
        .put(api.getRoutUpdateItems())
        .consumes(MIME_APPLICATION_JSON)
        .produces(MIME_APPLICATION_JSON)
        .handler(
            routingContext -> {
              /* checking auhthentication info in requests */
              if (routingContext.request().headers().contains(HEADER_AUTHORIZATION)) {
                String authHeader = routingContext.request().getHeader(HEADER_AUTHORIZATION);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                  String token = authHeader.substring("Bearer ".length()).trim();
                  routingContext.put(HEADER_TOKEN, token);
                }
                // Update params checked in createItemHandler
                crudApis.createItemHandler(routingContext);
              } else {
                LOGGER.warn("Unathorized CRUD operation");
                routingContext.response().setStatusCode(401).end();
              }
            });

    /* Delete Item - Query param contains id */
    router
        .delete(api.getRouteDeleteItems())
        .produces(MIME_APPLICATION_JSON)
        .handler(
            routingContext -> {
              /* checking auhthentication info in requests */
              if (routingContext.request().headers().contains(HEADER_AUTHORIZATION)
                  && routingContext.queryParams().contains(ID)) {
                String authHeader = routingContext.request().getHeader(HEADER_AUTHORIZATION);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                  String token = authHeader.substring("Bearer ".length()).trim();
                  routingContext.put(HEADER_TOKEN, token);
                }
                // Update params checked in createItemHandler
                crudApis.deleteItemHandler(routingContext);
              } else {
                LOGGER.warn("Unathorized CRUD operation");
                routingContext.response().setStatusCode(401).end();
              }
            });

    /* Create instance - Instance name in query param */
    router
        .post(api.getRouteInstance())
        .produces(MIME_APPLICATION_JSON)
        .handler(
            routingContext -> {
              /* checking auhthentication info in requests */
              if (routingContext.request().headers().contains(HEADER_AUTHORIZATION)) {
                String authHeader = routingContext.request().getHeader(HEADER_AUTHORIZATION);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                  String token = authHeader.substring("Bearer ".length()).trim();
                  routingContext.put(HEADER_TOKEN, token);
                }
                crudApis.createInstanceHandler(routingContext, catAdmin);
              } else {
                LOGGER.warn("Fail: Unathorized CRUD operation");
                routingContext.response().setStatusCode(401).end();
              }
            });

    /* Delete instance - Instance name in query param */
    router
        .delete(api.getRouteInstance())
        .produces(MIME_APPLICATION_JSON)
        .handler(
            routingContext -> {
              /* checking auhthentication info in requests */
              LOGGER.debug("Info: HIT instance");
              if (routingContext.request().headers().contains(HEADER_AUTHORIZATION)) {
                String authHeader = routingContext.request().getHeader(HEADER_AUTHORIZATION);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                  String token = authHeader.substring("Bearer ".length()).trim();
                  routingContext.put(HEADER_TOKEN, token);
                }
                crudApis.deleteInstanceHandler(routingContext, catAdmin);
              } else {
                LOGGER.warn("Fail: Unathorized CRUD operation");
                routingContext.response().setStatusCode(401).end();
              }
            });

    // Routes for search and count

    /* Search for an item */
    router
        .get(api.getRouteSearch())
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(
            routingContext -> {
              searchApis.searchHandler(routingContext);
            });
    router
        .post(api.getRouteSearch())
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(
            routingContext -> {
              String authHeader = routingContext.request().getHeader(HEADER_AUTHORIZATION);
              if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring("Bearer ".length()).trim();
                routingContext.put(HEADER_TOKEN, token);
              }
              searchApis.postSearchHandler(routingContext);
            });
    router
        .get(api.getRouteSearchMyAssets())
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(
            routingContext -> {
              /* checking authentication info in requests */
              if (routingContext.request().headers().contains(HEADER_AUTHORIZATION)) {
                String authHeader = routingContext.request().getHeader(HEADER_AUTHORIZATION);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                  String token = authHeader.substring("Bearer ".length()).trim();
                  routingContext.put(HEADER_TOKEN, token);
                }
                myAssetsApis.getSearchHandler(routingContext);
              } else {
                LOGGER.warn("Fail: Unathorized CRUD operation");
                routingContext.response().setStatusCode(401).end();
              }
            });
    router
        .post(api.getRouteSearchMyAssets())
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(
            routingContext -> {
              /* checking authentication info in requests */
              if (routingContext.request().headers().contains(HEADER_AUTHORIZATION)) {
                String authHeader = routingContext.request().getHeader(HEADER_AUTHORIZATION);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                  String token = authHeader.substring("Bearer ".length()).trim();
                  routingContext.put(HEADER_TOKEN, token);
                }
                myAssetsApis.postSearchHandler(routingContext);
              } else {
                LOGGER.warn("Fail: Unauthorized CRUD operation");
                routingContext.response().setStatusCode(401).end();
              }
            });
    router
        .post(api.getRouteOwnershipTransfer())
        .produces(MIME_APPLICATION_JSON)
        .consumes(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(
            routingContext -> {
              /* checking authentication info in requests */
              if (routingContext.request().headers().contains(HEADER_AUTHORIZATION)) {
                String authHeader = routingContext.request().getHeader(HEADER_AUTHORIZATION);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                  String token = authHeader.substring("Bearer ".length()).trim();
                  routingContext.put(HEADER_TOKEN, token);
                }
                ownershipApis.transferOwnershipHandler(routingContext);
              } else {
                LOGGER.warn("Fail: Unauthorized Ownership transfer operation");
                routingContext.response().setStatusCode(401).end();
              }
            });

    router
        .delete(api.getRouteOwnershipDelete())
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(
            routingContext -> {
              /* checking authentication info in requests */
              if (routingContext.request().headers().contains(HEADER_AUTHORIZATION)) {
                String authHeader = routingContext.request().getHeader(HEADER_AUTHORIZATION);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                  String token = authHeader.substring("Bearer ".length()).trim();
                  routingContext.put(HEADER_TOKEN, token);
                }
                ownershipApis.deleteOwnershipHandler(routingContext);
              } else {
                LOGGER.warn("Fail: Unauthorized Delete operation");
                routingContext.response().setStatusCode(401).end();
              }
            });


    /* Partial Update Item - Body contains fields to be updated */
    router
        .patch(api.getRouteOrgAsset())
        .consumes(MIME_APPLICATION_JSON)
        .produces(MIME_APPLICATION_JSON)
        .handler(routingContext -> {
          /* Check for Authorization Header */
          if (routingContext.request().headers().contains(HEADER_AUTHORIZATION)) {
            String authHeader = routingContext.request().getHeader(HEADER_AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
              String token = authHeader.substring("Bearer ".length()).trim();
              routingContext.put(HEADER_TOKEN, token);
            }

            organisationApis.partialUpdateItemHandler(routingContext);

          } else {
            LOGGER.warn("Unauthorized partial update attempt");
            routingContext.response().setStatusCode(401).end();
          }
        });

    /* To get all the Items from an organization */
    router
        .get(api.getRouteOrgAsset())
        .produces(MIME_APPLICATION_JSON)
        .handler(routingContext -> {
          /* Check for Authorization Header */
          if (routingContext.request().headers().contains(HEADER_AUTHORIZATION)) {
            String authHeader = routingContext.request().getHeader(HEADER_AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
              String token = authHeader.substring("Bearer ".length()).trim();
              routingContext.put(HEADER_TOKEN, token);
              organisationApis.getItemsByOrgHandler(routingContext);
            } else {
              LOGGER.warn("Invalid Authorization header");
              routingContext.response().setStatusCode(401).end();
            }
          } else {
            LOGGER.warn("Unauthorized GET attempt");
            routingContext.response().setStatusCode(401).end();
          }
        });

    router.get(api.getRouteHealthLive()).handler(ctx ->
        ctx.response()
            .setStatusCode(200)
            .putHeader("Content-Type", "text/plain")
            .end("Alive")
    );

    /* NLP Search */
    router
        .get(api.getRouteNlpSearch())
        .produces(MIME_APPLICATION_JSON)
        .handler(
            routingContext -> {
              searchApis.nlpSearchHandler(routingContext);
            });

    /* Count the Cataloque server items */
    router
        .post(api.getRouteCount())
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(
            routingContext -> {
              String authHeader = routingContext.request().getHeader(HEADER_AUTHORIZATION);
              if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring("Bearer ".length()).trim();
                routingContext.put(HEADER_TOKEN, token);
              }
              searchApis.postSearchHandler(routingContext);
            });

    //  Routes for list

    /* list the item from database using itemId */
    router
        .get(api.getRouteListItems())
        .produces(MIME_APPLICATION_JSON)
        .handler(
            routingContext -> {
              listApis.listItemsHandler(routingContext);
            });
    router
        .post(api.getRouteListMulItems())
        .produces(MIME_APPLICATION_JSON)
        .handler(
            routingContext -> {
              String authHeader = routingContext.request().getHeader(HEADER_AUTHORIZATION);
              if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring("Bearer ".length()).trim();
                routingContext.put(HEADER_TOKEN, token);
              }
              listApis.listItemsPostHandler(routingContext);
            });

    //  Routes for relationships

    /* Relationship related search */
    router
        .get(api.getRouteRelSearch())
        .handler(
            routingContext -> {
              relApis.relSearchHandler(routingContext);
            });

    /* Get all resources belonging to a resource group */
    router
        .get(api.getRouteRelationship())
        .handler(
            routingContext -> {
              relApis.listRelationshipHandler(routingContext);
            });

    //  Routes for Geocoding

    router
        .get(api.getRouteGeoCoordinates())
        .handler(
            routingContext -> {
              geoApis.getCoordinates(routingContext);
            });

    router
        .get(api.getRouteGeoReverse())
        .handler(
            routingContext -> {
              geoApis.getLocation(routingContext);
            });

    //  Routes for Rating APIs

    /* Create Rating */
    router
        .post(ROUTE_RATING)
        .consumes(MIME_APPLICATION_JSON)
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(
            routingContext -> {
              if (routingContext.request().headers().contains(HEADER_AUTHORIZATION)) {
                String authHeader = routingContext.request().getHeader(HEADER_AUTHORIZATION);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                  String token = authHeader.substring("Bearer ".length()).trim();
                  routingContext.put(HEADER_TOKEN, token);
                }
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
                if (routingContext.request().headers().contains(HEADER_AUTHORIZATION)) {
                  String authHeader = routingContext.request().getHeader(HEADER_AUTHORIZATION);
                  if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring("Bearer ".length()).trim();
                    routingContext.put(HEADER_TOKEN, token);
                  }
                  ratingApis.getRatingHandler(routingContext);
                } else {
                  LOGGER.error("Unauthorized Operation");
                  routingContext.response().setStatusCode(401).end();
                }
              }
            });

    /* Update Rating */
    router
        .put(ROUTE_RATING)
        .consumes(MIME_APPLICATION_JSON)
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(
            routingContext -> {
              if (routingContext.request().headers().contains(HEADER_AUTHORIZATION)) {
                String authHeader = routingContext.request().getHeader(HEADER_AUTHORIZATION);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                  String token = authHeader.substring("Bearer ".length()).trim();
                  routingContext.put(HEADER_TOKEN, token);
                }
                ratingApis.updateRatingHandler(routingContext);
              } else {
                LOGGER.error("Unauthorized Operation");
                routingContext.response().setStatusCode(401).end();
              }
            });

    /* Delete Rating */
    router
        .delete(ROUTE_RATING)
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(
            routingContext -> {
              if (routingContext.request().headers().contains(HEADER_AUTHORIZATION)) {
                String authHeader = routingContext.request().getHeader(HEADER_AUTHORIZATION);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                  String token = authHeader.substring("Bearer ".length()).trim();
                  routingContext.put(HEADER_TOKEN, token);
                }
                ratingApis.deleteRatingHandler(routingContext);
              } else {
                LOGGER.error("Unauthorized Operation");
                routingContext.response().setStatusCode(401).end();
              }
            });

    // Routes for Mlayer Instance APIs

    /* Create Mlayer Instance */
    router
        .post(api.getRouteMlayerInstance())
        .consumes(MIME_APPLICATION_JSON)
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(
            routingContext -> {
              if (routingContext.request().headers().contains(HEADER_AUTHORIZATION)) {
                String authHeader = routingContext.request().getHeader(HEADER_AUTHORIZATION);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                  String token = authHeader.substring("Bearer ".length()).trim();
                  routingContext.put(HEADER_TOKEN, token);
                }
                mlayerApis.createMlayerInstanceHandler(routingContext);
              } else {
                LOGGER.error("Unauthorized Operation");
                routingContext.response().setStatusCode(401).end();
              }
            });

    /* Get Mlayer Instance */
    router
        .get(api.getRouteMlayerInstance())
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(
            routingContext -> {
              mlayerApis.getMlayerInstanceHandler(routingContext);
            });

    /* Delete Mlayer Instance */
    router
        .delete(api.getRouteMlayerInstance())
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(
            routingContext -> {
              if (routingContext.request().headers().contains(HEADER_AUTHORIZATION)) {
                String authHeader = routingContext.request().getHeader(HEADER_AUTHORIZATION);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                  String token = authHeader.substring("Bearer ".length()).trim();
                  routingContext.put(HEADER_TOKEN, token);
                }
                mlayerApis.deleteMlayerInstanceHandler(routingContext);
              } else {
                LOGGER.error("Unauthorized Operation");
                routingContext.response().setStatusCode(401).end();
              }
            });

    /* Update Mlayer Instance */
    router
        .put(api.getRouteMlayerInstance())
        .consumes(MIME_APPLICATION_JSON)
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(
            routingContext -> {
              if (routingContext.request().headers().contains(HEADER_AUTHORIZATION)) {
                String authHeader = routingContext.request().getHeader(HEADER_AUTHORIZATION);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                  String token = authHeader.substring("Bearer ".length()).trim();
                  routingContext.put(HEADER_TOKEN, token);
                }
                mlayerApis.updateMlayerInstanceHandler(routingContext);
              } else {
                LOGGER.error("Unauthorized Operation");
                routingContext.response().setStatusCode(401).end();
              }
            });

    // Routes for Mlayer Domain APIs

    /* Create Mlayer Domain */
    router
        .post(api.getRouteMlayerDomains())
        .consumes(MIME_APPLICATION_JSON)
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(
            routingContext -> {
              if (routingContext.request().headers().contains(HEADER_AUTHORIZATION)) {
                String authHeader = routingContext.request().getHeader(HEADER_AUTHORIZATION);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                  String token = authHeader.substring("Bearer ".length()).trim();
                  routingContext.put(HEADER_TOKEN, token);
                }
                mlayerApis.createMlayerDomainHandler(routingContext);
              } else {
                LOGGER.error("Unauthorized Operation");
                routingContext.response().setStatusCode(401).end();
              }
            });

    /* Get Mlayer Domain */
    router
        .get(api.getRouteMlayerDomains())
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(
            routingContext -> {
              mlayerApis.getMlayerDomainHandler(routingContext);
            });

    /* Update Mlayer Domain */
    router
        .put(api.getRouteMlayerDomains())
        .consumes(MIME_APPLICATION_JSON)
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(
            routingContext -> {
              if (routingContext.request().headers().contains(HEADER_AUTHORIZATION)) {
                String authHeader = routingContext.request().getHeader(HEADER_AUTHORIZATION);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                  String token = authHeader.substring("Bearer ".length()).trim();
                  routingContext.put(HEADER_TOKEN, token);
                }
                mlayerApis.updateMlayerDomainHandler(routingContext);
              } else {
                LOGGER.error("Unauthorized Operation");
                routingContext.response().setStatusCode(401).end();
              }
            });

    /* Delete Mlayer Domain */
    router
        .delete(api.getRouteMlayerDomains())
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(
            routingContext -> {
              if (routingContext.request().headers().contains(HEADER_AUTHORIZATION)) {
                String authHeader = routingContext.request().getHeader(HEADER_AUTHORIZATION);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                  String token = authHeader.substring("Bearer ".length()).trim();
                  routingContext.put(HEADER_TOKEN, token);
                }
                mlayerApis.deleteMlayerDomainHandler(routingContext);
              } else {
                LOGGER.error("Unauthorized Operation");
                routingContext.response().setStatusCode(401).end();
              }
            });

    // Routes for Mlayer Provider API
    router
        .get(api.getRouteMlayerProviders())
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(
            routingContext -> {
              mlayerApis.getMlayerProvidersHandler(routingContext);
            });

    // Routes for Mlayer GeoQuery API
    router
        .post(api.getRouteMlayerGeoQuery())
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(
            routingContext -> {
              mlayerApis.getMlayerGeoQueryHandler(routingContext);
            });

    // Routes for Mlayer Dataset API
    /* route to get all datasets*/
    router
        .get(api.getRouteMlayerDataset())
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(
            routingContext -> {
              mlayerApis.getMlayerAllDatasetsHandler(routingContext);
            });
    /* route to get a dataset detail*/
    router
        .post(api.getRouteMlayerDataset())
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(
            routingContext -> {
              mlayerApis.getMlayerDatasetHandler(routingContext);
            });

    // Route for Mlayer PopularDatasets API
    router
        .get(api.getRouteMlayerPopularDatasets())
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(
            routingContext -> {
              mlayerApis.getMlayerPopularDatasetsHandler(routingContext);
            });

    // Total Count Api and Monthly Count & Size(MLayer)
    router
        .get(api.getSummaryCountSizeApi())
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(
            routingContext -> {
              mlayerApis.getSummaryCountSizeApi(routingContext);
            });
    router
        .get(api.getRealDatasetApi())
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(exceptionhandler)
        .handler(
            routingContext -> {
              mlayerApis.getCountSizeApi(routingContext);
            });

    router
        .route(api.getStackRestApis() + "/*")
        .subRouter(
            new StacRestApi(
                router, api, config(), validationService, authService, auditingService)
                .init());

    // Start server
    server.requestHandler(router).listen(port);

    /* Print the deployed endpoints */
    printDeployedEndpoints(router);
    LOGGER.info("API server deployed on :" + serverOptions.getPort());
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
