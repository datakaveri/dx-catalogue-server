package iudx.catalogue.server.nlpsearch.controller;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.junit5.VertxExtension;
import iudx.catalogue.server.nlpsearch.service.NLPSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.junit5.VertxTestContext;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class NLPSearchControllerTest {

  private NLPSearchController nlpSearchController;
  private NLPSearchService nlpSearchService;
  private RoutingContext routingContext;

  @BeforeEach
  void setUp() {
    nlpSearchService = mock(NLPSearchService.class);
    routingContext = mock(RoutingContext.class);
    nlpSearchController = new NLPSearchController(nlpSearchService);
  }

  @Test
  void testSetupRoutes() {
    Router mockRouter = mock(Router.class);
    when(mockRouter.get(anyString())).thenReturn(mock(Route.class));
    when(mockRouter.post(anyString())).thenReturn(mock(Route.class));
    nlpSearchController.setupRoutes(mockRouter);

    verify(mockRouter, times(1)).get("/nlp/search");
    verify(mockRouter, times(1)).post("/nlp/embedding");
  }

  @Test
  void testHandleSearchNullOrEmptyQuery(VertxTestContext testContext) {
    HttpServerResponse mockResponse = mock(HttpServerResponse.class);

    when(routingContext.request()).thenReturn(mock(HttpServerRequest.class));
    when(routingContext.request().getParam("q")).thenReturn(null); // Simulating null query
    when(routingContext.response()).thenReturn(mockResponse);
    when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
    doAnswer(invocation -> {
      testContext.completeNow();
      return null;
    }).when(mockResponse).end(anyString());

    nlpSearchController.handleSearch(routingContext);

    verify(mockResponse, times(1)).setStatusCode(400);
    verify(mockResponse, times(1)).end(anyString());
    testContext.completeNow();
  }

  @Test
  void testHandleSearchEmptyQuery(VertxTestContext testContext) {
    HttpServerResponse mockResponse = mock(HttpServerResponse.class);

    when(routingContext.request()).thenReturn(mock(HttpServerRequest.class));
    when(routingContext.request().getParam("q")).thenReturn(""); // Simulating empty query
    when(routingContext.response()).thenReturn(mockResponse);
    when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
    doAnswer(invocation -> {
      testContext.completeNow();
      return null;
    }).when(mockResponse).end(anyString());

    nlpSearchController.handleSearch(routingContext);

    verify(mockResponse, times(1)).setStatusCode(400);
    verify(mockResponse, times(1)).end(anyString());
    testContext.completeNow();
  }

  @Test
  void testHandleSearchSuccess(VertxTestContext testContext) {
    String query = "test query";
    HttpServerResponse mockResponse = mock(HttpServerResponse.class);

    when(routingContext.request()).thenReturn(mock(HttpServerRequest.class));
    when(routingContext.request().getParam("q")).thenReturn(query);
    when(routingContext.response()).thenReturn(mockResponse);
    when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
    when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    doAnswer(invocation -> {
      testContext.completeNow();
      return null;
    }).when(mockResponse).end(anyString());

    JsonObject response = new JsonObject().put("result", "success");
    when(nlpSearchService.search(query)).thenReturn(Future.succeededFuture(response));

    nlpSearchController.handleSearch(routingContext);

    verify(nlpSearchService, times(1)).search(query);
  }

  @Test
  void testHandleSearchFailure(VertxTestContext testContext) {
    String query = "test query";
    HttpServerResponse mockResponse = mock(HttpServerResponse.class);

    when(routingContext.request()).thenReturn(mock(HttpServerRequest.class));
    when(routingContext.request().getParam("q")).thenReturn(query);
    when(routingContext.response()).thenReturn(mockResponse);
    when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
    doAnswer(invocation -> {
      testContext.completeNow();
      return null;
    }).when(mockResponse).end(anyString());

    when(nlpSearchService.search(query)).thenReturn(Future.failedFuture("Service failure"));

    nlpSearchController.handleSearch(routingContext);

    verify(nlpSearchService, times(1)).search(query);
  }

  @Test
  void testHandleGetEmbeddingSuccess(VertxTestContext testContext) {
    Buffer buffer = Buffer.buffer(new JsonObject().put("key", "value").encode());
    HttpServerResponse mockResponse = mock(HttpServerResponse.class);

    when(routingContext.request()).thenReturn(mock(HttpServerRequest.class));
    when(routingContext.request().body()).thenReturn(Future.succeededFuture(buffer));
    when(routingContext.response()).thenReturn(mockResponse);
    when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
    when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    doAnswer(invocation -> {
      testContext.completeNow();
      return null;
    }).when(mockResponse).end(anyString());

    JsonObject response = new JsonObject().put("embedding", "success");
    when(nlpSearchService.getEmbedding(any(JsonObject.class))).thenReturn(Future.succeededFuture(response));

    nlpSearchController.handleGetEmbedding(routingContext);

    verify(nlpSearchService, times(1)).getEmbedding(any(JsonObject.class));
  }

  @Test
  void testHandleGetEmbeddingFailure(VertxTestContext testContext) {
    Buffer buffer = Buffer.buffer(new JsonObject().put("key", "value").encode());
    HttpServerResponse mockResponse = mock(HttpServerResponse.class);

    when(routingContext.request()).thenReturn(mock(HttpServerRequest.class));
    when(routingContext.request().body()).thenReturn(Future.succeededFuture(buffer));
    when(routingContext.response()).thenReturn(mockResponse);
    when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
    doAnswer(invocation -> {
      testContext.completeNow();
      return null;
    }).when(mockResponse).end(anyString());

    when(nlpSearchService.getEmbedding(any(JsonObject.class))).thenReturn(Future.failedFuture("Embedding failure"));

    nlpSearchController.handleGetEmbedding(routingContext);

    verify(nlpSearchService, times(1)).getEmbedding(any(JsonObject.class));
  }

  @Test
  void testHandleGetEmbeddingBodyFailure(VertxTestContext testContext) {
    HttpServerResponse mockResponse = mock(HttpServerResponse.class);

    when(routingContext.request()).thenReturn(mock(HttpServerRequest.class));
    when(routingContext.request().body()).thenReturn(Future.failedFuture("Body parsing failure"));
    when(routingContext.response()).thenReturn(mockResponse);
    when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
    doAnswer(invocation -> {
      testContext.completeNow();
      return null;
    }).when(mockResponse).end(anyString());

    nlpSearchController.handleGetEmbedding(routingContext);

    verify(nlpSearchService, never()).getEmbedding(any(JsonObject.class));
  }
}