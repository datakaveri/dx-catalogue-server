package iudx.catalogue.server.database.elastic.service;

import static iudx.catalogue.server.common.util.ResponseBuilderUtil.successResp;
import static iudx.catalogue.server.common.util.ResponseBuilderUtil.successfulItemOperationResp;
import static iudx.catalogue.server.util.Constants.AGGREGATIONS;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.JsonpMapperFeatures;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.elastic.ElasticClient;
import iudx.catalogue.server.database.elastic.model.ElasticsearchResponse;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import jakarta.json.stream.JsonGenerator;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ElasticsearchServiceImpl implements ElasticsearchService {
  private static final Logger LOGGER = LogManager.getLogger(ElasticsearchServiceImpl.class);

  static ElasticClient client;
  private static ElasticsearchAsyncClient asyncClient;

  public ElasticsearchServiceImpl(ElasticClient client) {
    ElasticsearchServiceImpl.client = client;
    asyncClient = client.getClient();
  }

  @Override
  public Future<List<ElasticsearchResponse>> search(String index, QueryModel queryModel) {
    // Convert QueryModel into Elasticsearch Query and aggregations
    Map<String, Aggregation> elasticsearchAggregations = new HashMap<>();
    if (queryModel.getAggregations() != null && !queryModel.getAggregations().isEmpty()) {
      queryModel.getAggregations().forEach(aggregation ->
          elasticsearchAggregations.put(
              aggregation.getAggregationName(),
              aggregation.toElasticsearchAggregations()
          )
      );
    }
    QueryModel queries = queryModel.getQueries();
    Query query = queries == null ? null : queries.toElasticsearchQuery();
    String size = queryModel.getLimit();
    String from = queryModel.getOffset();
    SearchRequest.Builder requestBuilder = new SearchRequest.Builder()
        .index(index);

    // Optional parameters, set only if not null
    if (query != null) {
      requestBuilder.query(query);
    }
    if (size != null) {
      requestBuilder.size(Integer.valueOf(size));
    }
    if (from != null) {
      requestBuilder.from(Integer.valueOf(from));
    }
    if (!elasticsearchAggregations.isEmpty()) {
      requestBuilder.aggregations(elasticsearchAggregations);
    }
    SourceConfig sourceConfig = queryModel.toSourceConfig();
    if (sourceConfig != null) {
      requestBuilder.source(sourceConfig);
    }
    List<SortOptions> sortOptions = queryModel.toSortOptions();
    if (sortOptions != null) {
      requestBuilder.sort(sortOptions);
    }
    SearchRequest request = requestBuilder.build();
    LOGGER.debug("Final SearchRequest: {}", request);

    return executeSearch(request)
        .map(this::convertToElasticSearchResponse);
  }

  @Override
  public Future<Integer> count(String index, QueryModel queryModel) {
    // Convert QueryModel into Elasticsearch Query
    Query query = queryModel.getQueries() == null ? null :
        queryModel.getQueries().toElasticsearchQuery();

    // Create a CountRequest.Builder for the count query
    CountRequest.Builder requestBuilder = new CountRequest.Builder()
        .index(index);

    // Add query if present
    if (query != null) {
      requestBuilder.query(query);
    }

    CountRequest request = requestBuilder.build();
    LOGGER.debug("Final CountRequest: {}", request);

    // Execute the count query
    return executeCount(request);
  }

  private Future<Integer> executeCount(CountRequest request) {
    Promise<Integer> promise = Promise.promise();

    // Execute the count request asynchronously
    asyncClient.count(request).whenComplete((response, error) -> {
      if (error != null) {
        LOGGER.error("Count operation failed: {}", error.getMessage());
        promise.fail(error);
      } else {
        // Return the total count of documents matching the query
        Integer count = Math.toIntExact(response.count());
        LOGGER.debug("Total document count: {}", count);
        promise.complete(count);
      }
    });

    return promise.future();
  }


  @Override
  public Future<JsonObject> createDocument(String index, JsonObject document) {
    Promise<JsonObject> promise = Promise.promise();
    IndexRequest<JsonObject> createRequest = IndexRequest.of(i -> i
        .index(index)
        .withJson(new StringReader(document.toString())));
    LOGGER.debug("Create Request: " + createRequest);
    asyncClient.index(createRequest).whenComplete((response, error) -> {
      if (error != null) {
        LOGGER.error("Create operation failed: {}", error.getMessage());
        promise.fail(error);
      } else {
        promise.complete(successfulItemOperationResp(document,
            "Success: Item created"));
      }
    });

    return promise.future();
  }

  @Override
  public Future<JsonObject> updateDocument(String index, String id, JsonObject document) {
    IndexRequest<JsonObject> updateRequest = IndexRequest.of(i -> i
        .index(index)
        .id(id)
        .withJson(new StringReader(document.toString()))
    );
    LOGGER.debug("Update Request: " + updateRequest);
    Promise<JsonObject> promise = Promise.promise();

    asyncClient.index(updateRequest).whenComplete((response, error) -> {
      if (error != null) {
        LOGGER.error("Update operation failed: {}", error.getMessage());
        promise.fail(error);
      } else {
        promise.complete(successfulItemOperationResp(document,
            "Success: Item updated successfully"));
      }
    });

    return promise.future();
  }


  @Override
  public Future<JsonObject> patchDocument(String index, String id, JsonObject document) {
    UpdateRequest<JsonObject, JsonObject> patchRequest = UpdateRequest.of(u -> u
        .index(index)
        .id(id)
        .withJson(new StringReader(document.toString()))
    );
    LOGGER.debug("Patch Request: " + patchRequest);
    Promise<JsonObject> promise = Promise.promise();

    asyncClient.update(patchRequest, JsonObject.class).whenComplete((response, error) -> {
      if (error != null) {
        LOGGER.error("Patch operation failed: {}", error.getMessage());
        promise.fail(error);
      } else {
        promise.complete(successfulItemOperationResp(document,
            "Success: Item updated successfully"));
      }
    });

    return promise.future();
  }


  @Override
  public Future<JsonObject> deleteDocument(String index, String id) {
    DeleteRequest deleteRequest = DeleteRequest.of(d -> d
        .index(index)
        .id(id));
    LOGGER.debug("Delete Request: " + deleteRequest);
    Promise<JsonObject> promise = Promise.promise();

    asyncClient.delete(deleteRequest).whenComplete((response, error) -> {
      if (error != null) {
        LOGGER.error("Delete operation failed: {}", error.getMessage());
        promise.fail(error);
      } else {
        promise.complete(successResp(id, "Success: Item deleted successfully"));
      }
    });

    return promise.future();
  }


  private List<ElasticsearchResponse> convertToElasticSearchResponse(
      SearchResponse<ObjectNode> response) {
    long totalHits = response.hits().total() != null ? response.hits().total().value() : 0;
    LOGGER.debug("Total Hits in Elasticsearch Response: " + totalHits);

    // Parse hits
    List<ElasticsearchResponse> responses = response.hits().hits().stream()
        .map(hit -> {
          String id = hit.id();
          JsonObject source =
              hit.source() != null ? JsonObject.mapFrom(hit.source()) : new JsonObject();
          return new ElasticsearchResponse(id, source);
        })
        .collect(Collectors.toList());

    // Parse aggregations if present
    if (response.aggregations() != null && !response.aggregations().isEmpty()) {
      JsonpMapper mapper =
          asyncClient._jsonpMapper().withAttribute(JsonpMapperFeatures.SERIALIZE_TYPED_KEYS, false);
      StringWriter writer = new StringWriter();
      try (JsonGenerator generator = mapper.jsonProvider().createGenerator(writer)) {
        mapper.serialize(response, generator);
      } catch (Exception e) {
        LOGGER.error("Error serializing aggregations: ", e);
        throw new RuntimeException("Failed to process aggregations", e);
      }
      String result = writer.toString();

      // Parse the aggregations object from the serialized result
      JsonObject aggs = new JsonObject(result).getJsonObject(AGGREGATIONS);
      ElasticsearchResponse.setAggregations(aggs);
    }

    return responses;
  }


  private Future<SearchResponse<ObjectNode>> executeSearch(SearchRequest request) {
    Promise<SearchResponse<ObjectNode>> promise = Promise.promise();
    asyncClient.search(request, ObjectNode.class).whenComplete((response, error) -> {
      if (error != null) {
        LOGGER.error("Search operation failed due to {}: {}", error.getClass().getSimpleName(),
            error.getMessage(), error);

        promise.fail(error);
      } else {
        promise.complete(response);
      }
    });
    return promise.future();
  }
}
