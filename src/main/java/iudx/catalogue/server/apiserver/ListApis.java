/**
 *
 *
 * <h1>SearchApis.java</h1>
 *
 * <p>Callback handlers for List APIS
 */

package iudx.catalogue.server.apiserver;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.authenticator.Constants.API_ENDPOINT;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.apiserver.util.QueryMapper;
import iudx.catalogue.server.apiserver.util.RespBuilder;
import iudx.catalogue.server.authenticator.AuthenticationService;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.util.Api;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ListApis {

  private static final Logger LOGGER = LogManager.getLogger(ListApis.class);
  private DatabaseService dbService;
  private AuthenticationService authService;
  private final Api api;

  public ListApis(Api api) {
    this.api = api;
  }

  public void setDbService(DatabaseService dbService) {
    this.dbService = dbService;
  }

  public void setAuthService(AuthenticationService authService) {
    this.authService = authService;
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
          case DEPARTMENT:
          case ORGANIZATION_TYPE:
          case FILE_FORMAT:
          case UPLOADED_BY:
            type = itemType;
            break;
          case OWNER:
            type = ITEM_TYPE_OWNER;
            break;
          case COS:
            type = ITEM_TYPE_COS;
            break;
          case AI_MODEL:
            type = ITEM_TYPE_AI_MODEL;
            break;
          case DATA_BANK:
            type = ITEM_TYPE_DATA_BANK;
            break;
          case APPS:
            type = ITEM_TYPE_APPS;
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
          dbService.listOwnerOrCos(
              requestBody,
              dbHandler -> {
                handleResponseFromDatabase(response, itemType, dbHandler);
              });
        } else {

          /* Request database service with requestBody for listing items */
          dbService.listItems(
              requestBody,
              dbhandler -> {
                handleResponseFromDatabase(response, itemType, dbhandler);
              });
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

  /**
   * Post API to get list of items for multiple itemTypes at once.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  public void listItemsPostHandler(RoutingContext routingContext) {

    LOGGER.debug("Info: Listing items via POST");

    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    String instanceId = request.getHeader(HEADER_INSTANCE);

    JsonObject requestBody = routingContext.body().asJsonObject();
    if (requestBody == null) {
      LOGGER.error("Fail: No request body provided");
      response.setStatusCode(400).end(
          new RespBuilder()
              .withType(TYPE_INVALID_SYNTAX)
              .withTitle(TITLE_INVALID_SYNTAX)
              .withDetail(DETAIL_INVALID_REQUEST_BODY)
              .getResponse());
      return;
    }

    JsonArray itemTypes = requestBody.getJsonArray(FILTER);
    if (itemTypes == null || itemTypes.isEmpty()) {
      LOGGER.error("Fail: No filter provided in request body");
      response.setStatusCode(400).end(
          new RespBuilder()
              .withType(TYPE_INVALID_SYNTAX)
              .withTitle(TITLE_INVALID_SYNTAX)
              .withDetail(DETAIL_WRONG_FILTER_TYPE)
              .getResponse());
      return;
    }
    requestBody.put(HEADER_INSTANCE, instanceId);

    String token = routingContext.get(HEADER_TOKEN);

    if (token != null && !token.isEmpty()) {
      JsonObject jwtAuthenticationInfo = new JsonObject()
          .put(HEADER_TOKEN, token)
          .put(METHOD, REQUEST_GET)
          .put(API_ENDPOINT, api.getRouteListMulItems());

      authService.tokenInterospect(new JsonObject(), jwtAuthenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          JsonObject authInfo = authHandler.result();
          String sub = authInfo.getString(SUB);

          if (sub != null && !sub.isEmpty()) {
            requestBody.put(SUB, sub);  // Add sub value to request body
          }

          proceedWithItemListing(requestBody, itemTypes, response);
        } else {
          LOGGER.error("Fail: Token introspection failed");
          response.setStatusCode(401)
              .end(new RespBuilder()
                  .withType(TYPE_TOKEN_INVALID)
                  .withTitle(TITLE_TOKEN_INVALID)
                  .withDetail(authHandler.cause().getMessage())
                  .getResponse());
        }
      });
    } else {
      proceedWithItemListing(requestBody, itemTypes, response);
    }
  }

  private void proceedWithItemListing(JsonObject requestBody, JsonArray itemTypes,
                                      HttpServerResponse response) {
    JsonObject resp = QueryMapper.validateQueryParam(requestBody);
    if (resp.getString(STATUS).equals(SUCCESS)) {
      List<String> type = new ArrayList<>();
      for (Object obj : itemTypes) {
        String itemType = obj.toString();

        switch (itemType) {
          case INSTANCE:
            type.add(ITEM_TYPE_INSTANCE);
            break;
          case RESOURCE_GRP:
            type.add(ITEM_TYPE_RESOURCE_GROUP);
            break;
          case RESOURCE_SVR:
            type.add(ITEM_TYPE_RESOURCE_SERVER);
            break;
          case PROVIDER:
            type.add(ITEM_TYPE_PROVIDER);
            break;
          case TAGS:
          case DEPARTMENT:
          case ORGANIZATION_TYPE:
          case FILE_FORMAT:
          case DATA_READINESS:
          case MODEL_TYPE:
          case UPLOADED_BY:
          case ID:
            type.add(itemType);
            break;
          case OWNER:
            type.add(ITEM_TYPE_OWNER);
            break;
          case COS:
            type.add(ITEM_TYPE_COS);
            break;
          case AI_MODEL:
            type.add(ITEM_TYPE_AI_MODEL);
            break;
          case DATA_BANK:
            type.add(ITEM_TYPE_DATA_BANK);
            break;
          case APPS:
            type.add(ITEM_TYPE_APPS);
            break;
          default:
            LOGGER.error("Fail: Invalid itemType: " + itemType);
        }
      }

      requestBody.put(TYPE, type);
      /* Request database service with requestBody for listing items */
      dbService.listMultipleItems(
          requestBody,
          dbhandler -> {
            handleResponseFromDatabase(response, itemTypes.toString(), dbhandler);
          });

    } else {
      LOGGER.error("Fail: Search/Count; Invalid request query parameters");
      response.setStatusCode(400).end(
          new RespBuilder()
              .withType(TYPE_INVALID_SYNTAX)
              .withTitle(TITLE_INVALID_SYNTAX)
              .withDetail(DETAIL_WRONG_ITEM_TYPE)
              .getResponse());
    }
  }

  void handleResponseFromDatabase(
      HttpServerResponse response, String itemType, AsyncResult<JsonObject> dbhandler) {
    if (dbhandler.succeeded()) {
      LOGGER.info("Success: Item listing");
      response.setStatusCode(200).end(dbhandler.result().toString());
    } else if (dbhandler.failed()) {
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
