package iudx.catalogue.server.apiserver.stack;

import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.util.Constants.DATABASE_ERROR;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.server.apiserver.stack.service.StacService;
import iudx.catalogue.server.apiserver.stack.service.StacServiceImpl;
import iudx.catalogue.server.apiserver.stack.util.StackConstants;
import iudx.catalogue.server.common.RespBuilder;
import iudx.catalogue.server.database.elastic.model.ElasticsearchResponse;
import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jdk.jfr.Description;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
class StackServiceImplTest {
  static StacService stackSevice;
  private static ElasticsearchService mockElasticsearchService;
  @Mock private static RespBuilder mockRespBuilder;
  @Mock JsonObject mockJson;
  String notFoundERRor =
      new RespBuilder()
          .withType(TYPE_ITEM_NOT_FOUND)
          .withTitle(TITLE_ITEM_NOT_FOUND)
          .withDetail("Fail: Stac doesn't exist")
          .getResponse();
  String dbError =
      new RespBuilder()
          .withType(FAILED)
          .withResult("stackId", REQUEST_GET, FAILED)
          .withDetail(DATABASE_ERROR)
          .getResponse();

  @BeforeAll
  @DisplayName("Deploying Verticle")
  static void startVertx(Vertx vertx, VertxTestContext testContext) {
    mockElasticsearchService = mock(ElasticsearchService.class);
    mockRespBuilder = mock(RespBuilder.class);
    // stackSevice = new StackServiceImpl(mockElasticClient, "index");
    testContext.completeNow();
  }

  @Test
  @Description("Success: get() stack")
  public void testGetStack4Success(VertxTestContext vertxTestContext) {
    // JsonObject sampleResult = new JsonObject().put("totalHits", 1).put("value", "value");
    List<ElasticsearchResponse> sampleResultList = new ArrayList<>();
    ElasticsearchResponse sampleResult = new ElasticsearchResponse();
    sampleResult.setDocId("dummy-docId");
    sampleResult.setSource(new JsonObject().put("id", "dummy-uuid"));
    sampleResultList.add(sampleResult);
    // Stubbing the searchAsync method with thenAnswer
    when(mockElasticsearchService.search(anyString(), any()))
        .thenReturn(Future.succeededFuture(sampleResultList));

    StacServiceImpl stackService = new StacServiceImpl(mockElasticsearchService, "Index");
    stackService.respBuilder = mockRespBuilder;

    Future<JsonObject> resultFuture = stackService.get("uuid");

    assertTrue(resultFuture.succeeded());
    assertEquals("dummy-uuid",
        resultFuture.result().getJsonArray(RESULTS).getJsonObject(0).getString(ID));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("Failed: get() stack not found")
  public void testGetStack4NotFound(VertxTestContext vertxTestContext) {
    List<ElasticsearchResponse> emptyResult = new ArrayList<>();

    when(mockElasticsearchService.search(anyString(), any()))
        .thenReturn(Future.succeededFuture(emptyResult));

    StacServiceImpl stackService = new StacServiceImpl(mockElasticsearchService, "index");
    stackService.respBuilder = mockRespBuilder;

    Future<JsonObject> resultFuture = stackService.get("stackId");

    assertTrue(resultFuture.failed());
    assertEquals(notFoundERRor, resultFuture.cause().getMessage());
    vertxTestContext.completeNow();
  }

  @Test
  @Description("Failed: get() Db error")
  public void testGetStack4DbError(VertxTestContext vertxTestContext) {
    // Stubbing the searchAsync method with thenReturn
    when(mockElasticsearchService.search(anyString(), any()))
        .thenReturn(Future.failedFuture("sampleResult"));

    StacServiceImpl stackService = new StacServiceImpl(mockElasticsearchService, "index");
    stackService.respBuilder = mockRespBuilder;

    Future<JsonObject> resultFuture = stackService.get("stackId");

    assertTrue(resultFuture.failed());
    assertEquals(dbError, resultFuture.cause().getMessage());
    vertxTestContext.completeNow();
  }

  @Test
  @Description("Success: create() stack creation")
  void testCreateSuccess(VertxTestContext testContext) {
    List<ElasticsearchResponse> emptyResult = new ArrayList<>();

    when(mockElasticsearchService.search(anyString(), any()))
        .thenReturn(Future.succeededFuture(emptyResult));

    when(mockElasticsearchService.createDocument(anyString(), any()))
        .thenReturn(Future.succeededFuture(new JsonObject().put("id", "generatedId")));

    StacServiceImpl stackService = new StacServiceImpl(mockElasticsearchService, "Index");

    JsonObject self = new JsonObject().put("href", "dummy_href").put("rel", "self");
    JsonObject root = new JsonObject().put("href", "dummy_href").put("rel", "root");
    JsonArray links = new JsonArray().add(self).add(root);
    when(mockJson.getJsonArray(anyString())).thenReturn(links);

    Future<JsonObject> resultFuture = stackService.create(mockJson);

    resultFuture.onComplete(
        handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result();
            assertEquals(SUCCESS, result.getString(TYPE));
            assertEquals(STAC_CREATION_SUCCESS, result.getString(DETAIL));
            testContext.completeNow();
          } else {
            testContext.failNow("failed: " + handler.cause().getMessage());
          }
        });
  }

  @Test
  @Description("Failed: docPostAsync() failure during stack creation")
  void testCreate4SFailureDbError(VertxTestContext testContext) {
    List<ElasticsearchResponse> emptyResult = new ArrayList<>();

    when(mockElasticsearchService.search(anyString(), any()))
        .thenReturn(Future.succeededFuture(emptyResult));

    when(mockElasticsearchService.createDocument(anyString(), any()))
        .thenReturn(Future.failedFuture("Db Error"));

    StacServiceImpl stackService = new StacServiceImpl(mockElasticsearchService, "Index");

    JsonObject self = new JsonObject().put("href", "dummy_href").put("rel", "self");
    JsonObject root = new JsonObject().put("href", "dummy_href").put("rel", "root");
    JsonArray links = new JsonArray().add(self).add(root);
    when(mockJson.getJsonArray(anyString())).thenReturn(links);

    Future<JsonObject> resultFuture = stackService.create(mockJson);

    resultFuture.onComplete(
        handler -> {
          if (handler.failed()) {
            JsonObject result = new JsonObject(handler.cause().getMessage());
            assertEquals("DB Error. Check logs for more information", result.getString(DETAIL));
            testContext.completeNow();
          } else {
            testContext.failNow("failed: ");
          }
        });
  }

  @Test
  @Description("Failed: conflicts during stack creation")
  void testCreate4ConflictSFailure(VertxTestContext testContext) {
    List<ElasticsearchResponse> nonEmptyResult = new ArrayList<>();
    ElasticsearchResponse existingDoc = new ElasticsearchResponse();
    existingDoc.setDocId("existing-docId");
    nonEmptyResult.add(existingDoc);

    when(mockElasticsearchService.search(anyString(), any()))
        .thenReturn(Future.succeededFuture(nonEmptyResult));

    StacServiceImpl stackService = new StacServiceImpl(mockElasticsearchService, "Index");

    JsonObject self = new JsonObject().put("href", "dummy_href").put("rel", "self");
    JsonObject root = new JsonObject().put("href", "dummy_href").put("rel", "root");
    JsonArray links = new JsonArray().add(self).add(root);
    when(mockJson.getJsonArray(anyString())).thenReturn(links);

    Future<JsonObject> resultFuture = stackService.create(mockJson);

    resultFuture.onComplete(
        handler -> {
          if (handler.failed()) {
            JsonObject result = new JsonObject(handler.cause().getMessage());
            assertEquals("urn:dx:cat:Conflicts", result.getString(TYPE));
            testContext.completeNow();
          } else {
            testContext.failNow("failed: ");
          }
        });
  }

  @Test
  @Description("Failed: Db Error during searchAsync while stack creation")
  void testCreate4DbErrorFailure(VertxTestContext testContext) {
    when(mockElasticsearchService.search(anyString(), any()))
        .thenReturn(Future.failedFuture("Db Error during search"));

    StacServiceImpl stackService = new StacServiceImpl(mockElasticsearchService, "Index");

    JsonObject self = new JsonObject().put("href", "dummy_href").put("rel", "self");
    JsonObject root = new JsonObject().put("href", "dummy_href").put("rel", "root");
    JsonArray links = new JsonArray().add(self).add(root);
    when(mockJson.getJsonArray(anyString())).thenReturn(links);

    Future<JsonObject> resultFuture = stackService.create(mockJson);

    resultFuture.onComplete(
        handler -> {
          if (handler.failed()) {
            String errorMessage = handler.cause().getMessage();
            assertEquals(
                errorMessage,
                new RespBuilder()
                    .withType(FAILED)
                    .withResult("stac", INSERT, FAILED)
                    .withDetail(DATABASE_ERROR)
                    .getResponse());
            testContext.completeNow();
          } else {
            testContext.failNow("failed: ");
          }
        });
  }

  @Test
  @Description("Success: stack [patch]")
  void testUpdate(VertxTestContext testContext) {
    stackSevice = new StacServiceImpl(mockElasticsearchService, "index");

    // Mocking data
    JsonObject stack = new JsonObject().put("id", "someId").put("rel", "child").put("href", "href");

    JsonArray links =
        new JsonArray().add(new JsonObject().put("rel", "child").put("href", "someHref"));

    JsonObject json = new JsonObject().put("links", links);

    ElasticsearchResponse existResult = new ElasticsearchResponse();
    existResult.setDocId("dummyDocId");
    existResult.setSource(
        new JsonObject().put("links", links));

    // Stubbing the search method to return the existing document
    when(mockElasticsearchService.search(anyString(), any()))
        .thenReturn(Future.succeededFuture(List.of(existResult)));

    // Stubbing the updateDocument method to return a successful update response
    when(mockElasticsearchService.patchDocument(anyString(), anyString(), any()))
        .thenReturn(Future.succeededFuture(new JsonObject().put("key", "value")));

    // Testing the update method
    Future<JsonObject> updateFuture = stackSevice.update(stack);

    updateFuture.onComplete(
        handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result();
            assertEquals(TYPE_SUCCESS, result.getString(TYPE));
            assertEquals(TITLE_SUCCESS, result.getString(TITLE));
            testContext.completeNow();
          } else {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Description("Conflict: stack [patch]")
  void testUpdate4ExistingNotAllowed(VertxTestContext testContext) {
    stackSevice = new StacServiceImpl(mockElasticsearchService, "index");

    // Mocking data
    JsonObject stack = new JsonObject().put("id", "someId").put("rel", "child").put("href", "href");

    JsonArray links = new JsonArray().add(new JsonObject().put("rel", "child").put("href", "href"));

    ElasticsearchResponse existResult = new ElasticsearchResponse();
    existResult.setDocId("dummyDocId");
    existResult.setSource(new JsonObject().put("id", "someId").put("links", links));

    // Stubbing the search method to return an existing document
    when(mockElasticsearchService.search(anyString(), any()))
        .thenReturn(Future.succeededFuture(List.of(existResult)));

    // Testing the update method
    Future<JsonObject> updateFuture = stackSevice.update(stack);

    updateFuture.onComplete(
        handler -> {
          if (handler.succeeded()) {
            testContext.failNow("Failed: " + handler.result());
          } else {
            System.out.println(handler.cause().getMessage());
            JsonObject errorResponse = new JsonObject(handler.cause().getMessage());
            assertEquals(TYPE_CONFLICT, errorResponse.getString(TYPE));
            assertEquals(TITLE_ALREADY_EXISTS, errorResponse.getString(TITLE));
            testContext.completeNow();
          }
        });
  }

  @Test
  @Description("NotFound: stack [patch]")
  void testUpdate4ItemNotFound(VertxTestContext testContext) {
    stackSevice = new StacServiceImpl(mockElasticsearchService, "index");

    // Mocking data
    JsonObject stack = new JsonObject().put("id", "someId").put("rel", "child").put("href", "href");

    // Stubbing the search method to return an empty result (Item Not Found)
    when(mockElasticsearchService.search(anyString(), any()))
        .thenReturn(Future.succeededFuture(Collections.emptyList()));

    // Testing the update method
    Future<JsonObject> updateFuture = stackSevice.update(stack);

    updateFuture.onComplete(
        handler -> {
          if (handler.succeeded()) {
            testContext.failNow("Failed: " + handler.result());
          } else {
            JsonObject errorResponse = new JsonObject(handler.cause().getMessage());
            assertEquals(TYPE_ITEM_NOT_FOUND, errorResponse.getString(TYPE));
            assertEquals(TITLE_ITEM_NOT_FOUND, errorResponse.getString(TITLE));
            testContext.completeNow();
          }
        });
  }

  @Test
  @Description("Success: stack deletion")
  void testDelete4SuccessfulDeletion(VertxTestContext testContext) {
    String stackId = "someId";
    ElasticsearchResponse existingDocument = new ElasticsearchResponse();
    existingDocument.setDocId("docId");
    existingDocument.setSource(new JsonObject().put("id", stackId));

    // Stubbing the search method to return an existing document
    when(mockElasticsearchService.search(anyString(), any()))
        .thenReturn(Future.succeededFuture(List.of(existingDocument)));

    // Stubbing the deleteDocumentAsync method to return success
    when(mockElasticsearchService.deleteDocument(anyString(), anyString()))
        .thenReturn(Future.succeededFuture());

    stackSevice = new StacServiceImpl(mockElasticsearchService, "index");

    // Testing the delete method
    Future<JsonObject> deleteFuture = stackSevice.delete(stackId);

    deleteFuture.onComplete(
        handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result();
            assertEquals(STAC_DELETION_SUCCESS, result.getString(DETAIL));
            assertEquals(SUCCESS, result.getString("type"));
            testContext.completeNow();
          } else {
            testContext.failNow("Failed: " + handler.cause().getMessage());
          }
        });
  }

  @Test
  @Description("Failed: stack deletion")
  void testDelete4DeletionFailure(VertxTestContext testContext) {
    String stackId = "someId";

    // Stubbing the search method to return an empty result (no matching document found)
    when(mockElasticsearchService.search(anyString(), any()))
        .thenReturn(Future.succeededFuture(Collections.emptyList()));

    stackSevice = new StacServiceImpl(mockElasticsearchService, "index");

    // Testing the delete method
    Future<JsonObject> deleteFuture = stackSevice.delete(stackId);

    deleteFuture.onComplete(
        handler -> {
          if (handler.succeeded()) {
            testContext.failNow("Deletion should have failed, but it succeeded.");
          } else {
            String errorMessage = handler.cause().getMessage();
            JsonObject result = new JsonObject(errorMessage);
            assertEquals(TYPE_ITEM_NOT_FOUND, result.getString(TYPE));
            assertEquals("Item not found, can't delete", result.getString(DETAIL));
            testContext.completeNow();
          }
        });
  }

  @Test
  @Description("Failed: stack deletion due to db error")
  void testDelete4FailedDeletionDbError(VertxTestContext testContext) {
    String stackId = "someId";

    ElasticsearchResponse existResult = new ElasticsearchResponse();
    existResult.setDocId("docId");
    existResult.setSource(
        new JsonObject().put("results", new JsonArray().add(new JsonObject().put("id", "someId"))));

    // Stubbing the search method to return a valid existing result
    when(mockElasticsearchService.search(anyString(), any()))
        .thenReturn(Future.succeededFuture(List.of(existResult)));

    // Stubbing the docDelAsync method to simulate a database error
    when(mockElasticsearchService.deleteDocument(anyString(), any()))
        .thenReturn(Future.failedFuture("Failed: db error"));

    stackSevice = new StacServiceImpl(mockElasticsearchService, "index");

    // Testing the delete method
    Future<JsonObject> deleteFuture = stackSevice.delete(stackId);

    deleteFuture.onComplete(
        handler -> {
          if (handler.succeeded()) {
            testContext.failNow("Deletion should have failed, but it succeeded.");
          } else {
            String errorMessage = handler.cause().getMessage();
            assertEquals(
                errorMessage,
                new RespBuilder()
                    .withType(FAILED)
                    .withResult(stackId, "DELETE", FAILED)
                    .withDetail(DATABASE_ERROR)
                    .getResponse());
            testContext.completeNow();
          }
        });
  }

  @Test
  @Description("Failed: stack deletion search() failure")
  void testDelete4DeletionAsyncFailure(VertxTestContext testContext) {
    String stackId = "someId";

    // Stubbing the search method to simulate an async failure
    when(mockElasticsearchService.search(anyString(), any()))
        .thenReturn(Future.failedFuture("failed: async failure"));

    stackSevice = new StacServiceImpl(mockElasticsearchService, "index");

    // Testing the delete method
    Future<JsonObject> deleteFuture = stackSevice.delete(stackId);

    deleteFuture.onComplete(
        handler -> {
          if (handler.succeeded()) {
            testContext.failNow("Deletion should have failed, but it succeeded.");
          } else {
            String errorMessage = handler.cause().getMessage();
            assertTrue(errorMessage.contains("urn:dx:cat:ItemNotFound"));
            testContext.completeNow();
          }
        });
  }

  @Test
  @Description("Failed: stack deletion Json decode() failure")
  void testDelete4DeletionJsonDecodeFailure(VertxTestContext vertxTestContext) {
    String stackId = "someId";
    ElasticsearchResponse existResult = new ElasticsearchResponse();
    existResult.setDocId("docId");
    existResult.setSource(
        new JsonObject().put("results", new JsonArray().add(new JsonObject().put("id", "someId"))));

    // Stubbing the search method to return a valid existing result
    when(mockElasticsearchService.search(anyString(), any()))
        .thenReturn(Future.succeededFuture(List.of(existResult)));
    when(mockElasticsearchService.deleteDocument(anyString(), anyString()))
        .thenReturn(Future.failedFuture("Failed to delete"));

    stackSevice = new StacServiceImpl(mockElasticsearchService, "index");

    // Testing the delete method
    Future<JsonObject> deleteFuture = stackSevice.delete(stackId);

    deleteFuture.onComplete(
        handler -> {
          if (handler.succeeded()) {
            vertxTestContext.failNow("Failed: {}" + handler.cause().getMessage());

          } else {
            String errorMessage = handler.cause().getMessage();
            JsonObject result = new JsonObject(errorMessage);
            assertEquals(FAILED, result.getString(TYPE));
            assertEquals("DB Error. Check logs for more information", result.getString(DETAIL));
            vertxTestContext.completeNow();
          }
        });
  }
}
