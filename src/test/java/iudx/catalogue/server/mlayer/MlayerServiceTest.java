package iudx.catalogue.server.mlayer;

import static iudx.catalogue.server.common.util.ResponseBuilderUtil.successfulItemOperationResp;
import static iudx.catalogue.server.database.elastic.util.Constants.DESCRIPTION_ATTR;
import static iudx.catalogue.server.database.elastic.util.Constants.KEY;
import static iudx.catalogue.server.database.elastic.util.Constants.SUMMARY_KEY;
import static iudx.catalogue.server.database.elastic.util.Constants.WORD_VECTOR_KEY;
import static iudx.catalogue.server.mlayer.util.Constants.DOMAIN_ID;
import static iudx.catalogue.server.mlayer.util.Constants.INSTANCE_ID;
import static iudx.catalogue.server.util.Constants.BUCKETS;
import static iudx.catalogue.server.util.Constants.ICON_BASE64;
import static iudx.catalogue.server.util.Constants.ID;
import static iudx.catalogue.server.util.Constants.INSTANCE;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_COS;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_PROVIDER;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_RESOURCE_GROUP;
import static iudx.catalogue.server.util.Constants.LIMIT;
import static iudx.catalogue.server.util.Constants.OFFSET;
import static iudx.catalogue.server.util.Constants.PROVIDER;
import static iudx.catalogue.server.util.Constants.RESULTS;
import static iudx.catalogue.server.util.Constants.VALUE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.server.database.elastic.model.ElasticsearchResponse;
import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
import iudx.catalogue.server.database.postgres.service.PostgresService;
import iudx.catalogue.server.mlayer.service.MlayerServiceImpl;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class MlayerServiceTest {
  private static final ElasticsearchService esService = mock(ElasticsearchService.class);
  private static final Logger LOGGER = LogManager.getLogger(MlayerServiceTest.class);
  private static final String tableName = "database Table";
  private static final String catSummaryTable = "cat_summary";
  private static final JsonArray jsonArray =
      new JsonArray().add("excluded_ids").add("excluded_ids2");
  static MlayerServiceImpl mlayerService;
  @Mock static WebClient webClient;
  @Mock static PostgresService postgresService;
  @Mock private static AsyncResult<JsonObject> asyncResult;
  JsonObject jsonObject =
      new JsonObject()
          .put("databaseTable", tableName)
          .put("catSummaryTable", catSummaryTable)
          .put("excluded_ids", jsonArray);
  @Mock JsonObject json;

  private JsonObject requestJson() {
    return new JsonObject()
        .put("name", "pune")
        .put("cover", "path of cover.jpg")
        .put("icon", "path of icon.jpg")
        .put("logo", "path og logo.jpg");
  }

  @Test
  @DisplayName("Success: test create mlayer instance")
  void successfulMlayerInstanceCreationTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);

    json = requestJson();
    when(esService.search(any(), any())).thenReturn(Future.succeededFuture(List.of()));
    when(esService.createDocument(any(), any()))
        .thenReturn(
            Future.succeededFuture(successfulItemOperationResp(json, "Success: Item created")));
    mlayerService
        .createMlayerInstance(json)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                verify(esService, times(3)).createDocument(any(), any());
                testContext.completeNow();
              } else {
                LOGGER.debug("Fail");
                testContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Failure: test create mlayer instance")
  void failureMlayerInstanceCreationTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);
    JsonObject request = requestJson();
    when(esService.search(any(), any())).thenReturn(Future.succeededFuture(List.of()));
    when(esService.createDocument(any(), any())).thenReturn(Future.failedFuture("Failed;"));

    mlayerService
        .createMlayerInstance(request)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                verify(esService, times(1)).createDocument(any(), any());
                LOGGER.debug("Fail");
                testContext.failNow(handler.cause());
              } else {
                testContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("Success: test get all mlayer instances")
  void successfulMlayerInstanceGetTest(VertxTestContext testContext) {
    JsonObject requestParams = new JsonObject(); // Example request parameters

    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);

    List<ElasticsearchResponse> searchResults =
        List.of(
            new ElasticsearchResponse("doc1", new JsonObject().put("name", "Instance1")),
            new ElasticsearchResponse("doc2", new JsonObject().put("name", "Instance2")));

    // Stubbing the search method to return a successful response
    when(esService.search(any(), any())).thenReturn(Future.succeededFuture(searchResults));

    mlayerService
        .getMlayerInstance(requestParams)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                verify(esService, times(3)).search(any(), any());
                testContext.completeNow();
              } else {
                LOGGER.debug("Fail");
                testContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Failure: test get all mlayer instances")
  void failureMlayerInstanceGetTest(VertxTestContext testContext) {
    JsonObject requestParams = new JsonObject();

    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);

    when(esService.search(any(), any()))
        .thenReturn(Future.failedFuture("Failed to fetch instances"));

    mlayerService
        .getMlayerInstance(requestParams)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOGGER.debug("Fail");
                testContext.failNow("Expected failure but got success");
              } else {
                testContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("Success: test delete mlayer instance")
  void successfulMlayerInstanceDeleteTest(VertxTestContext testContext) {
    String request = "dummy";
    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);

    when(esService.search(any(), any()))
        .thenReturn(
            Future.succeededFuture(
                List.of(
                    new ElasticsearchResponse("docId", new JsonObject().put("id", "dummy-uuid")))));
    when(esService.deleteDocument(any(), any()))
        .thenReturn(Future.succeededFuture(new JsonObject().put("status", "deleted")));

    mlayerService
        .deleteMlayerInstance(request)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                verify(esService, times(2)).deleteDocument(any(), any());
                testContext.completeNow();
              } else {
                LOGGER.debug("Fail");
                testContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Failure: test delete mlayer instance")
  void failureMlayerInstanceDeleteTest(VertxTestContext testContext) {
    String request = "dummy";
    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);
    when(esService.search(any(), any()))
        .thenReturn(
            Future.succeededFuture(
                List.of(
                    new ElasticsearchResponse("docId", new JsonObject().put("id", "dummy-uuid")))));
    when(esService.deleteDocument(any(), any())).thenReturn(Future.failedFuture("Deletion failed"));

    mlayerService
        .deleteMlayerInstance(request)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                verify(esService, times(1)).deleteDocument(any(), any());
                LOGGER.debug("Fail");
                testContext.failNow(handler.cause());
              } else {
                testContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("Success: test update mlayer instance")
  void successfulMlayerInstanceUpdateTest(VertxTestContext testContext) {
    String id = "instanceId";
    JsonObject request = new JsonObject().put("name", "instance name").put(INSTANCE_ID, id);

    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);

    when(esService.updateDocument(any(), any(), any()))
        .thenReturn(Future.succeededFuture(new JsonObject().put("updated", true)));
    when(esService.search(any(), any()))
        .thenReturn(
            Future.succeededFuture(
                List.of(
                    new ElasticsearchResponse(
                        "docId",
                        new JsonObject().put("id", "dummy-uuid").put("name", "instance name")))));

    mlayerService
        .updateMlayerInstance(request)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                verify(esService, times(2)).updateDocument(any(), any(), any());
                testContext.completeNow();
              } else {
                LOGGER.debug("Fail");
                testContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Failure: test update mlayer instance")
  void failureMlayerInstanceUpdateTest(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject().put("name", "instance name").put(INSTANCE_ID, "mlayer_id");
    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);

    when(esService.updateDocument(any(), any(), any()))
        .thenReturn(Future.failedFuture("Update failed"));
    when(esService.search(any(), any()))
        .thenReturn(
            Future.succeededFuture(
                List.of(
                    new ElasticsearchResponse(
                        "docId",
                        new JsonObject().put("id", "dummy-uuid").put("name", "instance name")))));

    mlayerService
        .updateMlayerInstance(request)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOGGER.debug("Unexpected Success");
                testContext.failNow(new Throwable("Test should fail but succeeded"));
              } else {
                verify(esService, times(3)).updateDocument(any(), any(), any());
                LOGGER.debug("Expected Failure");
                testContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("Success: test create mlayer domain")
  void successMlayerDomainCreateTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("name", "dummy");

    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);

    when(esService.createDocument(any(), any()))
        .thenReturn(Future.succeededFuture(new JsonObject().put("result", "created")));
    when(esService.search(any(), any())).thenReturn(Future.succeededFuture(List.of()));

    mlayerService
        .createMlayerDomain(request)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                verify(esService, times(1)).createDocument(any(), any());
                testContext.completeNow();
              } else {
                LOGGER.debug("Unexpected Failure");
                testContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Failure: test create mlayer domain")
  void failureMlayerDomainCreateTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("name", "dummy");

    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);

    when(esService.createDocument(any(), any()))
        .thenReturn(Future.failedFuture(new RuntimeException("Elasticsearch creation failed")));
    when(esService.search(any(), any())).thenReturn(Future.succeededFuture(List.of()));

    mlayerService
        .createMlayerDomain(request)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOGGER.debug("Unexpected Success");
                testContext.failNow(new AssertionError("Expected failure but got success"));
              } else {
                verify(esService, times(4)).createDocument(any(), any());
                testContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("Success: test update mlayer domain")
  void successMlayerDomainUpdateTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("name", "dummy").put(DOMAIN_ID, "domainId");

    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);

    when(esService.updateDocument(any(), any(), any()))
        .thenReturn(Future.succeededFuture(new JsonObject().put("status", "updated")));
    when(esService.search(any(), any()))
        .thenReturn(
            Future.succeededFuture(
                List.of(
                    new ElasticsearchResponse(
                        "docId",
                        new JsonObject().put(DOMAIN_ID, "domainId").put("name", "dummy")))));

    mlayerService
        .updateMlayerDomain(request)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                verify(esService, times(4)).updateDocument(any(), any(), any());
                testContext.completeNow();
              } else {
                LOGGER.debug("Unexpected failure");
                testContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Failure: test update mlayer domain")
  void failureMlayerDomainUpdateTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("name", "instance name").put(DOMAIN_ID, "domainId");

    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);

    // Mocking the updateDocument method to simulate a failure
    when(esService.updateDocument(any(), any(), any()))
        .thenReturn(Future.failedFuture("Update failed"));
    when(esService.search(any(), any()))
        .thenReturn(
            Future.succeededFuture(
                List.of(
                    new ElasticsearchResponse(
                        "docId",
                        new JsonObject()
                            .put("name", "instance " + "name")
                            .put(DOMAIN_ID, "domainId")))));

    mlayerService
        .updateMlayerDomain(request)
        .onComplete(
            handler -> {
              verify(esService, times(1)).updateDocument(any(), any(), any());

              if (handler.succeeded()) {
                LOGGER.debug("Unexpected Success");
                testContext.failNow(new AssertionError("Expected failure but got success"));
              } else {
                LOGGER.debug("Expected failure, completing test");
                testContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("Success: test delete mlayer domain")
  void successfulMlayerDomainDeleteTest(VertxTestContext testContext) {
    String documentId = "dummy"; // ID of the document to be deleted
    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);

    when(esService.deleteDocument(any(), any()))
        .thenReturn(Future.succeededFuture(new JsonObject().put("result", "deleted")));
    when(esService.search(any(), any()))
        .thenReturn(
            Future.succeededFuture(
                List.of(new ElasticsearchResponse("docId", new JsonObject().put(ID, documentId)))));

    mlayerService
        .deleteMlayerDomain(documentId)
        .onComplete(
            handler -> {
              verify(esService, times(4)).deleteDocument(any(), any());

              if (handler.succeeded()) {
                testContext.completeNow();
              } else {
                LOGGER.debug("Fail");
                testContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Failure: test delete mlayer domain")
  void failureMlayerDomainDeleteTest(VertxTestContext testContext) {
    String documentId = "docId"; // ID of the document to be deleted

    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);

    when(esService.search(any(), any()))
        .thenReturn(
            Future.succeededFuture(
                List.of(new ElasticsearchResponse("docId", new JsonObject().put(ID, documentId)))));
    when(esService.deleteDocument(any(), any())).thenReturn(Future.failedFuture("Deletion failed"));

    mlayerService
        .deleteMlayerDomain(documentId)
        .onComplete(
            handler -> {
              verify(esService, times(1)).deleteDocument(any(), any());

              if (handler.succeeded()) {
                LOGGER.debug("Unexpected Success");
                testContext.failNow(new AssertionError("Expected failure but got success"));
              } else {
                testContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("Success: test get all mlayer domain")
  void successfulMlayerDomainGetTest(VertxTestContext testContext) {
    JsonObject requestParams = new JsonObject(); // Query parameters

    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);

    List<ElasticsearchResponse> mockResponse =
        List.of(
            new ElasticsearchResponse("id1", new JsonObject().put("name", "Domain1")),
            new ElasticsearchResponse("id2", new JsonObject().put("name", "Domain2")));

    when(esService.search(any(), any())).thenReturn(Future.succeededFuture(mockResponse));

    mlayerService
        .getMlayerDomain(requestParams)
        .onComplete(
            handler -> {
              verify(esService, times(5)).search(any(), any());

              if (handler.succeeded()) {
                testContext.completeNow();
              } else {
                LOGGER.debug("Fail");
                testContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Failure: test get all mlayer domain")
  void failureMlayerDomainGetTest(VertxTestContext testContext) {
    JsonObject requestParams = new JsonObject(); // Query parameters
    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);

    when(esService.search(any(), any()))
        .thenReturn(Future.failedFuture("Elasticsearch query failed"));

    mlayerService
        .getMlayerDomain(requestParams)
        .onComplete(
            handler -> {
              verify(esService, times(36)).search(any(), any());

              if (handler.succeeded()) {
                LOGGER.debug("Unexpected Success");
                testContext.failNow(new AssertionError("Expected failure but got success"));
              } else {
                LOGGER.debug("Expected failure, completing test");
                testContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("Success: test get all mlayer providers")
  void successfulMlayerProvidersGetTest(VertxTestContext testContext) {

    JsonObject requestParams = new JsonObject(); // Query parameters

    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);

    when(esService.search(any(), any()))
        .thenReturn(
            Future.succeededFuture(
                List.of(
                    new ElasticsearchResponse("docId", new JsonObject().put("id", "dummy-uuid")))));

    mlayerService
        .getMlayerProviders(requestParams)
        .onComplete(
            handler -> {
              verify(esService, times(19)).search(any(), any());

              if (handler.succeeded()) {
                testContext.completeNow();
              } else {
                LOGGER.debug("Fail");
                testContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Failure: test get all mlayer providers")
  void failureMlayerProvidersGetTest(VertxTestContext testContext) {

    JsonObject requestParams = new JsonObject(); // Query parameters

    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);

    when(esService.search(any(), any())).thenReturn(Future.failedFuture("Search failed"));

    mlayerService
        .getMlayerProviders(requestParams)
        .onComplete(
            handler -> {
              verify(esService, times(27)).search(any(), any());

              if (handler.succeeded()) {
                LOGGER.debug("Fail");
                testContext.failNow(new AssertionError("Expected failure but got success"));
              } else {
                testContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("Success: test get dataset location and label")
  void successfulMlayerGeoQueryGetTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);

    JsonObject request =
        new JsonObject().put(ID, new JsonArray().add("dummy-uuid")).put(INSTANCE, "instance");

    // Mock ElasticsearchResponse
    ElasticsearchResponse mockResponse =
        new ElasticsearchResponse("docId", new JsonObject().put("status", "success"));

    when(esService.search(any(), any())).thenReturn(Future.succeededFuture(List.of(mockResponse)));

    mlayerService
        .getMlayerGeoQuery(request)
        .onComplete(
            handler -> {
              verify(esService, times(25)).search(any(), any());

              if (handler.succeeded()) {
                testContext.completeNow();
              } else {
                LOGGER.debug("Fail");
                testContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Failure: test get dataset location and label")
  void failureMlayerGeoQueryGetTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);

    JsonObject request =
        new JsonObject().put(ID, new JsonArray().add("dummy-uuid")).put(INSTANCE, "instance");

    // Stubbing search method to return a failure
    when(esService.search(any(), any())).thenReturn(Future.failedFuture("Geo query failed"));

    mlayerService
        .getMlayerGeoQuery(request)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                verify(esService, times(1)).search(any(), any());
                LOGGER.debug("Fail");
                testContext.failNow(handler.cause());
              } else {
                testContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("Success: test get all datasets")
  void successfulGetMlayerAllDatasetsTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);

    JsonObject request = new JsonObject().put(LIMIT, 0).put(OFFSET, 0);
    List<ElasticsearchResponse> mockResponse =
        List.of(
            new ElasticsearchResponse(
                "doc1",
                new JsonObject()
                    .put("name", "name")
                    .put("icon", "icon.png")
                    .put("type", new JsonArray().add("iudx:Owner"))));
    JsonArray buckets =
        new JsonArray()
            .add(
                0,
                new JsonObject()
                    .put(KEY, "key")
                    .put("doc_count", 1)
                    .put(
                        "access_policies",
                        new JsonObject()
                            .put(
                                "buckets",
                                new JsonArray()
                                    .add(
                                        0, new JsonObject().put(KEY, "key").put("doc_count", 1)))));

    // Stubbing search method to return a successful future with mock data
    // 1. Mock dataset search result
    JsonObject dataset1 =
        new JsonObject()
            .put("type", new JsonArray().add(ITEM_TYPE_COS))
            .put("id", "dataset1")
            .put("instance", "instance1");
    JsonObject dataset2 =
        new JsonObject()
            .put("id", "dummy-uuid")
            .put(PROVIDER, "dummy-uuid")
            .put("instance", "instance2")
            .put(DESCRIPTION_ATTR, "dummy description")
            .put(ICON_BASE64, "icon_base64.png")
            .put("type", new JsonArray().add(ITEM_TYPE_PROVIDER));
    JsonObject dataset3 =
        new JsonObject()
            .put("id", "dummy-uuid")
            .put(PROVIDER, "dummy-uuid")
            .put("instance", "instance3")
            .put(DESCRIPTION_ATTR, "dummy description")
            .put(ICON_BASE64, "icon_base64.png")
            .put("type", new JsonArray().add(ITEM_TYPE_RESOURCE_GROUP));

    // 2. Mock instance search result
    JsonObject instanceResponse =
        new JsonObject()
            .put("name", "Agra")
            .put("icon", "agra.png")
            .put("name", "Bangalore")
            .put("icon", "bangalore.png");

    // 3. Mock resource policy count result
    JsonObject resourcePolicyResponse =
        new JsonObject()
            .put("resourceItemCount", new JsonObject().put("dataset1", 5).put("dataset2", 10))
            .put(
                "resourceAccessPolicy",
                new JsonObject()
                    .put("dataset1", new JsonObject().put("PII", 1).put("SECURE", 2).put("OPEN", 3))
                    .put(
                        "dataset2",
                        new JsonObject().put("PII", 0).put("SECURE", 1).put("OPEN", 4)));

    // Stub esService.search() calls
    when(esService.search(any(), any()))
        .thenReturn(
            Future.succeededFuture(
                List.of(
                    new ElasticsearchResponse("ds1", dataset1),
                    new ElasticsearchResponse("ds2", dataset2),
                    new ElasticsearchResponse("ds3", dataset3)))) // For `gettingAllDatasets()`
        .thenReturn(
            Future.succeededFuture(
                List.of(
                    new ElasticsearchResponse(
                        "docId2", instanceResponse)))) // For `allMlayerInstance()`
        .thenReturn(
            Future.succeededFuture(
                List.of(
                    new ElasticsearchResponse(
                        "docId3",
                        resourcePolicyResponse)))) // For `gettingResourceAccessPolicyCount()`
        .thenReturn(Future.succeededFuture(mockResponse));

    JsonObject aggs = new JsonObject().put(RESULTS, new JsonObject().put(BUCKETS, buckets));
    ElasticsearchResponse.setAggregations(aggs);

    mlayerService
        .getMlayerAllDatasets(request)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                verify(esService, times(31)).search(any(), any());
                testContext.completeNow();
              } else {
                LOGGER.debug("Fail");
                testContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Failure: test get all datasets")
  void failureMlayerAllDatasetsTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);

    JsonObject request = new JsonObject(); // Example request parameters

    // Stubbing search method to return a failed future
    when(esService.search(any(), any()))
        .thenReturn(Future.failedFuture(new RuntimeException("Elasticsearch query failed")));

    mlayerService
        .getMlayerAllDatasets(request)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                verify(esService, times(41)).search(any(), any());
                testContext.completeNow(); // Test should pass on failure
              } else {
                LOGGER.debug("Unexpected Success");
                testContext.failNow(new Throwable("Expected failure but got success"));
              }
            });
  }

  @Test
  @DisplayName("Success: test get dataset and its resources details")
  void successMlayerDatasetAndResourcesTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);

    JsonObject request = new JsonObject().put("id", "dummy-id");

    // First search response (Fetching dataset details)
    List<ElasticsearchResponse> initialResponse =
        List.of(
            new ElasticsearchResponse(
                "doc1", new JsonObject().put("cos", "cos-id").put("provider", "provider-id")));

    // Second search response (Fetching dataset resources)
    List<ElasticsearchResponse> datasetResponse =
        List.of(
            new ElasticsearchResponse(
                "doc2",
                new JsonObject()
                    .put("id", "resource-group-id")
                    .put("type", new JsonArray().add("iudx:ResourceGroup"))
                    .put("description", "Sample dataset description")),
            new ElasticsearchResponse(
                "doc3",
                new JsonObject()
                    .put("id", "provider-id")
                    .put("type", new JsonArray().add("iudx:Provider"))
                    .put("resourceServerRegURL", "https://example.com")
                    .put(ICON_BASE64, "sampleIcon")),
            new ElasticsearchResponse(
                "doc4",
                new JsonObject()
                    .put("id", "resource-id")
                    .put("type", new JsonArray().add("iudx:Resource"))
                    .put("@context", "https://example-schema.com")
                    .put("resourceType", "sample-type")));

    // Stubbing Elasticsearch calls
    when(esService.search(any(), any()))
        .thenReturn(Future.succeededFuture(initialResponse)) // First query response
        .thenReturn(Future.succeededFuture(datasetResponse)); // Second query response

    mlayerService
        .getMlayerDataset(request)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                JsonObject response = handler.result();
                assertNotNull(response);
                assertTrue(response.containsKey("results"));
                JsonArray results = response.getJsonArray("results");
                assertFalse(results.isEmpty());

                JsonObject dataset = results.getJsonObject(0).getJsonObject("dataset");
                JsonArray resources = results.getJsonObject(0).getJsonArray("resource");
                LOGGER.debug("Resource: " + resources);
                LOGGER.debug("Dataset: " + dataset);

                assertEquals("resource-group-id", dataset.getString("id"));
                assertEquals("Sample dataset description", dataset.getString("description"));
                assertEquals("provider-id", dataset.getJsonObject("provider").getString("id"));
                assertEquals(
                    "sampleIcon", dataset.getJsonObject("provider").getString(ICON_BASE64));
                assertEquals("https://example.com", dataset.getString("resourceServerRegURL"));
                assertEquals(1, resources.size());
                assertEquals("sample-type", resources.getJsonObject(0).getString("resourceType"));

                verify(esService, times(24)).search(any(), any());
                testContext.completeNow();
              } else {
                LOGGER.error("Unexpected Failure", handler.cause());
                testContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Failure: test get dataset and its resources details")
  void failureMlayerDatasetAndResourcesTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);
    JsonObject request = new JsonObject().put("id", "dummy id");

    // Mocking a failed search response
    when(esService.search(any(), any()))
        .thenReturn(Future.failedFuture(new RuntimeException("Search failed")));

    mlayerService
        .getMlayerDataset(request)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOGGER.debug("Fail: Expected failure but got success");
                testContext.failNow(new AssertionError("Expected failure but got success"));
              } else {
                verify(esService, times(35)).search(any(), any());
                testContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("Success: test get dataset details")
  void successMlayerDatasetTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);

    JsonArray tags = new JsonArray().add("flood");
    JsonArray providers = new JsonArray().add("26005f3b-a6a0-4edb-ae28-70474b4ef90c");
    JsonObject request =
        new JsonObject()
            .put("instance", "pune")
            .put("tags", tags)
            .put("providers", providers)
            .put("domains", tags)
            .put(LIMIT, 0)
            .put(OFFSET, 0);

    List<ElasticsearchResponse> mockResponse =
        List.of(
            new ElasticsearchResponse(
                "doc1",
                new JsonObject()
                    .put("name", "name")
                    .put("icon", "icon.png")
                    .put("type", new JsonArray().add("iudx:Owner"))));
    JsonArray buckets =
        new JsonArray()
            .add(
                0,
                new JsonObject()
                    .put(KEY, "key")
                    .put("doc_count", 1)
                    .put(
                        "access_policies",
                        new JsonObject()
                            .put(
                                "buckets",
                                new JsonArray()
                                    .add(
                                        0, new JsonObject().put(KEY, "key").put("doc_count", 1)))));

    // Stubbing search method to return a successful future with mock data
    // 1. Mock dataset search result
    JsonObject dataset1 =
        new JsonObject()
            .put("type", new JsonArray().add(ITEM_TYPE_COS))
            .put("id", "dataset1")
            .put("instance", "instance1");
    JsonObject dataset2 =
        new JsonObject()
            .put("id", "dummy-uuid")
            .put(PROVIDER, "dummy-uuid")
            .put("instance", "instance2")
            .put(DESCRIPTION_ATTR, "dummy description")
            .put(ICON_BASE64, "icon_base64.png")
            .put("type", new JsonArray().add(ITEM_TYPE_PROVIDER));
    JsonObject dataset3 =
        new JsonObject()
            .put("id", "dummy-uuid")
            .put(PROVIDER, "dummy-uuid")
            .put("instance", "instance3")
            .put(DESCRIPTION_ATTR, "dummy description")
            .put(ICON_BASE64, "icon_base64.png")
            .put("type", new JsonArray().add(ITEM_TYPE_RESOURCE_GROUP));

    // 2. Mock instance search result
    JsonObject instanceResponse =
        new JsonObject()
            .put("name", "Agra")
            .put("icon", "agra.png")
            .put("name", "Bangalore")
            .put("icon", "bangalore.png");

    // 3. Mock resource policy count result
    JsonObject resourcePolicyResponse =
        new JsonObject()
            .put("resourceItemCount", new JsonObject().put("dataset1", 5).put("dataset2", 10))
            .put(
                "resourceAccessPolicy",
                new JsonObject()
                    .put("dataset1", new JsonObject().put("PII", 1).put("SECURE", 2).put("OPEN", 3))
                    .put(
                        "dataset2",
                        new JsonObject().put("PII", 0).put("SECURE", 1).put("OPEN", 4)));

    // Stub esService.search() calls
    when(esService.search(any(), any()))
        .thenReturn(
            Future.succeededFuture(
                List.of(
                    new ElasticsearchResponse("ds1", dataset1),
                    new ElasticsearchResponse("ds2", dataset2),
                    new ElasticsearchResponse("ds3", dataset3)))) // For `gettingAllDatasets()`
        .thenReturn(
            Future.succeededFuture(
                List.of(
                    new ElasticsearchResponse(
                        "docId2", instanceResponse)))) // For `allMlayerInstance()`
        .thenReturn(
            Future.succeededFuture(
                List.of(
                    new ElasticsearchResponse(
                        "docId3",
                        resourcePolicyResponse)))) // For `gettingResourceAccessPolicyCount()`
        .thenReturn(Future.succeededFuture(mockResponse));

    JsonObject aggs = new JsonObject().put(RESULTS, new JsonObject().put(BUCKETS, buckets));
    ElasticsearchResponse.setAggregations(aggs);

    mlayerService
        .getMlayerDataset(request)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                verify(esService, times(18)).search(any(), any());
                testContext.completeNow();
              } else {
                LOGGER.debug("Fail");
                testContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Failure: test get dataset details due to invalid parameters")
  void failureMlayerDatasetInvalidParamTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);

    JsonObject request =
        new JsonObject()
            .put("instances", "pune"); // Incorrect key ("instances" instead of "instance")

    mlayerService
        .getMlayerDataset(request)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOGGER.debug("Fail");
                testContext.failNow(new Throwable("Expected failure but test succeeded"));
              } else {
                testContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("Failure: test get dataset details")
  void failureMlayerDatasetTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);

    JsonArray tags = new JsonArray().add("flood");
    JsonArray providers = new JsonArray().add("providerId");
    JsonObject request =
        new JsonObject()
            .put("instance", "dummy value")
            .put("tags", tags)
            .put("providers", providers);

    when(esService.search(any(), any()))
        .thenReturn(Future.failedFuture("Dataset retrieval failed"));

    mlayerService
        .getMlayerDataset(request)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOGGER.debug("Fail");
                testContext.failNow(new Throwable("Expected failure but test succeeded"));
              } else {
                testContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("Success: test get overview detail")
  void successfulGetMlayerOverviewTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);

    JsonArray results =
        new JsonArray()
            .add(0, new JsonObject().put("resource_group", "rg-1"))
            .add(1, new JsonObject().put("resource_group", "rg-2"));
    JsonObject json =
        new JsonObject()
            .put("results", results); // Fixed possible typo
    String instanceName = "dummy";
    // Mock Elasticsearch responses
    JsonObject instanceResult =
        new JsonObject().put("icon", "icon1").put("name", "pune").put(SUMMARY_KEY, "summary").put(WORD_VECTOR_KEY, "");

    JsonObject domainResult1 = new JsonObject().put("domain", "geo").put(SUMMARY_KEY, "summary").put(WORD_VECTOR_KEY, "");
    JsonObject domainResult2 = new JsonObject().put("domain", "health").put(SUMMARY_KEY, "summary").put(WORD_VECTOR_KEY, "");
    List<ElasticsearchResponse> datasetResult = List.of(
        // Resource Group 1 (with totalResources and accessPolicy)
        new ElasticsearchResponse("1", new JsonObject()
            .put("id", "resourceGroup1")
            .put("description", "Resource Group 1 Description")
            .put("type", new JsonArray().add("iudx:ResourceGroup"))
            .put("provider", "provider1")
            .put("itemCreatedAt", "2024-01-07T03:51:39+0530")
            .put("instance", "instance1")
            .put("label", "Label1")
            .put(SUMMARY_KEY, "summary").put(WORD_VECTOR_KEY, "")
        ),
        // Resource Group 2 (without access policy, should get default accessPolicy)
        new ElasticsearchResponse("2", new JsonObject()
            .put("id", "resourceGroup2")
            .put("description", "Resource Group 2 Description")
            .put("type", new JsonArray().add("iudx:ResourceGroup"))
            .put("provider", "provider2")
            .put("itemCreatedAt", "2024-01-07T03:51:39+0530")
            .put("instance", "instance1")
            .put("label", "Label2")
            .put(SUMMARY_KEY, "summary").put(WORD_VECTOR_KEY, "")
        ),
        // Provider 1
        new ElasticsearchResponse("3", new JsonObject()
            .put("id", "provider1")
            .put("description", "Provider 1 Description")
            .put("type", new JsonArray().add("iudx:Provider"))
            .put(SUMMARY_KEY, "summary").put(WORD_VECTOR_KEY, "")
        ),
        // Provider 2
        new ElasticsearchResponse("4", new JsonObject()
            .put("id", "provider2")
            .put("description", "Provider 2 Description")
            .put("type", new JsonArray().add("iudx:Provider"))
            .put(SUMMARY_KEY, "summary").put(WORD_VECTOR_KEY, "")
        )
    );
    JsonArray buckets =
        new JsonArray()
            .add(
                0,
                new JsonObject()
                    .put(KEY, "key")
                    .put("doc_count", 1)
                    .put(
                        "access_policies",
                        new JsonObject()
                            .put(
                                "buckets",
                                new JsonArray()
                                    .add(
                                        0, new JsonObject().put(KEY, "key").put("doc_count", 1)))));

    // Mock Elasticsearch service response
    when(esService.search(any(), any()))
        .thenReturn(Future.succeededFuture(List.of(new ElasticsearchResponse("dummy-docId", instanceResult))))
        .thenReturn(Future.succeededFuture(datasetResult))
        .thenReturn(Future.succeededFuture(List.of(new ElasticsearchResponse("dummy-docId3",
            domainResult1), new ElasticsearchResponse("docId", domainResult2))));

    // Mock Postgres service response
    when(postgresService.executeQuery(any())).thenReturn(Future.succeededFuture(json));
    ElasticsearchResponse.setAggregations(
        new JsonObject()
            .put("provider_count", new JsonObject().put(VALUE, 1))
            .put(RESULTS, new JsonObject().put(BUCKETS, buckets)));

    // Call the method
    mlayerService
        .getMlayerPopularDatasets(instanceName)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                verify(esService, times(10)).search(any(), any());
                verify(postgresService, times(1)).executeQuery(any());
                testContext.completeNow();
              } else {
                LOGGER.debug("Fail");
                testContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Fail: test get overview detail when postgres query fails")
  void failedPostgresQueryTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);
    String instance = "dummy";

    // Simulate Postgres failure
    when(postgresService.executeQuery(any())).thenReturn(Future.failedFuture("Database error"));

    // Call the method
    mlayerService
        .getMlayerPopularDatasets(instance)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                verify(postgresService, times(1)).executeQuery(any());
                testContext.completeNow();
              } else {
                LOGGER.debug("Fail");
                testContext.failNow(new Throwable("Expected failure, but test passed"));
              }
            });
  }

  @Test
  @DisplayName("Success: Get Summary Count Api")
  public void successGetTotalCountApi(VertxTestContext vertxTestContext) {
    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);

    JsonArray jsonArray = new JsonArray();
    JsonObject json = new JsonObject();
    json.put("results", jsonArray);
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("counts", 122343243);
    jsonArray.add(jsonObject);
    when(postgresService.executeQuery(any())).thenReturn(Future.succeededFuture(json));

    mlayerService
        .getSummaryCountSizeApi()
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(handler.result(), json);
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Fail: Get Summary Count Api")
  void failGetTotalCountApi(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);
    when(postgresService.executeQuery(any())).thenReturn(Future.failedFuture("Fail;"));

    mlayerService
        .getSummaryCountSizeApi()
        .onComplete(
            handler -> {
              if (handler.failed()) {
                verify(postgresService, times(1)).executeQuery(any());
                testContext.completeNow();
              } else {
                testContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Success: Get  Count Size Api")
  public void successGetCountSizeApi(VertxTestContext vertxTestContext) {
    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);
    JsonArray jsonArray = new JsonArray();
    JsonObject json = new JsonObject();
    json.put("results", jsonArray);
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("month", "december");
    jsonObject.put("year", 2023);
    jsonObject.put("counts", 456);
    jsonObject.put("total_size", 122343243);
    jsonArray.add(jsonObject);
    when(postgresService.executeQuery(any())).thenReturn(Future.succeededFuture(json));

    mlayerService
        .getRealTimeDataSetApi()
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(handler.result(), json);
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Fail: Get Count Size Api")
  void failGetCountSizeApi(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);
    when(postgresService.executeQuery(any())).thenReturn(Future.failedFuture("Fail;"));

    mlayerService
        .getRealTimeDataSetApi()
        .onComplete(
            handler -> {
              if (handler.failed()) {
                verify(postgresService, times(1)).executeQuery(any());
                testContext.completeNow();
              } else {
                testContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Success: Get Count Size Api")
  public void successGetCountSizeApi2(VertxTestContext vertxTestContext) {
    jsonObject.put("excluded_ids", new JsonArray());
    mlayerService = new MlayerServiceImpl(webClient, esService, postgresService, jsonObject);

    JsonArray jsonArray = new JsonArray();
    JsonObject json = new JsonObject();
    json.put("results", jsonArray);
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("month", "december");
    jsonObject.put("year", 2023);
    jsonObject.put("counts", 456);
    jsonObject.put("total_size", 122343243);
    jsonArray.add(jsonObject);

    when(postgresService.executeQuery(any())).thenReturn(Future.succeededFuture(json));

    mlayerService
        .getRealTimeDataSetApi()
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(handler.result(), json);
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }
}
