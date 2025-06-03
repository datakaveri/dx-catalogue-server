package iudx.catalogue.server.authenticator;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.impl.predicate.ResponsePredicateImpl;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.server.apiserver.SearchApis;
import jdk.jfr.Description;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static iudx.catalogue.server.auditing.util.Constants.*;
import static iudx.catalogue.server.authenticator.Constants.*;
import static iudx.catalogue.server.authenticator.Constants.OPERATION;
import static iudx.catalogue.server.util.Constants.PROVIDER;
import static junit.framework.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class AuthenticationServiceImplTest {
  AuthenticationServiceImpl authenticationService;
  @Mock WebClient webClient;
  @Mock JsonObject request;
  @Mock JsonObject authenticationInfo;
  @Mock AsyncResult<JsonObject> asyncResult;
  @Mock Handler<AsyncResult<JsonObject>> handler;
  @Mock HttpRequest<Buffer> httpRequest;
  @Mock HttpResponse<Buffer> httpResponse;
  @Mock JsonObject json;
  @Mock AsyncResult<HttpResponse<Buffer>> httpResponseAsyncResult;

  private JsonObject config;


  @Test
  @Description("testing the method validateAuthInfo")
  public void testValidateAuthInfo(VertxTestContext vertxTestContext) {
    String authHost = "dummy";
    authenticationInfo = new JsonObject();


    request = new JsonObject();
    authenticationInfo.put(TOKEN, "dummy");
    authenticationInfo.put(OPERATION, "dummy");
    request.put(PROVIDER, "dummy");
    AuthenticationServiceImpl.webClient = mock(WebClient.class);
    when(webClient.post(anyInt(), anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.expect(any())).thenReturn(httpRequest);
    when(httpResponseAsyncResult.failed()).thenReturn(true);

    doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0)
                  throws Throwable {

                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(1))
                    .handle(httpResponseAsyncResult);
                return null;
              }
            })
        .when(httpRequest)
        .sendJsonObject(any(), any());
    config = new JsonObject();

    config.put("dxApiBasePath", "/iudx/cat/v1");
    config.put("dxAuthBasePath", "/auth/v1");

    authenticationService = new AuthenticationServiceImpl(webClient, authHost, config);
    authenticationService.tokenInterospect(
        request,
        authenticationInfo,
        handler -> {
          if (handler.succeeded()) {
            verify(httpRequest, times(1)).sendJsonObject(any(), any());
            verify(httpRequest, times(1)).expect(any());
            verify(AuthenticationServiceImpl.webClient, times(1))
                .post(anyInt(), anyString(), anyString());

            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Description("testing the method validateAuthInfo")
  public void testIsPermittedProviderID(VertxTestContext vertxTestContext) {
    String authHost = "dummy";
    authenticationInfo = new JsonObject();
    request = new JsonObject();
    json = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    json.put(ERROR, "error");
    json.put(MESSAGE, "dummy msg");
    json.put(REQUEST, jsonArray);
    jsonArray.add(json);
    json.put(ID, "abcd/abcd/abcd/abcd");
    json.put(PROVIDER, "dummy");
    JsonArray responseRequests = new JsonArray();
    authenticationInfo.put(TOKEN, "dummy");
    authenticationInfo.put(OPERATION, "dummy");
    request.put(PROVIDER, "dummy");
    AuthenticationServiceImpl.webClient = mock(WebClient.class);
    when(webClient.post(anyInt(), anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.expect(any())).thenReturn(httpRequest);
    when(httpResponseAsyncResult.result()).thenReturn(httpResponse);

    config = new JsonObject();

    config.put("dxApiBasePath", "/iudx/cat/v1");
    config.put("dxAuthBasePath", "/auth/v1");
    authenticationService = new AuthenticationServiceImpl(webClient, "dummy", config);


    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.bodyAsJsonObject()).thenReturn(json);
    doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0)
                  throws Throwable {

                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(1))
                    .handle(httpResponseAsyncResult);
                return null;
              }
            })
        .when(httpRequest)
        .sendJsonObject(any(), any());

    authenticationService.tokenInterospect(
        request,
        authenticationInfo,
        handler -> {
          if (handler.succeeded()) {
            verify(httpRequest, times(1)).sendJsonObject(any(), any());
            verify(httpRequest, times(1)).expect(any());
            verify(AuthenticationServiceImpl.webClient, times(1))
                .post(anyInt(), anyString(), anyString());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Description("testing the method validateAuthInfo")
  public void testIsPermittedMethodTrue(VertxTestContext vertxTestContext) {

    String authHost = "dummy";
    authenticationService = new AuthenticationServiceImpl(webClient, authHost, config);
    JsonArray methods = new JsonArray();
    methods.add("*");
    String operation = "dummy";
    assertTrue(authenticationService.isPermittedMethod(methods, operation));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing the method validateAuthInfo")
  public void testIsPermittedMethodFalse(VertxTestContext vertxTestContext) {

    String authHost = "dummy value";
    authenticationService = new AuthenticationServiceImpl(webClient, authHost, config);
    JsonArray methods = new JsonArray();
    methods.add("dummyy");
    String operation = "dummy";
    assertFalse(authenticationService.isPermittedMethod(methods, operation));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing the method validateAuthInfo when token and operation is blank")
  public void testValidateAuthInfoBlank(VertxTestContext vertxTestContext) {
    String authHost = "dummy";
    authenticationInfo = new JsonObject();
    request = new JsonObject();
    authenticationInfo.put(TOKEN, "");
    authenticationInfo.put(OPERATION, "");
    request.put(PROVIDER, "dummy");
    AuthenticationServiceImpl.webClient = mock(WebClient.class);
    authenticationService = new AuthenticationServiceImpl(webClient, authHost, config);
    assertNotNull(authenticationService.tokenInterospect(request, authenticationInfo, handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing the method validateAuthInfo when token and operation is blank")
  public void testValidateAuthInfoRegex(VertxTestContext vertxTestContext) {
    String authHost = "dummy";
    authenticationInfo = new JsonObject();
    request = new JsonObject();
    authenticationInfo.put(TOKEN, "!!");
    authenticationInfo.put(OPERATION, "dummy");
    request.put(PROVIDER, "dummy");
    AuthenticationServiceImpl.webClient = mock(WebClient.class);
    authenticationService = new AuthenticationServiceImpl(webClient, authHost, config);
    assertNotNull(authenticationService.tokenInterospect(request, authenticationInfo, handler));
    vertxTestContext.completeNow();
  }
}
