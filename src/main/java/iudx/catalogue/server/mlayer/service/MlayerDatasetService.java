package iudx.catalogue.server.mlayer.service;

import static iudx.catalogue.server.database.elastic.model.ElasticsearchResponse.getAggregations;
import static iudx.catalogue.server.database.elastic.util.Constants.*;
import static iudx.catalogue.server.mlayer.util.QueryModelUtil.getAllInstanceNamesAndIconsQuery;
import static iudx.catalogue.server.mlayer.util.QueryModelUtil.getMlayerDatasetQuery;
import static iudx.catalogue.server.mlayer.util.QueryModelUtil.getMlayerInstanceIconsQuery;
import static iudx.catalogue.server.mlayer.util.QueryModelUtil.getMlayerLookupQuery;
import static iudx.catalogue.server.mlayer.util.QueryModelUtil.getResourceApQueryModel;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.validator.util.Constants.CONTEXT;
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
import iudx.catalogue.server.geocoding.util.Constants;
import iudx.catalogue.server.mlayer.model.MlayerDatasetRequest;
import iudx.catalogue.server.mlayer.vocabulary.DataModel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MlayerDatasetService {
  private static final Logger LOGGER = LogManager.getLogger(MlayerDatasetService.class);
  private static final String internalErrorResp =
      new RespBuilder()
          .withType(TYPE_INTERNAL_SERVER_ERROR)
          .withTitle(TITLE_INTERNAL_SERVER_ERROR)
          .withDetail(DETAIL_INTERNAL_SERVER_ERROR)
          .getResponse();
  WebClient webClient;
  ElasticsearchService esService;
  String docIndex;
  String mlayerInstanceIndex;

  public MlayerDatasetService(WebClient webClient, ElasticsearchService esService, String docIndex,
                       String mlayerInstanceIndex) {
    this.webClient = webClient;
    this.esService = esService;
    this.docIndex = docIndex;
    this.mlayerInstanceIndex = mlayerInstanceIndex;
  }

  public Future<JsonObject> getMlayerDataset(MlayerDatasetRequest requestData) {
    String id = requestData.getId();
    QueryModel query = getMlayerLookupQuery(id);

    Promise<JsonObject> promise = Promise.promise();
    esService.search(docIndex, query)
        .onComplete(handlerRes -> {
          if (handlerRes.succeeded()) {
            if (handlerRes.result().isEmpty()) {
              LOGGER.debug("The dataset is not available.");
              promise.fail(
                  new RespBuilder()
                      .withType(TYPE_ITEM_NOT_FOUND)
                      .withTitle(TITLE_ITEM_NOT_FOUND)
                      .withDetail("dataset belonging to Id requested is not present")
                      .getResponse());
              return;
            }
            String providerId =
                handlerRes.result().getFirst().getSource().getString("provider");
            String cosId = "";
            if (handlerRes.result().getFirst().getSource().containsKey("cos")) {
              cosId = handlerRes.result().getFirst().getSource().getString("cos");
            }

            /*
            query to fetch resource group, provider of the resource group, resource
            items associated with the resource group and cos item.
            */
            QueryModel datasetDetailFetchQuery = getMlayerDatasetQuery(requestData.getId(),
                cosId, providerId);

            esService.search(docIndex, datasetDetailFetchQuery)
                .onComplete(resultHandler -> {
                  if (resultHandler.succeeded()) {
                    DbResponseMessageBuilder responseMsg = new DbResponseMessageBuilder();
                    responseMsg.statusSuccess();
                    JsonArray resources = new JsonArray();
                    JsonObject datasetDetail = new JsonObject();
                    JsonObject dataset = new JsonObject();
                    for (int i = 0; i < resultHandler.result().size(); i++) {
                      JsonObject record = resultHandler.result().get(i).getSource();
                      JsonObject provider = new JsonObject();
                      String itemType = Util.getItemType(record);
                      if (itemType.equals(VALIDATION_FAILURE_MSG)) {
                        promise.fail(VALIDATION_FAILURE_MSG);
                      }
                      if (itemType.equals("iudx:Provider")) {
                        provider
                            .put(ID, record.getString(ID))
                            .put(DESCRIPTION_ATTR, record.getString(DESCRIPTION_ATTR))
                            .put(ICON_BASE64, record.getString(ICON_BASE64));
                        dataset.put("resourceServerRegURL",
                            record.getString("resourceServerRegURL"));
                        dataset.put(PROVIDER, provider);
                      }
                      if (itemType.equals("iudx:Resource")) {
                        if (record.getJsonArray(Constants.TYPE).size() > 1) {
                          String schema =
                              record.getString("@context")
                                  + record
                                  .getJsonArray(Constants.TYPE)
                                  .getString(1)
                                  .substring(5,
                                      record.getJsonArray(Constants.TYPE).getString(1).length());
                          record.put("schema", schema);
                        }
                        record.remove("type");
                        record.put("resourceId", record.getString("id"));
                        record.remove("id");
                        resources.add(record);
                      }
                      if (itemType.equals("iudx:ResourceGroup")) {
                        dataset
                            .put(ID, record.getString(ID))
                            .put(DESCRIPTION_ATTR, record.getString(DESCRIPTION_ATTR));
                        if (record.getJsonArray(Constants.TYPE).size() > 1) {
                          String schema =
                              record.getString(CONTEXT)
                                  + record
                                  .getJsonArray(Constants.TYPE)
                                  .getString(1)
                                  .substring(5,
                                      record.getJsonArray(Constants.TYPE).getString(1).length());
                          record.put("schema", schema);
                          record.remove("@context");
                          dataset
                              .put("schema", schema);
                        }
                        if (record.containsKey(LABEL)) {
                          dataset.put(LABEL, record.getString(LABEL));
                        }
                        if (record.containsKey(ACCESS_POLICY)) {
                          dataset.put(ACCESS_POLICY, record.getString(ACCESS_POLICY));
                        }
                        if (record.containsKey(INSTANCE)) {
                          dataset.put(INSTANCE, record.getString(INSTANCE));
                        }
                        if (record.containsKey(DATA_SAMPLE)) {
                          dataset.put(DATA_SAMPLE, record.getJsonObject(DATA_SAMPLE));
                        }
                        if (record.containsKey("dataSampleFile")) {
                          dataset.put("dataSampleFile", record.getJsonArray("dataSampleFile"));
                        }
                        if (record.containsKey("dataQualityFile")) {
                          dataset.put("dataQualityFile", record.getJsonArray("dataQualityFile"));
                        }
                        if (record.containsKey(DATA_DESCRIPTOR)) {
                          dataset.put(DATA_DESCRIPTOR, record.getJsonObject(DATA_DESCRIPTOR));
                        }
                        if (record.containsKey("resourceType")) {
                          dataset.put("resourceType", record.getString("resourceType"));
                        }
                        if (record.containsKey("location")) {
                          dataset.put("location", record.getJsonObject("location"));
                        }
                        if (record.containsKey("itemCreatedAt")) {
                          dataset.put("itemCreatedAt", record.getString("itemCreatedAt"));
                        }
                      }

                      if (itemType.equals(ITEM_TYPE_COS)) {
                        dataset.put("cosURL", record.getString("cosURL"));
                      }
                    }
                    datasetDetail.put("dataset", dataset);
                    datasetDetail.put("resource", resources);
                    responseMsg.addResult(datasetDetail);


                    LOGGER.debug("Success: Successful DB Request");
                    JsonObject record =
                        responseMsg.getResponse().getJsonArray(RESULTS).getJsonObject(0);
                    record
                        .getJsonObject("dataset")
                        .put("totalResources", record.getJsonArray("resource").size());
                    String instanceName = "";
                    String instanceCapitalizeName = "";
                    if (record.getJsonObject("dataset").containsKey(INSTANCE)
                        && !(record.getJsonObject("dataset").getString(INSTANCE) == null)
                        && !(record.getJsonObject("dataset").getString(INSTANCE).isBlank())) {

                      instanceName = record.getJsonObject("dataset").getString(INSTANCE);
                      instanceCapitalizeName =
                          instanceName.substring(0, 1).toUpperCase() + instanceName.substring(1);

                      QueryModel fetchInstanceIconQuery =
                          getMlayerInstanceIconsQuery(instanceCapitalizeName);
                      esService.search(mlayerInstanceIndex, fetchInstanceIconQuery)
                          .onComplete(iconResultHandler -> {
                            if (iconResultHandler.succeeded()) {
                              LOGGER.debug("Success: Successful DB Request");
                              List<ElasticsearchResponse> responseList = iconResultHandler.result();
                              DbResponseMessageBuilder responseMessage =
                                  new DbResponseMessageBuilder();
                              responseMessage.statusSuccess();
                              responseMessage.setTotalHits(responseList.size());
                              responseList.stream()
                                  .map(ElasticsearchResponse::getSource)
                                  .peek(source -> {
                                    source.remove(SUMMARY_KEY);
                                    source.remove(WORD_VECTOR_KEY);
                                  })
                                  .forEach(responseMessage::addResult);
                              JsonObject instances = responseMessage.getResponse();
                              if (instances.getInteger(TOTAL_HITS) == 0) {
                                LOGGER.debug("The icon path for the instance is not present.");
                                record.getJsonObject("dataset").put("instance_icon", "");
                              } else {
                                JsonObject resource =
                                    instances.getJsonArray(RESULTS).getJsonObject(0);
                                String instancePath = resource.getString("icon");
                                record.getJsonObject("dataset").put("instance_icon", instancePath);
                              }
                              responseMsg.getResponse().remove(TOTAL_HITS);
                              promise.complete(responseMsg.getResponse());
                            } else {
                              LOGGER.error("Fail: failed DB request inner");
                              LOGGER.error(iconResultHandler.cause());
                              promise.fail(iconResultHandler.cause());
                            }
                          });
                    } else {
                      responseMsg.getResponse().remove(TOTAL_HITS);
                      record.getJsonObject("dataset").put("instance_icon", "");
                      promise.complete(responseMsg.getResponse());
                    }
                  } else {
                    LOGGER.error("Fail: failed DB request outer");
                    LOGGER.error(resultHandler.cause());
                    promise.fail(resultHandler.cause());
                  }
                });
          } else {
            LOGGER.error("Fail: DB request to get provider failed.");
            promise.fail(handlerRes.cause());
          }
        });
    return promise.future();
  }

  public Future<JsonObject> getMlayerAllDatasets(QueryModel queryModel, Integer limit,
                                                 Integer offset) {
    Promise<JsonObject> promise = Promise.promise();

    LOGGER.debug("Getting all the resource group items");
    Promise<JsonObject> datasetResult = Promise.promise();
    Promise<JsonObject> instanceResult = Promise.promise();
    Promise<JsonObject> resourceCount = Promise.promise();

    gettingAllDatasets(queryModel, datasetResult);
    allMlayerInstance(instanceResult);
    QueryModel resourceApQueryModel = getResourceApQueryModel();
    gettingResourceAccessPolicyCount(resourceCount, resourceApQueryModel);

    Future.all(instanceResult.future(), datasetResult.future(), resourceCount.future())
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                DataModel domainInfoFetcher = new DataModel(webClient, esService, docIndex);
                domainInfoFetcher
                    .getDataModelInfo()
                    .onComplete(
                        domainInfoResult -> {
                          if (domainInfoResult.succeeded()) {
                            JsonObject domains = domainInfoResult.result();

                            JsonObject instanceList = ar.result().resultAt(0);
                            JsonObject resourceGroupList = ar.result().resultAt(1);
                            JsonObject resourceAndPolicyCount = ar.result().resultAt(2);
                            JsonArray resourceGroupArray = new JsonArray();
                            LOGGER.debug("getMlayerDatasets resourceGroupList iteration started");
                            for (int i = 0;
                                 i < resourceGroupList.getInteger("resourceGroupCount");
                                 i++) {
                              JsonObject record =
                                  resourceGroupList.getJsonArray("resourceGroup").getJsonObject(i);
                              record.put(
                                  "icon",
                                  record.containsKey(INSTANCE)
                                      ? instanceList.getString(record.getString(INSTANCE))
                                      : "");
                              record.put(
                                  "totalResources",
                                  resourceAndPolicyCount
                                      .getJsonObject("resourceItemCount")
                                      .containsKey(record.getString(ID))
                                      ? resourceAndPolicyCount
                                      .getJsonObject("resourceItemCount")
                                      .getInteger(record.getString(ID))
                                      : 0);
                              if (resourceAndPolicyCount
                                  .getJsonObject("resourceAccessPolicy")
                                  .containsKey(record.getString(ID))) {
                                record.put(
                                    ACCESS_POLICY,
                                    resourceAndPolicyCount
                                        .getJsonObject("resourceAccessPolicy")
                                        .getJsonObject(record.getString(ID)));
                              } else {
                                record.put(
                                    ACCESS_POLICY,
                                    new JsonObject().put("PII", 0).put("SECURE", 0).put("OPEN", 0));
                              }
                              if (domains.getString(record.getString("id")) != null) {
                                record.put("domain", domains.getString(record.getString("id")));
                              }
                              record.remove(TYPE);
                              resourceGroupArray.add(record);
                            }
                            JsonArray pagedResourceGroups = new JsonArray();
                            int endIndex = limit + offset;
                            if (endIndex >= resourceGroupArray.size()) {
                              if (offset >= resourceGroupArray.size()) {
                                LOGGER.debug("Offset value has exceeded total hits");
                                RespBuilder respBuilder =
                                    new RespBuilder()
                                        .withType(TYPE_SUCCESS)
                                        .withTitle(SUCCESS)
                                        .withTotalHits(
                                            resourceGroupList.getInteger("resourceGroupCount"));
                                promise.complete(respBuilder.getJsonResponse());
                                return;
                              } else {
                                endIndex = resourceGroupArray.size();
                              }
                            }
                            for (int i = offset; i < endIndex; i++) {
                              pagedResourceGroups.add(resourceGroupArray.getJsonObject(i));
                            }

                            LOGGER.debug("getMlayerDatasets resourceGroupList iteration succeeded");
                            RespBuilder respBuilder =
                                new RespBuilder()
                                    .withType(TYPE_SUCCESS)
                                    .withTitle(SUCCESS)
                                    .withTotalHits(
                                        resourceGroupList.getInteger("resourceGroupCount"))
                                    .withResult(pagedResourceGroups);
                            LOGGER.debug("getMlayerDatasets succeeded");
                            promise.complete(respBuilder.getJsonResponse());
                          } else {
                            LOGGER.error("Fail: failed DataModel request");
                            promise.fail(internalErrorResp);
                          }
                        });
              } else {
                LOGGER.error("Fail: failed DB request");
                promise.fail(ar.cause().getMessage());
              }
            });
    return promise.future();
  }

  private void gettingAllDatasets(QueryModel queryModel, Promise<JsonObject> datasetResult) {
    LOGGER.debug(
        "Getting all resourceGroup along with provider description, "
            + "resource server url and cosUrl");
    esService.search(docIndex, queryModel)
        .onComplete(resultHandler -> {
          if (resultHandler.succeeded()) {
            try {
              LOGGER.debug("getRGs started");
              int size = resultHandler.result().size();
              if (size == 0) {
                LOGGER.debug("getRGs is zero");
                datasetResult.fail(NO_CONTENT_AVAILABLE);
                return;
              }
              JsonObject rsUrl = new JsonObject();
              JsonObject providerDescription = new JsonObject();
              JsonObject cosUrl = new JsonObject();
              LOGGER.debug("getRGs for each provider type result started");
              for (int i = 0; i < size; i++) {
                JsonObject record = resultHandler.result().get(i).getSource();
                String itemType = Util.getItemType(record);
                if (itemType.equals(VALIDATION_FAILURE_MSG)) {
                  datasetResult.fail(VALIDATION_FAILURE_MSG);
                  return;
                }
                if (itemType.equals(ITEM_TYPE_PROVIDER)) {
                  JsonObject newJson = new JsonObject().put(PROVIDER_DES,
                          record.getString(DESCRIPTION_ATTR))
                      .put(ICON_BASE64, record.getString(ICON_BASE64));
                  providerDescription.put(record.getString(ID), newJson);
                  rsUrl.put(
                      record.getString(ID),
                      record.containsKey("resourceServerRegURL")
                          ? record.getString("resourceServerRegURL")
                          : "");
                } else if (itemType.equals(ITEM_TYPE_COS)) {
                  cosUrl.put(record.getString(ID), record.getString("cosURL"));
                }
              }
              LOGGER.debug("getRGs for each provider type result succeeded");
              int resourceGroupHits = 0;
              JsonArray resourceGroup = new JsonArray();
              LOGGER.debug("getRGs for each resource group result started");
              LOGGER.debug(providerDescription);
              for (int i = 0; i < size; i++) {
                JsonObject record = resultHandler.result().get(i).getSource();
                String itemType = Util.getItemType(record);
                if (itemType.equals(ITEM_TYPE_RESOURCE_GROUP)) {
                  resourceGroupHits++;
                  providerDescription.getJsonObject(record.getString(PROVIDER))
                      .getString(ICON_BASE64);
                  record.put(
                      PROVIDER_DES,
                      providerDescription.getJsonObject(record.getString(PROVIDER))
                          .getString(PROVIDER_DES));
                  record.put(
                      ICON_BASE64, providerDescription.getJsonObject(record.getString(PROVIDER))
                          .getString(ICON_BASE64));
                  record.put("resourceServerRegURL", rsUrl.getString(record.getString(PROVIDER)));
                  record.put(
                      "cosURL",
                      record.containsKey("cos") ? cosUrl.getString(record.getString("cos")) : "");

                  record.remove("cos");
                  resourceGroup.add(record);
                }
              }
              LOGGER.debug("getRGs for each resource group result succeeded");
              JsonObject resourceGroupResult =
                  new JsonObject()
                      .put("resourceGroupCount", resourceGroupHits)
                      .put("resourceGroup", resourceGroup);
              LOGGER.debug("getRGs succeeded");
              datasetResult.complete(resourceGroupResult);
            } catch (Exception e) {
              LOGGER.error("getRGs unexpectedly failed : {}", e.getMessage());
              datasetResult.fail(e.getMessage());
            }
          } else {
            LOGGER.error("Fail: failed DB request");
            datasetResult.fail(internalErrorResp);
          }
        });
  }

  private void allMlayerInstance(Promise<JsonObject> instanceResult) {
    LOGGER.debug("Getting all instance name and icons");
    QueryModel queryModel = getAllInstanceNamesAndIconsQuery();
    esService.search(mlayerInstanceIndex, queryModel)
        .onComplete(instanceRes -> {
          if (instanceRes.succeeded()) {
            try {
              LOGGER.debug("getInstance started");
              int instanceSize = instanceRes.result().size();
              JsonObject instanceIcon = new JsonObject();
              LOGGER.debug("getInstance for each instance started");
              for (int i = 0; i < instanceSize; i++) {
                JsonObject instanceObject =
                    instanceRes.result().get(i).getSource();
                instanceIcon.put(
                    instanceObject.getString("name").toLowerCase(),
                    instanceObject.getString("icon"));
              }
              LOGGER.debug("getInstance succeeded");
              instanceResult.complete(instanceIcon);
            } catch (Exception e) {
              LOGGER.error("getInstance enexpectedly failed : {}", e.getMessage());
              instanceResult.fail(e.getMessage());
            }
          } else {
            LOGGER.error("Fail: query fail;" + instanceRes.cause());
            instanceResult.fail(internalErrorResp);
          }
        });
  }

  public void gettingResourceAccessPolicyCount(Promise<JsonObject> resourceCountResult,
                                               QueryModel queryModel) {
    LOGGER.debug("Getting resource item count");

    esService.search(docIndex, queryModel)
        .onComplete(resourceCountRes -> {
          if (resourceCountRes.succeeded()) {
            try {
              JsonArray resultsArray =
                  getAggregations().getJsonObject(RESULTS).getJsonArray(BUCKETS);

              if (resultsArray.isEmpty()) {
                LOGGER.debug("No Resources With AccessPolicy Found");
                resourceCountResult.fail(NO_CONTENT_AVAILABLE);
                return;
              }
              LOGGER.debug("resourceAP started");
              JsonObject resourceItemCount = new JsonObject();
              JsonObject resourceAccessPolicy = new JsonObject();
              LOGGER.debug("resourceAP for each resultsArray started");
              resultsArray.forEach(
                  record -> {
                    JsonObject recordObject = (JsonObject) record;
                    String resourceGroup = recordObject.getString(KEY);
                    int docCount = recordObject.getInteger("doc_count");
                    resourceItemCount.put(resourceGroup, docCount);
                    Map<String, Integer> accessPolicy = new HashMap<>();
                    accessPolicy.put("PII", 0);
                    accessPolicy.put("SECURE", 0);
                    accessPolicy.put("OPEN", 0);

                    JsonArray accessPoliciesArray =
                        recordObject.getJsonObject("access_policies").getJsonArray("buckets");

                    accessPoliciesArray.forEach(
                        accessPolicyRecord -> {
                          JsonObject accessPolicyRecordObject = (JsonObject) accessPolicyRecord;
                          String accessPolicyKey = accessPolicyRecordObject.getString(KEY);
                          int accessPolicyDocCount =
                              accessPolicyRecordObject.getInteger("doc_count");
                          accessPolicy.put(accessPolicyKey, accessPolicyDocCount);
                        });
                    resourceAccessPolicy.put(resourceGroup, accessPolicy);
                  });

              LOGGER.debug("resourceAP for each resultsArray succeeded");

              JsonObject results =
                  new JsonObject()
                      .put("resourceItemCount", resourceItemCount)
                      .put("resourceAccessPolicy", resourceAccessPolicy);
              LOGGER.debug("resourceAP Succeeded : {}", results.containsKey("resourceItemCount"));
              resourceCountResult.complete(results);
            } catch (Exception e) {
              LOGGER.error("resourceAP unexpectedly failed : {}", e.getMessage());
              resourceCountResult.fail(e.getMessage());
            }
          } else {
            LOGGER.error("Fail: query fail;" + resourceCountRes.cause());
            resourceCountResult.fail(internalErrorResp);
          }
        });
  }
}
