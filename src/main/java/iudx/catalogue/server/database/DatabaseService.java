package iudx.catalogue.server.database;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.geocoding.GeocodingService;
import iudx.catalogue.server.nlpsearch.NLPSearchService;

/**
 * The Database Service.
 *
 * <h1>Database Service</h1>
 *
 * <p>The Database Service in the IUDX Catalogue Server defines the operations to be performed with
 * the IUDX Database server.
 *
 * @see io.vertx.codegen.annotations.ProxyGen
 * @see io.vertx.codegen.annotations.VertxGen
 * @version 1.0
 * @since 2020-05-31
 */
@VertxGen
@ProxyGen
public interface DatabaseService {

  /* create db service with nlp and geocoding */
  @GenIgnore
  static DatabaseService create(
      ElasticClient client, NLPSearchService nlpService, GeocodingService geoService) {
    return new DatabaseServiceImpl(client, nlpService, geoService);
  }

  @GenIgnore
  static DatabaseService create(ElasticClient client) {
    return new DatabaseServiceImpl(client);
  }

  @GenIgnore
  static DatabaseService createProxy(Vertx vertx, String address) {
    return new DatabaseServiceVertxEBProxy(vertx, address);
  }

  /**
   * The searchQuery implements the search operation with the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService searchQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The searchQuery implements the nlp search operation with the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService nlpSearchQuery(JsonArray request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The searchQuery implements the nlp search operation with the database.
   *
   * @param request which is a JsonObject
   * @param queryParams which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService nlpSearchLocationQuery(
      JsonArray request, JsonObject queryParams, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The countQuery implements the count operation with the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService countQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The createItem implements the create item operation with the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService createItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The updateItem implements the update item operation with the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService updateItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The deleteItem implements the delete item operation with the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService deleteItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The listItems implements the list items operation with the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService listItems(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The listOwnerOrCos implements the fetch of entire owner or cos item from the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a service
   */
  @Fluent
  DatabaseService listOwnerOrCos(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The listRelationship implements the list resource, resourceGroup, provider, resourceServer,
   * type relationships operation with the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService listRelationship(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The relSearch implements the Relationship searches with the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService relSearch(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService getItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The createRating implements the rating creation operation with the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService createRating(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The updateRating implements the rating updation operation with the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService updateRating(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The deleteRating implements the rating deletion operation with the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService deleteRating(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The getRatings implements fetching ratings from the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService getRatings(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The createMlayerInstance implements the instance creation operation with the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service.
   */
  @Fluent
  DatabaseService createMlayerInstance(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The getMlayerInstance implements fetching instance from the database.
   *
   * @param handler which is a request handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService getMlayerInstance(
      JsonObject requestParams, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The deleteMlayerInstance implements deleting instance from the database.
   *
   * @param request which is JsonObject
   * @param handler which is a request handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService deleteMlayerInstance(String request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The updateMlayerInstance implements updating instance from the database.
   *
   * @param request which is a jsonobject
   * @param handler which is a request handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService updateMlayerInstance(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The createMlayerDomain implemenets creation of a domain from the database.
   *
   * @param request is a jsonObject
   * @param handler which is a request handler
   * @return DatabaseService which is Service
   */
  @Fluent
  DatabaseService createMlayerDomain(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The getMlayerDomain implements getting all domain from database.
   *
   * @param handler which is a request handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService getMlayerDomain(
      JsonObject requestParams, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The updateMlayerDomain implements updating all domain from database.
   *
   * @param request is a JsonObject
   * @param handler which is a request handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService updateMlayerDomain(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The deleteMlayerDomain deletes a domain from the darabase.
   *
   * @param request is a JsonObject
   * @param handler which is a request handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService deleteMlayerDomain(String request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The get Mlayer Providers get all the provider's description.
   *
   * @param handler which is a request handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService getMlayerProviders(
      JsonObject requestParams, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The post Mlayer GeoQuery posts all the dataset_id's location and label.
   *
   * @param query which is a String
   * @param handler which is a request handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService getMlayerGeoQuery(String query, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The get Mlayer All Datasets gets all the dataset belonging to IUDX.
   *
   * @param query which is a string
   * @param handler which is a request handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService getMlayerAllDatasets(
      JsonObject requestPram, String query, Handler<AsyncResult<JsonObject>> handler);

  /* create db service vanilla */

  /**
   * Retrieves provider and resource server ID based on the provided dataset ID.
   *
   * @param query which is a String to retrieve provider and resource server ID.
   * @param handler Handler to process the async result with a JsonObject.
   * @return DatabaseService instance for fluent API usage.
   */
  @Fluent
  DatabaseService getProviderAndResourceServerId(
      String query, Handler<AsyncResult<JsonObject>> handler);

  /**
   * Retrieves dataset details based on the provided query.
   *
   * @param query Query string to fetch dataset details.
   * @param handler Handler to process the async result with a JsonObject.
   * @return DatabaseService instance for fluent API usage.
   */
  @Fluent
  DatabaseService getDataset(String query, Handler<AsyncResult<JsonObject>> handler);

  /**
   * Retrieves instance icon details based on the provided instance name.
   *
   * @param query Name of the instance to fetch icon details.
   * @param handler Handler to process the async result with a JsonObject.
   * @return DatabaseService instance for fluent API usage.
   */
  @Fluent
  DatabaseService getInstanceIcon(String query, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService getMlayerPopularDatasets(
      String instance, JsonArray highestCountResource, Handler<AsyncResult<JsonObject>> handler);
}
