//package iudx.catalogue.server.apiserver.stack;
//
//import static iudx.catalogue.server.common.RoutingContextHelper.JWT_DATA;
//import static iudx.catalogue.server.common.RoutingContextHelper.VALIDATED_REQ_KEY;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//import io.vertx.core.Future;
//import io.vertx.core.http.HttpServerResponse;
//import io.vertx.core.json.JsonArray;
//import io.vertx.core.json.JsonObject;
//import io.vertx.ext.web.Router;
//import io.vertx.ext.web.RoutingContext;
//import iudx.catalogue.server.apiserver.item.model.Item;
//import iudx.catalogue.server.apiserver.item.service.ItemService;
//import iudx.catalogue.server.apiserver.stack.controller.StacController;
//import iudx.catalogue.server.apiserver.stack.service.StacService;
//import iudx.catalogue.server.auditing.handler.AuditHandler;
//import iudx.catalogue.server.authenticator.handler.authentication.AuthHandler;
//import iudx.catalogue.server.authenticator.handler.authorization.AuthValidationHandler;
//import iudx.catalogue.server.authenticator.model.JwtData;
//import iudx.catalogue.server.database.elastic.model.ElasticsearchResponse;
//import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
//import iudx.catalogue.server.exceptions.FailureHandler;
//import iudx.catalogue.server.util.Api;
//import iudx.catalogue.server.validator.service.ValidatorService;
//import java.util.List;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//public class StacControllerTest {
//  private ElasticsearchService esService;
//  private ItemService itemService;
//  private StacService stacService;
//  private RoutingContext routingContext;
//  private HttpServerResponse response;
//  private StacController stacController;
//
//  @BeforeEach
//  void setUp() {
//    Api api = mock(Api.class);
//    JsonObject config = new JsonObject().put("docIndex", "dummy-index");
//    stacService = mock(StacService.class);
//    ValidatorService validatorService = mock(ValidatorService.class);
//    esService = mock(ElasticsearchService.class);
//    itemService = mock(ItemService.class);
//    AuditHandler auditHandler = mock(AuditHandler.class);
//    AuthHandler authHandler = mock(AuthHandler.class);
//    AuthValidationHandler validateToken = mock(AuthValidationHandler.class);
//    FailureHandler failureHandler = mock(FailureHandler.class);
//    Router router = mock(Router.class);
//    routingContext = mock(RoutingContext.class);
//    response = mock(HttpServerResponse.class);
//    stacController = new StacController(api, auditHandler, itemService, authHandler,
//        validateToken, failureHandler);
//    when(routingContext.response()).thenReturn(response);
//    when(response.putHeader(anyString(), anyString())).thenReturn(response);
//    when(response.setStatusCode(anyInt())).thenReturn(response);
//  }
//
//  @Test
//  void testHandleGetStackRequestSuccess() {
//    String stackId = "52ffe1ca-2926-4437-9d3f-868f0ba81eba";
//    JsonObject resultJson = new JsonObject().put("id", stackId);
//    when(routingContext.queryParams()).thenReturn(mock(io.vertx.core.MultiMap.class));
//    when(routingContext.queryParams().get("id")).thenReturn(stackId);
//    when(esService.search(any(), any()))
//        .thenReturn(
//            Future.succeededFuture(List.of(new ElasticsearchResponse("docId", resultJson))));
//    when(stacService.get(stackId)).thenReturn(Future.succeededFuture(resultJson));
//
//    stacController.handleGetStackRequest(routingContext);
//
//    verify(response).setStatusCode(200);
//  }
//
//  @Test
//  void testHandleGetStackRequestFailure() {
//    String stackId = "invalid-id";
//    when(routingContext.queryParams()).thenReturn(mock(io.vertx.core.MultiMap.class));
//    when(routingContext.queryParams().get("id")).thenReturn(stackId);
//    when(stacService.get(stackId)).thenReturn(Future.failedFuture("Stack not found"));
//
//    stacController.handleGetStackRequest(routingContext);
//
//    verify(response).setStatusCode(400);
//    verify(response).end(anyString());
//  }
//
//  @Test
//  void testHandlePostStackRequestSuccess() {
//    JsonArray links = new JsonArray()
//        .add(new JsonObject().put("rel", "root").put("href", "someHref"))
//        .add(new JsonObject().put("rel", "self").put("href", "someHref"));
//    JsonObject requestBody = new JsonObject()
//        .put("links", links);
//    Item stacLink = mock(Item.class);
//    JsonObject resultJson = new JsonObject().put("results", requestBody);
//    when(routingContext.get(VALIDATED_REQ_KEY)).thenReturn(requestBody);
//    when(routingContext.get(JWT_DATA)).thenReturn(mock(JwtData.class));
//    when(esService.search(any(), any())).thenReturn(Future.succeededFuture(List.of()));
//    when(esService.createDocument(any(), any())).thenReturn(Future.succeededFuture());
//    when(stacService.create(any())).thenReturn(Future.succeededFuture(stacLink));
//
//    stacController.handlePostStackRequest(routingContext);
//
//    verify(response).setStatusCode(201);
//  }
//
//  @Test
//  void testHandlePostStackRequestFailure() {
//    JsonArray links = new JsonArray()
//        .add(new JsonObject().put("rel", "root").put("href", "someHref"))
//        .add(new JsonObject().put("rel", "self").put("href", "someHref"));
//    JsonObject requestBody = new JsonObject()
//        .put("links", links);
//    when(routingContext.get(VALIDATED_REQ_KEY)).thenReturn(requestBody);
//    when(routingContext.get(JWT_DATA)).thenReturn(mock(JwtData.class));
//    when(esService.search(any(), any())).thenReturn(Future.succeededFuture(List.of()));
//    when(esService.createDocument(any(), any())).thenReturn(Future.failedFuture("Bad Request"));
//    when(stacService.create(any())).thenReturn(Future.failedFuture("Database error"));
//
//    stacController.handlePostStackRequest(routingContext);
//
//    verify(response).setStatusCode(500);
//  }
//}
