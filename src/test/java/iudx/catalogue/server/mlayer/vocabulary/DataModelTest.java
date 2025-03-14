//package iudx.catalogue.server.mlayer.vocabulary;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//import io.vertx.core.*;
//import io.vertx.core.buffer.Buffer;
//import io.vertx.core.json.JsonArray;
//import io.vertx.core.json.JsonObject;
//import io.vertx.ext.web.client.HttpRequest;
//import io.vertx.ext.web.client.HttpResponse;
//import io.vertx.ext.web.client.WebClient;
//import io.vertx.junit5.VertxExtension;
//import io.vertx.junit5.VertxTestContext;
//import iudx.catalogue.server.database.elastic.model.ElasticsearchResponse;
//import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.atomic.AtomicInteger;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.*;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//@ExtendWith(MockitoExtension.class)
//@ExtendWith(VertxExtension.class)
//public class DataModelTest {
//  private static final Logger LOGGER = LogManager.getLogger(DataModelTest.class);
//  MultiMap mockHeaders = mock(MultiMap.class);
//  @Mock private WebClient webClient;
//  private ElasticsearchService esService;
//  @Mock private HttpResponse<Buffer> mockHttpResponse;
//  @Mock private HttpRequest<Buffer> mockHttpRequest;
//  @Mock private Buffer mockBuffer;
//  @InjectMocks private DataModel dataModel;
//
//  @BeforeEach
//  void setUp() {
//    MockitoAnnotations.openMocks(this);
//    esService = mock(ElasticsearchService.class);
//    dataModel = new DataModel(webClient, esService, "test-index");
//  }
//
//  @Test
//  void SuccessGetDataModelInfoTest(VertxTestContext vertxTestContext) {
//    JsonObject respone =
//        new JsonObject()
//            .put(
//                "results",
//                new JsonArray()
//                    .add(
//                        new JsonObject()
//                            .put("@context", "https://example.com/")
//                            .put("type", new JsonArray().add("dummy").add("iudx:Resource"))));
//    // Mocking Elasticsearch client's async search
//    List<ElasticsearchResponse> searchResults = List.of(new ElasticsearchResponse());
//    // Mocking search method of esService
//    doAnswer(invocation -> Future.succeededFuture(searchResults))
//        .when(esService)
//        .search(any(), any());
//
//    lenient().when(webClient.getAbs(anyString())).thenReturn(mockHttpRequest);
//    lenient().when(mockHttpRequest.send()).thenReturn(Future.succeededFuture(mockHttpResponse));
//    lenient()
//        .when(mockHttpResponse.body())
//        .thenReturn(
//            Buffer.buffer(
//                "{ \"@graph\": [ { \"@id\": \"iudx:Resource\", \"rdfs:subClassOf\": { \"@id\": \"iudx:SuperClass\" } } ] }"));
//
//    Future<JsonObject> resultFuture = dataModel.getDataModelInfo();
//
//    resultFuture.onComplete(
//        ar -> {
//          if (ar.succeeded()) {
//            JsonObject result = ar.result();
//            assertNotNull(result);
//            assertEquals("SuperClass", result.getString("Resource"));
//          } else {
//            fail("Test failed with exception: " + ar.cause().getMessage());
//            vertxTestContext.failNow(ar.cause());
//          }
//        });
//  }
//
//  @Test
//  void SuccessHandleDataModelResponseTest(VertxTestContext vertxTestContext) {
//    Promise<JsonObject> promise = Promise.promise();
//    JsonObject response =
//        new JsonObject()
//            .put(
//                "@graph",
//                new JsonArray()
//                    .add(
//                        new JsonObject()
//                            .put("@id", "iudx:Class")
//                            .put("rdfs:subClassOf", new JsonObject().put("@id", "iudx:SubClass"))));
//    JsonObject classIdToSubClassMap = new JsonObject();
//    AtomicInteger pendingRequests = new AtomicInteger(1);
//
//    // Mocking AsyncResult for successful response
//    AsyncResult<HttpResponse<Buffer>> dmAr = mock(AsyncResult.class);
//    when(dmAr.succeeded()).thenReturn(true);
//    when(dmAr.result()).thenReturn(mockHttpResponse);
//    when(mockHttpResponse.body()).thenReturn(mockBuffer);
//    when(mockHttpResponse.headers()).thenReturn(mockHeaders);
//    when(mockHttpResponse.headers().get(anyString())).thenReturn("application/json");
//    when(mockBuffer.toJsonObject()).thenReturn(response);
//    Map<String, String> idToClassIdMap = new HashMap<>();
//    idToClassIdMap.put("id", "Class");
//    dataModel.handleDataModelResponse(
//        dmAr, "Class", idToClassIdMap, classIdToSubClassMap, pendingRequests, promise, "dmUrl");
//
//    // Verify the result
//    promise
//        .future()
//        .onComplete(
//            ar -> {
//              if (ar.succeeded()) {
//                JsonObject result = ar.result();
//                assertNotNull(result);
//                assertEquals("SubClass", result.getString("id"));
//                vertxTestContext.completeNow();
//              } else {
//                fail("Test failed with exception: " + ar.cause().getMessage());
//                vertxTestContext.failNow(ar.cause());
//              }
//            });
//  }
//
//  @Test
//  void FailureHandleDataModelResponseTest(VertxTestContext vertxTestContext) {
//    Promise<JsonObject> promise = Promise.promise();
//    JsonObject classIdToSubClassMap = new JsonObject();
//    AtomicInteger pendingRequests = new AtomicInteger(1);
//
//    // Mocking AsyncResult for failed response
//    AsyncResult<HttpResponse<Buffer>> dmAr = mock(AsyncResult.class);
//    when(dmAr.succeeded()).thenReturn(false);
//    when(dmAr.cause()).thenReturn(new Throwable("Failed"));
//    Map<String, String> idToClassIdMap = new HashMap<>();
//    idToClassIdMap.put("id", "classId1");
//    dataModel.handleDataModelResponse(
//        dmAr, "id", idToClassIdMap, classIdToSubClassMap, pendingRequests, promise, "dmUrl");
//
//    // Verify the result
//    promise
//        .future()
//        .onComplete(
//            ar -> {
//              if (ar.succeeded()) {
//                JsonObject result = ar.result();
//                assertNotNull(result);
//                assertTrue(result.isEmpty());
//                vertxTestContext.completeNow();
//              } else {
//                fail("Test failed with exception: " + ar.cause().getMessage());
//                vertxTestContext.failNow(ar.cause());
//              }
//            });
//  }
//}
