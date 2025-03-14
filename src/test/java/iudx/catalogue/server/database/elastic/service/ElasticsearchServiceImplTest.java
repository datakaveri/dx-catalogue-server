package iudx.catalogue.server.database.elastic.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.json.JsonObject;
import io.vertx.core.Future;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import iudx.catalogue.server.database.elastic.ElasticClient;
import iudx.catalogue.server.database.elastic.model.ElasticsearchResponse;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import java.util.List;
import org.mockito.*;
import org.junit.jupiter.api.*;

import java.util.concurrent.CompletableFuture;

public class ElasticsearchServiceImplTest {

  @Mock
  private ElasticClient mockElasticClient;

  @Mock
  private co.elastic.clients.elasticsearch.ElasticsearchAsyncClient mockAsyncClient;

  private ElasticsearchServiceImpl elasticsearchService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(mockElasticClient.getClient()).thenReturn(mockAsyncClient);
    elasticsearchService = new ElasticsearchServiceImpl(mockElasticClient);
  }

  @Test
  void testSearch() {
    QueryModel queryModel = mock(QueryModel.class);
    when(queryModel.toElasticsearchQuery()).thenReturn(mock(Query.class));
    when(queryModel.toSourceConfig()).thenReturn(mock(SourceConfig.class));
    when(mockAsyncClient.search(any(SearchRequest.class), eq(ObjectNode.class)))
        .thenReturn(CompletableFuture.completedFuture(mock(SearchResponse.class)));

    Future<List<ElasticsearchResponse>> result = elasticsearchService.search("test-index", queryModel);
    assertNotNull(result);
    verify(mockAsyncClient).search(any(SearchRequest.class), eq(ObjectNode.class));
  }

  @Test
  void testCreateDocument() {
    JsonObject document = new JsonObject().put("id", "test-id").put("key", "value");
    when(mockAsyncClient.index(any(IndexRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(null));

    Future<JsonObject> result = elasticsearchService.createDocument("test-index", document);
    assertNotNull(result);
    verify(mockAsyncClient).index(any(IndexRequest.class));
  }

  @Test
  void testUpdateDocument() {
    JsonObject document = new JsonObject().put("id", "test-id").put("key", "updated-value");
    when(mockAsyncClient.index(any(IndexRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(null));

    Future<JsonObject> result = elasticsearchService.updateDocument("test-index",
        "dummy-id", document);
    assertNotNull(result);
    verify(mockAsyncClient).index(any(IndexRequest.class));
  }

  @Test
  void testDeleteDocument() {
    when(mockAsyncClient.delete(any(DeleteRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(null));

    Future<JsonObject> result = elasticsearchService.deleteDocument("test-index", "test-id");
    assertNotNull(result);
    verify(mockAsyncClient).delete(any(DeleteRequest.class));
  }
}
