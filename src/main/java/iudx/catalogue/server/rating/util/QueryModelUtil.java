package iudx.catalogue.server.rating.util;

import static iudx.catalogue.server.auditing.util.Constants.ID;
import static iudx.catalogue.server.database.elastic.util.Constants.ID_KEYWORD;
import static iudx.catalogue.server.database.elastic.util.Constants.KEYWORD_KEY;
import static iudx.catalogue.server.geocoding.util.Constants.RESULTS;
import static iudx.catalogue.server.rating.util.Constants.APPROVED;
import static iudx.catalogue.server.rating.util.Constants.DENIED;
import static iudx.catalogue.server.rating.util.Constants.STATUS;
import static iudx.catalogue.server.util.Constants.FIELD;
import static iudx.catalogue.server.util.Constants.FILTER_PAGINATION_FROM;
import static iudx.catalogue.server.util.Constants.MAX_LIMIT;
import static iudx.catalogue.server.util.Constants.VALUE;

import iudx.catalogue.server.database.elastic.model.QueryModel;
import iudx.catalogue.server.database.elastic.util.AggregationType;
import iudx.catalogue.server.database.elastic.util.QueryType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QueryModelUtil {

  // Basic MATCH Query
  public static QueryModel createMatchQuery(String field, String value) {
    return new QueryModel(QueryType.MATCH, Map.of(FIELD, field, VALUE, value));
  }

  public static QueryModel createAggregationQuery(AggregationType type, String field) {
    return new QueryModel(type, Map.of(FIELD, field));
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
  public static QueryModel getRatingQueryModel(String field, String value) {
    QueryModel ratingIdMatchQuery = createMatchQuery(field, value);
    ratingIdMatchQuery.setLimit(MAX_LIMIT);
    ratingIdMatchQuery.setOffset(FILTER_PAGINATION_FROM);

    QueryModel statusMatchQuery = createMatchQuery(STATUS, APPROVED);

    // Construct the Bool query with must and must_not clauses
    QueryModel boolQueryWithMust = createBoolQueryWithMust(ratingIdMatchQuery, statusMatchQuery);
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(boolQueryWithMust);
    queryModel.setIncludeFields(List.of(ID, "rating"));
    return queryModel;
  }

  public static QueryModel getRatingQueryModel(String ratingId) {
    QueryModel ratingIdMatchQuery = createMatchQuery("ratingID.keyword", ratingId);
    ratingIdMatchQuery.setLimit(MAX_LIMIT);
    ratingIdMatchQuery.setOffset(FILTER_PAGINATION_FROM);

    QueryModel statusMatchQuery = createMatchQuery(STATUS, DENIED);

    // Construct the Bool query with must and must_not clauses
    QueryModel boolQuery = createBoolQueryWithMust(ratingIdMatchQuery);
    boolQuery.setMustNotQueries(List.of(statusMatchQuery));
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(boolQuery);
    return queryModel;
  }

  public static QueryModel getAssociatedIdsQuery(String id){
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(createBoolQueryWithShould(createMatchQuery(ID_KEYWORD, id),
        createMatchQuery("resourceGroup" + KEYWORD_KEY, id)));
    queryModel.setMinimumShouldMatch("1");
    queryModel.setIncludeFields(List.of(ID));
    return queryModel;
  }

  public static QueryModel getAverageRatingQueryModel(List<String> ids) {

    List<QueryModel> shouldQueries =
        ids.stream()
            .map(id -> createMatchQuery(ID_KEYWORD, id))
            .collect(Collectors.toList());

    List<QueryModel> mustQueries = List.of(createMatchQuery(STATUS, APPROVED));

    QueryModel queryModel = new QueryModel(mustQueries, shouldQueries, null,
        null);
    queryModel.setMinimumShouldMatch("1");

    // Create the "average_rating" nested aggregation
    QueryModel avgRatingAgg = createAggregationQuery(AggregationType.AVG, "rating");

    // Create the outer "results" aggregation with the "terms" aggregation and nested
    // "average_rating" aggregation
    QueryModel termsAgg = createAggregationQuery(AggregationType.TERMS, ID_KEYWORD);
    termsAgg.setAggregationName(RESULTS);
    termsAgg.setAggregationsMap(Map.of("average_rating", avgRatingAgg));
    return new QueryModel(queryModel, List.of(termsAgg));
  }
}
