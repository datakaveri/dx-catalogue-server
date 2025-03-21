package iudx.catalogue.server.mlayer.service;

import static iudx.catalogue.server.database.elastic.model.ElasticsearchResponse.getAggregations;
import static iudx.catalogue.server.database.elastic.util.Constants.DESCRIPTION_ATTR;
import static iudx.catalogue.server.database.elastic.util.Constants.SUMMARY_KEY;
import static iudx.catalogue.server.database.elastic.util.Constants.WORD_VECTOR_KEY;
import static iudx.catalogue.server.mlayer.util.QueryModelUtil.createProviderAndResourceQuery;
import static iudx.catalogue.server.mlayer.util.QueryModelUtil.getMlayerProviderQuery;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.validator.util.Constants.VALIDATION_FAILURE_MSG;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.common.RespBuilder;
import iudx.catalogue.server.common.util.DbResponseMessageBuilder;
import iudx.catalogue.server.database.elastic.model.ElasticsearchResponse;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
import iudx.catalogue.server.database.util.Util;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MlayerProviderService {
  private static final Logger LOGGER = LogManager.getLogger(MlayerProviderService.class);
  private static final String internalErrorResp =
      new RespBuilder()
          .withType(TYPE_INTERNAL_SERVER_ERROR)
          .withTitle(TITLE_INTERNAL_SERVER_ERROR)
          .withDetail(DETAIL_INTERNAL_SERVER_ERROR)
          .getResponse();
  private final ElasticsearchService esService;
  String docIndex;

  public MlayerProviderService(ElasticsearchService esService, String docIndex) {
    this.esService = esService;
    this.docIndex = docIndex;
  }

  public Future<JsonObject> getMlayerProviders(JsonObject requestParams) {
    Promise<JsonObject> promise = Promise.promise();
    String limit = requestParams.getString(LIMIT);
    String offset = requestParams.getString(OFFSET);

    QueryModel queryModel =  requestParams.containsKey(INSTANCE)
        ? createProviderAndResourceQuery(requestParams.getString(INSTANCE))
        : getMlayerProviderQuery(limit, offset);
    if (requestParams.containsKey(INSTANCE)) {
      esService.search(docIndex, queryModel)
          .onComplete(resultHandler -> {
            if (resultHandler.succeeded()) {
              LOGGER.debug("Success: Successful DB Request");
              if (resultHandler.result().isEmpty()) {
                promise.fail(NO_CONTENT_AVAILABLE);
              }
              JsonObject result = new JsonObject();
              if (getAggregations() != null) {
                int providerCount =
                    getAggregations().getJsonObject("provider_count")
                        .getInteger(VALUE);
                result.put("providerCount", providerCount);
              }
              JsonArray resourceGroupAndProvider = new JsonArray();
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
                  .forEach(resourceGroupAndProvider::add);
              result.put("resourceGroupAndProvider", resourceGroupAndProvider);
              responseMsg.addResult(result);

              // providerCount depicts the number of provider associated with the instance
              Integer providerCount = responseMsg.getResponse()
                  .getJsonArray(RESULTS)
                  .getJsonObject(0)
                  .getInteger("providerCount");
              LOGGER.debug("provider Count {} ", providerCount);
              // results consists of all providers and resource groups belonging to instance
              JsonArray results = responseMsg.getResponse()
                  .getJsonArray(RESULTS)
                  .getJsonObject(0)
                  .getJsonArray("resourceGroupAndProvider");
              int resultSize = results.size();
              // 'allProviders' is a mapping of provider IDs to their corresponding JSON objects
              Map<String, JsonObject> allProviders = new HashMap<>();
              JsonArray providersList = new JsonArray();
              // creating mapping of all provider IDs to their corresponding JSON objects
              for (int i = 0; i < resultSize; i++) {
                JsonObject provider = results.getJsonObject(i);
                String itemType = Util.getItemType(provider);
                if (itemType.equals(VALIDATION_FAILURE_MSG)) {
                  promise.fail(VALIDATION_FAILURE_MSG);
                }
                if (ITEM_TYPE_PROVIDER.equals(itemType)) {
                  allProviders.put(
                      provider.getString(ID),
                      new JsonObject()
                          .put(ID, provider.getString(ID))
                          .put(DESCRIPTION_ATTR, provider.getString(DESCRIPTION_ATTR)));
                }
              }
              // filtering out providers which belong to the instance from all providers map.
              for (int i = 0; i < resultSize; i++) {
                JsonObject resourceGroup = results.getJsonObject(i);
                String itemType = Util.getItemType(resourceGroup);
                if (itemType.equals(VALIDATION_FAILURE_MSG)) {
                  promise.fail(VALIDATION_FAILURE_MSG);
                }
                if (ITEM_TYPE_RESOURCE_GROUP.equals(itemType)
                    && allProviders.containsKey(resourceGroup.getString(PROVIDER))) {
                  providersList.add(allProviders.get(resourceGroup.getString(PROVIDER)));
                  allProviders.remove(resourceGroup.getString(PROVIDER));
                }
              }
              LOGGER.debug("provider belonging to instance are {} ", providersList);
              // Pagination applied to the final response.
              int endIndex = requestParams.getInteger(LIMIT) + requestParams.getInteger(OFFSET);
              if (endIndex >= providerCount) {
                if (requestParams.getInteger(OFFSET) >= providerCount) {
                  LOGGER.debug("Offset value has exceeded total hits");
                  JsonObject response =
                      new JsonObject()
                          .put(TYPE, TYPE_SUCCESS)
                          .put(TITLE, SUCCESS)
                          .put(TOTAL_HITS, providerCount);
                  promise.complete(response);
                } else {
                  endIndex = providerCount;
                }
              }
              JsonArray pagedProviders = new JsonArray();
              for (int i = requestParams.getInteger(OFFSET); i < endIndex; i++) {
                pagedProviders.add(providersList.getJsonObject(i));
              }
              JsonObject response =
                  new JsonObject()
                      .put(TYPE, TYPE_SUCCESS)
                      .put(TITLE, SUCCESS)
                      .put(TOTAL_HITS, providerCount)
                      .put(RESULTS, pagedProviders);
              promise.complete(response);

            } else {
              LOGGER.error("Fail: failed DB request");
              promise.fail(internalErrorResp);
            }
          });
    } else {

      esService.search(docIndex, queryModel)
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
    }
    return promise.future();
  }
}
