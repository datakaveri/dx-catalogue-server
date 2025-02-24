package iudx.catalogue.server.authenticator.handler;

import static iudx.catalogue.server.util.Constants.ITEM_TYPE_RESOURCE_GROUP;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.authenticator.handler.authorization.AuthorizationHandler;
import iudx.catalogue.server.authenticator.model.DxRole;
import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.common.RoutingContextHelper;
import iudx.catalogue.server.exceptions.DxRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AuthorizationHandlerTest {

  private AuthorizationHandler authorizationHandler;

  private RoutingContext routingContext;

  private JwtData jwtData;

  @BeforeEach
  void setUp() {
    authorizationHandler = new AuthorizationHandler();
    routingContext = mock(RoutingContext.class);
    jwtData = mock(JwtData.class);
  }

  @Test
  void testHandleDefaultPassThrough() {
    authorizationHandler.handle(routingContext);
    verify(routingContext, times(1)).next();
  }

  @Test
  void testAuthorizedRoleBasedAccess() {
    when(RoutingContextHelper.getJwtData(routingContext)).thenReturn(jwtData);
    when(jwtData.getRole()).thenReturn("ADMIN");

    Handler<RoutingContext> handler = authorizationHandler.forRoleBasedAccess(DxRole.ADMIN);
    handler.handle(routingContext);

    verify(routingContext, times(1)).next();
  }

  @Test
  void testUnauthorizedRoleBasedAccess() {
    when(RoutingContextHelper.getJwtData(routingContext)).thenReturn(jwtData);
    when(jwtData.getRole()).thenReturn("USER");

    Handler<RoutingContext> handler = authorizationHandler.forRoleBasedAccess(DxRole.ADMIN);
    doThrow(DxRuntimeException.class).when(routingContext).fail(any(DxRuntimeException.class));

    assertThrows(DxRuntimeException.class, () -> handler.handle(routingContext));
  }

  @Test
  void testAuthorizedRoleAndEntityAccess() {
    when(RoutingContextHelper.getJwtData(routingContext)).thenReturn(jwtData);
    when(jwtData.getRole()).thenReturn(String.valueOf(DxRole.PROVIDER));
    when(RoutingContextHelper.getItemType(routingContext)).thenReturn(ITEM_TYPE_RESOURCE_GROUP);

    Handler<RoutingContext> handler = authorizationHandler.forRoleAndEntityAccess(DxRole.PROVIDER);
    handler.handle(routingContext);

    verify(routingContext, times(1)).next();
  }

  @Test
  void testUnauthorizedRoleRoleAndEntityAccess() {
    when(RoutingContextHelper.getJwtData(routingContext)).thenReturn(jwtData);
    when(jwtData.getRole()).thenReturn("USER");

    Handler<RoutingContext> handler = authorizationHandler.forRoleAndEntityAccess(DxRole.ADMIN);
    doThrow(DxRuntimeException.class).when(routingContext).fail(any(DxRuntimeException.class));

    assertThrows(DxRuntimeException.class, () -> handler.handle(routingContext));
  }

  @Test
  void testRoleAndEntityAccessForUnauthorizedEntity() {
    when(RoutingContextHelper.getJwtData(routingContext)).thenReturn(jwtData);
    when(jwtData.getRole()).thenReturn("ADMIN");
    when(RoutingContextHelper.getItemType(routingContext)).thenReturn("resource-group");

    Handler<RoutingContext> handler = authorizationHandler.forRoleAndEntityAccess(DxRole.ADMIN);
    doThrow(DxRuntimeException.class).when(routingContext).fail(any(DxRuntimeException.class));

    assertThrows(DxRuntimeException.class, () -> handler.handle(routingContext));
  }
  @Test
  void testRoleBasedAccessUnauthorizedRole() {
    when(RoutingContextHelper.getJwtData(routingContext)).thenReturn(jwtData);
    when(jwtData.getRole()).thenReturn("GUEST"); // A role not in allowed roles

    Handler<RoutingContext> handler = authorizationHandler.forRoleBasedAccess(DxRole.ADMIN, DxRole.PROVIDER);

    handler.handle(routingContext);

    ArgumentCaptor<DxRuntimeException> captor = ArgumentCaptor.forClass(DxRuntimeException.class);
    verify(routingContext).fail(captor.capture());
    assertEquals("No access for the given role", captor.getValue().getMessage());
  }

  @Test
  void testRoleAndEntityAccessUnauthorizedRole() {
    when(RoutingContextHelper.getJwtData(routingContext)).thenReturn(jwtData);
    when(jwtData.getRole()).thenReturn("GUEST"); // Unauthorized role

    Handler<RoutingContext> handler = authorizationHandler.forRoleAndEntityAccess(DxRole.ADMIN, DxRole.PROVIDER);

    handler.handle(routingContext);

    ArgumentCaptor<DxRuntimeException> captor = ArgumentCaptor.forClass(DxRuntimeException.class);
    verify(routingContext).fail(captor.capture());
    assertEquals("No access for the given role", captor.getValue().getMessage());
  }

  @Test
  void testRoleAndEntityAccessUnauthorizedEntity() {
    when(RoutingContextHelper.getJwtData(routingContext)).thenReturn(jwtData);
    when(jwtData.getRole()).thenReturn("PROVIDER");
    when(RoutingContextHelper.getItemType(routingContext)).thenReturn("INVALID_ENTITY");

    Handler<RoutingContext> handler = authorizationHandler.forRoleAndEntityAccess(DxRole.PROVIDER);

    handler.handle(routingContext);

    ArgumentCaptor<DxRuntimeException> captor = ArgumentCaptor.forClass(DxRuntimeException.class);
    verify(routingContext).fail(captor.capture());
    assertEquals("No access for the given entity type", captor.getValue().getMessage());
  }
}
