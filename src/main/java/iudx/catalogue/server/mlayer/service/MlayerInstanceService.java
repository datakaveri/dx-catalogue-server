package iudx.catalogue.server.mlayer.service;

import static iudx.catalogue.server.common.util.ResponseBuilderUtil.internalErrorResp;
import static iudx.catalogue.server.database.elastic.util.Constants.ID_KEYWORD;
import static iudx.catalogue.server.database.elastic.util.Constants.SUMMARY_KEY;
import static iudx.catalogue.server.database.elastic.util.Constants.WORD_VECTOR_KEY;
import static iudx.catalogue.server.mlayer.util.Constants.INSTANCE_ID;
import static iudx.catalogue.server.mlayer.util.Constants.MLAYER_ID;
import static iudx.catalogue.server.mlayer.util.QueryModelUtil.checkForExistingRecordQuery;
import static iudx.catalogue.server.mlayer.util.QueryModelUtil.createMlayerInstanceQuery;
import static iudx.catalogue.server.util.Constants.DETAIL_INTERNAL_SERVER_ERROR;
import static iudx.catalogue.server.util.Constants.FAILED;
import static iudx.catalogue.server.util.Constants.ID;
import static iudx.catalogue.server.util.Constants.LIMIT;
import static iudx.catalogue.server.util.Constants.OFFSET;
import static iudx.catalogue.server.util.Constants.SUCCESS;
import static iudx.catalogue.server.util.Constants.TITLE_ALREADY_EXISTS;
import static iudx.catalogue.server.util.Constants.TITLE_INTERNAL_SERVER_ERROR;
import static iudx.catalogue.server.util.Constants.TITLE_ITEM_NOT_FOUND;
import static iudx.catalogue.server.util.Constants.TITLE_WRONG_INSTANCE_NAME;
import static iudx.catalogue.server.util.Constants.TYPE_ALREADY_EXISTS;
import static iudx.catalogue.server.util.Constants.TYPE_FAIL;
import static iudx.catalogue.server.util.Constants.TYPE_INTERNAL_SERVER_ERROR;
import static iudx.catalogue.server.util.Constants.TYPE_ITEM_NOT_FOUND;
import static iudx.catalogue.server.util.Constants.TYPE_SUCCESS;
import static iudx.catalogue.server.util.Constants.WRONG_INSTANCE_NAME;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.common.RespBuilder;
import iudx.catalogue.server.common.util.DbResponseMessageBuilder;
import iudx.catalogue.server.database.elastic.model.ElasticsearchResponse;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
import iudx.catalogue.server.mlayer.model.MlayerInstanceRequest;
import iudx.catalogue.server.mlayer.util.QueryModelUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MlayerInstanceService {
  private static final Logger LOGGER = LogManager.getLogger(MlayerInstanceService.class);
  private static final String internalErrorResp =
      new RespBuilder()
          .withType(TYPE_INTERNAL_SERVER_ERROR)
          .withTitle(TITLE_INTERNAL_SERVER_ERROR)
          .withDetail(DETAIL_INTERNAL_SERVER_ERROR)
          .getResponse();
  private final ElasticsearchService esService;
  private final String mlayerInstanceIndex;

  public MlayerInstanceService(ElasticsearchService esService, String mlayerInstanceIndex) {
    this.esService = esService;
    this.mlayerInstanceIndex = mlayerInstanceIndex;
  }

  public Future<JsonObject> getMlayerInstance(JsonObject requestParams) {
    String id = requestParams.getString(ID);
    String limit = requestParams.getString(LIMIT);
    String offset = requestParams.getString(OFFSET);
    QueryModel queryModel = createMlayerInstanceQuery(id, limit, offset);

    return esService.search(mlayerInstanceIndex, queryModel)
        .compose(response -> {
          LOGGER.debug("Success: Successful DB Request");
          DbResponseMessageBuilder responseMsg = new DbResponseMessageBuilder();
          responseMsg.statusSuccess();
          responseMsg.setTotalHits(response.size());
          response.stream()
              .map(ElasticsearchResponse::getSource)
              .peek(source -> {
                source.remove(SUMMARY_KEY);
                source.remove(WORD_VECTOR_KEY);
              })
              .forEach(responseMsg::addResult);
          return Future.succeededFuture(responseMsg.getResponse());
        })
        .recover(err -> {
          LOGGER.error("Fail: failed DB request; " + err.getMessage());
          return Future.failedFuture(internalErrorResp());
        });
  }

  public Future<JsonObject> deleteMlayerInstance(String instanceId) {
    Promise<JsonObject> promise = Promise.promise();
    RespBuilder respBuilder = new RespBuilder();

    QueryModel query = checkForExistingRecordQuery("instanceId.keyword", instanceId);
    esService.search(mlayerInstanceIndex, query)
        .onComplete(checkRes -> {
          if (checkRes.failed()) {
            LOGGER.error("Fail: Check query fail;" + checkRes.cause());
            promise.fail(internalErrorResp);
          } else {
            if (checkRes.result().size() != 1) {
              LOGGER.error("Fail: Instance doesn't exist, can't delete");
              promise.fail(
                  respBuilder
                      .withType(TYPE_ITEM_NOT_FOUND)
                      .withTitle(TITLE_ITEM_NOT_FOUND)
                      .withResult(instanceId, "Fail: Instance doesn't exist, can't delete")
                      .getResponse());
              return;
            }
            String docId = checkRes.result().getFirst().getDocId();

            esService.deleteDocument(mlayerInstanceIndex, docId)
                .onComplete(delRes -> {
                  if (delRes.succeeded()) {
                    promise.complete(
                        respBuilder
                            .withType(TYPE_SUCCESS)
                            .withTitle(SUCCESS)
                            .withResult(instanceId, "Instance deleted Successfully")
                            .getJsonResponse());
                  } else {
                    promise.fail(internalErrorResp);
                    LOGGER.error("Fail: Deletion failed;" + delRes.cause());
                  }
                });
          }
        });
    return promise.future();
  }

  public Future<JsonObject> createMlayerInstance(MlayerInstanceRequest instanceDoc) {
    Promise<JsonObject> promise = Promise.promise();
    RespBuilder respBuilder = new RespBuilder();
    QueryModel queryModel = checkForExistingRecordQuery(ID_KEYWORD, instanceDoc.getId());

    esService.search(mlayerInstanceIndex, queryModel)
        .onComplete(res -> {
          if (res.failed()) {
            LOGGER.error("Fail: Insertion of mlayer Instance failed: " + res.cause());
            promise.fail(
                respBuilder
                    .withType(FAILED)
                    .withResult(MLAYER_ID)
                    .withDetail("Fail: Insertion of Instance failed")
                    .getResponse());

          } else {
            if (!res.result().isEmpty()) {
              JsonObject json = res.result().getFirst().getSource();
              String instanceIdExists = json.getString(INSTANCE_ID);

              promise.fail(
                  respBuilder
                      .withType(TYPE_ALREADY_EXISTS)
                      .withTitle(TITLE_ALREADY_EXISTS)
                      .withResult(instanceIdExists, " Fail: Instance Already Exists")
                      .withDetail(" Fail: Instance Already Exists")
                      .getResponse());
              return;
            }
            esService.createDocument(mlayerInstanceIndex, instanceDoc.toJson())
                .onComplete(result -> {
                  if (result.succeeded()) {
                    promise.complete(
                        respBuilder
                            .withType(TYPE_SUCCESS)
                            .withTitle(SUCCESS)
                            .withResult(instanceDoc.getInstanceId().toString(),
                                "Instance created Sucesssfully")
                            .getJsonResponse());
                  } else {

                    promise.fail(
                        respBuilder
                            .withType(TYPE_FAIL)
                            .withResult(FAILED)
                            .withDetail(DETAIL_INTERNAL_SERVER_ERROR)
                            .getResponse());

                    LOGGER.error("Fail: Insertion failed" + result.cause());
                  }
                });
          }
        });
    return promise.future();
  }


  public Future<JsonObject> updateMlayerInstance(MlayerInstanceRequest instanceDoc) {
    Promise<JsonObject> promise = Promise.promise();
    RespBuilder respBuilder = new RespBuilder();
    String instanceId = instanceDoc.getInstanceId().toString();

    QueryModel query = QueryModelUtil.checkForExistingRecordQuery("instanceId.keyword",
        instanceId);
    esService.search(mlayerInstanceIndex, query)
        .onComplete(checkRes -> {
          if (checkRes.failed()) {
            LOGGER.error("Fail: Check query fail;" + checkRes.cause());
            promise.fail(internalErrorResp);
          } else {
            if (checkRes.result().size() != 1) {
              LOGGER.error("Fail: Instance doesn't exist, can't update");
              promise.fail(
                  respBuilder
                      .withType(TYPE_ITEM_NOT_FOUND)
                      .withTitle(TITLE_ITEM_NOT_FOUND)
                      .withResult(instanceId, "Fail : Instance doesn't exist, can't update")
                      .getResponse());
              return;
            }
            JsonObject source = checkRes.result().getFirst().getSource();

            String parameterIdName = source.getString("name").toLowerCase();
            String requestBodyName = instanceDoc.getName().toLowerCase();
            if (parameterIdName.equals(requestBodyName)) {
              String docId = checkRes.result().getFirst().getDocId();
              esService.updateDocument(mlayerInstanceIndex, docId, instanceDoc.toJson())
                  .onComplete(putRes -> {
                    if (putRes.succeeded()) {
                      promise.complete(
                          respBuilder
                              .withType(TYPE_SUCCESS)
                              .withTitle(SUCCESS)
                              .withResult(instanceId, "Instance Updated Successfully")
                              .getJsonResponse());
                    } else {
                      promise.fail(internalErrorResp);
                      LOGGER.error("Fail: Updation failed" + putRes.cause());
                    }
                  });
            } else {
              promise.fail(
                  respBuilder
                      .withType(TYPE_FAIL)
                      .withTitle(TITLE_WRONG_INSTANCE_NAME)
                      .withDetail(WRONG_INSTANCE_NAME)
                      .getResponse());
              LOGGER.error("Fail: Updation Failed" + checkRes.cause());
            }
          }
        });
    return promise.future();
  }
}
