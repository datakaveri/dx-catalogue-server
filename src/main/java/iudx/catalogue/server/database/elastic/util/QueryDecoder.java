package iudx.catalogue.server.database.elastic.util;

import static iudx.catalogue.server.database.elastic.util.Constants.*;
import static iudx.catalogue.server.database.elastic.util.Constants.KEYWORD_KEY;
import static iudx.catalogue.server.database.elastic.util.QueryType.BOOL;
import static iudx.catalogue.server.database.elastic.util.QueryType.GEO_SHAPE;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.common.RespBuilder;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class QueryDecoder {

  private static final Logger LOGGER = LogManager.getLogger(QueryDecoder.class);

  private static QueryModel handleResponseFiltering(
      JsonObject request, String relationshipType, QueryModel elasticQueryModel) {
    Integer limit =
        request.getInteger(LIMIT, FILTER_PAGINATION_SIZE - request.getInteger(OFFSET, 0));
    elasticQueryModel.setLimit(limit.toString());

    if (TYPE_KEY.equals(relationshipType)) {
      elasticQueryModel.setIncludeFields(List.of(TYPE_KEY));
    }

    /* checking the requests for limit attribute */
    if (request.containsKey(LIMIT)) {
      Integer sizeFilter = request.getInteger(LIMIT);
      elasticQueryModel.setLimit(sizeFilter.toString());
    }

    /* checking the requests for offset attribute */
    if (request.containsKey(OFFSET)) {
      Integer offsetFilter = request.getInteger(OFFSET);
      elasticQueryModel.setOffset(offsetFilter.toString());
    }

    if (request.containsKey(FILTER)) {
      JsonArray sourceFilter = request.getJsonArray(FILTER, new JsonArray());
      elasticQueryModel.setIncludeFields(sourceFilter.getList());
    }
    return elasticQueryModel;
  }

  /**
   * Decodes and constructs ElasticSearch Search/Count query based on the parameters passed in the
   * request.
   *
   * @param request Json object containing various fields related to query-type.
   * @return JsonObject which contains fully formed ElasticSearch query.
   */
  public JsonObject searchQuery(JsonObject request) {

    String searchType = request.getString(SEARCH_TYPE);
    JsonObject elasticQuery = new JsonObject();
    QueryModel geoShapeQueryModel = null;
    QueryModel tempQueryModel = new QueryModel(BOOL);
    boolean match = false;

    if (searchType.equalsIgnoreCase("getParentObjectInfo")) {
      QueryModel getDocQueryModel = new QueryModel();
      Map<String, Object> termParams = new HashMap<>();
      termParams.put(FIELD, ID_KEYWORD);
      termParams.put(VALUE, request.getString(ID));
      getDocQueryModel.setQueries(new QueryModel(QueryType.TERM, termParams));
      getDocQueryModel.setIncludeFields(
          List.of("type", "provider", "ownerUserId", "resourceGroup", "resourceServer",
              "resourceServerRegURL", "cos", "cos_admin"));
      elasticQuery.put(QUERY_KEY, getDocQueryModel);
      return elasticQuery;
    }

    /* Handle the search type */
    if (searchType.matches(GEOSEARCH_REGEX)) {
      LOGGER.debug("Info: Geo search block");

      match = true;
      String relation;
      JsonArray coordinates;
      String geometry = request.getString(GEOMETRY);
      String geoProperty = request.getString(GEOPROPERTY);
      /* Construct the search query */
      if (POINT.equalsIgnoreCase(geometry)) {
        /* Construct the query for Circle */
        coordinates = request.getJsonArray(COORDINATES_KEY);
        relation = request.getString(GEORELATION);
        String radius = Integer.toString(request.getInteger(MAX_DISTANCE));

        Map<String, Object> geoParams = new HashMap<>();
        geoParams.put(TYPE, GEO_CIRCLE);
        geoParams.put(COORDINATES, coordinates);
        geoParams.put("radius", radius + "m");
        geoParams.put("relation", relation);
        geoParams.put(GEOPROPERTY, geoProperty + GEO_KEY);
        geoShapeQueryModel = new QueryModel();
        geoShapeQueryModel.setQueryType(GEO_SHAPE);
        geoShapeQueryModel.setQueryParameters(geoParams);

      } else if (POLYGON.equalsIgnoreCase(geometry) || LINESTRING.equalsIgnoreCase(geometry)) {
        relation = request.getString(GEORELATION);
        coordinates = request.getJsonArray(COORDINATES_KEY);
        int length = coordinates.getJsonArray(0).size();
        /* Check if valid polygon */
        if (geometry.equalsIgnoreCase(POLYGON)
            && (!coordinates.getJsonArray(0).getJsonArray(0).getDouble(0)
            .equals(coordinates.getJsonArray(0).getJsonArray(length - 1).getDouble(0))
            || !coordinates.getJsonArray(0).getJsonArray(0).getDouble(1)
            .equals(coordinates.getJsonArray(0).getJsonArray(length - 1).getDouble(1)))) {

          return new JsonObject().put(ERROR, new RespBuilder()
              .withType(TYPE_INVALID_GEO_VALUE)
              .withTitle(TITLE_INVALID_GEO_VALUE)
              .withDetail(DETAIL_INVALID_COORDINATE_POLYGON)
              .getJsonResponse());
        }
        Map<String, Object> geoParams = new HashMap<>();
        geoParams.put(TYPE, geometry);
        geoParams.put(COORDINATES, coordinates);
        geoParams.put("relation", relation);
        geoParams.put(GEOPROPERTY, geoProperty + GEO_KEY);
        geoShapeQueryModel = new QueryModel();
        geoShapeQueryModel.setQueryType(GEO_SHAPE);
        geoShapeQueryModel.setQueryParameters(geoParams);

      } else if (BBOX.equalsIgnoreCase(geometry)) {
        /* Construct the query for BBOX */
        relation = request.getString(GEORELATION);
        coordinates = request.getJsonArray(COORDINATES_KEY);
        Map<String, Object> geoParams = new HashMap<>();
        geoParams.put(TYPE, GEO_BBOX);
        geoParams.put(COORDINATES, coordinates);
        geoParams.put("relation", relation);
        geoParams.put(GEOPROPERTY, geoProperty + GEO_KEY);
        geoShapeQueryModel = new QueryModel();
        geoShapeQueryModel.setQueryType(GEO_SHAPE);
        geoShapeQueryModel.setQueryParameters(geoParams);

      } else {
        return new JsonObject().put(ERROR, new RespBuilder()
            .withType(TYPE_INVALID_GEO_PARAM)
            .withTitle(TITLE_INVALID_GEO_PARAM)
            .withDetail(DETAIL_INVALID_GEO_PARAMETER)
            .getJsonResponse());
      }
    }

    /* Construct the query for text based search */
    if (searchType.matches(TEXTSEARCH_REGEX)) {
      LOGGER.debug("Info: Text search block");

      match = true;
      /* validating tag search attributes */
      if (request.containsKey(Q_VALUE) && !request.getString(Q_VALUE).isBlank()) {
        /* constructing db queries */
        String textAttr = request.getString(Q_VALUE);

        QueryModel textQueryModel = new QueryModel(QueryType.TEXT, Map.of(Q_VALUE, textAttr));
        tempQueryModel.addMustQuery(textQueryModel);
      } else {
        return new JsonObject().put(ERROR, new RespBuilder()
            .withType(TYPE_BAD_TEXT_QUERY)
            .withTitle(TITLE_BAD_TEXT_QUERY)
            .withDetail("bad text query values")
            .getJsonResponse());
      }
    }

    /* Construct the query for attribute based search */
    if (searchType.matches(ATTRIBUTE_SEARCH_REGEX)) {
      LOGGER.debug("Info: Attribute search block");

      match = true;
      /* validating tag search attributes */
      if (request.containsKey(PROPERTY) && !request.getJsonArray(PROPERTY).isEmpty()
          && request.containsKey(VALUE) && !request.getJsonArray(VALUE).isEmpty()) {
        /* fetching values from request */
        JsonArray propertyAttrs = request.getJsonArray(PROPERTY);
        JsonArray valueAttrs = request.getJsonArray(VALUE);
        /* For attribute property and values search */
        if (propertyAttrs.size() == valueAttrs.size()) {
          /* Mapping and constructing the value attributes with the property attributes for query */
          for (int i = 0; i < valueAttrs.size(); i++) {
            QueryModel shouldQueryModel = new QueryModel(BOOL);
            JsonArray valueArray = valueAttrs.getJsonArray(i);
            for (int j = 0; j < valueArray.size(); j++) {
              QueryModel matchQueryModel = new QueryModel(QueryType.MATCH);
              /* Attribute related queries using "match" and without the ".keyword" */
              if (propertyAttrs.getString(i).equals(TAGS)
                  || propertyAttrs.getString(i).equals(DESCRIPTION_ATTR)
                  || propertyAttrs.getString(i).startsWith(LOCATION)) {

                matchQueryModel.setQueryParameters(Map.of(FIELD, propertyAttrs.getString(i),
                    VALUE, valueArray.getString(j)));

                try {
                  shouldQueryModel.addShouldQuery(matchQueryModel);
                } catch (Exception e) {
                  LOGGER.debug("Error: " + e.getLocalizedMessage());
                }
                /* Attribute related queries using "match" and with the ".keyword" */
              } else {
                /* checking keyword in the query paramters */
                if (propertyAttrs.getString(i).endsWith(KEYWORD_KEY)) {
                  matchQueryModel.setQueryParameters(Map.of(FIELD, propertyAttrs.getString(i),
                      VALUE, valueArray.getString(j)));
                } else {

                  /* add keyword if not avaialble */
                  matchQueryModel.setQueryParameters(Map.of(FIELD,
                      propertyAttrs.getString(i) + KEYWORD_KEY, VALUE, valueArray.getString(j)));
                }
                shouldQueryModel.addShouldQuery(matchQueryModel);
              }
            }
            tempQueryModel.addMustQuery(shouldQueryModel);
          }
        } else {
          return new JsonObject().put(ERROR, new RespBuilder()
              .withType(TYPE_INVALID_PROPERTY_VALUE)
              .withTitle(TITLE_INVALID_PROPERTY_VALUE)
              .withDetail("Invalid Property Value")
              .getJsonResponse());
        }
      }
    }

    /* Will be used for multi-tenancy */
    String instanceId = request.getString(INSTANCE);

    if (instanceId != null) {
      QueryModel matchQueryModel = new QueryModel(QueryType.MATCH, Map.of(INSTANCE, instanceId));
      LOGGER.debug("Info: Instance found in query;" + matchQueryModel.toJson());
      tempQueryModel.addMustQuery(matchQueryModel);
    }

    QueryModel queryModel = new QueryModel();

    /* checking the requests for limit attribute */
    if (request.containsKey(LIMIT)) {
      Integer sizeFilter = request.getInteger(LIMIT);
      queryModel.setLimit(String.valueOf(sizeFilter));
    }

    /* checking the requests for offset attribute */
    if (request.containsKey(OFFSET)) {
      Integer offsetFilter = request.getInteger(OFFSET);
      queryModel.setOffset(String.valueOf(offsetFilter));
    }

    /* TODO: Pagination for large result set */
    if (request.getBoolean(SEARCH)) {
      Integer limit =
          request.getInteger(LIMIT, FILTER_PAGINATION_SIZE - request.getInteger(OFFSET, 0));
      queryModel.setLimit(String.valueOf(limit));
    }

    if (searchType.matches(RESPONSE_FILTER_REGEX)) {

      /* Construct the filter for response */
      LOGGER.debug("Info: Adding responseFilter");
      match = true;

      if (!request.getBoolean(SEARCH)) {
        return new JsonObject().put(ERROR, new RespBuilder()
            .withType(TYPE_OPERATION_NOT_ALLOWED)
            .withTitle(TITLE_OPERATION_NOT_ALLOWED)
            .withDetail("operation not allowed")
            .getJsonResponse());
      }

      if (request.containsKey(ATTRIBUTE)) {
        JsonArray sourceFilter = request.getJsonArray(ATTRIBUTE);
        queryModel.setIncludeFields(sourceFilter.getList());
      } else if (request.containsKey(FILTER)) {
        JsonArray sourceFilter = request.getJsonArray(FILTER);
        queryModel.setIncludeFields(sourceFilter.getList());
      } else {
        return new JsonObject().put(ERROR, new RespBuilder()
            .withType(TYPE_BAD_FILTER)
            .withTitle(TITLE_BAD_FILTER)
            .withDetail("bad filters applied")
            .getJsonResponse());
      }
    }

    if (!match) {
      return new JsonObject().put(ERROR, new RespBuilder()
          .withType(TYPE_INVALID_SYNTAX)
          .withTitle(TITLE_INVALID_SYNTAX)
          .withDetail("Invalid Syntax")
          .getJsonResponse());
    } else {

      /* return fully formed elastic query */
      queryModel.setQueries(tempQueryModel);
      if (geoShapeQueryModel != null) {
        try {
          List<QueryModel> filterQueries = List.of(geoShapeQueryModel);
          if (queryModel.getQueries() == null) {
            queryModel.setQueries(new QueryModel(BoolOperator.FILTER, filterQueries));
          } else {
            queryModel.getQueries().setFilterQueries(filterQueries);
          }

        } catch (Exception e) {
          return new JsonObject().put(ERROR, new RespBuilder()
              .withType(TYPE_INVALID_GEO_VALUE)
              .withTitle(TITLE_INVALID_GEO_VALUE)
              .withDetail(DETAIL_INVALID_COORDINATE_POLYGON)
              .getJsonResponse());
        }
      }
      elasticQuery.put(QUERY_KEY, queryModel);
      return elasticQuery;
    }
  }

  /**
   * Decodes and constructs ElasticSearch Relationship queries based on the parameters passed in the
   * request.
   *
   * @param request Json object containing various fields related to query-type.
   * @return JsonObject which contains fully formed ElasticSearch queryModel.
   */

  public QueryModel listRelationshipQueryModel(JsonObject request) {
    LOGGER.debug("request: " + request);

    String relationshipType = request.getString(RELATIONSHIP, "");
    String itemType = request.getString(ITEM_TYPE, "");
    QueryModel queryModel = new QueryModel(BOOL);

    /* Validating the request */
    if (request.containsKey(ID) && relationshipType.equalsIgnoreCase("cos")) {
      String cosId = request.getString(COS_ITEM);

      queryModel.addMustQuery(
          new QueryModel(QueryType.TERM, Map.of(FIELD, ID_KEYWORD, VALUE, cosId)));
    } else if (request.containsKey(ID) && itemType.equalsIgnoreCase(ITEM_TYPE_COS)) {
      String cosId = request.getString(ID);

      queryModel.addMustQuery(
          new QueryModel(QueryType.TERM, Map.of(FIELD, COS_ITEM + KEYWORD_KEY, VALUE, cosId)));
      switch (relationshipType) {
        case RESOURCE:

          queryModel.addMustQuery(new QueryModel(QueryType.TERM,
              Map.of(FIELD, TYPE_KEYWORD, VALUE, ITEM_TYPE_RESOURCE)));
          break;
        case RESOURCE_GRP:

          queryModel.addMustQuery(new QueryModel(QueryType.TERM,
              Map.of(FIELD, TYPE_KEYWORD, VALUE, ITEM_TYPE_RESOURCE_GROUP)));
          break;
        case RESOURCE_SVR:

          queryModel.addMustQuery(new QueryModel(QueryType.TERM,
              Map.of(FIELD, TYPE_KEYWORD, VALUE, ITEM_TYPE_RESOURCE_SERVER)));
          break;
        case PROVIDER:

          queryModel.addMustQuery(new QueryModel(QueryType.TERM,
              Map.of(FIELD, TYPE_KEYWORD, VALUE, ITEM_TYPE_PROVIDER)));
          break;
        default:
          return null;
      }
    } else if (request.containsKey(ID)
        && RESOURCE.equals(relationshipType)
        && itemType.equalsIgnoreCase(ITEM_TYPE_PROVIDER)) {
      String providerId = request.getString(ID);

      queryModel.addMustQuery(
          new QueryModel(QueryType.TERM, Map.of(FIELD, PROVIDER + KEYWORD_KEY, VALUE, providerId)));
      queryModel.addMustQuery(
          new QueryModel(QueryType.TERM, Map.of(FIELD, TYPE_KEYWORD, VALUE, ITEM_TYPE_RESOURCE)));
    } else if (request.containsKey(ID)
        && RESOURCE.equals(relationshipType)
        && itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_GROUP)) {
      String resourceGroupId = request.getString(ID);

      queryModel.addMustQuery(new QueryModel(QueryType.TERM,
          Map.of(FIELD, RESOURCE_GRP + KEYWORD_KEY, VALUE, resourceGroupId)));
      queryModel.addMustQuery(
          new QueryModel(QueryType.TERM, Map.of(FIELD, TYPE_KEYWORD, VALUE, ITEM_TYPE_RESOURCE)));
    } else if (request.containsKey(ID)
        && RESOURCE.equals(relationshipType)
        && itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_SERVER)) {
      String resourceServerId = request.getString(ID);

      queryModel.addMustQuery(new QueryModel(QueryType.TERM,
          Map.of(FIELD, RESOURCE_SVR + KEYWORD_KEY, VALUE, resourceServerId)));
      queryModel.addMustQuery(
          new QueryModel(QueryType.TERM, Map.of(FIELD, TYPE_KEYWORD, VALUE, ITEM_TYPE_RESOURCE)));
    } else if (request.containsKey(ID)
        && RESOURCE_GRP.equals(relationshipType)
        && itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE)) {
      String resourceGroupId = request.getString("resourceGroup");

      queryModel.addMustQuery(
          new QueryModel(QueryType.TERM, Map.of(FIELD, ID_KEYWORD, VALUE, resourceGroupId)));
      queryModel.addMustQuery(new QueryModel(QueryType.TERM,
          Map.of(FIELD, TYPE_KEYWORD, VALUE, ITEM_TYPE_RESOURCE_GROUP)));

    } else if (request.containsKey(ID)
        && RESOURCE_GRP.equals(relationshipType)
        && itemType.equalsIgnoreCase(ITEM_TYPE_PROVIDER)) {
      String providerId = request.getString(ID);

      queryModel.addMustQuery(new QueryModel(QueryType.TERM, Map.of(FIELD, PROVIDER + KEYWORD_KEY,
          VALUE, providerId)));
      queryModel.addMustQuery(new QueryModel(QueryType.TERM, Map.of(FIELD, TYPE_KEYWORD, VALUE,
          ITEM_TYPE_RESOURCE_GROUP)));

    } else if (request.containsKey(ID)
        && RESOURCE_GRP.equals(relationshipType)
        && itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_SERVER)) {
      JsonArray providerIds = request.getJsonArray("providerIds");
      List<String> ids =
          providerIds.stream()
              .map(JsonObject.class::cast)
              .map(providerId -> providerId.getString(ID))
              .toList();
      ids.forEach(id -> queryModel.addShouldQuery(new QueryModel(QueryType.MATCH,
          Map.of(FIELD, PROVIDER + KEYWORD_KEY, VALUE, id))));
      queryModel.setMinimumShouldMatch("1");
      return queryModel;
    } else if (request.containsKey(ID)
        && PROVIDER.equals(relationshipType)
        && itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_SERVER)) {
      String resourceServerId = request.getString(ID);

      queryModel.addMustQuery(new QueryModel(QueryType.TERM, Map.of(FIELD,
          RESOURCE_SVR + KEYWORD_KEY, VALUE, resourceServerId)));
      queryModel.addMustQuery(new QueryModel(QueryType.TERM, Map.of(FIELD, TYPE_KEYWORD,
          VALUE, ITEM_TYPE_PROVIDER)));
    } else if (request.containsKey(ID) && PROVIDER.equals(relationshipType)) {
      String providerId = request.getString(PROVIDER);

      queryModel.addMustQuery(new QueryModel(QueryType.TERM, Map.of(FIELD, ID_KEYWORD, VALUE,
          providerId)));
      queryModel.addMustQuery(new QueryModel(QueryType.TERM, Map.of(FIELD, TYPE_KEYWORD, VALUE,
          ITEM_TYPE_PROVIDER)));
    } else if (request.containsKey(ID) && RESOURCE_SVR.equals(relationshipType)) {
      String resourceServer = request.getString(RESOURCE_SVR);

      queryModel.addMustQuery(new QueryModel(QueryType.MATCH, Map.of(FIELD,
          ID_KEYWORD, VALUE, resourceServer)));
      queryModel.addMustQuery(new QueryModel(QueryType.TERM,
          Map.of(FIELD, TYPE_KEYWORD, VALUE, ITEM_TYPE_RESOURCE_SERVER)));
    } else if (request.containsKey(ID) && TYPE_KEY.equals(relationshipType)) {
      /* parsing id from the request */
      String itemId = request.getString(ID);

      queryModel.addMustQuery(
          new QueryModel(QueryType.TERM, Map.of(FIELD, ID_KEYWORD, VALUE, itemId)));

    } else if (request.containsKey(ID) && ALL.equalsIgnoreCase(relationshipType)) {

      queryModel.addShouldQuery(
          new QueryModel(QueryType.MATCH, Map.of(FIELD, ID_KEYWORD, VALUE, request.getString(ID))));
      if (request.containsKey(RESOURCE_GRP)) {
        queryModel.addShouldQuery(new QueryModel(QueryType.MATCH,
            Map.of(FIELD, ID_KEYWORD, VALUE, request.getString(RESOURCE_GRP))));
      }
      if (request.containsKey(PROVIDER)) {

        queryModel.addShouldQuery(new QueryModel(QueryType.MATCH,
            Map.of(FIELD, ID_KEYWORD, VALUE, request.getString(PROVIDER))));
      }
      if (request.containsKey(RESOURCE_SVR)) {

        queryModel.addShouldQuery(new QueryModel(QueryType.MATCH,
            Map.of(FIELD, ID_KEYWORD, VALUE, request.getString(RESOURCE_SVR))));
      }
      if (request.containsKey(COS_ITEM)) {

        queryModel.addShouldQuery(new QueryModel(QueryType.MATCH,
            Map.of(FIELD, ID_KEYWORD, VALUE, request.getString(COS_ITEM))));

      }
      return handleResponseFiltering(request, relationshipType, queryModel);
    } else {
      return null;
    }
    return handleResponseFiltering(request, relationshipType, queryModel);
  }

  /**
   * Decodes and constructs Elastic query for listing items based on the parameters passed in the
   * request.
   *
   * @param request Json object containing various fields related to query-type.
   * @return JsonObject which contains fully formed ElasticSearch query model.
   */

  public QueryModel listItemQueryModel(JsonObject request) {

    LOGGER.debug("Info: Reached list items;" + request.toString());
    String itemType = request.getString(ITEM_TYPE);
    String type = request.getString(TYPE_KEY);
    String instanceId = request.getString(INSTANCE);
    QueryModel tempQueryModel;
    Integer limit =
        request.getInteger(LIMIT, FILTER_PAGINATION_SIZE - request.getInteger(OFFSET, 0));

    if (itemType.equalsIgnoreCase(TAGS)) {
      if (instanceId == null || instanceId == "") {
        Map<String, Object> termsAggParams = new HashMap<>();
        termsAggParams.put(FIELD, TAGS + KEYWORD_KEY);
        termsAggParams.put(SIZE_KEY, limit);

        QueryModel aggs = new QueryModel(AggregationType.TERMS, termsAggParams);
        aggs.setAggregationName(RESULTS);
        tempQueryModel = new QueryModel();
        tempQueryModel.setAggregations(List.of(aggs));
      } else {
        Map<String, Object> termsAggParams = new HashMap<>();
        termsAggParams.put(FIELD, TAGS + KEYWORD_KEY);
        termsAggParams.put(SIZE_KEY, limit);
        QueryModel aggs = new QueryModel(AggregationType.TERMS, termsAggParams);
        aggs.setAggregationName(RESULTS);

        QueryModel instanceIdTermQuery = new QueryModel(QueryType.TERM,
            Map.of(FIELD, INSTANCE + KEYWORD_KEY, VALUE, instanceId));
        QueryModel queryModel = new QueryModel(BoolOperator.FILTER, List.of(instanceIdTermQuery));
        tempQueryModel = new QueryModel(queryModel, List.of(aggs));
      }
    } else {
      if (instanceId == null || instanceId == "") {
        Map<String, Object> termsAggParams = new HashMap<>();
        termsAggParams.put(FIELD, ID_KEYWORD);
        termsAggParams.put(SIZE_KEY, limit);
        QueryModel aggs = new QueryModel(AggregationType.TERMS, termsAggParams);
        aggs.setAggregationName(RESULTS);

        QueryModel typeMatchQuery = new QueryModel(QueryType.MATCH,
            Map.of(FIELD, TYPE, VALUE, type));
        QueryModel queryModel = new QueryModel(BoolOperator.FILTER, List.of(typeMatchQuery));
        tempQueryModel = new QueryModel(queryModel, List.of(aggs));
      } else {
        Map<String, Object> termsAggParams = new HashMap<>();
        termsAggParams.put(FIELD, ID_KEYWORD);
        termsAggParams.put(SIZE_KEY, limit);
        QueryModel aggs = new QueryModel(AggregationType.TERMS, termsAggParams);
        aggs.setAggregationName(RESULTS);

        QueryModel instanceIdTermQuery = new QueryModel(QueryType.TERM,
            Map.of(FIELD, INSTANCE + KEYWORD_KEY, VALUE, instanceId));
        QueryModel typeMatchQuery = new QueryModel(QueryType.MATCH,
            Map.of(FIELD, TYPE, VALUE, type));
        QueryModel queryModel =
            new QueryModel(BoolOperator.FILTER, List.of(typeMatchQuery, instanceIdTermQuery));
        tempQueryModel = new QueryModel(queryModel, List.of(aggs));
      }
    }
    return tempQueryModel;
  }
}
