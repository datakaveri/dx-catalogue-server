package iudx.catalogue.server.validator;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * The Validator Service.
 * <h1>Validator Service</h1>
 *
 * <p>
 * The Validator Service in the IUDX Catalogue Server defines the operations to be performed with
 * the IUDX File server.
 * </p>
 *
 *
 * @see io.vertx.codegen.annotations.ProxyGen
 *
 * @see io.vertx.codegen.annotations.VertxGen
 * @version 1.0
 * @since 2020-05-31
 */

@VertxGen
@ProxyGen
public interface ValidatorService {

  /**
   * The createProxy helps the code generation blocks to generate proxy code.
   *
   * @param vertx which is the vertx instance
   * @param address which is the proxy address
   * @return ValidatorServiceVertxEBProxy which is a service proxy
   */

  @GenIgnore
  static ValidatorService createProxy(Vertx vertx, String address) {
    return new ValidatorServiceVertxEBProxy(vertx, address);
  }

  /**
   * The validateSchema method implements the item schema validation.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return ValidatorService which is a Service
   */
  @Fluent
  ValidatorService validateSchema(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The validateItem method implements the item validation flow based on the schema of the item.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return ValidatorService which is a Service
   */
  @Fluent
  ValidatorService validateItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  ValidatorService validateRating(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  ValidatorService validateMlayerInstance(JsonObject request,
                                          Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  ValidatorService validateMlayerDomain(JsonObject request,
                                        Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  ValidatorService validateMlayerGeoQuery(JsonObject request,
                                           Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  ValidatorService validateMlayerDatasetId(JsonObject requestData,
                                           Handler<AsyncResult<JsonObject>> handler);


}