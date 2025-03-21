package iudx.catalogue.server.apiserver.item.handler;

import static iudx.catalogue.server.common.util.ResponseBuilderUtil.invalidUuidResponse;
import static iudx.catalogue.server.common.util.ResponseBuilderUtil.itemNotFoundResponse;
import static iudx.catalogue.server.common.util.ResponseBuilderUtil.linkValidationFailureResponse;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.apiserver.item.service.ItemService;
import iudx.catalogue.server.authenticator.model.JwtAuthenticationInfo;
import iudx.catalogue.server.common.RoutingContextHelper;
import iudx.catalogue.server.exceptions.DatabaseFailureException;
import iudx.catalogue.server.exceptions.InternalServerException;
import iudx.catalogue.server.validator.service.ValidatorService;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ItemLinkValidationHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(ItemLinkValidationHandler.class);
  private final ItemService itemService;
  private final ValidatorService validatorService;

  public ItemLinkValidationHandler(ItemService itemService, ValidatorService validatorService) {
    this.itemService = itemService;
    this.validatorService = validatorService;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    routingContext.next();
  }

  public void handleItemTypeCases(RoutingContext routingContext) {

    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerResponse response = routingContext.response();
    JwtAuthenticationInfo jwtAuthenticationInfo =
        RoutingContextHelper.getJwtAuthInfo(routingContext);
    JwtAuthenticationInfo.MutableJwtInfo mutableJwtInfo =
        new JwtAuthenticationInfo.MutableJwtInfo(jwtAuthenticationInfo);
    String itemType = RoutingContextHelper.getItemType(routingContext);

    if (itemType.equalsIgnoreCase(ITEM_TYPE_COS)
        || itemType.equalsIgnoreCase(ITEM_TYPE_OWNER)
        || itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_SERVER)) {
      RoutingContextHelper.setJwtAuthInfo(routingContext, mutableJwtInfo.get());
      RoutingContextHelper.setValidatedRequest(routingContext, requestBody);
      routingContext.next();
    } else if (itemType.equals(ITEM_TYPE_PROVIDER)) {
      Future<JsonObject> resourceServerUrlFuture =
          getParentObjectInfo(requestBody.getString(RESOURCE_SVR));
      // add resource server url to provider body
      resourceServerUrlFuture.onComplete(
          resourceServerUrl -> {
            if (resourceServerUrl.succeeded()) {
              String rsUrl = resourceServerUrl.result().getString(RESOURCE_SERVER_URL);
              // used for relationship apis
              String cosId = resourceServerUrl.result().getString(COS_ITEM);

              mutableJwtInfo.set(
                  new JwtAuthenticationInfo.Builder(mutableJwtInfo.get())
                      .setResourceServerUrl(rsUrl)
                      .build());
              requestBody.put(RESOURCE_SERVER_URL, rsUrl);
              requestBody.put(COS_ITEM, cosId);
              RoutingContextHelper.setJwtAuthInfo(routingContext, mutableJwtInfo.get());
              RoutingContextHelper.setValidatedRequest(routingContext, requestBody);
              routingContext.next();
            } else {
              response
                  .setStatusCode(400)
                  .end(linkValidationFailureResponse("Resource Server not found"));
            }
          });
    } else {
      Future<JsonObject> ownerUserIdFuture = getParentObjectInfo(requestBody.getString(PROVIDER));
      // add provider kc id to requestBody
      ownerUserIdFuture.onComplete(
          ownerUserId -> {
            if (ownerUserId.succeeded()) {
              String kcId = ownerUserId.result().getString(PROVIDER_USER_ID);
              String rsUrl = ownerUserId.result().getString(RESOURCE_SERVER_URL);

              mutableJwtInfo.set(
                  new JwtAuthenticationInfo.Builder(mutableJwtInfo.get())
                      .setProviderUserId(kcId)
                      .setResourceServerUrl(rsUrl)
                      .build());
              // cosId is used for relationship apis
              String cosId = ownerUserId.result().getString(COS_ITEM);
              requestBody.put(PROVIDER_USER_ID, kcId);
              requestBody.put(COS_ITEM, cosId);

              RoutingContextHelper.setJwtAuthInfo(routingContext, mutableJwtInfo.get());
              RoutingContextHelper.setValidatedRequest(routingContext, requestBody);
              routingContext.next();
            } else {
              response.setStatusCode(400).end(linkValidationFailureResponse("Provider not found"));
            }
          });
    }
  }

  Future<JsonObject> getParentObjectInfo(String itemId) {
    Promise<JsonObject> promise = Promise.promise();
    List<String> includeFields =
        List.of(
            TYPE,
            PROVIDER,
            PROVIDER_USER_ID,
            RESOURCE_GRP,
            RESOURCE_SVR,
            RESOURCE_SERVER_URL,
            COS,
            COS_ADMIN);
    JsonObject req = new JsonObject().put(ID, itemId).put(INCLUDE_FIELDS, includeFields);
    LOGGER.debug(req);
    itemService
        .search(itemId, includeFields)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                promise.complete(handler.result().getResponse().getJsonArray("results").getJsonObject(0));
              } else {
                if (handler.cause() instanceof NoSuchElementException) {
                  LOGGER.error("Fail: Item not found");
                  promise.fail(
                      itemNotFoundResponse("Fail: Doc doesn't exist, can't perform operation"));
                  return;
                } else if (handler.cause() instanceof InternalServerException exception) {
                  promise.fail(
                      new InternalServerException(exception.getMessage(), exception.getMethod()));
                } else if (handler.cause() instanceof DatabaseFailureException exception) {
                  promise.fail(exception);
                }
                promise.fail(handler.cause());
              }
            });
    return promise.future();
  }

  public void itemLinkValidation(RoutingContext routingContext) {

    HttpServerResponse response = routingContext.response();
    JsonObject requestBody = RoutingContextHelper.getValidatedRequest(routingContext);

    requestBody.put(HTTP_METHOD, routingContext.request().method().toString());
    /* Link Validating the request to ensure item correctness */
    validatorService
        .validateItem(requestBody)
        .onComplete(
            valHandler -> {
              if (valHandler.failed()) {
                LOGGER.error("Fail: Item validation failed;" + valHandler.cause().getMessage());
                if (valHandler.cause().getMessage().contains("validation failed. Incorrect id")) {
                  response
                      .setStatusCode(400)
                      .end(invalidUuidResponse("Syntax of the UUID is incorrect"));
                } else {
                  response
                      .setStatusCode(400)
                      .end(linkValidationFailureResponse(valHandler.cause().getMessage()));
                }
                return;
              }
              LOGGER.debug("Success: Item link validation");
              RoutingContextHelper.setValidatedRequest(routingContext, valHandler.result());
              routingContext.next();
            });
  }

  /**
   * Validates the request for deleting an item by checking the item's existence and setting
   * authentication info. Continues to the next handler if validation succeeds.
   *
   * @param routingContext {@link RoutingContext} @TODO Throw error if load failed
   */
  // tag::db-service-calls[]
  public void validateDeleteItemHandler(RoutingContext routingContext, Boolean isUac) {
    /* Id in path param */
    HttpServerResponse response = routingContext.response();
    String itemId = routingContext.queryParams().get(ID);
    LOGGER.debug("Info: Deleting item; id=" + itemId);
    //  populating JWT authentication info ->
    HttpServerRequest request = routingContext.request();

    /* authentication related information */
    String token = RoutingContextHelper.getToken(routingContext);
    JwtAuthenticationInfo.Builder jwtInfoBuilder =
        new JwtAuthenticationInfo.Builder()
            .setToken(token)
            .setMethod(REQUEST_DELETE)
            .setApiEndpoint(routingContext.normalizedPath());

    Future<JsonObject> itemTypeFuture = getParentObjectInfo(itemId);
    itemTypeFuture.onComplete(
        itemTypeHandler -> {
          if (itemTypeHandler.succeeded()) {

            Set<String> types =
                new HashSet<String>(itemTypeHandler.result().getJsonArray(TYPE).getList());
            types.retainAll(ITEM_TYPES);
            String itemType = types.toString().replaceAll("\\[", "").replaceAll("\\]", "");
            LOGGER.debug("itemType : {} ", itemType);
            RoutingContextHelper.setItemType(routingContext, itemType);
            jwtInfoBuilder.setItemType(itemType);
            if (isUac) {
              RoutingContextHelper.setJwtAuthInfo(routingContext, jwtInfoBuilder.build());
              routingContext.next();
            } else {
              if (itemType.equalsIgnoreCase(ITEM_TYPE_COS)
                  || itemType.equalsIgnoreCase(ITEM_TYPE_OWNER)
                  || itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_SERVER)) {
                RoutingContextHelper.setJwtAuthInfo(routingContext, jwtInfoBuilder.build());
                routingContext.next();
              } else if (itemType.equalsIgnoreCase(ITEM_TYPE_PROVIDER)) {
                LOGGER.debug(itemTypeHandler.result());
                String rsUrl = itemTypeHandler.result().getString(RESOURCE_SERVER_URL, "");
                jwtInfoBuilder.setResourceServerUrl(rsUrl);
                RoutingContextHelper.setJwtAuthInfo(routingContext, jwtInfoBuilder.build());
                routingContext.next();
              } else {
                LOGGER.debug(itemTypeHandler.result());
                Future<JsonObject> providerInfoFuture =
                    getParentObjectInfo(itemTypeHandler.result().getString(PROVIDER));
                providerInfoFuture.onComplete(
                    providerInfo -> {
                      if (providerInfo.succeeded()) {
                        LOGGER.debug(providerInfo.result());
                        String rsUrl = providerInfo.result().getString(RESOURCE_SERVER_URL, "");
                        String ownerUserId = providerInfo.result().getString(PROVIDER_USER_ID, "");
                        jwtInfoBuilder.setResourceServerUrl(rsUrl).setProviderUserId(ownerUserId);
                        RoutingContextHelper.setJwtAuthInfo(routingContext, jwtInfoBuilder.build());
                        routingContext.next();
                      } else {
                        response.setStatusCode(400).end(itemNotFoundResponse("item is not found"));
                      }
                    });
              }
            }
          } else {
            if (itemTypeHandler.cause().getMessage().contains(TYPE_ITEM_NOT_FOUND)) {
              response.setStatusCode(404).end(itemTypeHandler.cause().getMessage());
            } else {
              response.setStatusCode(400).end(itemTypeHandler.cause().getMessage());
            }
          }
        });
  }
}
