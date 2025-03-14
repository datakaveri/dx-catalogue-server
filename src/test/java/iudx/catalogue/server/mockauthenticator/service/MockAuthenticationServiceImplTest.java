package iudx.catalogue.server.mockauthenticator.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.authenticator.model.JwtAuthenticationInfo;
import jdk.jfr.Description;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static junit.framework.Assert.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class MockAuthenticationServiceImplTest {
    @Mock
    WebClient client;
    @Mock
    Handler<AsyncResult<JsonObject>> handler;
    MockAuthenticationServiceImpl mockAuthenticationService;
    @Test
    @Description("test tokenIntrospect method ")
    public void testTokenInterospect(VertxTestContext vertxTestContext) {
        String authHost="dummy";
        mockAuthenticationService=new MockAuthenticationServiceImpl(client,authHost);
        JwtData request=new JwtData();
        JwtAuthenticationInfo authenticationInfo= new JwtAuthenticationInfo(new JsonObject());
        assertNotNull(mockAuthenticationService.tokenIntrospect(request,authenticationInfo));
        vertxTestContext.completeNow();
    }
    @Test
    @Description("test IsPermittedProviderID method ")
    public void testIsPermittedProviderID(VertxTestContext vertxTestContext) {
        String authHost="dummy";
        mockAuthenticationService=new MockAuthenticationServiceImpl(client,authHost);
        String requestID="abcd/abcd/abcd/abcd";
        String providerID="dummy";
        assertFalse(mockAuthenticationService.isPermittedProviderId(requestID,providerID));
        vertxTestContext.completeNow();
    }
    @Test
    @Description("test IsPermittedMethod method ")
    public void testIsPermittedMethod(VertxTestContext vertxTestContext) {
        String authHost="dummy";
        mockAuthenticationService=new MockAuthenticationServiceImpl(client,authHost);
        JsonArray methods =new JsonArray();
        String providerID="dummy";
        assertFalse(mockAuthenticationService.isPermittedMethod(methods,providerID));
        vertxTestContext.completeNow();
    }
}
