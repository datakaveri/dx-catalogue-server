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
    JsonArray mustQuery = new JsonArray();
    JsonArray mustNotQuery = new JsonArray();
    Boolean match = false;

    if (searchType.equalsIgnoreCase("getParentObjectInfo")) {
      elasticQuery =
          new JsonObject(
              GET_DOC_QUERY
                  .replace("$1", request.getString(ID))
                  .replace(
                      "$2",
                      "\"type\",\"provider\",\"ownerUserId\","
                          + "\"resourceGroup\",\"name\",\"organizationId\", \"shortDescription\","
                          + "\"resourceServer\","
                          + "\"resourceServerRegURL\", \"cos\", \"cos_admin\""));
      return elasticQuery;
    }

    /* TODO: Pagination for large result set */
    if (request.getBoolean(SEARCH)) {
      Integer limit =
          request.getInteger(LIMIT, DEFAULT_MAX_PAGE_SIZE - request.getInteger(OFFSET, 0));
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

    /* Construct the query for text based search */
    if (searchType.matches(TEXTSEARCH_REGEX)) {
      LOGGER.debug("Info: Text search block");

      match = true;
      /* validating tag search attributes */
      if (request.containsKey(Q_VALUE) && !request.getString(Q_VALUE).isBlank()) {
        /* constructing db queries */
        JsonArray shouldQuery = new JsonArray();
        String textAttr = request.getString(Q_VALUE);
        boolean isFuzzy = request.containsKey(FUZZY) && "true".equals(request.getString(FUZZY));
        boolean isAutoComplete =
            request.containsKey(AUTO_COMPLETE) &&  "true".equals(request.getString(AUTO_COMPLETE));
        if (isFuzzy) {
          String textQuery = TEXT_QUERY_FUZZY.replace("$1", textAttr);

          shouldQuery.add(new JsonObject(textQuery));
        }
        if (isAutoComplete) {
          String textQuery = TEXT_QUERY_AUTO_COMPLETE.replace("$1", textAttr);

          shouldQuery.add(new JsonObject(textQuery));
        }
        if (!isFuzzy && !isAutoComplete) {
          String textQuery = TEXT_QUERY.replace("$1", textAttr);

          shouldQuery.add(new JsonObject(textQuery));
        }
        mustQuery.add(new JsonObject(SHOULD_QUERY.replace("$1", shouldQuery.toString())));
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

                if (request.containsKey(FUZZY)
                    && "true".equals(request.getString(FUZZY))
                    && (TAGS.equals(propertyAttrs.getString(i))
                    || DESCRIPTION_ATTR.equals(propertyAttrs.getString(i)))) {
                  matchQuery = FUZZY_MATCH_QUERY
                      .replace("$1", propertyAttrs.getString(i))
                      .replace("$2", valueArray.getString(j));
                  shouldQuery.add(new JsonObject(matchQuery));
                }

                /* Attribute related queries using "match" and with the ".keyword" */
              } else {
                /* checking keyword in the query paramters */
                if (propertyAttrs.getString(i).endsWith(KEYWORD_KEY)) {
                  matchQuery = MATCH_QUERY.replace("$1", propertyAttrs.getString(i))
                      .replace("$2", valueArray.getString(j));
                } else {

                  /* add keyword if not avaialble */
                  matchQuery = MATCH_QUERY.replace("$1",
                          propertyAttrs.getString(i) + KEYWORD_KEY)
                      .replace("$2", valueArray.getString(j));
                }
                shouldQuery.add(new JsonObject(matchQuery));
                if ("true".equals(request.getString(FUZZY))
                    && (LABEL.equals(propertyAttrs.getString(i))
                    || INSTANCE.equals(propertyAttrs.getString(i)))) {
                  matchQuery = FUZZY_MATCH_QUERY
                      .replace("$1", propertyAttrs.getString(i))
                      .replace("$2", valueArray.getString(j));
                  shouldQuery.add(new JsonObject(matchQuery));
                }

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

    /* Construct the query for post request for attribute based search */
    if (searchType.matches(SEARCH_CRITERIA_REGEX)) {
      LOGGER.debug("Info: Search Criteria search block; {}", request);

      match = true;

      if (request.containsKey(SEARCH_CRITERIA_KEY)
          && !request.getJsonArray(SEARCH_CRITERIA_KEY).isEmpty()) {
        JsonArray searchCriteriaArray = request.getJsonArray(SEARCH_CRITERIA_KEY);

        for (int i = 0; i < searchCriteriaArray.size(); i++) {
          JsonObject criterion = searchCriteriaArray.getJsonObject(i);
          String field = criterion.getString(FIELD);
          JsonArray values = criterion.getJsonArray(VALUES);
          String criterionSearchType = criterion.getString(SEARCH_TYPE, TERM);

          switch (criterionSearchType) {
            case TERM: {
              JsonArray shouldQuery = new JsonArray();
              for (int j = 0; j < values.size(); j++) {
                String value = values.getString(j);
                String matchQuery;

                // raw field matches (e.g. tags, description, location)
                if (DESCRIPTION_ATTR.equals(field)
                    || field.startsWith(LOCATION)) {
                  matchQuery = MATCH_QUERY.replace("$1", field).replace("$2", value);
                  shouldQuery.add(new JsonObject(matchQuery));

                  if ("true".equals(request.getString(FUZZY))
                      && DESCRIPTION_ATTR.equals(field)) {
                    matchQuery = FUZZY_MATCH_QUERY.replace("$1", field)
                        .replace("$2", value);
                    shouldQuery.add(new JsonObject(matchQuery));
                  }
                } else if (TAGS.equals(field)) {
                  matchQuery = MATCH_PHRASE_QUERY.replace("$1", field).replace("$2", value);
                  shouldQuery.add(new JsonObject(matchQuery));

                  if ("true".equals(request.getString(FUZZY))) {
                    matchQuery = FUZZY_MATCH_QUERY.replace("$1", field)
                        .replace("$2", value);
                    shouldQuery.add(new JsonObject(matchQuery));
                  }
                } else if (FILE_FORMAT.equals(field)) {
                  // Use wildcard query for case-insensitive match
                  JsonObject wildcardQuery = new JsonObject()
                      .put(WILDCARD_KEY, new JsonObject()
                          .put(field + KEYWORD_KEY, new JsonObject()
                              .put(VALUE, value.toLowerCase())
                              .put(CASE_INSENSITIVE, true)));
                  shouldQuery.add(wildcardQuery);
                } else {
                  // fallback to .keyword match
                  if (field.endsWith(KEYWORD_KEY)) {
                    matchQuery = MATCH_QUERY.replace("$1", field)
                        .replace("$2", value);
                  } else {
                    matchQuery = MATCH_QUERY.replace("$1", field + KEYWORD_KEY)
                        .replace("$2", value);
                  }
                  shouldQuery.add(new JsonObject(matchQuery));

                  if ("true".equals(request.getString(FUZZY))
                      && (LABEL.equals(field) || INSTANCE.equals(field))) {
                    matchQuery = FUZZY_MATCH_QUERY.replace("$1", field)
                        .replace("$2", value);
                    shouldQuery.add(new JsonObject(matchQuery));
                  }
                }
              }
              mustQuery.add(new JsonObject(SHOULD_QUERY.replace("$1", shouldQuery.toString())));
              break;
            }

            case BETWEEN_RANGE:
            case BEFORE_RANGE:
            case AFTER_RANGE:
            case BETWEEN_TEMPORAL:
            case BEFORE_TEMPORAL:
            case AFTER_TEMPORAL: {
              JsonObject rangeObj = new JsonObject();
              JsonObject rangeFieldObj = new JsonObject();

              if (criterionSearchType.startsWith(BETWEEN)) {
                if (values.size() == 2) {
                  rangeFieldObj.put(GREATER_THAN_EQUALS, values.getValue(0));
                  rangeFieldObj.put(LESS_THAN_EQUALS, values.getValue(1));
                } else {
                  return new JsonObject().put(ERROR, new RespBuilder()
                      .withType(TYPE_INVALID_PROPERTY_VALUE)
                      .withTitle(TITLE_INVALID_PROPERTY_VALUE)
                      .withDetail("Expected 2 values for between-type search")
                      .getJsonResponse());
                }
              } else if (criterionSearchType.startsWith(BEFORE)) {
                rangeFieldObj.put(LESS_THAN, values.getValue(0));
              } else {
                rangeFieldObj.put(GREATER_THAN, values.getValue(0));
              }

              rangeObj.put(field, rangeFieldObj);
              mustQuery.add(new JsonObject().put(RANGE, rangeObj));
              break;
            }

            default: {
              return new JsonObject().put(ERROR, new RespBuilder()
                  .withType(TYPE_INVALID_PROPERTY_VALUE)
                  .withTitle(TITLE_INVALID_PROPERTY_VALUE)
                  .withDetail("Unsupported searchType: " + criterionSearchType)
                  .getJsonResponse());
            }
          }
        }
      } else {
        return new JsonObject().put(ERROR, new RespBuilder()
            .withType(TYPE_INVALID_PROPERTY_VALUE)
            .withTitle(TITLE_INVALID_PROPERTY_VALUE)
            .withDetail("Invalid Property Value: Empty searchCriteria")
            .getJsonResponse());
      }
    }

    /* Will be used for multi-tenancy */
    String instanceId = request.getString(INSTANCE);

    if (instanceId != null) {
      String instanceFilter = INSTANCE_FILTER.replace("$1", instanceId);
      LOGGER.debug("Info: Instance found in query;" + instanceFilter);
      mustQuery.add(new JsonObject(instanceFilter));
    }

    JsonObject accessPolicyClause = buildAccessPolicyFilter(request.getString(SUB));
    mustQuery.addAll(accessPolicyClause.getJsonArray(MUST));
    mustNotQuery.addAll(accessPolicyClause.getJsonArray(MUST_NOT));

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
      if (!mustNotQuery.isEmpty()) {
        boolQuery.getJsonObject("bool").put(MUST_NOT, mustNotQuery);
      }
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
   * Builds access control filter based on authentication (sub).
   *
   * @param sub The user ID from token
   * @return A JsonObject containing two arrays: mustQuery, mustNotQuery
   */
  private JsonObject buildAccessPolicyFilter(String sub) {
    JsonArray mustQuery = new JsonArray();
    JsonArray mustNotQuery = new JsonArray();

    if (sub != null && !sub.isEmpty()) {
      // Public access
      JsonObject publicAccess = new JsonObject(MATCH_QUERY
          .replace("$1", ACCESS_POLICY).replace("$2", OPEN));

      // Restricted access
      JsonObject restrictedAccess = new JsonObject(MATCH_QUERY
          .replace("$1", ACCESS_POLICY).replace("$2", RESTRICTED));

      // Private access
      JsonObject privateAccess = new JsonObject(MATCH_QUERY
          .replace("$1", ACCESS_POLICY).replace("$2", PRIVATE));

      // Owner match
      JsonObject ownerMatch = new JsonObject(MATCH_QUERY
          .replace("$1", PROVIDER_USER_ID).replace("$2", sub));

      // private + owner match → own private items
      JsonObject privateOwned = new JsonObject()
          .put("bool", new JsonObject()
              .put("must", new JsonArray()
                  .add(privateAccess)
                  .add(ownerMatch)));

      // access control → allow OPEN, RESTRICTED, or OWN PRIVATE
      JsonObject accessControl = new JsonObject()
          .put("bool", new JsonObject()
              .put("should", new JsonArray()
                  .add(publicAccess)
                  .add(restrictedAccess)
                  .add(privateOwned)));

      mustQuery.add(accessControl);
    } else {
      // If not authenticated, exclude PRIVATE
      JsonObject excludeAllPrivate = new JsonObject(MATCH_QUERY
          .replace("$1", ACCESS_POLICY)
          .replace("$2", PRIVATE));
      mustNotQuery.add(excludeAllPrivate);
    }

    return new JsonObject()
        .put(MUST, mustQuery)
        .put(MUST_NOT, mustNotQuery);
  }

  /**
   * Builds a count aggregation query for Elasticsearch using the first `term`-type search criterion.
   *
   * <p>This method supports multiple `searchCriteria` filters, but uses only the first item
   * to determine the aggregation field and values.</p>
   *
   * <p>The rest of the filters (including access control) are added to the bool query.</p>
   *
   * <p>Aggregation is only performed for `term` type queries on fields with keyword mapping.</p>
   *
   * @param request The incoming request body containing `searchCriteria` and optionally `sub` for access control.
   * @return Elasticsearch query as a JsonObject with filters and a single terms aggregation.
   */
  public JsonObject countQuery(JsonObject request) {
    // Validate presence of searchCriteria
    if (!request.containsKey(SEARCH_CRITERIA_KEY)
        || request.getJsonArray(SEARCH_CRITERIA_KEY).isEmpty()) {
      return new JsonObject().put(ERROR, new RespBuilder()
          .withType("MISSING_SEARCH_CRITERIA")
          .withTitle("Missing Search Criteria")
          .withDetail("The request must contain a non-empty searchCriteria block.")
          .getJsonResponse());
    }

    JsonArray searchCriteriaArray = request.getJsonArray(SEARCH_CRITERIA_KEY);
    JsonArray mustArray = new JsonArray();
    JsonObject termsMap = new JsonObject(); // field -> array of values

    for (int i = 0; i < searchCriteriaArray.size(); i++) {
      JsonObject criterion = searchCriteriaArray.getJsonObject(i);
      String searchType = criterion.getString(SEARCH_TYPE);
      String field = criterion.getString(FIELD);

      switch (searchType) {
        case TERM: {
          JsonArray values = criterion.getJsonArray(VALUES);
          // Aggregate values for same field
          if (!termsMap.containsKey(field)) {
            termsMap.put(field, values);
          } else {
            JsonArray existing = termsMap.getJsonArray(field);
            for (int j = 0; j < values.size(); j++) {
              if (!existing.contains(values.getString(j))) {
                existing.add(values.getString(j));
              }
            }
            termsMap.put(field, existing);
          }
          break;
        }

        case BETWEEN_RANGE:
        case BETWEEN_TEMPORAL: {
          JsonObject range = new JsonObject();
          range.put(GREATER_THAN_EQUALS, criterion.getJsonArray(VALUES).getValue(0));
          range.put(LESS_THAN_EQUALS, criterion.getJsonArray(VALUES).getValue(1));

          mustArray.add(new JsonObject()
              .put(RANGE, new JsonObject()
                  .put(field, range)));
          break;
        }

        case AFTER_RANGE:
        case AFTER_TEMPORAL: {
          JsonObject range = new JsonObject().put(GREATER_THAN_EQUALS,
              criterion.getJsonArray(VALUES).getValue(0));
          mustArray.add(new JsonObject()
              .put(RANGE, new JsonObject()
                  .put(field, range)));
          break;
        }

        case BEFORE_RANGE:
        case BEFORE_TEMPORAL: {
          JsonObject range = new JsonObject().put(LESS_THAN_EQUALS,
              criterion.getJsonArray(VALUES).getValue(0));
          mustArray.add(new JsonObject()
              .put(RANGE, new JsonObject()
                  .put(field, range)));
          break;
        }

        default: {
          return new JsonObject().put(ERROR, new RespBuilder()
              .withType("UNSUPPORTED_SEARCH_TYPE")
              .withTitle("Invalid Search Type")
              .withDetail("Only 'term' and range/temporal types are supported in /count.")
              .getJsonResponse());
        }
      }
    }

    // Add all collected term filters
    for (String field : termsMap.fieldNames()) {
      JsonArray values = termsMap.getJsonArray(field);
      mustArray.add(new JsonObject()
          .put(TERMS_KEY, new JsonObject()
              .put(field + KEYWORD_KEY, values)));
    }

    // Access control filter
    JsonObject accessPolicy = buildAccessPolicyFilter(request.getString(SUB));
    mustArray.addAll(accessPolicy.getJsonArray(MUST));
    JsonArray mustNotArray = accessPolicy.getJsonArray(MUST_NOT);

    JsonObject boolFilter = new JsonObject();
    if (!mustArray.isEmpty()) {
      boolFilter.put(MUST, mustArray);
    }
    if (!mustNotArray.isEmpty()) {
      boolFilter.put(MUST_NOT, mustNotArray);
    }

    return new JsonObject()
        .put(QUERY_KEY, new JsonObject().put("bool", boolFilter))
        .put(SIZE_KEY, 0)
        .put(AGGREGATION_KEY, new JsonObject()
            .put(RESULTS, new JsonObject()
                .put(TERMS_KEY, new JsonObject()
                    .put(FIELD, TYPE_KEYWORD))));

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
                + MATCH_QUERY.replace("$1", ID_KEYWORD)
                .replace("$2", request.getString(RESOURCE_GRP))
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

    if (itemType.equalsIgnoreCase(TAGS) || itemType.equalsIgnoreCase(DEPARTMENT)
        || itemType.equalsIgnoreCase(ORGANIZATION_TYPE) || itemType.equalsIgnoreCase(FILE_FORMAT)
        || itemType.equalsIgnoreCase(DATA_READINESS) || itemType.equalsIgnoreCase(MODEL_TYPE)) {
      if (instanceId == null || instanceId == "") {
        tempQuery = LIST_AGGREGATION_QUERY_NO_FILTER
            .replace("$field", itemType + KEYWORD_KEY);
      } else {
        tempQuery = LIST_INSTANCE_FIELD_QUERY
            .replace("$1", instanceId)
            .replace("$field", itemType + KEYWORD_KEY);
      }
    } else {
      if (instanceId == null || instanceId == "") {
        tempQuery = LIST_TYPES_QUERY.replace("$1", type);
      } else {
        tempQuery = LIST_INSTANCE_TYPES_QUERY.replace("$1", type)
            .replace("$2", instanceId);
      }
    }

    Integer limit =
        request.getInteger(LIMIT, FILTER_PAGINATION_SIZE - request.getInteger(OFFSET, 0));
    elasticQuery = tempQuery.replace("$size", limit.toString());

    return elasticQuery;
  }

  public String listMultipleItemTypesQuery(JsonObject request) {
    LOGGER.debug("Info: Reached list multiple items; " + request.toString());

    Integer limit = request.getInteger(LIMIT,
        FILTER_PAGINATION_SIZE - request.getInteger(OFFSET, 0));

    StringBuilder queryBuilder = new StringBuilder();
    queryBuilder.append(QUERY_START);

    // Always include at least accessPolicy filter
    queryBuilder.append(QUERY_BOOL_FILTER_START);

    // Access policy filter
    JsonArray allowedPolicies = new JsonArray();
    allowedPolicies.add(OPEN).add(RESTRICTED);

    String openRestrictedFilter = TERMS_QUERY_TEMPLATE
        .replace("$field", ACCESS_POLICY + KEYWORD_KEY)
        .replace("$value", allowedPolicies.toString());

    // Create the `must_not exists` part for missing accessPolicy
    String accessPolicyMissing = MUST_NOT_EXISTS_QUERY_TEMPLATE
        .replace("$field", ACCESS_POLICY);

    String shouldArray = String.format("[%s,%s]", openRestrictedFilter, accessPolicyMissing);
    String openRestrictedOrMissing = SHOULD_QUERY.replace("$1", shouldArray);

    // Private access policy needs special handling: restrict by ownerUserId = sub
    // Access policy filter logic
    boolean hasToken = request.containsKey(SUB) && request.getString(SUB) != null;
    if (hasToken) {
      String shouldClause = getShouldClause(request.getString(SUB), openRestrictedOrMissing);
      queryBuilder.append(shouldClause).append(",");
    } else {
      // Only open + restricted if no token
      queryBuilder.append(openRestrictedOrMissing).append(",");
    }

    //instanceId filter
    String instanceId = request.getString(INSTANCE);
    if (instanceId != null && !instanceId.isEmpty()) {
      String instanceFilter = TERM_QUERY_TEMPLATE
          .replace("$field", INSTANCE + KEYWORD_KEY)
          .replace("$value", instanceId);
      queryBuilder.append(instanceFilter).append(",");
    }

    // Handle additional filters (from the filters array)
    JsonArray filters = request.getJsonArray(SEARCH_CRITERIA_KEY);
    if (filters != null && !filters.isEmpty()) {
      for (Object filterObj : filters) {
        JsonObject filter = (JsonObject) filterObj;
        String field = filter.getString(FIELD);
        JsonArray values = filter.getJsonArray(VALUES);

        if (field != null && values != null && !values.isEmpty()) {
          String extraFilter = TERMS_QUERY_TEMPLATE
              .replace("$field", field + KEYWORD_KEY)
              .replace("$value", values.toString());
          // Assuming a single value in "values" for simplicity
          queryBuilder.append(extraFilter).append(",");
        }
      }
    }

    // Remove last comma if necessary
    if (queryBuilder.toString().endsWith(",")) {
      queryBuilder = new StringBuilder(queryBuilder.substring(0, queryBuilder.length() - 1));
    }

    queryBuilder.append(QUERY_BOOL_FILTER_END);

    queryBuilder.append(AGGS_START);

    // Aggregation part for item types
    JsonArray itemTypes = request.getJsonArray(TYPE);
    for (int i = 0; i < itemTypes.size(); i++) {
      String itemType = itemTypes.getString(i);
      String agg = TERMS_AGG_TEMPLATE
          .replace("$name", itemType)
          .replace("$field", itemType + KEYWORD_KEY)
          .replace("$size", limit.toString());
      queryBuilder.append(agg);

      if (i != itemTypes.size() - 1) {
        queryBuilder.append(",");
      }
    }

    queryBuilder.append(AGGS_END);
    queryBuilder.append(QUERY_END);

    return queryBuilder.toString();
  }

  private static String getShouldClause(String sub, String openRestrictedFilter) {
    String privateFilter = TERM_QUERY_TEMPLATE
        .replace("$field", ACCESS_POLICY + KEYWORD_KEY)
        .replace("$value", PRIVATE);

    String ownerFilter = TERM_QUERY_TEMPLATE
        .replace("$field", PROVIDER_USER_ID + KEYWORD_KEY)
        .replace("$value", sub);

    // Combine private access filters in must clause
    String mustArray = String.format("[%s,%s]", privateFilter, ownerFilter);
    String privateMustClause = MUST_QUERY.replace("$1", mustArray);

    // Combine with open/restricted in should clause
    String shouldArray = String.format("[%s,%s]", openRestrictedFilter, privateMustClause);
    return SHOULD_QUERY.replace("$1", shouldArray);
  }

}
