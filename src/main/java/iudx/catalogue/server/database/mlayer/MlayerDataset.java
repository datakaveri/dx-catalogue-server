package iudx.catalogue.server.database.mlayer;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.util.Constants.TOTAL_HITS;
import static iudx.catalogue.server.validator.Constants.VALIDATION_FAILURE_MSG;

import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.ElasticClient;
import iudx.catalogue.server.database.RespBuilder;
import iudx.catalogue.server.database.Util;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MlayerDataset {
  private static final Logger LOGGER = LogManager.getLogger(MlayerDataset.class);
  private static String internalErrorResp =
      new RespBuilder()
          .withType(TYPE_INTERNAL_SERVER_ERROR)
          .withTitle(TITLE_INTERNAL_SERVER_ERROR)
          .withDetail(DETAIL_INTERNAL_SERVER_ERROR)
          .getResponse();
  ElasticClient client;
  String docIndex;
  String mlayerInstanceIndex;

  public MlayerDataset(ElasticClient client, String docIndex, String mlayerInstanceIndex) {
    this.client = client;
    this.docIndex = docIndex;
    this.mlayerInstanceIndex = mlayerInstanceIndex;
  }

  public void getMlayerDataset(JsonObject requestData, Handler<AsyncResult<JsonObject>> handler) {

    LOGGER.debug("dataset Id" + requestData.getString(ID));
    client.searchAsync(
        GET_PROVIDER_AND_RS_ID.replace("$1", requestData.getString(ID)),
        docIndex,
        handlerRes -> {
          if (handlerRes.succeeded()) {
            if (handlerRes.result().getInteger(TOTAL_HITS) == 0) {
              LOGGER.debug("The dataset is not available.");
              handler.handle(
                  Future.failedFuture(
                      new RespBuilder()
                          .withType(TYPE_ITEM_NOT_FOUND)
                          .withTitle(TITLE_ITEM_NOT_FOUND)
                          .withDetail("dataset belonging to Id requested is not present")
                          .getResponse()));
            }
            String providerId =
                handlerRes.result().getJsonArray(RESULTS).getJsonObject(0).getString("provider");
            String cosId = "";
            if (handlerRes.result().getJsonArray(RESULTS).getJsonObject(0).containsKey("cos")) {
              cosId = handlerRes.result().getJsonArray(RESULTS).getJsonObject(0).getString("cos");
            }

            /*
            query to fetch resource group, provider of the resource group, resource
            items associated with the resource group and cos item.
            */
            String query =
                GET_MLAYER_DATASET
                    .replace("$1", requestData.getString(ID))
                    .replace("$2", providerId)
                    .replace("$3", cosId);
            LOGGER.debug("Query " + query);
            client.searchAsyncDataset(
                query,
                docIndex,
                resultHandler -> {
                  if (resultHandler.succeeded()) {
                    LOGGER.debug("Success: Successful DB Request");
                    JsonObject record =
                        resultHandler.result().getJsonArray(RESULTS).getJsonObject(0);
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

                      // query to get the icon path of the instance in the  resource group
                      String getIconQuery =
                          GET_MLAYER_INSTANCE_ICON.replace("$1", instanceCapitalizeName);
                      client.searchAsync(
                          getIconQuery,
                          mlayerInstanceIndex,
                          iconResultHandler -> {
                            if (iconResultHandler.succeeded()) {
                              LOGGER.debug("Success: Successful DB Request");
                              JsonObject instances = iconResultHandler.result();
                              if (instances.getInteger(TOTAL_HITS) == 0) {
                                LOGGER.debug("The icon path for the instance is not present.");
                                record.getJsonObject("dataset").put("instance_icon", "");
                              } else {
                                JsonObject resource =
                                    instances.getJsonArray(RESULTS).getJsonObject(0);
                                String instancePath = resource.getString("icon");
                                record.getJsonObject("dataset").put("instance_icon", instancePath);
                              }
                              resultHandler.result().remove(TOTAL_HITS);
                              handler.handle(Future.succeededFuture(resultHandler.result()));
                            } else {
                              LOGGER.error("Fail: failed DB request inner");
                              LOGGER.error(resultHandler.cause());
                              handler.handle(Future.failedFuture(internalErrorResp));
                            }
                          });
                    } else {
                      resultHandler.result().remove(TOTAL_HITS);
                      record.getJsonObject("dataset").put("instance_icon", "");
                      handler.handle(Future.succeededFuture(resultHandler.result()));
                    }
                  } else {
                    LOGGER.error("Fail: failed DB request outer");
                    LOGGER.error(resultHandler.cause());
                    handler.handle(Future.failedFuture(internalErrorResp));
                  }
                });
          } else {
            LOGGER.error("Fail: DB request to get provider failed.");
            handler.handle(Future.failedFuture(internalErrorResp));
          }
        });
  }

  public void getMlayerAllDatasets(
      JsonObject requestParam, String query, Handler<AsyncResult<JsonObject>> handler) {

    LOGGER.debug("Getting all the resource group items");
    Promise<JsonObject> datasetResult = Promise.promise();
    Promise<JsonObject> instanceResult = Promise.promise();
    Promise<JsonObject> resourceCount = Promise.promise();

    gettingAllDatasets(query, datasetResult);
    allMlayerInstance(instanceResult);
    String getResourceAP = RESOURCE_ACCESSPOLICY_COUNT;
    gettingResourceAccessPolicyCount(getResourceAP, resourceCount);

    CompositeFuture.all(instanceResult.future(), datasetResult.future(), resourceCount.future())
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
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
                  record.remove(TYPE);
                  resourceGroupArray.add(record);
                }
                JsonArray pagedResourceGroups = new JsonArray();
                int endIndex =
                    requestParam.getInteger(LIMIT) + requestParam.getInteger(OFFSET);
                if (endIndex >= resourceGroupArray.size()) {
                  if (requestParam.getInteger(OFFSET) >= resourceGroupArray.size()) {
                    LOGGER.debug("Offset value has exceeded total hits");
                    RespBuilder respBuilder =
                        new RespBuilder()
                            .withType(TYPE_SUCCESS)
                            .withTitle(SUCCESS)
                            .withTotalHits(resourceGroupList.getInteger("resourceGroupCount"));
                    handler.handle(Future.succeededFuture(respBuilder.getJsonResponse()));
                  } else {
                    endIndex = resourceGroupArray.size();
                  }
                }
                for (int i = requestParam.getInteger(OFFSET); i < endIndex; i++) {
                  pagedResourceGroups.add(resourceGroupArray.getJsonObject(i));
                }

                LOGGER.debug("getMlayerDatasets resourceGroupList interation succeeded");
                RespBuilder respBuilder =
                    new RespBuilder()
                        .withType(TYPE_SUCCESS)
                        .withTitle(SUCCESS)
                        .withTotalHits(
                                resourceGroupList.getInteger("resourceGroupCount"))
                        .withResult(pagedResourceGroups);
                LOGGER.debug("getMlayerDatasets succeeded");
                handler.handle(Future.succeededFuture(respBuilder.getJsonResponse()));

              } else {
                LOGGER.error("Fail: failed DB request");
                handler.handle(Future.failedFuture(ar.cause().getMessage()));
              }
            });
  }

  private void gettingAllDatasets(String query, Promise<JsonObject> datasetResult) {

    LOGGER.debug(
        "Getting all resourceGroup along with provider description, "
            + "resource server url and cosUrl");
    client.searchAsync(
        query,
        docIndex,
        resultHandler -> {
          if (resultHandler.succeeded()) {
            try {
              LOGGER.debug("getRGs started");
              int size = resultHandler.result().getJsonArray(RESULTS).size();
              if (size == 0) {
                LOGGER.debug("getRGs is zero");
                datasetResult.handle(Future.failedFuture(NO_CONTENT_AVAILABLE));
                return;
              }
              JsonObject rsUrl = new JsonObject();
              JsonObject providerDescription = new JsonObject();
              JsonObject cosUrl = new JsonObject();
              LOGGER.debug("getRGs for each provider type result started");
              for (int i = 0; i < size; i++) {
                JsonObject record = resultHandler.result().getJsonArray(RESULTS).getJsonObject(i);
                String itemType = Util.getItemType(record);
                if (itemType.equals(VALIDATION_FAILURE_MSG)) {
                  datasetResult.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
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
              for (int i = 0; i < size; i++) {
                JsonObject record = resultHandler.result().getJsonArray(RESULTS).getJsonObject(i);
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
            datasetResult.handle(Future.failedFuture(internalErrorResp));
          }
        });
  }

  private void allMlayerInstance(Promise<JsonObject> instanceResult) {
    LOGGER.debug("Getting all instance name and icons");
    client.searchAsync(
        GET_ALL_MLAYER_INSTANCES,
        mlayerInstanceIndex,
        instanceRes -> {
          if (instanceRes.succeeded()) {
            try {
              LOGGER.debug("getInstance started");
              int instanceSize = instanceRes.result().getJsonArray(RESULTS).size();
              JsonObject instanceIcon = new JsonObject();
              LOGGER.debug("getInstance for each instance started");
              for (int i = 0; i < instanceSize; i++) {
                JsonObject instanceObject =
                    instanceRes.result().getJsonArray(RESULTS).getJsonObject(i);
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
            instanceResult.handle(Future.failedFuture(internalErrorResp));
          }
        });
  }

  public void gettingResourceAccessPolicyCount(
          String query, Promise<JsonObject> resourceCountResult) {
    LOGGER.debug("Getting resource item count");
    client.resourceAggregationAsync(
        query,
        docIndex,
        resourceCountRes -> {
          if (resourceCountRes.succeeded()) {
            try {
              if (resourceCountRes.result().getInteger(TOTAL_HITS) == 0) {
                LOGGER.debug("No Resources With AccessPolicy Found");
                resourceCountResult.handle(Future.failedFuture(NO_CONTENT_AVAILABLE));
                return;
              }
              LOGGER.debug("resourceAP started");
              JsonObject resourceItemCount = new JsonObject();
              JsonObject resourceAccessPolicy = new JsonObject();
              JsonArray resultsArray = resourceCountRes.result().getJsonArray(RESULTS);
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
            resourceCountResult.handle(Future.failedFuture(internalErrorResp));
          }
        });
  }
}
