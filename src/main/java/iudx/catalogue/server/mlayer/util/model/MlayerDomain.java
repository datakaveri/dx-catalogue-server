package iudx.catalogue.server.mlayer.util.model;

import static iudx.catalogue.server.database.elastic.util.Constants.ID_KEYWORD;
import static iudx.catalogue.server.database.elastic.util.Constants.SUMMARY_KEY;
import static iudx.catalogue.server.database.elastic.util.Constants.WORD_VECTOR_KEY;
import static iudx.catalogue.server.mlayer.util.Constants.DOMAIN_ID;
import static iudx.catalogue.server.mlayer.util.Constants.MLAYER_ID;
import static iudx.catalogue.server.util.Constants.DETAIL_INTERNAL_SERVER_ERROR;
import static iudx.catalogue.server.util.Constants.FAILED;
import static iudx.catalogue.server.util.Constants.FIELD;
import static iudx.catalogue.server.util.Constants.ID;
import static iudx.catalogue.server.util.Constants.LIMIT;
import static iudx.catalogue.server.util.Constants.MAX_LIMIT;
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
import static iudx.catalogue.server.util.Constants.VALUE;
import static iudx.catalogue.server.util.Constants.WRONG_INSTANCE_NAME;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.common.RespBuilder;
import iudx.catalogue.server.common.util.DbResponseMessageBuilder;
import iudx.catalogue.server.database.elastic.model.ElasticsearchResponse;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
import iudx.catalogue.server.database.elastic.util.QueryType;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MlayerDomain {
  private static final Logger LOGGER = LogManager.getLogger(MlayerDomain.class);
  private static String internalErrorResp =
      new RespBuilder()
          .withType(TYPE_INTERNAL_SERVER_ERROR)
          .withTitle(TITLE_INTERNAL_SERVER_ERROR)
          .withDetail(DETAIL_INTERNAL_SERVER_ERROR)
          .getResponse();
  ElasticsearchService esService;
  String mlayerDomainIndex;

  public MlayerDomain(ElasticsearchService esService, String mlayerDomainIndex) {
    this.esService = esService;
    this.mlayerDomainIndex = mlayerDomainIndex;
  }

  public Future<JsonObject> createMlayerDomain(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    RespBuilder respBuilder = new RespBuilder();
    String domainId = request.getString(DOMAIN_ID);
    String id = request.getString(MLAYER_ID);
    QueryModel query = new QueryModel(QueryType.BOOL);
    query.addMustQuery(new QueryModel(QueryType.MATCH, Map.of(FIELD, ID_KEYWORD,
        VALUE, id)));
    QueryModel checkForExistingDomain = new QueryModel();
    checkForExistingDomain.setQueries(query);

    esService.search(mlayerDomainIndex, checkForExistingDomain)
        .onComplete(res -> {
          if (res.failed()) {
            LOGGER.error("Fail: Insertion of mLayer domain failed: " + res.cause());
            promise.fail(
                respBuilder
                    .withType(FAILED)
                    .withResult(id)
                    .withDetail("Fail: Insertion of mLayer domain failed: ")
                    .getResponse());

          } else {
            if (!res.result().isEmpty()) {
              JsonObject json = res.result().get(0).getSource();
              String domainIdExists = json.getString(DOMAIN_ID);
              promise.fail(
                  respBuilder
                      .withType(TYPE_ALREADY_EXISTS)
                      .withTitle(TITLE_ALREADY_EXISTS)
                      .withResult(domainIdExists, "Fail: Domain Already Exists")
                      .withDetail("Fail: Domain Already Exists")
                      .getResponse());
              return;
            }
            esService.createDocument(mlayerDomainIndex, request).onComplete(
                result -> {
                  if (result.succeeded()) {
                    promise.complete(
                        respBuilder
                            .withType(TYPE_SUCCESS)
                            .withTitle(SUCCESS)
                            .withResult(domainId, "domain Created Successfully")
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

  public Future<JsonObject> getMlayerDomain(JsonObject requestParams) {
    Promise<JsonObject> promise = Promise.promise();
    String id = requestParams.getString(ID);
    String limit = requestParams.getString(LIMIT);
    String offset = requestParams.getString(OFFSET);
    QueryModel queryModel = new QueryModel();
    queryModel.setIncludeFields(List.of("domainId", "description", "icon", "label", "name"));
    if (!requestParams.containsKey(ID)) {
      queryModel.setQueries(new QueryModel(QueryType.MATCH_ALL));
      queryModel.setLimit(limit);
      queryModel.setOffset(offset);

    } else {
      queryModel.setQueries(new QueryModel(QueryType.MATCH, Map.of(FIELD, "domainId.keyword",
          VALUE, id)));
      queryModel.setLimit(MAX_LIMIT);
    }
    esService.search(mlayerDomainIndex, queryModel)
        .onComplete(resultHandler -> {
          if (resultHandler.succeeded()) {
            LOGGER.debug("Success: Successful DB Request");

            List<ElasticsearchResponse> responseList = resultHandler.result();
            DbResponseMessageBuilder responseMsg = new DbResponseMessageBuilder();
            responseMsg.statusSuccess();
            responseMsg.setTotalHits(responseList.size());
            responseList.stream()
                .map(ElasticsearchResponse::getSource)
                .peek(source -> {
                  source.remove(SUMMARY_KEY);
                  source.remove(WORD_VECTOR_KEY);
                })
                .forEach(responseMsg::addResult);

            promise.complete(responseMsg.getResponse());
          } else {
            LOGGER.error("Fail: failed DB request");
            promise.fail(internalErrorResp);
          }
        });
    return promise.future();
  }

  public Future<JsonObject> updateMlayerDomain(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    RespBuilder respBuilder = new RespBuilder();
    String domainId = request.getString(DOMAIN_ID);

    QueryModel query = new QueryModel(QueryType.BOOL);
    query.addMustQuery(
        new QueryModel(QueryType.MATCH, Map.of(FIELD, "domainId.keyword", VALUE, domainId)));
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(query);
    esService.search(mlayerDomainIndex, queryModel)
        .onComplete(checkRes -> {
          if (checkRes.failed()) {
            LOGGER.error("Fail: Check Query Fail");
            promise.fail(internalErrorResp);
          } else {
            LOGGER.debug(checkRes.result());
            if (checkRes.result().size() != 1) {
              LOGGER.error("Fail: Domain does not exist, can't update");
              promise.fail(
                      respBuilder
                          .withType(TYPE_ITEM_NOT_FOUND)
                          .withTitle(TITLE_ITEM_NOT_FOUND)
                          .withResult(domainId, "Fail: Domain doesn't exist, can't update")
                          .withDetail("Fail: Domain doesn't exist, can't update")
                          .getResponse());
              return;
            }

            JsonObject source = checkRes.result().get(0).getSource();

            String parameterIdName = source.getString("name").toLowerCase();
            String requestBodyName = request.getString("name").toLowerCase();
            if (parameterIdName.equals(requestBodyName)) {
              String docId = checkRes.result().get(0).getDocId();
              esService.updateDocument(mlayerDomainIndex, docId, request)
                  .onComplete(putRes -> {
                    if (putRes.succeeded()) {
                      promise.complete(
                              respBuilder
                                  .withType(TYPE_SUCCESS)
                                  .withTitle(SUCCESS)
                                  .withResult(domainId, "Domain Updated Successfully")
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

  public Future<JsonObject> deleteMlayerDomain(String domainId) {
    LOGGER.debug("domainId: " + domainId);

    QueryModel query = new QueryModel(QueryType.BOOL);
    query.addMustQuery(
        new QueryModel(QueryType.MATCH, Map.of(FIELD, "domainId.keyword", VALUE, domainId)));
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(query);

    Promise<JsonObject> promise = Promise.promise();
    RespBuilder respBuilder = new RespBuilder();
    esService.search(mlayerDomainIndex, queryModel)
        .onComplete(checkRes -> {
          if (checkRes.failed()) {
            LOGGER.error("Fail: Check query fail;" + checkRes.cause());
            promise.fail(internalErrorResp);
          } else {
            if (checkRes.result().size() != 1) {
              LOGGER.error("Fail: Domain doesn't exist, can't delete");
              promise.fail(
                      respBuilder
                          .withType(TYPE_ITEM_NOT_FOUND)
                          .withTitle(TITLE_ITEM_NOT_FOUND)
                          .withResult(domainId, "Fail: Domain doesn't exist, can't delete")
                          .withDetail("Fail: Domain doesn't exist, can't delete")
                          .getResponse());
              return;
            }

            String docId = checkRes.result().get(0).getDocId();

            esService.deleteDocument(mlayerDomainIndex, docId)
                .onComplete(putRes -> {
                  if (putRes.succeeded()) {

                    promise.complete(
                            respBuilder
                                .withType(TYPE_SUCCESS)
                                .withTitle(SUCCESS)
                                .withResult(domainId, "Domain deleted Successfully")
                                .getJsonResponse());
                  } else {
                    promise.fail(internalErrorResp);
                    LOGGER.error("Fail: Deletion failed;" + putRes.cause());
                  }
                });
          }
        });
    return promise.future();
  }
}
