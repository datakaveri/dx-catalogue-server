package iudx.catalogue.server.apiserver;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.authenticator.Constants.*;
import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.util.Constants.METHOD;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.apiserver.util.QueryMapper;
import iudx.catalogue.server.apiserver.util.RespBuilder;
import iudx.catalogue.server.authenticator.AuthenticationService;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.util.Api;
import iudx.catalogue.server.validator.ValidatorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MyAssetsApis {
  private static final Logger LOGGER = LogManager.getLogger(MyAssetsApis.class);
  private final Api api;
  private DatabaseService dbService;
  private ValidatorService validatorService;
  private AuthenticationService authService;

  public MyAssetsApis(Api api) {
    this.api = api;
  }

  /**
   * Sets the database service, validator service, and auth service for this class.
   *
   * @param dbService        the database service to be set
   * @param validatorService the validator service to be set
   * @param authService      the authservice to be set
   */
  public void setService(DatabaseService dbService, ValidatorService validatorService,
                         AuthenticationService authService) {
    this.dbService = dbService;
    this.validatorService = validatorService;
    this.authService = authService;
  }

  public void getSearchHandler(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    String token = routingContext.get(HEADER_TOKEN);
    String instanceId = request.getHeader(HEADER_INSTANCE);

    // Set defaults
    int size = DEFAULT_MAX_PAGE_SIZE;
    int page = DEFAULT_PAGE_NUMBER;

    if (request.getParam(SIZE_KEY) != null) {
      try {
        size = Integer.parseInt(request.getParam(SIZE_KEY));
      } catch (NumberFormatException e) {
        LOGGER.warn("Invalid size param, defaulting to {}", size);
      }
    }

    if (request.getParam(PAGE_KEY) != null) {
      try {
        page = Integer.parseInt(request.getParam(PAGE_KEY));
      } catch (NumberFormatException e) {
        LOGGER.warn("Invalid page param, defaulting to {}", page);
      }
    }

    int offset = size * (page - 1);

    if (token != null && !token.isEmpty()) {
      JsonObject jwtAuthenticationInfo = new JsonObject()
          .put(TOKEN, token)
          .put(METHOD, REQUEST_GET)
          .put(API_ENDPOINT, routingContext.normalizedPath());

      int finalSize = size;
      int finalPage = page;
      authService.tokenInterospect(new JsonObject(), jwtAuthenticationInfo, authHandler -> {
        if (authHandler.failed()) {
          LOGGER.error("Unauthorized access: {}", authHandler.cause().getMessage());
          response.setStatusCode(401).end(new RespBuilder()
              .withType(TYPE_TOKEN_INVALID)
              .withTitle(TITLE_TOKEN_INVALID)
              .withDetail(authHandler.cause().getMessage())
              .getResponse());
        } else {
          String userId = authHandler.result().getString(SUB);

          JsonObject requestBody = new JsonObject()
              .put(SUB, userId)
              .put(MY_ASSETS_REQ, true)
              .put(SEARCH_TYPE, SEARCH_TYPE_MY_ASSETS_ALL)
              .put(SIZE_KEY, finalSize)
              .put(LIMIT, finalSize)
              .put(PAGE_KEY, finalPage)
              .put(OFFSET, offset);
          QueryMapper.extractSortFromRawUri(routingContext.request().uri(), requestBody);

          // Also include instance if present
          if (instanceId != null) {
            requestBody.put(HEADER_INSTANCE, instanceId);
          }

          dbService.searchQuery(requestBody, handler -> {
            if (handler.succeeded()) {
              JsonObject resultJson = handler.result();
              String status = resultJson.getString(STATUS);
              if (status.equalsIgnoreCase(SUCCESS)) {
                LOGGER.info("Success: GET /my-assets");
                response.setStatusCode(200);
              } else if (status.equalsIgnoreCase(PARTIAL_CONTENT)) {
                response.setStatusCode(206);
              } else {
                LOGGER.error("Fail: search query");
                response.setStatusCode(400);
              }
              response.end(resultJson.toString());
            } else {
              LOGGER.error("Fail: GET /my-assets; {}", handler.cause().getMessage());
              response.setStatusCode(400).end(handler.cause().getMessage());
            }
          });
        }
      });
    } else {
      LOGGER.warn("Missing auth token");
      response.setStatusCode(401).end(new RespBuilder()
          .withType(TYPE_MISSING_TOKEN)
          .withTitle(TITLE_MISSING_TOKEN)
          .withDetail("Token is required for /search/myassets")
          .getResponse());
    }
  }

  /**
   * Processes the attribute, temporal, range, geoSpatial, and text search  POST requests and
   * returns the results
   * from the
   * database.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void postSearchHandler(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject requestBody = routingContext.body().asJsonObject();

    /* HTTP request instance/host details */
    String instanceId = request.getHeader(HEADER_INSTANCE);
    requestBody.put(HEADER_INSTANCE, instanceId);

    // Set default size and page
    int size = DEFAULT_MAX_PAGE_SIZE;
    int page = DEFAULT_PAGE_NUMBER;

    // Override if query params are present
    if (request.getParam(SIZE_KEY) != null) {
      try {
        size = Integer.parseInt(request.getParam(SIZE_KEY));
      } catch (NumberFormatException e) {
        LOGGER.warn("Invalid size param, defaulting to 100");
      }
    }

    if (request.getParam(PAGE_KEY) != null) {
      try {
        page = Integer.parseInt(request.getParam(PAGE_KEY));
      } catch (NumberFormatException e) {
        LOGGER.warn("Invalid page param, defaulting to 1");
      }
    }

    int offset = size * (page - 1);

    requestBody.put(SIZE_KEY, size);
    requestBody.put(LIMIT, size);
    requestBody.put(PAGE_KEY, page);
    requestBody.put(OFFSET, offset);

    QueryMapper.extractSortFromRawUri(routingContext.request().uri(), requestBody);

    String token = routingContext.get(HEADER_TOKEN);
    if (token != null && !token.isEmpty()) {
      JsonObject jwtAuthenticationInfo = new JsonObject()
          .put(TOKEN, token)
          .put(METHOD, REQUEST_GET)
          .put(API_ENDPOINT, routingContext.normalizedPath());

      authService.tokenInterospect(new JsonObject(), jwtAuthenticationInfo, authHandler -> {
        if (authHandler.failed()) {
          LOGGER.error("Error: " + authHandler.cause().getMessage());
          response.setStatusCode(401)
              .end(new RespBuilder()
                  .withType(TYPE_TOKEN_INVALID)
                  .withTitle(TITLE_TOKEN_INVALID)
                  .withDetail(authHandler.cause().getMessage())
                  .getResponse());
        } else {
          requestBody.put(SUB, authHandler.result().getString(SUB));
          processSearch(request, response, requestBody);
        }
      });
    } else {
      // No token provided, proceed without "sub"
      processSearch(request, response, requestBody);
    }
  }

  private void processSearch(HttpServerRequest request, HttpServerResponse response,
                             JsonObject requestBody) {
    boolean hasValidFilter = false;

    if ((!requestBody.containsKey(SEARCH_CRITERIA_KEY)
        || requestBody.getJsonArray(SEARCH_CRITERIA_KEY).isEmpty())
        && (!requestBody.containsKey(GEOPROPERTY)
        || !requestBody.containsKey(GEORELATION)
        || !requestBody.containsKey(GEOMETRY)
        || !requestBody.containsKey(COORDINATES))
        && !requestBody.containsKey(Q_VALUE)) {

      LOGGER.error("Fail: Invalid Syntax");
      response.setStatusCode(400)
          .end(new RespBuilder()
              .withType(TYPE_INVALID_SYNTAX)
              .withTitle(TITLE_INVALID_SYNTAX)
              .withDetail("Mandatory field(s) not provided")
              .getResponse());
      return;
    }

    /* SEARCH_CRITERIA filter (attribute, temporal, range) */
    if (requestBody.getJsonArray(SEARCH_CRITERIA_KEY) != null
        && !requestBody.getJsonArray(SEARCH_CRITERIA_KEY).isEmpty()) {
      requestBody.put(SEARCH_TYPE, requestBody.getString(SEARCH_TYPE, "") + SEARCH_TYPE_CRITERIA);
      hasValidFilter = true;
    }

    /* GEO filter */
    if (GEOMETRIES.contains(requestBody.getString(GEOMETRY))
        && GEORELS.contains(requestBody.getString(GEORELATION))
        && GEO_PROPERTY.equals(requestBody.getString(GEOPROPERTY))) {
      requestBody.put(SEARCH_TYPE, requestBody.getString(SEARCH_TYPE, "") + SEARCH_TYPE_GEO);
      hasValidFilter = true;
    }

    /* TEXT filter */
    if (requestBody.getString(Q_VALUE) != null && !requestBody.getString(Q_VALUE).isBlank()) {
      requestBody.put(SEARCH_TYPE, requestBody.getString(SEARCH_TYPE, "") + SEARCH_TYPE_TEXT);
      hasValidFilter = true;
    }

    /* TAG filter */
    if (requestBody.containsKey(FILTER) && requestBody.getJsonArray(FILTER) != null) {
      requestBody.put(SEARCH_TYPE, requestBody.getString(SEARCH_TYPE, "") + RESPONSE_FILTER);
      hasValidFilter = true;
    }

    /* If none of the filters are valid, respond with 400 */
    if (!hasValidFilter) {
      LOGGER.error("Fail: Invalid Syntax");
      response.setStatusCode(400)
          .end(new RespBuilder()
              .withType(TYPE_INVALID_SYNTAX)
              .withTitle(TITLE_INVALID_SYNTAX)
              .withDetail("Mandatory field(s) not provided")
              .getResponse());
      return;
    }

    validatorService.validateSearchQuery(requestBody, validateHandler -> {
      if (validateHandler.failed()) {
        LOGGER.error("Fail: Search/Count; Invalid request query parameters");
        response.setStatusCode(400)
            .end(validateHandler.cause().getLocalizedMessage());
      } else {
        String path = request.path();
        requestBody.put(MY_ASSETS_REQ, true);
        if (path.equals(api.getRouteSearchMyAssets())) {
          dbService.searchQuery(requestBody, handler -> {
            if (handler.succeeded()) {
              JsonObject resultJson = handler.result();
              String status = resultJson.getString(STATUS);
              if (status.equalsIgnoreCase(SUCCESS)) {
                LOGGER.info("Success: search query");
                response.setStatusCode(200);
              } else if (status.equalsIgnoreCase(PARTIAL_CONTENT)) {
                LOGGER.info("Success: search query");
                response.setStatusCode(206);
              } else {
                LOGGER.error("Fail: search query");
                response.setStatusCode(400);
              }
              response.end(resultJson.toString());
            } else if (handler.failed()) {
              LOGGER.error("Fail: Search;" + handler.cause().getMessage());
              response.setStatusCode(400).end(handler.cause().getMessage());
            }
          });
        } else {
          dbService.countQuery(requestBody, handler -> {
            if (handler.succeeded()) {
              JsonObject resultJson = handler.result();
              String status = resultJson.getString(STATUS);
              if (status.equalsIgnoreCase(SUCCESS)) {
                LOGGER.info("Success: count query");
                response.setStatusCode(200);
              } else if (status.equalsIgnoreCase(PARTIAL_CONTENT)) {
                LOGGER.info("Success: count query");
                response.setStatusCode(206);
              } else {
                LOGGER.error("Fail: count query");
                response.setStatusCode(400);
              }
              response.end(resultJson.toString());
            } else if (handler.failed()) {
              LOGGER.error("Fail: Count;" + handler.cause().getMessage());
              response.setStatusCode(400)
                  .end(handler.cause().getMessage());
            }
          });
        }
      }
    });
  }
}
