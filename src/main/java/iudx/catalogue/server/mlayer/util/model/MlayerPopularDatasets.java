package iudx.catalogue.server.mlayer.util.model;

import static iudx.catalogue.server.database.elastic.model.ElasticsearchResponse.getAggregations;
import static iudx.catalogue.server.database.elastic.util.Constants.*;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.util.Constants.KEYWORD_KEY;
import static iudx.catalogue.server.validator.util.Constants.VALIDATION_FAILURE_MSG;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import iudx.catalogue.server.common.RespBuilder;
import iudx.catalogue.server.common.util.DbResponseMessageBuilder;
import iudx.catalogue.server.database.elastic.model.ElasticsearchResponse;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
import iudx.catalogue.server.database.elastic.util.AggregationType;
import iudx.catalogue.server.database.elastic.util.QueryType;
import iudx.catalogue.server.database.util.Util;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MlayerPopularDatasets {
  private static final Logger LOGGER = LogManager.getLogger(MlayerPopularDatasets.class);
  private static final String internalErrorResp =
      new RespBuilder()
          .withType(TYPE_INTERNAL_SERVER_ERROR)
          .withTitle(TITLE_INTERNAL_SERVER_ERROR)
          .withDetail(DETAIL_INTERNAL_SERVER_ERROR)
          .getResponse();
  private final WebClient webClient;
  ElasticsearchService esService;
  String docIndex;
  String mlayerInstanceIndex;
  String mlayerDomainIndex;

  public MlayerPopularDatasets(
      WebClient webClient,
      ElasticsearchService esService,
      String docIndex,
      String mlayerInstanceIndex,
      String mlayerDomainIndex) {
    this.webClient = webClient;
    this.esService = esService;
    this.docIndex = docIndex;
    this.mlayerInstanceIndex = mlayerInstanceIndex;
    this.mlayerDomainIndex = mlayerDomainIndex;
  }

  public Future<JsonObject> getMlayerPopularDatasets(
      String instance, JsonArray frequentlyUsedResourceGroup) {
    Promise<JsonObject> promise = Promise.promise();

    Promise<JsonObject> instanceResult = Promise.promise();
    Promise<JsonArray> domainResult = Promise.promise();
    Promise<JsonObject> datasetResult = Promise.promise();

    searchSortedMlayerInstances(instanceResult);
    if (instance.isBlank()) {
      datasets(datasetResult, frequentlyUsedResourceGroup);
    } else {
      datasets(instance, datasetResult, frequentlyUsedResourceGroup);
    }
    allMlayerDomains(domainResult);
    Future.all(instanceResult.future(), domainResult.future(), datasetResult.future())
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                JsonObject instanceList = ar.result().resultAt(0);
                JsonObject datasetJson = ar.result().resultAt(2);
                for (int i = 0; i < datasetJson.getJsonArray("latestDataset").size(); i++) {
                  if (datasetJson
                      .getJsonArray("latestDataset")
                      .getJsonObject(i)
                      .containsKey(INSTANCE)) {
                    LOGGER.debug("given dataset has associated instance");
                    datasetJson
                        .getJsonArray("latestDataset")
                        .getJsonObject(i)
                        .put(
                            "icon",
                            instanceList
                                .getJsonObject("instanceIconPath")
                                .getString(
                                    datasetJson
                                        .getJsonArray("latestDataset")
                                        .getJsonObject(i)
                                        .getString(INSTANCE)
                                        .toLowerCase()));
                  } else {
                    LOGGER.debug("given dataset does not have associated instance");
                    datasetJson.getJsonArray("latestDataset").getJsonObject(i).put("icon", "");
                  }
                }
                for (int i = 0; i < datasetJson.getJsonArray("featuredDataset").size(); i++) {
                  if (datasetJson
                      .getJsonArray("featuredDataset")
                      .getJsonObject(i)
                      .containsKey(INSTANCE)) {
                    datasetJson
                        .getJsonArray("featuredDataset")
                        .getJsonObject(i)
                        .put(
                            "icon",
                            instanceList
                                .getJsonObject("instanceIconPath")
                                .getString(
                                    datasetJson
                                        .getJsonArray("featuredDataset")
                                        .getJsonObject(i)
                                        .getString(INSTANCE)));
                  } else {
                    datasetJson.getJsonArray("featuredDataset").getJsonObject(i).put("icon", "");
                  }
                }
                JsonArray domainList = ar.result().resultAt(1);
                JsonObject result = new JsonObject();
                result.mergeIn(datasetJson.getJsonObject("typeCount"));
                result
                    .put("totalInstance", instanceList.getInteger("totalInstance"))
                    .put("totalDomain", domainList.size())
                    .put("domains", domainList)
                    .put(INSTANCE, instanceList.getJsonArray("instanceList"))
                    .put("featuredDataset", datasetJson.getJsonArray("featuredDataset"))
                    .put("latestDataset", datasetJson.getJsonArray("latestDataset"));

                RespBuilder respBuilder =
                    new RespBuilder().withType(TYPE_SUCCESS).withTitle(SUCCESS).withResult(result);
                promise.complete(respBuilder.getJsonResponse());
              } else {
                LOGGER.error("Fail: failed DB request");
                if (ar.cause().getMessage().equals("No Content Available")) {
                  promise.fail(ar.cause().getMessage());
                }
                promise.fail(internalErrorResp);
              }
            });
    return promise.future();
  }

  private void searchSortedMlayerInstances(Promise<JsonObject> instanceResult) {
    QueryModel queryModel = new QueryModel();
    QueryModel query = new QueryModel(QueryType.MATCH_ALL);
    queryModel.setQueries(query);
    queryModel.setIncludeFields(List.of("name", "cover", "icon"));
    queryModel.setLimit(MAX_LIMIT);
    queryModel.setSortFields(Map.of("name", "asc"));
    esService
        .search(mlayerInstanceIndex, queryModel)
        .onComplete(
            resultHandler -> {
              if (resultHandler.succeeded()) {
                int totalInstance = resultHandler.result().size();
                Map<String, String> instanceIconPath = new HashMap<>();
                JsonArray instanceList = new JsonArray();
                for (int i = 0; i < resultHandler.result().size(); i++) {
                  JsonObject instance = resultHandler.result().get(i).getSource();
                  instanceIconPath.put(
                      instance.getString("name").toLowerCase(), instance.getString("icon"));
                  if (i < 4) {
                    instanceList.add(i, instance);
                  }
                }

                JsonObject json =
                    new JsonObject()
                        .put("instanceIconPath", instanceIconPath)
                        .put("instanceList", instanceList)
                        .put("totalInstance", totalInstance);

                instanceResult.complete(json);
              } else {
                LOGGER.error("Fail: failed instances DB request");
                instanceResult.handle(Future.failedFuture(internalErrorResp));
              }
            });
  }

  private void allMlayerDomains(Promise<JsonArray> domainResult) {
    QueryModel getAllMlayerDomainQueryModel = new QueryModel();
    QueryModel query = new QueryModel(QueryType.MATCH_ALL);
    getAllMlayerDomainQueryModel.setQueries(query);
    getAllMlayerDomainQueryModel.setIncludeFields(
        List.of("domainId", "description", "icon", "label", "name"));
    getAllMlayerDomainQueryModel.setLimit(MAX_LIMIT);
    getAllMlayerDomainQueryModel.setOffset("0");
    esService
        .search(mlayerDomainIndex, getAllMlayerDomainQueryModel)
        .onComplete(
            getDomainHandler -> {
              if (getDomainHandler.succeeded()) {
                JsonArray domainList = new JsonArray();
                getDomainHandler.result().stream()
                    .map(ElasticsearchResponse::getSource)
                    .peek(
                        source -> {
                          source.remove(SUMMARY_KEY);
                          source.remove(WORD_VECTOR_KEY);
                        })
                    .forEach(domainList::add);
                domainResult.complete(domainList);
              } else {
                LOGGER.error("Fail: failed domains DB request");
                domainResult.handle(Future.failedFuture(internalErrorResp));
              }
            });
  }

  private void datasets(
      String instance, Promise<JsonObject> datasetResult, JsonArray frequentlyUsedResourceGroup) {

    QueryModel resourceGroupQuery = new QueryModel(QueryType.BOOL);
    resourceGroupQuery.addMustQuery(
        new QueryModel(
            QueryType.MATCH, Map.of(FIELD, "type.keyword", VALUE, "iudx:ResourceGroup")));
    resourceGroupQuery.addMustQuery(
        new QueryModel(QueryType.MATCH, Map.of(FIELD, "instance.keyword", VALUE, instance)));

    QueryModel providerQuery = new QueryModel(QueryType.BOOL);
    providerQuery.addMustQuery(
        new QueryModel(QueryType.MATCH, Map.of(FIELD, "type.keyword", VALUE, "iudx:Provider")));

    QueryModel query = new QueryModel(QueryType.BOOL);
    query.setShouldQueries(List.of(resourceGroupQuery, providerQuery));
    Map<String, Object> aggsParams = new HashMap<>();
    aggsParams.put(FIELD, PROVIDER + KEYWORD_KEY);
    QueryModel aggs = new QueryModel(AggregationType.CARDINALITY, aggsParams);
    aggs.setAggregationName("provider_count");
    QueryModel providerAndResourcesQueryModel = new QueryModel();

    providerAndResourcesQueryModel.setQueries(query);
    providerAndResourcesQueryModel.setAggregations(List.of(aggs));
    providerAndResourcesQueryModel.setIncludeFields(
        List.of(
            "id",
            "description",
            "type",
            "resourceGroup",
            "accessPolicy",
            "provider",
            "itemCreatedAt",
            "instance",
            "label"));
    providerAndResourcesQueryModel.setLimit(MAX_LIMIT);
    esService
        .search(docIndex, providerAndResourcesQueryModel)
        .onComplete(
            getCatRecords -> {
              if (getCatRecords.succeeded()) {
                DbResponseMessageBuilder responseMsg = new DbResponseMessageBuilder();
                if (getCatRecords.result().isEmpty()) {
                  datasetResult.handle(Future.failedFuture(NO_CONTENT_AVAILABLE));
                }

                JsonObject result = new JsonObject();
                JsonArray resourceGroupAndProvider = new JsonArray();

                if (getAggregations() != null) {
                  int providerCount =
                      getAggregations().getJsonObject("provider_count").getInteger(VALUE);
                  result.put("providerCount", providerCount);
                }

                List<ElasticsearchResponse> responseList = getCatRecords.result();
                responseMsg.statusSuccess();
                responseList.stream()
                    .map(ElasticsearchResponse::getSource)
                    .peek(
                        source -> {
                          source.remove(SUMMARY_KEY);
                          source.remove(WORD_VECTOR_KEY);
                        })
                    .forEach(resourceGroupAndProvider::add);
                result.put("resourceGroupAndProvider", resourceGroupAndProvider);
                responseMsg.addResult(result);

                JsonArray results =
                    responseMsg
                        .getResponse()
                        .getJsonArray(RESULTS)
                        .getJsonObject(0)
                        .getJsonArray("resourceGroupAndProvider");
                int resultSize = results.size();

                Promise<JsonObject> resourceCount = Promise.promise();
                MlayerDataset mlayerDataset =
                    new MlayerDataset(webClient, esService, docIndex, mlayerInstanceIndex);
                // function to get the resource group items count
                QueryModel resourceApQueryModel = getResourceApQueryModel();
                mlayerDataset.gettingResourceAccessPolicyCount(resourceApQueryModel, resourceCount);
                resourceCount
                    .future()
                    .onComplete(
                        handler -> {
                          if (handler.succeeded()) {
                            JsonObject resourceItemCount =
                                resourceCount.future().result().getJsonObject("resourceItemCount");
                            JsonObject resourceAccessPolicy =
                                resourceCount
                                    .future()
                                    .result()
                                    .getJsonObject("resourceAccessPolicy");
                            int totalResourceItem = 0;
                            ArrayList<JsonObject> latestDatasetArray = new ArrayList<JsonObject>();
                            Map<String, JsonObject> resourceGroupMap = new HashMap<>();
                            Map<String, String> providerDescription = new HashMap<>();
                            for (int i = 0; i < resultSize; i++) {
                              JsonObject record = results.getJsonObject(i);
                              String itemType = Util.getItemType(record);
                              if (itemType.equals(VALIDATION_FAILURE_MSG)) {
                                datasetResult.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
                              }
                              // making a map of all resource group and provider id and its
                              // description
                              if (ITEM_TYPE_RESOURCE_GROUP.equals(itemType)) {
                                String id = record.getString(ID);
                                int resourceItemCountInGroup =
                                    resourceItemCount.containsKey(id)
                                        ? resourceItemCount.getInteger(id)
                                        : 0;
                                record.put("totalResources", resourceItemCountInGroup);
                                if (resourceAccessPolicy.containsKey(id)) {
                                  record.put(
                                      "accessPolicy", resourceAccessPolicy.getJsonObject(id));
                                } else {
                                  record.put(
                                      "accessPolicy",
                                      new JsonObject()
                                          .put("PII", 0)
                                          .put("SECURE", 0)
                                          .put("OPEN", 0));
                                }

                                // getting total count of resource items
                                totalResourceItem = totalResourceItem + resourceItemCountInGroup;
                                if (record.containsKey("itemCreatedAt")) {
                                  latestDatasetArray.add(record);
                                }
                                resourceGroupMap.put(record.getString(ID), record);
                              } else if (ITEM_TYPE_PROVIDER.equals(itemType)) {
                                String description = record.getString(DESCRIPTION_ATTR);
                                String providerId = record.getString(ID);

                                providerDescription.put(providerId, description);
                              }
                            }
                            // sorting resource group based on the time of creation.
                            Collections.sort(latestDatasetArray, comapratorForLatestDataset());

                            JsonObject typeCount =
                                new JsonObject()
                                    .put("totalDatasets", resourceGroupMap.size())
                                    .put("totalResources", totalResourceItem);

                            if (instance.isBlank()) {
                              typeCount.put("totalPublishers", providerDescription.size());
                            } else {
                              typeCount.put(
                                  "totalPublishers",
                                  responseMsg
                                      .getResponse()
                                      .getJsonArray(RESULTS)
                                      .getJsonObject(0)
                                      .getInteger("providerCount"));
                            }

                            // making an arrayList of top six latest resource group
                            ArrayList<JsonObject> latestResourceGroup = new ArrayList<>();
                            int resourceGroupSize = Math.min(latestDatasetArray.size(), 6);
                            for (int i = 0; i < resourceGroupSize; i++) {
                              JsonObject resourceGroup = latestDatasetArray.get(i);
                              resourceGroup.put(
                                  "providerDescription",
                                  providerDescription.get(
                                      latestDatasetArray.get(i).getString(PROVIDER)));

                              latestResourceGroup.add(resourceGroup);
                            }

                            // making array list of most accessed resource groups
                            ArrayList<JsonObject> featuredResourceGroup = new ArrayList<>();
                            for (int resourceIndex = 0;
                                resourceIndex < frequentlyUsedResourceGroup.size();
                                resourceIndex++) {
                              String id = frequentlyUsedResourceGroup.getString(resourceIndex);
                              if (resourceGroupMap.containsKey(id)) {
                                JsonObject resourceGroup = resourceGroupMap.get(id);
                                resourceGroup.put(
                                    "providerDescription",
                                    providerDescription.get(resourceGroup.getString(PROVIDER)));
                                featuredResourceGroup.add(resourceGroup);
                                // removing the resourceGroup from resourceGroupMap after
                                // resources added to featuredResourceGroup array
                                resourceGroupMap.remove(id);
                              }
                            }

                            // Determining the number of resource group that can be added if
                            // total featured datasets are not 6. Max value is 6.
                            int remainingResources =
                                Math.min(6 - featuredResourceGroup.size(), resourceGroupMap.size());

                            /* Iterate through the values of 'resourceGroupMap' to add resources
                              to 'featuredResourceGroup' array while ensuring we don't exceed the
                              'remainingResources' limit. For each resource, we update its
                              'providerDescription' before adding it to the group.
                            */
                            for (JsonObject resourceGroup : resourceGroupMap.values()) {
                              if (remainingResources <= 0) {
                                break; // No need to continue if we've added enough resources
                              }
                              resourceGroup.put(
                                  "providerDescription",
                                  providerDescription.get(resourceGroup.getString("provider")));

                              featuredResourceGroup.add(resourceGroup);
                              remainingResources--;
                            }

                            JsonObject jsonDataset =
                                new JsonObject()
                                    .put("latestDataset", latestResourceGroup)
                                    .put("typeCount", typeCount)
                                    .put("featuredDataset", featuredResourceGroup);
                            datasetResult.complete(jsonDataset);

                          } else {
                            LOGGER.error("Fail: failed resourceCount DB request");
                            datasetResult.handle(Future.failedFuture(internalErrorResp));
                          }
                        });

              } else {
                LOGGER.error("Fail: failed datasets DB request");
                datasetResult.handle(Future.failedFuture(internalErrorResp));
              }
            });
  }

  /**
   * This API forms the response when an instance is not provided. The process involves several
   * steps: 1) Retrieve the top six newly created resource groups, along with counts of associated
   * RIs, RGs, and providers from Elasticsearch
   *
   * <p>2) Using the frequentlyUsedResourceGroup array, gather all resource groups and providers
   * listed in that array from Elasticsearch Then, create a HashMap that maps provider IDs to their
   * descriptions.
   *
   * <p>3) Populate the popularDataset array, ensuring it contains at least six items.
   *
   * <p>4) Assemble an array of twelve resource groups (six from popular resource groups and six
   * from latest resource groups). Retrieve their resource item counts and access policies from
   * Elasticsearch.
   *
   * <p>5) Using the Elasticsearch results, populate all items with their access policies and total
   * resource counts.
   *
   * @param datasetResult the resulting dataset to be completed
   * @param frequentlyUsedResourceGroup an array of frequently used resource groups
   */
  private void datasets(Promise<JsonObject> datasetResult, JsonArray frequentlyUsedResourceGroup) {

    // Main query
    QueryModel mainQuery = new QueryModel(QueryType.BOOL);
    mainQuery.addMustQuery(
        new QueryModel(QueryType.MATCH, Map.of(FIELD, "type", VALUE, "iudx:ResourceGroup")));

    QueryModel finalQueryModel = new QueryModel();
    finalQueryModel.setQueries(mainQuery);
    List<QueryModel> aggs = getResourceGroupCountAgg();
    finalQueryModel.setAggregations(aggs);
    finalQueryModel.setSortFields(Map.of("itemCreatedAt", "desc"));
    finalQueryModel.setIncludeFields(
        List.of(
            "id",
            "description",
            "type",
            "resourceGroup",
            "accessPolicy",
            "provider",
            "itemCreatedAt",
            "instance",
            "label"));
    finalQueryModel.setLimit("6");
    finalQueryModel.setOffset("0");

    esService
        .search(docIndex, finalQueryModel)
        .onComplete(
            latestRgHandler -> {
              if (latestRgHandler.succeeded()) {
                if (latestRgHandler.result().isEmpty()) {
                  LOGGER.debug("RGs not present");
                  datasetResult.handle(Future.failedFuture(NO_CONTENT_AVAILABLE));
                }
                JsonObject aggregations = getAggregations();
                JsonObject aggregationResult = new JsonObject();
                aggregationResult =
                    new JsonObject()
                        .put(
                            RESOURCE_GROUP_COUNT,
                            aggregations.getJsonObject(RESOURCE_GROUP_COUNT).getInteger(DOC_COUNT))
                        .put(
                            RESOURCE_COUNT,
                            aggregations
                                .getJsonObject(RESOURCE_COUNT)
                                .getJsonObject("Resources")
                                .getInteger(DOC_COUNT))
                        .put(
                            PROVIDER_COUNT,
                            aggregations
                                .getJsonObject(PROVIDER_COUNT)
                                .getJsonObject("Providers")
                                .getInteger(DOC_COUNT));
                List<ElasticsearchResponse> responseList = latestRgHandler.result();
                DbResponseMessageBuilder responseMsg = new DbResponseMessageBuilder();
                responseMsg.statusSuccess();
                responseList.stream()
                    .map(ElasticsearchResponse::getSource)
                    .peek(
                        source -> {
                          source.remove(SUMMARY_KEY);
                          source.remove(WORD_VECTOR_KEY);
                        })
                    .forEach(responseMsg::addResult);
                responseMsg.getResponse().put("count", aggregationResult);

                QueryModel termsQuery = new QueryModel(QueryType.TERMS,
                    Map.of(FIELD, ID_KEYWORD, VALUE, frequentlyUsedResourceGroup.getList()));
                QueryModel termQuery = new QueryModel(QueryType.TERM,
                    Map.of(FIELD, "type.keyword", VALUE, "iudx:Provider"));

                QueryModel query = new QueryModel(QueryType.BOOL);
                query.setShouldQueries(List.of(termsQuery, termQuery));
                QueryModel providerAndPopularRgs = new QueryModel();
                providerAndPopularRgs.setQueries(query);
                providerAndPopularRgs.setIncludeFields(
                    List.of(
                        "id",
                        "description",
                        "type",
                        "resourceGroup",
                        "accessPolicy",
                        "provider",
                        "itemCreatedAt",
                        "instance",
                        "label"));
                providerAndPopularRgs.setLimit(MAX_LIMIT);

                esService
                    .search(docIndex, providerAndPopularRgs)
                    .onComplete(
                        providerAndPopularRgHandler -> {
                          if (providerAndPopularRgHandler.succeeded()) {
                            List<ElasticsearchResponse> respList =
                                providerAndPopularRgHandler.result();
                            DbResponseMessageBuilder respMsg = new DbResponseMessageBuilder();
                            respMsg.statusSuccess();
                            respList.stream()
                                .map(ElasticsearchResponse::getSource)
                                .peek(
                                    source -> {
                                      source.remove(SUMMARY_KEY);
                                      source.remove(WORD_VECTOR_KEY);
                                    })
                                .forEach(respMsg::addResult);
                            if (respMsg.getResponse().getJsonArray(RESULTS).isEmpty()) {
                              LOGGER.debug("Providers and RGs not present");
                              datasetResult.handle(Future.failedFuture(NO_CONTENT_AVAILABLE));
                            }
                            Map<String, String> providerDetails = new HashMap<>();
                            JsonArray popularDatasets = new JsonArray();

                            for (int count = 0;
                                count < respMsg.getResponse().getJsonArray(RESULTS).size();
                                count++) {

                              JsonObject resultItem =
                                  respMsg.getResponse().getJsonArray(RESULTS).getJsonObject(count);
                              String itemType = Util.getItemType(resultItem);
                              if (itemType.equals(VALIDATION_FAILURE_MSG)) {
                                datasetResult.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
                              }

                              if (ITEM_TYPE_PROVIDER.equals(itemType)) {

                                providerDetails.put(
                                    resultItem.getString(ID),
                                    resultItem.getString(DESCRIPTION_ATTR));
                              } else if (ITEM_TYPE_RESOURCE_GROUP.equals(itemType)) {
                                popularDatasets.add(resultItem);
                              }
                            }
                            JsonArray latestDatasets =
                                responseMsg.getResponse().getJsonArray(RESULTS);
                            int popularDatasetCount = 0;
                            int datasetIndex = 0;
                            if (popularDatasets.size() < 6) {
                              popularDatasetCount = latestDatasets.size();
                            } else {
                              popularDatasetCount = POPULAR_DATASET_COUNT;
                            }
                            while (popularDatasets.size() < popularDatasetCount) {

                              if (!frequentlyUsedResourceGroup.contains(
                                  latestDatasets.getJsonObject(datasetIndex).getString("id"))) {
                                popularDatasets.add(latestDatasets.getJsonObject(datasetIndex));
                                datasetIndex++;
                              }
                            }
                            JsonArray allRgId = new JsonArray();
                            for (int count = 0; count < popularDatasets.size(); count++) {
                              allRgId.add(popularDatasets.getJsonObject(count).getString(ID));
                              allRgId.add(latestDatasets.getJsonObject(count).getString(ID));
                            }
                            // Aggregation: accessPolicy_count
                            QueryModel accessPolicyCountAgg =
                                new QueryModel(
                                    AggregationType.VALUE_COUNT,
                                    Map.of(FIELD, "accessPolicy.keyword"));

                            // Aggregation: access_policies
                            QueryModel accessPoliciesAgg =
                                new QueryModel(
                                    AggregationType.TERMS,
                                    Map.of(FIELD, "accessPolicy.keyword", SIZE_KEY, 10000));
                            accessPoliciesAgg.setAggregationsMap(
                                Map.of("accessPolicy_count", accessPolicyCountAgg));

                            // Aggregation: resource_count
                            QueryModel resourceCountAgg =
                                new QueryModel(
                                    AggregationType.VALUE_COUNT, Map.of(FIELD, "id.keyword"));

                            // Aggregation: results
                            QueryModel resultsAgg =
                                new QueryModel(
                                    AggregationType.TERMS,
                                    Map.of(FIELD, "resourceGroup.keyword", SIZE_KEY, 10000));
                            resultsAgg.setAggregationName("results");
                            resultsAgg.setAggregationsMap(
                                Map.of(
                                    "access_policies",
                                    accessPoliciesAgg,
                                    "resource_count",
                                    resourceCountAgg));
                            // Base Query: Terms Query
                            QueryModel termsQueryModel = new QueryModel(QueryType.TERMS,
                                Map.of(FIELD, "resourceGroup.keyword", VALUE, allRgId));
                            // Final QueryModel
                            QueryModel getCategorizedResourceAP = new QueryModel();
                            getCategorizedResourceAP.setQueries(termsQueryModel);
                            getCategorizedResourceAP.setAggregations(List.of(resultsAgg));
                            getCategorizedResourceAP.setLimit("0");

                            Promise<JsonObject> resourceCount = Promise.promise();
                            MlayerDataset mlayerDataset =
                                new MlayerDataset(
                                    webClient, esService, docIndex, mlayerInstanceIndex);
                            mlayerDataset.gettingResourceAccessPolicyCount(
                                getCategorizedResourceAP, resourceCount);

                            resourceCount
                                .future()
                                .onComplete(
                                    handler -> {
                                      if (handler.succeeded()) {
                                        JsonObject resourceItemCount =
                                            resourceCount
                                                .future()
                                                .result()
                                                .getJsonObject("resourceItemCount");
                                        JsonObject resourceAccessPolicy =
                                            resourceCount
                                                .future()
                                                .result()
                                                .getJsonObject("resourceAccessPolicy");

                                        int totalResourceItem = 0;
                                        for (JsonArray datasets :
                                            Arrays.asList(popularDatasets, latestDatasets)) {
                                          for (int i = 0; i < datasets.size(); i++) {
                                            JsonObject datasetRecord = datasets.getJsonObject(i);
                                            String itemType = Util.getItemType(datasetRecord);

                                            if (itemType.equals(VALIDATION_FAILURE_MSG)) {
                                              datasetResult.handle(
                                                  Future.failedFuture(VALIDATION_FAILURE_MSG));
                                              datasetResult.handle(
                                                  Future.failedFuture(VALIDATION_FAILURE_MSG));
                                            }

                                            if (ITEM_TYPE_RESOURCE_GROUP.equals(itemType)) {
                                              String rgId = datasetRecord.getString(ID);
                                              String providerId = datasetRecord.getString(PROVIDER);
                                              int resourceItemCountInGroup =
                                                  resourceItemCount.containsKey(rgId)
                                                      ? resourceItemCount.getInteger(rgId)
                                                      : 0;
                                              datasetRecord.put(
                                                  "totalResources", resourceItemCountInGroup);
                                              datasetRecord.put(
                                                  "providerDescription",
                                                  providerDetails.get(providerId));

                                              JsonObject accessPolicy =
                                                  resourceAccessPolicy.containsKey(rgId)
                                                      ? resourceAccessPolicy.getJsonObject(rgId)
                                                      : new JsonObject()
                                                          .put("PII", 0)
                                                          .put("SECURE", 0)
                                                          .put("OPEN", 0);
                                              datasetRecord.put("accessPolicy", accessPolicy);

                                              totalResourceItem += resourceItemCountInGroup;
                                            }
                                          }
                                        }
                                        int resourceGroupCount =
                                            responseMsg
                                                .getResponse()
                                                .getJsonObject("count")
                                                .getInteger("resourceGroupCount");
                                        int resourcesCount =
                                            responseMsg
                                                .getResponse()
                                                .getJsonObject("count")
                                                .getInteger("resourceCount");
                                        int providerCount =
                                            responseMsg
                                                .getResponse()
                                                .getJsonObject("count")
                                                .getInteger("providerCount");
                                        JsonObject typeCount =
                                            new JsonObject()
                                                .put("totalDatasets", resourceGroupCount)
                                                .put("totalResources", resourcesCount);
                                        typeCount.put("totalPublishers", providerCount);

                                        JsonObject jsonDataset =
                                            new JsonObject()
                                                .put("latestDataset", latestDatasets)
                                                .put("typeCount", typeCount)
                                                .put("featuredDataset", popularDatasets);
                                        datasetResult.complete(jsonDataset);
                                      }
                                    });
                          } else {
                            LOGGER.error("Fail: failed providerAndPopularRg DB request");
                            datasetResult.handle(Future.failedFuture(internalErrorResp));
                          }
                        });
              } else {
                LOGGER.error("Fail: failed latestRG DB request");
                datasetResult.handle(Future.failedFuture(internalErrorResp));
              }
            });
  }

  private QueryModel getResourceApQueryModel() {
    // TODO: check properly
    QueryModel aggs =
        new QueryModel(
            AggregationType.TERMS, Map.of(FIELD, "resourceGroup.keyword", SIZE_KEY, 10000));
    QueryModel accessPolicies =
        new QueryModel(
            AggregationType.TERMS, Map.of(FIELD, "accessPolicy.keyword", SIZE_KEY, 10000));

    QueryModel accessPolicyCount =
        new QueryModel(AggregationType.VALUE_COUNT, Map.of(FIELD, "accessPolicy.keyword"));

    QueryModel resourceCount =
        new QueryModel(AggregationType.VALUE_COUNT, Map.of(FIELD, ID_KEYWORD));

    // Creating the query part for search
    aggs.setAggregationName(RESULTS);
    aggs.setAggregationsMap(
        Map.of(
            "access_policies",
            accessPolicies,
            "accessPolicy_count",
            accessPolicyCount,
            "resource_count",
            resourceCount));

    QueryModel queryModel = new QueryModel();
    queryModel.setAggregations(List.of(aggs));
    queryModel.setLimit("0");
    return queryModel;
  }

  private List<QueryModel> getResourceGroupCountAgg() {

    // Step 1: Create the main query model for the root aggregation
    QueryModel queryModel = new QueryModel();
    queryModel.setAggregationType(AggregationType.FILTER);
    queryModel.setAggregationName("resourceGroupCount");

    // Set parameters for the filter (filter by type.keyword = "iudx:ResourceGroup")
    Map<String, Object> filterParams =
        Map.of(FIELD, "type.keyword", VALUE, ITEM_TYPE_RESOURCE_GROUP);
    queryModel.setAggregationParameters(filterParams);

    // Step 2: Create sub-aggregations for resourceCount and providerCount

    // resourceCount sub-aggregation (global -> filter -> Resources)
    QueryModel resourceCountQuery = new QueryModel();
    resourceCountQuery.setAggregationName("resourceCount");
    resourceCountQuery.setAggregationType(AggregationType.GLOBAL);
    resourceCountQuery.setAggregationParameters(Map.of(FIELD, "NULL", VALUE, "NULL"));
    QueryModel resourceFilter = new QueryModel();
    resourceFilter.setAggregationType(AggregationType.FILTER);
    Map<String, Object> resourceFilterParams = new HashMap<>();
    resourceFilterParams.put(FIELD, "type.keyword");
    resourceFilterParams.put(VALUE, "iudx:Resource");
    resourceFilter.setAggregationParameters(resourceFilterParams);
    Map<String, QueryModel> resourceSubAggregations = new HashMap<>();
    resourceSubAggregations.put("Resources", resourceFilter);
    resourceCountQuery.setAggregationsMap(resourceSubAggregations);

    // providerCount sub-aggregation (global -> filter -> Providers)
    QueryModel providerCountQuery = new QueryModel();
    providerCountQuery.setAggregationName("providerCount");
    providerCountQuery.setAggregationType(AggregationType.GLOBAL);
    providerCountQuery.setAggregationParameters(Map.of(FIELD, "NULL", VALUE, "NULL"));
    QueryModel providerFilter = new QueryModel();
    providerFilter.setAggregationType(AggregationType.FILTER);
    Map<String, Object> providerFilterParams = new HashMap<>();
    providerFilterParams.put(FIELD, "type.keyword");
    providerFilterParams.put(VALUE, ITEM_TYPE_PROVIDER);
    providerFilter.setAggregationParameters(providerFilterParams);
    Map<String, QueryModel> providerSubAggregations = new HashMap<>();
    providerSubAggregations.put("Providers", providerFilter);
    providerCountQuery.setAggregationsMap(providerSubAggregations);

    return List.of(queryModel, resourceCountQuery, providerCountQuery);
  }

  private Comparator<JsonObject> comapratorForLatestDataset() {
    Comparator<JsonObject> jsonComparator =
        new Comparator<JsonObject>() {

          @Override
          public int compare(JsonObject record1, JsonObject record2) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

            LocalDateTime dateTime1 =
                LocalDateTime.parse(record1.getString("itemCreatedAt"), formatter);
            LocalDateTime dateTime2 =
                LocalDateTime.parse(record2.getString("itemCreatedAt"), formatter);
            return dateTime2.compareTo(dateTime1);
          }
        };
    return jsonComparator;
  }
}
