/**
 * <h1>SearchController.java</h1>
 *
 * <p>Callback handlers for CRUD</p>
 */

package iudx.catalogue.server.apiserver;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.common.util.ResponseBuilderUtil.*;
import static iudx.catalogue.server.database.elastic.util.Constants.*;
import static iudx.catalogue.server.geocoding.util.Constants.*;
import static iudx.catalogue.server.geocoding.util.Constants.BBOX;
import static iudx.catalogue.server.geocoding.util.Constants.COORDINATES;
import static iudx.catalogue.server.geocoding.util.Constants.GEOMETRY;
import static iudx.catalogue.server.geocoding.util.Constants.TYPE;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.util.Constants.RESULTS;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.apiserver.util.QueryMapper;
import iudx.catalogue.server.apiserver.util.RespBuilder;
import iudx.catalogue.server.common.util.DbResponseMessageBuilder;
import iudx.catalogue.server.database.elastic.model.ElasticsearchResponse;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
import iudx.catalogue.server.database.elastic.util.QueryDecoder;
import iudx.catalogue.server.database.elastic.util.QueryType;
import iudx.catalogue.server.exceptions.FailureHandler;
import iudx.catalogue.server.geocoding.service.GeocodingService;
import iudx.catalogue.server.nlpsearch.service.NLPSearchService;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class SearchController {
  private static final Logger LOGGER = LogManager.getLogger(SearchController.class);
  private final QueryDecoder queryDecoder = new QueryDecoder();
  private final ElasticsearchService esService;
  private final GeocodingService geoService;
  private final NLPSearchService nlpService;
  private final FailureHandler failureHandler;
  private final String dxApiBasePath;
  private final String docIndex;

  public SearchController(
      ElasticsearchService esService,
      GeocodingService geoService,
      NLPSearchService nlpService,
      FailureHandler failureHandler,
      String dxApiBasePath,
      String docIndex) {
    this.esService = esService;
    this.geoService = geoService;
    this.nlpService = nlpService;
    this.failureHandler = failureHandler;
    this.dxApiBasePath = dxApiBasePath;
    this.docIndex = docIndex;
  }

  // Routes for search and count
  public Router init(Router router) {

    /* Search for an item */
    router
        .get(ROUTE_SEARCH)
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(failureHandler)
        .handler(this::searchHandler);

    /* NLP Search */
    router
        .get(ROUTE_NLP_SEARCH)
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(failureHandler)
        .handler(this::nlpSearchHandler);

    /* Count the Cataloque server items */
    router
        .get(ROUTE_COUNT)
        .produces(MIME_APPLICATION_JSON)
        .failureHandler(failureHandler)
        .handler(this::searchHandler);
    return router;
  }

  /**
   * Processes the attribute, geoSpatial, and text search requests and returns the results from the
   * database.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void searchHandler(RoutingContext routingContext) {

    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    HttpServerRequest request = routingContext.request();
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
        && request.getParam(Q_VALUE) == null) {

      LOGGER.error("Fail: Invalid Syntax");
      response
          .setStatusCode(400)
          .end(
              new RespBuilder()
                  .withType(TYPE_INVALID_SYNTAX)
                  .withTitle(TITLE_INVALID_SYNTAX)
                  .withDetail("Mandatory field(s) not provided")
                  .getResponse());
      return;

      /* checking the values of the query parameters */
    } else if (request.getParam(PROPERTY) != null && !request.getParam(PROPERTY).isBlank()) {

      /* converting query parameters in json */
      requestBody = QueryMapper.map2Json(queryParameters);

      /* checking the values of the query parameters for geo related count */
    } else if (GEOMETRIES.contains(request.getParam(GEOMETRY))
        && GEORELS.contains(request.getParam(GEORELATION))
        && request.getParam(GEOPROPERTY).equals(GEO_PROPERTY)) {
      requestBody = QueryMapper.map2Json(queryParameters);
    } else if (request.getParam(Q_VALUE) != null && !request.getParam(Q_VALUE).isBlank()) {
      /* checking the values of the query parameters */

      requestBody = QueryMapper.map2Json(queryParameters);

    } else {
      response
          .setStatusCode(400)
          .end(
              new RespBuilder()
                  .withType(TYPE_INVALID_GEO_VALUE)
                  .withTitle(TITLE_INVALID_GEO_VALUE)
                  .withDetail(TITLE_INVALID_QUERY_PARAM_VALUE)
                  .getResponse());
      return;
    }

    if (requestBody != null) {
      requestBody.put(HEADER_INSTANCE, instanceId);

      JsonObject resp = QueryMapper.validateQueryParam(requestBody);
      String path = routingContext.normalisedPath();
      if (resp.getString(STATUS).equals(SUCCESS)) {
        if (path.equals(dxApiBasePath + ROUTE_SEARCH)) {
          searchQuery(requestBody)
              .onComplete(
                  handler -> {
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
          countQuery(requestBody)
              .onComplete(
                  handler -> {
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
                      response.setStatusCode(400).end(handler.cause().getMessage());
                    }
                  });
        }
      } else {
        LOGGER.error("Fail: Search/Count; Invalid request query parameters");
        LOGGER.debug(resp);
        response.setStatusCode(400).end(resp.toString());
      }
    } else {
      LOGGER.error("Fail: Search/Count; Invalid request query parameters");
      response
          .setStatusCode(400)
          .end(
              new RespBuilder()
                  .withType(TYPE_INVALID_SYNTAX)
                  .withTitle(TITLE_INVALID_SYNTAX)
                  .withDetail(TITLE_INVALID_QUERY_PARAM_VALUE)
                  .getResponse());
    }
  }

  public Future<JsonObject> searchQuery(JsonObject request) {

    LOGGER.debug("Info: searchQuery");

    Promise<JsonObject> promise = Promise.promise();
    request.put(SEARCH, true);

    /* Validate the Request */
    if (!request.containsKey(SEARCH_TYPE)) {
      promise.fail(invalidSyntaxResponse(NO_SEARCH_TYPE_FOUND));
      return promise.future();
    }
    /* Construct the query to be made */
    JsonObject query = queryDecoder.searchQuery(request);
    if (query.containsKey(ERROR)) {

      LOGGER.error("Fail: Query returned with an error");
      promise.fail(query.getJsonObject(ERROR).toString());
      return promise.future();
    }

    QueryModel queryModel = (QueryModel) query.getValue(QUERY_KEY);
    LOGGER.debug("Info: Query constructed;" + queryModel.getQueries().getQueryType());

    esService
        .search(docIndex, queryModel)
        .onComplete(
            searchRes -> {
              if (searchRes.succeeded()) {
                LOGGER.debug("Success: Successful DB request");
                List<ElasticsearchResponse> response = searchRes.result();
                DbResponseMessageBuilder responseMsg = new DbResponseMessageBuilder();
                responseMsg.statusSuccess().setTotalHits(response.size());
                response.stream()
                    .map(ElasticsearchResponse::getSource)
                    .peek(
                        source -> {
                          source.remove(SUMMARY_KEY);
                          source.remove(WORD_VECTOR_KEY);
                        })
                    .forEach(responseMsg::addResult);

                promise.complete(responseMsg.getResponse());
              } else {
                LOGGER.error("Fail: DB Request;" + searchRes.cause().getMessage());
                promise.fail(internalErrorResp());
              }
            });
    return promise.future();
  }

  public Future<JsonObject> countQuery(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    request.put(SEARCH, false);

    /* Validate the Request */
    if (!request.containsKey(SEARCH_TYPE)) {
      promise.fail(internalErrorResp());
      return promise.future();
    }

    /* Construct the query to be made */
    JsonObject query = queryDecoder.searchQuery(request);
    if (query.containsKey(ERROR)) {

      LOGGER.error("Fail: Query returned with an error");

      promise.fail(query.getJsonObject(ERROR).toString());
      return promise.future();
    }

    QueryModel queryModel = (QueryModel) query.getValue(QUERY_KEY);
    LOGGER.debug("Info: Query constructed;" + queryModel.toString());

    esService
        .count(docIndex, queryModel)
        .onComplete(
            searchRes -> {
              if (searchRes.succeeded()) {
                LOGGER.debug("Success: Successful DB request");
                DbResponseMessageBuilder responseMsg = new DbResponseMessageBuilder();
                responseMsg.statusSuccess().setTotalHits(searchRes.result());
                promise.complete(responseMsg.getResponse());
              } else {
                LOGGER.error("Fail: DB Request;" + searchRes.cause().getMessage());
                promise.fail(internalErrorResp());
              }
            });
    return promise.future();
  }

  /**
   * Handles the NLP search request from the client and responds with a JSON array of search
   * results.
   *
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
          new RespBuilder()
              .withType(TYPE_MISSING_PARAMS)
              .withTitle(TITLE_MISSING_PARAMS)
              .withDetail("Query param q is missing");

      routingContext.response().setStatusCode(400).end(respBuilder.getResponse());
      return;
    }

    nlpService
        .search(query)
        .onComplete(
            res -> {
              if (res.succeeded()) {
                JsonArray result = res.result().getJsonArray("result");
                embeddings.add(result);
                String location = res.result().getString("location");
                if (location.equals("EMPTY")) {
                  nlpSearchQuery(embeddings)
                      .onComplete(
                          handler -> {
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
                  geoService
                      .geocoder(location)
                      .onComplete(
                          ar -> {
                            if (ar.succeeded()) {
                              JsonObject results = new JsonObject(ar.result());
                              LOGGER.debug("Info: geocoding result - " + results);
                              nlpSearchLocationQuery(embeddings, results)
                                  .onComplete(
                                      handler -> {
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
                                          LOGGER.error(
                                              "Fail: Search;" + handler.cause().getMessage());
                                          response
                                              .setStatusCode(400)
                                              .end(handler.cause().getMessage());
                                        }
                                      });
                            } else {
                              LOGGER.info("Failed to get bounding box");
                              routingContext
                                  .response()
                                  .setStatusCode(404)
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

  /**
   * Executes an NLP search query by passing in the request embeddings and invoking the appropriate
   * search method on the ElasticSearch client.
   *
   * @param request the request embeddings
   * @return Future<{@link io.vertx.core.json.JsonObject}>
   *     that completes with the search results when the Elasticsearch client successfully
   *     processes the query, or fails with an error response if the request fails.
   */
  public Future<JsonObject> nlpSearchQuery(JsonArray request) {
    Promise<JsonObject> promise = Promise.promise();
    JsonArray embeddings = request.getJsonArray(0);
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(
        new QueryModel(QueryType.SCRIPT_SCORE, Map.of("query_vector", embeddings)));
    queryModel.setExcludeFields(List.of("_word_vector"));
    esService
        .search(docIndex, queryModel)
        .onComplete(
            searchRes -> {
              if (searchRes.succeeded()) {
                LOGGER.debug("Success:Successful DB request");
                List<ElasticsearchResponse> response = searchRes.result();
                DbResponseMessageBuilder responseMsg = new DbResponseMessageBuilder();
                responseMsg.statusSuccess().setTotalHits(response.size());
                response.stream()
                    .map(ElasticsearchResponse::getSource)
                    .peek(
                        source -> {
                          source.remove(SUMMARY_KEY);
                          source.remove(WORD_VECTOR_KEY);
                        })
                    .forEach(responseMsg::addResult);

                promise.complete(responseMsg.getResponse());
              } else {
                LOGGER.error("Fail: DB request;" + searchRes.cause().getMessage());
                promise.fail(internalErrorResp());
              }
            });
    return promise.future();
  }

  public Future<JsonObject> nlpSearchLocationQuery(JsonArray request, JsonObject queryParams) {
    Promise<JsonObject> promise = Promise.promise();
    JsonArray embeddings = request.getJsonArray(0);
    JsonArray params = queryParams.getJsonArray(RESULTS);

    List<Future> futures =
        params.stream()
            .map(
                param -> {
                  try {
                    return scriptLocationSearch(embeddings, (JsonObject) param);
                  } catch (Exception e) {
                    LOGGER.error("Error during scriptLocationSearch for param: " + param, e);
                    return Future.failedFuture(
                        e); // returns a Future<JsonObject> in case of an error
                  }
                })
            .collect(Collectors.toList());

    // When all futures return, respond back with the result object in the response
    CompositeFuture.all(futures)
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                JsonArray results = new JsonArray();
                ar.result()
                    .list()
                    .forEach(
                        h -> {
                          JsonArray hr = ((JsonObject) h).getJsonArray(RESULTS);
                          if (hr != null && !hr.isEmpty()) {
                            hr.stream().forEach(results::add);
                          }
                        });
                if (results.isEmpty()) {
                  promise.complete(itemNotFoundJsonResp("NLP Search Failed"));
                } else {
                  promise.complete(successResponse(results));
                }
              } else {
                LOGGER.error("Failed to process NLP search", ar.cause());
                promise.fail(ar.cause());
              }
            });

    return promise.future();
  }

  private Future<JsonObject> scriptLocationSearch(JsonArray embeddings, JsonObject param) {
    Promise<JsonObject> promise = Promise.promise();
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(generateGeoScriptScoreQuery(param, embeddings));
    queryModel.setExcludeFields(List.of("_word_vector"));
    esService
        .search(docIndex, queryModel)
        .onSuccess(
            searchRes -> {
              LOGGER.debug("Success: Successful DB request");
              DbResponseMessageBuilder responseMsg = new DbResponseMessageBuilder();
              responseMsg.statusSuccess().setTotalHits(searchRes.size());
              searchRes.stream()
                  .map(ElasticsearchResponse::getSource)
                  .peek(
                      source -> {
                        source.remove(SUMMARY_KEY);
                        source.remove(WORD_VECTOR_KEY);
                      })
                  .forEach(responseMsg::addResult);
              promise.complete(responseMsg.getResponse());
            })
        .onFailure(
            throwable -> {
              LOGGER.debug("Fail: {}", throwable.getLocalizedMessage());
              promise.fail(internalErrorResp());
            });
    return promise.future();
  }

  public QueryModel generateGeoScriptScoreQuery(JsonObject queryParams, JsonArray queryVector) {

    QueryModel boolQueryModel = new QueryModel(QueryType.BOOL);

    // Adding MatchQuery clauses for each of the fields based on the queryParams
    if (queryParams.containsKey(BOROUGH)) {
      boolQueryModel.addShouldQuery(
          new QueryModel(
              QueryType.MATCH,
              Map.of(
                  FIELD,
                  "_geosummary._geocoded.results.borough",
                  VALUE,
                  queryParams.getString(BOROUGH))));
    }
    if (queryParams.containsKey(LOCALITY)) {
      boolQueryModel.addShouldQuery(
          new QueryModel(
              QueryType.MATCH,
              Map.of(
                  FIELD,
                  "_geosummary._geocoded.results.locality",
                  VALUE,
                  queryParams.getString(LOCALITY))));
    }
    if (queryParams.containsKey(COUNTY)) {
      boolQueryModel.addShouldQuery(
          new QueryModel(
              QueryType.MATCH,
              Map.of(
                  FIELD,
                  "_geosummary._geocoded.results.county",
                  VALUE,
                  queryParams.getString(COUNTY))));
    }
    if (queryParams.containsKey(REGION)) {
      boolQueryModel.addShouldQuery(
          new QueryModel(
              QueryType.MATCH,
              Map.of(
                  FIELD,
                  "_geosummary._geocoded.results.region",
                  VALUE,
                  queryParams.getString(REGION))));
    }
    if (queryParams.containsKey(COUNTRY)) {
      boolQueryModel.addShouldQuery(
          new QueryModel(
              QueryType.MATCH,
              Map.of(
                  FIELD,
                  "_geosummary._geocoded.results.country",
                  VALUE,
                  queryParams.getString(COUNTRY))));
    }

    // Set minimum_should_match to 1
    boolQueryModel.setMinimumShouldMatch("1");

    // Geo shape filter
    if (queryParams.containsKey(BBOX)) {
      JsonArray bboxCoords = queryParams.getJsonArray(BBOX);
      JsonArray coordinates =
          new JsonArray()
              .add(
                  new JsonArray()
                      .add(bboxCoords.getFloat(0)) // minLon
                      .add(bboxCoords.getFloat(3))) // maxLat
              .add(
                  new JsonArray()
                      .add(bboxCoords.getFloat(2)) // maxLon
                      .add(bboxCoords.getFloat(1))); // minLat
      boolQueryModel.addFilterQuery(
          new QueryModel(
              QueryType.GEO_SHAPE,
              Map.of(
                  GEOPROPERTY,
                  "location.geometry",
                  TYPE,
                  GEO_BBOX,
                  COORDINATES,
                  coordinates,
                  "relation",
                  INTERSECTS)));
    }

    // Script score for cosine similarity
    return new QueryModel(
        QueryType.SCRIPT_SCORE,
        Map.of("query_vector", queryVector, "custom_query", boolQueryModel.toJson()));
  }
}
