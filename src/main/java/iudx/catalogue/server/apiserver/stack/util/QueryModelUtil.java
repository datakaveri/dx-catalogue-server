package iudx.catalogue.server.apiserver.stack.util;

import co.elastic.clients.json.JsonData;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.apiserver.stack.model.StacLink;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import iudx.catalogue.server.database.elastic.util.QueryType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryModelUtil {
  private static final QueryBuilder queryBuilder = new QueryBuilder();
  public static QueryModel getStacQuery(String stacId) {
    QueryModel query = queryBuilder.getQuery(stacId);
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(query);
    return queryModel;
  }

  public static QueryModel checkStacCatItemQuery(JsonObject request) {
    QueryModel query = queryBuilder.getQuery4CheckExistence(request);
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(query);
    return queryModel;
  }

  public static QueryModel getPatchQuery(StacLink request) {
    QueryModel queryModel = new QueryModel();
    Map<String, Object> params = new HashMap<>();

    // Add mandatory fields
    params.put("rel", request.getRel());
    params.put("href", request.getHref());

    // Add optional fields if they exist
    if (request.getType() != null) {
      params.put("type", request.getType());
    }
    if (request.getTitle() != null) {
      params.put("title", request.getTitle());
    }

    queryModel.setScriptSource("ctx._source.links.add(params)");
    queryModel.setScriptLanguage("painless");
    queryModel.setScriptParams(params);
    return queryModel;
  }

}
