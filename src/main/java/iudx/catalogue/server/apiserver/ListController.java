/**
 *
 *
 * <h1>SearchController.java</h1>
 *
 * <p>Callback handlers for List APIS
 */

package iudx.catalogue.server.apiserver;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.common.util.ResponseBuilderUtil.internalErrorResp;
import static iudx.catalogue.server.database.elastic.util.Constants.KEY;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.apiserver.item.service.ItemService;
import iudx.catalogue.server.apiserver.util.QueryMapper;
import iudx.catalogue.server.apiserver.util.RespBuilder;
import iudx.catalogue.server.common.util.DbResponseMessageBuilder;
import iudx.catalogue.server.database.elastic.model.ElasticsearchResponse;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import iudx.catalogue.server.database.elastic.util.QueryDecoder;
import iudx.catalogue.server.database.elastic.util.ResponseFilter;
import iudx.catalogue.server.exceptions.InternalServerException;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ListController {

  private static final Logger LOGGER = LogManager.getLogger(ListController.class);
  private final ItemService itemService;
  private final QueryDecoder queryDecoder = new QueryDecoder();

  public ListController(ItemService itemService) {
    this.itemService = itemService;
  }

  //  Routes for list

  /* list the item from database using itemId */
  public Router init(Router router) {
    /* list the item from database using itemId */
    router
        .get(ROUTE_LIST_ITEMS)
        .produces(MIME_APPLICATION_JSON)
        .handler(this::listItemsHandler);
    return router;
  }

  /**
   * Get the list of items for a catalogue instance.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  public void listItemsHandler(RoutingContext routingContext) {

    LOGGER.debug("Info: Listing items");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();
    MultiMap queryParameters = routingContext.queryParams();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    /* HTTP request instance/host details */
    String instanceId = request.getHeader(HEADER_INSTANCE);

    String itemType = request.getParam(ITEM_TYPE);
    JsonObject requestBody = QueryMapper.map2Json(queryParameters);
    if (requestBody != null) {

      requestBody.put(ITEM_TYPE, itemType);
      /* Populating query mapper */
      requestBody.put(HEADER_INSTANCE, instanceId);

      JsonObject resp = QueryMapper.validateQueryParam(requestBody);
      if (resp.getString(STATUS).equals(SUCCESS)) {

        String type = null;

        switch (itemType) {
          case INSTANCE:
            type = ITEM_TYPE_INSTANCE;
            break;
          case RESOURCE_GRP:
            type = ITEM_TYPE_RESOURCE_GROUP;
            break;
          case RESOURCE_SVR:
            type = ITEM_TYPE_RESOURCE_SERVER;
            break;
          case PROVIDER:
            type = ITEM_TYPE_PROVIDER;
            break;
          case TAGS:
            type = itemType;
            break;
          case OWNER:
            type = ITEM_TYPE_OWNER;
            break;
          case COS:
            type = ITEM_TYPE_COS;
            break;
          default:
            LOGGER.error("Fail: Invalid itemType:" + itemType);
            response
                .setStatusCode(400)
                .end(
                    new RespBuilder()
                        .withType(TYPE_INVALID_SYNTAX)
                        .withTitle(TITLE_INVALID_SYNTAX)
                        .withDetail(DETAIL_WRONG_ITEM_TYPE)
                        .getResponse());
            return;
        }
        requestBody.put(TYPE, type);

        if (type.equalsIgnoreCase(ITEM_TYPE_OWNER) || type.equalsIgnoreCase(ITEM_TYPE_COS)) {
          listOwnerOrCos(requestBody)
              .onComplete(
                  dbHandler -> {
                    if (dbHandler.succeeded()) {
                      handleResponseFromDatabase(routingContext, response, itemType, dbHandler);
                    }
                  });
        } else {

          /* Request database service with requestBody for listing items */
          listItems(requestBody)
              .onComplete(dbHandler -> handleResponseFromDatabase(routingContext, response,
                  itemType, dbHandler));
        }
      } else {
        LOGGER.error("Fail: Search/Count; Invalid request query parameters");
        response
            .setStatusCode(400)
            .end(
                new RespBuilder()
                    .withType(TYPE_INVALID_SYNTAX)
                    .withTitle(TITLE_INVALID_SYNTAX)
                    .withDetail(DETAIL_WRONG_ITEM_TYPE)
                    .getResponse());
      }
    } else {
      LOGGER.error("Fail: Search/Count; Invalid request query parameters");
      response
          .setStatusCode(400)
          .end(
              new RespBuilder()
                  .withType(TYPE_INVALID_SYNTAX)
                  .withTitle(TITLE_INVALID_SYNTAX)
                  .withDetail(DETAIL_WRONG_ITEM_TYPE)
                  .getResponse());
    }
  }

  public Future<JsonObject> listOwnerOrCos(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    LOGGER.debug("Info: Listing items;");

    QueryModel queryModel = queryDecoder.listItemQueryModel(request);
    itemService.search(queryModel, ResponseFilter.SOURCE_WITHOUT_EMBEDDINGS)
        .onComplete(
            res -> {
              if (res.failed()) {
                LOGGER.error("Fail: DB request has failed;" + res.cause());
                promise.fail(res.cause());
              } else {
                DbResponseMessageBuilder responseMsg = res.result();
                LOGGER.debug("Success: Successful DB Request");
                promise.complete(responseMsg.getResponse());
              }
            });
    return promise.future();
  }

  public Future<JsonObject> listItems(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    LOGGER.debug("Info: Listing items;");
    QueryModel queryModel = queryDecoder.listItemQueryModel(request);

    itemService.search(queryModel, ResponseFilter.SOURCE_ONLY)
        .onComplete(
            res -> {
              if (res.succeeded()) {
                LOGGER.debug("Success: Successful DB request");
                DbResponseMessageBuilder responseMsg = new DbResponseMessageBuilder();
                try {
                  // Process the aggregation results
                  JsonArray results = new JsonArray();
                  JsonArray aggregations =
                      ElasticsearchResponse.getAggregations()
                          .getJsonObject(RESULTS)
                          .getJsonArray(BUCKETS);

                  // If aggregations are present, process them once
                  if (aggregations != null && !aggregations.isEmpty()) {
                    // Add the aggregations to the results
                    aggregations.stream()
                        .filter(bucket -> bucket instanceof JsonObject)
                        .map(bucket -> ((JsonObject) bucket).getString(KEY))
                        .filter(Objects::nonNull) // Filter out null keys
                        .forEach(results::add);
                  }
                  responseMsg.statusSuccess().setTotalHits(results.size());
                  results.forEach(result -> responseMsg.addResult(result.toString()));

                  promise.complete(responseMsg.getResponse());
                } catch (Exception e) {
                  LOGGER.error("Error processing aggregation buckets: " + e.getMessage(), e);
                  promise.fail(internalErrorResp());
                }
              } else {
                LOGGER.error("Fail: DB request has failed;" + res.cause());
                /* Handle request error */
                promise.fail(res.cause());
              }
            });
    return promise.future();
  }

  void handleResponseFromDatabase(RoutingContext routingContext,
      HttpServerResponse response, String itemType, AsyncResult<JsonObject> dbhandler) {
    if (dbhandler.succeeded()) {
      LOGGER.info("Success: Item listing");
      response.setStatusCode(200).end(dbhandler.result().toString());
    } else if (dbhandler.failed()) {
      if (dbhandler.cause() instanceof InternalServerException exception) {
        routingContext.fail(exception);
      }
      LOGGER.error("Fail: Issue in listing " + itemType + ": " + dbhandler.cause().getMessage());
      response
          .setStatusCode(400)
          .end(
              new RespBuilder()
                  .withType(TYPE_INVALID_SYNTAX)
                  .withTitle(TITLE_INVALID_SYNTAX)
                  .withDetail(DETAIL_WRONG_ITEM_TYPE)
                  .getResponse());
    }
  }
}
