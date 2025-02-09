package iudx.catalogue.server.authenticator.handler;

import static iudx.catalogue.server.authenticator.Constants.TOKEN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.authenticator.service.AuthenticationService;
import iudx.catalogue.server.authenticator.model.JwtAuthenticationInfo;
import iudx.catalogue.server.common.RoutingContextHelper;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.MultiMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.shaded.org.yaml.snakeyaml.tokens.Token;

@ExtendWith(MockitoExtension.class)
public class AuthenticationHandlerTest {
  private AuthenticationService authService;
  private AuthenticationHandler authHandler;
  private RoutingContext context;
  private HttpServerResponse response;
  private HttpServerRequest request;
  private MultiMap headers;

  @BeforeEach
  void setUp() {
    authService = mock(AuthenticationService.class);
    authHandler = new AuthenticationHandler(authService);
    context = mock(RoutingContext.class);
    response = mock(HttpServerResponse.class);
    request = mock(HttpServerRequest.class);
    headers = mock(MultiMap.class);

    when(context.request()).thenReturn(request);
    when(request.headers()).thenReturn(headers);
  }

  @Test
  void testHandleMissingToken() {
    when(headers.contains(anyString())).thenReturn(false);
    when(context.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);

    authHandler.handle(context);

    verify(response).setStatusCode(401);
    verify(response).end();
  }

  @Test
  void testHandleSuccess() {
    when(headers.contains(anyString())).thenReturn(true);
    JwtData jwtData = mock(JwtData.class);
    JsonObject jwtJsonObject = new JsonObject().put(TOKEN, "token");
    JwtAuthenticationInfo jwtAuthInfo = new JwtAuthenticationInfo(jwtJsonObject);
    when(authService.decodeToken(any())).thenReturn(Future.succeededFuture(jwtData));
    when(authService.tokenIntrospect(any(), any())).thenReturn(Future.succeededFuture());
    when(RoutingContextHelper.getJwtAuthInfo(context)).thenReturn(jwtAuthInfo);

    authHandler.handle(context);

    verify(context).next();
  }

  @Test
  void testHandleFailedDecoding() {
    when(headers.contains(anyString())).thenReturn(true);
    JsonObject jwtJsonObject = new JsonObject().put(TOKEN, "token");
    JwtAuthenticationInfo jwtAuthInfo = new JwtAuthenticationInfo(jwtJsonObject);
    when(RoutingContextHelper.getJwtAuthInfo(context)).thenReturn(jwtAuthInfo);
    when(authService.decodeToken(any()))
        .thenReturn(Future.failedFuture("Invalid Token"));

    authHandler.handle(context);

    verify(context).fail(any());
  }

  @Test
  void testHandleFailedAuthentication() {
    when(headers.contains(anyString())).thenReturn(true);
    JwtData jwtData = mock(JwtData.class);
    JsonObject jwtJsonObject = new JsonObject().put(TOKEN, "token");
    JwtAuthenticationInfo jwtAuthInfo = new JwtAuthenticationInfo(jwtJsonObject);
    when(RoutingContextHelper.getJwtAuthInfo(context)).thenReturn(jwtAuthInfo);
    when(authService.decodeToken(any())).thenReturn(Future.succeededFuture(jwtData));
    when(authService.tokenIntrospect(any(), any())).thenReturn(Future.failedFuture("User information is invalid"));

    RoutingContextHelper.setJwtDecodedInfo(context, jwtData);

    authHandler.handle(context);

    verify(context).fail(any());
  }
}
