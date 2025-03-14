package iudx.catalogue.server.geocoding.service;

import static iudx.catalogue.server.geocoding.util.Constants.ADDRESS;
import static iudx.catalogue.server.geocoding.util.Constants.BBOX;
import static iudx.catalogue.server.geocoding.util.Constants.BOROUGH;
import static iudx.catalogue.server.geocoding.util.Constants.CONFIDENCE;
import static iudx.catalogue.server.geocoding.util.Constants.COORDINATES;
import static iudx.catalogue.server.geocoding.util.Constants.COUNTRY;
import static iudx.catalogue.server.geocoding.util.Constants.COUNTY;
import static iudx.catalogue.server.geocoding.util.Constants.FEATURES;
import static iudx.catalogue.server.geocoding.util.Constants.GEOCODED;
import static iudx.catalogue.server.geocoding.util.Constants.GEOMETRY;
import static iudx.catalogue.server.geocoding.util.Constants.LOCALITY;
import static iudx.catalogue.server.geocoding.util.Constants.LOCATION;
import static iudx.catalogue.server.geocoding.util.Constants.NAME;
import static iudx.catalogue.server.geocoding.util.Constants.PROPERTIES;
import static iudx.catalogue.server.geocoding.util.Constants.REGION;
import static iudx.catalogue.server.geocoding.util.Constants.RESULTS;
import static iudx.catalogue.server.geocoding.util.Constants.REVERSE_GEOCODED;
import static iudx.catalogue.server.geocoding.util.Constants.SERVICE_TIMEOUT;
import static iudx.catalogue.server.geocoding.util.Constants.TYPE;
import static iudx.catalogue.server.util.Constants.TITLE_ITEM_NOT_FOUND;
import static iudx.catalogue.server.util.Constants.TYPE_ITEM_NOT_FOUND;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The GeocodingController Service Implementation.
 *
 * <h1>GeocodingController Service Implementation</h1>
 *
 * <p>The GeocodingController Service implementation in the IUDX Catalogue Server implements the
 * definitions of the {@link GeocodingService}.
 *
 * @version 1.0
 * @since 2020-11-05
 */
public class GeocodingServiceImpl implements GeocodingService {

  private static final Logger LOGGER = LogManager.getLogger(GeocodingServiceImpl.class);
  public static WebClient webClient;
  private final String peliasUrl;
  private final int peliasPort;

  /**
   * Constructs a new instance of GeocodingServiceImpl with the provided WebClient and Pelias URL
   * and port. The geocoding service is used to convert textual location queries into geographic
   * coordinates.
   *
   * @param webClient the WebClient used to perform HTTP requests
   * @param peliasUrl the URL of the Pelias geocoding service
   * @param peliasPort the port number of the Pelias geocoding service
   */
  public GeocodingServiceImpl(WebClient webClient, String peliasUrl, int peliasPort) {
    GeocodingServiceImpl.webClient = webClient;
    this.peliasUrl = peliasUrl;
    this.peliasPort = peliasPort;
  }

  @Override
  public Future<String> geocoder(String location) {
    Promise<String> promise = Promise.promise();
    webClient
        .get(peliasPort, peliasUrl, "/v1/search")
        .timeout(SERVICE_TIMEOUT)
        .addQueryParam("text", location)
        .putHeader("Accept", "application/json")
        .send()
        .onSuccess(
            response -> {
              if (response.body().toJsonObject().containsKey(FEATURES)
                  && !response.body().toJsonObject().getJsonArray(FEATURES).isEmpty()) {
                JsonArray features = response.body().toJsonObject().getJsonArray(FEATURES);
                JsonObject property;
                JsonObject feature;
                JsonObject resultEntry;
                double confidence = 0;
                JsonArray resultArray = new JsonArray();
                for (int i = 0; i < features.size(); i++) {
                  feature = features.getJsonObject(i);
                  property = feature.getJsonObject(PROPERTIES);
                  resultEntry = generateGeocodingJson(property);
                  if (confidence < property.getDouble(CONFIDENCE) && resultArray.isEmpty()
                      || confidence == property.getDouble(CONFIDENCE)) {
                    confidence = property.getDouble(CONFIDENCE);
                  } else if (confidence < property.getDouble(CONFIDENCE)
                      && !resultArray.isEmpty()) {
                    confidence = property.getDouble(CONFIDENCE);
                    resultArray = new JsonArray();
                    resultArray.add(resultEntry);
                  }

                  if (feature.getJsonArray(BBOX) != null) {
                    resultEntry.put(BBOX, feature.getJsonArray(BBOX));
                    resultArray.add(resultEntry);
                  }
                }
                LOGGER.debug("Request succeeded!");
                JsonObject result = new JsonObject().put(RESULTS, resultArray);
                promise.complete(result.toString());
              } else {
                LOGGER.error("Failed to find coordinates");
                promise.fail(
                    new JsonObject()
                        .put("type", TYPE_ITEM_NOT_FOUND)
                        .put("title", TITLE_ITEM_NOT_FOUND)
                        .put("detail", "Failed to find coordinates")
                        .toString());
              }
            })
        .onFailure(
            err -> {
              LOGGER.error("Failed to find coordinates");
              promise.fail(err);
            });

    return promise.future();
  }

  private JsonObject generateGeocodingJson(JsonObject property) {
    JsonObject resultEntry = new JsonObject();
    if (property.containsKey(NAME)) {
      resultEntry.put(NAME, property.getString(NAME));
    }
    if (property.containsKey(COUNTRY)) {
      resultEntry.put(COUNTRY, property.getString(COUNTRY));
    }
    if (property.containsKey(REGION)) {
      resultEntry.put(REGION, property.getString(REGION));
    }
    if (property.containsKey(COUNTY)) {
      resultEntry.put(COUNTY, property.getString(COUNTY));
    }
    if (property.containsKey(LOCALITY)) {
      resultEntry.put(LOCALITY, property.getString(LOCALITY));
    }
    if (property.containsKey(BOROUGH)) {
      resultEntry.put(BOROUGH, property.getString(BOROUGH));
    }
    return resultEntry;
  }

  private Future<JsonObject> geocoderHelper(String location) {
    Promise<JsonObject> promise = Promise.promise();
    geocoder(location)
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                LOGGER.debug(ar.result());
                JsonObject arResToJson = new JsonObject(ar.result());
                promise.complete(arResToJson);
              } else {
                LOGGER.error("Request failed!");
                promise.complete(new JsonObject());
              }
            });
    return promise.future();
  }

  @Override
  public Future<JsonObject> reverseGeocoder(String lat, String lon) {
    Promise<JsonObject> promise = Promise.promise();
    webClient
        .get(peliasPort, peliasUrl, "/v1/reverse")
        .timeout(SERVICE_TIMEOUT)
        .addQueryParam("point.lon", lon)
        .addQueryParam("point.lat", lat)
        .putHeader("Accept", "application/json")
        .send()
        .onSuccess(
            response -> {
              LOGGER.debug("Request succeeded!");
              promise.complete(response.bodyAsJsonObject());
            })
        .onFailure(
            err -> {
              LOGGER.error("Failed to find location");
              promise.fail(err);
            });
    return promise.future();
  }

  private Future<JsonObject> reverseGeocoderHelper(String lat, String lon) {
    return reverseGeocoder(lat, lon)
        .map(
            ar -> {
              JsonArray res = ar.getJsonArray(FEATURES);
              JsonObject properties = res.getJsonObject(0).getJsonObject(PROPERTIES);
              return generateGeocodingJson(properties);
            })
        .otherwise(new JsonObject());
  }

  @Override
  public Future<String> geoSummarize(JsonObject doc) {
    Promise<String> promise = Promise.promise();
    Future<JsonObject> p1 = Future.succeededFuture(new JsonObject());
    Future<JsonObject> p2 = Future.succeededFuture(new JsonObject());

    if (doc.containsKey(LOCATION)) {

      /* GeocodingController information*/
      JsonObject location = doc.getJsonObject(LOCATION);
      String address = location.getString(ADDRESS);
      if (address != null) {
        p1 = geocoderHelper(address);
      }

      /* Reverse GeocodingController information */
      if (location.containsKey(GEOMETRY)
          && location.getJsonObject(GEOMETRY).getString(TYPE).equalsIgnoreCase("Point")) {
        JsonObject geometry = location.getJsonObject(GEOMETRY);
        JsonArray pos = geometry.getJsonArray(COORDINATES);
        String lon = pos.getString(0);
        String lat = pos.getString(1);
        p2 = reverseGeocoderHelper(lat, lon);
      }
    }
    CompositeFuture.all(p1, p2)
        .onSuccess(
            successHandler -> {
              JsonObject j1 = successHandler.resultAt(0);
              JsonObject j2 = successHandler.resultAt(1);
              JsonObject res = new JsonObject().put(GEOCODED, j1).put(REVERSE_GEOCODED, j2);
              promise.complete(res.toString());
            })
        .onFailure(promise::fail);

    return promise.future();
  }
}
