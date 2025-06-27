/**
 * <h1>CrudApis.java</h1>
 * Callback handlers for CRUD
 */

package iudx.catalogue.server.apiserver;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.auditing.util.Constants.*;
import static iudx.catalogue.server.auditing.util.Constants.OPERATION;
import static iudx.catalogue.server.auditing.util.Constants.ROLE;
import static iudx.catalogue.server.authenticator.Constants.API_ENDPOINT;
import static iudx.catalogue.server.authenticator.Constants.TOKEN;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.util.Constants.DETAIL;
import static iudx.catalogue.server.util.Constants.ERROR;
import static iudx.catalogue.server.util.Constants.ID;
import static iudx.catalogue.server.util.Constants.METHOD;
import static iudx.catalogue.server.util.Constants.RESULTS;
import static iudx.catalogue.server.util.Constants.STATUS;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.apiserver.util.RespBuilder;
import iudx.catalogue.server.auditing.AuditingService;
import iudx.catalogue.server.auditing.util.AuditMetadata;
import iudx.catalogue.server.authenticator.AuthenticationService;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.util.Api;
import iudx.catalogue.server.validator.ValidatorService;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class CrudApis {


  private static final Logger LOGGER = LogManager.getLogger(CrudApis.class);
  private static final Pattern UUID_PATTERN =
      Pattern.compile(
          "^[a-zA-Z0-9]{8}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{12}$");
  private final Api api;
  private final boolean isUac;
  private DatabaseService dbService;
  private AuthenticationService authService;
  private ValidatorService validatorService;
  private AuditingService auditingService;
  private boolean hasAuditService = false;
  private String host;


  /**
   * Crud  constructor.
   *
   * @param api endpoint for base path
   * @TODO Throw error if load failed
   */
  public CrudApis(Api api, boolean isUac) {
    this.api = api;
    this.isUac = isUac;
  }

  public void setDbService(DatabaseService dbService) {
    this.dbService = dbService;
  }

  public void setAuthService(AuthenticationService authService) {
    this.authService = authService;
  }

  public void setValidatorService(ValidatorService validatorService) {
    this.validatorService = validatorService;
  }

  public void setAuditingService(AuditingService auditingService) {
    this.auditingService = auditingService;
    hasAuditService = true;
  }

  public void setHost(String host) {
    this.host = host;
  }

  /**
   * Create/Update Item.
   *
   * @param routingContext {@link RoutingContext}
   * @TODO Throw error if load failed
   */
  // tag::db-service-calls[]
  public void createItemHandler(RoutingContext routingContext) {

    LOGGER.debug("Info: Creating/Updating item");

    /* Contains the cat-item */
    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerResponse response = routingContext.response();

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    Set<String> type;
    try {
      JsonArray typeArray = requestBody.getJsonArray(TYPE);

      if (typeArray == null || typeArray.isEmpty()) {
        throw new IllegalArgumentException("Missing or empty 'type' field");
      }

      type = new HashSet<>(typeArray.getList());
      type.retainAll(ITEM_TYPES);

      if (type.isEmpty()) {
        throw new IllegalArgumentException("No valid types found in 'type' field");
      }
    } catch (Exception e) {
      LOGGER.error("Fail: Invalid type", e);
      RespBuilder respBuilder = new RespBuilder()
          .withType(TYPE_INVALID_SCHEMA)
          .withTitle(TITLE_INVALID_SCHEMA)
          .withDetail("Invalid type for item/type not present");
      response.setStatusCode(400)
          .end(respBuilder.getResponse());
      return;
    }
    String itemType = type.toString().replaceAll("\\[", "")
        .replaceAll("\\]", "");
    LOGGER.debug("Info: itemType;" + itemType);

    // Start insertion flow

    // Json schema validate item
    validatorService.validateSchema(
        requestBody,
        schValHandler -> {
          if (schValHandler.failed()) {
            try {
              JsonObject errJson = new JsonObject(schValHandler.cause().getMessage());
              response
                  .setStatusCode(400)
                  .end(
                      new RespBuilder()
                          .withType(TYPE_INVALID_SCHEMA)
                          .withTitle(TITLE_INVALID_SCHEMA)
                          .withDetail(errJson.getString(DETAIL, TITLE_INVALID_SCHEMA))
                          .withResult(errJson.getJsonArray(RESULTS, new JsonArray()))
                          .getResponse());
            } catch (Exception e) {
              LOGGER.error("Invalid error format", e);
              response
                  .setStatusCode(400)
                  .end(
                      new RespBuilder()
                          .withType(TYPE_INVALID_SCHEMA)
                          .withTitle(TITLE_INVALID_SCHEMA)
                          .withDetail(TITLE_INVALID_SCHEMA)
                          .withResult(schValHandler.cause().getMessage())
                          .getResponse());
            }
            return;
          }
          if (schValHandler.succeeded()) {
            LOGGER.debug("Success: Schema validation");

            JsonObject jwtAuthenticationInfo = new JsonObject();

            // populating jwt authentication info ->
            jwtAuthenticationInfo
                .put(TOKEN, routingContext.get(HEADER_TOKEN))
                .put(METHOD, REQUEST_POST)
                .put(API_ENDPOINT, api.getRouteItems())
                .put(ITEM_TYPE, itemType);
            if (itemType.equalsIgnoreCase(ITEM_TYPE_AI_MODEL)
                || itemType.equalsIgnoreCase(ITEM_TYPE_DATA_BANK)) {
              jwtAuthenticationInfo.put(ORGANIZATION_ID, requestBody.getString(ORGANIZATION_ID));
            }

            if (isUac) {
              handleItemCreation(routingContext, requestBody, response, jwtAuthenticationInfo);
            } else {
              if (itemType.equalsIgnoreCase(ITEM_TYPE_COS)
                  || itemType.equalsIgnoreCase(ITEM_TYPE_OWNER)
                  || itemType.equalsIgnoreCase(ITEM_TYPE_APPS)
                  || itemType.equalsIgnoreCase(ITEM_TYPE_AI_MODEL)
                  || itemType.equalsIgnoreCase(ITEM_TYPE_DATA_BANK)) {
                handleItemCreation(routingContext, requestBody, response, jwtAuthenticationInfo);
              } else if (itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_SERVER)) {
                handleItemCreation(routingContext, requestBody, response, jwtAuthenticationInfo);
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

                        jwtAuthenticationInfo.put(RESOURCE_SERVER_URL, rsUrl);
                        requestBody.put(RESOURCE_SERVER_URL, rsUrl);
                        requestBody.put(COS_ITEM, cosId);
                        handleItemCreation(
                            routingContext, requestBody, response, jwtAuthenticationInfo);
                      } else {
                        response
                            .setStatusCode(400)
                            .end(
                                new RespBuilder()
                                    .withType(TYPE_LINK_VALIDATION_FAILED)
                                    .withTitle(TITLE_LINK_VALIDATION_FAILED)
                                    .withDetail("Resource Server not found")
                                    .getResponse());
                      }
                    });
              } else {
                Future<JsonObject> ownerUserIdFuture =
                    getParentObjectInfo(requestBody.getString(PROVIDER));
                // add provider kc id to requestBody
                ownerUserIdFuture.onComplete(
                    ownerUserId -> {
                      if (ownerUserId.succeeded()) {
                        LOGGER.debug(ownerUserId.result());
                        String kcId = ownerUserId.result().getString(PROVIDER_USER_ID);
                        String rsUrl = ownerUserId.result().getString(RESOURCE_SERVER_URL);
                        // cosId is used for relationship apis

                        jwtAuthenticationInfo.put(PROVIDER_USER_ID, kcId);
                        jwtAuthenticationInfo.put(RESOURCE_SERVER_URL, rsUrl);
                        requestBody.put(PROVIDER_USER_ID, kcId);

                        String cosId = ownerUserId.result().getString(COS_ITEM);
                        requestBody.put(COS_ITEM, cosId);
                        handleItemCreation(
                            routingContext, requestBody, response, jwtAuthenticationInfo);
                      } else {
                        response
                            .setStatusCode(400)
                            .end(
                                new RespBuilder()
                                    .withType(TYPE_LINK_VALIDATION_FAILED)
                                    .withTitle(TITLE_LINK_VALIDATION_FAILED)
                                    .withDetail("Provider not found")
                                    .getResponse());
                      }
                    });
              }
            }
          }
        });
  }


  private void handleItemCreation(RoutingContext routingContext,
                                  JsonObject requestBody,
                                  HttpServerResponse response,
                                  JsonObject jwtAuthenticationInfo) {
    authService.tokenInterospect(new JsonObject(),
        jwtAuthenticationInfo, authHandler -> {
          if (authHandler.failed()) {
            LOGGER.error("Error: " + authHandler.cause().getMessage());
            response.setStatusCode(401)
                .end(new RespBuilder()
                    .withType(TYPE_TOKEN_INVALID)
                    .withTitle(TITLE_TOKEN_INVALID)
                    .withDetail(authHandler.cause().getMessage())
                    .getResponse());
          } else {
            LOGGER.debug("Success: JWT Auth successful: {}", authHandler.result());

            if (jwtAuthenticationInfo.getString(ITEM_TYPE).equals(ITEM_TYPE_AI_MODEL)
                || jwtAuthenticationInfo.getString(ITEM_TYPE).equals(ITEM_TYPE_DATA_BANK)
                || jwtAuthenticationInfo.getString(ITEM_TYPE).equals(ITEM_TYPE_APPS)) {
              String kcId = authHandler.result().getString(SUB);
              String orgName = authHandler.result().getString(ORGANIZATION_NAME);
              requestBody.put(PROVIDER_USER_ID, kcId);
              requestBody.put(DEPARTMENT, orgName);
              requestBody.put(UPLOADED_BY, orgName);
            }

            requestBody.put(HTTP_METHOD, routingContext.request().method().toString());
            /* Link Validating the request to ensure item correctness */
            validatorService.validateItem(requestBody, valhandler -> {
              if (valhandler.failed()) {
                LOGGER.error("Fail: Item validation failed;" + valhandler.cause().getMessage());
                if (valhandler.cause().getMessage().contains("validation failed. Incorrect id")) {
                  response.setStatusCode(400)
                      .end(new RespBuilder()
                          .withType(TYPE_INVALID_UUID)
                          .withTitle(TITLE_INVALID_UUID)
                          .withDetail("Syntax of the UUID is incorrect")
                          .getResponse());
                  return;
                }
                response.setStatusCode(400)
                    .end(new RespBuilder()
                        .withType(TYPE_LINK_VALIDATION_FAILED)
                        .withTitle(TITLE_LINK_VALIDATION_FAILED)
                        .withDetail(valhandler.cause().getMessage())
                        .getResponse());
              }
              if (valhandler.succeeded()) {
                LOGGER.debug("Success: Item link validation");

                // If post, create. If put, update
                if (routingContext.request().method().toString() == REQUEST_POST) {
                  /* Requesting database service, creating a item */
                  LOGGER.debug("Info: Inserting item");
                  dbService.createItem(valhandler.result(), dbhandler -> {
                    if (dbhandler.failed()) {
                      LOGGER.error("Fail: Item creation;" + dbhandler.cause().getMessage());
                      response.setStatusCode(400)
                          .end(dbhandler.cause().getMessage());
                    }
                    if (dbhandler.succeeded()) {
                      LOGGER.info("Success: Item created;");
                      response.setStatusCode(201)
                          .end(dbhandler.result().toString());

                      String itemType =
                          dbhandler.result().getJsonObject(RESULTS).getJsonArray(TYPE).getString(0);
                      String itemName =
                          dbhandler.result().getJsonObject(RESULTS).getString(NAME);
                      if (hasAuditService) {
                        updateAuditTable(new AuditMetadata(valhandler.result().getString(ID),
                            valhandler.result().getString(SHORTDESCRIPTION),
                            api.getRouteItems(), REQUEST_POST, itemType, itemName,
                            authHandler.result().getString(USER_ID),
                            authHandler.result().getString(USER_ROLE),
                            authHandler.result().getString(ORGANIZATION_ID),
                            authHandler.result().getString(ORGANIZATION_NAME)));
                      }
                    }
                  });
                } else {
                  LOGGER.debug("Info: Updating item");
                  /* Requesting database service, creating a item */
                  dbService.updateItem(valhandler.result(), dbhandler -> {
                    if (dbhandler.succeeded()) {
                      LOGGER.info("Success: Item updated;");
                      response.setStatusCode(200)
                          .end(dbhandler.result().toString());

                      String itemType =
                          dbhandler.result().getJsonObject(RESULTS).getJsonArray(TYPE).getString(0);
                      String itemName = dbhandler.result().getJsonObject(RESULTS).getString(NAME);
                      if (hasAuditService) {
                        updateAuditTable(new AuditMetadata(valhandler.result().getString(ID),
                            valhandler.result().getString(SHORTDESCRIPTION),
                            api.getRouteItems(), REQUEST_PUT, itemType, itemName,
                            authHandler.result().getString(USER_ID),
                            authHandler.result().getString(USER_ROLE),
                            authHandler.result().getString(ORGANIZATION_ID),
                            authHandler.result().getString(ORGANIZATION_NAME)));
                      }
                    } else if (dbhandler.failed()) {
                      LOGGER.error("Fail: Item update;" + dbhandler.cause().getMessage());
                      if (dbhandler.cause().getLocalizedMessage().contains("Doc doesn't exist")) {
                        response.setStatusCode(404);
                      } else {
                        response.setStatusCode(400);
                      }
                      response.end(dbhandler.cause().getMessage());
                    }
                  });
                }
              }
            });
          }
        });
  }

  /**
   * Get Item.
   *
   * @param routingContext {@link RoutingContext}
   * @TODO Throw error if load failed
   */
  // tag::db-service-calls[]
  public void getItemHandler(RoutingContext routingContext) {

    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    String itemId = routingContext.queryParams().get(ID);
    String token = routingContext.get(HEADER_TOKEN);

    LOGGER.debug("Info: Getting item; id=" + itemId);

    JsonObject requestBody = new JsonObject().put(ID, itemId);

    if (!validateId(itemId)) {
      LOGGER.error("Fail: Invalid request payload");
      response.setStatusCode(400)
          .end(new RespBuilder()
              .withType(TYPE_INVALID_UUID)
              .withTitle(TITLE_INVALID_UUID)
              .withDetail("The id is invalid")
              .getResponse());
      return;
    }

    // Token is optional: if present, validate & audit
    if (token != null && !token.isEmpty()) {
      JsonObject jwtAuthenticationInfo = new JsonObject()
          .put(TOKEN, token)
          .put(METHOD, REQUEST_GET)
          .put(API_ENDPOINT, api.getRouteItems());

      authService.tokenInterospect(new JsonObject(), jwtAuthenticationInfo, authHandler -> {
        if (authHandler.failed()) {
          LOGGER.warn("Warning: Token introspection failed, skipping audit");
          fetchItemWithoutAudit(requestBody, response);
        } else {
          dbService.getItem(requestBody, dbhandler -> {
            if (dbhandler.succeeded()) {
              if (dbhandler.result().getInteger(TOTAL_HITS) == 0) {
                LOGGER.error("Fail: Item not found");
                JsonObject result = dbhandler.result();
                result.put(STATUS, ERROR).put(TYPE, TYPE_ITEM_NOT_FOUND)
                    .put("detail", "doc doesn't exist");
                response.setStatusCode(404).end(result.toString());
              } else {
                LOGGER.info("Success: Retrieved item");
                response.setStatusCode(200).end(dbhandler.result().toString());

                String itemType = dbhandler.result().getJsonArray(RESULTS).getJsonObject(0)
                    .getJsonArray(TYPE).getString(0);
                String itemName =
                    dbhandler.result().getJsonArray(RESULTS).getJsonObject(0).getString(NAME);
                String shortDescription =
                    dbhandler.result().getJsonArray(RESULTS).getJsonObject(0)
                        .getString(SHORTDESCRIPTION);
                if (hasAuditService) {
                  updateAuditTable(new AuditMetadata(itemId, shortDescription, api.getRouteItems(),
                      REQUEST_GET, itemType, itemName, authHandler.result().getString(USER_ID),
                      authHandler.result().getString(USER_ROLE),
                      authHandler.result().getString(ORGANIZATION_ID),
                      authHandler.result().getString(ORGANIZATION_NAME)));
                }
              }
            } else {
              LOGGER.error("Fail: DB Error;" + dbhandler.cause().getMessage());
              response.setStatusCode(400).end(dbhandler.cause().getMessage());
            }
          });
        }
      });
    } else {
      fetchItemWithoutAudit(requestBody, response);
    }
  }

  private void fetchItemWithoutAudit(JsonObject requestBody, HttpServerResponse response) {
    dbService.getItem(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        if (dbhandler.result().getInteger(TOTAL_HITS) == 0) {
          LOGGER.error("Fail: Item not found");
          JsonObject result = dbhandler.result();
          result.put(STATUS, ERROR).put(TYPE, TYPE_ITEM_NOT_FOUND)
              .put("detail", "doc doesn't exist");
          response.setStatusCode(404).end(result.toString());
        } else {
          LOGGER.info("Success: Retrieved item (no audit)");
          response.setStatusCode(200).end(dbhandler.result().toString());
        }
      } else {
        LOGGER.error("Fail: DB Error;" + dbhandler.cause().getMessage());
        response.setStatusCode(400).end(dbhandler.cause().getMessage());
      }
    });
  }

  /**
   * Delete Item.
   *
   * @param routingContext {@link RoutingContext} @TODO Throw error if load failed
   */
  // tag::db-service-calls[]
  public void deleteItemHandler(RoutingContext routingContext) {

    /* Id in path param */
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    /* JsonObject of authentication related information */
    JsonObject jwtAuthenticationInfo = new JsonObject();
    JsonObject requestBody = new JsonObject();

    String itemId = routingContext.queryParams().get(ID);
    requestBody.put(ID, itemId);

    LOGGER.debug("Info: Deleting item; id=" + itemId);

    //    if (validateId(itemId) == true) {

    //  populating JWT authentication info ->
    jwtAuthenticationInfo
        .put(TOKEN, routingContext.get(HEADER_TOKEN))
        .put(METHOD, REQUEST_DELETE)
        .put(API_ENDPOINT, api.getRouteItems());
    if (validateId(itemId)) {
      Future<JsonObject> itemTypeFuture = getParentObjectInfo(itemId);
      itemTypeFuture.onComplete(
          itemTypeHandler -> {
            if (itemTypeHandler.succeeded()) {

              String organizationId = itemTypeHandler.result().getString(ORGANIZATION_ID);
              jwtAuthenticationInfo.put(ORGANIZATION_ID, organizationId);
              jwtAuthenticationInfo.put(NAME, itemTypeHandler.result().getString(NAME));
              jwtAuthenticationInfo.put(SHORTDESCRIPTION, itemTypeHandler.result()
                  .getString(SHORTDESCRIPTION));
              jwtAuthenticationInfo.put(PROVIDER_USER_ID,
                  itemTypeHandler.result().getString(PROVIDER_USER_ID));
              Set<String> types =
                  new HashSet<String>(itemTypeHandler.result().getJsonArray(TYPE).getList());
              types.retainAll(ITEM_TYPES);
              String itemType = types.toString().replaceAll("\\[", "")
                  .replaceAll("\\]", "");
              LOGGER.debug("itemType : {} ", itemType);
              jwtAuthenticationInfo.put(ITEM_TYPE, itemType);
              if (isUac) {
                handleItemDeletion(response, jwtAuthenticationInfo, requestBody, itemId);
              } else {
                if (itemType.equalsIgnoreCase(ITEM_TYPE_COS)
                    || itemType.equalsIgnoreCase(ITEM_TYPE_OWNER)) {
                  handleItemDeletion(response, jwtAuthenticationInfo, requestBody, itemId);
                } else if (itemType.equalsIgnoreCase(ITEM_TYPE_DATA_BANK)
                    || itemType.equalsIgnoreCase(ITEM_TYPE_AI_MODEL)
                    || itemType.equalsIgnoreCase(ITEM_TYPE_APPS)) {
                  handleItemDeletion(response, jwtAuthenticationInfo, requestBody, itemId);
                } else if (itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_SERVER)) {
                  handleItemDeletion(response, jwtAuthenticationInfo, requestBody, itemId);
                } else if (itemType.equalsIgnoreCase(ITEM_TYPE_PROVIDER)) {
                  LOGGER.debug(itemTypeHandler.result());
                  String rsUrl = itemTypeHandler.result().getString(RESOURCE_SERVER_URL, "");
                  jwtAuthenticationInfo.put(RESOURCE_SERVER_URL, rsUrl);
                  handleItemDeletion(response, jwtAuthenticationInfo, requestBody, itemId);
                } else {
                  LOGGER.debug(itemTypeHandler.result());
                  Future<JsonObject> providerInfoFuture =
                      getParentObjectInfo(itemTypeHandler.result().getString(PROVIDER));
                  providerInfoFuture.onComplete(
                      providerInfo -> {
                        if (providerInfo.succeeded()) {
                          LOGGER.debug(providerInfo.result());
                          String rsUrl = providerInfo.result().getString(RESOURCE_SERVER_URL, "");
                          String ownerUserId =
                              providerInfo.result().getString(PROVIDER_USER_ID, "");
                          jwtAuthenticationInfo.put(RESOURCE_SERVER_URL, rsUrl);
                          jwtAuthenticationInfo.put(PROVIDER_USER_ID, ownerUserId);
                          handleItemDeletion(response, jwtAuthenticationInfo, requestBody, itemId);
                        } else {
                          response
                              .setStatusCode(400)
                              .end(
                                  new RespBuilder()
                                      .withType(TYPE_ITEM_NOT_FOUND)
                                      .withTitle(TITLE_ITEM_NOT_FOUND)
                                      .withDetail("item is not found")
                                      .getResponse());
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
    } else {
      LOGGER.error("Fail: Invalid request payload");
      response
          .setStatusCode(400)
          .end(
              new RespBuilder()
                  .withType(TYPE_INVALID_SYNTAX)
                  .withTitle(TITLE_INVALID_SYNTAX)
                  .withDetail("Fail: The syntax of the id is incorrect")
                  .getResponse());
    }
  }

  private void handleItemDeletion(HttpServerResponse response,
                                  JsonObject jwtAuthenticationInfo,
                                  JsonObject requestBody, String itemId) {
    authService.tokenInterospect(new JsonObject(),
        jwtAuthenticationInfo, authHandler -> {
          if (authHandler.failed()) {
            LOGGER.error("Error: " + authHandler.cause().getMessage());
            response.setStatusCode(401)
                .end(new RespBuilder()
                    .withType(TYPE_TOKEN_INVALID)
                    .withTitle(TITLE_TOKEN_INVALID)
                    .withDetail(authHandler.cause().getMessage())
                    .getResponse());
          } else {
            LOGGER.debug("Success: JWT Auth successful");
            /* Requesting database service, deleting a item */
            dbService.deleteItem(requestBody, dbHandler -> {
              if (dbHandler.succeeded()) {
                LOGGER.info("Success: Item deleted;");
                LOGGER.debug(dbHandler.result().toString());
                if (dbHandler.result().getString(STATUS).equals(TITLE_SUCCESS)) {
                  response.setStatusCode(200).end(dbHandler.result().toString());
                  String itemType = jwtAuthenticationInfo.getString(ITEM_TYPE);
                  String itemName = jwtAuthenticationInfo.getString(NAME);
                  String shortDescription = jwtAuthenticationInfo.getString(SHORTDESCRIPTION);
                  if (hasAuditService) {
                    updateAuditTable(new AuditMetadata(itemId, shortDescription,
                        api.getRouteItems(), REQUEST_DELETE, itemType, itemName,
                        authHandler.result().getString(USER_ID),
                        authHandler.result().getString(USER_ROLE),
                        authHandler.result().getString(ORGANIZATION_ID),
                        authHandler.result().getString(ORGANIZATION_NAME)));
                  }
                } else {
                  response.setStatusCode(404)
                      .end(dbHandler.result().toString());
                }
              } else if (dbHandler.failed()) {
                if (dbHandler.cause().getMessage().contains(TYPE_ITEM_NOT_FOUND)) {
                  response.setStatusCode(404)
                      .end(dbHandler.cause().getMessage());
                } else {
                  response.setStatusCode(400)
                      .end(dbHandler.cause().getMessage());
                }
              }
            });
          }
        });
  }

  Future<JsonObject> getParentObjectInfo(String itemId) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject req = new JsonObject().put(ID, itemId)
        .put(SEARCH_TYPE, "getParentObjectInfo");

    LOGGER.debug(req);
    dbService.searchQuery(
        req,
        handler -> {
          if (handler.succeeded()) {
            if (handler.result().getInteger(TOTAL_HITS) != 1) {
              RespBuilder respBuilder = new RespBuilder()
                  .withType(TYPE_ITEM_NOT_FOUND)
                  .withTitle(TITLE_ITEM_NOT_FOUND)
                  .withDetail("Fail: Doc doesn't exist, can't perform operation");
              promise.fail(respBuilder.getResponse());
            } else {
              promise.complete(handler.result().getJsonArray("results").getJsonObject(0));
            }
          } else {
            promise.fail(handler.cause());
          }
        });
    return promise.future();
  }

  /**
   * Creates a new catalogue instance and handles the request/response flow.
   *
   * @param routingContext the routing context for handling HTTP requests and responses
   * @param catAdmin       he catalogue admin for the item
   * @throws RuntimeException if item creation fails
   */
  public void createInstanceHandler(RoutingContext routingContext, String catAdmin) {

    LOGGER.debug("Info: Creating new instance");

    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);


    JsonObject authenticationInfo = new JsonObject();

    String instance = routingContext.queryParams().get(ID);


    //  Start insertion flow


    // Json schema validate item
    authenticationInfo.put(TOKEN,
            routingContext.get(HEADER_TOKEN))
        .put(METHOD, REQUEST_POST)
        .put(API_ENDPOINT, api.getRouteInstance())
        .put(ITEM_TYPE, ITEM_TYPE_INSTANCE)
        .put(ID, host);
    // Introspect token and authorize operation
    authService.tokenInterospect(new JsonObject(), authenticationInfo, authhandler -> {
      if (authhandler.failed()) {
        response.setStatusCode(401)
            .end(new RespBuilder()
                .withType(TYPE_TOKEN_INVALID)
                .withTitle(TITLE_TOKEN_INVALID)
                .withDetail(authhandler.cause().getMessage())
                .getResponse());
      } else {
        /* INSTANCE = "" to make sure createItem can be used for onboarding instance and items */
        JsonObject body = new JsonObject().put(ID, instance)
            .put(TYPE, new JsonArray().add(ITEM_TYPE_INSTANCE))
            .put(INSTANCE, "");
        dbService.createItem(body, res -> {
          if (res.succeeded()) {
            LOGGER.info("Success: Instance created;");
            response.setStatusCode(201)
                .end(res.result().toString());
            // TODO: call auditing service here
          } else {
            LOGGER.error("Fail: Creating instance");
            response.setStatusCode(400).end(res.cause().getMessage());
          }
        });
        LOGGER.debug("Success: Authenticated instance creation request");
      }
    });
  }

  /**
   * Deletes the specified instance from the database.
   *
   * @param routingContext the routing context
   * @param catAdmin       the catalogue admin
   * @throws NullPointerException if routingContext is null
   * @throws RuntimeException     if the instance cannot be deleted
   * @TODO call auditing service after successful deletion
   */
  public void deleteInstanceHandler(RoutingContext routingContext, String catAdmin) {

    LOGGER.debug("Info: Deleting instance");

    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject authenticationInfo = new JsonObject();

    String instance = routingContext.queryParams().get(ID);


    //  Start insertion flow


    // Json schema validate item
    authenticationInfo.put(TOKEN, routingContext.get(HEADER_TOKEN))
        .put(METHOD, REQUEST_DELETE)
        .put(API_ENDPOINT, api.getRouteInstance())
        .put(ITEM_TYPE, ITEM_TYPE_INSTANCE)
        .put(ID, host);
    // Introspect token and authorize operation
    authService.tokenInterospect(new JsonObject(), authenticationInfo, authhandler -> {
      if (authhandler.failed()) {
        response.setStatusCode(401)
            .end(new RespBuilder()
                .withType(TYPE_TOKEN_INVALID)
                .withTitle(TITLE_TOKEN_INVALID)
                .withDetail(authhandler.cause().getMessage())
                .getResponse());
      } else {
        /* INSTANCE = "" to make sure createItem can be used for onboarding instance and items */
        JsonObject body = new JsonObject().put(ID, instance)
            .put(INSTANCE, "");
        dbService.deleteItem(body, res -> {
          if (res.succeeded()) {
            LOGGER.info("Success: Instance deleted;");
            response.setStatusCode(200)
                .end(res.result().toString());
            // TODO: call auditing service here
          } else {
            LOGGER.error("Fail: Deleting instance");
            response.setStatusCode(404).end(res.cause().getMessage());
          }
        });
        LOGGER.debug("Success: Authenticated instance creation request");
      }
    });
  }

  /**
   * Check if the itemId contains certain invalid characters.
   *
   * @param itemId which is a String
   * @return true if the item ID contains invalid characters, false otherwise
   */
  private boolean validateId(String itemId) {
    return UUID_PATTERN.matcher(itemId).matches();

  }

  /**
   * Publishes audit information to the auditing service.
   *
   * <p>This method builds a structured audit log from the provided {@link AuditMetadata} instance,
   * which contains details about the API request and user information. It determines the operation
   * type and user role based on the HTTP method and user role embedded within the metadata. The
   * audit record is then asynchronously published to a RabbitMQ-based auditing service.
   *
   * @param metadata An instance of {@link AuditMetadata} containing information such as:
   *                 <ul>
   *                   <li>Item ID</li>
   *                   <li>API endpoint</li>
   *                   <li>HTTP method</li>
   *                   <li>Item type and name</li>
   *                   <li>User ID and user role</li>
   *                 </ul>
   */
  private void updateAuditTable(AuditMetadata metadata) {
    LOGGER.info("Updating audit table on successful transaction");

    String createdAt = LocalDateTime.now().toString();

    JsonObject auditInfo = new JsonObject()
        .put(ASSET_ID, metadata.itemId)
        .put(API, metadata.apiEndpoint)
        .put(METHOD, metadata.httpMethod)
        .put(ASSET_TYPE, metadata.itemType)
        .put(ASSET_NAME, metadata.itemName)
        .put(CREATED_AT, createdAt)
        .put(USERID, metadata.userId)
        .put(ROLE, metadata.getRole())
        .put(ORG_ID, metadata.orgId)
        .put(ORG_NAME, metadata.orgName)
        .put(OPERATION, metadata.getOperation())
        .put(SHORT_DESCRIPTION, metadata.shortDescription)
        .put(MYACTIVITY_ENABLED, true);

    LOGGER.debug("audit data: " + auditInfo.encodePrettily());

    auditingService.insertAuditngValuesInRmq(auditInfo, auditHandler -> {
      if (auditHandler.succeeded()) {
        LOGGER.info("message published in RMQ.");
      } else {
        LOGGER.error("failed to publish message in RMQ.", auditHandler.cause());
      }
    });
  }

}
