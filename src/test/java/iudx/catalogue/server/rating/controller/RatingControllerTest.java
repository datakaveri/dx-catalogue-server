package iudx.catalogue.server.rating.controller;

import static iudx.catalogue.server.apiserver.util.Constants.ROUTE_RATING;
import static iudx.catalogue.server.authenticator.Constants.RATINGS_ENDPOINT;
import static iudx.catalogue.server.common.RoutingContextHelper.JWT_AUTH_INFO_KEY;
import static iudx.catalogue.server.util.Constants.ID;
import static iudx.catalogue.server.util.Constants.STATUS;
import static iudx.catalogue.server.util.Constants.TITLE_SUCCESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import iudx.catalogue.server.auditing.handler.AuditHandler;
import iudx.catalogue.server.authenticator.handler.authentication.AuthHandler;
import iudx.catalogue.server.authenticator.handler.authorization.AuthValidationHandler;
import iudx.catalogue.server.authenticator.handler.authorization.AuthorizationHandler;
import iudx.catalogue.server.authenticator.model.JwtAuthenticationInfo;
import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.common.RoutingContextHelper;
import iudx.catalogue.server.exceptions.FailureHandler;
import iudx.catalogue.server.rating.service.RatingService;
import iudx.catalogue.server.validator.service.ValidatorService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class RatingControllerTest {
  private ValidatorService validatorService;
  private RatingService ratingService;
  private RoutingContext routingContext;
  private HttpServerRequest httpServerRequest;
  private HttpServerResponse httpServerResponse;

  private RatingController ratingController;

  @BeforeEach
  public void setUp() {
    Router router = mock(Router.class);
    Route routeMock = mock(Route.class);

    when(router.post(anyString())).thenReturn(routeMock);
    when(router.get(anyString())).thenReturn(routeMock);
    when(router.put(anyString())).thenReturn(routeMock);
    when(router.delete(anyString())).thenReturn(routeMock);

    when(routeMock.consumes(anyString())).thenReturn(routeMock);
    when(routeMock.produces(anyString())).thenReturn(routeMock);
    when(routeMock.handler(any())).thenReturn(routeMock);

    validatorService = mock(ValidatorService.class);
    ratingService = mock(RatingService.class);
    AuthHandler authHandler = mock(AuthHandler.class);
    AuthValidationHandler validateToken = mock(AuthValidationHandler.class);
    AuthorizationHandler authorizationHandler = mock(AuthorizationHandler.class);
    AuditHandler auditHandler = mock(AuditHandler.class);
    FailureHandler failureHandler = mock(FailureHandler.class);
    routingContext = mock(RoutingContext.class);
    httpServerRequest = mock(HttpServerRequest.class);
    httpServerResponse = mock(HttpServerResponse.class);

    ratingController =
        new RatingController(
            validatorService,
            ratingService,
            "dummy-host",
            authHandler,
            validateToken,
            authorizationHandler,
            auditHandler,
            failureHandler);
    ratingController.init(router);
    verify(router, times(1)).post(ROUTE_RATING);
    verify(router, times(1)).get(ROUTE_RATING);
    verify(router, times(1)).put(ROUTE_RATING);
    verify(router, times(1)).delete(ROUTE_RATING);
  }

  @Test
  void testValidateAuthWithInvalidToken() {
    when(routingContext.request()).thenReturn(httpServerRequest);
    when(routingContext.request().headers()).thenReturn(mock(MultiMap.class));
    when(routingContext.request().headers().contains("token")).thenReturn(false);

    when(routingContext.response()).thenReturn(httpServerResponse);
    when(httpServerResponse.setStatusCode(anyInt())).thenReturn(httpServerResponse);
    when(httpServerResponse.end()).thenReturn(null);

    ratingController.checkIfHeaderTokenExists(routingContext);

    verify(httpServerResponse, times(1)).setStatusCode(eq(401));
    verify(httpServerResponse, times(1)).end();
    verify(routingContext, never()).next();
  }

  @Test
  void testValidateAuthWithValidToken() {
    when(routingContext.request()).thenReturn(httpServerRequest);
    when(routingContext.request().headers()).thenReturn(mock(MultiMap.class));
    when(routingContext.request().headers().contains("token")).thenReturn(true);

    ratingController.checkIfHeaderTokenExists(routingContext);

    verify(routingContext, times(1)).next();
  }

  @Test
  void testValidateIDWithInvalidId() {
    when(routingContext.request()).thenReturn(httpServerRequest);
    when(routingContext.response()).thenReturn(httpServerResponse);
    when(httpServerRequest.getParam(ID)).thenReturn("dummy-id");
    when(httpServerResponse.putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(httpServerResponse.setStatusCode(anyInt())).thenReturn(httpServerResponse);

    // Call the validateID method
    ratingController.validateID(routingContext);

    // Verify the response status code and response body
    verify(httpServerResponse).setStatusCode(400);
    verify(httpServerResponse).end(anyString());
    verify(routingContext, never()).next();  // Ensure next is not called on invalid ID
  }

  @Test
  void testValidateIDWithValidId() {
    String dummyId = "0f5f1033-5fc9-4fcc-87df-636baeb8f821";
    when(routingContext.request()).thenReturn(httpServerRequest);
    when(routingContext.response()).thenReturn(httpServerResponse);
    when(httpServerRequest.getParam(ID)).thenReturn(dummyId);
    when(httpServerResponse.putHeader(anyString(), anyString())).thenReturn(httpServerResponse);

    // Call the validateID method
    ratingController.validateID(routingContext);

    // Verify next() is called when ID is valid
    verify(routingContext, times(1)).next();
    verify(httpServerResponse, never()).setStatusCode(anyInt());
    verify(httpServerResponse, never()).end(anyString());
  }

  @Test
  void testCreateRatingHandlerWithInvalidRequest() {
    when(RoutingContextHelper.getValidatedRequest(routingContext)).thenReturn(null);

    when(routingContext.response()).thenReturn(httpServerResponse);
    when(httpServerResponse.setStatusCode(anyInt())).thenReturn(httpServerResponse);
    when(ratingService.createRating(any())).thenReturn(Future.failedFuture("Fail"));

    ratingController.createRatingHandler(routingContext);

    verify(httpServerResponse, times(1)).setStatusCode(eq(400));
    verify(httpServerResponse, times(1)).end(anyString());
  }

  @Test
  void testCreateRatingHandlerSuccess() {
    JsonObject requestBody = new JsonObject().put("key", "value");
    when(RoutingContextHelper.getValidatedRequest(routingContext)).thenReturn(requestBody);
    when(ratingService.createRating(any())).thenReturn(Future.succeededFuture(new JsonObject()));
    when(routingContext.response()).thenReturn(httpServerResponse);
    when(httpServerResponse.setStatusCode(anyInt())).thenReturn(httpServerResponse);

    ratingController.createRatingHandler(routingContext);

    verify(httpServerResponse, times(1)).setStatusCode(eq(201));
    verify(httpServerResponse, times(1)).end(anyString());
  }

  @Test
  void testCreateRatingHandlerFailure() {
    JsonObject requestBody = new JsonObject().put("key", "value");
    when(RoutingContextHelper.getValidatedRequest(routingContext)).thenReturn(requestBody);
    when(ratingService.createRating(any())).thenReturn(Future.failedFuture("Error"));
    when(routingContext.response()).thenReturn(httpServerResponse);
    when(httpServerResponse.setStatusCode(anyInt())).thenReturn(httpServerResponse);

    ratingController.createRatingHandler(routingContext);
    verify(httpServerResponse, times(1)).setStatusCode(eq(400));
    verify(httpServerResponse, times(1)).end(anyString());
  }

  @Test
  void testGetRatingHandlerNoTypeParam() {
    when(routingContext.request()).thenReturn(httpServerRequest);
    when(routingContext.request().params()).thenReturn(mock(MultiMap.class));
    when(routingContext.request().params().contains("type")).thenReturn(false);
    JwtData mockJwtData = mock(JwtData.class);
    when(RoutingContextHelper.getJwtData(routingContext)).thenReturn(mockJwtData);

    when(routingContext.response()).thenReturn(httpServerResponse);
    when(httpServerResponse.setStatusCode(anyInt())).thenReturn(httpServerResponse);
    when(ratingService.getRating(any())).thenReturn(Future.failedFuture("Fail"));

    ratingController.getRatingHandler(routingContext);

    verify(httpServerResponse, times(1)).setStatusCode(eq(400));
    verify(httpServerResponse, times(1)).end(anyString());
  }

  @Test
  void testGetRatingHandlerWithValidType() {
    when(routingContext.request()).thenReturn(httpServerRequest);
    when(routingContext.request().params()).thenReturn(mock(MultiMap.class));
    when(routingContext.request().params().contains("type")).thenReturn(true);

    when(ratingService.getRating(any())).thenReturn(
        Future.succeededFuture(new JsonObject().put("results", new JsonArray().add("value"))));
    when(routingContext.response()).thenReturn(httpServerResponse);
    when(httpServerResponse.setStatusCode(anyInt())).thenReturn(httpServerResponse);

    ratingController.getRatingHandler(routingContext);

    verify(httpServerResponse, times(1)).setStatusCode(eq(200));
    verify(httpServerResponse, times(1)).end(anyString());
  }
  @Test
  void testUpdateRatingHandlerSuccess() {
    JsonObject requestBody = new JsonObject().put("rating", 5);
    when(RoutingContextHelper.getValidatedRequest(routingContext)).thenReturn(requestBody);
    when(ratingService.updateRating(any())).thenReturn(Future.succeededFuture(new JsonObject()));
    when(routingContext.response()).thenReturn(httpServerResponse);
    when(httpServerResponse.setStatusCode(anyInt())).thenReturn(httpServerResponse);

    ratingController.updateRatingHandler(routingContext);

    verify(httpServerResponse, times(1)).setStatusCode(eq(200));
    verify(httpServerResponse, times(1)).end(anyString());
  }

  @Test
  void testUpdateRatingHandlerFailure() {
    JsonObject requestBody = new JsonObject().put("rating", 5);
    when(RoutingContextHelper.getValidatedRequest(routingContext)).thenReturn(requestBody);
    when(ratingService.updateRating(any())).thenReturn(Future.failedFuture("Error"));
    when(routingContext.response()).thenReturn(httpServerResponse);
    when(httpServerResponse.setStatusCode(anyInt())).thenReturn(httpServerResponse);

    ratingController.updateRatingHandler(routingContext);
    verify(httpServerResponse, times(1)).setStatusCode(eq(400));
    verify(httpServerResponse, times(1)).end(anyString());
  }

  @Test
  void testDeleteRatingHandlerSuccess() {
    when(routingContext.request()).thenReturn(httpServerRequest);
    when(routingContext.request().getParam(anyString())).thenReturn("dummy-Id");
    when(ratingService.deleteRating(any())).thenReturn(Future.succeededFuture(new JsonObject().put(STATUS, TITLE_SUCCESS)));
    when(routingContext.response()).thenReturn(httpServerResponse);
    when(httpServerResponse.setStatusCode(anyInt())).thenReturn(httpServerResponse);
    when(RoutingContextHelper.getJwtData(routingContext)).thenReturn(mock(JwtData.class));
    when(RoutingContextHelper.getJwtData(routingContext).getSub()).thenReturn("sub");

    ratingController.deleteRatingHandler(routingContext);

    verify(httpServerResponse, times(1)).setStatusCode(eq(200));
    verify(httpServerResponse, times(1)).end(anyString());
  }

  @Test
  void testDeleteRatingHandlerFailure() {
    when(routingContext.request()).thenReturn(httpServerRequest);
    when(routingContext.request().getParam(anyString())).thenReturn("dummy-Id");
    when(RoutingContextHelper.getJwtData(routingContext)).thenReturn(mock(JwtData.class));
    when(RoutingContextHelper.getJwtData(routingContext).getSub()).thenReturn("sub");
    when(ratingService.deleteRating(any())).thenReturn(Future.failedFuture("Error"));
    when(routingContext.response()).thenReturn(httpServerResponse);
    when(httpServerResponse.setStatusCode(anyInt())).thenReturn(httpServerResponse);

    ratingController.deleteRatingHandler(routingContext);
    verify(httpServerResponse, times(1)).setStatusCode(eq(400));
    verify(httpServerResponse, times(1)).end(anyString());
  }
  @Test
  void testSetAuthInfoSuccess() {
    HttpServerRequest request = mock(HttpServerRequest.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    when(routingContext.request()).thenReturn(request);
    when(routingContext.response()).thenReturn(response);
    when(routingContext.request().headers()).thenReturn(mock(MultiMap.class));
    when(response.putHeader(anyString(), anyString())).thenReturn(response);

    // Call the method
    ratingController.setAuthInfo(routingContext, "GET");

    // Verify JWT info is set
    ArgumentCaptor<JwtAuthenticationInfo> captor = ArgumentCaptor.forClass(JwtAuthenticationInfo.class);
    verify(routingContext, times(1)).put(eq(JWT_AUTH_INFO_KEY), captor.capture());

    // Assert values
    JwtAuthenticationInfo capturedAuthInfo = captor.getValue();
    Assertions.assertEquals("GET", capturedAuthInfo.getMethod());
    Assertions.assertEquals(RATINGS_ENDPOINT, capturedAuthInfo.getApiEndpoint());

    // Verify routingContext.next() is called
    verify(routingContext, times(1)).next();
  }
  @Test
  void testValidateSchemaSuccess() {
    JsonObject requestBody = new JsonObject();
    JwtData jwtData = mock(JwtData.class);
    when(routingContext.response()).thenReturn(mock(HttpServerResponse.class));
    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonObject()).thenReturn(requestBody);
    when(routingContext.request()).thenReturn(mock(HttpServerRequest.class));
    when(routingContext.request().getParam(ID)).thenReturn("123");
    when(RoutingContextHelper.getJwtData(routingContext)).thenReturn(jwtData);
    when(jwtData.getSub()).thenReturn("user123");
    // Mock validation future
    when(validatorService.validateRating(any()))
        .thenReturn(Future.succeededFuture(new JsonObject()));

    // Call the method
    ratingController.validateSchema(routingContext);

    // Verify validation service is called
    verify(validatorService, times(1)).validateRating(requestBody);

    // Ensure routingContext.next() is called
    verify(routingContext, times(1)).next();
  }

  @Test
  void testValidateSchemaFailure() {
    JwtData jwtData = mock(JwtData.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    when(routingContext.body()).thenReturn(mock(RequestBody.class));
    when(routingContext.body().asJsonObject()).thenReturn(new JsonObject());
    when(routingContext.request()).thenReturn(request);
    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(mock(HttpServerResponse.class));
    when(routingContext.request().getParam(ID)).thenReturn("123");
    when(RoutingContextHelper.getJwtData(routingContext)).thenReturn(jwtData);
    when(jwtData.getSub()).thenReturn("user123");
    // Mock validation failure
    when(validatorService.validateRating(any()))
        .thenReturn(Future.failedFuture("Validation error"));
    // Call the method
    ratingController.validateSchema(routingContext);

    // Verify error response is sent
    verify(response, times(1)).setStatusCode(400);

    // Ensure routingContext.next() is NOT called
    verify(routingContext, never()).next();
  }
}
