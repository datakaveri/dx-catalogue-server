package iudx.catalogue.server.mlayer.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@ProxyGen
@VertxGen
public interface MlayerService {
  @GenIgnore
  static MlayerService createProxy(Vertx vertx, String address) {
    return new MlayerServiceVertxEBProxy(vertx, address);
  }

  Future<JsonObject> createMlayerInstance(JsonObject request);

  Future<JsonObject> getMlayerInstance(JsonObject requestParams);

  Future<JsonObject> deleteMlayerInstance(String request);

  Future<JsonObject> updateMlayerInstance(JsonObject request);

  Future<JsonObject> createMlayerDomain(JsonObject request);

  Future<JsonObject> getMlayerDomain(JsonObject requestParams);

  Future<JsonObject> deleteMlayerDomain(String request);

  Future<JsonObject> updateMlayerDomain(JsonObject request);

  Future<JsonObject> getMlayerProviders(JsonObject requestParams);

  Future<JsonObject> getMlayerGeoQuery(JsonObject request);

  Future<JsonObject> getMlayerAllDatasets(JsonObject requestParam);

  Future<JsonObject> getMlayerDataset(JsonObject requestData);

  Future<JsonObject> getMlayerPopularDatasets(String instance);

  Future<JsonObject> getSummaryCountSizeApi();

  Future<JsonObject> getRealTimeDataSetApi();
}
