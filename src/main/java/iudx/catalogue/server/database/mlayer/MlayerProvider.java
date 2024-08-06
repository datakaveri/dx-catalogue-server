package iudx.catalogue.server.database.mlayer;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.validator.Constants.VALIDATION_FAILURE_MSG;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.ElasticClient;
import iudx.catalogue.server.database.RespBuilder;
import iudx.catalogue.server.database.Util;
import java.util.HashMap;
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
  ElasticClient client;
  String docIndex;

  public MlayerProvider(ElasticClient client, String docIndex) {
    this.client = client;
    this.docIndex = docIndex;
  }

  public void getMlayerProviders(
      JsonObject requestParams, Handler<AsyncResult<JsonObject>> handler) {
    String limit = requestParams.getString(LIMIT);
    String offset = requestParams.getString(OFFSET);
    if (requestParams.containsKey(INSTANCE)) {
      String query = GET_DATASET_BY_INSTANCE.replace("$1", requestParams.getString(INSTANCE));
      client.searchAsyncResourceGroupAndProvider(
          query,
          docIndex,
          resultHandler -> {
            if (resultHandler.succeeded()) {
              LOGGER.debug("Success: Successful DB Request");
              if (resultHandler.result().getJsonArray(RESULTS).isEmpty()) {
                handler.handle(Future.failedFuture(NO_CONTENT_AVAILABLE));
              }

              // providerCount depicts the number of provider associated with the instance
              Integer providerCount =
                  resultHandler
                      .result()
                      .getJsonArray(RESULTS)
                      .getJsonObject(0)
                      .getInteger("providerCount");
              LOGGER.debug("provider Count {} ", providerCount);
              // results consists of all providers and resource groups belonging to instance
              JsonArray results =
                  resultHandler
                      .result()
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
                  handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
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
                  handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
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
                  handler.handle(Future.succeededFuture(response));
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
              handler.handle(Future.succeededFuture(response));

            } else {
              LOGGER.error("Fail: failed DB request");
              handler.handle(Future.failedFuture(internalErrorResp));
            }
          });
    } else {
      String query = GET_MLAYER_PROVIDERS_QUERY.replace("$0", limit).replace("$1", offset);
      client.searchAsync(
          query,
          docIndex,
          resultHandler -> {
            if (resultHandler.succeeded()) {
              LOGGER.debug("Success: Successful DB Request");
              JsonObject result = resultHandler.result();
              handler.handle(Future.succeededFuture(result));
            } else {
              LOGGER.error("Fail: failed DB request");
              handler.handle(Future.failedFuture(internalErrorResp));
            }
        });
    }
  }
}
