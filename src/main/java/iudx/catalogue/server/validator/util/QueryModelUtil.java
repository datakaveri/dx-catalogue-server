package iudx.catalogue.server.validator.util;

import static iudx.catalogue.server.database.elastic.util.Constants.ID_KEYWORD;
import static iudx.catalogue.server.util.Constants.FIELD;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_COS;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_RESOURCE_GROUP;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_RESOURCE_SERVER;
import static iudx.catalogue.server.util.Constants.NAME;
import static iudx.catalogue.server.util.Constants.RESOURCE_SERVER_URL;
import static iudx.catalogue.server.util.Constants.VALUE;

import iudx.catalogue.server.database.elastic.model.QueryModel;
import iudx.catalogue.server.database.elastic.util.QueryType;
import java.util.List;
import java.util.Map;

public class QueryModelUtil {
  public static QueryModel getValidateQueryForResourceGroup(String name, String provider) {
    QueryModel boolIdQuery = new QueryModel(QueryType.BOOL);
    boolIdQuery.addMustQuery(
        new QueryModel(QueryType.MATCH, Map.of(FIELD, ID_KEYWORD, VALUE, provider)));
    QueryModel boolRsQuery = new QueryModel(QueryType.BOOL);
    boolRsQuery.addMustQuery(
        new QueryModel(
            QueryType.MATCH, Map.of(FIELD, "type.keyword", VALUE, ITEM_TYPE_RESOURCE_GROUP)));
    boolRsQuery.addMustQuery(
        new QueryModel(
            QueryType.MATCH, Map.of(FIELD, NAME + ".keyword", VALUE, name)));
    QueryModel finalQuery = new QueryModel(QueryType.BOOL);
    finalQuery.setShouldQueries(List.of(boolIdQuery, boolRsQuery));
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(finalQuery);
    queryModel.setIncludeFields(List.of("type"));
    return  queryModel;
  }

  public static QueryModel getValidateQueryForProvider(String resourceServer, String ownerUserId,
                                                       String resourceServerUrl) {
    QueryModel boolIdQuery = new QueryModel(QueryType.BOOL);
    boolIdQuery.addMustQuery(
        new QueryModel(QueryType.MATCH, Map.of(FIELD, ID_KEYWORD, VALUE, resourceServer)));
    QueryModel boolRsQuery = new QueryModel(QueryType.BOOL);
    boolRsQuery.addMustQuery(
        new QueryModel(QueryType.MATCH, Map.of(FIELD, "ownerUserId.keyword", VALUE, ownerUserId)));
    boolRsQuery.addMustQuery(
        new QueryModel(
            QueryType.MATCH,
            Map.of(FIELD, "resourceServerRegURL.keyword", VALUE, resourceServerUrl)));
    QueryModel finalQuery = new QueryModel(QueryType.BOOL);
    finalQuery.setShouldQueries(List.of(boolIdQuery, boolRsQuery));
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(finalQuery);
    queryModel.setIncludeFields(List.of("type"));
    return  queryModel;
  }

  public static QueryModel getValidateQueryForResourceServer(String cos, String resourceServerUrl) {
    QueryModel boolIdQuery = new QueryModel(QueryType.BOOL);
    boolIdQuery.addMustQuery(
        new QueryModel(QueryType.MATCH, Map.of(FIELD, ID_KEYWORD, VALUE, cos)));
    QueryModel boolRsQuery = new QueryModel(QueryType.BOOL);
    boolRsQuery.addMustQuery(
        new QueryModel(
            QueryType.MATCH, Map.of(FIELD, "type.keyword", VALUE, ITEM_TYPE_RESOURCE_SERVER)));
    boolRsQuery.addMustQuery(
        new QueryModel(
            QueryType.MATCH,
            Map.of(FIELD, RESOURCE_SERVER_URL + ".keyword", VALUE, resourceServerUrl)));
    QueryModel finalQuery = new QueryModel(QueryType.BOOL);
    finalQuery.setShouldQueries(List.of(boolIdQuery, boolRsQuery));
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(finalQuery);
    queryModel.setIncludeFields(List.of("type"));
    return  queryModel;
  }

  public static QueryModel getValidateQueryForResource(String name, String resourceServer,
                                                       String provider, String resourceGroup) {
    QueryModel mustQuery = new QueryModel(QueryType.BOOL);
    mustQuery.addMustQuery(
        new QueryModel(QueryType.MATCH, Map.of(FIELD, "type.keyword", VALUE, "iudx:Resource")));
    mustQuery.addMustQuery(
        new QueryModel(
            QueryType.MATCH, Map.of(FIELD, "name.keyword", VALUE, name)));
    QueryModel finalQuery = new QueryModel(QueryType.BOOL);
    finalQuery.addShouldQuery(
        new QueryModel(QueryType.MATCH, Map.of(FIELD, ID_KEYWORD, VALUE, resourceServer)));
    finalQuery.addShouldQuery(
        new QueryModel(QueryType.MATCH, Map.of(FIELD, ID_KEYWORD, VALUE, provider)));
    finalQuery.addShouldQuery(
        new QueryModel(QueryType.MATCH, Map.of(FIELD, ID_KEYWORD, VALUE, resourceGroup)));
    finalQuery.addShouldQuery(mustQuery);
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(finalQuery);
    queryModel.setIncludeFields(List.of("type"));
    return  queryModel;
  }
  public static QueryModel getValidateQueryForCos(String name, String owner) {
    QueryModel boolIdQuery = new QueryModel(QueryType.BOOL);
    boolIdQuery.addMustQuery(
        new QueryModel(QueryType.MATCH, Map.of(FIELD, ID_KEYWORD, VALUE, owner)));
    QueryModel boolRsQuery = new QueryModel(QueryType.BOOL);
    boolRsQuery.addMustQuery(
        new QueryModel(QueryType.MATCH, Map.of(FIELD, "type.keyword", VALUE, ITEM_TYPE_COS)));
    boolRsQuery.addMustQuery(
        new QueryModel(
            QueryType.MATCH, Map.of(FIELD, NAME + ".keyword", VALUE, name)));
    QueryModel finalQuery = new QueryModel(QueryType.BOOL);
    finalQuery.setShouldQueries(List.of(boolIdQuery, boolRsQuery));
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(finalQuery);
    queryModel.setIncludeFields(List.of("type"));
    return queryModel;
  }

  public static QueryModel getValidateQueryForOwner(String name) {
    QueryModel query = new QueryModel(QueryType.BOOL);
    query.addMustQuery(new QueryModel(QueryType.MATCH, Map.of(FIELD, "type", VALUE,
        "iudx:Owner")));
    query.addMustQuery(
        new QueryModel(
            QueryType.MATCH, Map.of(FIELD, "name.keyword", VALUE, name)));

    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(query);
    return queryModel;
  }
}
