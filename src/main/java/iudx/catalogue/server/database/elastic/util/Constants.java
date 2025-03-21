package iudx.catalogue.server.database.elastic.util;

public class Constants {

  public static final String DATA_SAMPLE = "dataSample";
  public static final String DATA_DESCRIPTOR = "dataDescriptor";
  public static final String LABEL = "label";
  public static final String DOC_ID = "_id";
  public static final String KEY = "key";
  public static final String ERROR_INVALID_PARAMETER = "Incorrect/missing query parameters";

  /**
   * Search type regex.
   */
  public static final String TAGSEARCH_REGEX = "(.*)tagsSearch(.*)";

  public static final String TEXTSEARCH_REGEX = "(.*)textSearch(.*)";
  public static final String ATTRIBUTE_SEARCH_REGEX = "(.*)attributeSearch(.*)";
  public static final String GEOSEARCH_REGEX = "(.*)geoSearch(.*)";
  public static final String RESPONSE_FILTER_GEO = "responseFilter_geoSearch_";
  public static final String RESPONSE_FILTER_REGEX = "(.*)responseFilter(.*)";

  /**
   * DB Query related.
   */
  public static final String MATCH_KEY = "match";

  public static final String TERMS_KEY = "terms";
  public static final String STRING_QUERY_KEY = "query_string";
  public static final String FROM = "from";
  public static final String KEYWORD_KEY = ".keyword";
  public static final String DEVICEID_KEY = "deviceId";
  public static final String TAG_AQM = "aqm";
  public static final String DESCRIPTION_ATTR = "description";
  public static final String ACCESS_POLICY = "accessPolicy";

  /**
   * OldElasticClient search types.
   */
  public static final String DOC_IDS_ONLY = "DOCIDS";

  public static final String SOURCE_ONLY = "SOURCE";
  public static final String DATASET = "DATASET";
  public static final String FORWARD_SLASH = "/";
  public static final String WILDCARD_KEY = "wildcard";
  public static final String AGGREGATION_ONLY = "AGGREGATION";
  public static final String RATING_AGGREGATION_ONLY = "R_AGGREGATION";
  public static final String TYPE_KEYWORD = "type.keyword";
  public static final String WORD_VECTOR_KEY = "_word_vector";
  public static final String SOURCE_AND_ID = "SOURCE_ID";
  public static final String SOURCE_AND_ID_GEOQUERY = "SOURCE_ID_GEOQUERY";
  public static final String RESOURCE_AGGREGATION_ONLY = "RESOURCE_AGGREGATION";
  public static final String PROVIDER_AGGREGATION_ONLY = "PROVIDER_AGGREGATION";

  /* General purpose */
  public static final String SEARCH = "search";
  public static final String COUNT = "count";
  public static final String ATTRIBUTE = "attrs";
  public static final String RESULT = "results";
  public static final String SIZE_KEY = "size";
  public static final int STATIC_DELAY_TIME = 3000;
  public static final String FILTER_PATH = "?filter_path=took,hits.total.value,hits.hits._source";
  public static final String FILTER_PATH_AGGREGATION =
      "?filter_path=hits.total.value,aggregations.results.buckets";
  public static final String FILTER_ID_ONLY_PATH =
      "?filter_path=hits.total.value,hits.hits._id&size=10000";
  public static final String FILTER_PATH_ID_AND_SOURCE =
      "?filter_path=took,hits.total.value,hits.hits._source,hits.hits._id";
  public static final String TYPE_KEY = "type";
  public static final String ID_KEYWORD = "id.keyword";
  public static final String DOC_COUNT = "doc_count";
  public static final String SUMMARY_KEY = "_summary";
  public static final String GEOSUMMARY_KEY = "_geosummary";
  /* Geo-Spatial */
  public static final String COORDINATES_KEY = "coordinates";
  public static final String GEO_BBOX = "envelope";
  public static final String GEO_CIRCLE = "circle";
  public static final String GREATER_THAN_OP = ">";
  public static final String LESS_THAN_OP = "<";
  public static final String GREATER_THAN_EQ_OP = ">=";
  public static final String LESS_THAN_EQ_OP = "<=";
  /* Replace above source list with commented one to include comment in response for rating API */
  //    "\"_source\": [\"rating\",\"comment\",\"id\"] }";
  public static final String GEO_KEY = ".geometry";
  /* Error */
  public static final String DATABASE_BAD_QUERY = "Query Failed with status != 20x";
  public static final String NO_SEARCH_TYPE_FOUND = "No searchType found";
  public static final String ERROR_DB_REQUEST = "DB request has failed";
  public static final String INSTANCE_NOT_EXISTS = "instance doesn't exist";
  static final String DESCRIPTION = "detail";
  static final String HTTP = "http";
  public static final String SHAPE_KEY = "shape";
  /* Database */
  public static final String AGGREGATION_KEY = "aggs";
  static final String FILTER_RATING_AGGREGATION = "?filter_path=hits.total.value,aggregations";
  static final String DISTANCE_IN_METERS = "m";
  static final String GEO_RADIUS = "radius";
  static final String GEO_RELATION_KEY = "relation";
  public static final String GEO_SHAPE_KEY = "geo_shape";
  static final String EMPTY_RESPONSE = "Empty response";
  static final String COUNT_UNSUPPORTED = "Count is not supported with filtering";
  static final String INVALID_SEARCH = "Invalid search request";
  static final String DOC_EXISTS = "item already exists";
}
