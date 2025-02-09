package iudx.catalogue.server.geocoding.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import iudx.catalogue.server.Configuration;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static iudx.catalogue.server.geocoding.util.Constants.*;
import static iudx.catalogue.server.util.Constants.TITLE_ITEM_NOT_FOUND;
import static iudx.catalogue.server.util.Constants.TYPE_ITEM_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GeocodingServiceTest {
  @Mock Handler<AsyncResult<String>> handler;
  @Mock WebClient webClient;
  @Mock AsyncResult<io.vertx.ext.web.client.HttpResponse<io.vertx.core.buffer.Buffer>> ar;
  @Mock HttpRequest<Buffer> httpRequest;
  @Mock HttpResponse<Buffer> httpResponse;
  @Mock Buffer buffer;
  @Mock Throwable throwable;
  @Mock AsyncResult<JsonObject> asyncResult;
  private static GeocodingService geoService;
  private static JsonObject config;
  private static Vertx vertxObj;
  private static final Logger LOGGER = LogManager.getLogger(GeocodingServiceTest.class);
  JsonObject doc =
      new JsonObject(
          "{\"tags\": [\"a\",\"b\",\"c\"], \"description\": \"some description,with characters\", \"name\": \"iudx\", \"label\": \"thisisiudx\", \"descriptor\": {\"co2\": \"high\", \"no2\": [\"low\", \"medium\"]}, \"location\": {\"type\": \"Place\",\"address\": \"Pune\",\"geometry\": {\"type\": \"Point\", \"coordinates\": [\"77.570423\",\"13.013945\"]}}}");

  @BeforeAll
  static void startVertx(Vertx vertx, VertxTestContext testContext) {
    vertxObj = vertx;

    config = Configuration.getConfiguration("./configs/config-test.json", 1);

    WebClientOptions webClientOptions = new WebClientOptions();
    webClientOptions.setTrustAll(true).setVerifyHost(false);
    WebClient client = WebClient.create(vertx, webClientOptions);
    // WebClient client = GeocodingVerticle.createWebClient(vertxObj, config, true);
    geoService = new GeocodingServiceImpl(client, "pelias_api", 4000);

    LOGGER.info("GeocodingController Service setup complete");
    testContext.completeNow();
  }

  @AfterEach
  public void finish(VertxTestContext testContext) {
    LOGGER.info("Finishing....");
    vertxObj.close(testContext.succeeding(response -> testContext.completeNow()));
  }

  @Test
  @DisplayName("Summarize test")
  void summarize(VertxTestContext testContext) {
    geoService.geoSummarize(doc)
        .onComplete(ar -> {
          LOGGER.debug("Result: " + ar.result());
          testContext.completeNow();
        });
  }

  @Test
  @DisplayName("geoCoder test when handler succeeded with max confidence")
  void testGeocoderMaxConfidence(VertxTestContext testContext) {
    String location = "dummy";
    String peliasUrl = "dummy";
    int peliasPort = 200;
    JsonObject json = new JsonObject();
    JsonObject feature = new JsonObject();
    JsonObject properties = new JsonObject();
    properties.put("confidence", 0.7);
    feature
        .put("properties", properties)
        .put("bbox", new JsonArray().add(77.563072).add(12.968362).add(77.573072).add(12.978362));
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(feature);
    jsonArray.add(new JsonObject().put("properties", new JsonObject().put("confidence", 1)));
    json.put("features", jsonArray);
    GeocodingServiceImpl.webClient = mock(WebClient.class);
    when(webClient.get(anyInt(), anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.timeout(anyLong())).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeader(anyString(), anyString())).thenReturn(httpRequest);
    when(httpResponse.body()).thenReturn(buffer);
    when(buffer.toJsonObject()).thenReturn(json);
    doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<io.vertx.ext.web.client.HttpResponse<io.vertx.core.buffer.Buffer>>
                  answer(InvocationOnMock arg0) throws Throwable {
                return Future.succeededFuture(httpResponse);
              }
            })
        .when(httpRequest)
        .send();

    geoService = new GeocodingServiceImpl(webClient, peliasUrl, peliasPort);
    geoService.geocoder(location)
        .onComplete(handler -> {
          if (handler.succeeded()) {
            verify(httpRequest, times(1)).send();
            testContext.completeNow();
          } else {
            testContext.failNow("Fail");
          }
        });
  }

  @Test
  @DisplayName("geoCoder test when handler succeeded with confidence equal to previous confidence")
  void testGeocoderEqualConfidence(VertxTestContext testContext) {
    String location = "dummy";
    String peliasUrl = "dummy";
    int peliasPort = 200;
    JsonObject json = new JsonObject();
    JsonObject feature = new JsonObject();
    JsonObject properties = new JsonObject();
    properties.put("confidence", 0);
    feature.put("properties", properties);
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(feature);
    json.put("features", jsonArray);
    GeocodingServiceImpl.webClient = mock(WebClient.class);
    when(webClient.get(anyInt(), anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.timeout(anyLong())).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeader(anyString(), anyString())).thenReturn(httpRequest);
    when(httpResponse.body()).thenReturn(buffer);
    when(buffer.toJsonObject()).thenReturn(json);
    doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<io.vertx.ext.web.client.HttpResponse<io.vertx.core.buffer.Buffer>>
                  answer(InvocationOnMock arg0) throws Throwable {
                return Future.succeededFuture(httpResponse);
              }
            })
        .when(httpRequest)
        .send();

    geoService = new GeocodingServiceImpl(webClient, peliasUrl, peliasPort);
    geoService.geocoder(location)
        .onComplete(handler -> {
          if (handler.succeeded()) {
            verify(httpRequest, times(1)).send();
            testContext.completeNow();
          } else {
            testContext.failNow("Fail");
          }
        });
  }

  @Test
  @DisplayName("geoCoder test when handler failed")
  void testGeoCoderFailed(VertxTestContext testContext) {
    String location = "dummy";
    String peliasUrl = "dummy";
    int peliasPort = 200;
    GeocodingServiceImpl.webClient = mock(WebClient.class);
    when(webClient.get(anyInt(), anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.timeout(anyLong())).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeader(anyString(), anyString())).thenReturn(httpRequest);
    when(httpResponse.body()).thenReturn(buffer);
    when(buffer.toJsonObject()).thenReturn(new JsonObject());
    //    when(ar.cause()).thenReturn(throwable);
    when(httpRequest.send()).thenReturn(Future.succeededFuture(httpResponse));
    geoService = new GeocodingServiceImpl(webClient, peliasUrl, peliasPort);
    JsonObject expected = new JsonObject();
    expected.put("type", TYPE_ITEM_NOT_FOUND);
    expected.put("title", TITLE_ITEM_NOT_FOUND);
    expected.put("detail", "Failed to find coordinates");
    geoService.geocoder(location)
        .onComplete(handler -> {
          if (handler.failed()) {
            verify(httpRequest, times(1)).send();
            assertEquals(expected.encode(), handler.cause().getMessage());
            testContext.completeNow();
          } else {
            testContext.failNow("Fail");
          }
        });
  }

  @Test
  @DisplayName("ReverseGeocoder test when handler succeeded")
  void testReverseGeocoderSucceeded(VertxTestContext testContext) {
    String lat = "dummy";
    String lon = "dummy";
    JsonObject json = new JsonObject();
    json.put("dummy key", "dummy value");
    String peliasUrl = "dummy";
    int peliasPort = 200;
    GeocodingServiceImpl.webClient = mock(WebClient.class);
    when(webClient.get(anyInt(), anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.timeout(anyLong())).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeader(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(Future.succeededFuture(httpResponse));
    geoService = new GeocodingServiceImpl(webClient, peliasUrl, peliasPort);
    geoService.reverseGeocoder(
        lat,
        lon)
        .onComplete(handler -> {
          if (handler.succeeded()) {
            verify(httpRequest, times(1)).send();
            testContext.completeNow();
          } else {
            testContext.failNow("Fail");
          }
        });
  }

  @Test
  @DisplayName("reverseGeocoder test when handler failed")
  void testReverseGeocoderFailed(VertxTestContext testContext) {
    String lat = "dummy";
    String lon = "dummy";
    String peliasUrl = "dummy";
    int peliasPort = 200;
    GeocodingServiceImpl.webClient = mock(WebClient.class);
    when(webClient.get(anyInt(), anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.timeout(anyLong())).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeader(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(Future.failedFuture(throwable));
    geoService = new GeocodingServiceImpl(webClient, peliasUrl, peliasPort);
    geoService.reverseGeocoder(
        lat,
        lon)
        .onComplete(handler -> {
          if (handler.failed()) {
            verify(httpRequest, times(1)).send();
            testContext.completeNow();
          } else {
            testContext.failNow("Fail");
          }
        });
  }

  @Test
  @DisplayName("geoSummarize test")
  void testGeoSummarize(VertxTestContext testContext) {
    String peliasUrl = "dummy";
    int peliasPort = 200;
    JsonObject jsonObject = new JsonObject();
    JsonObject json = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    jsonArray.add("dummy");
    json.put("bbox", jsonArray);
    JsonObject jsonObject2 = new JsonObject();
    jsonObject2.put("address", "dummy");
    jsonObject.put("location", jsonObject2);
    GeocodingServiceImpl.webClient = mock(WebClient.class);
    when(webClient.get(anyInt(), anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.timeout(anyLong())).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeader(anyString(), anyString())).thenReturn(httpRequest);
    when(httpResponse.body()).thenReturn(buffer);
    when(buffer.toJsonObject()).thenReturn(json);
    when(httpRequest.send()).thenReturn(Future.succeededFuture(httpResponse));
    geoService = new GeocodingServiceImpl(webClient, peliasUrl, peliasPort);
    geoService.geoSummarize(jsonObject)
        .onComplete(handler -> {
          if (handler.succeeded()) {
            verify(httpRequest, times(1)).send();
            testContext.completeNow();
          } else {
            testContext.failNow("Fail");
          }
        });
  }

  @Test
  @DisplayName("testing method geoSummarize when address is null")
  void testGeoSummarizeNullAddress(VertxTestContext testContext) {
    String peliasUrl = "dummy";
    int peliasPort = 200;
    JsonObject jsonObject = new JsonObject();
    JsonObject jsonObject2 = new JsonObject();
    jsonObject2.put("address", null);
    jsonObject.put("location", jsonObject2);
    geoService = new GeocodingServiceImpl(webClient, peliasUrl, peliasPort);
    geoService.geoSummarize(jsonObject);
    testContext.completeNow();
  }

  @Test
  @DisplayName("testing method geocoderHelper")
  void testGeocoderHelper(VertxTestContext testContext) {
    String peliasUrl = "dummy";
    int peliasPort = 200;
    JsonObject doc =
        new JsonObject()
            .put(
                LOCATION,
                new JsonObject()
                    .put(
                        GEOMETRY,
                        new JsonObject()
                            .put(TYPE, "point")
                            .put(
                                COORDINATES,
                                new JsonArray().add(0, "73.8723841").add(1, "18.527615"))));
    JsonObject jsonObject = new JsonObject();
    JsonObject jsonObject2 = new JsonObject();
    JsonObject jsonObject3 = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(0, "dummy");
    jsonArray.add(1, "dummy");
    JsonObject json = new JsonObject();
    JsonArray jsonArray2 = new JsonArray();
    JsonObject jsonObject4 = new JsonObject();
    JsonObject jsonObject5 = new JsonObject();
    jsonObject4.put("properties", jsonObject5);
    jsonArray2.add(0, jsonObject4);
    json.put("features", jsonArray2);
    jsonObject3.put("coordinates", jsonArray);
    jsonObject2.put("address", null);
    jsonObject2.put("geometry", jsonObject3);
    GeocodingServiceImpl.webClient = mock(WebClient.class);
    when(webClient.get(anyInt(), anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.timeout(anyLong())).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeader(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(Future.succeededFuture(httpResponse));
    geoService = new GeocodingServiceImpl(webClient, peliasUrl, peliasPort);
    geoService.geoSummarize(doc)
        .onComplete(handler -> {
          if (handler.succeeded()) {
            verify(httpRequest, times(1)).send();
            testContext.completeNow();
          } else {
            testContext.failNow("Fail");
          }
        });
  }
}
