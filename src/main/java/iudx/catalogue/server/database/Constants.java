package iudx.catalogue.server.database;

public class Constants {

  public static final String DATA_SAMPLE = "dataSample";
  public static final String DATA_DESCRIPTOR = "dataDescriptor";
  public static final String LABEL = "label";
  public static final String DOC_ID = "_id";
  public static final String KEY = "key";
  public static final String ERROR_INVALID_PARAMETER = "Incorrect/missing query parameters";

  /** Search type regex. */
  public static final String TAGSEARCH_REGEX = "(.*)tagsSearch(.*)";

  public static final String TEXTSEARCH_REGEX = "(.*)textSearch(.*)";
  public static final String ATTRIBUTE_SEARCH_REGEX = "(.*)attributeSearch(.*)";
  public static final String GEOSEARCH_REGEX = "(.*)geoSearch(.*)";
  public static final String RESPONSE_FILTER_GEO = "responseFilter_geoSearch_";
  public static final String RESPONSE_FILTER_REGEX = "(.*)responseFilter(.*)";

  /** DB Query related. */
  public static final String MATCH_KEY = "match";

  public static final String TERMS_KEY = "terms";
  public static final String STRING_QUERY_KEY = "query_string";
  public static final String FROM = "from";
  public static final String KEYWORD_KEY = ".keyword";
  public static final String DEVICEID_KEY = "deviceId";
  public static final String TAG_AQM = "aqm";
  public static final String DESCRIPTION_ATTR = "description";
  public static final String ACCESS_POLICY = "accessPolicy";

  /** ElasticClient search types. */
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
  public static final String LATEST_RG_AGG = "LATEST_RG_AGG";

  /** Some queries. */
  public static final String LIST_INSTANCES_QUERY =
      "{\"size\": 0, \"aggs\":"
          + "{\"results\": {\"terms\":"
          + "{\"field\":instances.keyword,"
          + "\"size\": 10000}}}}";

  public static final String LIST_INSTANCE_TAGS_QUERY =
      "{\"query\": {\"bool\": {\"filter\": {\"term\": {\"instance.keyword\": \"$1\"}}}},"
          + "\"aggs\":"
          + "{\"results\": {\"terms\":"
          + "{\"field\":\"tags.keyword\","
          + "\"size\": $size}}}}";
  public static final String LIST_TAGS_QUERY =
      "{ \"aggs\":"
          + "{\"results\": {\"terms\":"
          + "{\"field\":\"tags.keyword\","
          + "\"size\": $size}}}}";
  public static final String LIST_INSTANCE_TYPES_QUERY =
      "{\"query\": {\"bool\": {\"filter\": [ {\"match\": {\"type\": \"$1\"}},"
          + "{\"term\": {\"instance.keyword\": \"$2\"}}]}},"
          + "\"aggs\": {\"results\": {\"terms\": {\"field\": \"id.keyword\", \"size\": $size}}}}";
  public static final String LIST_TYPES_QUERY =
      "{\"query\": {\"bool\": {\"filter\": [ {\"match\": {\"type\": \"$1\"}} ]}},"
          + "\"aggs\": {\"results\": {\"terms\": {\"field\": \"id.keyword\", \"size\": $size}}}}";
  public static final String GEO_SHAPE_QUERY =
      "{ \"geo_shape\": { \"$4\": { \"shape\": { \"type\": \"$1\", \"coordinates\": $2 },"
          + " \"relation\": \"$3\" } } }";
  public static final String TEXT_QUERY = "{\"query_string\":{\"query\":\"$1\"}}";
  public static final String GET_DOC_QUERY =
      "{\"_source\":[$2],\"query\":{\"term\":{\"id.keyword\":\"$1\"}}}";

  public static final String GET_INSTANCE_CASE_INSENSITIVE_QUERY =
      "{\"_source\":[$2], \"query\": {\"match\": {\"id\": \"$1\"}}}";
  public static final String GET_ASSOCIATED_ID_QUERY =
      "{\"query\":{\"bool\":{\"should\":[{"
          + "\"match\":{\"id.keyword\":\"$1\"}},{"
          + "\"match\":{\"resourceGroup.keyword\":\"$2\"}}],"
          + "\"minimum_should_match\":1}},"
          + "\"_source\":[\"id\"]}";
  public static final String GET_RDOC_QUERY =
      "{\"_source\":[$2],\"query\":{\"bool\": {\"must\": "
          + "[ { \"match\": {\"ratingID.keyword\":\"$1\"} } ],"
          + "\"must_not\": [ { \"match\": {\"status\": \"denied\"} } ] } } }";
  public static final String CHECK_MDOC_QUERY =
      "{\"_source\":[$2],\"query\":{\"bool\": {\"must\": [ { \"match\": "
          + "{\"id.keyword\":\"$1\"} } ] } } }";
  public static final String CHECK_MDOC_QUERY_INSTANCE =
      "{\"_source\":[$2],\"query\":{\"bool\": {\"must\": [ { \"match\": "
          + "{\"instanceId.keyword\":\"$1\"} } ] } } }";
  public static final String GET_MLAYER_INSTANCE_QUERY =
      "{\"query\":{\"match\":{\"instanceId.keyword\": \"$1\"}},\"_source\":"
          + "{\"includes\": [\"instanceId\",\"name\",\"cover\",\"icon\","
          + "\"logo\",\"coordinates\"]}}";
  public static final String GET_ALL_MLAYER_INSTANCE_QUERY =
      "{\"query\": {\"match_all\": {}},\"_source\":{\"includes\": "
          + "[\"instanceId\",\"name\",\"cover\",\"icon\",\"logo\","
          + "\"coordinates\" ]},\"size\": $0,\"from\": $2}";
  public static final String GET_MLAYER_DOMAIN_QUERY =
      "{\"query\": {\"match\":{\"domainId.keyword\": \"$1\"}},\"_source\":{\"includes\": "
          + "[\"domainId\",\"description\",\"icon\",\"label\",\"name\"]},\"size\": 10000}";
  public static final String GET_ALL_MLAYER_DOMAIN_QUERY =
      "{\"query\": {\"match_all\": {}},\"_source\":{\"includes\": "
          + "[\"domainId\",\"description\",\"icon\",\"label\",\"name\"]},"
          + "\"size\": $0,\"from\": $2}";
  public static final String CHECK_MDOC_QUERY_DOMAIN =
      "{\"_source\":[$2],\"query\":{\"bool\": {\"must\": [ { \"match\": "
          + "{\"domainId.keyword\":\"$1\"} } ] } } }";
  public static final String GET_MLAYER_PROVIDERS_QUERY =
      "{\"query\": {\"match\": {\"type.keyword\": \"iudx:Provider\"}},\"_source\": "
          + "{\"includes\": [\"id\",\"description\"]},\"size\": $0,\"from\": $1}";
  public static final String GET_MLAYER_GEOQUERY =
      "{ \"query\": { \"bool\": { \"minimum_should_match\": 1, \"should\": [$1]}},\"_source\": "
          + "{\"includes\": [\"id\",\"location\",\"instance\",\"label\"] }}";
  public static final String GET_MLAYER_BOOL_GEOQUERY =
      "{\"bool\": {\"should\": [{ \"match\": { \"type.keyword\": \"iudx:Resource\" } },"
          + "{ \"match\": { \"type.keyword\": \"iudx:ResourceGroup\" } }],\"must\": "
          + "[{\"match\": {\"instance.keyword\": \"$2\"}},{\"match\": {\"id.keyword\": "
          + "\"$3\"}}]}}";
  public static final String GET_ALL_MLAYER_INSTANCES =
      "{\"size\": 10000,\"_source\": [\"name\", \"icon\"]}";
  public static final String GET_MLAYER_ALL_DATASETS =
      "{\"query\":{\"bool\":{\"must\":{\"terms\":{\"type.keyword\": [\"iudx:Provider\", "
          + "\"iudx:COS\", \"iudx:ResourceGroup\", \"iudx:Resource\"]}}}},\"_source\""
          + ":{\"includes\": "
          + "[\"type\",\"id\",\"label\",\"accessPolicy\",\"tags\",\"instance\","
          + "\"provider\", \"resourceServerRegURL\",\"description\" ,\"cosURL\", "
          + "\"cos\", \"resourceGroup\", \"resourceType\", \"itemCreatedAt\","
          + "\"icon_base64\"]},\"size\": 10000}";
  public static final String GET_ALL_DATASETS_BY_RS_GRP =
          "{\"size\":10000,\"query\":{\"bool\":{\"must\":[{\"bool\":{\"should\":[{\"match\":"
                  + "{\"type.keyword\":\"iudx:ResourceGroup\"}}]}}]}}}";
  public static final String GET_ALL_DATASETS_BY_FIELDS =
      "{\"query\":{\"bool\":{\"should\":[{\"bool\":{\"must\":[{\"match\":"
          + "{\"type.keyword\":\"iudx:ResourceGroup\"}}";
  public static final String GET_ALL_DATASETS_BY_FIELD_SOURCE =
      "]}},"
          + "{\"bool\":{\"must\":[{\"terms\":{\"type.keyword\":"
          + "[\"iudx:Provider\",\"iudx:COS\"]}}]}}]}},\"_source\":"
          + "{\"includes\":[\"type\",\"id\",\"label\",\"accessPolicy\","
          + "\"tags\",\"instance\",\"provider\",\"resourceServerRegURL\","
          + "\"description\",\"cosURL\",\"cos\",\"resourceGroup\", "
          + "\"itemCreatedAt\", \"icon_base64\"]},\"size\":10000}";
  public static final String GET_MLAYER_DATASET =
      "{\"query\":{\"bool\":{\"should\":[{\"bool\":{\"must\":[{\"match\":{\"id.keyword\":\"$1\"}}"
          + ",{\"match\": {\"type.keyword\": \"iudx:ResourceGroup\"}}]}},{\"bool\":{\"must\":"
          + "[{\"match\":{\"id.keyword\": \"$2\"}},{\"match\":{\"type.keyword\": "
          + "\"iudx:Provider\"}}]}},{\"bool\":{\"must\":[{\"match\":{\"resourceGroup.keyword\""
          + ":\"$1\"}},{\"match\":{\"type.keyword\":\"iudx:Resource\"}}]}},{\"bool\":{\"must\":"
          + "[{\"match\":{\"id.keyword\": \"$3\"}},{\"match\":{\"type.keyword\": \"iudx:COS\""
          + "}}]}}]}},\"_source\":{\"includes\":[\"resourceServer\", \"id\", \"type\","
          + " \"apdURL\","
          + " \"label\", \"description\", \"instance\", \"accessPolicy\",\"cosURL\","
          + " \"dataSample\","
          + " \"dataDescriptor\", \"@context\", \"dataQualityFile\", \"dataSampleFile\","
          + " \"resourceType\", \"resourceServerRegURL\",\"resourceType\","
          + "\"location\", \"iudxResourceAPIs\", \"itemCreatedAt\",\"nsdi\", "
          + "\"icon_base64\"]},\"size\": 10000}";
  public static final String RESOURCE_ACCESSPOLICY_COUNT =
      "{\"size\": 0,\"aggs\":{\"results\":{\"terms\":{\"field\":\"resourceGroup.keyword\","
          + "\"size\":10000},\"aggs\":{\"access_policies\":{\"terms\":{\"field\":"
          + "\"accessPolicy.keyword\",\"size\":10000},\"aggs\":{\"accessPolicy_count\":"
          + "{\"value_count\":{\"field\":\"accessPolicy.keyword\"}}}},\"resource_count\":"
          + "{\"value_count\":{\"field\":\"id.keyword\"}}}}}}\n";
  public static final String GET_MLAYER_INSTANCE_ICON =
      "{\"query\":{\"match\":{\"name\":\"$1\"}},\"_source\": {\"includes\": [\"icon\"]}}";
  public static final String GET_PROVIDER_AND_RESOURCEGROUP =
      "{\"query\":{\"bool\":{\"should\":[{\"bool\":{\"must\":[{\"match\":{\"type.keyword\":"
          + "\"iudx:ResourceGroup\"}}]}},{\"bool\":{\"must\":[{\"match\":{\"type.keyword\":"
          + " \"iudx:Provider\"}}]}}]}},\"_source\":{\"includes\": [\"id\",\"description\","
          + "\"type\",\"resourceGroup\",\"accessPolicy\",\"provider\",\"itemCreatedAt\","
          + "\"instance\",\"label\"]},\"size\":10000}";
  public static final String GET_DATASET_BY_INSTANCE =
      "{\"query\":{\"bool\":{\"should\":[{\"bool\":{\"must\":[{\"match\":{\"type.keyword\""
          + ":\"iudx:ResourceGroup\"}},{\"match\":{\"instance.keyword\":\"$1\"}}]}},"
          + "{\"bool\":{\"must\":[{\"match\":{\"type.keyword\":\"iudx:Provider\"}}]}}]}},"
          + "\"aggs\":{\"provider_count\":{\"cardinality\":{\"field\":"
          + "\"provider.keyword\"}}},\"_source\":{\"includes\":[\"id\","
          + "\"description\",\"type\",\"resourceGroup\","
          + "\"accessPolicy\",\"provider\",\"itemCreatedAt\",\"instance\",\"label\"]},"
          + "\"size\":10000}";
  public static final String GET_LATEST_TOTAL_RG =
          "{\"size\":6,\"from\": 0,\"sort\":[{\"itemCreatedAt\":{\"order\": \"desc\"}}],"
          + "\"query\":{\"bool\":{\"must\":[{\"match\":{\"type\":\"iudx:ResourceGroup\"}}"
          + "]}},\"aggs\":{\"resourceGroupCount\":{\"filter\":{\"term\": {\"type.keyword\":"
          + " \"iudx:ResourceGroup\"}}},\"resourceCount\":{\"global\":{},\"aggs\":"
          + "{\"Resources\":{\"filter\":{\"term\":{\"type.keyword\": \"iudx:Resource\""
          + "}}}}},\"providerCount\":{\"global\":{},\"aggs\":{\"Providers\":{\"filter\":{\"term\""
          + ":{\"type.keyword\": \"iudx:Provider\"}}}}}},\"_source\":{\"includes\":"
          + "[\"id\",\"description\",\"type\",\"resourceGroup\",\"accessPolicy\",\"provider\","
          + "\"itemCreatedAt\",\"instance\",\"label\"]}}";
  public static final String GET_PROVIDERS_AND_POPULAR_RG =
          "{\"size\": 10000,\"_source\":[\"id\",\"description\",\"type\",\"resourceGroup\","
          + "\"accessPolicy\",\"provider\",\"itemCreatedAt\",\"instance\",\"label\"],\"query\":"
          + " {\"bool\": {\"should\":[{\"terms\":{\"id.keyword\": $1}},{\"term\": "
          + "{\"type.keyword\": \"iudx:Provider\"}}]}}}";
  public static final String GET_CATEGORIZED_RESOURCES_AP =
          "{\"size\": 0, \"query\": {\"terms\": {\"resourceGroup.keyword\": $1}}, "
          + "\"aggs\": {\"results\": {\"terms\": {\"field\": \"resourceGroup.keyword\","
          + "\"size\": 10000},\"aggs\": {\"access_policies\": {\"terms\": {\"field\": "
          + "\"accessPolicy.keyword\",\"size\": 10000},\"aggs\": {\"accessPolicy_count\":"
          + " {\"value_count\": {\"field\": \"accessPolicy.keyword\"}}}},"
          + "\"resource_count\": {\"value_count\": {\"field\": \"id.keyword\"}}}}}}";
  public static final String GET_SORTED_MLAYER_INSTANCES =
      "{\"query\": {\"match_all\":{}},\"sort\":[{\"name\":\"asc\"}],\"_source\": "
          + "{\"includes\": [\"name\",\"cover\",\"icon\"]},\"size\":10000}";
  public static final String GET_PROVIDER_AND_RS_ID =
      "{\"query\":{\"bool\":{\"must\":[{\"match\":{\"type.keyword\": \"iudx:ResourceGroup\"}},"
          + "{\"match\":{\"id.keyword\":\"$1\"}}]}},\"_source\": [\"provider\",\"cos\"]}";
  public static final String INSTANCE_FILTER = "{\"match\":" + "{\"instance\": \"" + "$1" + "\"}}";
  public static final String BOOL_MUST_QUERY = "{\"query\":{\"bool\":{\"must\":[$1]}}}";
  public static final String BOOL_SHOULD_QUERY = "{\"query\":{\"bool\":{\"should\":[$1]}}}";
  public static final String SHOULD_QUERY = "{\"bool\":{\"should\":$1}}";
  public static final String MUST_QUERY = "{\"bool\":{\"must\":$1}}";
  public static final String FILTER_QUERY = "{\"bool\":{\"filter\":[$1]}}";
  public static final String MATCH_QUERY = "{\"match\":{\"$1\":\"$2\"}}";
  public static final String TERM_QUERY = "{\"term\":{\"$1\":\"$2\"}}";
  public static final String GET_RATING_DOCS =
      "{\"query\": {\"bool\": {\"must\": [ { \"match\": {\"$1\":\"$2\" } }, "
          + "{ \"match\": { \"status\": \"approved\" } } ] } } , "
          + "\"_source\": [\"rating\",\"id\"] }";
  public static final String GET_AVG_RATING_PREFIX =
      "{\"aggs\":{\"results\":{\"terms\":{\"field\":\"id.keyword\"},"
          + "\"aggs\":{\"average_rating\":{\"avg\":{\"field\":\"rating\"}}}}},"
          + "\"query\":{\"bool\":{\"should\":[";
  public static final String GET_AVG_RATING_MATCH_QUERY = "{\"match\":{\"id.keyword\":\"$1\"}},";
  public static final String GET_AVG_RATING_SUFFIX =
      "],\"minimum_should_match\":1,\"must\":[{\"match\":{\"status\":\"approved\"}}]}}}";
  public static final String QUERY_RESOURCE_GRP =
      "{\"query\":{\"bool\":{\"should\":[{\"term\":{\"id.keyword\":\"$1\"}},{\"term\":"
          + "{\"resourceGroup.keyword\": \"$1\"}},{\"term\":{\"provider.keyword\":\"$1\"}},"
          + "{\"term\":{\"resourceServer.keyword\": \"$1\"}},{\"term\":"
          + "{\"cos.keyword\": \"$1\"}}]}}}";
  public static final String NLP_SEARCH =
      "{\"query\": {\"script_score\": {\"query\": "
          + "{\"match_all\": {}},\"script\":\"source\": \"cosineSimilarity(params.query_vector,"
          + " '_word_vector'') + 1.0\",\"lang\":\"painless\",\"params\": \"query_vector\":"
          + " \"$1\"}}}}}";
  public static final String NLP_LOCATION_SEARCH =
      "{\"query\": {\"script_score\": {\"query\":" + " {\"bool\": {\"should\": [";
  public static final String GET_TYPE_SEARCH =
      "{\"query\": {\"bool\": {\"filter\": [{\"terms\": "
          + "{\"id.keyword\": [\"$1\"],\"boost\": 1}}]}},"
          + "\"_source\": [\"cos\",\"resourceServer\",\"type\","
          + "\"provider\",\"resourceGroup\",\"id\"]}";
  public static final String GET_RSGROUP =
      "{\"query\": {\"bool\": {\"must\": [{\"match\": "
          + "{\"resourceServer.keyword\": \"$1\"}},"
          + "{\"term\": {\"type.keyword\": \"iudx:Provider\"}}]}},"
          + "\"_source\": [\"id\"],\"size\": \"10000\"}";
  public static final String GET_RS1 = "{\"query\": {\"bool\": {\"should\": [";
  public static final String GET_RS2 = "{\"match\": {\"provider.keyword\": \"$1\"}},";
  public static final String GET_RS3 = "],\"minimum_should_match\": 1}}}";
  public static final String GET_DOC_QUERY_WITH_TYPE =
      "{\"_source\":[\"$2\"],\"query\":{\"bool\": {\"must\": "
          + "[{\"term\": {\"id.keyword\": \"$1\"}},"
          + "{\"match\":{ \"type.keyword\": \"$3\"}}]}}}";
  /* General purpose */
  static final String SEARCH = "search";
  static final String COUNT = "count";
  static final String DESCRIPTION = "detail";
  static final String HTTP = "http";
  static final String ATTRIBUTE = "attrs";
  static final String RESULT = "results";
  static final String SHAPE_KEY = "shape";
  static final String SIZE_KEY = "size";
  static final int STATIC_DELAY_TIME = 3000;
  /* Database */
  static final String AGGREGATION_KEY = "aggs";
  static final String FILTER_PATH = "?filter_path=took,hits.total.value,hits.hits._source";
  static final String FILTER_PATH_AGGREGATION =
      "?filter_path=hits.total.value,aggregations.results.buckets";
  static final String FILTER_RATING_AGGREGATION = "?filter_path=hits.total.value,aggregations";
  static final String FILTER_ID_ONLY_PATH =
      "?filter_path=hits.total.value,hits.hits._id&size=10000";
  static final String FILTER_PATH_ID_AND_SOURCE =
      "?filter_path=took,hits.total.value,hits.hits._source,hits.hits._id";
  static final String TYPE_KEY = "type";
  static final String ID_KEYWORD = "id.keyword";
  static final String DOC_COUNT = "doc_count";
  static final String SUMMARY_KEY = "_summary";
  static final String GEOSUMMARY_KEY = "_geosummary";
  /* Geo-Spatial */
  static final String COORDINATES_KEY = "coordinates";
  static final String DISTANCE_IN_METERS = "m";
  static final String GEO_BBOX = "envelope";
  static final String GEO_CIRCLE = "circle";
  /* Replace above source list with commented one to include comment in response for rating API */
  //    "\"_source\": [\"rating\",\"comment\",\"id\"] }";
  static final String GEO_KEY = ".geometry";
  static final String GEO_RADIUS = "radius";
  static final String GEO_RELATION_KEY = "relation";
  static final String GEO_SHAPE_KEY = "geo_shape";
  /* Error */
  static final String DATABASE_BAD_QUERY = "Query Failed with status != 20x";
  static final String EMPTY_RESPONSE = "Empty response";
  static final String NO_SEARCH_TYPE_FOUND = "No searchType found";
  static final String COUNT_UNSUPPORTED = "Count is not supported with filtering";
  static final String INVALID_SEARCH = "Invalid search request";
  static final String ERROR_DB_REQUEST = "DB request has failed";
  static final String DOC_EXISTS = "item already exists";
  static final String INSTANCE_NOT_EXISTS = "instance doesn't exist";
}
