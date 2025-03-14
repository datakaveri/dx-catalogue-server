package iudx.catalogue.server.rating.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@ProxyGen
@VertxGen
public interface RatingService {
  @GenIgnore
  static RatingService createProxy(Vertx vertx, String address) {
    return new RatingServiceVertxEBProxy(vertx, address);
  }

  Future<JsonObject> createRating(JsonObject request);

  Future<JsonObject> getRating(JsonObject request);

  Future<JsonObject> updateRating(JsonObject request);

  Future<JsonObject> deleteRating(JsonObject request);
}
