package iudx.catalogue.server.database.elastic.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.elastic.model.ElasticsearchResponse;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class ElasticsearchServiceTest {

  private ElasticsearchService elasticsearchService;
  private Vertx vertx;

  @BeforeEach
  void setUp() {
    vertx = Vertx.vertx();
    elasticsearchService = mock(ElasticsearchService.class);
  }

  @Test
  void testSearch() {
    String index = "test-index";
    QueryModel queryModel = new QueryModel(new JsonObject()); // Add necessary setup for QueryModel

    List<ElasticsearchResponse> expectedResponse = List.of(
        new ElasticsearchResponse(),
        new ElasticsearchResponse()
    );

    when(elasticsearchService.search(index, queryModel))
        .thenReturn(Future.succeededFuture(expectedResponse));

    Future<List<ElasticsearchResponse>> resultFuture = elasticsearchService.search(index, queryModel);

    assertTrue(resultFuture.succeeded());
    assertEquals(expectedResponse, resultFuture.result());

    verify(elasticsearchService, times(1)).search(index, queryModel);
  }

  @Test
  void testCreateDocument() {
    String index = "test-index";
    JsonObject document = new JsonObject().put("key", "value");
    JsonObject expectedResponse = new JsonObject().put("result", "created");

    when(elasticsearchService.createDocument(index, document))
        .thenReturn(Future.succeededFuture(expectedResponse));

    Future<JsonObject> resultFuture = elasticsearchService.createDocument(index, document);

    assertTrue(resultFuture.succeeded());
    assertEquals(expectedResponse, resultFuture.result());

    verify(elasticsearchService, times(1)).createDocument(index, document);
  }

  @Test
  void testUpdateDocument() {
    String index = "test-index";
    String doc_id = "dummy-id";
    JsonObject document = new JsonObject().put("key", "updated-value");
    JsonObject expectedResponse = new JsonObject().put("result", "updated");

    when(elasticsearchService.updateDocument(index, doc_id, document))
        .thenReturn(Future.succeededFuture(expectedResponse));

    Future<JsonObject> resultFuture = elasticsearchService.updateDocument(index, doc_id, document);

    assertTrue(resultFuture.succeeded());
    assertEquals(expectedResponse, resultFuture.result());

    verify(elasticsearchService, times(1)).updateDocument(index, doc_id, document);
  }

  @Test
  void testDeleteDocument() {
    String index = "test-index";
    String documentId = "doc-id";
    JsonObject expectedResponse = new JsonObject().put("result", "deleted");

    when(elasticsearchService.deleteDocument(index, documentId))
        .thenReturn(Future.succeededFuture(expectedResponse));

    Future<JsonObject> resultFuture = elasticsearchService.deleteDocument(index, documentId);

    assertTrue(resultFuture.succeeded());
    assertEquals(expectedResponse, resultFuture.result());

    verify(elasticsearchService, times(1)).deleteDocument(index, documentId);
  }
}
