package iudx.catalogue.server.apiserver.item.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.apiserver.item.model.Item;

public interface ItemService {
  Future<JsonObject> createItem(Item item);

  Future<JsonObject> updateItem(Item item);

  Future<JsonObject> getItem(JsonObject requestBody);

  Future<JsonObject> deleteItem(String id);

  Future<Boolean> verifyInstance(String instanceId);
}

