package iudx.catalogue.server.database.mlayer;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.ElasticClient;
import iudx.catalogue.server.database.RespBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MlayerProvider {
  private static final Logger LOGGER = LogManager.getLogger(MlayerProvider.class);
  private static String internalErrorResp =
      new RespBuilder()
          .withType(TYPE_INTERNAL_SERVER_ERROR)
          .withTitle(TITLE_INTERNAL_SERVER_ERROR)
          .withDetail(DETAIL_INTERNAL_SERVER_ERROR)
          .getResponse();
  ElasticClient client;
  String docIndex;

  public MlayerProvider(ElasticClient client, String docIndex) {
    this.client = client;
    this.docIndex = docIndex;
  }

  public void getMlayerProviders(
      JsonObject requestParams, Handler<AsyncResult<JsonObject>> handler) {
    String limit = requestParams.getString(LIMIT);
    String offset = requestParams.getString(OFFSET);
    if (requestParams.containsKey(INSTANCE)) {
      String query = GET_DATASET_BY_INSTANCE.replace("$1", requestParams.getString(INSTANCE));
      client.searchAsyncResourceGroupAndProvider(
          query,
          docIndex,
          resultHandler -> {
            if (resultHandler.succeeded()) {
              LOGGER.debug("Success: Successful DB Request");
              if (resultHandler.result().getJsonArray(RESULTS).isEmpty()) {
                handler.handle(Future.failedFuture(NO_CONTENT_AVAILABLE));
              }
              JsonObject resultHandlerResult = resultHandler.result();
              handler.handle(Future.succeededFuture(resultHandlerResult));
            } else {
              LOGGER.error("Fail: failed DB request");
              handler.handle(Future.failedFuture(internalErrorResp));
            }
          });
    } else {
      String query = GET_MLAYER_PROVIDERS_QUERY.replace("$0", limit).replace("$1", offset);
      client.searchAsync(
          query,
          docIndex,
          resultHandler -> {
            if (resultHandler.succeeded()) {
              LOGGER.debug("Success: Successful DB Request");
              JsonObject result = resultHandler.result();
              handler.handle(Future.succeededFuture(result));
            } else {
              LOGGER.error("Fail: failed DB request");
              handler.handle(Future.failedFuture(internalErrorResp));
            }
          });
    }
  }
}
