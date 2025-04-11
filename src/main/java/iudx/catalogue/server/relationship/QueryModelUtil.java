package iudx.catalogue.server.relationship;

import static iudx.catalogue.server.auditing.util.Constants.ID;
import static iudx.catalogue.server.database.elastic.util.Constants.ID_KEYWORD;
import static iudx.catalogue.server.database.elastic.util.Constants.KEYWORD_KEY;
import static iudx.catalogue.server.database.elastic.util.Constants.TYPE_KEYWORD;
import static iudx.catalogue.server.util.Constants.FIELD;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_PROVIDER;
import static iudx.catalogue.server.util.Constants.MAX_LIMIT;
import static iudx.catalogue.server.util.Constants.RESOURCE_SVR;
import static iudx.catalogue.server.util.Constants.VALUE;

import io.vertx.core.json.JsonArray;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import iudx.catalogue.server.database.elastic.util.QueryType;
import java.util.List;
import java.util.Map;

public class QueryModelUtil {

  // Basic TERM Query
  public static QueryModel createTermQuery(String field, String value) {
    return new QueryModel(QueryType.TERM, Map.of(FIELD, field, VALUE, value));
  }
  public static QueryModel listRelQuery(String id) {
    QueryModel termsQuery = createTermQuery(ID_KEYWORD, id);
    QueryModel typeQuery = new QueryModel(QueryType.BOOL);
    typeQuery.setFilterQueries(List.of(termsQuery));
    List<String> sourceFields =
        List.of("cos", "resourceServer", "type", "provider", "resourceGroup", "id");
    QueryModel queryModel = new QueryModel();
    queryModel.setIncludeFields(sourceFields);
    queryModel.setQueries(typeQuery);
    return queryModel;
  }

  public static QueryModel getResourceGroupsForRsQuery(String id) {
    QueryModel queryModel = new QueryModel();

    QueryModel typeQueryModel4RsGroup = new QueryModel(QueryType.BOOL);
    typeQueryModel4RsGroup.addMustQuery(new QueryModel(QueryType.MATCH,
        Map.of(FIELD, RESOURCE_SVR + KEYWORD_KEY, VALUE, id)));
    typeQueryModel4RsGroup.addMustQuery(new QueryModel(QueryType.TERM,
        Map.of(FIELD, TYPE_KEYWORD, VALUE, ITEM_TYPE_PROVIDER)));

    queryModel.setQueries(typeQueryModel4RsGroup);
    queryModel.setIncludeFields(List.of(ID));
    queryModel.setLimit(MAX_LIMIT);
    //LOGGER.debug("INFO: typeQueryModel4RsGroup build");
    return queryModel;
  }

  public static QueryModel getRsForResourceGroupQuery(String id) {
    QueryModel queryModel = new QueryModel();

    QueryModel typeQueryModel4RsServer = new QueryModel(QueryType.BOOL);
    typeQueryModel4RsServer.addFilterQuery(new QueryModel(QueryType.TERMS,
        Map.of(FIELD, ID_KEYWORD, VALUE, List.of(id)))); //Provider id
    typeQueryModel4RsServer.setIncludeFields(List.of("cos", "resourceServer", "type", "provider",
        "resourceGroup", "id"));

    queryModel.setQueries(typeQueryModel4RsServer);
    //LOGGER.debug("INFO: typeQueryModel4RsServer build");
    return queryModel;
  }

  public static QueryModel createRelationshipQueryModel(String typeValue, String relReqsKey,
                                                        String relReqsValue) {
    QueryModel relationshipQueryModel = new QueryModel(QueryType.BOOL);

    relationshipQueryModel.addMustQuery(
        new QueryModel(QueryType.TERM, Map.of(FIELD, TYPE_KEYWORD, VALUE, typeValue)));
    relationshipQueryModel.addMustQuery(new QueryModel(QueryType.MATCH, Map.of(FIELD, relReqsKey,
        VALUE, relReqsValue)));
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(relationshipQueryModel);
    queryModel.setIncludeFields(List.of(ID));
    return queryModel;
  }

  public static QueryModel createIdWildcardQueryModel(JsonArray ids, String limit, String offset) {
    QueryModel idWildcardQueryModel = new QueryModel(QueryType.BOOL);
    ids.forEach(id -> idWildcardQueryModel.addShouldQuery(new QueryModel(QueryType.WILDCARD,
        Map.of(FIELD, ID_KEYWORD, VALUE, id + "*"))));
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(idWildcardQueryModel);
    if (limit != null) {
      queryModel.setLimit(limit);
    }
    if (offset!= null) {
      queryModel.setOffset(offset);
    }
    return queryModel;
  }

}
