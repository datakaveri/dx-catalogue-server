package iudx.catalogue.server.apiserver.util;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.apiserver.util.Constants.MIME_APPLICATION_JSON;
import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.server.exceptions.FailureHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class FailureHandlerTest {
    @Mock
    RoutingContext routingContext;
    @Mock
    Throwable failure;
    @Mock
    DecodeException decodeException;
    @Mock
    ClassCastException classCastException;
    @Mock
    HttpServerResponse httpServerResponse;
    @Mock
    HttpServerRequest httpServerRequest;
    @Mock
    Future<Void> voidFuture;

    @Test
    @DisplayName("Test handle method with RuntimeException")
    public void testHandle(VertxTestContext vertxTestContext) {
        when(routingContext.failure()).thenReturn(failure);
        when(routingContext.response()).thenReturn(httpServerResponse);
        when(httpServerResponse.setStatusCode(400)).thenReturn(httpServerResponse);
        when(httpServerResponse.putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
        FailureHandler failureHandler = new FailureHandler();
        failureHandler.handle(routingContext);
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test handleDecodeException method with string ROUTE_ITEM ")
    public void testHandleDecodeExceptionRouteItem(VertxTestContext vertxTestContext) {
        String ROUTE_ITEM = "/iudx/cat/v1" + "/item";
        when(routingContext.failure()).thenReturn(failure);
        when(failure.getLocalizedMessage()).thenReturn("dummy msg");
        when(routingContext.request()).thenReturn(httpServerRequest);
        when(httpServerRequest.uri()).thenReturn(ROUTE_ITEM);
        when(routingContext.response()).thenReturn(httpServerResponse);
        when(httpServerResponse.setStatusCode(anyInt())).thenReturn(httpServerResponse);
        when(httpServerResponse.putHeader(anyString(),anyString())).thenReturn(httpServerResponse);
        when(httpServerResponse.end(anyString())).thenReturn(voidFuture);
        FailureHandler failureHandler=new FailureHandler();
        assertEquals(ROUTE_ITEM,routingContext.request().uri());
        failureHandler.handleDecodeException(routingContext);
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test handle method with string ROUTE_SEARCH")
    public void testHandleDecodeExceptionRouteSearch(VertxTestContext vertxTestContext) {

        String ROUTE_SEARCH = "/iudx/cat/v1" + "/search";
        when(routingContext.failure()).thenReturn(failure);
        when(failure.getLocalizedMessage()).thenReturn("dummy msg");
        when(routingContext.request()).thenReturn(httpServerRequest);
        when(httpServerRequest.uri()).thenReturn(ROUTE_SEARCH);
        when(routingContext.response()).thenReturn(httpServerResponse);
        when(httpServerResponse.setStatusCode(400)).thenReturn(httpServerResponse);
        when(httpServerResponse.putHeader(anyString(),anyString())).thenReturn(httpServerResponse);
        when(httpServerResponse.end(anyString())).thenReturn(voidFuture);
        FailureHandler FailureHandler=new FailureHandler();
        assertEquals(ROUTE_SEARCH,routingContext.request().uri());
        FailureHandler.handleDecodeException(routingContext);
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test handleDecodeException method with random string")
    public void testHandleDecodeExceptionRandom(VertxTestContext vertxTestContext) {

        String ROUTE_SEARCH = "random";
        when(routingContext.failure()).thenReturn(failure);
        when(failure.getLocalizedMessage()).thenReturn("dummy msg");
        when(routingContext.request()).thenReturn(httpServerRequest);
        when(httpServerRequest.uri()).thenReturn(ROUTE_SEARCH);
        when(routingContext.response()).thenReturn(httpServerResponse);
        when(httpServerResponse.setStatusCode(400)).thenReturn(httpServerResponse);
        when(httpServerResponse.putHeader(anyString(),anyString())).thenReturn(httpServerResponse);
        when(httpServerResponse.end(anyString())).thenReturn(voidFuture);
        FailureHandler failureHandler=new FailureHandler();
        assertEquals("random",routingContext.request().uri());
        failureHandler.handleDecodeException(routingContext);
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test handleClassCastException method")
    public void testHandleClassCastException(VertxTestContext vertxTestContext) {
        when(routingContext.failure()).thenReturn(failure);
        when(failure.getLocalizedMessage()).thenReturn("dummy msg");
        when(routingContext.response()).thenReturn(httpServerResponse);
        when(httpServerResponse.setStatusCode(400)).thenReturn(httpServerResponse);
        when(httpServerResponse.putHeader(anyString(),anyString())).thenReturn(httpServerResponse);
        when(httpServerResponse.end(anyString())).thenReturn(voidFuture);
        assertEquals("dummy msg",routingContext.failure().getLocalizedMessage());
        FailureHandler FailureHandler=new FailureHandler();
        FailureHandler.handleClassCastException(routingContext);
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test handleClassCastException method")
    public void testHandleClassCastException2(VertxTestContext vertxTestContext) {
        when(routingContext.failure()).thenReturn(failure);
        when(failure.getLocalizedMessage()).thenReturn("dummy msg");
        when(routingContext.request()).thenReturn(httpServerRequest);
        when(httpServerRequest.uri()).thenReturn(ROUTE_RATING);
        when(routingContext.response()).thenReturn(httpServerResponse);
        when(httpServerResponse.setStatusCode(400)).thenReturn(httpServerResponse);
        when(httpServerResponse.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)).thenReturn(httpServerResponse);
        when(httpServerResponse.end(anyString())).thenReturn(voidFuture);
        FailureHandler failureHandler=new FailureHandler();
        assertEquals(ROUTE_RATING,routingContext.request().uri());
        failureHandler.handleDecodeException(routingContext);
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test handleClassCastException method")
    public void testHandleDecodeException(VertxTestContext vertxTestContext) {
        FailureHandler failureHandler=new FailureHandler();
        when(routingContext.failure()).thenReturn(decodeException);
        when(routingContext.response()).thenReturn(httpServerResponse);
        when(httpServerResponse.setStatusCode(400)).thenReturn(httpServerResponse);
        when(httpServerResponse.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)).thenReturn(httpServerResponse);
        when(httpServerResponse.end(anyString())).thenReturn(voidFuture);
        failureHandler.handle(routingContext);
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test handleClassCastException method")
    public void testClassCastException(VertxTestContext vertxTestContext) {
        FailureHandler failureHandler=new FailureHandler();
        when(routingContext.failure()).thenReturn(classCastException);
        when(routingContext.response()).thenReturn(httpServerResponse);
        when(httpServerResponse.setStatusCode(400)).thenReturn(httpServerResponse);
        when(httpServerResponse.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)).thenReturn(httpServerResponse);
        when(httpServerResponse.end(anyString())).thenReturn(voidFuture);
        failureHandler.handle(routingContext);
        vertxTestContext.completeNow();
    }
}

