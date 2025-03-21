package iudx.catalogue.server.mlayer.controller;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.mlayer.util.Constants.*;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.validator.util.Constants.VALIDATION_FAILURE_MSG;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.apiserver.item.util.ItemCategory;
import iudx.catalogue.server.apiserver.util.RespBuilder;
import iudx.catalogue.server.authenticator.handler.authentication.AuthHandler;
import iudx.catalogue.server.authenticator.handler.authorization.AuthValidationHandler;
import iudx.catalogue.server.authenticator.model.JwtAuthenticationInfo;
import iudx.catalogue.server.authenticator.model.JwtAuthenticationInfo.Builder;
import iudx.catalogue.server.common.RoutingContextHelper;
import iudx.catalogue.server.exceptions.DatabaseFailureException;
import iudx.catalogue.server.exceptions.FailureHandler;
import iudx.catalogue.server.exceptions.DocAlreadyExistsException;
import iudx.catalogue.server.mlayer.model.MlayerDatasetRequest;
import iudx.catalogue.server.mlayer.model.MlayerDomainRequest;
import iudx.catalogue.server.mlayer.model.MlayerGeoQueryRequest;
import iudx.catalogue.server.mlayer.model.MlayerInstanceRequest;
import iudx.catalogue.server.mlayer.service.MlayerService;
import iudx.catalogue.server.validator.service.ValidatorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MlayerController {
  private static final Logger LOGGER = LogManager.getLogger(MlayerController.class);
  private final MlayerService mlayerService;
  private final ValidatorService validatorService;
  private final FailureHandler failureHandler;
  private final AuthHandler authHandler;
  private final AuthValidationHandler validateToken;
  private final String host;

  public MlayerController(String host, ValidatorService validationService,
                          MlayerService mlayerService,
                          FailureHandler failureHandler,
                          AuthHandler authHandler, AuthValidationHandler validateToken) {
    this.host = host;
    this.validatorService = validationService;
    this.mlayerService = mlayerService;
    this.failureHandler = failureHandler;
    this.authHandler = authHandler;
    this.validateToken = validateToken;

  }

  public Router init(Router router) {
    // Routes for Mlayer Instance APIs

    /* Create Mlayer Instance */
    router
        .post(ROUTE_MLAYER_INSTANCE)
        .consumes(MIME_APPLICATION_JSON)
        .produces(MIME_APPLICATION_JSON)
        .handler(
            routingContext -> populateAuthInfo(routingContext, REQUEST_POST))
        .handler(authHandler) // Authentication
        .handler(validateToken)
        .handler(
            routingContext -> {
              if (routingContext.request().headers().contains(HEADER_TOKEN) ||
                  routingContext.request().headers().contains(HEADER_BEARER_AUTHORIZATION)) {
                createMlayerInstanceHandler(routingContext);
              } else {
                LOGGER.error("Unauthorized Operation");
                routingContext.response().setStatusCode(401).end();
              }
            })
        .failureHandler(failureHandler);

    /* Get Mlayer Instance */
    router
        .get(ROUTE_MLAYER_INSTANCE)
        .produces(MIME_APPLICATION_JSON)
        .handler(this::getMlayerInstanceHandler)
        .failureHandler(failureHandler);

    /* Delete Mlayer Instance */
    router
        .delete(ROUTE_MLAYER_INSTANCE)
        .produces(MIME_APPLICATION_JSON)
        .handler(routingContext -> populateAuthInfo(routingContext, REQUEST_DELETE))
        .handler(authHandler) // Authentication
        .handler(validateToken)
        .handler(
            routingContext -> {
              if (routingContext.request().headers().contains(HEADER_TOKEN)) {
                deleteMlayerInstanceHandler(routingContext);
              } else {
                LOGGER.error("Unauthorized Operation");
                routingContext.response().setStatusCode(401).end();
              }
            })
        .failureHandler(failureHandler);

    /* Update Mlayer Instance */
    router
        .put(ROUTE_MLAYER_INSTANCE)
        .consumes(MIME_APPLICATION_JSON)
        .produces(MIME_APPLICATION_JSON)
        .handler(
            routingContext -> populateAuthInfo(routingContext, REQUEST_PUT))
        .handler(authHandler) // Authentication
        .handler(validateToken)
        .handler(
            routingContext -> {
              if (routingContext.request().headers().contains(HEADER_TOKEN)) {
                updateMlayerInstanceHandler(routingContext);
              } else {
                LOGGER.error("Unauthorized Operation");
                routingContext.response().setStatusCode(401).end();
              }
            })
        .failureHandler(failureHandler);

    // Routes for Mlayer Domain APIs

    /* Create Mlayer Domain */
    router
        .post(ROUTE_MLAYER_DOMAIN)
        .consumes(MIME_APPLICATION_JSON)
        .produces(MIME_APPLICATION_JSON)
        .handler(
            routingContext -> populateAuthInfo(routingContext, REQUEST_POST))
        .handler(authHandler) // Authentication
        .handler(validateToken)
        .handler(
            routingContext -> {
              if (routingContext.request().headers().contains(HEADER_TOKEN)) {
                createMlayerDomainHandler(routingContext);
              } else {
                LOGGER.error("Unauthorized Operation");
                routingContext.response().setStatusCode(401).end();
              }
            })
        .failureHandler(failureHandler);

    /* Get Mlayer Domain */
    router
        .get(ROUTE_MLAYER_DOMAIN)
        .produces(MIME_APPLICATION_JSON)
        .handler(this::getMlayerDomainHandler)
        .failureHandler(failureHandler);

    /* Update Mlayer Domain */
    router
        .put(ROUTE_MLAYER_DOMAIN)
        .consumes(MIME_APPLICATION_JSON)
        .produces(MIME_APPLICATION_JSON)
        .handler(
            routingContext -> populateAuthInfo(routingContext, REQUEST_PUT))
        .handler(authHandler) // Authentication
        .handler(validateToken)
        .handler(
            routingContext -> {
              if (routingContext.request().headers().contains(HEADER_TOKEN)) {
                updateMlayerDomainHandler(routingContext);
              } else {
                LOGGER.error("Unauthorized Operation");
                routingContext.response().setStatusCode(401).end();
              }
            })
        .failureHandler(failureHandler);

    /* Delete Mlayer Domain */
    router
        .delete(ROUTE_MLAYER_DOMAIN)
        .produces(MIME_APPLICATION_JSON)
        .handler(
            routingContext -> populateAuthInfo(routingContext, REQUEST_DELETE))
        .handler(authHandler) // Authentication
        .handler(validateToken)
        .handler(
            routingContext -> {
              if (routingContext.request().headers().contains(HEADER_TOKEN)) {
                deleteMlayerDomainHandler(routingContext);
              } else {
                LOGGER.error("Unauthorized Operation");
                routingContext.response().setStatusCode(401).end();
              }
            })
        .failureHandler(failureHandler);

    // Routes for Mlayer Provider API
    router
        .get(ROUTE_MLAYER_PROVIDER)
        .produces(MIME_APPLICATION_JSON)
        .handler(this::getMlayerProvidersHandler)
        .failureHandler(failureHandler);

    // Routes for Mlayer GeoQuery API
    router
        .post(ROUTE_MLAYER_GEOQUERY)
        .produces(MIME_APPLICATION_JSON)
        .handler(this::getMlayerGeoQueryHandler)
        .failureHandler(failureHandler);

    // Routes for Mlayer Dataset API
    /* route to get all datasets*/
    router
        .get(ROUTE_MLAYER_DATASET)
        .produces(MIME_APPLICATION_JSON)
        .handler(this::getMlayerAllDatasetsHandler)
        .failureHandler(failureHandler);
    /* route to get a dataset detail*/
    router
        .post(ROUTE_MLAYER_DATASET)
        .produces(MIME_APPLICATION_JSON)
        .handler(this::getMlayerDatasetHandler)
        .failureHandler(failureHandler);

    // Route for Mlayer PopularDatasets API
    router
        .get(ROUTE_MLAYER_POPULAR_DATASETS)
        .produces(MIME_APPLICATION_JSON)
        .handler(this::getMlayerPopularDatasetsHandler)
        .failureHandler(failureHandler);

    // Total Count Api and Monthly Count & Size(MLayer)
    router
        .get(SUMMARY_TOTAL_COUNT_SIZE_API)
        .produces(MIME_APPLICATION_JSON)
        .handler(this::getSummaryCountSizeApi)
        .failureHandler(failureHandler);
    router
        .get(COUNT_SIZE_API)
        .produces(MIME_APPLICATION_JSON)
        .handler(this::getCountSizeApi)
        .failureHandler(failureHandler);

    return router;
  }

  /**
   * Create Mlayer Instance Handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void createMlayerInstanceHandler(RoutingContext routingContext) {
    LOGGER.debug("Info: Create Instance");

    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    //Validates the schema
    MlayerInstanceRequest mlayerInstanceRequest = new MlayerInstanceRequest(requestBody);
    LOGGER.debug("Validation Successful");

    mlayerService.createMlayerInstance(mlayerInstanceRequest)
        .onSuccess(result -> response.setStatusCode(201).end(result.toString()))
        .recover(err -> {
          LOGGER.debug("Call back to controller...");
          LOGGER.debug(err);
          LOGGER.debug(err.fillInStackTrace());
          LOGGER.debug(err instanceof DocAlreadyExistsException);

          JsonObject errorJson = new JsonObject(err.getMessage());
          if (errorJson.getString(TYPE).equals("DocAlreadyExistsException")) {
            routingContext.fail(new DocAlreadyExistsException(
                errorJson.getString("itemId")));
          } else if (errorJson.getString(TYPE).equals("DatabaseFailureException")) {
            routingContext.fail(new DatabaseFailureException(ItemCategory.MLAYER_INSTANCE,
                errorJson.getString("itemId"), errorJson.getString("message")));
          } else {
            LOGGER.error("Error occurred: {}", err.getMessage());
            response.setStatusCode(400).end(err.getMessage());
          }
          return Future.failedFuture(err);
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
    mlayerService.getMlayerInstance(requestParams)
        .onComplete(handler -> {
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


    String instanceId = request.getParam(MLAYER_ID);
    mlayerService.deleteMlayerInstance(instanceId)
        .onComplete(dbHandler -> {
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

    String instanceId = request.getParam(MLAYER_ID);
    requestBody.put(INSTANCE_ID, instanceId);
    //Validates the schema
    MlayerInstanceRequest mlayerInstanceRequest = new MlayerInstanceRequest(requestBody);
    LOGGER.debug("Validation Successful");

    mlayerService.updateMlayerInstance(mlayerInstanceRequest)
        .onComplete(handler -> {
          if (handler.succeeded()) {
            response.setStatusCode(200).end(handler.result().toString());
          } else {
            response.setStatusCode(400).end(handler.cause().getMessage());
          }
        });
  }

  /* Populate authentication info */
  public void populateAuthInfo(RoutingContext routingContext, String method) {
    String token = RoutingContextHelper.getToken(routingContext);
    JwtAuthenticationInfo jwtAuthenticationInfo = new Builder()
        .setToken(token)
        .setMethod(method)
        .setApiEndpoint(routingContext.normalizedPath())
        .setId(host)
        .build();

    RoutingContextHelper.setJwtAuthInfo(routingContext, jwtAuthenticationInfo);
    routingContext.next();
  }

  /**
   * Create Mlayer Domain Handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void createMlayerDomainHandler(RoutingContext routingContext) {
    LOGGER.debug("Info: Domain Created");

    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    //Validate the schema
    MlayerDomainRequest domainRequest = new MlayerDomainRequest(requestBody);
    LOGGER.debug("Validation Successful");
    mlayerService.createMlayerDomain(domainRequest)
        .onSuccess(result -> response.setStatusCode(201).end(result.toString()))
        .recover(err -> {
          JsonObject errorJson = new JsonObject(err.getMessage());
          if (errorJson.getString(TYPE).equals("DocAlreadyExistsException")) {
            routingContext.fail(new DocAlreadyExistsException(
                errorJson.getString("itemId")));
          } else if (errorJson.getString(TYPE).equals("DatabaseFailureException")) {
            routingContext.fail(new DatabaseFailureException(ItemCategory.MLAYER_DOMAIN,
                errorJson.getString("itemId"), errorJson.getString("message")));
          } else {
            LOGGER.error("Error occurred: {}", err.getMessage());
            response.setStatusCode(400).end(err.getMessage());
          }
          return Future.failedFuture(err);
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
    mlayerService.getMlayerDomain(requestParams)
        .onComplete(handler -> {
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
    LOGGER.debug("Info: Updating Mlayer Domain");

    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
    //Validates schema
    String domainId = request.getParam(MLAYER_ID);
    requestBody.put(DOMAIN_ID, domainId);

    MlayerDomainRequest domainRequest = new MlayerDomainRequest(requestBody);
    LOGGER.debug("Validation Successful");
    mlayerService.updateMlayerDomain(domainRequest)
        .onComplete(handler -> {
          if (handler.succeeded()) {
            response.setStatusCode(200).end(handler.result().toString());
          } else {
            response.setStatusCode(400).end(handler.cause().getMessage());
          }
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

    String domainId = request.getParam(MLAYER_ID);
    mlayerService.deleteMlayerDomain(domainId)
        .onComplete(dbHandler -> {
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
    mlayerService.getMlayerProviders(requestParams)
        .onComplete(handler -> {
          if (handler.succeeded()) {
            response.setStatusCode(200).end(handler.result().toString());
          } else {
            if (handler.cause().getMessage().equals("No Content Available")) {
              response.setStatusCode(204).end();
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

    //Validate the schema
    MlayerGeoQueryRequest geoQueryRequest = new MlayerGeoQueryRequest(requestBody);
    LOGGER.debug("Validation Successful");
    mlayerService.getMlayerGeoQuery(geoQueryRequest)
        .onComplete(handler -> {
          if (handler.succeeded()) {
            response.setStatusCode(200).end(handler.result().toString());
          } else {
            response.setStatusCode(400).end(handler.cause().getMessage());
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
    Integer limit = requestParams.getInteger(LIMIT);
    Integer offset = requestParams.getInteger(OFFSET);

    mlayerService.getMlayerAllDatasets(limit, offset)
        .onComplete(handler -> {
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

    JsonObject requestParam = parseRequestParams(routingContext);
    requestData
        .put(LIMIT, requestParam.getInteger(LIMIT))
        .put(OFFSET, requestParam.getInteger(OFFSET));

    MlayerDatasetRequest datasetModel = new MlayerDatasetRequest(requestData);
    LOGGER.debug("Validation of dataset Id Successful");
    LOGGER.debug(datasetModel.toJson());

    mlayerService.getMlayerDataset(datasetModel)
        .onComplete(handler -> {
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
    mlayerService.getMlayerPopularDatasets(instance)
        .onComplete(handler -> {
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
    mlayerService.getSummaryCountSizeApi()
        .onComplete(handler -> {
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
    mlayerService.getRealTimeDataSetApi()
        .onComplete(handler -> {
          if (handler.succeeded()) {
            response.setStatusCode(200).end(handler.result().toString());
          } else {
            response.setStatusCode(400).end(handler.cause().getMessage());
          }
        });
  }
}
