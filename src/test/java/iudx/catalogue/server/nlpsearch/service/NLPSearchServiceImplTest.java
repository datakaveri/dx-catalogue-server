package iudx.catalogue.server.nlpsearch.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import jdk.jfr.Description;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static iudx.catalogue.server.util.Constants.SERVICE_TIMEOUT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class NLPSearchServiceImplTest {
  NLPSearchServiceImpl nlpSearchService;
  @Mock WebClient webClient;
  @Mock Handler<AsyncResult<JsonObject>> handler;
  @Mock HttpRequest<Buffer> httpRequest;
  @Mock HttpResponse<Buffer> httpResponse;
  @Mock Buffer buffer;
  @Mock Throwable throwable;
  @Mock AsyncResult<io.vertx.ext.web.client.HttpResponse<io.vertx.core.buffer.Buffer>> ar;

  @Test
  @Description("testing the method getEmbedding when handler succeeded")
  public void testGetEmbedding(VertxTestContext vertxTestContext) {
    String nlpServiceUrl = "dummy";
    JsonObject json = new JsonObject();
    int nlpServicePort = 200;
    JsonObject doc = new JsonObject();
    NLPSearchServiceImpl.webClient = mock(WebClient.class);
    when(webClient.post(nlpServicePort, nlpServiceUrl, "/indexdoc")).thenReturn(httpRequest);
    when(httpRequest.timeout(SERVICE_TIMEOUT)).thenReturn(httpRequest);
    when(ar.succeeded()).thenReturn(true);
    when(ar.result()).thenReturn(httpResponse);
    when(httpResponse.body()).thenReturn(buffer);
    when(buffer.toJsonObject()).thenReturn(json);
    doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0)
                  throws Throwable {

                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(1)).handle(ar);
                return null;
              }
            })
        .when(httpRequest)
        .sendJsonObject(any(), any());
    nlpSearchService = new NLPSearchServiceImpl(webClient, nlpServiceUrl, nlpServicePort);
    nlpSearchService.getEmbedding(doc)
        .onComplete(handler -> {
          if (handler.succeeded()) {
            verify(httpRequest, times(1)).sendJsonObject(any(), any());
            verify(httpRequest, times(1)).timeout(SERVICE_TIMEOUT);
            verify(NLPSearchServiceImpl.webClient, times(1))
                .post(anyInt(), anyString(), anyString());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("testing the method getEmbedding when handler failed")
  public void testGetEmbeddingFailure(VertxTestContext vertxTestContext) {
    String nlpServiceUrl = "dummy";
    JsonObject json = new JsonObject();
    int nlpServicePort = 200;
    JsonObject doc = new JsonObject();
    NLPSearchServiceImpl.webClient = mock(WebClient.class);
    when(webClient.post(nlpServicePort, nlpServiceUrl, "/indexdoc")).thenReturn(httpRequest);
    when(httpRequest.timeout(SERVICE_TIMEOUT)).thenReturn(httpRequest);
    when(ar.succeeded()).thenReturn(false);
    when(ar.cause()).thenReturn(throwable);
    doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0)
                  throws Throwable {

                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(1)).handle(ar);
                return null;
              }
            })
        .when(httpRequest)
        .sendJsonObject(any(), any());
    nlpSearchService = new NLPSearchServiceImpl(webClient, nlpServiceUrl, nlpServicePort);
    nlpSearchService.getEmbedding(doc)
        .onComplete(handler -> {
          if (handler.failed()) {
            verify(httpRequest, times(1)).sendJsonObject(any(), any());
            verify(httpRequest, times(1)).timeout(SERVICE_TIMEOUT);
            verify(NLPSearchServiceImpl.webClient, times(1))
                .post(anyInt(), anyString(), anyString());

            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("testing the method search when handler succeeded")
  public void testSearch(VertxTestContext vertxTestContext) {
    String nlpServiceUrl = "dummy";
    JsonObject json = new JsonObject();
    int nlpServicePort = 200;
    String query = "dummy";
    NLPSearchServiceImpl.webClient = mock(WebClient.class);
    when(webClient.get(nlpServicePort, nlpServiceUrl, "/search")).thenReturn(httpRequest);
    when(httpRequest.timeout(SERVICE_TIMEOUT)).thenReturn(httpRequest);
    when(httpRequest.addQueryParam("q", query)).thenReturn(httpRequest);
    when(httpRequest.putHeader("Accept", "application/json")).thenReturn(httpRequest);
    when(ar.succeeded()).thenReturn(true);
    when(ar.result()).thenReturn(httpResponse);
    when(httpResponse.body()).thenReturn(buffer);
    when(buffer.toJsonObject()).thenReturn(json);
    doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0)
                  throws Throwable {

                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(ar);
                return null;
              }
            })
        .when(httpRequest)
        .send(any());
    nlpSearchService = new NLPSearchServiceImpl(webClient, nlpServiceUrl, nlpServicePort);
    nlpSearchService.search(query)
        .onComplete(handler -> {
          if (handler.succeeded()) {
            verify(httpRequest, times(1)).send(any());
            verify(httpRequest, times(1)).timeout(SERVICE_TIMEOUT);
            verify(httpRequest, times(1)).addQueryParam(anyString(), anyString());
            verify(httpRequest, times(1)).putHeader("Accept", "application/json");
            verify(NLPSearchServiceImpl.webClient, times(1))
                .get(anyInt(), anyString(), anyString());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("testing the method search when handler failed")
  public void testSearchFailure(VertxTestContext vertxTestContext) {
    String nlpServiceUrl = "dummy";
    int nlpServicePort = 200;
    String query = "dummy";
    NLPSearchServiceImpl.webClient = mock(WebClient.class);
    when(webClient.get(nlpServicePort, nlpServiceUrl, "/search")).thenReturn(httpRequest);
    when(httpRequest.timeout(SERVICE_TIMEOUT)).thenReturn(httpRequest);
    when(httpRequest.addQueryParam("q", query)).thenReturn(httpRequest);
    when(httpRequest.putHeader("Accept", "application/json")).thenReturn(httpRequest);
    when(ar.succeeded()).thenReturn(false);
    when(ar.cause()).thenReturn(throwable);
    doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0)
                  throws Throwable {

                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(ar);
                return null;
              }
            })
        .when(httpRequest)
        .send(any());
    nlpSearchService = new NLPSearchServiceImpl(webClient, nlpServiceUrl, nlpServicePort);
    nlpSearchService.search(query)
        .onComplete(handler -> {
          if (handler.failed()) {
            verify(httpRequest, times(1)).send(any());
            verify(httpRequest, times(1)).timeout(SERVICE_TIMEOUT);
            verify(httpRequest, times(1)).addQueryParam(anyString(), anyString());
            verify(httpRequest, times(1)).putHeader("Accept", "application/json");
            verify(NLPSearchServiceImpl.webClient, times(1))
                .get(anyInt(), anyString(), anyString());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }
}
