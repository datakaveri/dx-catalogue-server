package iudx.catalogue.server.rating.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.rating.model.FilterRatingRequest;
import iudx.catalogue.server.rating.model.RatingRequest;

@ProxyGen
@VertxGen
public interface RatingService {
  @GenIgnore
  static RatingService createProxy(Vertx vertx, String address) {
    return new RatingServiceVertxEBProxy(vertx, address);
  }

  Future<JsonObject> createRating(RatingRequest ratingRequest);

  Future<JsonObject> getRating(FilterRatingRequest request);

  Future<JsonObject> updateRating(RatingRequest request);

  Future<JsonObject> deleteRating(FilterRatingRequest request);
}
