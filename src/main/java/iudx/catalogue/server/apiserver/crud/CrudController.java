package iudx.catalogue.server.apiserver.crud;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.common.util.ResponseBuilderUtil.*;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.apiserver.item.handler.ItemLinkValidationHandler;
import iudx.catalogue.server.apiserver.item.handler.ItemSchemaHandler;
import iudx.catalogue.server.auditing.handler.AuditHandler;
import iudx.catalogue.server.authenticator.handler.authentication.AuthHandler;
import iudx.catalogue.server.authenticator.handler.authorization.AuthValidationHandler;
import iudx.catalogue.server.authenticator.handler.authorization.AuthorizationHandler;
import iudx.catalogue.server.authenticator.model.DxRole;
import iudx.catalogue.server.authenticator.model.JwtAuthenticationInfo;
import iudx.catalogue.server.common.RoutingContextHelper;
import iudx.catalogue.server.exceptions.FailureHandler;
import iudx.catalogue.server.validator.service.ValidatorService;
import java.util.NoSuchElementException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class handles CRUD (Create, Read, Update, Delete) operations for the catalogue items.
 *
 * <p>It manages routes for CRUD endpoints, validation of request payloads, and the coordination
 *
 * <p>between services for item operations.
 */
public class CrudController {
  private final Logger LOGGER = LogManager.getLogger(CrudController.class);
  private final CrudService crudService;
  private final boolean isUac;
  private final AuthHandler authHandler;
  private final AuthValidationHandler validateToken;
  private final AuthorizationHandler authorizationHandler;
  private final ItemSchemaHandler itemSchemaHandler;
  private final ItemLinkValidationHandler itemLinkValidationHandler;
  private final AuditHandler auditHandler;
  private final FailureHandler failureHandler;
  private final String host;

  /**
   * CrudController constructor.
   *
   * @param isUac flag indicating if UAC is enabled
   * @param crudService service for CRUD operations
   */
  public CrudController(
      boolean isUac,
      String host,
      CrudService crudService,
      ValidatorService validatorService,
      AuthHandler authHandler,
      AuthValidationHandler validateToken,
      AuthorizationHandler authorizationHandler,
      AuditHandler auditHandler,
      FailureHandler failureHandler) {
    this.isUac = isUac;
    this.host = host;
    this.crudService = crudService;
    this.authHandler = authHandler;
    this.validateToken = validateToken;
    this.authorizationHandler = authorizationHandler;
    this.itemSchemaHandler = new ItemSchemaHandler();
    this.itemLinkValidationHandler = new ItemLinkValidationHandler(crudService, validatorService);
    this.auditHandler = auditHandler;
    this.failureHandler = failureHandler;
  }

  /**
   * Configures the routes for CRUD operations, including creation, update, retrieval, and deletion
   *
   * <p>of items, along with validation, authorization, and auditing functionalities.
   *
   * @return
   */
  public Router init(Router router) {

    /* Create Item - Body contains data */
    router
        .post(ROUTE_ITEMS)
        .consumes(MIME_APPLICATION_JSON)
        .produces(MIME_APPLICATION_JSON)
        .handler(itemSchemaHandler::verifyAuthHeader)
        .handler(itemSchemaHandler::validateItemSchema)
        .handler(
            routingContext -> {
              if (!isUac) {
                itemLinkValidationHandler.handleItemTypeCases(routingContext);
              } else {
                routingContext.next();
              }
            })
        .handler(authHandler)
        .handler(validateToken)
        .handler(itemLinkValidationHandler::itemLinkValidation)
        .handler(
            authorizationHandler.forRoleAndEntityAccess(
                DxRole.COS_ADMIN, DxRole.ADMIN, DxRole.PROVIDER, DxRole.DELEGATE))
        .handler(this::createOrUpdateItemHandler)
        .handler(
            routingContext -> {
              if (!isUac) {
                auditHandler.handle(routingContext, routingContext.normalizedPath());
              } else {
                routingContext.next();
              }
            })
        .failureHandler(failureHandler);

    /* Update Item - Body contains data */
    router
        .put(ROUTE_UPDATE_ITEMS)
        .consumes(MIME_APPLICATION_JSON)
        .produces(MIME_APPLICATION_JSON)
        .handler(itemSchemaHandler::verifyAuthHeader)
        .handler(itemSchemaHandler::validateItemSchema)
        .handler(
            routingContext -> {
              if (!isUac) {
                itemLinkValidationHandler.handleItemTypeCases(routingContext);
              } else {
                routingContext.next();
              }
            })
        .handler(authHandler)
        .handler(validateToken)
        .handler(itemLinkValidationHandler::itemLinkValidation)
        .handler(
            authorizationHandler.forRoleAndEntityAccess(
                DxRole.COS_ADMIN, DxRole.ADMIN, DxRole.PROVIDER, DxRole.DELEGATE))
        .handler(this::createOrUpdateItemHandler)
        .handler(
            routingContext -> {
              if (!isUac) {
                auditHandler.handle(routingContext, routingContext.normalizedPath());
              } else {
                routingContext.next();
              }
            })
        .failureHandler(failureHandler);

    /* Get Item */
    router
        .get(ROUTE_ITEMS)
        .produces(MIME_APPLICATION_JSON)
        .handler(itemSchemaHandler::validateIdHandler)
        .handler(this::getItemHandler)
        .failureHandler(failureHandler);

    /* Delete Item - Query param contains id */
    router
        .delete(ROUTE_DELETE_ITEMS)
        .produces(MIME_APPLICATION_JSON)
        .handler(itemSchemaHandler::verifyAuthHeader)
        .handler(itemSchemaHandler::validateIdHandler)
        .handler(
            routingContext ->
                itemLinkValidationHandler.validateDeleteItemHandler(routingContext, isUac))
        .handler(authHandler)
        .handler(validateToken)
        .handler(
            authorizationHandler.forRoleAndEntityAccess(
                DxRole.COS_ADMIN, DxRole.ADMIN, DxRole.PROVIDER, DxRole.DELEGATE))
        .handler(this::deleteItemHandler)
        .handler(
            routingContext -> {
              if (!isUac) {
                auditHandler.handle(routingContext, routingContext.normalizedPath());
              } else {
                routingContext.next();
              }
            })
        .failureHandler(failureHandler);

    /* Create instance - Instance name in query param */
    router
        .post(ROUTE_INSTANCE)
        .produces(MIME_APPLICATION_JSON)
        .handler(itemSchemaHandler::verifyAuthHeader)
        .handler(routingContext -> populateAuthInfo(routingContext, REQUEST_POST))
        // Populate authentication info
        .handler(authHandler) // Authentication
        .handler(validateToken)
        .handler(this::createInstanceHandler)
        .failureHandler(failureHandler);

    /* Delete instance - Instance name in query param */
    router
        .delete(ROUTE_INSTANCE)
        .produces(MIME_APPLICATION_JSON)
        .handler(itemSchemaHandler::verifyAuthHeader)
        .handler(routingContext -> populateAuthInfo(routingContext, REQUEST_DELETE))
        // Populate authentication info
        .handler(authHandler) // Authentication
        .handler(validateToken)
        .handler(this::deleteInstanceHandler)
        .failureHandler(failureHandler);
    return router;
  }

  private void populateAuthInfo(RoutingContext routingContext, String method) {
    String token = RoutingContextHelper.getToken(routingContext);

    JwtAuthenticationInfo jwtAuthenticationInfo =
        new JwtAuthenticationInfo.Builder()
            .setToken(token)
            .setMethod(method)
            .setApiEndpoint(routingContext.normalizedPath())
            .setItemType(ITEM_TYPE_INSTANCE)
            .setId(host)
            .build();

    RoutingContextHelper.setJwtAuthInfo(routingContext, jwtAuthenticationInfo);
    routingContext.next();
  }

  void createOrUpdateItemHandler(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonObject validatedRequest = RoutingContextHelper.getValidatedRequest(routingContext);

    // If post then create. Else, update
    if (routingContext.request().method().toString().equals(REQUEST_POST)) {
      crudService
          .createItem(validatedRequest)
          .onComplete(
              itemHandler -> {
                if (itemHandler.failed()) {
                  LOGGER.error("Failed to create item: " + itemHandler.cause().getMessage());
                  response.setStatusCode(400).end(itemHandler.cause().getMessage());
                } else {
                  JsonObject createdItem = itemHandler.result();
                  response.setStatusCode(201).end(createdItem.encodePrettily());
                  routingContext.next();
                }
              });
    } else {
      crudService
          .updateItem(validatedRequest)
          .onComplete(
              itemHandler -> {
                if (itemHandler.failed()) {
                  LOGGER.error("Failed to update item: " + itemHandler.cause().getMessage());
                  if (itemHandler.cause().getMessage().contains("Doc doesn't exist")) {
                    routingContext
                        .response()
                        .setStatusCode(404)
                        .end(itemHandler.cause().getMessage());
                  } else {
                    routingContext
                        .response()
                        .setStatusCode(400)
                        .end(itemHandler.cause().getMessage());
                  }
                } else {
                  JsonObject updatedItem = itemHandler.result();
                  routingContext.response().setStatusCode(200).end(updatedItem.encodePrettily());
                  routingContext.next();
                }
              });
    }
  }

  /**
   * Get Item.
   *
   * @param routingContext {@link RoutingContext} @ TODO: Throw error if load failed
   */
  // tag::db-service-calls[]
  public void getItemHandler(RoutingContext routingContext) {
    /* Id in path param */
    HttpServerResponse response = routingContext.response();
    String itemId = routingContext.queryParams().get(ID);
    LOGGER.debug("Info: Getting item; id=" + itemId);
    JsonObject requestBody = new JsonObject().put(ID, itemId);

    crudService
        .getItem(requestBody)
        .onComplete(
            getHandler -> {
              if (getHandler.succeeded()) {
                JsonObject retrievedItem = getHandler.result();
                response.setStatusCode(200).end(retrievedItem.toString());
              } else {
                if (getHandler.cause().getLocalizedMessage().contains("urn:dx:cat:ItemNotFound")) {
                  LOGGER.error("Fail: Item not found");
                  JsonObject errorResponse =
                      new JsonObject()
                          .put(TYPE, TYPE_ITEM_NOT_FOUND)
                          .put(STATUS, ERROR)
                          .put(TOTAL_HITS, 0)
                          .put(RESULTS, new JsonArray())
                          .put(DETAIL, "doc doesn't exist");
                  response.setStatusCode(404).end(errorResponse.toString());
                } else {
                  LOGGER.error("Fail: Item retrieval failed; " + getHandler.cause().getMessage());
                  response.setStatusCode(400).end(getHandler.cause().getMessage());
                }
              }
            });
  }

  /**
   * Delete Item.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void deleteItemHandler(RoutingContext routingContext) {
    JsonObject requestBody = new JsonObject();
    String itemId = routingContext.queryParams().get(ID);
    requestBody.put(ID, itemId);

    crudService
        .deleteItem(requestBody)
        .onSuccess(
            result -> {
              LOGGER.info("Item deleted successfully");
              routingContext.response().setStatusCode(200).end(result.toString());
              routingContext.next();
            })
        .onFailure(
            throwable -> {
              LOGGER.error("Failed to delete item", throwable);
              if (throwable instanceof NoSuchElementException) {
                routingContext
                    .response()
                    .setStatusCode(404)
                    .end(itemNotFoundResponse("Item not found"));
              } else {
                routingContext.response().setStatusCode(400).end(throwable.getMessage());
              }
            });
  }

  /**
   * Creates a new catalogue instance and handles the request/response flow.
   *
   * @param routingContext the routing context for handling HTTP requests and responses
   * @throws RuntimeException if item creation fails
   */
  public void createInstanceHandler(RoutingContext routingContext) {

    LOGGER.debug("Info: Creating new instance");

    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    String instance = routingContext.queryParams().get(ID);

    /* INSTANCE = "" to make sure createItem can be used for onboarding instance and items */
    JsonObject body =
        new JsonObject()
            .put(ID, instance)
            .put(TYPE, new JsonArray().add(ITEM_TYPE_INSTANCE))
            .put(INSTANCE, "");
    crudService
        .createItem(body)
        .onComplete(
            res -> {
              if (res.succeeded()) {
                LOGGER.info("Success: Instance created;");
                response.setStatusCode(201).end(res.result().toString());
                // TODO: call auditing service here
              } else {
                LOGGER.error("Fail: Creating instance");
                response.setStatusCode(400).end(res.cause().getMessage());
              }
            });
    LOGGER.debug("Success: Authenticated instance creation request");
  }

  /**
   * Deletes the specified instance from the database.
   *
   * @param routingContext the routing context
   * @throws NullPointerException if routingContext is null
   * @throws RuntimeException if the instance cannot be deleted @ TODO: call auditing service after
   *     successful deletion
   */
  public void deleteInstanceHandler(RoutingContext routingContext) {
    LOGGER.debug("Info: Deleting instance");

    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    String instance = routingContext.queryParams().get(ID);

    /* INSTANCE = "" to make sure createItem can be used for onboarding instance and items */
    JsonObject body = new JsonObject().put(ID, instance).put(INSTANCE, "");
    crudService
        .deleteItem(body)
        .onComplete(
            res -> {
              if (res.succeeded()) {
                LOGGER.info("Success: Instance deleted;");
                response.setStatusCode(200).end(res.result().toString());
                // TODO: call auditing service here
              } else {
                LOGGER.error("Fail: Deleting instance");
                response.setStatusCode(404).end(res.cause().getMessage());
              }
            });
    LOGGER.debug("Success: Authenticated instance creation request");
  }
}
