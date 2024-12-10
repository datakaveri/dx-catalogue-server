package iudx.catalogue.server.database;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class QueryDecoder {

  private static final Logger LOGGER = LogManager.getLogger(QueryDecoder.class);

  private static JsonObject handleResponseFiltering(
      JsonObject request, String relationshipType, String elasticQuery) {
    Integer limit =
        request.getInteger(LIMIT, FILTER_PAGINATION_SIZE - request.getInteger(OFFSET, 0));
    JsonObject tempQuery = new JsonObject(elasticQuery).put(SIZE_KEY, limit.toString());

    if (TYPE_KEY.equals(relationshipType)) {
      tempQuery.put(SOURCE, TYPE_KEY);
    }

    /* checking the requests for limit attribute */
    if (request.containsKey(LIMIT)) {
      Integer sizeFilter = request.getInteger(LIMIT);
      tempQuery.put(SIZE_KEY, sizeFilter);
    }

    /* checking the requests for offset attribute */
    if (request.containsKey(OFFSET)) {
      Integer offsetFilter = request.getInteger(OFFSET);
      tempQuery.put(FROM, offsetFilter);
    }

    if (request.containsKey(FILTER)) {
      JsonArray sourceFilter = request.getJsonArray(FILTER, new JsonArray());
      tempQuery.put(SOURCE, sourceFilter);
    }
    return tempQuery;
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
    String queryGeoShape = null;
    Boolean match = false;

    if (searchType.equalsIgnoreCase("getParentObjectInfo")) {
      elasticQuery =
          new JsonObject(
              GET_DOC_QUERY
                  .replace("$1", request.getString(ID))
                  .replace(
                      "$2",
                      "\"type\",\"provider\",\"ownerUserId\","
                          + "\"resourceGroup\",\"resourceServer\","
                          + "\"resourceServerRegURL\", \"cos\", \"cos_admin\""));
      return elasticQuery;
    }

    /* TODO: Pagination for large result set */
    if (request.getBoolean(SEARCH)) {
      Integer limit =
          request.getInteger(LIMIT, FILTER_PAGINATION_SIZE - request.getInteger(OFFSET, 0));
      elasticQuery.put(SIZE_KEY, limit);
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
        int radius = request.getInteger(MAX_DISTANCE);
        String radiusStr = ",\"radius\": \"$1m\"".replace("$1", Integer.toString(radius));
        queryGeoShape = GEO_SHAPE_QUERY.replace("$1", GEO_CIRCLE)
            .replace("$2", coordinates.toString() + radiusStr).replace("$3", relation)
            .replace("$4", geoProperty + GEO_KEY);
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
        queryGeoShape = GEO_SHAPE_QUERY.replace("$1", geometry)
                .replace("$2", coordinates.toString())
            .replace("$3", relation).replace("$4", geoProperty + GEO_KEY);

      } else if (BBOX.equalsIgnoreCase(geometry)) {
        /* Construct the query for BBOX */
        relation = request.getString(GEORELATION);
        coordinates = request.getJsonArray(COORDINATES_KEY);
        queryGeoShape = GEO_SHAPE_QUERY
                .replace("$1", GEO_BBOX).replace("$2", coordinates.toString())
            .replace("$3", relation).replace("$4", geoProperty + GEO_KEY);
      } else {
        return new JsonObject().put(ERROR, new RespBuilder()
                    .withType(TYPE_INVALID_GEO_PARAM)
                    .withTitle(TITLE_INVALID_GEO_PARAM)
                .withDetail(DETAIL_INVALID_GEO_PARAMETER)
                .getJsonResponse());
      }
    }

    JsonArray mustQuery = new JsonArray();

    /* Construct the query for text based search */
    if (searchType.matches(TEXTSEARCH_REGEX)) {
      LOGGER.debug("Info: Text search block");

      match = true;
      /* validating tag search attributes */
      if (request.containsKey(Q_VALUE) && !request.getString(Q_VALUE).isBlank()) {
        /* constructing db queries */
        String textAttr = request.getString(Q_VALUE);
        String textQuery = TEXT_QUERY.replace("$1", textAttr);
        mustQuery.add(new JsonObject(textQuery));
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
            JsonArray shouldQuery = new JsonArray();
            JsonArray valueArray = valueAttrs.getJsonArray(i);
            for (int j = 0; j < valueArray.size(); j++) {
              String matchQuery;
              /* Attribute related queries using "match" and without the ".keyword" */
              if (propertyAttrs.getString(i).equals(TAGS)
                  || propertyAttrs.getString(i).equals(DESCRIPTION_ATTR)
                  || propertyAttrs.getString(i).startsWith(LOCATION)) {

                matchQuery = MATCH_QUERY.replace("$1", propertyAttrs.getString(i))
                                        .replace("$2", valueArray.getString(j));
                shouldQuery.add(new JsonObject(matchQuery));
                /* Attribute related queries using "match" and with the ".keyword" */
              } else {
                /* checking keyword in the query paramters */
                if (propertyAttrs.getString(i).endsWith(KEYWORD_KEY)) {
                  matchQuery = MATCH_QUERY.replace("$1", propertyAttrs.getString(i))
                                          .replace("$2", valueArray.getString(j));
                } else {

                  /* add keyword if not avaialble */
                  matchQuery = MATCH_QUERY.replace("$1", propertyAttrs.getString(i) + KEYWORD_KEY)
                                          .replace("$2", valueArray.getString(j));
                }
                shouldQuery.add(new JsonObject(matchQuery));
              }
            }
            mustQuery.add(new JsonObject(SHOULD_QUERY.replace("$1", shouldQuery.toString())));
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
      String instanceFilter = INSTANCE_FILTER.replace("$1", instanceId);
      LOGGER.debug("Info: Instance found in query;" + instanceFilter);
      mustQuery.add(new JsonObject(instanceFilter));
    }

    /* checking the requests for limit attribute */
    if (request.containsKey(LIMIT)) {
      Integer sizeFilter = request.getInteger(LIMIT);
      elasticQuery.put(SIZE_KEY, sizeFilter);
    }

    /* checking the requests for offset attribute */
    if (request.containsKey(OFFSET)) {
      Integer offsetFilter = request.getInteger(OFFSET);
      elasticQuery.put(FROM, offsetFilter);
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
        elasticQuery.put(SOURCE, sourceFilter);
      } else if (request.containsKey(FILTER)) {
        JsonArray sourceFilter = request.getJsonArray(FILTER);
        elasticQuery.put(SOURCE, sourceFilter);
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

      JsonObject boolQuery = new JsonObject(MUST_QUERY.replace("$1", mustQuery.toString()));
      /* return fully formed elastic query */
      if (queryGeoShape != null) {
        try {
          boolQuery.getJsonObject("bool").put(FILTER,
              new JsonArray().add(new JsonObject(queryGeoShape)));
        } catch (Exception e) {
          return new JsonObject().put(ERROR, new RespBuilder()
                      .withType(TYPE_INVALID_GEO_VALUE)
                      .withTitle(TITLE_INVALID_GEO_VALUE)
                      .withDetail(DETAIL_INVALID_COORDINATE_POLYGON)
                      .getJsonResponse());
        }
      }
      return elasticQuery.put(QUERY_KEY, boolQuery);
    }
  }

  /**
   * Decodes and constructs ElasticSearch Relationship queries based on the parameters passed in the
   * request.
   *
   * @param request Json object containing various fields related to query-type.
   * @return JsonObject which contains fully formed ElasticSearch query.
   */
  public String listRelationshipQuery(JsonObject request) {
    LOGGER.debug("request: " + request);

    String relationshipType = request.getString(RELATIONSHIP, "");
    String itemType = request.getString(ITEM_TYPE, "");
    String subQuery = "";

    /* Validating the request */
    if (request.containsKey(ID) && relationshipType.equalsIgnoreCase("cos")) {
      String cosId = request.getString(COS_ITEM);

      subQuery = TERM_QUERY.replace("$1", ID + KEYWORD_KEY).replace("$2", cosId);
    } else if (request.containsKey(ID) && itemType.equalsIgnoreCase(ITEM_TYPE_COS)) {
      String cosId = request.getString(ID);

      subQuery = TERM_QUERY.replace("$1", COS_ITEM + KEYWORD_KEY).replace("$2", cosId) + ",";
      switch (relationshipType) {
        case RESOURCE:
          subQuery =
              subQuery + TERM_QUERY.replace("$1", TYPE_KEYWORD).replace("$2", ITEM_TYPE_RESOURCE);
          break;
        case RESOURCE_GRP:
          subQuery =
              subQuery
                  + TERM_QUERY.replace("$1", TYPE_KEYWORD).replace("$2", ITEM_TYPE_RESOURCE_GROUP);
          break;
        case RESOURCE_SVR:
          subQuery =
              subQuery
                  + TERM_QUERY.replace("$1", TYPE_KEYWORD).replace("$2", ITEM_TYPE_RESOURCE_SERVER);
          break;
        case PROVIDER:
          subQuery =
              subQuery + TERM_QUERY.replace("$1", TYPE_KEYWORD).replace("$2", ITEM_TYPE_PROVIDER);
          break;
        default:
          return null;
      }
    } else if (request.containsKey(ID)
        && RESOURCE.equals(relationshipType)
        && itemType.equalsIgnoreCase(ITEM_TYPE_PROVIDER)) {
      String providerId = request.getString(ID);
      subQuery =
          TERM_QUERY.replace("$1", PROVIDER + KEYWORD_KEY).replace("$2", providerId)
              + ","
              + TERM_QUERY.replace("$1", TYPE_KEYWORD).replace("$2", ITEM_TYPE_RESOURCE);
    } else if (request.containsKey(ID)
        && RESOURCE.equals(relationshipType)
        && itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_GROUP)) {
      String resourceGroupId = request.getString(ID);

      subQuery =
          TERM_QUERY.replace("$1", RESOURCE_GRP + KEYWORD_KEY).replace("$2", resourceGroupId)
              + ","
              + TERM_QUERY.replace("$1", TYPE_KEYWORD).replace("$2", ITEM_TYPE_RESOURCE);
    } else if (request.containsKey(ID)
        && RESOURCE.equals(relationshipType)
        && itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_SERVER)) {
      String resourceServerId = request.getString(ID);

      subQuery =
          TERM_QUERY.replace("$1", RESOURCE_SVR + KEYWORD_KEY).replace("$2", resourceServerId)
              + ","
              + TERM_QUERY.replace("$1", TYPE_KEYWORD).replace("$2", ITEM_TYPE_RESOURCE);
    } else if (request.containsKey(ID)
        && RESOURCE_GRP.equals(relationshipType)
        && itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE)) {
      String resourceGroupId = request.getString("resourceGroup");
      subQuery =
          TERM_QUERY.replace("$1", ID_KEYWORD).replace("$2", resourceGroupId)
              + ","
              + TERM_QUERY.replace("$1", TYPE_KEYWORD).replace("$2", ITEM_TYPE_RESOURCE_GROUP);

    } else if (request.containsKey(ID)
        && RESOURCE_GRP.equals(relationshipType)
        && itemType.equalsIgnoreCase(ITEM_TYPE_PROVIDER)) {
      String providerId = request.getString(ID);
      subQuery =
          TERM_QUERY.replace("$1", PROVIDER + KEYWORD_KEY).replace("$2", providerId)
              + ","
              + TERM_QUERY.replace("$1", TYPE_KEYWORD).replace("$2", ITEM_TYPE_RESOURCE_GROUP);

    } else if (request.containsKey(ID)
        && RESOURCE_GRP.equals(relationshipType)
        && itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_SERVER)) {
      JsonArray providerIds = request.getJsonArray("providerIds");
      StringBuilder first = new StringBuilder(GET_RS1);
      List<String> ids =
          providerIds.stream()
              .map(JsonObject.class::cast)
              .map(providerId -> providerId.getString(ID))
              .collect(Collectors.toList());
      ids.forEach(id -> first.append(GET_RS2.replace("$1", id)));
      first.deleteCharAt(first.lastIndexOf(","));
      first.append(GET_RS3);
      return first.toString();
    } else if (request.containsKey(ID)
        && PROVIDER.equals(relationshipType)
        && itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_SERVER)) {
      String resourceServerId = request.getString(ID);

      subQuery =
          TERM_QUERY.replace("$1", RESOURCE_SVR + KEYWORD_KEY).replace("$2", resourceServerId)
              + ","
              + TERM_QUERY.replace("$1", TYPE_KEYWORD).replace("$2", ITEM_TYPE_PROVIDER);
    } else if (request.containsKey(ID) && PROVIDER.equals(relationshipType)) {
      // String id = request.getString(ID);
      String providerId = request.getString(PROVIDER);

      subQuery =
          TERM_QUERY.replace("$1", ID_KEYWORD).replace("$2", providerId)
              + ","
              + TERM_QUERY.replace("$1", TYPE_KEYWORD).replace("$2", ITEM_TYPE_PROVIDER);

    } else if (request.containsKey(ID) && RESOURCE_SVR.equals(relationshipType)) {
      String resourceServer = request.getString(RESOURCE_SVR);

      subQuery =
          MATCH_QUERY.replace("$1", ID_KEYWORD).replace("$2", resourceServer)
              + ","
              + TERM_QUERY.replace("$1", TYPE_KEYWORD).replace("$2", ITEM_TYPE_RESOURCE_SERVER);

    } else if (request.containsKey(ID) && TYPE_KEY.equals(relationshipType)) {
      /* parsing id from the request */
      String itemId = request.getString(ID);

      subQuery = TERM_QUERY.replace("$1", ID_KEYWORD).replace("$2", itemId);

    } else if (request.containsKey(ID) && ALL.equalsIgnoreCase(relationshipType)) {
      subQuery = MATCH_QUERY.replace("$1", ID_KEYWORD).replace("$2", request.getString(ID))
          + ",";
      if (request.containsKey(RESOURCE_GRP)) {
        subQuery =
            subQuery
            + MATCH_QUERY.replace("$1", ID_KEYWORD).replace("$2", request.getString(RESOURCE_GRP))
                + ",";
      }
      if (request.containsKey(PROVIDER)) {
        subQuery =
            subQuery
                + MATCH_QUERY.replace("$1", ID_KEYWORD).replace("$2", request.getString(PROVIDER))
                + ",";
      }
      if (request.containsKey(RESOURCE_SVR)) {

        subQuery =
            subQuery
                + MATCH_QUERY
                    .replace("$1", ID_KEYWORD)
                    .replace("$2", request.getString(RESOURCE_SVR))
                + ",";
      }
      if (request.containsKey(COS_ITEM)) {

        subQuery =
            subQuery
            + MATCH_QUERY
                .replace("$1", ID_KEYWORD)
                .replace("$2", request.getString(COS_ITEM));

      } else {
        subQuery = subQuery.substring(0, subQuery.length() - 1);
      }

      String elasticQuery = BOOL_SHOULD_QUERY.replace("$1", subQuery);
      JsonObject tempQuery = handleResponseFiltering(request, relationshipType, elasticQuery);
      return tempQuery.toString();
    } else {
      return null;
    }

    String elasticQuery = BOOL_MUST_QUERY.replace("$1", subQuery);
    JsonObject tempQuery = handleResponseFiltering(request, relationshipType, elasticQuery);
    return tempQuery.toString();
  }

  /**
   * Decodes and constructs Elastic query for listing items based on the parameters passed in the
   * request.
   *
   * @param request Json object containing various fields related to query-type.
   * @return JsonObject which contains fully formed ElasticSearch query.
   */
  public String listItemQuery(JsonObject request) {

    LOGGER.debug("Info: Reached list items;" + request.toString());
    String itemType = request.getString(ITEM_TYPE);
    String type = request.getString(TYPE_KEY);
    String instanceId = request.getString(INSTANCE);
    String elasticQuery = "";
    String tempQuery = "";

    if (itemType.equalsIgnoreCase(TAGS)) {
      if (instanceId == null || instanceId == "") {
        tempQuery = LIST_TAGS_QUERY;
      } else {
        tempQuery = LIST_INSTANCE_TAGS_QUERY.replace("$1", instanceId);
      }
    } else {
      if (instanceId == null || instanceId == "") {
        tempQuery = LIST_TYPES_QUERY.replace("$1", type);
      } else {
        tempQuery = LIST_INSTANCE_TYPES_QUERY.replace("$1", type).replace("$2", instanceId);
      }
    }

    Integer limit =
        request.getInteger(LIMIT, FILTER_PAGINATION_SIZE - request.getInteger(OFFSET, 0));
    elasticQuery = tempQuery.replace("$size", limit.toString());

    return elasticQuery;
  }
}
