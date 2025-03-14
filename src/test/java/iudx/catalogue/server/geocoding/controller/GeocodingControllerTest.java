package iudx.catalogue.server.geocoding.controller;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.server.geocoding.service.GeocodingService;
import iudx.catalogue.server.util.Api;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static iudx.catalogue.server.apiserver.util.Constants.ROUTE_GEO_COORDINATES;
import static iudx.catalogue.server.apiserver.util.Constants.ROUTE_GEO_REVERSE;
import static iudx.catalogue.server.util.Constants.COORDINATES;
import static iudx.catalogue.server.util.Constants.GEOMETRY;
import static iudx.catalogue.server.util.Constants.RESULTS;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class GeocodingControllerTest {

  private GeocodingController geocodingController;
  private GeocodingService geocodingService;
  private RoutingContext routingContext;

  @BeforeEach
  void setUp() {
    Router router = mock(Router.class);
    when(router.get(anyString())).thenReturn(mock(Route.class));

    geocodingService = mock(GeocodingService.class);
    routingContext = mock(RoutingContext.class);
    geocodingController = new GeocodingController(geocodingService);
    geocodingController.init(router);
    verify(router, times(1)).get(ROUTE_GEO_COORDINATES);
    verify(router, times(1)).get(ROUTE_GEO_REVERSE);
  }

  @Test
  void testGetCoordinatesSuccess(VertxTestContext testContext) {
    HttpServerResponse mockResponse = mock(HttpServerResponse.class);
    String location = "dummy-location";

    when(routingContext.response()).thenReturn(mockResponse);
    when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
    when(routingContext.queryParams()).thenReturn(mock(MultiMap.class));
    when(routingContext.queryParams().contains("q")).thenReturn(true);
    when(routingContext.queryParams().get("q")).thenReturn(location);

    JsonObject responseJson = new JsonObject().put(RESULTS, new JsonArray().add(new JsonObject().put(
        "location", location)));

    doAnswer(invocation -> {
      testContext.completeNow();
      return null;
    }).when(mockResponse).end(anyString());
    when(geocodingService.geocoder(location)).thenReturn(Future.succeededFuture(responseJson.toString()));

    geocodingController.getCoordinates(routingContext);

    // Verify the geocoding service method call
    verify(geocodingService, times(1)).geocoder(location);
    verify(mockResponse).setStatusCode(200); // Verify success status code
  }

  @Test
  void testGetCoordinatesFailure(VertxTestContext testContext) {
    HttpServerResponse mockResponse = mock(HttpServerResponse.class);
    String location = "dummy-location";

    when(routingContext.response()).thenReturn(mockResponse);
    when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
    when(routingContext.queryParams()).thenReturn(mock(MultiMap.class));
    when(routingContext.queryParams().contains("q")).thenReturn(true);
    when(routingContext.queryParams().get("q")).thenReturn(location);

    doAnswer(invocation -> {
      testContext.completeNow();
      return null;
    }).when(mockResponse).end(anyString());
    when(geocodingService.geocoder(location)).thenReturn(Future.failedFuture("Fail"));

    geocodingController.getCoordinates(routingContext);

    // Verify that the response is set with a 400 status code for missing location
    verify(routingContext.response(), times(1)).setStatusCode(400);
  }

  @Test
  void testGetCoordinatesEmptyQueryParamVal(VertxTestContext testContext) {
    HttpServerResponse mockResponse = mock(HttpServerResponse.class);
    String location = "";

    when(routingContext.response()).thenReturn(mockResponse);
    when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
    when(routingContext.queryParams()).thenReturn(mock(MultiMap.class));
    when(routingContext.queryParams().contains("q")).thenReturn(true);
    when(routingContext.queryParams().get("q")).thenReturn(location);

    doAnswer(invocation -> {
      testContext.completeNow();
      return null;
    }).when(mockResponse).end(anyString());
    //when(geocodingService.geocoder(location)).thenReturn(Future.failedFuture("Fail"));

    geocodingController.getCoordinates(routingContext);

    // Verify that the response is set with a 400 status code for missing location
    verify(routingContext.response(), times(1)).setStatusCode(400);
  }

  @Test
  void testGetCoordinatesInvalidQueryParam(VertxTestContext testContext) {
    HttpServerResponse mockResponse = mock(HttpServerResponse.class);
    String location = "";

    when(routingContext.response()).thenReturn(mockResponse);
    when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
    when(routingContext.queryParams()).thenReturn(mock(MultiMap.class));
    when(routingContext.queryParams().contains("q")).thenReturn(false);

    doAnswer(invocation -> {
      testContext.completeNow();
      return null;
    }).when(mockResponse).end(anyString());
    //when(geocodingService.geocoder(location)).thenReturn(Future.failedFuture("Fail"));

    geocodingController.getCoordinates(routingContext);

    // Verify that the response is set with a 400 status code for missing location
    verify(routingContext.response(), times(1)).setStatusCode(400);
  }

  @Test
  void testGetLocationSuccess(VertxTestContext testContext) {
    String coordinates = "[1.0, 2.0]";
    String geometry = "Point";
    JsonObject responseJson = new JsonObject().put("location", new JsonObject().put("coordinates", new JsonArray().add(1.0).add(2.0)));

    HttpServerResponse mockResponse = mock(HttpServerResponse.class);
    HttpServerRequest mockRequest = mock(HttpServerRequest.class);
    when(routingContext.response()).thenReturn(mockResponse);
    when(routingContext.request()).thenReturn(mockRequest);
    when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
    when(routingContext.request().getParam("coordinates")).thenReturn(coordinates);
    when(routingContext.request().getParam("geometry")).thenReturn(geometry);
    when(geocodingService.reverseGeocoder(anyString(), anyString())).thenReturn(Future.succeededFuture(responseJson));

    doAnswer(invocation -> {
      testContext.completeNow();
      return null;
    }).when(mockResponse).end(anyString());

    geocodingController.getLocation(routingContext);

    verify(geocodingService, times(1)).reverseGeocoder(anyString(), anyString());
  }

  @Test
  void testGetLocationFailure(VertxTestContext testContext) {
    String coordinates = "[1.0, 2.0]";
    String geometry = "Point";

    HttpServerResponse mockResponse = mock(HttpServerResponse.class);
    HttpServerRequest mockRequest = mock(HttpServerRequest.class);
    when(routingContext.response()).thenReturn(mockResponse);
    when(routingContext.request()).thenReturn(mockRequest);
    when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
    when(routingContext.request().getParam("coordinates")).thenReturn(coordinates);
    when(routingContext.request().getParam("geometry")).thenReturn(geometry);
    when(geocodingService.reverseGeocoder(anyString(), anyString())).thenReturn(Future.failedFuture("Fail"));

    doAnswer(invocation -> {
      testContext.completeNow();
      return null;
    }).when(mockResponse).end();

    geocodingController.getLocation(routingContext);

    verify(geocodingService, times(1)).reverseGeocoder(anyString(), anyString());
    verify(routingContext.response(), times(1)).setStatusCode(400);
  }

  @Test
  void testGetLocationInvalidSyntax(VertxTestContext testContext) {
    HttpServerResponse mockResponse = mock(HttpServerResponse.class);
    HttpServerRequest mockRequest = mock(HttpServerRequest.class);
    when(routingContext.response()).thenReturn(mockResponse);
    when(routingContext.request()).thenReturn(mockRequest);
    when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
    when(routingContext.request().getParam("coordinates")).thenReturn(null);

    doAnswer(invocation -> {
      testContext.completeNow();
      return null;
    }).when(mockResponse).end(anyString());

    geocodingController.getLocation(routingContext);

    verify(routingContext.response(), times(1)).setStatusCode(400);
  }

  @Test
  void testGetLocationInvalidGeometry(VertxTestContext testContext) {
    String coordinates = "[1.0, 2.0]";
    String geometry = "Polygon";
    JsonObject responseJson = new JsonObject().put("location", new JsonObject().put("coordinates", new JsonArray().add(1.0).add(2.0)));

    HttpServerResponse mockResponse = mock(HttpServerResponse.class);
    HttpServerRequest mockRequest = mock(HttpServerRequest.class);
    when(routingContext.response()).thenReturn(mockResponse);
    when(routingContext.request()).thenReturn(mockRequest);
    when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
    when(routingContext.request().getParam(GEOMETRY)).thenReturn(geometry);
    when(routingContext.request().getParam(COORDINATES)).thenReturn(coordinates);
    when(geocodingService.reverseGeocoder(anyString(), anyString())).thenReturn(Future.succeededFuture(responseJson));

    doAnswer(invocation -> {
      testContext.completeNow();
      return null;
    }).when(mockResponse).end();

    geocodingController.getLocation(routingContext);

    verify(mockResponse, times(1)).setStatusCode(400);
  }
}