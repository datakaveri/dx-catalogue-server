package iudx.catalogue.server.apiserver.stack.service;

import static iudx.catalogue.server.apiserver.stack.util.StackConstants.DOC_ID;
import static iudx.catalogue.server.database.elastic.util.Constants.SUMMARY_KEY;
import static iudx.catalogue.server.database.elastic.util.Constants.WORD_VECTOR_KEY;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.util.Constants.TITLE_ITEM_NOT_FOUND;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.apiserver.stack.util.QueryBuilder;
import iudx.catalogue.server.common.RespBuilder;
import iudx.catalogue.server.common.util.DbResponseMessageBuilder;
import iudx.catalogue.server.database.elastic.model.ElasticsearchResponse;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StacServiceImpl implements StacService {
  private static final Logger LOGGER = LogManager.getLogger(StacServiceImpl.class);

  private final ElasticsearchService esService;
  private final String index;
  public RespBuilder respBuilder;
  Supplier<String> idSuppler = () -> UUID.randomUUID().toString();
  private final QueryBuilder queryBuilder = new QueryBuilder();

  public StacServiceImpl(ElasticsearchService esService, String index) {
    this.esService = esService;
    this.index = index;
  }

  /**
   * @param stackId stack id
   * @return future Json
   */
  @Override
  public Future<JsonObject> get(String stackId) {
    Promise<JsonObject> promise = Promise.promise();
    QueryModel query = queryBuilder.getQuery(stackId);
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(query);
    esService.search(index, queryModel)
        .onComplete(clientHandler -> {
          if (clientHandler.succeeded()) {
            LOGGER.debug("Success: Successful DB request");
            List<ElasticsearchResponse> responseList = clientHandler.result();
            DbResponseMessageBuilder result = new DbResponseMessageBuilder();
            result.statusSuccess();
            result.setTotalHits(responseList.size());
            responseList.stream()
                .map(ElasticsearchResponse::getSource)
                .peek(source -> {
                  source.remove(SUMMARY_KEY);
                  source.remove(WORD_VECTOR_KEY);
                })
                .forEach(result::addResult);

            if (result.getResponse().getInteger(TOTAL_HITS) > 0) {
              promise.complete(result.getResponse());
            } else {
              LOGGER.error("Fail: Item not found");
              respBuilder =
                  new RespBuilder()
                      .withType(TYPE_ITEM_NOT_FOUND)
                      .withTitle(TITLE_ITEM_NOT_FOUND)
                      .withDetail("Fail: Stac doesn't exist");
              promise.fail(respBuilder.getResponse());
            }
          } else {
            LOGGER.error("Fail: DB request has failed;" + clientHandler.cause());
            respBuilder =
                new RespBuilder()
                    .withType(FAILED)
                    .withResult(stackId, REQUEST_GET, FAILED)
                    .withDetail(DATABASE_ERROR);
            promise.fail(respBuilder.getResponse());
          }
        });

    return promise.future();
  }

  /**
   * @param request json
   * @return future json
   */
  @Override
  public Future<JsonObject> create(JsonObject request) {
    LOGGER.debug("create () method started");
    QueryModel query = queryBuilder.getQuery4CheckExistence(request);
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(query);
    String id = idSuppler.get();
    request.put(ID, id);
    LOGGER.info("id :{}", request);
    Promise<JsonObject> promise = Promise.promise();
    esService.search(index, queryModel)
        .onComplete(searchHandler -> {
          if (searchHandler.succeeded()) {
            if (searchHandler.result().isEmpty()) {
              esService.createDocument(index, request)
                  .onComplete(postHandler -> {
                    if (postHandler.succeeded()) {
                      LOGGER.info("Success: Stac creation");
                      JsonObject result = postHandler.result();
                      LOGGER.debug("Success : " + result);
                      respBuilder =
                          new RespBuilder()
                              .withType(SUCCESS)
                              .withResult(id, INSERT, SUCCESS)
                              .withDetail(STAC_CREATION_SUCCESS);
                      promise.complete(respBuilder.getJsonResponse());
                    } else {
                      LOGGER.error("Fail: STAC creation : {}", postHandler.cause().getMessage());
                      respBuilder =
                          new RespBuilder()
                              .withType(FAILED)
                              .withResult("stack", INSERT, FAILED)
                              .withDetail(DATABASE_ERROR);
                      promise.fail(respBuilder.getResponse());
                    }
                  });
            } else {
              LOGGER.error("STAC already exists, skipping creation");
              respBuilder =
                  new RespBuilder()
                      .withType(TYPE_CONFLICT)
                      .withTitle(DETAIL_CONFLICT)
                      .withDetail("STAC already exists,creation skipped");
              promise.fail(respBuilder.getResponse());
            }
          } else {
            LOGGER.error("Fail: Search operation : {}", searchHandler.cause().getMessage());
            respBuilder =
                new RespBuilder()
                    .withType(FAILED)
                    .withResult("stac", INSERT, FAILED)
                    .withDetail(DATABASE_ERROR);
            promise.fail(respBuilder.getResponse());
          }
        });

    return promise.future();
  }

  /**
   * @param stack Json object
   * @return future json
   */
  @Override
  public Future<JsonObject> update(JsonObject stack) {
    LOGGER.debug("update () method started");
    Promise<JsonObject> promise = Promise.promise();
    ResultContainer result = new ResultContainer();
    String stacId = stack.getString("id");
    Future<JsonObject> existFuture = isExist(stacId);
    existFuture
        .compose(
            existHandler -> {
              LOGGER.info(existHandler);
              result.links = existHandler.getJsonObject("_source").getJsonArray("links");
              result.docId = existHandler.getString(DOC_ID);
              return isAllowPatch(stack, result.links);
            })
        .compose(
            allowHandler -> doUpdate(stack, result.docId, allowHandler))
        .onSuccess(
            successHandler -> {
              respBuilder =
                  new RespBuilder()
                      .withType(TYPE_SUCCESS)
                      .withTitle(TITLE_SUCCESS)
                      .withResult(stacId)
                      .withDetail("Success: Item updated successfully");
              promise.complete(respBuilder.getJsonResponse());
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error("error : " + failureHandler.getMessage());
              promise.fail(failureHandler.getMessage());
            });

    return promise.future();
  }

  /**
   * @param stacId String
   * @return future json
   */
  @Override
  public Future<JsonObject> delete(String stacId) {
    LOGGER.debug("delete () method started");
    Promise<JsonObject> promise = Promise.promise();
    LOGGER.debug("stackId for delete :{}", stacId);
    isExist(stacId)
        .onComplete(
            existHandler -> {
              if (existHandler.succeeded()) {
                JsonObject result = existHandler.result();
                String docId = result.getString(DOC_ID);
                esService.deleteDocument(index, docId)
                    .onComplete(deleteHandler -> {
                      if (deleteHandler.succeeded()) {
                        LOGGER.info("Deletion success :{}", deleteHandler.result());
                        respBuilder =
                            new RespBuilder()
                                .withType(SUCCESS)
                                .withResult(stacId)
                                .withDetail(STAC_DELETION_SUCCESS);
                        promise.complete(respBuilder.getJsonResponse());
                      } else {
                        LOGGER.error(
                            "Fail: Delete operation failed : {}",
                            deleteHandler.cause().getMessage());
                        respBuilder =
                            new RespBuilder()
                                .withType(FAILED)
                                .withResult(stacId, REQUEST_DELETE, FAILED)
                                .withDetail(DATABASE_ERROR);
                        promise.fail(respBuilder.getResponse());
                      }
                    });
              } else {
                LOGGER.error(
                    "Fail: Item not found for deletion : {}", existHandler.cause().getMessage());
                respBuilder =
                    new RespBuilder()
                        .withType(TYPE_ITEM_NOT_FOUND)
                        .withResult(stacId, REQUEST_DELETE, FAILED)
                        .withDetail("Item not found, can't delete");
                promise.fail(respBuilder.getResponse());
              }
            });

    return promise.future();
  }

  private Future<JsonObject> isExist(String id) {
    LOGGER.debug("isExist () method started");
    Promise<JsonObject> promise = Promise.promise();
    LOGGER.debug("stacId: {}", id);
    QueryModel query = queryBuilder.getQuery(id);
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(query);

    esService.search(index, queryModel)
        .onComplete(existHandler -> {
          LOGGER.debug("existHandler succeeded " + existHandler.succeeded());
          if (existHandler.failed()) {
            LOGGER.error("Fail: Check Query Fail : {}", existHandler.cause().getMessage());
            promise.fail("Fail: Check Query Fail : " + existHandler.cause().getMessage());
            return;
          }
          if (existHandler.result().isEmpty()) {
            LOGGER.debug("success: existHandler " + existHandler.result());
            respBuilder =
                new RespBuilder()
                    .withType(TYPE_ITEM_NOT_FOUND)
                    .withTitle(TITLE_ITEM_NOT_FOUND)
                    .withDetail("Fail: stac doesn't exist");
            promise.fail(respBuilder.getResponse());
          } else {
            try {
              List<ElasticsearchResponse> response = existHandler.result();
              DbResponseMessageBuilder responseMsg = new DbResponseMessageBuilder();
              responseMsg.statusSuccess().setTotalHits(response.size());
              response.stream()
                  .map(elasticResponse -> {
                    JsonObject json = new JsonObject();
                    json.put(SOURCE, elasticResponse.getSource());
                    json.put(DOC_ID, elasticResponse.getDocId());
                    return json;
                  })
                  .forEach(responseMsg::addResult);
              LOGGER.debug(existHandler.result());
              JsonObject result =
                  new JsonObject(responseMsg.getResponse().getJsonArray(RESULTS).getString(0));
              promise.complete(result);
            } catch (Exception e) {
              LOGGER.error("Fail: Parsing result : {}", e.getMessage());
              promise.fail("Fail: Parsing result");
            }
          }
        });

    return promise.future();
  }

  private Future<Boolean> isAllowPatch(JsonObject requestBody, JsonArray links) {
    LOGGER.debug("isAllowPatch () method started");
    Promise<Boolean> promise = Promise.promise();
    AtomicBoolean allowPatch = new AtomicBoolean(true);
    links.stream()
        .map(JsonObject.class::cast)
        .forEach(
            child -> {
              if (child.getString("rel").equalsIgnoreCase("child")
                  && child.getString("href").equalsIgnoreCase(requestBody.getString("href"))) {
                allowPatch.set(false);
              }
            });
    LOGGER.info("isAllowPatch : {}", allowPatch.get());
    promise.complete(allowPatch.get());
    return promise.future();
  }

  private Future<JsonObject> doUpdate(JsonObject request, String docId, boolean isAllowed) {
    LOGGER.debug("doUpdate () method started");
    if (!isAllowed) {
      LOGGER.debug("Patch operations not allowed for duplicate child");
      respBuilder =
          new RespBuilder()
              .withType(TYPE_CONFLICT)
              .withTitle(TITLE_ALREADY_EXISTS)
              .withDetail("Patch operations not allowed for duplicate child");
      return Future.failedFuture(respBuilder.getResponse());
    }
    LOGGER.debug("docId: {}", docId);
    request.remove("id");
    String query = queryBuilder.getPatchQuery(request);
    JsonObject doc = new JsonObject(query);
    LOGGER.debug("patchQuery:: " + query);
    Promise<JsonObject> promise = Promise.promise();
    esService.patchDocument(index, docId, doc)
        .onComplete(patchHandler -> {
          if (patchHandler.succeeded()) {

            JsonObject result = patchHandler.result();
            LOGGER.debug("patch result " + result);
            promise.complete(result);
          } else {
            LOGGER.error("failed:: " + patchHandler.cause().getMessage());
            promise.fail(patchHandler.cause().getMessage());
          }
        });
    return promise.future();
  }

  static final class ResultContainer {
    JsonArray links;
    String docId;
    boolean allowPatch;
  }
}
