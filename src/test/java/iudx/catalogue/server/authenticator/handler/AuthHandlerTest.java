package iudx.catalogue.server.authenticator.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.authenticator.handler.authentication.AuthHandler;
import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.authenticator.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AuthHandlerTest {
  private AuthenticationService authService;
  private AuthHandler authHandler;
  private RoutingContext context;
  private HttpServerResponse response;
  private MultiMap headers;

  @BeforeEach
  void setUp() {
    authService = mock(AuthenticationService.class);
    authHandler = new AuthHandler(authService);
    context = mock(RoutingContext.class);
    response = mock(HttpServerResponse.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
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
    when(headers.get(anyString())).thenReturn("token");
    JwtData jwtData = mock(JwtData.class);

    when(authService.decodeToken(any())).thenReturn(Future.succeededFuture(jwtData));

    authHandler.handle(context);

    verify(context).next();
  }

  @Test
  void testHandleFailedDecoding() {
    when(headers.contains(anyString())).thenReturn(true);
    when(headers.get(anyString())).thenReturn("token");
    when(authService.decodeToken(any())).thenReturn(Future.failedFuture("Invalid Token"));

    authHandler.handle(context);

    verify(context).fail(any());
  }
}
