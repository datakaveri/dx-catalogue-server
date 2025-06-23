package iudx.catalogue.server.apiserver.util;

import static iudx.catalogue.server.database.Constants.SORT;
import static iudx.catalogue.server.util.Constants.*;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.Range;
import io.vertx.core.MultiMap;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * QueryMapper class to convert NGSILD query into json object for the purpose of debugrmation
 * exchange among different verticals.
 */
public class QueryMapper {

  private static final Logger LOGGER = LogManager.getLogger(QueryMapper.class);

  /**
   * Converts the query parameters to jsonObject and jsonArray.
   *
   * @param queryParameters of the request.
   * @return jsonObject of queryParameters
   */
  public static JsonObject map2Json(MultiMap queryParameters) {

    JsonObject jsonBody = new JsonObject();

    ArrayList<String> excepAttribute = new ArrayList<String>();
    excepAttribute.add(COORDINATES);
    excepAttribute.add(OFFSET);
    excepAttribute.add(LIMIT);
    excepAttribute.add(MAX_DISTANCE);
    excepAttribute.add(Q_VALUE);

    Pattern regPatternMatchString = Pattern.compile("[\\w]+[^\\,]*(?:\\.*[\\w])");
    Pattern regPatternText = Pattern.compile("^[\\*]{0,1}[A-Za-z0-9\\-\\_ ]+[\\*]{0,1}");

    LOGGER.debug("In query mapper");

    for (Entry<String, String> entry : queryParameters.entries()) {

      String paramValue = entry.getValue().replaceAll("^\"|\"$", "").trim();
      String paramKey = entry.getKey();
      if (paramValue != null && paramValue.isEmpty()) {
        LOGGER.debug("Error: Invalid parameter value; key: " + paramKey);
        return null;
      } else if (!paramValue.startsWith("[") && !paramValue.endsWith("]")) {
        if (!excepAttribute.contains(paramKey)) {
          jsonBody.put(paramKey, paramValue);
        } else if (excepAttribute.contains(paramKey) && !paramKey.equals("q")) {
          jsonBody.put(paramKey, Double.valueOf(paramValue).intValue());
        } else if (paramKey.equals(Q_VALUE) && !regPatternText.matcher(paramValue).matches()) {
          LOGGER.error("Error: Invalid text string");
          return null;
        } else {
          jsonBody.put(paramKey, paramValue);
        }
      } else {
        try {
          Matcher matcher = regPatternMatchString.matcher(entry.getValue());
          if (matcher.find() && !excepAttribute.contains(paramKey)) {
            String replacedValue = paramValue.replaceAll("[\\w]+[^\\,]*(?:\\.*[\\w])",
                "\"$0\"");
            jsonBody.put(paramKey, new JsonArray(replacedValue));
          } else if (excepAttribute.contains(paramKey)) {
            try {
              jsonBody.put(paramKey, new JsonArray(paramValue));
            } catch (DecodeException decodeException) {
              LOGGER.error("Error: Invalid Json value " + decodeException.getMessage());
              return null;
            }
          }
        } catch (Exception e) {
          LOGGER.error("Error: Invalid Json value ");
          return null;
        }
      }
    }

    /* adding search type for geo related search */
    if (jsonBody.containsKey(GEOMETRY)) {
      jsonBody.put(SEARCH_TYPE, jsonBody.getString(SEARCH_TYPE, "") + SEARCH_TYPE_GEO);
    }

    /* adding search type for text related search */
    if (jsonBody.containsKey(Q_VALUE)) {
      jsonBody.put(SEARCH_TYPE, jsonBody.getString(SEARCH_TYPE, "") + SEARCH_TYPE_TEXT);
    }

    /* Tag related search are to be considered as attribute search and are being merged as one */
    if (jsonBody.containsKey(PROPERTY)) {

      jsonBody.put(SEARCH_TYPE, jsonBody.getString(SEARCH_TYPE, "") + SEARCH_TYPE_ATTRIBUTE);
    }

    /* adding response filter */
    if (jsonBody.containsKey(FILTER)) {
      jsonBody.put(SEARCH_TYPE, jsonBody.getString(SEARCH_TYPE, "") + RESPONSE_FILTER);
    }

    LOGGER.debug("Info: Json Query Mapped: " + jsonBody);

    return jsonBody;
  }

  /**
   * Validates the request parameters, headers to compliance with default values.
   *
   * @param requestBody which is a JsonObject
   * @return JsonObject having success and failure status
   */
  public static JsonObject validateQueryParam(JsonObject requestBody) {

    LOGGER.debug("Info: Validating attributes limits and  constraints");
    JsonObject errResponse = new JsonObject().put(STATUS, FAILED);

    /* Validating GeoSearch limits */
    String searchType = requestBody.getString(SEARCH_TYPE, "");
    if (searchType.contains(SEARCH_TYPE_GEO)) {

      /* Checking limits and precision of coordinate attributes */
      if (requestBody.containsKey(COORDINATES)) {

        Pattern pattern = Pattern.compile("[\\w]+[^\\,]*(?:\\.*[\\w])");
        String coordinateStr = requestBody.getJsonArray(COORDINATES, new JsonArray()).toString();
        Matcher matcher = pattern.matcher(coordinateStr);

        List<String> coordinatesValues =
            matcher.results().map(MatchResult::group).collect(Collectors.toList());

        if (coordinatesValues.size() <= COORDINATES_SIZE * 2) {
          for (String value : coordinatesValues) {

            Double tempValue = Double.parseDouble(value);
            if (Double.isFinite(tempValue)) {

              boolean isPrecise =
                  BigDecimal.valueOf(tempValue).scale() >= 0
                      && BigDecimal.valueOf(tempValue).scale() <= COORDINATES_PRECISION;

              if (isPrecise == Boolean.FALSE) {
                LOGGER.error("Error: Overflow coordinate precision");
                return errResponse
                    .put(TYPE, TYPE_INVALID_PROPERTY_VALUE)
                    .put(
                        DESC,
                        "The max point of 'coordinates' precision is " + COORDINATES_PRECISION);
              }
            } else {
              LOGGER.error("Error: Overflow coordinate value");
              return errResponse
                  .put(TYPE, TYPE_INVALID_PROPERTY_VALUE)
                  .put(DESC, "Unable to parse 'coordinates'; value is " + tempValue);
            }
          }
        } else {
          LOGGER.error("Error: Overflow coordinate values");
          return errResponse
              .put(TYPE, TYPE_INVALID_PROPERTY_VALUE)
              .put(DESC, "The max number of 'coordinates' value is " + COORDINATES_SIZE);
        }

        String geometry = requestBody.getString(GEOMETRY, "");
        int countStr = StringUtils.countMatches(coordinateStr.substring(0, 5), "[");
        if (!(geometry.equalsIgnoreCase(POLYGON) && countStr == 3)
            && !(geometry.equalsIgnoreCase(POINT) && countStr == 1)
            && !((geometry.equalsIgnoreCase(LINESTRING) || geometry.equals(BBOX))
                && countStr == 2)) {
          LOGGER.error("Error: Invalid coordinate format");
          return errResponse
              .put(TYPE, TYPE_INVALID_PROPERTY_VALUE)
              .put(DESC, "Invalid coordinate format");
        }
      }

      /* Validating maxDistance attribute for positive integer */
      if (requestBody.getString(GEOMETRY, "").equalsIgnoreCase(POINT)) {
        if (requestBody.containsKey(MAX_DISTANCE)) {
          if (!Range.closed(0, MAXDISTANCE_LIMIT).contains(requestBody.getInteger(MAX_DISTANCE))) {
            LOGGER.error("Error: maxDistance should range between 0-10000m");
            return errResponse
                .put(TYPE, TYPE_INVALID_PROPERTY_VALUE)
                .put(DESC, "The 'maxDistance' should range between 0-10000m");
          }
        } else {
          return new RespBuilder()
              .withType(TYPE_INVALID_SYNTAX)
              .withTitle(TITLE_INVALID_SYNTAX)
              .getJsonResponse();
        }
      }
    }

    /* Validating text search limits */
    if (searchType.contains(SEARCH_TYPE_TEXT)) {

      String searchString = requestBody.getString(Q_VALUE);
      if (searchString.length() > STRING_SIZE) {
        LOGGER.error("Error: 'q' must be " + STRING_SIZE + " in char");
        return errResponse
            .put(TYPE, TYPE_INVALID_PROPERTY_VALUE)
            .put(DESC, "The max string(q) size supported is " + STRING_SIZE);
      }
    }

    /* Validating AttributeSearch limits */
    if (searchType.contains(SEARCH_TYPE_ATTRIBUTE)) {
      LOGGER.debug("Validating attribute search query params;");

      Pattern valuePattern = Pattern.compile("^[a-zA-Z0-9]([\\w-._:\\/() ]*[a-zA-Z0-9])?$");

      /* Checking the number of property and value within the request */
      if (requestBody.getJsonArray(PROPERTY).size() <= PROPERTY_SIZE) {
        JsonArray values = requestBody.getJsonArray(VALUE);

        if (values.size() <= VALUE_SIZE) {
          for (Object value : values) {

            JsonArray nestedValue = (JsonArray) value;
            for (Object entry : nestedValue) {
              LOGGER.debug(entry);
              LOGGER.debug(valuePattern.matcher((String) entry).matches());
              if (!valuePattern.matcher((String) entry).matches()) {
                return errResponse
                    .put(TYPE, TYPE_INVALID_PROPERTY_VALUE)
                    .put(DESC, "Invalid 'value' format");
              }
            }

            if (nestedValue.size() > VALUE_SIZE) {
              LOGGER.error("Error: The value query param has exceeded the limit");
              return errResponse
                  .put(TYPE, TYPE_INVALID_PROPERTY_VALUE)
                  .put(DESC, "The max number of 'value' should be " + VALUE_SIZE);
            }
          }
        } else {
          LOGGER.error("Error: The value query param has exceeded the limit");
          return errResponse
              .put(TYPE, TYPE_INVALID_PROPERTY_VALUE)
              .put(DESC, "The max number of 'value' should be " + VALUE_SIZE);
        }
      } else {
        LOGGER.error("Error: The property query param has exceeded the limit");
        return errResponse
            .put(TYPE, TYPE_INVALID_PROPERTY_VALUE)
            .put(DESC, "The max number of 'property' should be " + PROPERTY_SIZE);
      }
    }

    /* Validating ResponseFilter limits */
    if (searchType.contains(RESPONSE_FILTER)
        && requestBody.getJsonArray(FILTER, new JsonArray()).size() > FILTER_VALUE_SIZE) {

      LOGGER.error("Error: The filter in query param has exceeded the limit");
      return errResponse
          .put(TYPE, TYPE_BAD_FILTER)
          .put(DESC, "The max number of 'filter' should be " + FILTER_VALUE_SIZE);
    }

    /* Validating length of instance header */
    if (requestBody.containsKey(INSTANCE)) {
      String instance = requestBody.getString(INSTANCE, "");
      if (instance != null && instance.length() > INSTANCE_SIZE) {
        LOGGER.error("Error: The instance length has exceeded the limit");
        return errResponse
            .put(TYPE, TYPE_INVALID_PROPERTY_VALUE)
            .put(DESC, "The max length of 'instance' should be " + INSTANCE_SIZE);
      }
    }

    /* Validating length of limit & offset param */
    if (requestBody.containsKey(LIMIT) || requestBody.containsKey(OFFSET)) {
      Integer limit = requestBody.getInteger(LIMIT, 0);
      Integer offset = requestBody.getInteger(OFFSET, 0);
      int totalSize = limit + offset;
      if (totalSize <= 0 || totalSize > MAX_RESULT_WINDOW) {
        LOGGER.error("Error: The limit + offset param has exceeded the limit");
        return errResponse
            .put(TYPE, TYPE_INVALID_PROPERTY_VALUE)
            .put(DESC, "The limit + offset should be between 1 to " + MAX_RESULT_WINDOW);
      }
    }

    return new JsonObject().put(STATUS, SUCCESS);
  }

  /**
   * Extracts and injects 'sort' param from raw query string to support semicolon-based multi-field sort.
   *
   * @param uri the full request URI
   * @param requestBody the JsonObject being built
   */
  public static void extractSortFromRawUri(String uri, JsonObject requestBody) {
    try {
      String[] uriParts = uri.split("\\?", 2);
      if (uriParts.length < 2) return;

      String query = uriParts[1]; // get part after ?
      String[] pairs = query.split("&");

      for (String pair : pairs) {
        if (pair.startsWith("sort=")) {
          String rawSort = pair.substring(5); // after 'sort='
          String decodedSort = java.net.URLDecoder.decode(rawSort,
              UTF_8.name());
          if (!decodedSort.isEmpty()) {
            requestBody.put(SORT, decodedSort);
            LOGGER.debug("Manually extracted and set sort param: {}", decodedSort);
          }
          break;
        }
      }
    } catch (Exception e) {
      LOGGER.error("Failed to extract sort param from raw URI", e);
    }
  }

}
