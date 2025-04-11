package iudx.catalogue.server.apiserver.stack.service;

import static iudx.catalogue.server.apiserver.stack.util.QueryModelUtil.*;
import static iudx.catalogue.server.apiserver.stack.util.StackConstants.DOC_ID;
import static iudx.catalogue.server.database.elastic.util.Constants.SUMMARY_KEY;
import static iudx.catalogue.server.database.elastic.util.Constants.WORD_VECTOR_KEY;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.apiserver.stack.model.StacCatalog;
import iudx.catalogue.server.apiserver.stack.model.StacLink;
import iudx.catalogue.server.common.util.DbResponseMessageBuilder;
import iudx.catalogue.server.database.elastic.model.ElasticsearchResponse;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
import iudx.catalogue.server.exceptions.DatabaseFailureException;
import iudx.catalogue.server.exceptions.DocAlreadyExistsException;
import iudx.catalogue.server.exceptions.InternalServerException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StacServiceImpl implements StacService {
  private static final Logger LOGGER = LogManager.getLogger(StacServiceImpl.class);
  private final String docIndex;
  private final ElasticsearchService esService;
  public StacServiceImpl(ElasticsearchService esService, String docIndex) {
    this.esService = esService;
    this.docIndex = docIndex;
  }

  /**
   * @param stackId stack id
   * @return future Json
   */
  @Override
  public Future<JsonObject> get(String stackId) {
    LOGGER.debug("get () method started");
    QueryModel queryModel = getStacQuery(stackId);

    return esService
        .search(docIndex, queryModel)
        .compose(esResponse -> {
          if (esResponse.isEmpty()) {
            return Future.failedFuture(new NoSuchElementException("Item not found"));
          }
          DbResponseMessageBuilder responseMsg = new DbResponseMessageBuilder()
              .statusSuccess().setTotalHits(esResponse.size());
          esResponse.stream()
              .map(ElasticsearchResponse::getSource)
              .peek(source -> {
                source.remove(SUMMARY_KEY);
                source.remove(WORD_VECTOR_KEY);
              })
              .forEach(responseMsg::addResult);
          LOGGER.info("Success: Stac item retrieved;");
          return Future.succeededFuture(responseMsg.getResponse());
        })
        .recover(
            err -> {
              LOGGER.error("Fail: Item retrieval failed; " + err.getMessage());
              return Future.failedFuture(new InternalServerException(err.getMessage(),
                  REQUEST_GET));
            });
  }

  /**
   * @param stacCatalog StacCatalog instance
   * @return future json
   */
  @Override
  public Future<StacCatalog> create(StacCatalog stacCatalog) {
    LOGGER.debug("create () method started");

    QueryModel queryModel = checkStacCatItemQuery(stacCatalog.toJson());
    JsonObject doc = stacCatalog.toJson();

    return esService.search(docIndex, queryModel)
        .compose(searchRes -> {
            if (searchRes.isEmpty()) {
              return esService.createDocument(docIndex, doc)
                  .compose(res -> {
                    LOGGER.info("Success: Stac creation");
                    LOGGER.debug("Success : " + res);
                    return Future.succeededFuture(stacCatalog);
                  })
                  .recover(cause -> {
                    LOGGER.error("Fail: STAC creation : {}", cause.getMessage());
                    return Future.failedFuture(TYPE_DB_ERROR);
                  });
            } else {
              LOGGER.error("STAC already exists, skipping creation");
              return Future.failedFuture(TYPE_ALREADY_EXISTS);
            }
        })
        .recover(cause -> {
          if (cause.getMessage().contains(TYPE_ALREADY_EXISTS)) {
            return Future.failedFuture(new DocAlreadyExistsException(stacCatalog.getId().toString()));
          } else if (cause.getMessage().contains(TYPE_DB_ERROR)) {
            return Future.failedFuture(new DatabaseFailureException(stacCatalog.getId().toString(),
                    cause.getMessage()));
          } else {
            LOGGER.error("Fail: Search operation : {}", cause.getMessage());
            return Future.failedFuture(new InternalServerException(cause.getMessage(),
                REQUEST_POST));
          }
        });
  }

  /**
   * @param stacLink instance of StacLink
   * @return future json
   */
  @Override
  public Future<String> update(StacLink stacLink) {
    LOGGER.debug("update () method started");
    Promise<String> promise = Promise.promise();
    ResultContainer result = new ResultContainer();
    String stacId = stacLink.getId();
    Future<JsonObject> existFuture = isExist(stacId);
    existFuture
        .compose(
            existHandler -> {
              LOGGER.info(existHandler);
              result.links = existHandler.getJsonObject("_source").getJsonArray("links");
              result.docId = existHandler.getString(DOC_ID);
              return isAllowPatch(stacLink, result.links);
            })
        .compose(
            allowHandler -> doUpdate(stacLink, result.docId, allowHandler))
        .onSuccess(promise::complete)
        .onFailure(
            failureHandler -> {
              LOGGER.error("error : " + failureHandler.getMessage());
              if(failureHandler instanceof NoSuchElementException exception){
                promise.fail(exception);
                return;
              }
              promise.fail(failureHandler.getMessage());
            });

    return promise.future();
  }

  /**
   * @param stacId String
   * @return future json
   */
  @Override
  public Future<String> delete(String stacId) {
    LOGGER.debug("delete () method started");
    Promise<String> promise = Promise.promise();
    LOGGER.debug("stackId for delete :{}", stacId);

    isExist(stacId)
        .onComplete(
            existHandler -> {
              if (existHandler.succeeded()) {
                JsonObject result = existHandler.result();
                String docId = result.getString(DOC_ID);
                esService.deleteDocument(docIndex, docId)
                    .onComplete(deleteHandler -> {
                      if (deleteHandler.succeeded()) {
                        LOGGER.info("Deletion success :{}", deleteHandler.result());
                        promise.complete(stacId);
                      } else {
                        LOGGER.error("Fail: Delete operation failed : {}",
                            deleteHandler.cause().getMessage());
                        promise.fail(new InternalServerException(deleteHandler.cause().getMessage(),
                            REQUEST_DELETE));
                      }
                    });
              } else {
                LOGGER.error(
                    "Fail: Item not found for deletion : {}", existHandler.cause().getMessage());
                promise.fail(existHandler.cause().getMessage());
              }
            });

    return promise.future();
  }

  private Future<JsonObject> isExist(String id) {
    LOGGER.debug("isExist () method started");
    Promise<JsonObject> promise = Promise.promise();
    LOGGER.debug("stacId: {}", id);

    QueryModel queryModel = getStacQuery(id);

    esService.search(docIndex, queryModel)
        .onComplete(existHandler -> {
          LOGGER.debug("existHandler succeeded " + existHandler.succeeded());
          if (existHandler.failed()) {
            LOGGER.error("Fail: Check Query Fail : {}", existHandler.cause().getMessage());
            promise.fail("Fail: Check Query Fail : " + existHandler.cause().getMessage());
            return;
          }
          if (existHandler.result().isEmpty()) {
            LOGGER.debug("success: existHandler " + existHandler.result());
            promise.fail(new NoSuchElementException("Item not found"));
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

  private Future<Boolean> isAllowPatch(StacLink requestBody, JsonArray links) {
    LOGGER.debug("isAllowPatch () method started");
    Promise<Boolean> promise = Promise.promise();
    AtomicBoolean allowPatch = new AtomicBoolean(true);
    links.stream()
        .map(JsonObject.class::cast)
        .forEach(
            child -> {
              if (child.getString("rel").equalsIgnoreCase("child")
                  && child.getString("href").equalsIgnoreCase(requestBody.getHref())) {
                allowPatch.set(false);
              }
            });
    LOGGER.info("isAllowPatch : {}", allowPatch.get());
    promise.complete(allowPatch.get());
    return promise.future();
  }

  private Future<String> doUpdate(StacLink request, String docId, boolean isAllowed) {
    LOGGER.debug("doUpdate () method started");
    if (!isAllowed) {
      LOGGER.debug("Patch operations not allowed for duplicate child");
      return Future.failedFuture(TITLE_ALREADY_EXISTS);
    }
    LOGGER.debug("docId: {}", docId);
    //request.remove("id");
    QueryModel queryModel = getPatchQuery(request);
    LOGGER.debug("patch queryModel: " + queryModel.toJson());
    Promise<String> promise = Promise.promise();

    esService.patchDocument(docIndex, docId, queryModel)
        .onComplete(patchHandler -> {
          if (patchHandler.succeeded()) {
            JsonObject result = patchHandler.result();
            LOGGER.debug("patch result " + result);
            promise.complete(request.getId());
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
