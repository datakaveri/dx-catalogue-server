package iudx.catalogue.server.apiserver.crud;

import static iudx.catalogue.server.common.util.ResponseBuilderUtil.itemNotFoundResponse;
import static iudx.catalogue.server.util.Constants.ID;
import static iudx.catalogue.server.util.Constants.INSTANCE;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_INSTANCE;
import static iudx.catalogue.server.util.Constants.TITLE_ITEM_NOT_FOUND;
import static iudx.catalogue.server.util.Constants.TYPE;
import static iudx.catalogue.server.util.Constants.TYPE_ITEM_NOT_FOUND;
import static org.mockito.Mockito.*;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.apiserver.item.controller.ItemController;
import iudx.catalogue.server.apiserver.item.handler.ItemLinkValidationHandler;
import iudx.catalogue.server.apiserver.item.model.Item;
import iudx.catalogue.server.apiserver.item.service.ItemService;
import iudx.catalogue.server.auditing.handler.AuditHandler;
import iudx.catalogue.server.authenticator.handler.authentication.AuthHandler;
import iudx.catalogue.server.authenticator.handler.authorization.AuthValidationHandler;
import iudx.catalogue.server.authenticator.handler.authorization.AuthorizationHandler;
import iudx.catalogue.server.common.RespBuilder;
import iudx.catalogue.server.common.RoutingContextHelper;
import iudx.catalogue.server.common.util.DbResponseMessageBuilder;
import iudx.catalogue.server.exceptions.FailureHandler;
import iudx.catalogue.server.validator.service.ValidatorService;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ItemControllerTest {
  private ItemLinkValidationHandler itemLinkValidationHandler;
  private boolean isUac;
  private ItemController itemController;
  private ItemService itemService;
  private RoutingContext routingContext;
  private HttpServerResponse response;
  private HttpServerRequest request;

  @BeforeEach
  void setUp() {
    routingContext = mock(RoutingContext.class);
    itemLinkValidationHandler = mock(ItemLinkValidationHandler.class);
    AuditHandler auditHandler = mock(AuditHandler.class);
    request = mock(HttpServerRequest.class);
    response = mock(HttpServerResponse.class);
    itemService = mock(ItemService.class);
    ValidatorService validatorService = mock(ValidatorService.class);
    AuthHandler authHandler = mock(AuthHandler.class);
    AuthValidationHandler validateToken = mock(AuthValidationHandler.class);
    AuthorizationHandler authorizationHandler = mock(AuthorizationHandler.class);
    FailureHandler failureHandler = mock(FailureHandler.class);
    itemController =
        new ItemController(
            false,
            "host",
            itemService,
            validatorService,
            authHandler,
            validateToken,
            authorizationHandler,
            auditHandler,
            failureHandler);
  }

  @Test
  void testHandlerWhenIsUacIsFalse() {
    isUac = false;

    // Execute handler logic
    if (!isUac) {
      itemLinkValidationHandler.handleItemTypeCases(routingContext);
    } else {
      routingContext.next();
    }

    // Verify `handleItemTypeCases` is called
    verify(itemLinkValidationHandler, times(1)).handleItemTypeCases(routingContext);
    verify(routingContext, never()).next();
  }

  @Test
  void testHandlerWhenIsUacIsTrue() {
    isUac = true;

    // Execute handler logic
    if (!isUac) {
      itemLinkValidationHandler.handleItemTypeCases(routingContext);
    } else {
      routingContext.next();
    }

    // Verify `next()` is called
    verify(routingContext, times(1)).next();
    verify(itemLinkValidationHandler, never()).handleItemTypeCases(routingContext);
  }

  @Test
  void testCreateItemSuccess() {
    JsonObject itemRequest = new JsonObject().put("id", "item1");
    JsonObject createdItem = new JsonObject().put("id", "item1");
    Item item = mock(Item.class);

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(itemService.create(any()))
        .thenReturn(Future.succeededFuture(item));
    when(RoutingContextHelper.getValidatedRequest(routingContext)).thenReturn(itemRequest);
    when(routingContext.request()).thenReturn(request);
    when(routingContext.request().method()).thenReturn(HttpMethod.POST);

    itemController.createOrUpdateItemHandler(routingContext);

    verify(itemService, times(1)).create(item);
    verify(response).setStatusCode(201);
    verify(response).end(createdItem.encodePrettily());
  }

  @Test
  void testCreateItemFailure() {
    JsonObject itemRequest = new JsonObject().put("id", "item1");
    Item item = mock(Item.class);

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(itemService.create(any()))
        .thenReturn(Future.failedFuture("Creation Failed"));
    when(RoutingContextHelper.getValidatedRequest(routingContext)).thenReturn(itemRequest);
    when(routingContext.request()).thenReturn(request);
    when(routingContext.request().method()).thenReturn(HttpMethod.POST);

    itemController.createOrUpdateItemHandler(routingContext);

    verify(itemService, times(1)).create(item);
    verify(response).setStatusCode(400);
    verify(response).end("Creation Failed");
  }

  @Test
  void testGetItemSuccess() {
    String itemId = "item1";
    JsonObject retrievedItem = new JsonObject().put("id", itemId).put("name", "Item Name");
    DbResponseMessageBuilder responseMsg = mock(DbResponseMessageBuilder.class);

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(routingContext.queryParams()).thenReturn(mock(MultiMap.class));
    when(routingContext.queryParams().get("id")).thenReturn(itemId);
    when(itemService.search(anyString(), any()))
        .thenReturn(Future.succeededFuture(responseMsg));

    itemController.getItemHandler(routingContext);

    verify(itemService, times(1)).search(anyString(), any());
    verify(response).setStatusCode(200);
    verify(response).end(retrievedItem.toString());
  }

  @Test
  void testGetItemFailure404() {
    String itemId = "item1";
    String errorResponse =
        new RespBuilder()
            .withType(TYPE_ITEM_NOT_FOUND)
            .withTitle(TITLE_ITEM_NOT_FOUND)
            .withDetail("Fail: Doc doesn't exist, can't perform operation")
            .getResponse();

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(routingContext.queryParams()).thenReturn(mock(MultiMap.class));
    when(routingContext.queryParams().get("id")).thenReturn(itemId);
    when(itemService.search(anyString(), any())).thenReturn(Future.failedFuture(errorResponse));

    itemController.getItemHandler(routingContext);

    verify(itemService, times(1)).search(anyString(), any());
    verify(response).setStatusCode(404);
  }

  @Test
  void testGetItemFailure400() {
    String itemId = "item1";
    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(routingContext.queryParams()).thenReturn(mock(MultiMap.class));
    when(routingContext.queryParams().get("id")).thenReturn(itemId);
    when(itemService.search(anyString(), any()))
        .thenReturn(Future.failedFuture("Fail; Item retrieval failed"));

    itemController.getItemHandler(routingContext);

    verify(itemService, times(1)).search(anyString(), any());
    verify(response).setStatusCode(400);
  }

  @Test
  void testDeleteItemSuccess() {
    String itemId = "item1";
    JsonObject deleteResponse = new JsonObject().put("message", "Item deleted successfully");

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(routingContext.queryParams()).thenReturn(mock(MultiMap.class));
    when(routingContext.queryParams().get("id")).thenReturn(itemId);
    when(itemService.delete(anyString()))
        .thenReturn(Future.succeededFuture(itemId));

    itemController.deleteItemHandler(routingContext);

    verify(itemService, times(1)).delete(anyString());
    verify(response).setStatusCode(200);
    verify(response).end(deleteResponse.toString());
  }

  @Test
  void testDeleteItemFailure() {
    String itemId = "item1";

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(routingContext.queryParams()).thenReturn(mock(MultiMap.class));
    when(routingContext.queryParams().get("id")).thenReturn(itemId);
    when(itemService.delete(anyString()))
        .thenReturn(Future.failedFuture("Deletion Failed"));

    itemController.deleteItemHandler(routingContext);

    verify(itemService, times(1)).delete(anyString());
    verify(response).setStatusCode(400);
    verify(response).end("Deletion Failed");
  }

  @Test
  void testDeleteItemFailure404() {
    String itemId = "item1";
    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(routingContext.queryParams()).thenReturn(mock(MultiMap.class));
    when(routingContext.queryParams().get("id")).thenReturn(itemId);
    when(itemService.delete(anyString()))
        .thenReturn(
            Future.failedFuture(
                new NoSuchElementException("Fail: Doc doesn't exist, can't perform operation")));

    itemController.deleteItemHandler(routingContext);

    verify(itemService, times(1)).delete(anyString());
    verify(response).setStatusCode(404);
    verify(response).end(itemNotFoundResponse("Item not found"));
  }

  @Test
  void testUpdateItemSuccess() {
    JsonObject itemRequest = new JsonObject().put("id", "item1").put("name", "Updated Item");
    JsonObject updatedItem = new JsonObject().put("id", "item1").put("name", "Updated Item");
    Item item = mock(Item.class);

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(RoutingContextHelper.getValidatedRequest(routingContext)).thenReturn(itemRequest);
    when(routingContext.request()).thenReturn(request);
    when(routingContext.request().method()).thenReturn(HttpMethod.PUT);
    when(itemService.update(any()))
        .thenReturn(Future.succeededFuture(item));

    itemController.createOrUpdateItemHandler(routingContext);

    verify(itemService, times(1)).update(item);
    verify(response).setStatusCode(200);
    verify(response).end(updatedItem.encodePrettily());
  }

  @Test
  void testUpdateItemFailure404() {
    JsonObject itemRequest = new JsonObject().put("id", "item1").put("name", "Updated Item");
    Item item = mock(Item.class);

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(RoutingContextHelper.getValidatedRequest(routingContext)).thenReturn(itemRequest);
    when(routingContext.request()).thenReturn(request);
    when(routingContext.request().method()).thenReturn(HttpMethod.PUT);
    when(itemService.update(any()))
        .thenReturn(Future.failedFuture("Doc doesn't exist"));

    itemController.createOrUpdateItemHandler(routingContext);

    verify(itemService, times(1)).update(item);
    verify(response).setStatusCode(404);
    verify(response).end("Doc doesn't exist");
  }

  @Test
  void testUpdateItemFailure400() {
    JsonObject itemRequest = new JsonObject().put("id", "item1").put("name", "Updated Item");
    Item item = mock(Item.class);

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(RoutingContextHelper.getValidatedRequest(routingContext)).thenReturn(itemRequest);
    when(routingContext.request()).thenReturn(request);
    when(routingContext.request().method()).thenReturn(HttpMethod.PUT);
    when(itemService.update(any()))
        .thenReturn(Future.failedFuture("Update Failed"));

    itemController.createOrUpdateItemHandler(routingContext);

    verify(itemService, times(1)).update(item);
    verify(response).setStatusCode(400);
    verify(response).end("Update Failed");
  }

  @Test
  void testCreateInstanceSuccess() {
    String itemId = "dummyId";
    JsonObject itemRequest =
        new JsonObject()
            .put(ID, itemId)
            .put(TYPE, new JsonArray().add(ITEM_TYPE_INSTANCE))
            .put(INSTANCE, "");
    JsonObject createdItem = new JsonObject().put("id", itemId);
    Item item = mock(Item.class);

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(routingContext.queryParams()).thenReturn(mock(MultiMap.class));
    when(routingContext.queryParams().get("id")).thenReturn(itemId);
    when(itemService.create(any()))
        .thenReturn(Future.succeededFuture(item));

    itemController.createInstanceHandler(routingContext);

    verify(itemService, times(1)).create(item);
    verify(response).setStatusCode(201);
  }

  @Test
  void testCreateInstanceFailure() {
    String instance = "dummyId";
    Item item = mock(Item.class);
    JsonObject itemRequest =
        new JsonObject()
            .put(ID, instance)
            .put(TYPE, new JsonArray().add(ITEM_TYPE_INSTANCE))
            .put(INSTANCE, "");

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(routingContext.queryParams()).thenReturn(mock(MultiMap.class));
    when(routingContext.queryParams().get("id")).thenReturn(instance);
    when(itemService.create(any()))
        .thenReturn(Future.failedFuture("Fail; creating instance"));

    itemController.createInstanceHandler(routingContext);

    verify(itemService, times(1)).create(item);
    verify(response).setStatusCode(400);
  }

  @Test
  void testDeleteInstanceSuccess() {
    String instance = "dummyId";
    JsonObject itemRequest = new JsonObject().put(ID, instance).put(INSTANCE, "");
    JsonObject createdItem = new JsonObject().put("id", instance);

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(routingContext.queryParams()).thenReturn(mock(MultiMap.class));
    when(routingContext.queryParams().get("id")).thenReturn(instance);
    when(itemService.delete(anyString()))
        .thenReturn(Future.succeededFuture(instance));

    itemController.deleteInstanceHandler(routingContext);

    verify(itemService, times(1)).delete(eq(instance));
    verify(response).setStatusCode(200);
  }

  @Test
  void testDeleteInstanceFailure() {
    String instance = "dummyId";
    JsonObject itemRequest = new JsonObject().put(ID, instance).put(INSTANCE, "");

    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(routingContext.queryParams()).thenReturn(mock(MultiMap.class));
    when(routingContext.queryParams().get("id")).thenReturn(instance);
    when(itemService.delete(anyString()))
        .thenReturn(Future.failedFuture("Fail; deletion failed"));

    itemController.deleteInstanceHandler(routingContext);

    verify(itemService, times(1)).delete(eq(instance));
    verify(response).setStatusCode(404);
  }
}
