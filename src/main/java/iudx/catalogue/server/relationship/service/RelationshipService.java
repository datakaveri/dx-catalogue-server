package iudx.catalogue.server.relationship.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public interface RelationshipService {
  /**
   * The listRelationship implements the list resource, resourceGroup, provider, resourceServer,
   * type relationships operation with the database.
   *
   * @param request which is a JsonObject
   * @return a Future<{@link io.vertx.core.json.JsonObject}>
   */
  Future<JsonObject> listRelationship(JsonObject request);

  /**
   * The relSearch implements the Relationship searches with the database.
   *
   * @param request which is a JsonObject
   * @return a Future<{@link io.vertx.core.json.JsonObject}>
   */
  Future<JsonObject> relSearch(JsonObject request);
}
