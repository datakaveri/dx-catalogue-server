package iudx.catalogue.server.apiserver.item.model;

import static iudx.catalogue.server.auditing.util.Constants.ID;
import static iudx.catalogue.server.database.elastic.util.Constants.ID_KEYWORD;
import static iudx.catalogue.server.database.elastic.util.Constants.KEYWORD_KEY;
import static iudx.catalogue.server.database.elastic.util.Constants.TYPE_KEYWORD;
import static iudx.catalogue.server.util.Constants.COS;
import static iudx.catalogue.server.util.Constants.FIELD;
import static iudx.catalogue.server.util.Constants.FILTER_PAGINATION_FROM;
import static iudx.catalogue.server.util.Constants.MAX_LIMIT;
import static iudx.catalogue.server.util.Constants.PROVIDER;
import static iudx.catalogue.server.util.Constants.RESOURCE_GRP;
import static iudx.catalogue.server.util.Constants.RESOURCE_SVR;
import static iudx.catalogue.server.util.Constants.VALUE;

import iudx.catalogue.server.database.elastic.model.QueryModel;
import iudx.catalogue.server.database.elastic.util.BoolOperator;
import iudx.catalogue.server.database.elastic.util.QueryType;
import java.util.List;
import java.util.Map;

public class QueryModelUtil {
  public static QueryModel createTermQuery(String field, String value) {
    return new QueryModel(QueryType.TERM, Map.of(FIELD, field, VALUE, value));
  }
  public static QueryModel createMatchQuery(String field, String value) {
    return new QueryModel(QueryType.MATCH, Map.of(FIELD, field, VALUE, value));
  }
  public static QueryModel createGetItemQuery(String id, List<String> includeFields) {
    QueryModel queryModel = new QueryModel();
    QueryModel idTermQuery = createTermQuery(ID_KEYWORD, id);

    queryModel.setQueries(idTermQuery);
    if (includeFields != null && !includeFields.isEmpty()) {
      queryModel.setIncludeFields(includeFields);
    }
    queryModel.setLimit(MAX_LIMIT);
    queryModel.setOffset(FILTER_PAGINATION_FROM);

    return queryModel;
  }

  public static QueryModel createItemExistenceQuery(String id, String type) {
    QueryModel idTermQuery = createTermQuery(ID_KEYWORD, id);
    QueryModel typeMatchQuery = createMatchQuery(TYPE_KEYWORD, type);

    QueryModel query = new QueryModel();
    QueryModel checkItemExistenceQuery =
        new QueryModel(BoolOperator.MUST, List.of(idTermQuery, typeMatchQuery));
    query.setQueries(checkItemExistenceQuery);
    query.setIncludeFields(List.of(ID)); // Only include ID field in the search results
    return query;
  }

  public static QueryModel checkQueryModel(String id) {
    List<String> fields =
        List.of(
            ID_KEYWORD,
            RESOURCE_GRP + KEYWORD_KEY,
            PROVIDER + KEYWORD_KEY,
            RESOURCE_SVR + KEYWORD_KEY,
            COS + KEYWORD_KEY);

    List<QueryModel> shouldQueries =
        fields.stream().map(field -> createTermQuery(field, id)).toList();
    QueryModel queryResourceGrp = new QueryModel();
    queryResourceGrp.setQueries(new QueryModel(BoolOperator.SHOULD, shouldQueries));
    return queryResourceGrp;
  }

  public static QueryModel createVerifyInstanceQuery(String instanceId) {
    QueryModel queryModel = new QueryModel();
    QueryModel instanceMatchQuery = createMatchQuery(ID, instanceId);

    queryModel.setQueries(instanceMatchQuery);
    queryModel.setLimit(MAX_LIMIT);
    queryModel.setOffset(FILTER_PAGINATION_FROM);

    return queryModel;
  }
}
