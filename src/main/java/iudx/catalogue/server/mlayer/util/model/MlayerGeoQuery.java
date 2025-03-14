package iudx.catalogue.server.mlayer.util.model;

import static iudx.catalogue.server.database.elastic.util.Constants.ID_KEYWORD;
import static iudx.catalogue.server.util.Constants.DETAIL_INTERNAL_SERVER_ERROR;
import static iudx.catalogue.server.util.Constants.FIELD;
import static iudx.catalogue.server.util.Constants.ID;
import static iudx.catalogue.server.util.Constants.INSTANCE;
import static iudx.catalogue.server.util.Constants.TITLE_INTERNAL_SERVER_ERROR;
import static iudx.catalogue.server.util.Constants.TYPE_INTERNAL_SERVER_ERROR;
import static iudx.catalogue.server.util.Constants.VALUE;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.common.RespBuilder;
import iudx.catalogue.server.common.util.DbResponseMessageBuilder;
import iudx.catalogue.server.database.elastic.model.ElasticsearchResponse;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
import iudx.catalogue.server.database.elastic.util.QueryType;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MlayerGeoQuery {
  private static final Logger LOGGER = LogManager.getLogger(MlayerGeoQuery.class);
  private static String internalErrorResp =
      new RespBuilder()
          .withType(TYPE_INTERNAL_SERVER_ERROR)
          .withTitle(TITLE_INTERNAL_SERVER_ERROR)
          .withDetail(DETAIL_INTERNAL_SERVER_ERROR)
          .getResponse();
  ElasticsearchService esService;
  String docIndex;

  public MlayerGeoQuery(ElasticsearchService esService, String docIndex) {
    this.esService = esService;
    this.docIndex = docIndex;
  }

  public Future<JsonObject> getMlayerGeoQuery(JsonObject request) {
    String instance = request.getString(INSTANCE);
    JsonArray id = request.getJsonArray(ID);
    QueryModel query = new QueryModel(QueryType.BOOL);
    for (int i = 0; i < id.size(); i++) {
      String datasetId = id.getString(i);

      QueryModel subQueryModel =
          getQueryModel(instance, datasetId);

      query.addShouldQuery(subQueryModel);
    }

    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(query);
    queryModel.setMinimumShouldMatch("1");
    queryModel.setIncludeFields(List.of("id", "location", "instance", "label"));
    Promise<JsonObject> promise = Promise.promise();
    esService.search(docIndex, queryModel)
        .onComplete(resultHandler -> {
          if (resultHandler.succeeded()) {
            List<ElasticsearchResponse> response = resultHandler.result();
            DbResponseMessageBuilder responseMsg = new DbResponseMessageBuilder();
            responseMsg.statusSuccess().setTotalHits(response.size());
            response.stream()
                .map(elasticResponse -> {
                  JsonObject json = new JsonObject(elasticResponse.getSource().toString());
                  json.put("doc_id", elasticResponse.getDocId());
                  return json;
                })
                .forEach(responseMsg::addResult);

            LOGGER.debug("Success: Successful DB Request");
            promise.complete(responseMsg.getResponse());

          } else {

            LOGGER.error("Fail: failed DB request");
            promise.fail(internalErrorResp);
          }
        });
    return promise.future();
  }

  private static QueryModel getQueryModel(String instance, String datasetId) {
    QueryModel subQueryModel = new QueryModel(QueryType.BOOL);
    subQueryModel.setShouldQueries(List.of(new QueryModel(QueryType.MATCH,
        Map.of(FIELD, "type.keyword", VALUE, "iudx:Resource")), new QueryModel(QueryType.MATCH,
        Map.of(FIELD, "type.keyword", VALUE, "iudx:ResourceGroup"))));
    subQueryModel.setMustQueries(List.of(new QueryModel(QueryType.MATCH,
        Map.of(FIELD, "instance.keyword", VALUE, instance)), new QueryModel(QueryType.MATCH,
        Map.of(FIELD, ID_KEYWORD, VALUE, datasetId))));
    return subQueryModel;
  }
}
