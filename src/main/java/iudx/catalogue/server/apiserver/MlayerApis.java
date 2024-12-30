package iudx.catalogue.server.apiserver;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.authenticator.Constants.*;
import static iudx.catalogue.server.mlayer.util.Constants.*;
import static iudx.catalogue.server.mlayer.util.Constants.METHOD;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.validator.Constants.VALIDATION_FAILURE_MSG;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.apiserver.util.RespBuilder;
import iudx.catalogue.server.authenticator.AuthenticationService;
import iudx.catalogue.server.mlayer.MlayerService;
import iudx.catalogue.server.util.Api;
import iudx.catalogue.server.validator.ValidatorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MlayerApis {
  private static final Logger LOGGER = LogManager.getLogger(MlayerApis.class);
  private MlayerService mlayerService;
  private AuthenticationService authService;
  private ValidatorService validatorService;
  private String host;
  private Api api;

  public MlayerApis(Api api) {
    this.api = api;
  }

  public void setMlayerService(MlayerService mlayerService) {
    this.mlayerService = mlayerService;
  }

  public void setAuthService(AuthenticationService authService) {
    this.authService = authService;
  }

  public void setValidatorService(ValidatorService validatorService) {
    this.validatorService = validatorService;
  }

  public void setHost(String host) {
    this.host = host;
  }

  /**
   * Create Mlayer Instance Handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void createMlayerInstanceHandler(RoutingContext routingContext) {
    LOGGER.debug("Info: Create Instance");

    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject jwtAuthInfo = new JsonObject();

    jwtAuthInfo
        .put(TOKEN, request.getHeader(HEADER_TOKEN))
        .put(METHOD, REQUEST_POST)
        .put(API_ENDPOINT, api.getRouteMlayerInstance())
        .put(ID, host);

    Future<JsonObject> authFuture = inspectToken(jwtAuthInfo);

    authFuture
        .onSuccess(
            successHandler -> {
              validatorService.validateMlayerInstance(
                  requestBody,
                  validateHandler -> {
                    if (validateHandler.failed()) {
                      response
                          .setStatusCode(400)
                          .end(
                              new RespBuilder()
                                  .withType(TYPE_INVALID_SCHEMA)
                                  .withTitle(TITLE_INVALID_SCHEMA)
                                  .withDetail("The Schema of requested body is invalid.")
                                  .getResponse());
                    } else {
                      LOGGER.debug("Validation Successful");
                      mlayerService.createMlayerInstance(
                          requestBody,
                          handler -> {
                            if (handler.succeeded()) {
                              response.setStatusCode(201).end(handler.result().toString());

                            } else {
                              if (handler.cause().getMessage().contains("Item already exists")) {
                                response.setStatusCode(409).end(handler.cause().getMessage());
                              } else {
                                response.setStatusCode(400).end(handler.cause().getMessage());
                              }
                            }
                          });
                    }
                  });
            })
        .onFailure(
            failureHandler -> {
              response.setStatusCode(401).end(failureHandler.getMessage());
            });
  }

  /**
   * Get mlayer instance handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void getMlayerInstanceHandler(RoutingContext routingContext) {
    LOGGER.debug("Info : fetching mlayer Instance");
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
    JsonObject requestParams = parseRequestParams(routingContext);
    mlayerService.getMlayerInstance(
        requestParams,
        handler -> {
          if (handler.succeeded()) {
            response.setStatusCode(200).end(handler.result().toString());
          } else {
            response.setStatusCode(400).end(handler.cause().getMessage());
          }
        });
  }

  /**
   * Delete Mlayer Instance Handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void deleteMlayerInstanceHandler(RoutingContext routingContext) {
    LOGGER.debug("Info : deleting mlayer Instance");

    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject jwtAuthInfo = new JsonObject();

    jwtAuthInfo
        .put(TOKEN, request.getHeader(HEADER_TOKEN))
        .put(METHOD, REQUEST_DELETE)
        .put(API_ENDPOINT, api.getRouteMlayerInstance())
        .put(ID, host);

    Future<JsonObject> authFuture = inspectToken(jwtAuthInfo);
    authFuture
        .onSuccess(
            successHandler -> {
              String instanceId = request.getParam(MLAYER_ID);
              mlayerService.deleteMlayerInstance(
                  instanceId,
                  dbHandler -> {
                    if (dbHandler.succeeded()) {
                      LOGGER.info("Success: Item deleted");
                      LOGGER.debug(dbHandler.result().toString());
                      response.setStatusCode(200).end(dbHandler.result().toString());
                    } else {
                      if (dbHandler.cause().getMessage().contains("urn:dx:cat:ItemNotFound")) {
                        response.setStatusCode(404).end(dbHandler.cause().getMessage());
                      } else {
                        response.setStatusCode(400).end(dbHandler.cause().getMessage());
                      }
                    }
                  });
            })
        .onFailure(
            failureHandler -> {
              response.setStatusCode(401).end(failureHandler.getMessage());
            });
  }

  /**
   * Update Mlayer Instance Handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void updateMlayerInstanceHandler(RoutingContext routingContext) {
    LOGGER.debug("Info: Updating Mlayer Instance");

    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject jwtAuthInfo = new JsonObject();
    jwtAuthInfo
        .put(TOKEN, request.getHeader(HEADER_TOKEN))
        .put(METHOD, REQUEST_PUT)
        .put(API_ENDPOINT, api.getRouteMlayerInstance())
        .put(ID, host);

    Future<JsonObject> authFuture = inspectToken(jwtAuthInfo);
    authFuture
        .onSuccess(
            successHandler -> {
              validatorService.validateMlayerInstance(
                  requestBody,
                  validationHandler -> {
                    if (validationHandler.failed()) {
                      response
                          .setStatusCode(400)
                          .end(
                              new RespBuilder()
                                  .withType(TYPE_INVALID_SCHEMA)
                                  .withTitle(TITLE_INVALID_SCHEMA)
                                  .withDetail("The Schema of requested body is invalid.")
                                  .getResponse());
                    } else {

                      String instanceId = request.getParam(MLAYER_ID);
                      requestBody.put(INSTANCE_ID, instanceId);
                      mlayerService.updateMlayerInstance(
                          requestBody,
                          handler -> {
                            if (handler.succeeded()) {
                              response.setStatusCode(200).end(handler.result().toString());
                            } else {
                              response.setStatusCode(400).end(handler.cause().getMessage());
                            }
                          });
                    }
                  });
            })
        .onFailure(
            failureHandler -> {
              response.setStatusCode(401).end(failureHandler.getMessage());
            });
  }

  private Future<JsonObject> inspectToken(JsonObject jwtAuthInfo) {
    Promise<JsonObject> promise = Promise.promise();

    authService.tokenInterospect(
        new JsonObject(),
        jwtAuthInfo,
        authHandler -> {
          if (authHandler.succeeded()) {
            LOGGER.debug("JWT Auth Successful");
            LOGGER.debug(authHandler.result());
            promise.complete(authHandler.result());
          } else {
            LOGGER.error(authHandler.cause().getMessage());
            promise.fail(
                new RespBuilder()
                    .withType(TYPE_TOKEN_INVALID)
                    .withTitle(TITLE_TOKEN_INVALID)
                    .withDetail(DETAIL_INVALID_TOKEN)
                    .getResponse());
          }
        });
    return promise.future();
  }

  /**
   * Create Mlayer Domain Handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void createMlayerDomainHandler(RoutingContext routingContext) {
    LOGGER.debug("Info: Doamin Created");

    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject jwtAuthInfo = new JsonObject();
    jwtAuthInfo
        .put(TOKEN, request.getHeader(HEADER_TOKEN))
        .put(METHOD, REQUEST_POST)
        .put(API_ENDPOINT, api.getRouteMlayerDomains())
        .put(ID, host);

    Future<JsonObject> authenticationFuture = inspectToken(jwtAuthInfo);
    authenticationFuture
        .onSuccess(
            successHandler -> {
              validatorService.validateMlayerDomain(
                  requestBody,
                  validateHandler -> {
                    if (validateHandler.failed()) {
                      response
                          .setStatusCode(400)
                          .end(
                              new RespBuilder()
                                  .withType(TYPE_INVALID_SCHEMA)
                                  .withTitle(TITLE_INVALID_SCHEMA)
                                  .withDetail("The Schema of requested body is invalid.")
                                  .getResponse());
                    } else {
                      LOGGER.debug("Validation Successful");
                      mlayerService.createMlayerDomain(
                          requestBody,
                          handler -> {
                            if (handler.succeeded()) {
                              response.setStatusCode(201).end(handler.result().toString());
                            } else {
                              if (handler.cause().getMessage().contains("Item already exists")) {
                                response.setStatusCode(409).end(handler.cause().getMessage());
                              } else {
                                response.setStatusCode(400).end(handler.cause().getMessage());
                              }
                            }
                          });
                    }
                  });
            })
        .onFailure(
            failureHandler -> {
              response.setStatusCode(401).end(failureHandler.getMessage());
            });
  }

  /**
   * Get Mlayer Domain Handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void getMlayerDomainHandler(RoutingContext routingContext) {
    LOGGER.debug("Info: getMlayerDomainHandler() started");
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
    JsonObject requestParams = parseRequestParams(routingContext);
    mlayerService.getMlayerDomain(
        requestParams,
        handler -> {
          if (handler.succeeded()) {
            response.setStatusCode(200).end(handler.result().toString());
          } else {
            response.setStatusCode(400).end(handler.cause().getMessage());
          }
        });
  }

  private JsonObject parseRequestParams(RoutingContext routingContext) {
    LOGGER.debug("Info: parseRequestParams() started");

    JsonObject requestParams = new JsonObject();
    String id = routingContext.request().getParam(ID);
    String limit = routingContext.request().getParam(LIMIT);
    String offset = routingContext.request().getParam(OFFSET);

    int limitInt = 10000;
    int offsetInt = 0;

    if (id != null) {
      return requestParams.put(ID, id);
    }

    if (limit != null && !limit.isBlank()) {
      if (validateLimitAndOffset(limit)) {
        limitInt = Integer.parseInt(limit);
      } else {
        handleInvalidParameter(400, "Invalid limit parameter", routingContext);
      }
    }
    if (offset != null && !offset.isBlank()) {
      if (validateLimitAndOffset(offset)) {
        offsetInt = Integer.parseInt(offset);
        if (limitInt + offsetInt > 10000) {
          if (limitInt > offsetInt) {
            limitInt = limitInt - offsetInt;
          } else {
            offsetInt = offsetInt - limitInt;
          }
        }
      } else {
        handleInvalidParameter(400, "Invalid offset parameter", routingContext);
      }
    }
    requestParams.put(LIMIT, limitInt).put(OFFSET, offsetInt);
    return requestParams;
  }

  boolean validateLimitAndOffset(String value) {
    try {
      int size = Integer.parseInt(value);
      if (size > 10000 || size < 0) {
        LOGGER.error(
            "Validation error : invalid pagination limit Value > 10000 or negative value passed [ "
                + value
                + " ]");
        return false;
      }
      return true;
    } catch (NumberFormatException e) {
      LOGGER.error(
          "Validation error : invalid pagination limit Value [ "
              + value
              + " ] only integer expected");
      return false;
    }
  }

  private void handleInvalidParameter(
      int statusCode, String errorMessage, RoutingContext routingContext) {
    LOGGER.error(errorMessage);
    String responseMessage =
        new RespBuilder()
            .withType(TYPE_INVALID_QUERY_PARAM_VALUE)
            .withTitle(TITLE_INVALID_QUERY_PARAM_VALUE)
            .withDetail(errorMessage)
            .getResponse();
    routingContext.response().setStatusCode(statusCode).end(responseMessage);
  }

  /**
   * Update Mlayer Domain Handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void updateMlayerDomainHandler(RoutingContext routingContext) {
    LOGGER.debug("Info: Updating Mlayer Instance");

    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject jwtAuthInfo = new JsonObject();
    jwtAuthInfo
        .put(TOKEN, request.getHeader(HEADER_TOKEN))
        .put(METHOD, REQUEST_PUT)
        .put(API_ENDPOINT, api.getRouteMlayerDomains())
        .put(ID, host);

    Future<JsonObject> authFuture = inspectToken(jwtAuthInfo);
    authFuture
        .onSuccess(
            successHandler -> {
              validatorService.validateMlayerDomain(
                  requestBody,
                  validationHandler -> {
                    if (validationHandler.failed()) {
                      response
                          .setStatusCode(400)
                          .end(
                              new RespBuilder()
                                  .withType(TYPE_INVALID_SCHEMA)
                                  .withTitle(TITLE_INVALID_SCHEMA)
                                  .withDetail("The Schema of requested body is invalid.")
                                  .getResponse());
                    } else {

                      String domainId = request.getParam(MLAYER_ID);
                      requestBody.put(DOMAIN_ID, domainId);
                      mlayerService.updateMlayerDomain(
                          requestBody,
                          handler -> {
                            if (handler.succeeded()) {
                              response.setStatusCode(200).end(handler.result().toString());
                            } else {
                              response.setStatusCode(400).end(handler.cause().getMessage());
                            }
                          });
                    }
                  });
            })
        .onFailure(
            failureHandler -> {
              response.setStatusCode(401).end(failureHandler.getMessage());
            });
  }

  /**
   * Delete Mlayer Domain Handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void deleteMlayerDomainHandler(RoutingContext routingContext) {
    LOGGER.debug("Info : deleting mlayer Domain");

    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject jwtAuthInfo = new JsonObject();

    jwtAuthInfo
        .put(TOKEN, request.getHeader(HEADER_TOKEN))
        .put(METHOD, REQUEST_DELETE)
        .put(API_ENDPOINT, api.getRouteMlayerDomains())
        .put(ID, host);

    Future<JsonObject> authenticationFuture = inspectToken(jwtAuthInfo);
    authenticationFuture
        .onSuccess(
            successHandler -> {
              String domainId = request.getParam(MLAYER_ID);
              mlayerService.deleteMlayerDomain(
                  domainId,
                  dbHandler -> {
                    if (dbHandler.succeeded()) {
                      LOGGER.info("Success: Item deleted");
                      LOGGER.debug(dbHandler.result().toString());
                      response.setStatusCode(200).end(dbHandler.result().toString());
                    } else {
                      if (dbHandler.cause().getMessage().contains("urn:dx:cat:ItemNotFound")) {
                        response.setStatusCode(404).end(dbHandler.cause().getMessage());
                      } else {
                        response.setStatusCode(400).end(dbHandler.cause().getMessage());
                      }
                    }
                  });
            })
        .onFailure(
            failureHandler -> {
              response.setStatusCode(401).end(failureHandler.getMessage());
            });
  }

  /**
   * Get mlayer providers handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void getMlayerProvidersHandler(RoutingContext routingContext) {
    LOGGER.debug("Info : fetching mlayer Providers");
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
    JsonObject requestParams = parseRequestParams(routingContext);
    if (routingContext.request().getParam(INSTANCE) != null) {
      routingContext.request().getParam(INSTANCE);
      requestParams.put(INSTANCE, routingContext.request().getParam(INSTANCE));
      LOGGER.debug("Instance {}", requestParams.getString(INSTANCE));
    }
    mlayerService.getMlayerProviders(
        requestParams,
        handler -> {
          if (handler.succeeded()) {
            response.setStatusCode(200).end(handler.result().toString());
          } else {
            if (handler.cause().getMessage().equals("No Content Available")) {
              response.setStatusCode(204).end();
              return;
            } else if (handler.cause().getMessage().contains(VALIDATION_FAILURE_MSG)) {
              response
                  .setStatusCode(400)
                  .end(
                      new RespBuilder()
                          .withType(TYPE_INVALID_SCHEMA)
                          .withTitle(TITLE_INVALID_SCHEMA)
                          .withDetail("The Schema of dataset is invalid")
                          .getResponse());
              return;
            } else {
              response.setStatusCode(400).end(handler.cause().getMessage());
            }
          }
        });
  }

  /**
   * Get mlayer GeoQuery Handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void getMlayerGeoQueryHandler(RoutingContext routingContext) {
    LOGGER.debug("Info : fetching location and label of datasets");
    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
    validatorService.validateMlayerGeoQuery(
        requestBody,
        validationHandler -> {
          if (validationHandler.failed()) {
            response
                .setStatusCode(400)
                .end(
                    new RespBuilder()
                        .withType(TYPE_INVALID_SCHEMA)
                        .withTitle(TITLE_INVALID_SCHEMA)
                        .withDetail("The Schema of requested body is invalid.")
                        .getResponse());
          } else {
            mlayerService.getMlayerGeoQuery(
                requestBody,
                handler -> {
                  if (handler.succeeded()) {
                    response.setStatusCode(200).end(handler.result().toString());
                  } else {
                    response.setStatusCode(400).end(handler.cause().getMessage());
                  }
                });
          }
        });
  }

  /**
   * Get mlayer All Datasets Handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void getMlayerAllDatasetsHandler(RoutingContext routingContext) {
    LOGGER.debug("Info : fetching all datasets that belong to IUDX");
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
    JsonObject requestParams = parseRequestParams(routingContext);
    mlayerService.getMlayerAllDatasets(
        requestParams,
        handler -> {
          if (handler.succeeded()) {
            response.setStatusCode(200).end(handler.result().toString());
          } else {
            if (handler.cause().getMessage().contains(VALIDATION_FAILURE_MSG)) {
              response
                  .setStatusCode(400)
                  .end(
                      new RespBuilder()
                          .withType(TYPE_INVALID_SCHEMA)
                          .withTitle(TITLE_INVALID_SCHEMA)
                          .withDetail("The Schema of dataset is invalid")
                          .getResponse());
            } else if (handler.cause().getMessage().contains(NO_CONTENT_AVAILABLE)) {
              response.setStatusCode(204).end();
            } else {
              response.setStatusCode(400).end(handler.cause().getMessage());
            }
          }
        });
  }

  /**
   * Get mlayer Dataset Handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void getMlayerDatasetHandler(RoutingContext routingContext) {
    LOGGER.debug("Info : fetching details of the dataset");
    HttpServerResponse response = routingContext.response();
    JsonObject requestData = routingContext.body().asJsonObject();

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    validatorService.validateMlayerDatasetId(
        requestData,
        validationHandler -> {
          if (validationHandler.failed()) {
            response
                .setStatusCode(400)
                .end(
                    new RespBuilder()
                        .withType(TYPE_INVALID_SCHEMA)
                        .withTitle(TITLE_INVALID_SCHEMA)
                        .withDetail("The Schema of requested body is invalid.")
                        .getResponse());
          } else {
            LOGGER.debug("Validation of dataset Id Successful");
            JsonObject requestParam = parseRequestParams(routingContext);
            requestData
                .put(LIMIT, requestParam.getInteger(LIMIT))
                .put(OFFSET, requestParam.getInteger(OFFSET));
            mlayerService.getMlayerDataset(
                requestData,
                handler -> {
                  if (handler.succeeded()) {
                    response.setStatusCode(200).end(handler.result().toString());
                  } else {
                    if (handler.cause().getMessage().contains(NO_CONTENT_AVAILABLE)) {
                      response.setStatusCode(204).end();
                    } else if (handler.cause().getMessage().contains("urn:dx:cat:ItemNotFound")) {
                        response.setStatusCode(404).end(handler.cause().getMessage());
                    } else if (handler.cause().getMessage().contains(VALIDATION_FAILURE_MSG)) {
                      response
                          .setStatusCode(400)
                          .end(
                              new RespBuilder()
                                  .withType(TYPE_INVALID_SCHEMA)
                                  .withTitle(TITLE_INVALID_SCHEMA)
                                  .withDetail("The Schema of dataset is invalid")
                                  .getResponse());
                    } else {
                      response.setStatusCode(400).end(handler.cause().getMessage());
                    }
                  }
                });
          }
        });
  }

  /**
   * Get mlayer popular Datasets handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void getMlayerPopularDatasetsHandler(RoutingContext routingContext) {
    LOGGER.debug("Info : fetching the data for the landing Page");
    String instance = "";
    if (routingContext.request().getParam(INSTANCE) != null) {
      instance = routingContext.request().getParam(INSTANCE);
    }
    LOGGER.debug("Instance {}", instance);
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
    mlayerService.getMlayerPopularDatasets(
        instance,
        handler -> {
          if (handler.succeeded()) {
            response.setStatusCode(200).end(handler.result().toString());
          } else {
            if (handler.cause().getMessage().contains(VALIDATION_FAILURE_MSG)) {
              response
                  .setStatusCode(400)
                  .end(
                      new RespBuilder()
                          .withType(TYPE_INVALID_SCHEMA)
                          .withTitle(TITLE_INVALID_SCHEMA)
                          .withDetail("The Schema of dataset is invalid")
                          .getResponse());
            } else if (handler.cause().getMessage().contains(NO_CONTENT_AVAILABLE)) {
              response.setStatusCode(204).end();
            } else {
              response.setStatusCode(400).end(handler.cause().getMessage());
            }
          }
        });
  }

  /**
   * Get mlayer total count and size.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void getSummaryCountSizeApi(RoutingContext routingContext) {
    LOGGER.debug("Info : fetching total counts");
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
    mlayerService.getSummaryCountSizeApi(
        handler -> {
          if (handler.succeeded()) {
            response.setStatusCode(200).end(handler.result().toString());
          } else {
            response.setStatusCode(400).end(handler.cause().getMessage());
          }
        });
  }

  /**
   * Get mlayer monthly count and size.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void getCountSizeApi(RoutingContext routingContext) {
    LOGGER.debug("Info : fetching monthly count and size");
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
    mlayerService.getRealTimeDataSetApi(
        handler -> {
          if (handler.succeeded()) {
            response.setStatusCode(200).end(handler.result().toString());
          } else {
            response.setStatusCode(400).end(handler.cause().getMessage());
          }
        });
  }
}
