package iudx.catalogue.server.apiserver.stack.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public interface StacService {

  Future<JsonObject> get(String stackId);

  Future<JsonObject> create(JsonObject stackObj);

  Future<JsonObject> update(JsonObject childObj);

  Future<JsonObject> delete(String stackId);
}
