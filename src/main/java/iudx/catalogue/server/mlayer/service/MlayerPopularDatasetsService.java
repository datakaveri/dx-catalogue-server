package iudx.catalogue.server.mlayer.service;

import static iudx.catalogue.server.database.elastic.model.ElasticsearchResponse.getAggregations;
import static iudx.catalogue.server.database.elastic.util.Constants.*;
import static iudx.catalogue.server.mlayer.util.QueryModelUtil.buildMlayerDomainQueryModel;
import static iudx.catalogue.server.mlayer.util.QueryModelUtil.createLatestResourceGroupsQuery;
import static iudx.catalogue.server.mlayer.util.QueryModelUtil.createProviderAndResourceQuery;
import static iudx.catalogue.server.mlayer.util.QueryModelUtil.getCategorizedResourceApQueryModel;
import static iudx.catalogue.server.mlayer.util.QueryModelUtil.getMatchAllSortedQueryModel;
import static iudx.catalogue.server.mlayer.util.QueryModelUtil.getProviderAndPopularRgsQuery;
import static iudx.catalogue.server.mlayer.util.QueryModelUtil.getResourceApQueryModel;
import static iudx.catalogue.server.util.Constants.*;
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

public class MlayerPopularDatasetsService {
  private static final Logger LOGGER = LogManager.getLogger(MlayerPopularDatasetsService.class);
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

  public MlayerPopularDatasetsService(
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
//    RequestParams params = new RequestParams(mlayerInstanceIndex, ALL_SORTED_MLAYER_INSTANCES,
//        ResponseFilter.SOURCE_ONLY);
    QueryModel queryModel = getMatchAllSortedQueryModel();
    esService.search(mlayerInstanceIndex, queryModel)
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
    QueryModel queryModel = buildMlayerDomainQueryModel();
    esService.search(mlayerDomainIndex, queryModel)
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
    QueryModel getProviderAndResourcesQuery = createProviderAndResourceQuery(instance);
    esService
        .search(docIndex, getProviderAndResourcesQuery)
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
                MlayerDatasetService mlayerDataset =
                    new MlayerDatasetService(webClient, esService, docIndex, mlayerInstanceIndex);
                // function to get the resource group items count
                QueryModel resourceApQueryModel = getResourceApQueryModel();
                mlayerDataset.gettingResourceAccessPolicyCount(resourceCount, resourceApQueryModel);
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
                            LOGGER.error(
                                "Fail: failed resourceCount DB request; " + handler.cause());
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

    QueryModel getLatestRgsQuery = createLatestResourceGroupsQuery();
    esService
        .search(docIndex, getLatestRgsQuery)
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

                QueryModel getPopularRgQueryModel =
                    getProviderAndPopularRgsQuery(frequentlyUsedResourceGroup);
                esService
                    .search(docIndex, getPopularRgQueryModel)
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
                            QueryModel resourceApQuery =
                                getCategorizedResourceApQueryModel(allRgId);
                            Promise<JsonObject> resourceCount = Promise.promise();
                            MlayerDatasetService mlayerDataset =
                                new MlayerDatasetService(
                                    webClient, esService, docIndex, mlayerInstanceIndex);
                            mlayerDataset.gettingResourceAccessPolicyCount(
                                resourceCount, resourceApQuery);

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
