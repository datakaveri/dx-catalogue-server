package iudx.catalogue.server.mlayer.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.mlayer.model.MlayerDatasetRequest;
import iudx.catalogue.server.mlayer.model.MlayerDomainRequest;
import iudx.catalogue.server.mlayer.model.MlayerGeoQueryRequest;
import iudx.catalogue.server.mlayer.model.MlayerInstanceRequest;

@ProxyGen
@VertxGen
public interface MlayerService {
  @GenIgnore
  static MlayerService createProxy(Vertx vertx, String address) {
    return new MlayerServiceVertxEBProxy(vertx, address);
  }

  Future<JsonObject> createMlayerInstance(MlayerInstanceRequest request);

  Future<JsonObject> getMlayerInstance(JsonObject requestParams);

  Future<JsonObject> deleteMlayerInstance(String request);

  Future<JsonObject> updateMlayerInstance(MlayerInstanceRequest request);

  Future<JsonObject> createMlayerDomain(MlayerDomainRequest request);

  Future<JsonObject> getMlayerDomain(JsonObject requestParams);

  Future<JsonObject> deleteMlayerDomain(String request);

  Future<JsonObject> updateMlayerDomain(MlayerDomainRequest request);

  Future<JsonObject> getMlayerProviders(JsonObject requestParams);

  Future<JsonObject> getMlayerGeoQuery(MlayerGeoQueryRequest request);

  Future<JsonObject> getMlayerAllDatasets(Integer limit, Integer offset);

  Future<JsonObject> getMlayerDataset(MlayerDatasetRequest requestData);

  Future<JsonObject> getMlayerPopularDatasets(String instance);

  Future<JsonObject> getSummaryCountSizeApi();

  Future<JsonObject> getRealTimeDataSetApi();
}
