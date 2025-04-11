package iudx.catalogue.server.mlayer.service;

import static iudx.catalogue.server.mlayer.util.QueryModelUtil.buildQueryModelFromIds;
import static iudx.catalogue.server.util.Constants.DETAIL_INTERNAL_SERVER_ERROR;
import static iudx.catalogue.server.util.Constants.TITLE_INTERNAL_SERVER_ERROR;
import static iudx.catalogue.server.util.Constants.TYPE_INTERNAL_SERVER_ERROR;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.common.RespBuilder;
import iudx.catalogue.server.common.util.DbResponseMessageBuilder;
import iudx.catalogue.server.database.elastic.model.ElasticsearchResponse;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
import iudx.catalogue.server.mlayer.model.MlayerGeoQueryRequest;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MlayerGeoQueryService {
  private static final Logger LOGGER = LogManager.getLogger(MlayerGeoQueryService.class);
  private static final String internalErrorResp =
      new RespBuilder()
          .withType(TYPE_INTERNAL_SERVER_ERROR)
          .withTitle(TITLE_INTERNAL_SERVER_ERROR)
          .withDetail(DETAIL_INTERNAL_SERVER_ERROR)
          .getResponse();
  private final ElasticsearchService esService;
  String docIndex;

  public MlayerGeoQueryService(ElasticsearchService esService, String docIndex) {
    this.esService = esService;
    this.docIndex = docIndex;
  }

  public Future<JsonObject> getMlayerGeoQuery(MlayerGeoQueryRequest geoQueryModel) {
    String instance = geoQueryModel.getInstance();
    JsonArray id = new JsonArray(geoQueryModel.getId().stream().map(UUID::toString).collect(
        Collectors.toList()));
    QueryModel query = buildQueryModelFromIds(id, instance);
    Promise<JsonObject> promise = Promise.promise();
    esService.search(docIndex, query)
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

}
