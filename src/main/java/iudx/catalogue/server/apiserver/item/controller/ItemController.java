package iudx.catalogue.server.apiserver.item.controller;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.common.util.ResponseBuilderUtil.*;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.apiserver.item.exception.ItemNotFoundException;
import iudx.catalogue.server.apiserver.item.exception.ItemAlreadyExistsException;
import iudx.catalogue.server.apiserver.item.exception.ItemDatabaseFailureException;
import iudx.catalogue.server.apiserver.item.handler.ItemLinkValidationHandler;
import iudx.catalogue.server.apiserver.item.handler.ItemSchemaHandler;
import iudx.catalogue.server.apiserver.item.model.COS;
import iudx.catalogue.server.apiserver.item.model.Instance;
import iudx.catalogue.server.apiserver.item.model.Item;
import iudx.catalogue.server.apiserver.item.model.Owner;
import iudx.catalogue.server.apiserver.item.model.Provider;
import iudx.catalogue.server.apiserver.item.model.Resource;
import iudx.catalogue.server.apiserver.item.model.ResourceGroup;
import iudx.catalogue.server.apiserver.item.model.ResourceServer;
import iudx.catalogue.server.apiserver.item.service.ItemService;
import iudx.catalogue.server.auditing.handler.AuditHandler;
import iudx.catalogue.server.authenticator.handler.authentication.AuthHandler;
import iudx.catalogue.server.authenticator.handler.authorization.AuthValidationHandler;
import iudx.catalogue.server.authenticator.handler.authorization.AuthorizationHandler;
import iudx.catalogue.server.authenticator.model.DxRole;
import iudx.catalogue.server.authenticator.model.JwtAuthenticationInfo;
import iudx.catalogue.server.common.RoutingContextHelper;
import iudx.catalogue.server.exceptions.DatabaseFailureException;
import iudx.catalogue.server.exceptions.DocAlreadyExistsException;
import iudx.catalogue.server.exceptions.DocNotFoundException;
import iudx.catalogue.server.exceptions.FailureHandler;
import iudx.catalogue.server.exceptions.InternalServerException;
import iudx.catalogue.server.exceptions.InvalidSyntaxException;
import iudx.catalogue.server.exceptions.OperationNotAllowedException;
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
public class ItemController {
  private final Logger LOGGER = LogManager.getLogger(ItemController.class);
  private final ItemService itemService;
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
   * ItemController constructor.
   *
   * @param isUac flag indicating if UAC is enabled
   * @param itemService service for CRUD operations on Dx items
   */
  public ItemController(boolean isUac, String host, ItemService itemService,
                        ValidatorService validatorService, AuthHandler authHandler,
                        AuthValidationHandler validateToken,
                        AuthorizationHandler authorizationHandler, AuditHandler auditHandler,
                        FailureHandler failureHandler) {
    this.isUac = isUac;
    this.host = host;
    this.itemService = itemService;
    this.authHandler = authHandler;
    this.validateToken = validateToken;
    this.authorizationHandler = authorizationHandler;
    this.itemSchemaHandler = new ItemSchemaHandler();
    this.itemLinkValidationHandler = new ItemLinkValidationHandler(itemService, validatorService);
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

  public void createOrUpdateItemHandler(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonObject validatedRequest = RoutingContextHelper.getValidatedRequest(routingContext);
    String itemType = RoutingContextHelper.getItemType(routingContext);
    LOGGER.debug("Item type: " + itemType);
    Item item = createItemFromType(itemType, validatedRequest);
    // If post then create. Else, update
    if (routingContext.request().method().toString().equals(REQUEST_POST)) {
      itemService.create(item)
          .compose(itemHandler -> {
            JsonObject responseMsg = DxPostOperationRespSuccess(itemHandler.toJson());
            response.setStatusCode(201).end(responseMsg.encodePrettily());
            routingContext.next();
            return Future.succeededFuture(itemHandler);
          })
          .recover(itemHandler -> {
            if (itemHandler instanceof DocAlreadyExistsException existsException) {
              LOGGER.error("Item already exists, skipping creation");
              routingContext.fail(new ItemAlreadyExistsException(existsException.getItemId()));
              return Future.failedFuture(existsException);
            } else if (itemHandler instanceof DatabaseFailureException dbException) {
              LOGGER.error("Fail: DB request has failed; " + dbException.getMessage());
              routingContext.fail(new ItemDatabaseFailureException(dbException.getId(),
                  dbException.getMessage()));
            } else if (itemHandler instanceof InvalidSyntaxException exception) {
              LOGGER.error("Invalid syntax: " + exception.getMessage());
              routingContext.fail(exception);
            }
            LOGGER.error("Failed to create item: " + itemHandler.getMessage());
            response.setStatusCode(400).end(itemHandler.getMessage());
            return Future.failedFuture(itemHandler.getMessage());
          });
    } else {
      itemService.update(item)
          .onComplete(itemHandler -> {
            if (itemHandler.failed()) {
              LOGGER.error("Failed to update item: " + itemHandler.cause().getMessage());
              if (itemHandler.cause() instanceof DocNotFoundException exception) {
                LOGGER.error("Failed to update item with id {}", exception.getId());
                routingContext.fail(new ItemNotFoundException(exception.getId(), UPDATE));
              } else {
                routingContext.response().setStatusCode(400).end(itemHandler.cause().getMessage());
              }
            } else {
              JsonObject responseMsg = DxPutRespSuccess(itemHandler.result().toJson());
              routingContext.response().setStatusCode(200).end(responseMsg.encodePrettily());
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
    itemService.search(itemId, null)
        .onComplete(getHandler -> {
          if (getHandler.succeeded()) {
            JsonObject retrievedItem = getHandler.result().getResponse();
            response.setStatusCode(200).end(retrievedItem.toString());
          } else {
            if (getHandler.cause() instanceof NoSuchElementException) {
              LOGGER.error("Fail: Item not found");
              JsonObject errorResponse = new JsonObject()
                  .put(TYPE, TYPE_ITEM_NOT_FOUND)
                  .put(STATUS, ERROR)
                  .put(TOTAL_HITS, 0)
                  .put(RESULTS, new JsonArray())
                  .put(DETAIL, "doc doesn't exist");
                  response.setStatusCode(404).end(errorResponse.toString());
            } else if (getHandler.cause() instanceof InternalServerException exception) {
              routingContext.fail(exception);
            } else if (getHandler.cause() instanceof DatabaseFailureException exception) {
              routingContext.fail(exception);
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

    itemService.delete(itemId)
        .onSuccess(result -> {
          LOGGER.info("Item deleted successfully");
          JsonObject responseMsg = DxItemDelRespSuccess(result,
              "Success: Item deleted successfully");
          routingContext.response().setStatusCode(200).end(responseMsg.toString());
          routingContext.next();
        })
        .onFailure(throwable -> {
          LOGGER.error("Failed to delete item", throwable);
          if (throwable instanceof DocNotFoundException exception) {
            routingContext.fail(new ItemNotFoundException(exception.getId(),
                exception.getMethod()));
          } else if (throwable instanceof OperationNotAllowedException exception) {
            routingContext.fail(exception);
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
    JsonObject body = new JsonObject()
        .put(ID, instance)
        .put(TYPE, new JsonArray().add(ITEM_TYPE_INSTANCE))
        .put(INSTANCE, "");
    Item item = createItemFromType(ITEM_TYPE_INSTANCE, body);

    itemService.create(item)
        .onSuccess(successHandler -> {
          LOGGER.info("Success: Instance created;");
          JsonObject responseMsg = DxPostOperationRespSuccess(successHandler.toJson());
          response.setStatusCode(201).end(responseMsg.encodePrettily());
        })
        .recover(instanceHandler -> {
          if (instanceHandler instanceof DocAlreadyExistsException existsException) {
            LOGGER.error("Item already exists, skipping creation");
            routingContext.fail(new ItemAlreadyExistsException(existsException.getItemId()));
            return Future.failedFuture(existsException);
          } else if (instanceHandler instanceof DatabaseFailureException dbException) {
            LOGGER.error("Fail: DB request has failed; " + dbException.getMessage());
            routingContext.fail(new ItemDatabaseFailureException(dbException.getId(),
                dbException.getMessage()));
          } else if (instanceHandler instanceof InvalidSyntaxException exception) {
            LOGGER.error("Invalid syntax: " + exception.getMessage());
            routingContext.fail(exception);
          }
          LOGGER.error("Failed to create item: " + instanceHandler.getMessage());
          response.setStatusCode(400).end(instanceHandler.getMessage());
          return Future.failedFuture(instanceHandler.getMessage());
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
    //JsonObject body = new JsonObject().put(ID, instance).put(INSTANCE, "");
    itemService.delete(instance)
        .onComplete(handler -> {
          if (handler.succeeded()) {
            LOGGER.info("Success: Instance deleted;");
            JsonObject responseMsg = DxItemDelRespSuccess(handler.result(),
                "Success: Item deleted successfully");
            response.setStatusCode(200).end(responseMsg.encodePrettily());
            // TODO: call auditing service here
          } else {
            if (handler.cause() instanceof DocNotFoundException exception) {
              routingContext.fail(new ItemNotFoundException(exception.getId(),
                  exception.getMethod()));
            } else if (handler.cause() instanceof OperationNotAllowedException exception) {
              routingContext.fail(exception);
            } else {
              LOGGER.error("Fail: Deleting instance");
              response.setStatusCode(404).end(handler.cause().getMessage());
            }
          }
        });
    LOGGER.debug("Success: Authenticated instance creation request");
  }

  private Item createItemFromType(String itemType, JsonObject requestBody) {
    return switch (itemType) {
      case ITEM_TYPE_OWNER -> new Owner(requestBody);
      case ITEM_TYPE_COS -> new COS(requestBody);
      case ITEM_TYPE_RESOURCE_SERVER -> new ResourceServer(requestBody);
      case ITEM_TYPE_PROVIDER -> new Provider(requestBody);
      case ITEM_TYPE_RESOURCE_GROUP -> new ResourceGroup(requestBody);
      case ITEM_TYPE_RESOURCE -> new Resource(requestBody);
      case ITEM_TYPE_INSTANCE -> new Instance(requestBody);
      default -> throw new IllegalArgumentException("Invalid item type: " + itemType);
    };
  }
}
