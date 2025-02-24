/**
 *
 *
 * <h1>GeocodingController.java</h1>
 *
 *<p>Callback handlers for Geocoding Service APIs</p>
 */

package iudx.catalogue.server.geocoding.controller;

import static iudx.catalogue.server.apiserver.util.Constants.INVALID_VALUE;
import static iudx.catalogue.server.apiserver.util.Constants.LOCATION_NOT_FOUND;
import static iudx.catalogue.server.apiserver.util.Constants.ROUTE_GEO_COORDINATES;
import static iudx.catalogue.server.apiserver.util.Constants.ROUTE_GEO_REVERSE;
import static iudx.catalogue.server.util.Constants.COORDINATES;
import static iudx.catalogue.server.util.Constants.FAILED;
import static iudx.catalogue.server.util.Constants.GEOMETRY;
import static iudx.catalogue.server.util.Constants.POINT;
import static iudx.catalogue.server.util.Constants.RESULTS;
import static iudx.catalogue.server.util.Constants.TITLE_INVALID_GEO_VALUE;
import static iudx.catalogue.server.util.Constants.TITLE_INVALID_SYNTAX;
import static iudx.catalogue.server.util.Constants.TITLE_SUCCESS;
import static iudx.catalogue.server.util.Constants.TYPE_INVALID_GEO_VALUE;
import static iudx.catalogue.server.util.Constants.TYPE_INVALID_QUERY_PARAM_VALUE;
import static iudx.catalogue.server.util.Constants.TYPE_INVALID_SYNTAX;
import static iudx.catalogue.server.util.Constants.TYPE_ITEM_NOT_FOUND;
import static iudx.catalogue.server.util.Constants.TYPE_SUCCESS;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.apiserver.util.RespBuilder;
import iudx.catalogue.server.geocoding.service.GeocodingService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public final class GeocodingController {
  private static final Logger LOGGER = LogManager.getLogger(GeocodingController.class);

  private final GeocodingService geoService;
  private Router router;


  public GeocodingController(GeocodingService geoService) {
    this.geoService = geoService;
  }

  // Setup routes for Geocoding
  public Router init(Router router) {
    router
        .get(ROUTE_GEO_COORDINATES)
        .handler(this::getCoordinates);

    router
        .get(ROUTE_GEO_REVERSE)
        .handler(this::getLocation);
    return router;
  }

  // Method to return the router for mounting
  public Router getRouter() {
    return this.router;
  }

  /**
   * Get Bbox for location.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  public void getCoordinates(RoutingContext routingContext) {
    String location = null;
    try {
      if (routingContext.queryParams().contains("q")) {
        location = routingContext.queryParams().get("q");
      }
      if (location.isEmpty()) {
        LOGGER.error("NO location found");
        routingContext
            .response()
            .putHeader("content-type", "application/json")
            .setStatusCode(400)
            .end(
                new RespBuilder()
                    .withType(TYPE_ITEM_NOT_FOUND)
                    .withTitle(LOCATION_NOT_FOUND)
                    .withDetail(FAILED)
                    .getResponse());
        return;
      }
    } catch (Exception e) {
      LOGGER.error("No query parameter");
      routingContext
          .response()
          .putHeader("content-type", "application/json")
          .setStatusCode(400)
          .end(
              new RespBuilder()
                  .withType(TYPE_INVALID_QUERY_PARAM_VALUE)
                  .withTitle(INVALID_VALUE)
                  .withDetail(FAILED)
                  .getResponse());
      return;
    }
    geoService.geocoder(location)
        .onComplete(reply -> {
          if (reply.succeeded()) {
            JsonObject  result = new JsonObject(reply.result());
            routingContext
                .response()
                .putHeader("content-type", "application/json")
                .setStatusCode(200)
                .end(
                    new RespBuilder()
                        .withType(TYPE_SUCCESS)
                        .withTitle(TITLE_SUCCESS)
                        .totalHits(result.getJsonArray(RESULTS))
                        .withResult(result.getJsonArray(RESULTS))
                        .getJsonResponse().toString());
          } else {
            LOGGER.error("Failed to find coordinates");
            routingContext
                .response()
                .putHeader("content-type", "application/json")
                .setStatusCode(400)
                .end(
                    new RespBuilder()
                        .withType(TYPE_INVALID_GEO_VALUE)
                        .withTitle(TITLE_INVALID_GEO_VALUE)
                        .withDetail(FAILED)
                        .getResponse());
          }
        });
  }

  /**
   * Get location for coordinates.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  public void getLocation(RoutingContext routingContext) {

    JsonArray coordinates = new JsonArray();
    String geometry = "Point";

    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    if (request.getParam(COORDINATES) == null || request.getParam(GEOMETRY) == null) {
      LOGGER.error("Fail: Invalid Syntax");
      response
          .setStatusCode(400)
          .end(
              new RespBuilder()
                  .withType(TYPE_INVALID_SYNTAX)
                  .withTitle(TITLE_INVALID_SYNTAX)
                      .withDetail("Invalid Syntax")
                  .getResponse());
      return;
    }

    try {
      coordinates = new JsonArray(request.getParam(COORDINATES));
      geometry = request.getParam(GEOMETRY);
      if (geometry != POINT) {
        // go to catch
        throw new Exception();
      }
    } catch (Exception e) {
      LOGGER.error("Failed to find location");
      routingContext
          .response()
          .putHeader("content-type", "application/json")
          .setStatusCode(400)
          .end();
    }

    geoService.reverseGeocoder(
        coordinates.getString(1),
        coordinates.getString(0))
        .onComplete(reply -> {
          if (reply.succeeded()) {
            routingContext
                .response()
                .putHeader("content-type", "application/json")
                .setStatusCode(200)
                .end(reply.result().encode());
          } else {
            LOGGER.error("Failed to find location");
            routingContext
                .response()
                .putHeader("content-type", "application/json")
                .setStatusCode(400)
                .end();
          }
        });
  }
}
