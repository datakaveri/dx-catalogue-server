package iudx.catalogue.server.mlayer.util.model;

import static iudx.catalogue.server.database.elastic.model.ElasticsearchResponse.getAggregations;
import static iudx.catalogue.server.database.elastic.util.Constants.DESCRIPTION_ATTR;
import static iudx.catalogue.server.database.elastic.util.Constants.KEYWORD_KEY;
import static iudx.catalogue.server.database.elastic.util.Constants.SUMMARY_KEY;
import static iudx.catalogue.server.database.elastic.util.Constants.WORD_VECTOR_KEY;
import static iudx.catalogue.server.util.Constants.DETAIL_INTERNAL_SERVER_ERROR;
import static iudx.catalogue.server.util.Constants.FIELD;
import static iudx.catalogue.server.util.Constants.ID;
import static iudx.catalogue.server.util.Constants.INSTANCE;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_PROVIDER;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_RESOURCE_GROUP;
import static iudx.catalogue.server.util.Constants.LIMIT;
import static iudx.catalogue.server.util.Constants.MAX_LIMIT;
import static iudx.catalogue.server.util.Constants.NO_CONTENT_AVAILABLE;
import static iudx.catalogue.server.util.Constants.OFFSET;
import static iudx.catalogue.server.util.Constants.PROVIDER;
import static iudx.catalogue.server.util.Constants.RESULTS;
import static iudx.catalogue.server.util.Constants.SUCCESS;
import static iudx.catalogue.server.util.Constants.TITLE;
import static iudx.catalogue.server.util.Constants.TITLE_INTERNAL_SERVER_ERROR;
import static iudx.catalogue.server.util.Constants.TOTAL_HITS;
import static iudx.catalogue.server.util.Constants.TYPE;
import static iudx.catalogue.server.util.Constants.TYPE_INTERNAL_SERVER_ERROR;
import static iudx.catalogue.server.util.Constants.TYPE_SUCCESS;
import static iudx.catalogue.server.util.Constants.VALUE;
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
import iudx.catalogue.server.database.elastic.util.AggregationType;
import iudx.catalogue.server.database.elastic.util.QueryType;
import iudx.catalogue.server.database.util.Util;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MlayerProvider {
  private static final Logger LOGGER = LogManager.getLogger(MlayerProvider.class);
  private static String internalErrorResp =
      new RespBuilder()
          .withType(TYPE_INTERNAL_SERVER_ERROR)
          .withTitle(TITLE_INTERNAL_SERVER_ERROR)
          .withDetail(DETAIL_INTERNAL_SERVER_ERROR)
          .getResponse();
  ElasticsearchService esService;
  String docIndex;

  public MlayerProvider(ElasticsearchService esService, String docIndex) {
    this.esService = esService;
    this.docIndex = docIndex;
  }

  public Future<JsonObject> getMlayerProviders(JsonObject requestParams) {
    Promise<JsonObject> promise = Promise.promise();
    String limit = requestParams.getString(LIMIT);
    String offset = requestParams.getString(OFFSET);
    if (requestParams.containsKey(INSTANCE)) {

      QueryModel resourceGroupQuery = new QueryModel(QueryType.BOOL);
      resourceGroupQuery.addMustQuery(new QueryModel(QueryType.MATCH, Map.of(FIELD,
          "type.keyword", VALUE, "iudx:ResourceGroup")));
      resourceGroupQuery.addMustQuery(new QueryModel(QueryType.MATCH, Map.of(FIELD,
          "instance.keyword", VALUE, requestParams.getString(INSTANCE))));

      QueryModel providerQuery = new QueryModel(QueryType.BOOL);
      providerQuery.addMustQuery(new QueryModel(QueryType.MATCH, Map.of(FIELD, "type.keyword",
          VALUE, "iudx:Provider")));

      QueryModel mainQuery = new QueryModel(QueryType.BOOL);
      mainQuery.setShouldQueries(List.of(resourceGroupQuery, providerQuery));

      Map<String, Object> aggsParams = new HashMap<>();
      aggsParams.put(FIELD, PROVIDER + KEYWORD_KEY);
      QueryModel aggs = new QueryModel(AggregationType.CARDINALITY, aggsParams);
      aggs.setAggregationName("provider_count");

      mainQuery.setLimit(MAX_LIMIT);

      QueryModel queryModel = new QueryModel();
      queryModel.setQueries(mainQuery);
      queryModel.setAggregations(List.of(aggs));
      queryModel.setIncludeFields(
          List.of(
              "id", "description", "type", "resourceGroup",
              "accessPolicy", "provider", "itemCreatedAt",
              "instance", "label"
          )
      );
      queryModel.setLimit(MAX_LIMIT);

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
      QueryModel query =
          new QueryModel(QueryType.MATCH, Map.of(FIELD, "type.keyword", VALUE, "iudx:Provider"));
      QueryModel queryModel = new QueryModel();
      queryModel.setQueries(query);
      queryModel.setIncludeFields(List.of("id", "description"));
      queryModel.setLimit(limit);
      queryModel.setOffset(offset);

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
