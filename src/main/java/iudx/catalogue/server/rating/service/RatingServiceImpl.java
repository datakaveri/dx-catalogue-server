package iudx.catalogue.server.rating.service;

import static iudx.catalogue.server.auditing.util.Constants.ID;
import static iudx.catalogue.server.common.util.ResponseBuilderUtil.*;
import static iudx.catalogue.server.database.elastic.model.ElasticsearchResponse.getAggregations;
import static iudx.catalogue.server.database.elastic.util.AggregationType.AVG;
import static iudx.catalogue.server.database.elastic.util.Constants.DOC_COUNT;
import static iudx.catalogue.server.database.elastic.util.Constants.ID_KEYWORD;
import static iudx.catalogue.server.database.elastic.util.Constants.KEY;
import static iudx.catalogue.server.database.elastic.util.QueryType.MATCH;
import static iudx.catalogue.server.geocoding.util.Constants.RESULTS;
import static iudx.catalogue.server.geocoding.util.Constants.TYPE;
import static iudx.catalogue.server.rating.util.Constants.APPROVED;
import static iudx.catalogue.server.rating.util.Constants.AUDIT_INFO_QUERY;
import static iudx.catalogue.server.rating.util.Constants.RATING_ID;
import static iudx.catalogue.server.rating.util.Constants.STATUS;
import static iudx.catalogue.server.rating.util.Constants.USER_ID;
import static iudx.catalogue.server.util.Constants.AVERAGE_RATING;
import static iudx.catalogue.server.util.Constants.BUCKETS;
import static iudx.catalogue.server.util.Constants.DELETE;
import static iudx.catalogue.server.util.Constants.FIELD;
import static iudx.catalogue.server.util.Constants.FILTER_PAGINATION_FROM;
import static iudx.catalogue.server.util.Constants.KEYWORD_KEY;
import static iudx.catalogue.server.util.Constants.MAX_LIMIT;
import static iudx.catalogue.server.util.Constants.TITLE_REQUIREMENTS_NOT_MET;
import static iudx.catalogue.server.util.Constants.TOTAL_RATINGS;
import static iudx.catalogue.server.util.Constants.TYPE_ACCESS_DENIED;
import static iudx.catalogue.server.util.Constants.UPDATE;
import static iudx.catalogue.server.util.Constants.VALUE;

import com.google.common.hash.Hashing;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.apiserver.util.RespBuilder;
import iudx.catalogue.server.common.util.DbResponseMessageBuilder;
import iudx.catalogue.server.database.elastic.model.ElasticsearchResponse;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
import iudx.catalogue.server.database.elastic.util.AggregationType;
import iudx.catalogue.server.database.elastic.util.BoolOperator;
import iudx.catalogue.server.database.postgres.service.PostgresService;
import iudx.catalogue.server.databroker.model.QueryObject;
import iudx.catalogue.server.databroker.service.RabbitMQService;
import iudx.catalogue.server.util.Constants;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RatingServiceImpl implements RatingService {
  private static final Logger LOGGER = LogManager.getLogger(RatingServiceImpl.class);
  private final String rsauditingtable;
  private final int minReadNumber;
  private final String ratingIndex;
  private final String docIndex;
  private final String ratingExchangeName;
  ElasticsearchService esService;
  RabbitMQService rmqService;
  PostgresService postgresService;

  /**
   * Constructor for RatingServiceImpl class. Initializes the object with the given parameters.
   *
   * @param exchangeName the name of the exchange used for rating
   * @param rsauditingtable the name of the table used for auditing the rating system
   * @param minReadNumber the minimum number of reads for a rating to be considered valid
   * @param ratingIndex the index of the rating docs
   * @param elasticsearchService the service used for interacting with the database
   * @param rmqService the service used for interacting with the data broker
   * @param postgresService the service used for interacting with the PostgreSQL database
   */
  public RatingServiceImpl(
      String exchangeName,
      String rsauditingtable,
      int minReadNumber,
      String ratingIndex,
      ElasticsearchService elasticsearchService,
      String docIndex,
      RabbitMQService rmqService,
      PostgresService postgresService) {
    this.ratingExchangeName = exchangeName;
    this.rsauditingtable = rsauditingtable;
    this.minReadNumber = minReadNumber;
    this.ratingIndex = ratingIndex;
    this.esService = elasticsearchService;
    this.docIndex = docIndex;
    this.rmqService = rmqService;
    this.postgresService = postgresService;
  }

  private static QueryModel getQueryModel(String field, String value) {
    Map<String, Object> ratingIdParams = new HashMap<>();
    ratingIdParams.put(FIELD, field);
    ratingIdParams.put(VALUE, value);
    QueryModel ratingIdMatchQuery = new QueryModel(MATCH, ratingIdParams);
    ratingIdMatchQuery.setLimit(MAX_LIMIT);
    ratingIdMatchQuery.setOffset(FILTER_PAGINATION_FROM);

    Map<String, Object> statusParams = new HashMap<>();
    statusParams.put(FIELD, STATUS);
    statusParams.put(VALUE, APPROVED);
    QueryModel statusMatchQuery = new QueryModel(MATCH, statusParams);

    // Construct the Bool query with must and must_not clauses
    QueryModel queryModel =
        new QueryModel(List.of(ratingIdMatchQuery, statusMatchQuery), null, null, null);
    queryModel.setIncludeFields(List.of(ID, "rating"));
    return queryModel;
  }

  @Override
  public Future<JsonObject> createRating(JsonObject ratingDoc) {
    Promise<JsonObject> promise = Promise.promise();

    String sub = ratingDoc.getString(USER_ID);
    String id = ratingDoc.getString(ID);
    StringBuilder query =
        new StringBuilder(
            AUDIT_INFO_QUERY.replace("$1", rsauditingtable).replace("$2", sub).replace("$3", id));
    Future<JsonObject> getRsAuditingInfo = getAuditingInfo(query);

    getRsAuditingInfo
        .onSuccess(
            successHandler -> {
              int countResourceAccess = successHandler.getInteger("totalHits");
              if (countResourceAccess > minReadNumber) {

                String ratingId =
                    Hashing.sha256().hashString(sub + id, StandardCharsets.UTF_8).toString();

                ratingDoc.put(RATING_ID, ratingId);

                QueryModel queryModel = new QueryModel();
                queryModel.setQueries(getQueryModel("ratingID" + ".keyword", ratingId));

                esService
                    .search(ratingIndex, queryModel)
                    .onComplete(
                        checkRes -> {
                          if (checkRes.failed()) {
                            LOGGER.error("Fail: Insertion of rating failed: " + checkRes.cause());
                            promise.fail(failureResp(ratingId));
                          } else {
                            if (!checkRes.result().isEmpty()) {
                              promise.fail(
                                  itemAlreadyExistsResponse(ratingId, " Fail: Doc Already Exists"));
                              return;
                            }
                            esService
                                .createDocument(ratingIndex, ratingDoc)
                                .onComplete(
                                    postRes -> {
                                      if (postRes.failed()) {
                                        LOGGER.error("Fail: Insertion failed" + postRes.cause());
                                        promise.fail(failureResponse(ratingId));
                                      } else {
                                        LOGGER.info("Success: Rating Recorded");
                                        promise.complete(successResponse(ratingId));
                                      }
                                    });
                          }
                        });
              } else {
                LOGGER.error("Fail: Rating creation failed");
                promise.fail(
                    new RespBuilder()
                        .withType(TYPE_ACCESS_DENIED)
                        .withTitle(TITLE_REQUIREMENTS_NOT_MET)
                        .withDetail(
                            "User has to access resource at least "
                                + minReadNumber
                                + " times to give rating")
                        .getResponse());
              }
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error(
                  "User has not accessed resource"
                      + " before and hence is not authorised to give rating");
              promise.fail(failureHandler.getMessage());
            });

    return promise.future();
  }

  @Override
  public Future<JsonObject> getRating(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();

    String id = request.getString(ID);
    if (!request.containsKey(TYPE)) {
      String sub = request.getString(USER_ID);
      String ratingId = Hashing.sha256().hashString(sub + id, StandardCharsets.UTF_8).toString();

      request.put(RATING_ID, ratingId);
    }

    QueryModel queryModel = new QueryModel();
    if (request.containsKey(RATING_ID)) {
      String ratingId = request.getString(RATING_ID);
      queryModel.setQueries(getQueryModel("ratingID.keyword", ratingId));
      queryModel.setIncludeFields(List.of(ID, "rating"));
    } else {
      if (request.containsKey(TYPE) && request.getString(TYPE).equalsIgnoreCase("average")) {
        Future<List<String>> getAssociatedIdFuture = getAssociatedIDs(id);
        getAssociatedIdFuture.onComplete(
            ids -> {
              if (ids.succeeded()) {
                QueryModel avgRatingQuery = getAverageRatingQueryModel(ids.result());
                esService
                    .search(ratingIndex, avgRatingQuery)
                    .onComplete(
                        getRes -> {
                          if (getRes.succeeded()) {
                            LOGGER.debug("Success: Successful DB request");
                            DbResponseMessageBuilder responseMsg = new DbResponseMessageBuilder();
                            responseMsg.statusSuccess().setTotalHits(getRes.result().size());
                            try {
                              JsonArray globalAggregations =
                                  getAggregations()
                                      .getJsonObject(Constants.RESULTS)
                                      .getJsonArray(BUCKETS);
                              // Process the global aggregations using streams
                              globalAggregations.stream()
                                  .filter(
                                      aggregation ->
                                          aggregation
                                              instanceof JsonObject) // Ensure it's a JsonObject
                                  .map(
                                      aggregation -> {
                                        JsonObject aggObj = (JsonObject) aggregation;

                                        // Extract the necessary fields from the aggregation
                                        String key = aggObj.getString(KEY);
                                        String totalRatings = aggObj.getString(DOC_COUNT);
                                        double averageRating =
                                            aggObj.getJsonObject(AVERAGE_RATING).getDouble(VALUE);

                                        // Create and return a new JsonObject with the extracted
                                        // fields
                                        return new JsonObject()
                                            .put(ID, key)
                                            .put(TOTAL_RATINGS, totalRatings)
                                            .put(AVERAGE_RATING, averageRating);
                                      })
                                  .forEach(
                                      responseMsg
                                          ::addResult); // Add each result to the response message

                              // Complete the promise
                              promise.complete(responseMsg.getResponse());

                            } catch (Exception e) {
                              LOGGER.error(
                                  "Error processing global aggregations: " + e.getMessage(), e);
                              promise.fail(internalErrorResp());
                            }

                          } else {
                            LOGGER.error("Fail: failed getting average rating: " + getRes.cause());
                            promise.fail(internalErrorResp());
                          }
                        });
              } else {
                promise.fail(internalErrorResp());
              }
            });

        return promise.future();
      } else {
        queryModel.setQueries(getQueryModel(ID_KEYWORD, id));
        queryModel.setIncludeFields(List.of(ID, "rating"));
      }
    }

    esService
        .search(ratingIndex, queryModel)
        .onComplete(
            getRes -> {
              if (getRes.succeeded()) {
                LOGGER.debug("Success: Successful DB request");
                List<ElasticsearchResponse> responseList = getRes.result();
                DbResponseMessageBuilder responseMsg = new DbResponseMessageBuilder();
                responseMsg.statusSuccess();
                if (!request.containsKey(RATING_ID)) {
                  responseMsg.setTotalHits(responseList.size());
                }
                responseList.stream()
                    .map(ElasticsearchResponse::getSource)
                    .forEach(responseMsg::addResult);
                promise.complete(responseMsg.getResponse());
              } else {
                LOGGER.error("Fail: failed getting rating: " + getRes.cause());
                promise.fail(internalErrorResp());
              }
            });
    return promise.future();
  }

  private QueryModel getAverageRatingQueryModel(List<String> ids) {

    List<QueryModel> shouldQueries =
        ids.stream()
            .map(id -> new QueryModel(MATCH, Map.of(FIELD, ID_KEYWORD, VALUE, id)))
            .collect(Collectors.toList());

    List<QueryModel> mustQueries =
        List.of(new QueryModel(MATCH, Map.of(FIELD, STATUS, VALUE, APPROVED)));

    QueryModel queryModel = new QueryModel(mustQueries, shouldQueries, null, null);
    queryModel.setMinimumShouldMatch("1");

    Map<String, Object> avgAggParams = new HashMap<>();
    avgAggParams.put(FIELD, "rating");

    // Create the "average_rating" nested aggregation
    QueryModel avgRatingAgg = new QueryModel(AVG, avgAggParams);

    // Create the outer "results" aggregation with the "terms" aggregation and nested
    // "average_rating" aggregation
    Map<String, Object> termsAggParams = new HashMap<>();
    termsAggParams.put(FIELD, ID_KEYWORD);

    QueryModel termsAgg = new QueryModel(AggregationType.TERMS, termsAggParams);
    termsAgg.setAggregationName(RESULTS);
    termsAgg.setAggregationsMap(Map.of("average_rating", avgRatingAgg));
    return new QueryModel(queryModel, List.of(termsAgg));
  }

  private Future<List<String>> getAssociatedIDs(String id) {
    QueryModel idMatch = new QueryModel(MATCH, Map.of(FIELD, ID_KEYWORD, VALUE, id));
    QueryModel resourceGroupMatch =
        new QueryModel(MATCH, Map.of(FIELD, "resourceGroup" + KEYWORD_KEY, VALUE, id));
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(
        new QueryModel(BoolOperator.SHOULD, List.of(idMatch, resourceGroupMatch)));
    queryModel.setMinimumShouldMatch("1");
    queryModel.setIncludeFields(List.of(ID));

    Promise<List<String>> promise = Promise.promise();
    esService
        .search(docIndex, queryModel)
        .onComplete(
            res -> {
              if (res.succeeded()) {
                List<String> idCollector =
                    res.result().stream()
                        .map(ElasticsearchResponse::getSource)
                        .map(d -> d.getString(ID))
                        .collect(Collectors.toList());
                promise.complete(idCollector);
              } else {
                LOGGER.error("Fail: Get average rating failed");
                promise.fail("Fail: Get average rating failed");
              }
            });
    return promise.future();
  }

  @Override
  public Future<JsonObject> updateRating(JsonObject ratingDoc) {
    Promise<JsonObject> promise = Promise.promise();

    String sub = ratingDoc.getString(USER_ID);
    String id = ratingDoc.getString(ID);

    String ratingId = Hashing.sha256().hashString(sub + id, StandardCharsets.UTF_8).toString();

    ratingDoc.put(RATING_ID, ratingId);
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(getQueryModel("ratingID.keyword", ratingId));
    // TODO: Filter ID only path
    esService
        .search(ratingIndex, queryModel)
        .onComplete(
            checkRes -> {
              if (checkRes.failed()) {
                LOGGER.error("Fail: Check query fail;" + checkRes.cause());
                promise.fail(internalErrorResp());
              } else {
                if (checkRes.result().size() != 1) {
                  LOGGER.error("Fail: Doc doesn't exist, can't update");
                  promise.fail(
                      itemNotFoundResponse(
                          ratingId, UPDATE, "Fail: Doc doesn't exist, can't update"));
                  return;
                }
                String docId = checkRes.result().get(0).getDocId();
                esService
                    .updateDocument(ratingIndex, docId, ratingDoc)
                    .onComplete(
                        putRes -> {
                          if (putRes.failed()) {
                            promise.fail(internalErrorResp());
                            LOGGER.error("Fail: Updation failed;" + putRes.cause());
                          } else {
                            promise.complete(ratingSuccessResponse(ratingId));
                          }
                        });
              }
            });
    return promise.future();
  }

  @Override
  public Future<JsonObject> deleteRating(JsonObject ratingDoc) {
    Promise<JsonObject> promise = Promise.promise();

    String sub = ratingDoc.getString(USER_ID);
    String id = ratingDoc.getString(ID);

    String ratingId = Hashing.sha256().hashString(sub + id, StandardCharsets.UTF_8).toString();

    ratingDoc.put(RATING_ID, ratingId);

    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(getQueryModel("ratingID.keyword", ratingId));
    // TODO: Filter ID only path
    esService
        .search(ratingIndex, queryModel)
        .onComplete(
            checkRes -> {
              if (checkRes.failed()) {
                LOGGER.error("Fail: Check query fail;" + checkRes.cause());
                promise.fail(internalErrorResp());
              } else {
                if (checkRes.result().size() != 1) {
                  LOGGER.error("Fail: Doc doesn't exist, can't delete");
                  promise.fail(
                      itemNotFoundResponse(
                          ratingId, DELETE, "Fail: Doc doesn't exist, can't delete"));
                  return;
                }
                String docId = checkRes.result().get(0).getDocId();
                esService
                    .deleteDocument(ratingIndex, docId)
                    .onComplete(
                        delRes -> {
                          if (delRes.succeeded()) {
                            promise.complete(ratingSuccessResponse(ratingId));
                          } else {
                            promise.fail(internalErrorResp());
                            LOGGER.error("Fail: Deletion failed;" + delRes.cause());
                          }
                        });
              }
            });
    return promise.future();
  }

  public Future<JsonObject> getAuditingInfo(StringBuilder query) {
    Promise<JsonObject> promise = Promise.promise();
    postgresService
        .executeCountQuery(query.toString())
        .onComplete(
            pgHandler -> {
              if (pgHandler.succeeded()) {
                promise.complete(pgHandler.result());
              } else {
                promise.fail(pgHandler.cause());
              }
            });
    return promise.future();
  }

  public void publishMessage(QueryObject rmqMessage) {
    rmqService
        .publishMessage(rmqMessage, ratingExchangeName, "#")
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOGGER.info("Rating info publish to RabbitMQ");
              } else {
                LOGGER.error("Failed to publish Rating info");
              }
            });
  }
}
