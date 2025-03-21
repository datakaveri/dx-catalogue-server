package iudx.catalogue.server.rating.service;

import static iudx.catalogue.server.auditing.util.Constants.ID;
import static iudx.catalogue.server.common.util.ResponseBuilderUtil.*;
import static iudx.catalogue.server.database.elastic.model.ElasticsearchResponse.getAggregations;
import static iudx.catalogue.server.database.elastic.util.Constants.DOC_COUNT;
import static iudx.catalogue.server.database.elastic.util.Constants.ID_KEYWORD;
import static iudx.catalogue.server.database.elastic.util.Constants.KEY;
import static iudx.catalogue.server.rating.util.QueryModelUtil.*;
import static iudx.catalogue.server.util.Constants.AVERAGE_RATING;
import static iudx.catalogue.server.util.Constants.BUCKETS;
import static iudx.catalogue.server.util.Constants.DELETE;
import static iudx.catalogue.server.util.Constants.RESULTS;
import static iudx.catalogue.server.util.Constants.TITLE_REQUIREMENTS_NOT_MET;
import static iudx.catalogue.server.util.Constants.TOTAL_RATINGS;
import static iudx.catalogue.server.util.Constants.TYPE_ACCESS_DENIED;
import static iudx.catalogue.server.util.Constants.UPDATE;
import static iudx.catalogue.server.util.Constants.VALUE;

import com.google.common.hash.Hashing;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.apiserver.util.RespBuilder;
import iudx.catalogue.server.common.util.DbResponseMessageBuilder;
import iudx.catalogue.server.database.cache.service.CacheService;
import iudx.catalogue.server.database.elastic.model.ElasticsearchResponse;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
import iudx.catalogue.server.databroker.service.RabbitMQService;
import iudx.catalogue.server.rating.model.FilterRatingRequest;
import iudx.catalogue.server.rating.model.RatingRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
  CacheService cacheService;

  /**
   * Constructor for RatingServiceImpl class. Initializes the object with the given parameters.
   *
   * @param exchangeName the name of the exchange used for rating
   * @param rsauditingtable the name of the table used for auditing the rating system
   * @param minReadNumber the minimum number of reads for a rating to be considered valid
   * @param ratingIndex the index of the rating docs
   * @param elasticsearchService the service used for interacting with the database
   * @param rmqService the service used for interacting with the data broker
   * @param cacheService the service used for interacting with the PostgresService database
   */
  public RatingServiceImpl(
      String exchangeName,
      String rsauditingtable,
      int minReadNumber,
      String ratingIndex,
      ElasticsearchService elasticsearchService,
      String docIndex,
      RabbitMQService rmqService,
      CacheService cacheService) {
    this.ratingExchangeName = exchangeName;
    this.rsauditingtable = rsauditingtable;
    this.minReadNumber = minReadNumber;
    this.ratingIndex = ratingIndex;
    this.esService = elasticsearchService;
    this.docIndex = docIndex;
    this.rmqService = rmqService;
    this.cacheService = cacheService;
  }

  @Override
  public Future<JsonObject> createRating(RatingRequest ratingRequestModel) {
    String sub = ratingRequestModel.getUserID();
    String id = ratingRequestModel.getId();
    Future<Integer> getRsAuditingInfo = cacheService.getAuditingInfo(rsauditingtable, sub, id);

    return getRsAuditingInfo
        .recover(failureHandler -> {
          LOGGER.error("User has not accessed resource"
              + " before and hence is not authorised to give rating");
          return Future.failedFuture(failureHandler.getMessage());
        })
        .compose(
            totalHits -> {
              int countResourceAccess = totalHits;
              if (countResourceAccess > minReadNumber) {
                String ratingId =
                    Hashing.sha256().hashString(sub + id, StandardCharsets.UTF_8).toString();
                ratingRequestModel.setRatingID(ratingId);
                QueryModel queryModel = getRatingQueryModel(ratingId);

                return esService.search(ratingIndex, queryModel)
                    .recover(err -> {
                      LOGGER.error("Fail: Insertion of rating failed: " + err.getMessage());
                      return Future.failedFuture(failureResp(ratingId));
                    })
                    .compose(successRes -> {
                      if (!successRes.isEmpty()) {
                        return Future.failedFuture(
                            itemAlreadyExistsResponse(ratingId, " Fail: Doc Already Exists"));
                      }
                      return esService.createDocument(ratingIndex, ratingRequestModel.toJson())
                          .recover(err -> {
                            LOGGER.error("Fail: Insertion failed" + err.getMessage());
                            return Future.failedFuture(failureResponse(ratingId));
                          })
                          .compose(postRes -> {
                            LOGGER.info("Success: Rating Recorded");
                            return Future.succeededFuture(successResponse(ratingId));
                          });
                      });
              } else {
                LOGGER.error("Fail: RatingRequest creation failed");
                String message = new RespBuilder()
                    .withType(TYPE_ACCESS_DENIED)
                    .withTitle(TITLE_REQUIREMENTS_NOT_MET)
                    .withDetail(
                        "User has to access resource at least "
                            + minReadNumber
                            + " times to give rating")
                    .getResponse();
                return Future.failedFuture(message);
              }
            });

  }

  @Override
  public Future<JsonObject> getRating(FilterRatingRequest filterRequestModel) {
    String id = filterRequestModel.getId();
    String ratingId;
    if (filterRequestModel.getType() != null) {
      String sub = filterRequestModel.getUserID();
      ratingId = Hashing.sha256().hashString(sub + id, StandardCharsets.UTF_8).toString();
      filterRequestModel.setRatingID(ratingId);
    }

    if (filterRequestModel.getRatingID() == null) {
      if (filterRequestModel.getType() != null &&
          filterRequestModel.getType().equalsIgnoreCase("average")) {
        Future<List<String>> getAssociatedIdFuture = getAssociatedIDs(id);
        return getAssociatedIdFuture
            .recover(err -> Future.failedFuture(internalErrorResp()))
            .compose(ids -> {
              QueryModel avgRatingQuery = getAverageRatingQueryModel(ids);
              return esService.search(ratingIndex, avgRatingQuery)
                  .recover(err -> {
                    LOGGER.error("Fail: failed getting average rating: " + err.getMessage());
                    return Future.failedFuture(internalErrorResp());
                  })
                  .compose(successRes -> {
                    LOGGER.debug("Success: Successful DB request");
                    DbResponseMessageBuilder responseMsg = new DbResponseMessageBuilder();
                    responseMsg.statusSuccess().setTotalHits(successRes.size());
                    try {
                      JsonObject aggregationRes = getAggregations().getJsonObject(RESULTS);
                      JsonArray buckets = aggregationRes.getJsonArray(BUCKETS);
                      // Process the aggregations using streams
                      buckets.stream()
                          // Ensure it's a JsonObject
                          .filter(aggregation -> aggregation instanceof JsonObject)
                          .map(aggregation -> {
                            JsonObject bucket = (JsonObject) aggregation;
                            // Extract the necessary fields from the aggregation
                            String key = bucket.getString(KEY);
                            String totalRatings = bucket.getString(DOC_COUNT);
                            double averageRating =
                                bucket.getJsonObject(AVERAGE_RATING).getDouble(VALUE);
                            // Create and return a new JsonObject with the extracted fields
                            return new JsonObject()
                                .put(ID, key)
                                .put(TOTAL_RATINGS, totalRatings)
                                .put(AVERAGE_RATING, averageRating);
                          }).forEach(responseMsg::addResult); // Add each result to the response message
                          return Future.succeededFuture(responseMsg.getResponse());
                    } catch (Exception e) {
                      LOGGER.error("Error processing global aggregations: " + e.getMessage(), e);
                      return Future.failedFuture(internalErrorResp());
                    }
                  });
            });
      }
    }

    QueryModel queryModel = getRatingQueryModel(filterRequestModel.getRatingID() != null ?
            "ratingID.keyword" : ID_KEYWORD,
          filterRequestModel.getRatingID() != null ? filterRequestModel.getRatingID() :
              filterRequestModel.getId());
    return esService.search(ratingIndex, queryModel)
        .recover(err -> {
          LOGGER.error("Fail: failed getting rating: " + err.getMessage());
          return Future.failedFuture(internalErrorResp());
        })
        .compose(getRes -> {
          LOGGER.debug("Success: Successful DB request");
          DbResponseMessageBuilder responseMsg = new DbResponseMessageBuilder();
          responseMsg.statusSuccess();
          if (filterRequestModel.getRatingID() != null) {
            responseMsg.setTotalHits(getRes.size());
          }
          getRes.stream()
              .map(ElasticsearchResponse::getSource)
              .forEach(responseMsg::addResult);
          return Future.succeededFuture(responseMsg.getResponse());
        });
  }

  private Future<List<String>> getAssociatedIDs(String id) {
    QueryModel queryModel = getAssociatedIdsQuery(id);
    return esService.search(docIndex, queryModel)
        .recover(err -> {
          LOGGER.error("Fail: Get average rating failed");
          return Future.failedFuture("Fail: Get average rating failed");
        })
        .compose(res -> {
          List<String> idCollector =
              res.stream()
                  .map(ElasticsearchResponse::getSource)
                  .map(d -> d.getString(ID))
                  .toList();
          return Future.succeededFuture(idCollector);
        });
  }

  @Override
  public Future<JsonObject> updateRating(RatingRequest requestModel) {
    String sub = requestModel.getUserID();
    String id = requestModel.getId();
    String ratingId = Hashing.sha256().hashString(sub + id, StandardCharsets.UTF_8).toString();
    requestModel.setRatingID(ratingId);

    QueryModel queryModel = getRatingQueryModel(ratingId);
    return esService.search(ratingIndex, queryModel)
        .recover(err -> {
          LOGGER.error("Fail: Check query fail;" + err.getMessage());
          return Future.failedFuture(internalErrorResp());
        })
        .compose(checkRes -> {
          if (checkRes.size() != 1) {
            LOGGER.error("Fail: Doc doesn't exist, can't update");
            return Future.failedFuture(
                itemNotFoundResponse(ratingId, UPDATE, "Fail: Doc doesn't exist, can't update"));
          }
          String docId = checkRes.getFirst().getDocId();
          return esService.updateDocument(ratingIndex, docId, requestModel.toJson())
              .recover(err -> {
                LOGGER.error("Fail: Updation failed;" + err.getMessage());
                return Future.failedFuture(internalErrorResp());
              })
              .compose(putRes -> Future.succeededFuture(ratingSuccessResponse(ratingId)));
        });
  }

  @Override
  public Future<JsonObject> deleteRating(FilterRatingRequest filterRequest) {
    String sub = filterRequest.getUserID();
    String id = filterRequest.getId();
    String ratingId = Hashing.sha256().hashString(sub + id, StandardCharsets.UTF_8).toString();

    QueryModel queryModel = getRatingQueryModel(ratingId);
    return esService.search(ratingIndex, queryModel)
        .recover(err -> {
          LOGGER.error("Fail: Check query fail;" + err.getMessage());
          return Future.failedFuture(internalErrorResp());
        })
        .compose(checkRes -> {
          if (checkRes.size() != 1) {
            LOGGER.error("Fail: Doc doesn't exist, can't delete");
            return Future.failedFuture(
                itemNotFoundResponse(ratingId, DELETE, "Fail: Doc doesn't exist, can't delete"));
          }
          String docId = checkRes.getFirst().getDocId();
          return esService.deleteDocument(ratingIndex, docId)
              .recover(err -> {
                LOGGER.error("Fail: Deletion failed;" + err.getMessage());
                return Future.failedFuture(internalErrorResp());
              })
              .compose(delRes -> Future.succeededFuture(ratingSuccessResponse(ratingId)));
        });
  }
}
