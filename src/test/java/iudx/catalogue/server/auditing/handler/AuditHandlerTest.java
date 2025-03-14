package iudx.catalogue.server.auditing.handler;

import static iudx.catalogue.server.auditing.util.Constants.API;
import static iudx.catalogue.server.auditing.util.Constants.EPOCH_TIME;
import static iudx.catalogue.server.auditing.util.Constants.IID;
import static iudx.catalogue.server.auditing.util.Constants.IUDX_ID;
import static iudx.catalogue.server.auditing.util.Constants.USER_ID;
import static iudx.catalogue.server.auditing.util.Constants.USER_ROLE;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import iudx.catalogue.server.auditing.service.AuditingService;
import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.common.RoutingContextHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(VertxExtension.class)
class AuditHandlerTest {

  private AuditingService auditingService;
  private AuditHandler auditHandler;
  private RoutingContext routingContext;
  private JwtData jwtData;

  @BeforeEach
  void setUp() {
    auditingService = mock(AuditingService.class);
    auditHandler = new AuditHandler(auditingService);
    routingContext = mock(RoutingContext.class);
    jwtData = mock(JwtData.class);
  }

  @Test
  void testHandleWithValidData() {
    when(jwtData.getRole()).thenReturn("admin");
    when(jwtData.getSub()).thenReturn("user123");
    when(jwtData.getIid()).thenReturn("iid123");

    when(RoutingContextHelper.getJwtData(routingContext)).thenReturn(jwtData);
    when(routingContext.request()).thenReturn(mock(HttpServerRequest.class));
    when(routingContext.request().method()).thenReturn(HttpMethod.POST);

    JsonObject requestBody = new JsonObject().put("id", "resource-id-123");
    when(RoutingContextHelper.getValidatedRequest(routingContext)).thenReturn(requestBody);
    when(auditingService.insertAuditingValuesInRmq(any(JsonObject.class)))
        .thenReturn(Future.succeededFuture());

    auditHandler.handle(routingContext, "/test-api");

    ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
    verify(auditingService).insertAuditingValuesInRmq(captor.capture());
    JsonObject capturedAuditInfo = captor.getValue();

    assertEquals("admin", capturedAuditInfo.getString(USER_ROLE));
    assertEquals("user123", capturedAuditInfo.getString(USER_ID));
    assertEquals("iid123", capturedAuditInfo.getString(IID));
    assertEquals("resource-id-123", capturedAuditInfo.getString(IUDX_ID));
    assertEquals("/test-api", capturedAuditInfo.getString(API));
    assertNotNull(capturedAuditInfo.getLong(EPOCH_TIME));
  }
  @Test
  void testHandleFailure() {
    when(jwtData.getRole()).thenReturn("admin");
    when(jwtData.getSub()).thenReturn("user123");
    when(jwtData.getIid()).thenReturn("iid123");

    when(RoutingContextHelper.getJwtData(routingContext)).thenReturn(jwtData);
    when(routingContext.request()).thenReturn(mock(HttpServerRequest.class));
    when(routingContext.request().method()).thenReturn(HttpMethod.POST);

    JsonObject requestBody = new JsonObject().put("id", "resource-id-123");
    when(RoutingContextHelper.getValidatedRequest(routingContext)).thenReturn(requestBody);
    when(auditingService.insertAuditingValuesInRmq(any(JsonObject.class)))
        .thenReturn(Future.failedFuture("Failed to publish message in RMQ."));

    auditHandler.handle(routingContext, "/test-api");
  }

  @Test
  void testHandleNextCalled() {
    auditHandler.handle(routingContext);
    verify(routingContext).next();
  }
}

