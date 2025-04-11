package iudx.catalogue.server.apiserver.item.handler;

import static iudx.catalogue.server.apiserver.util.Constants.HEADER_BEARER_AUTHORIZATION;
import static iudx.catalogue.server.apiserver.util.Constants.HEADER_CONTENT_TYPE;
import static iudx.catalogue.server.apiserver.util.Constants.HEADER_TOKEN;
import static iudx.catalogue.server.apiserver.util.Constants.MIME_APPLICATION_JSON;
import static iudx.catalogue.server.common.util.ResponseBuilderUtil.invalidSchemaResponse;
import static iudx.catalogue.server.common.util.ResponseBuilderUtil.invalidSyntaxResponse;
import static iudx.catalogue.server.common.util.ResponseBuilderUtil.invalidUuidResponse;
import static iudx.catalogue.server.util.Constants.ID;
import static iudx.catalogue.server.util.Constants.ITEM_TYPES;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_COS;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_OWNER;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_PROVIDER;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_RESOURCE;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_RESOURCE_GROUP;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_RESOURCE_SERVER;
import static iudx.catalogue.server.util.Constants.REQUEST_GET;
import static iudx.catalogue.server.util.Constants.REQUEST_POST;
import static iudx.catalogue.server.util.Constants.REQUEST_PUT;
import static iudx.catalogue.server.util.Constants.TYPE;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.apiserver.item.model.COS;
import iudx.catalogue.server.apiserver.item.model.Item;
import iudx.catalogue.server.apiserver.item.model.Owner;
import iudx.catalogue.server.apiserver.item.model.Provider;
import iudx.catalogue.server.apiserver.item.model.Resource;
import iudx.catalogue.server.apiserver.item.model.ResourceGroup;
import iudx.catalogue.server.apiserver.item.model.ResourceServer;
import iudx.catalogue.server.authenticator.model.JwtAuthenticationInfo;
import iudx.catalogue.server.common.RoutingContextHelper;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ItemSchemaHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(ItemSchemaHandler.class);
  private static final Pattern UUID_PATTERN =
      Pattern.compile(
          "^[a-zA-Z0-9]{8}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{12}$");

  @Override
  public void handle(RoutingContext context) throws IllegalArgumentException {
    context.next();
  }

  /**
   * Validates the authorization of the incoming request. Checks if the request contains a token
   * field.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void verifyAuthHeader(RoutingContext routingContext) {
    /* checking authentication info in requests */
    if (routingContext.request().headers().contains(HEADER_TOKEN) ||
        routingContext.request().headers().contains(HEADER_BEARER_AUTHORIZATION)) {
      routingContext.next();
    } else {
      LOGGER.warn("Fail: Unauthorized CRUD operation");
      routingContext.response().setStatusCode(401).end();
    }
  }

  /**
   * Create/Update Item. Validates the schema of the item being created or updated by performing
   * JSON schema validation. Populates JWT authentication information for further processing.
   *
   * @param routingContext {@link RoutingContext} @TODO Throw error if load failed
   */
  // tag::db-service-calls[]
  public void validateItemSchema(RoutingContext routingContext) {
    LOGGER.debug("Info: Creating/Updating item");
    /* Contains the cat-item */
    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    String itemType = getItemType(requestBody, response);

    LOGGER.debug("Info: itemType: " + itemType);
    RoutingContextHelper.setItemType(routingContext, itemType);
    Item item = createItemFromType(itemType, requestBody);
    LOGGER.debug("Success: Schema validation for item: " + item.toJson());

    // populating jwt authentication info ->
    String token = RoutingContextHelper.getToken(routingContext);
    JwtAuthenticationInfo.Builder jwtAuthenticationInfo =
        new JwtAuthenticationInfo.Builder()
            .setToken(token)
            .setApiEndpoint(routingContext.normalizedPath())
            .setItemType(itemType);
    if (routingContext.request().method().toString().equals(REQUEST_POST)) {
      jwtAuthenticationInfo.setMethod(REQUEST_POST);
    } else if (routingContext.request().method().toString().equals(REQUEST_PUT)) {
      jwtAuthenticationInfo.setMethod(REQUEST_PUT);
    }
    RoutingContextHelper.setJwtAuthInfo(routingContext, jwtAuthenticationInfo.build());
    RoutingContextHelper.setValidatedRequest(routingContext, requestBody);
    routingContext.next();
  }

  /**
   * Extracts the item type from the provided JSON request body.
   *
   * @param requestBody the {@link JsonObject}
   * @param response the {@link HttpServerResponse} object used to send an error response if the
   *     item type is invalid or missing.
   * @return a {@link String} representing the valid item type extracted from the request body.
   *     Returns an empty string if no valid type is found.
   * @throws IllegalArgumentException if the type field is not present or invalid in the request
   *     body.
   */
  private String getItemType(JsonObject requestBody, HttpServerResponse response) {
    Set<String> type = new HashSet<String>(new JsonArray().getList());
    try {
      type = new HashSet<String>(requestBody.getJsonArray(TYPE).getList());
    } catch (Exception e) {
      LOGGER.error("Fail: Invalid type");
      response
          .setStatusCode(400)
          .end(invalidSchemaResponse("Invalid type for item/type not present"));
    }
    type.retainAll(ITEM_TYPES);
    String itemType = type.toString().replaceAll("\\[", "").replaceAll("\\]", "");
    return itemType;
  }

  // Helper method to create Item based on item type
  private Item createItemFromType(String itemType, JsonObject requestBody) {
    return switch (itemType) {
      case ITEM_TYPE_OWNER -> new Owner(requestBody);
      case ITEM_TYPE_COS -> new COS(requestBody);
      case ITEM_TYPE_RESOURCE_SERVER -> new ResourceServer(requestBody);
      case ITEM_TYPE_PROVIDER -> new Provider(requestBody);
      case ITEM_TYPE_RESOURCE_GROUP -> new ResourceGroup(requestBody);
      case ITEM_TYPE_RESOURCE -> new Resource(requestBody);
      default -> throw new IllegalArgumentException("Invalid item type: " + itemType);
    };
  }

  /**
   * Validates the request for deleting an item by checking the item's existence and setting
   * authentication info. Continues to the next handler if validation succeeds.
   *
   * @param routingContext {@link RoutingContext} @TODO Throw error if load failed
   */
  // tag::db-service-calls[]
  public void validateIdHandler(RoutingContext routingContext) {

    /* Id in path param */
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    String itemId = routingContext.queryParams().get(ID);

    if (validateId(itemId)) {
      routingContext.next();
    } else {
      LOGGER.error("Fail: Invalid request payload");
      if (routingContext.request().method().toString().equals(REQUEST_GET)) {
        response.setStatusCode(400).end(invalidUuidResponse("The id is invalid"));
      } else {
        response
            .setStatusCode(400)
            .end(invalidSyntaxResponse("Fail: The syntax of the id is incorrect"));
      }
    }
  }

  /**
   * Check if the itemId contains certain invalid characters.
   *
   * @param itemId which is a String
   * @return true if the item ID contains valid characters, false otherwise
   */
  private boolean validateId(String itemId) {
    return UUID_PATTERN.matcher(itemId).matches();
  }
}
