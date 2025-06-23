package iudx.catalogue.server.database;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.geocoding.util.Constants.*;
import static iudx.catalogue.server.geocoding.util.Constants.BBOX;
import static iudx.catalogue.server.geocoding.util.Constants.RESULTS;
import static iudx.catalogue.server.geocoding.util.Constants.TYPE;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.validator.Constants.VALIDATION_FAILURE_MSG;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;



public final class ElasticClient {

  private static final Logger LOGGER = LogManager.getLogger(ElasticClient.class);
  private final RestClient client;
  private String index;

  /**
   * ElasticClient - Wrapper around ElasticSearch low level client.
   *
   * @param databaseIp IP of the DB
   * @param databasePort Port
   * @TODO XPack Security
   */
  public ElasticClient(String databaseIp, int databasePort, String index,
                        String databaseUser, String databasePassword) {
    CredentialsProvider credentials = new BasicCredentialsProvider();
    credentials.setCredentials(AuthScope.ANY,
                                new UsernamePasswordCredentials(databaseUser, databasePassword));
    client = RestClient.builder(new HttpHost(databaseIp, databasePort)).setHttpClientConfigCallback(
        httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentials)).build();
    this.index = index;
  }

  /**
   * searchAsync - private function which perform performRequestAsync for search apis.
   *
   * @param request Elastic Request
   * @param options SOURCE - Source only
   *                DOCIDS - DOCIDs only
   *                IDS - IDs only
   * @TODO XPack Security
   */

  private Future<JsonObject> searchAsync(Request request, String options) {
    Promise<JsonObject> promise = Promise.promise();

    DbResponseMessageBuilder responseMsg = new DbResponseMessageBuilder();

    client.performRequestAsync(request, new ResponseListener() {
      @Override
      public void onSuccess(Response response) {

        try {
          int statusCode = response.getStatusLine().getStatusCode();
          if (statusCode != 200 && statusCode != 204) {
            promise.fail(DATABASE_BAD_QUERY);
            return;
          }
          JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
          int totalHits = responseJson.getJsonObject(HITS)
                  .getJsonObject(TOTAL)
                  .getInteger(VALUE);
          responseMsg.statusSuccess()
                  .setTotalHits(totalHits);
          if (totalHits > 0) {
            JsonArray results = new JsonArray();
            JsonObject aggregationResult = new JsonObject();

            if ((options == SOURCE_ONLY
                    || options == DOC_IDS_ONLY
                    || options == SOURCE_AND_ID
                    || options == SOURCE_AND_ID_GEOQUERY
                    || options == DATASET
                    || options == PROVIDER_AGGREGATION_ONLY)
                    && responseJson.getJsonObject(HITS).containsKey(HITS)) {
              results = responseJson.getJsonObject(HITS).getJsonArray(HITS);
            }
            if (options == AGGREGATION_ONLY || options == RATING_AGGREGATION_ONLY
                        || options == RESOURCE_AGGREGATION_ONLY) {
                  results = responseJson.getJsonObject(AGGREGATIONS)
                          .getJsonObject(RESULTS)
                          .getJsonArray(BUCKETS);
                  responseMsg.response.put(TOTAL_HITS, results.size());

            }
                if (options == LATEST_RG_AGG) {
                  results = responseJson.getJsonObject(HITS).getJsonArray(HITS);
                  aggregationResult =
                      new JsonObject()
                          .put(
                              RESOURCE_GROUP_COUNT,
                              responseJson
                                  .getJsonObject(AGGREGATIONS)
                                  .getJsonObject(RESOURCE_GROUP_COUNT)
                                  .getInteger(DOC_COUNT))
                          .put(
                              RESOURCE_COUNT,
                              responseJson
                                  .getJsonObject(AGGREGATIONS)
                                  .getJsonObject(RESOURCE_COUNT)
                                  .getJsonObject("Resources")
                                  .getInteger(DOC_COUNT))
                          .put(
                              PROVIDER_COUNT,
                              responseJson
                                  .getJsonObject(AGGREGATIONS)
                                  .getJsonObject(PROVIDER_COUNT)
                                  .getJsonObject("Providers")
                                  .getInteger(DOC_COUNT));
                }

            for (int i = 0; i < results.size(); i++) {
              if (options == SOURCE_ONLY) {
                // Todo: This might slow system down
                JsonObject source = results.getJsonObject(i).getJsonObject(SOURCE);
                source.remove(SUMMARY_KEY);
                source.remove(WORD_VECTOR_KEY);
                responseMsg.addResult(source);
              }
              if (options == DOC_IDS_ONLY) {
                responseMsg.addResult(results.getJsonObject(i).getString(DOC_ID));
              }
              if (options == AGGREGATION_ONLY) {
                responseMsg.addResult(results.getJsonObject(i).getString(KEY));
              }
              if (options == LATEST_RG_AGG) {
                    JsonObject source = results.getJsonObject(i).getJsonObject(SOURCE);
                    source.remove(SUMMARY_KEY);
                    source.remove(WORD_VECTOR_KEY);
                    responseMsg.addResult(source);
                    responseMsg.response.put("count", aggregationResult);
                  }
              if (options == RATING_AGGREGATION_ONLY) {
                JsonObject result = new JsonObject()
                        .put(ID, results.getJsonObject(i).getString(KEY))
                        .put(TOTAL_RATINGS, results.getJsonObject(i).getString(DOC_COUNT))
                        .put(AVERAGE_RATING, results.getJsonObject(i)
                                .getJsonObject(AVERAGE_RATING).getDouble(VALUE));
                responseMsg.addResult(result);
              }
              if (options == SOURCE_AND_ID) {

                JsonObject source = results.getJsonObject(i).getJsonObject(SOURCE);
                String docId = results.getJsonObject(i).getString(DOC_ID);
                JsonObject result = new JsonObject().put(SOURCE, source).put(DOC_ID, docId);
                responseMsg.addResult(result);
              }
              if (options == SOURCE_AND_ID_GEOQUERY) {
                String docId = results.getJsonObject(i).getString(DOC_ID);
                JsonObject source = results.getJsonObject(i).getJsonObject(SOURCE);
                source.put("doc_id", docId);
                JsonObject result = new JsonObject();
                result.mergeIn(source);
                responseMsg.addResult(result);
              }
              if (options == RESOURCE_AGGREGATION_ONLY) {
                responseMsg.addResult(results.getJsonObject(i));
              }
            }
            if (options == DATASET) {
              JsonArray resource = new JsonArray();
              JsonObject datasetDetail = new JsonObject();
              JsonObject dataset = new JsonObject();
              for (int i = 0; i < results.size(); i++) {
                JsonObject record = results.getJsonObject(i).getJsonObject(SOURCE);
                JsonObject provider = new JsonObject();
                String itemType = Util.getItemType(record);
                if (itemType.equals(VALIDATION_FAILURE_MSG)) {
                  promise.fail(VALIDATION_FAILURE_MSG);
                }
                if (itemType.equals("iudx:Provider")) {
                  provider
                          .put(ID, record.getString(ID))
                          .put(DESCRIPTION_ATTR, record.getString(DESCRIPTION_ATTR))
                          .put(ICON_BASE64, record.getString(ICON_BASE64));
                      dataset.put("resourceServerRegURL", record.getString("resourceServerRegURL"));
                      dataset.put(PROVIDER, provider);
                    }
                    if (itemType.equals("iudx:Resource")) {
                      if (record.getJsonArray(TYPE).size() > 1) {
                        String schema =
                                record.getString("@context")
                                        + record
                                        .getJsonArray(TYPE)
                                        .getString(1)
                                        .substring(5,
                                                record.getJsonArray(TYPE).getString(1).length());
                        record.put("schema", schema);
                      }
                      record.remove("type");
                      record.put("resourceId", record.getString("id"));
                      record.remove("id");
                      resource.add(record);
                    }
                    if (itemType.equals("iudx:ResourceGroup")) {
                      dataset
                          .put(ID, record.getString(ID))
                          .put(DESCRIPTION_ATTR, record.getString(DESCRIPTION_ATTR));
                      if (record.getJsonArray(TYPE).size() > 1) {
                        String schema =
                            record.getString("@context")
                                + record
                                    .getJsonArray(TYPE)
                                    .getString(1)
                                    .substring(5, record.getJsonArray(TYPE).getString(1).length());
                        record.put("schema", schema);
                        record.remove("@context");
                        dataset
                          .put("schema", schema);
                      }
                      if (record.containsKey(LABEL)) {
                        dataset.put(LABEL, record.getString(LABEL));
                      }
                      if (record.containsKey(ACCESS_POLICY)) {
                        dataset.put(ACCESS_POLICY, record.getString(ACCESS_POLICY));
                      }
                      if (record.containsKey(INSTANCE)) {
                        dataset.put(INSTANCE, record.getString(INSTANCE));
                      }
                      if (record.containsKey(DATA_SAMPLE)) {
                        dataset.put(DATA_SAMPLE, record.getJsonObject(DATA_SAMPLE));
                      }
                      if (record.containsKey("dataSampleFile")) {
                        dataset.put("dataSampleFile", record.getJsonArray("dataSampleFile"));
                      }
                      if (record.containsKey("dataQualityFile")) {
                        dataset.put("dataQualityFile", record.getJsonArray("dataQualityFile"));
                      }
                      if (record.containsKey(DATA_DESCRIPTOR)) {
                        dataset.put(DATA_DESCRIPTOR, record.getJsonObject(DATA_DESCRIPTOR));
                      }
                      if (record.containsKey("resourceType")) {
                        dataset.put("resourceType", record.getString("resourceType"));
                      }
                      if (record.containsKey("location")) {
                        dataset.put("location", record.getJsonObject("location"));
                      }
                      if (record.containsKey("itemCreatedAt")) {
                        dataset.put("itemCreatedAt", record.getString("itemCreatedAt"));
                      }
                    }

                if (itemType.equals("iudx:COS")) {
                  dataset.put("cosURL", record.getString("cosURL"));
                }
              }
              datasetDetail.put("dataset", dataset);
              datasetDetail.put("resource", resource);
              responseMsg.addResult(datasetDetail);
            }
            if (options == PROVIDER_AGGREGATION_ONLY) {
              JsonObject result = new JsonObject();
              JsonArray resourceGroupAndProvider = new JsonArray();
              if (responseJson.containsKey(AGGREGATIONS)) {
                int providerCount = responseJson
                        .getJsonObject(AGGREGATIONS)
                        .getJsonObject("provider_count")
                        .getInteger(VALUE);
                result.put("providerCount", providerCount);
              }
              for (int i = 0; i < results.size(); i++) {
                JsonObject source = results.getJsonObject(i).getJsonObject(SOURCE);
                source.remove(SUMMARY_KEY);
                source.remove(WORD_VECTOR_KEY);
                resourceGroupAndProvider.add(source);
              }
              result.put("resourceGroupAndProvider", resourceGroupAndProvider);
              responseMsg.addResult(result);
            }
            if (options == AGGREGATION_LIST) {
              // Fetch all aggregations from the response JSON
              JsonObject aggregations = responseJson.getJsonObject(AGGREGATIONS);

              // Loop through each aggregation in the aggregations object
              for (String aggregationKey : aggregations.fieldNames()) {
                JsonObject aggregation = aggregations.getJsonObject(aggregationKey);

                // If the aggregation contains "buckets", process them
                if (aggregation.containsKey(BUCKETS)) {
                  JsonArray buckets = aggregation.getJsonArray(BUCKETS);

                  // Create a list to hold the keys of this aggregation's buckets
                  JsonArray aggregationKeys = new JsonArray();
                  for (int i = 0; i < buckets.size(); i++) {
                    JsonObject bucket = buckets.getJsonObject(i);
                    aggregationKeys.add(bucket.getString(KEY)); // Add the key of the bucket
                  }

                  // Create a new JsonObject to represent the aggregation as key-value pair
                  JsonObject result = new JsonObject();
                  result.put(aggregationKey, aggregationKeys);  // aggregationKey as the field name
                  responseMsg.addResult(result);
                }
              }
            }

          } else {
            responseMsg.addResult();
          }
          promise.complete(responseMsg.getResponse());

        } catch (IOException e) {
          promise.fail(e);
        } finally {
          EntityUtils.consumeQuietly(response.getEntity());
        }
      }

      @Override
      public void onFailure(Exception e) {
        promise.fail(e);
      }
    });
    return promise.future();
  }

  /**
   * searchAsync - Wrapper around elasticsearch async search requests.
   *
   * @param query Query
   * @param resultHandler JsonObject result {@link AsyncResult}
   * @TODO XPack Security
   */
  public ElasticClient searchAsync(String query, String index,
      Handler<AsyncResult<JsonObject>> resultHandler) {

    Request queryRequest = new Request(REQUEST_GET, index + "/_search" + FILTER_PATH);
    queryRequest.setJsonEntity(query);
    LOGGER.debug("searchAsync called");
    LOGGER.debug(query);
    LOGGER.debug(queryRequest);
    Future<JsonObject> future = searchAsync(queryRequest, SOURCE_ONLY);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * Searches for a dataset in the specified Elasticsearch index using the provided query string.
   *
   * @param query the query string to use for the search
   * @param index the name of the Elasticsearch index to search in
   * @param resultHandler the async result handler to receive the search results in JSON format
   * @return the ElasticClient instance for chaining method calls
   */
  public ElasticClient searchAsyncDataset(
      String query, String index, Handler<AsyncResult<JsonObject>> resultHandler) {
    Request queryRequest = new Request(REQUEST_GET, index + "/_search" + FILTER_PATH);
    queryRequest.setJsonEntity(query);
    Future<JsonObject> future = searchAsync(queryRequest, DATASET);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * Executes a resource aggregation request asynchronously for the given query and index,
   * and returns the result through the resultHandler.
   * @param query the aggregation query to execute
   * @param index the index to search in
   * @param resultHandler the handler for the result of the aggregation query
   * @return the ElasticClient object
   */
  public ElasticClient resourceAggregationAsync(
          String query, String index, Handler<AsyncResult<JsonObject>> resultHandler) {
    Request queryRequest = new Request(REQUEST_GET, index
            + "/_search"
            + FILTER_PATH_AGGREGATION);
    queryRequest.setJsonEntity(query);

    LOGGER.debug("resourceAggregationAsync called");
    LOGGER.debug(query);
    LOGGER.debug(queryRequest);
    Future<JsonObject> future = searchAsync(queryRequest, RESOURCE_AGGREGATION_ONLY);
    future.onComplete(resultHandler);
    return this;
  }

  public ElasticClient searchLatestResource(
      String query, String index, Handler<AsyncResult<JsonObject>> resultHandler) {
    Request queryRequest =
        new Request(
            REQUEST_GET,
            index + "/_search" + "?filter_path=aggregations,hits.total.value,hits.hits._source");
    queryRequest.setJsonEntity(query);
    Future<JsonObject> future = searchAsync(queryRequest, "LATEST_RG_AGG");
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * Executes an asynchronous search operation with a geo query in the specified index.
   * @param query the query to execute
   * @param index the name of the index to search
   * @param resultHandler the handler for the search result
   * @return this ElasticClient instance
   */
  public ElasticClient searchAsyncGeoQuery(
      String query, String index, Handler<AsyncResult<JsonObject>> resultHandler) {
    Request queryRequest =
        new Request(REQUEST_GET, index + "/_search" + FILTER_PATH_ID_AND_SOURCE);
    queryRequest.setJsonEntity(query);
    LOGGER.debug(queryRequest);
    Future<JsonObject> future = searchAsync(queryRequest, SOURCE_AND_ID_GEOQUERY);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * Asynchronously search for documents and retrieve only their ids and sources in the given index.
   * @param query the query in JSON format as a string
   * @param index the name of the index to search in
   * @param resultHandler the handler for the asynchronous result of the search
   * @return the ElasticClient instance to enable method chaining
   */
  public ElasticClient searchAsyncGetId(
      String query, String index, Handler<AsyncResult<JsonObject>> resultHandler) {
    Request queryRequest = new Request(REQUEST_GET, index + "/_search" + FILTER_PATH_ID_AND_SOURCE);
    queryRequest.setJsonEntity(query);
    LOGGER.debug(queryRequest);
    Future<JsonObject> future = searchAsync(queryRequest, SOURCE_AND_ID);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * Asynchronously searches for resource groups and providers based on the provided query.
   * @param query the query in JSON format as a string
   * @param index the index to search within
   * @param resultHandler the handler for the asynchronous result of the search
   * @return the ElasticClient instance to allow method chaining
   */
  public ElasticClient searchAsyncResourceGroupAndProvider(
      String query, String index, Handler<AsyncResult<JsonObject>> resultHandler) {
    Request queryRequest = new Request(REQUEST_GET, index + "/_search" + "?filter_path=");
    queryRequest.setJsonEntity(query);
    LOGGER.debug("searchAsync called for resource group and provider");
    Future<JsonObject> future = searchAsync(queryRequest, "PROVIDER_AGGREGATION");
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * Performs a search based on the given query vector using a script score query
   * with cosine similarity between the query vector and the word
   * vectors of the documents in the index.
   * @param queryVector the query vector in a JSON array format
   * @param resultHandler the handler for the result of the search operation
   * @return the ElasticClient instance for chaining calls
   */
  public ElasticClient scriptSearch(
      JsonArray queryVector, Handler<AsyncResult<JsonObject>> resultHandler) {
    // String query = NLP_SEARCH.replace("$1", queryVector.toString());
    String query = "{\"query\": {\"script_score\": {\"query\": {\"match\": {}},"
            + " \"script\": {\"source\": \"doc['_word_vector'].size() == 0 ? 0 : cosineSimilarity"
            + "(params.query_vector, '_word_vector') + 1.0\","
            + "\"lang\": \"painless\",\"params\": {\"query_vector\":" + queryVector.toString()
            + "}}}},\"_source\": {\"excludes\": [\"_word_vector\"]}}";

    Request queryRequest = new Request(REQUEST_GET, index + "/_search");
    queryRequest.setJsonEntity(query);
    Future<JsonObject> future = searchAsync(queryRequest, SOURCE_ONLY);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * Performs a location-based search query using a vector and a set of query parameters.
   *
   * @param queryVector The vector to use for the query.
   * @param queryParams The parameters to filter results based on location.
   * @return A Future of a JsonObject containing the search results.
   */
  public Future<JsonObject> scriptLocationSearch(JsonArray queryVector, JsonObject queryParams) {

    StringBuilder query = new StringBuilder("{\"query\": {\"script_score\": {\"query\": {\"bool\":"
            + " {\"should\": [");
    if (queryParams.containsKey(BOROUGH)) {
      query.append("{\"match\": {\"_geosummary._geocoded.results.borough\": \"")
              .append(queryParams.getString(BOROUGH)).append("\"}},");
    }
    if (queryParams.containsKey(LOCALITY)) {
      query.append("{\"match\": {\"_geosummary._geocoded.results.locality\": \"")
                      .append(queryParams.getString(LOCALITY)).append("\"}},");
    }
    if (queryParams.containsKey(COUNTY)) {
      query.append("{\"match\": {\"_geosummary._geocoded.results.county\": \"")
              .append(queryParams.getString(COUNTY)).append("\"}},");
    }
    if (queryParams.containsKey(REGION)) {
      query.append("{\"match\": {\"_geosummary._geocoded.results.region\": \"")
              .append(queryParams.getString(REGION)).append("\"}},");
    }
    if (queryParams.containsKey(COUNTRY)) {
      query.append("{\"match\": {\"_geosummary._geocoded.results.country\": \"")
              .append(queryParams.getString(COUNTRY)).append("\"}}");
    } else {
      query.deleteCharAt(query.length() - 1);
    }
    JsonArray bboxCoords = queryParams.getJsonArray(BBOX);
    query.append("],\"minimum_should_match\": 1, \"filter\": {\"geo_shape\":"
                    + " {\"location.geometry\": {\"shape\": {\"type\": \"envelope\",")
        .append("\"coordinates\": [ [ ").append(bboxCoords.getFloat(0)).append(",")
            .append(bboxCoords.getFloat(3)).append("],")
        .append("[").append(bboxCoords.getFloat(2)).append(",").append(bboxCoords
                    .getFloat(1)).append("] ]}, \"relation\": \"intersects\" }}}}},")
        .append("\"script\": {\"source\": \"doc['_word_vector'].size() == 0 ? 0 : "
                + "cosineSimilarity(params.query_vector, '_word_vector') + 1.0\",")
        .append("\"params\": { \"query_vector\":").append(queryVector.toString())
            .append("}}}}, \"_source\": {\"excludes\": [\"_word_vector\"]}}");

    Request queryRequest = new Request(REQUEST_GET, index + "/_search");
    queryRequest.setJsonEntity(query.toString());
    Future<JsonObject> future = searchAsync(queryRequest, SOURCE_ONLY);
    Promise<JsonObject> promise = Promise.promise();

    future.onSuccess(h -> {
      promise.complete(future.result());
    }).onFailure(h -> {
      promise.fail(future.cause());
    });
    return promise.future();
  }


  /**
   * searchGetIdAsync - Get document IDs matching a query.
   *
   * @param query Query
   * @param resultHandler JsonObject result {@link AsyncResult}
   * @TODO XPack Security
   */
  public ElasticClient searchGetId(String query, String index,
      Handler<AsyncResult<JsonObject>> resultHandler) {

    Request queryRequest = new Request(REQUEST_GET, index + "/_search" + FILTER_ID_ONLY_PATH);
    queryRequest.setJsonEntity(query);
    Future<JsonObject> future = searchAsync(queryRequest, DOC_IDS_ONLY);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * Executes a rating aggregation request asynchronously for the given query and index, and returns
   * the result through the resultHandler.
   * @param query the aggregation query to execute
   * @param index the index to search in
   * @param resultHandler the handler for the result of the aggregation query
   * @return the ElasticClient object
   */
  public ElasticClient ratingAggregationAsync(
      String query, String index, Handler<AsyncResult<JsonObject>> resultHandler) {
    Request queryRequest = new Request(REQUEST_GET, index
        + "/_search"
        + FILTER_PATH_AGGREGATION);
    queryRequest.setJsonEntity(query);
    Future<JsonObject> future = searchAsync(queryRequest, RATING_AGGREGATION_ONLY);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * aggregationsAsync - Wrapper around elasticsearch async search requests.
   *
   * @param query Query
   * @param resultHandler JsonObject result {@link AsyncResult}
   * @TODO XPack Security
   */
  public ElasticClient listAggregationAsync(String query,
      Handler<AsyncResult<JsonObject>> resultHandler) {

    Request queryRequest = new Request(REQUEST_GET, index
                              + "/_search"
                              + FILTER_PATH_AGGREGATION);
    queryRequest.setJsonEntity(query);
    Future<JsonObject> future = searchAsync(queryRequest, AGGREGATION_ONLY);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * aggregationsAsync - Wrapper around elasticsearch async search requests.
   *
   * @param query Query
   * @param resultHandler JsonObject result {@link AsyncResult}
   * @TODO XPack Security
   */
  public ElasticClient listAggregationsAsync(String query,
                                            Handler<AsyncResult<JsonObject>> resultHandler) {

    Request queryRequest = new Request(REQUEST_GET, index
        + "/_search"
        + FILTER_PATH_AGGREGATIONS);
    queryRequest.setJsonEntity(query);
    Future<JsonObject> future = searchAsync(queryRequest, AGGREGATION_LIST);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * countAsync - private function which perform performRequestAsync for count apis.
   *
   * @param request Elastic Request
   * @TODO XPack Security
   * @TODO Can combine countAsync and searchAsync
   */
  private Future<JsonObject> countAsync(Request request) {
    Promise<JsonObject> promise = Promise.promise();

    DbResponseMessageBuilder responseMsg = new DbResponseMessageBuilder();

    client.performRequestAsync(request, new ResponseListener() {
      @Override
      public void onSuccess(Response response) {

        try {
          int statusCode = response.getStatusLine().getStatusCode();
          if (statusCode != 200 && statusCode != 204) {
            promise.fail(DATABASE_BAD_QUERY);
            return;
          }
          JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
          responseMsg.statusSuccess()
                  .setTotalHits(responseJson.getInteger(COUNT));
          promise.complete(responseMsg.getResponse());

        } catch (IOException e) {
          promise.fail(e);
        } finally {
          EntityUtils.consumeQuietly(response.getEntity());
        }
      }

      @Override
      public void onFailure(Exception e) {
        promise.fail(e);
      }
    });
    return promise.future();
  }

  /**
   * countAsync - Wrapper around elasticsearch async count requests.
   *
   * @param index Index to search on
   * @param query Query
   * @param resultHandler JsonObject result {@link AsyncResult}
   * @TODO XPack Security
   */
  public ElasticClient countAsync(String query, String index,
      Handler<AsyncResult<JsonObject>> resultHandler) {

    Request queryRequest = new Request(REQUEST_GET, index + "/_count");
    queryRequest.setJsonEntity(query);
    Future<JsonObject> future = countAsync(queryRequest);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * docPostAsync - Wrapper around elasticsearch async doc post request.
   *
   * @param index Index to search on
   * @param doc Document
   * @param resultHandler JsonObject
   * @TODO XPack Security
   */
  public ElasticClient docPostAsync(
      String index, String doc, Handler<AsyncResult<JsonObject>> resultHandler) {

    // TODO: Validation
    JsonObject docJson;
    try {
      docJson = new JsonObject(doc);
    } catch (DecodeException e) {
      resultHandler.handle(Future.failedFuture(
          "Invalid JSON document: " + e.getMessage()));
      return this;
    }

    String docId = docJson.getString(ID);
    Request docRequest = new Request(REQUEST_POST, index + "/_doc/" + docId);
    docRequest.setJsonEntity(doc.toString());

    Future<JsonObject> future = docAsync(REQUEST_POST, docRequest);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * docPutAsync - Wrapper around elasticsearch async doc put request.
   *
   * @param index Index to search on
   * @param docId Document id (elastic id)
   * @param doc Document
   * @param resultHandler JsonObject
   * @TODO XPack Security
   */
  public ElasticClient docPutAsync(String docId, String index, String doc,
      Handler<AsyncResult<JsonObject>> resultHandler) {

    // TODO: Validation
    Request docRequest = new Request(REQUEST_PUT, index + "/_doc/" + docId);
    docRequest.setJsonEntity(doc.toString());
    Future<JsonObject> future = docAsync(REQUEST_PUT, docRequest);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * docDelAsync - Wrapper around elasticsearch async doc delete request.
   *
   * @param index Index to search on
   * @param docId String of the Document
   * @param resultHandler JsonObject
   * @TODO XPack Security
   */
  public ElasticClient docDelAsync(String docId, String index,
      Handler<AsyncResult<JsonObject>> resultHandler) {

    // TODO: Validation
    Request docRequest = new Request(REQUEST_DELETE, index + "/_doc/" + docId);

    Future<JsonObject> future = docAsync(REQUEST_DELETE, docRequest);
    future.onComplete(resultHandler);
    return this;
  }

  public ElasticClient docPatchAsync(
      String docId, String index, String doc, Handler<AsyncResult<JsonObject>> resultHandler) {

    // TODO: Validation
    Request docRequest = new Request(REQUEST_POST, index + "/_update/" + docId);
    docRequest.setJsonEntity(doc.toString());
    Future<JsonObject> future = docAsync(REQUEST_POST, docRequest);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * DbResponseMessageBuilder} Message builder for search APIs.
   */
  private class DbResponseMessageBuilder {
    private JsonObject response = new JsonObject();
    private JsonArray results = new JsonArray();

    DbResponseMessageBuilder() {
    }

    DbResponseMessageBuilder statusSuccess() {
      response.put(TYPE, TYPE_SUCCESS);
      response.put(TITLE, TITLE_SUCCESS);
      return this;
    }

    DbResponseMessageBuilder setTotalHits(int hits) {
      response.put(TOTAL_HITS, hits);
      return this;
    }

    /** Overloaded for source only request. */
    DbResponseMessageBuilder addResult(JsonObject obj) {
      response.put(RESULTS, results.add(obj));
      return this;
    }

    /** Overloaded for doc-ids request. */
    DbResponseMessageBuilder addResult(String value) {
      response.put(RESULTS, results.add(value));
      return this;
    }

    DbResponseMessageBuilder addResult() {
      response.put(RESULTS, results);
      return this;
    }

    JsonObject getResponse() {
      return response;
    }
  }

  /**
   * docAsync - private function which perform performRequestAsync for doc apis.
   *
   * @param request Elastic Request
   * @param method SOURCE - Source only DOCIDS - DOCIDs only IDS - IDs only @TODO XPack
   *     Security @TODO Can combine countAsync and searchAsync
   */
  private Future<JsonObject> docAsync(String method, Request request) {
    Promise<JsonObject> promise = Promise.promise();

    client.performRequestAsync(
        request,
        new ResponseListener() {
          @Override
          public void onSuccess(Response response) {
            try {
              JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
              int statusCode = response.getStatusLine().getStatusCode();
              switch (method) {
                case REQUEST_POST:
                  if (statusCode == 201 || statusCode == 200) {
                    promise.complete(responseJson);
                    return;
                  }
                  break;
                case REQUEST_DELETE:
                case REQUEST_PUT:
                  if (statusCode == 200) {
                    promise.complete(responseJson);
                    return;
                  }
                  break;
                default:
                  promise.fail(DATABASE_BAD_QUERY);
              }
              promise.fail("Failed request");
            } catch (IOException e) {
              promise.fail(e);
            } finally {
              EntityUtils.consumeQuietly(response.getEntity());
            }
          }

          @Override
          public void onFailure(Exception e) {
            promise.fail(e);
          }
        });
    return promise.future();
  }
}
