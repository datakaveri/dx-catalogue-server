/**
 * <h1>SearchApis.java</h1>
 * Callback handlers for CRUD
 */

package iudx.catalogue.server.apiserver;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.apiserver.util.QueryMapper;
import iudx.catalogue.server.apiserver.util.RespBuilder;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.geocoding.GeocodingService;
import iudx.catalogue.server.nlpsearch.NLPSearchService;
import iudx.catalogue.server.util.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public final class SearchApis {


  private DatabaseService dbService;
  private GeocodingService geoService;
  private NLPSearchService nlpService;
  private Api api;

  private static final Logger LOGGER = LogManager.getLogger(SearchApis.class);

  public SearchApis(Api api) {
    this.api = api;
  }

  /**
   * Sets the database service, geocoding service, and NLP search service for this class.
   *
   * @param dbService the database service to be set
   * @param  geoService the geocoding service to be set
   * @param nlpService the NLPService to be set
   */
  public void setService(DatabaseService dbService,
                         GeocodingService geoService, NLPSearchService nlpService) {
    this.dbService = dbService;
    this.geoService = geoService;
    this.nlpService = nlpService;
  }

  public void setDbService(DatabaseService dbService) {
    this.dbService = dbService;
  }

  public void setGeoService(GeocodingService geoService) {
    this.geoService = geoService;
  }

  public void setNlpService(NLPSearchService nlpService) {
    this.nlpService = nlpService;
  }


  /**
   * Processes the attribute, geoSpatial, and text search requests and returns the results from the
   * database.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void searchHandler(RoutingContext routingContext) {

    final String path =  routingContext.normalisedPath();

    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceId = request.getHeader(HEADER_INSTANCE);

    MultiMap queryParameters = routingContext.queryParams();

    LOGGER.debug("Info: routed to search/count");
    LOGGER.debug("Info: instance;" + instanceId);

    /* validating proper actual query parameters from request */
    if ((request.getParam(PROPERTY) == null || request.getParam(VALUE) == null)
        && (request.getParam(GEOPROPERTY) == null
            || request.getParam(GEORELATION) == null
            || request.getParam(GEOMETRY) == null
            || request.getParam(COORDINATES) == null)
        && request
            .getParam(Q_VALUE) == null) {

      LOGGER.error("Fail: Invalid Syntax");
      response.setStatusCode(400)
          .end(new RespBuilder()
                  .withType(TYPE_INVALID_SYNTAX)
                  .withTitle(TITLE_INVALID_SYNTAX)
                  .withDetail("Mandatory field(s) not provided")
                  .getResponse());
      return;

      /* checking the values of the query parameters */
    } else if (request.getParam(PROPERTY) != null
        && !request.getParam(PROPERTY).isBlank()) {

      /* converting query parameters in json */
      requestBody = QueryMapper.map2Json(queryParameters);

      /* checking the values of the query parameters for geo related count */
    } else if (GEOMETRIES.contains(request.getParam(GEOMETRY))
        && GEORELS.contains(request.getParam(GEORELATION))
        && request.getParam(GEOPROPERTY).equals(GEO_PROPERTY)) {
      requestBody = QueryMapper.map2Json(queryParameters);
    } else if (request.getParam(Q_VALUE) != null
        && !request.getParam(Q_VALUE).isBlank()) {
      /* checking the values of the query parameters */

      requestBody = QueryMapper.map2Json(queryParameters);

    } else {
      response.setStatusCode(400)
                  .end(new RespBuilder()
                              .withType(TYPE_INVALID_GEO_VALUE)
                              .withTitle(TITLE_INVALID_GEO_VALUE)
                              .withDetail(TITLE_INVALID_QUERY_PARAM_VALUE)
                              .getResponse());
      return;
    }

    if (requestBody != null) {
      requestBody.put(HEADER_INSTANCE, instanceId);

      JsonObject resp = QueryMapper.validateQueryParam(requestBody);
      if (resp.getString(STATUS).equals(SUCCESS)) {

        if (path.equals(api.getRouteSearch())) {
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
      } else {
        LOGGER.error("Fail: Search/Count; Invalid request query parameters");
        LOGGER.debug(resp);
        response.setStatusCode(400)
                .end(resp.toString());
      }
    } else {
      LOGGER.error("Fail: Search/Count; Invalid request query parameters");
      response.setStatusCode(400)
          .end(new RespBuilder()
                    .withType(TYPE_INVALID_SYNTAX)
                    .withTitle(TITLE_INVALID_SYNTAX)
                    .withDetail(TITLE_INVALID_QUERY_PARAM_VALUE)
                    .getResponse());
    }

  }

  /**
   * Handles the NLP search request from the client and responds with
   * a JSON array of search results.
   * @param routingContext the routing context of the request
   */
  public void nlpSearchHandler(RoutingContext routingContext) {
    String query = "";
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
    JsonArray embeddings = new JsonArray();
    try {
      if (routingContext.queryParams().contains("q")) {
        query = routingContext.queryParams().get("q");
      }
    } catch (Exception e) {
      LOGGER.info("Missing query parameter");
      RespBuilder respBuilder =
          new RespBuilder().withType(TYPE_MISSING_PARAMS)
                  .withTitle(TITLE_MISSING_PARAMS)
                      .withDetail("Query param q is missing");

      routingContext.response().setStatusCode(400).end(respBuilder.getResponse());
      return;
    }
    
    nlpService.search(query, res -> {
      if (res.succeeded()) {
        JsonArray result = res.result().getJsonArray("result");
        embeddings.add(result);
        String location = res.result().getString("location");
        if (location.equals("EMPTY")) {
          dbService.nlpSearchQuery(embeddings, handler -> {
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
              response.setStatusCode(400)
                            .end(handler.cause().getMessage());
            }
          });
        } else {
          geoService.geocoder(location, ar -> {
            if (ar.succeeded()) {
              JsonObject results = new JsonObject(ar.result());
              LOGGER.debug("Info: geocoding result - " + results);
              dbService.nlpSearchLocationQuery(embeddings, results, handler -> {
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
                  response.setStatusCode(400)
                              .end(handler.cause().getMessage());
                }
              });
            } else {
              LOGGER.info("Failed to get bounding box");
              routingContext.response().setStatusCode(404)
                      .end(ar.cause().getMessage());
            }
          });
        }
      } else {
        LOGGER.info("Failed to get embeddings");
        routingContext.response().setStatusCode(400).end();
      }
    }); 
  }
}
