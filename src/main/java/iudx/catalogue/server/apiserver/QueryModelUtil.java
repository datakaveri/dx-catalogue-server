package iudx.catalogue.server.apiserver;

import static iudx.catalogue.server.database.elastic.util.Constants.GEO_BBOX;
import static iudx.catalogue.server.geocoding.util.Constants.BBOX;
import static iudx.catalogue.server.geocoding.util.Constants.BOROUGH;
import static iudx.catalogue.server.geocoding.util.Constants.COORDINATES;
import static iudx.catalogue.server.geocoding.util.Constants.COUNTRY;
import static iudx.catalogue.server.geocoding.util.Constants.COUNTY;
import static iudx.catalogue.server.geocoding.util.Constants.LOCALITY;
import static iudx.catalogue.server.geocoding.util.Constants.REGION;
import static iudx.catalogue.server.geocoding.util.Constants.TYPE;
import static iudx.catalogue.server.util.Constants.FIELD;
import static iudx.catalogue.server.util.Constants.GEOPROPERTY;
import static iudx.catalogue.server.util.Constants.INTERSECTS;
import static iudx.catalogue.server.util.Constants.VALUE;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import iudx.catalogue.server.database.elastic.util.QueryType;
import java.util.List;
import java.util.Map;

public class QueryModelUtil {
  // Basic MATCH Query
  public static QueryModel createMatchQuery(String field, String value) {
    return new QueryModel(QueryType.MATCH, Map.of(FIELD, field, VALUE, value));
  }
  public static QueryModel getNlpSearchQuery(JsonArray embeddings) {
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(
        new QueryModel(QueryType.SCRIPT_SCORE, Map.of("query_vector", embeddings)));
    queryModel.setExcludeFields(List.of("_word_vector"));
    return queryModel;
  }

  public static QueryModel generateGeoScriptScoreQuery(JsonObject queryParams, JsonArray queryVector) {

    QueryModel boolQueryModel = new QueryModel(QueryType.BOOL);

    // Adding MatchQuery clauses for each of the fields based on the queryParams
    if (queryParams.containsKey(BOROUGH)) {
      boolQueryModel.addShouldQuery(
          createMatchQuery("_geosummary._geocoded.results.borough",
              queryParams.getString(BOROUGH)));
    }
    if (queryParams.containsKey(LOCALITY)) {
      boolQueryModel.addShouldQuery(createMatchQuery("_geosummary._geocoded.results.locality",
          queryParams.getString(LOCALITY)));
    }
    if (queryParams.containsKey(COUNTY)) {
      boolQueryModel.addShouldQuery(createMatchQuery("_geosummary._geocoded.results.county",
          queryParams.getString(COUNTY)));
    }
    if (queryParams.containsKey(REGION)) {
      boolQueryModel.addShouldQuery(createMatchQuery("_geosummary._geocoded.results.region",
          queryParams.getString(REGION)));
    }
    if (queryParams.containsKey(COUNTRY)) {
      boolQueryModel.addShouldQuery(createMatchQuery("_geosummary._geocoded.results.country",
          queryParams.getString(COUNTRY)));
    }

    // Set minimum_should_match to 1
    boolQueryModel.setMinimumShouldMatch("1");

    // Geo shape filter
    if (queryParams.containsKey(BBOX)) {
      JsonArray bboxCoords = queryParams.getJsonArray(BBOX);
      JsonArray coordinates =
          new JsonArray()
              .add(new JsonArray()
                  .add(bboxCoords.getFloat(0)) // minLon
                  .add(bboxCoords.getFloat(3))) // maxLat
              .add(new JsonArray()
                  .add(bboxCoords.getFloat(2)) // maxLon
                  .add(bboxCoords.getFloat(1))); // minLat
      boolQueryModel.addFilterQuery(
          new QueryModel(QueryType.GEO_SHAPE,
              Map.of(GEOPROPERTY, "location.geometry",
                  TYPE, GEO_BBOX,
                  COORDINATES, coordinates,
                  "relation", INTERSECTS)));
    }

    // Script score for cosine similarity
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(new QueryModel(
        QueryType.SCRIPT_SCORE,
        Map.of("query_vector", queryVector, "custom_query", boolQueryModel.toJson())));
    queryModel.setExcludeFields(List.of("_word_vector"));
    return queryModel;
  }
}
