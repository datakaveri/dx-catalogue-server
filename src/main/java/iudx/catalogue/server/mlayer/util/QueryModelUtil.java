package iudx.catalogue.server.mlayer.util;

import static iudx.catalogue.server.database.elastic.util.Constants.ID_KEYWORD;
import static iudx.catalogue.server.database.elastic.util.Constants.KEYWORD_KEY;
import static iudx.catalogue.server.database.elastic.util.Constants.SIZE_KEY;
import static iudx.catalogue.server.database.elastic.util.Constants.TYPE_KEY;
import static iudx.catalogue.server.database.elastic.util.Constants.TYPE_KEYWORD;
import static iudx.catalogue.server.geocoding.util.Constants.RESULTS;
import static iudx.catalogue.server.util.Constants.FIELD;
import static iudx.catalogue.server.util.Constants.FILTER_PAGINATION_FROM;
import static iudx.catalogue.server.util.Constants.FILTER_PAGINATION_SIZE;
import static iudx.catalogue.server.util.Constants.INSTANCE;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_COS;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_PROVIDER;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_RESOURCE;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_RESOURCE_GROUP;
import static iudx.catalogue.server.util.Constants.MAX_LIMIT;
import static iudx.catalogue.server.util.Constants.PROVIDER;
import static iudx.catalogue.server.util.Constants.PROVIDERS;
import static iudx.catalogue.server.util.Constants.RESOURCE_GRP;
import static iudx.catalogue.server.util.Constants.TAGS;
import static iudx.catalogue.server.util.Constants.VALUE;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import iudx.catalogue.server.database.elastic.util.AggregationType;
import iudx.catalogue.server.database.elastic.util.QueryType;
import java.util.List;
import java.util.Map;

public class QueryModelUtil {

  // Basic MATCH Query
  public static QueryModel createMatchQuery(String field, String value) {
    return new QueryModel(QueryType.MATCH, Map.of(FIELD, field, VALUE, value));
  }

  // Basic MATCH_ALL Query
  public static QueryModel createMatchAllQuery() {
    return new QueryModel(QueryType.MATCH_ALL);
  }
  public static QueryModel createBoolQueryWithMust(QueryModel... mustQueries) {
    QueryModel boolQuery = new QueryModel(QueryType.BOOL);
    boolQuery.setMustQueries(List.of(mustQueries));
    return boolQuery;
  }

  public static QueryModel createBoolQueryWithShould(QueryModel... shouldQueries) {
    QueryModel boolQuery = new QueryModel(QueryType.BOOL);
    boolQuery.setShouldQueries(List.of(shouldQueries));
    return boolQuery;
  }
  public static QueryModel createAggregationQuery(AggregationType type, String field) {
    return new QueryModel(type, Map.of(FIELD, field));
  }

  public static QueryModel createAggregationQuery(AggregationType type, String field, int size) {
    return new QueryModel(type, Map.of(FIELD, field, SIZE_KEY, size));
  }
  // A generic entity-type query with ID match and Type match
  public static QueryModel createEntityTypeQuery(String idField, String idValue, String typeValue) {
    return createBoolQueryWithMust(
        createMatchQuery(idField, idValue),
        createMatchQuery(TYPE_KEYWORD, typeValue)
    );
  }
  public static QueryModel createMlayerInstanceQuery(String id, String limit, String offset) {
    QueryModel queryModel = new QueryModel();

    if (id == null || id.isBlank()) {
      queryModel.setQueries(createMatchAllQuery());
    } else {
      queryModel.setQueries(createMatchQuery("instanceId.keyword", id));
    }

    queryModel.setIncludeFields(
        List.of("instanceId", "name", "cover", "icon", "logo", "coordinates"));
    queryModel.setLimit(limit);
    queryModel.setOffset(offset);

    return queryModel;
  }

  public static QueryModel checkForExistingRecordQuery(String field, String value) {
    QueryModel matchQuery = createMatchQuery(field, value);
    QueryModel query = createBoolQueryWithMust(matchQuery);
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(query);
    return queryModel;
  }

  public static QueryModel createMlayerDomainQuery(String id, String limit, String offset) {
    QueryModel queryModel = new QueryModel();

    if (id == null || id.isBlank()) {
      queryModel.setQueries(createMatchAllQuery());
    } else {
      queryModel.setQueries(createMatchQuery("domainId.keyword", id));
    }

    queryModel.setIncludeFields(List.of("domainId", "description", "icon", "label", "name"));
    queryModel.setLimit(limit);
    queryModel.setOffset(offset);

    return queryModel;
  }

  public static QueryModel buildQueryModelFromIds(JsonArray idArray, String instance) {
    QueryModel query = new QueryModel(QueryType.BOOL);

    List<String> ids = idArray.stream().map(Object::toString).toList();
    for (String datasetId : ids) {
      QueryModel subQueryModel = createInstanceDatasetQuery(instance, datasetId);
      query.addShouldQuery(subQueryModel);
    }
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(query);
    queryModel.setMinimumShouldMatch("1");
    queryModel.setIncludeFields(List.of("id", "location", "instance", "label"));
    return queryModel;
  }
  private static QueryModel createInstanceDatasetQuery(String instance, String datasetId) {
    QueryModel queryModel = createBoolQueryWithShould(
        createMatchQuery(TYPE_KEYWORD, ITEM_TYPE_RESOURCE),
        createMatchQuery(TYPE_KEYWORD, ITEM_TYPE_RESOURCE_GROUP)
    );

    queryModel.setMustQueries(List.of(
        createMatchQuery("instance.keyword", instance),
        createMatchQuery(ID_KEYWORD, datasetId)
    ));

    return queryModel;
  }

  public static QueryModel getMlayerLookupQuery(String id) {
    QueryModel query = createEntityTypeQuery(ID_KEYWORD, id, ITEM_TYPE_RESOURCE_GROUP);
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(query);
    queryModel.setIncludeFields(List.of("provider", "cos"));
    return queryModel;
  }

  public static QueryModel getMlayerDatasetQuery(String id, String cosId, String providerId) {
    QueryModel getMlayerDatasetQuery = createBoolQueryWithShould(
        createEntityTypeQuery(ID_KEYWORD, id, ITEM_TYPE_RESOURCE_GROUP),
        createEntityTypeQuery(ID_KEYWORD, providerId, ITEM_TYPE_PROVIDER),
        createEntityTypeQuery(RESOURCE_GRP + KEYWORD_KEY, id, ITEM_TYPE_RESOURCE),
        createEntityTypeQuery(ID_KEYWORD, cosId, ITEM_TYPE_COS)
    );

    return getMlayerDatasetQueryModel(getMlayerDatasetQuery);
  }
  private static QueryModel getMlayerDatasetQueryModel(QueryModel getMlayerDatasetQuery) {
    QueryModel getMlayerDatasetQueryModel = new QueryModel();
    getMlayerDatasetQueryModel.setQueries(getMlayerDatasetQuery);
    getMlayerDatasetQueryModel.setLimit(MAX_LIMIT);
    getMlayerDatasetQueryModel
        .setIncludeFields(List.of(
            "resourceServer", "id", "type", "apdURL", "label", "description", "instance",
            "accessPolicy", "cosURL", "dataSample", "dataDescriptor", "@context",
            "dataQualityFile", "dataSampleFile", "resourceType", "resourceServerRegURL",
            "location", "iudxResourceAPIs", "itemCreatedAt", "nsdi", "icon_base64"
        ));
    return getMlayerDatasetQueryModel;
  }

  public static QueryModel getMlayerInstanceIconsQuery(String instanceCapitalizeName) {
    // query to get the icon path of the instance in the  resource group
    QueryModel getIconQuery = new QueryModel(QueryType.MATCH,
        Map.of(FIELD, "name", VALUE, instanceCapitalizeName));
    QueryModel getIconQueryModel = new QueryModel();
    getIconQueryModel.setQueries(getIconQuery);
    getIconQueryModel.setIncludeFields(List.of("icon"));
    return getIconQueryModel;
  }

  public static QueryModel getMlayerAllDatasetsQuery() {
    QueryModel query = new QueryModel(QueryType.BOOL);
    query.addMustQuery(new QueryModel(QueryType.TERMS,
        Map.of(FIELD, "type.keyword", VALUE,
            List.of(ITEM_TYPE_PROVIDER, ITEM_TYPE_COS, ITEM_TYPE_RESOURCE_GROUP,
                ITEM_TYPE_RESOURCE))));

    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(query);
    queryModel.setIncludeFields(
        List.of("type", "id", "label", "accessPolicy", "tags", "instance", "provider",
            "resourceServerRegURL", "description", "cosURL", "cos", "resourceGroup",
            "resourceType", "itemCreatedAt", "icon_base64"));
    queryModel.setLimit(MAX_LIMIT);
    return queryModel;
  }

  public static QueryModel getMlayerAllResourcesQuery(JsonObject requestData) {
    QueryModel baseResourceGroupQuery = new QueryModel(QueryType.BOOL);
    baseResourceGroupQuery.addMustQuery(new QueryModel(QueryType.MATCH, Map.of(FIELD,
        "type.keyword", VALUE, ITEM_TYPE_RESOURCE_GROUP)));

    if (requestData.containsKey(TAGS) && !requestData.getJsonArray(TAGS).isEmpty()) {
      JsonArray tagsArray = requestData.getJsonArray(TAGS);
      String tagQueryString = tagsArray.stream()
          .filter(tag -> tag instanceof String)
          .map(Object::toString)
          .reduce((a, b) -> a + " AND " + b)
          .orElse("");

      if (!tagQueryString.isEmpty()) {
        QueryModel tagQuery = new QueryModel(QueryType.QUERY_STRING, Map.of(
            "default_field", "tags",
            "query", "(" + tagQueryString + ")"
        ));
        baseResourceGroupQuery.addMustQuery(tagQuery);
      }
    }
    if (requestData.containsKey(INSTANCE) && !requestData.getString(INSTANCE).isBlank()) {
      String instanceValue = requestData.getString(INSTANCE).toLowerCase();
      baseResourceGroupQuery.addMustQuery(
          new QueryModel(QueryType.MATCH, Map.of(FIELD, "instance.keyword", VALUE,
              instanceValue)));
    }
    if (requestData.containsKey(PROVIDERS) && !requestData.getJsonArray(PROVIDERS).isEmpty()) {
      baseResourceGroupQuery.addMustQuery(new QueryModel(QueryType.TERMS, Map.of(FIELD,
          "provider.keyword", VALUE, requestData.getJsonArray(PROVIDERS)
      )));
    }

    QueryModel baseProviderCosQuery = new QueryModel(QueryType.BOOL);
    baseProviderCosQuery.addMustQuery(new QueryModel(QueryType.TERMS, Map.of(FIELD,
        "type.keyword", VALUE, List.of(ITEM_TYPE_PROVIDER, ITEM_TYPE_COS))));

    QueryModel mainQueryModel = new QueryModel();
    QueryModel queryModel = new QueryModel(QueryType.BOOL);
    queryModel.setShouldQueries(List.of(baseResourceGroupQuery, baseProviderCosQuery));
    mainQueryModel.setQueries(queryModel);
    mainQueryModel.setIncludeFields(List.of(
        "type", "id", "label", "accessPolicy", "tags", "instance",
        "provider", "resourceServerRegURL", "description", "cosURL",
        "cos", "resourceGroup", "itemCreatedAt", "icon_base64"
    ));
    mainQueryModel.setLimit(MAX_LIMIT);
    return mainQueryModel;
  }

  public static QueryModel getAllInstanceNamesAndIconsQuery() {
    QueryModel queryModel = new QueryModel();
    queryModel.setLimit(MAX_LIMIT);
    queryModel.setIncludeFields(List.of("name", "icon"));
    return queryModel;
  }

  public static QueryModel getResourceApQueryModel() {
    QueryModel aggs = createAggregationQuery(
        AggregationType.TERMS, "resourceGroup.keyword", FILTER_PAGINATION_SIZE);
    QueryModel accessPolicies = createAggregationQuery(
        AggregationType.TERMS, "accessPolicy.keyword", FILTER_PAGINATION_SIZE);
    QueryModel accessPolicyCount = createAggregationQuery(
        AggregationType.VALUE_COUNT, "accessPolicy.keyword");
    QueryModel resourceCount = createAggregationQuery(AggregationType.VALUE_COUNT, ID_KEYWORD);

    // Set aggregations
    aggs.setAggregationName(RESULTS);
    aggs.setAggregationsMap(Map.of(
        "access_policies", accessPolicies,
        "accessPolicy_count", accessPolicyCount,
        "resource_count", resourceCount
    ));

    QueryModel queryModel = new QueryModel();
    queryModel.setAggregations(List.of(aggs));
    queryModel.setLimit(FILTER_PAGINATION_FROM);
    return queryModel;
  }

  public static QueryModel getCategorizedResourceApQueryModel(JsonArray allRgId) {
    // Aggregation: accessPolicy_count
    QueryModel accessPolicyCountAgg =
        new QueryModel(
            AggregationType.VALUE_COUNT,
            Map.of(FIELD, "accessPolicy.keyword"));

    // Aggregation: access_policies
    QueryModel accessPoliciesAgg =
        new QueryModel(
            AggregationType.TERMS,
            Map.of(FIELD, "accessPolicy.keyword", SIZE_KEY, 10000));
    accessPoliciesAgg.setAggregationsMap(
        Map.of("accessPolicy_count", accessPolicyCountAgg));

    // Aggregation: resource_count
    QueryModel resourceCountAgg =
        new QueryModel(
            AggregationType.VALUE_COUNT, Map.of(FIELD, "id.keyword"));

    // Aggregation: results
    QueryModel resultsAgg =
        new QueryModel(
            AggregationType.TERMS,
            Map.of(FIELD, "resourceGroup.keyword", SIZE_KEY, 10000));
    resultsAgg.setAggregationName("results");
    resultsAgg.setAggregationsMap(
        Map.of(
            "access_policies",
            accessPoliciesAgg,
            "resource_count",
            resourceCountAgg));
    QueryModel termsQueryModel = new QueryModel(QueryType.TERMS,
        Map.of(FIELD, "resourceGroup.keyword", VALUE, allRgId));
    QueryModel getCategorizedResourceAP = new QueryModel();
    getCategorizedResourceAP.setQueries(termsQueryModel);
    getCategorizedResourceAP.setAggregations(List.of(resultsAgg));
    getCategorizedResourceAP.setLimit("0");
    return getCategorizedResourceAP;
  }

  public static QueryModel getAllDatasetsByResourceGroupQuery() {
    QueryModel shouldQuery = new QueryModel(QueryType.MATCH, Map.of(
        FIELD, "type.keyword", VALUE, ITEM_TYPE_RESOURCE_GROUP
    ));
    QueryModel innerBoolQuery = new QueryModel(QueryType.BOOL);
    innerBoolQuery.addShouldQuery(shouldQuery);
    QueryModel outerBoolQuery = new QueryModel(QueryType.BOOL);
    outerBoolQuery.addMustQuery(innerBoolQuery);
    QueryModel finalQueryModel = new QueryModel();
    finalQueryModel.setQueries(outerBoolQuery);
    finalQueryModel.setLimit(MAX_LIMIT);
    return finalQueryModel;
  }

  public static QueryModel getMatchAllSortedQueryModel() {
    return buildSortedSearchQueryModel(QueryType.MATCH_ALL,
        List.of("name", "cover", "icon"),
        Map.of("name", "asc"),
        MAX_LIMIT);
  }
  public static QueryModel buildSortedSearchQueryModel(QueryType queryType,
                                                       List<String> includeFields,
                                                       Map<String, String> sortFields,
                                                       String limit) {
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(new QueryModel(queryType));
    queryModel.setIncludeFields(includeFields);
    queryModel.setSortFields(sortFields);
    queryModel.setLimit(limit);
    return queryModel;
  }

  public static QueryModel buildMlayerDomainQueryModel() {
    return createMatchAllQueryModel(
        List.of("domainId", "description", "icon", "label", "name"),
        MAX_LIMIT, FILTER_PAGINATION_FROM
    );
  }
  public static QueryModel createMatchAllQueryModel(List<String> includeFields, String limit,
                                                    String offset) {
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(createMatchAllQuery());
    queryModel.setIncludeFields(includeFields);
    queryModel.setLimit(limit);
    queryModel.setOffset(offset);
    return queryModel;
  }

  public static QueryModel createProviderAndResourceQuery(String instance) {
    QueryModel resourceGroupQuery = createBoolQueryWithMust(createMatchQuery(TYPE_KEYWORD,
        ITEM_TYPE_RESOURCE_GROUP), createMatchQuery("instance.keyword", instance));
    QueryModel providerQuery = createBoolQueryWithMust(createMatchQuery(TYPE_KEYWORD,
        ITEM_TYPE_PROVIDER));
    QueryModel query = createBoolQueryWithShould(resourceGroupQuery, providerQuery);

    QueryModel aggs =
        createAggregationQuery(AggregationType.CARDINALITY, PROVIDER + KEYWORD_KEY);
    aggs.setAggregationName("provider_count");

    QueryModel providerAndResourcesQueryModel = new QueryModel();
    providerAndResourcesQueryModel.setQueries(query);
    providerAndResourcesQueryModel.setAggregations(List.of(aggs));
    providerAndResourcesQueryModel.setIncludeFields(
        List.of("id", "description", "type", "resourceGroup", "accessPolicy",
            "provider", "itemCreatedAt", "instance", "label")
    );
    providerAndResourcesQueryModel.setLimit(MAX_LIMIT);

    return providerAndResourcesQueryModel;
  }

  public static QueryModel getMlayerProviderQuery(String limit, String offset) {
    QueryModel query = createMatchQuery(TYPE_KEYWORD, ITEM_TYPE_PROVIDER);
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(query);
    queryModel.setIncludeFields(List.of("id", "description"));
    queryModel.setLimit(limit);
    queryModel.setOffset(offset);
    return queryModel;
  }

  /**
   * Constructs a QueryModel to fetch the latest resource groups.
   * @return QueryModel for retrieving the latest resource groups.
   */
  public static QueryModel createLatestResourceGroupsQuery() {
    QueryModel query = createBoolQueryWithMust(createMatchQuery(TYPE_KEY,
        ITEM_TYPE_RESOURCE_GROUP));

    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(query);
    queryModel.setAggregations(getResourceGroupCountAgg());
    queryModel.setSortFields(Map.of("itemCreatedAt", "desc"));
    queryModel.setIncludeFields(List.of("id", "description", "type", "resourceGroup",
        "accessPolicy", "provider", "itemCreatedAt", "instance", "label"));
    queryModel.setLimit(("6"));
    queryModel.setOffset("0");

    return queryModel;
  }
  private static List<QueryModel> getResourceGroupCountAgg() {
    // Root aggregation: Filter by ResourceGroup type
    QueryModel resourceGroupCountAgg = createFilterAggregation();

    // Sub-aggregations inside resourceGroupCount
    QueryModel resourceCountAgg = createGlobalWithSubAggregation(
        "resourceCount", "Resources", ITEM_TYPE_RESOURCE);
    QueryModel providerCountAgg = createGlobalWithSubAggregation(
        "providerCount", "Providers", ITEM_TYPE_PROVIDER);

    return List.of(resourceGroupCountAgg, resourceCountAgg, providerCountAgg);
  }
  /**
   * Creates a filter aggregation query.
   *
   * @return QueryModel representing the filter aggregation
   */
  private static QueryModel createFilterAggregation() {
    QueryModel queryModel = new QueryModel();
    queryModel.setAggregationType(AggregationType.FILTER);
    queryModel.setAggregationName("resourceGroupCount");
    queryModel.setAggregationParameters(
        Map.of(FIELD, TYPE_KEYWORD, VALUE, ITEM_TYPE_RESOURCE_GROUP));
    return queryModel;
  }

  /**
   * Creates a global aggregation with a nested filter inside it.
   *
   * @param aggName Aggregation name (e.g., "resourceCount")
   * @param subAggName Sub-aggregation name (e.g., "Resources")
   * @param typeValue Value for TYPE_KEYWORD field in the nested filter
   * @return QueryModel representing the global aggregation with a nested filter
   */
  private static QueryModel createGlobalWithSubAggregation(String aggName, String subAggName,
                                                           String typeValue) {
    QueryModel globalAgg = new QueryModel();
    globalAgg.setAggregationName(aggName);
    globalAgg.setAggregationType(AggregationType.GLOBAL);
    globalAgg.setAggregationParameters(Map.of(FIELD, "NULL", VALUE, "NULL"));

    // Nested filter inside the global aggregation
    QueryModel filterAgg = new QueryModel();
    filterAgg.setAggregationType(AggregationType.FILTER);
    filterAgg.setAggregationParameters(Map.of(FIELD, TYPE_KEYWORD, VALUE, typeValue));

    // Attach the filter as a named sub-aggregation
    globalAgg.setAggregationsMap(Map.of(subAggName, filterAgg));

    return globalAgg;
  }

  public static QueryModel getProviderAndPopularRgsQuery(JsonArray frequentlyUsedResourceGroup) {
    QueryModel termsQuery = new QueryModel(QueryType.TERMS,
        Map.of(FIELD, ID_KEYWORD, VALUE, frequentlyUsedResourceGroup.getList()));
    QueryModel termQuery = new QueryModel(QueryType.TERM,
        Map.of(FIELD, "type.keyword", VALUE, "iudx:Provider"));

    QueryModel query = new QueryModel(QueryType.BOOL);
    query.setShouldQueries(List.of(termsQuery, termQuery));
    QueryModel providerAndPopularRgs = new QueryModel();
    providerAndPopularRgs.setQueries(query);
    providerAndPopularRgs.setIncludeFields(
        List.of("id", "description", "type", "resourceGroup", "accessPolicy", "provider",
            "itemCreatedAt", "instance", "label"));
    providerAndPopularRgs.setLimit(MAX_LIMIT);
    return providerAndPopularRgs;
  }


}
