package iudx.catalogue.server.apiserver.stack.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.apiserver.stack.model.StacCatalog;
import iudx.catalogue.server.apiserver.stack.model.StacLink;

public interface StacService {

  Future<JsonObject> get(String stackId);

  Future<StacCatalog> create(StacCatalog stackObj);

  Future<String> update(StacLink childObj);

  Future<String> delete(String stackId);
}
