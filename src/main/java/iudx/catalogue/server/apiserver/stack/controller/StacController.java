package iudx.catalogue.server.apiserver.stack.controller;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.auditing.util.Constants.API;
import static iudx.catalogue.server.auditing.util.Constants.IID;
import static iudx.catalogue.server.auditing.util.Constants.IUDX_ID;
import static iudx.catalogue.server.auditing.util.Constants.USER_ID;
import static iudx.catalogue.server.auditing.util.Constants.USER_ROLE;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.validator.util.Constants.INVALID_SCHEMA_MSG;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.apiserver.stack.service.StacService;
import iudx.catalogue.server.apiserver.stack.service.StacServiceImpl;
import iudx.catalogue.server.apiserver.util.RespBuilder;
import iudx.catalogue.server.auditing.handler.AuditHandler;
import iudx.catalogue.server.authenticator.handler.authentication.AuthHandler;
import iudx.catalogue.server.authenticator.handler.authorization.AuthValidationHandler;
import iudx.catalogue.server.authenticator.model.JwtAuthenticationInfo;
import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.common.RoutingContextHelper;
import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
import iudx.catalogue.server.exceptions.FailureHandler;
import iudx.catalogue.server.util.Api;
import iudx.catalogue.server.validator.service.ValidatorService;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StacController {
  private static final Logger LOGGER = LogManager.getLogger(StacController.class);
  private final ValidatorService validatorService;
  private final AuditHandler auditHandler;
  private final AuthHandler authHandler;
  private final AuthValidationHandler validateToken;
  private final FailureHandler failureHandler;
  private final Api api;
  private final StacService stacService;
  private RespBuilder respBuilder;

  public StacController(
      Api api,
      JsonObject config,
      ValidatorService validatorService,
      AuditHandler auditHandler,
      ElasticsearchService esService,
      AuthHandler authHandler, AuthValidationHandler validateToken,
      FailureHandler failureHandler) {
    this.api = api;
    this.validatorService = validatorService;
    this.auditHandler = auditHandler;
    this.authHandler = authHandler;
    this.validateToken = validateToken;
    this.failureHandler = failureHandler;
    stacService = new StacServiceImpl(esService, config.getString(DOC_INDEX));
  }

  public Router init(Router router) {
    router
        .post(api.getStackRestApis())
        .handler(this::validateAuth)
        .handler(routingContext -> validateSchema(routingContext, REQUEST_POST))
        .handler(authHandler)
        .handler(validateToken)
        .handler(this::handlePostStackRequest)
        .failureHandler(failureHandler);
    router
        .patch(api.getStackRestApis())
        .handler(this::validateAuth)
        .handler(routingContext -> validateSchema(routingContext, REQUEST_PATCH))
        .handler(authHandler)
        .handler(validateToken)
        .handler(this::handlePatchStackRequest)
        .failureHandler(failureHandler);

    router.get(api.getStackRestApis()).handler(this::handleGetStackRequest);
    router
        .delete(api.getStackRestApis())
        .handler(this::validateAuth)
        .handler(routingContext -> validateSchema(routingContext, REQUEST_DELETE))
        .handler(authHandler)
        .handler(validateToken)
        .handler(this::deleteStackHandler)
        .failureHandler(failureHandler);

    return router;
  }

  private void validateAuth(RoutingContext routingContext) {
    /* checking authentication info in requests */
    if (routingContext.request().headers().contains(HEADER_TOKEN) ||
        routingContext.request().headers().contains(HEADER_BEARER_AUTHORIZATION)) {
      routingContext.next();
    } else {
      LOGGER.warn("Fail: Unauthorized CRUD operation");
      routingContext.response().setStatusCode(400).end(respBuilder.getResponse());
    }
  }

  public void handleGetStackRequest(RoutingContext routingContext) {
    LOGGER.debug("method HandleGetStackRequest() started");
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    String stacId = routingContext.queryParams().get(ID);
    LOGGER.debug("stackId:: {}", stacId);
    if (validateId(stacId)) {
      stacService.get(stacId)
          .onComplete(stackHandler -> {
            if (stackHandler.succeeded()) {
              JsonObject resultJson = stackHandler.result();
              handleSuccessResponse(response, resultJson.toString());
            } else {
              LOGGER.error("Fail: Stack not found;" + stackHandler.cause().getMessage());
              processBackendResponse(response, stackHandler.cause().getMessage());
            }
          });
    } else {
      respBuilder = new RespBuilder()
          .withType(TYPE_INVALID_UUID)
          .withTitle(TITLE_INVALID_UUID)
          .withDetail("The id is invalid or not present");
      LOGGER.error("Error invalid id : {}", stacId);
      processBackendResponse(response, respBuilder.getResponse());
    }
  }

  public void validateSchema(RoutingContext routingContext, String method) {
    LOGGER.debug("method validateSchema() started");
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
    HttpServerRequest request = routingContext.request();
    JsonObject validationJson;
    if (!Objects.equals(method, REQUEST_DELETE)) {
      JsonObject requestBody = routingContext.body().asJsonObject();
      validationJson = requestBody.copy();
    } else {
      validationJson = new JsonObject();
    }

    switch (method) {
      case REQUEST_DELETE:
        break;
      case REQUEST_POST:
        validationJson.put("stack_type", "post:Stack");
        break;
      case REQUEST_PATCH:
        validationJson.put("stack_type", "patch:Stack");
        break;
      default:
        break;
    }

    if (!method.equals(REQUEST_DELETE)) {

      Future<JsonObject> validateSchemaFuture = validatorService.validateSchema(validationJson);
      String token = RoutingContextHelper.getToken(routingContext);
      JwtAuthenticationInfo jwtAuthenticationInfo = new JwtAuthenticationInfo.Builder()
          .setToken(token)
          .setMethod(method)
          .setApiEndpoint(routingContext.normalizedPath())
          .build();
      RoutingContextHelper.setJwtAuthInfo(routingContext, jwtAuthenticationInfo);

      validateSchemaFuture.onSuccess(validationSuccessHandler -> {
        RoutingContextHelper.setValidatedRequest(routingContext, validationJson);
        routingContext.next();
      }).onFailure(validateFailure -> {
        respBuilder = new RespBuilder()
            .withType(TYPE_INVALID_SCHEMA)
            .withTitle(INVALID_SCHEMA_MSG)
            .withDetail(DETAIL_INVALID_SCHEMA);
        processBackendResponse(response, respBuilder.getResponse());
      });
    } else {
      String stacId = routingContext.queryParams().get(ID);
      LOGGER.debug("stackId:: {}", stacId);
      if (validateId(stacId)) {
        String token = RoutingContextHelper.getToken(routingContext);
        JwtAuthenticationInfo jwtAuthenticationInfo = new JwtAuthenticationInfo.Builder()
            .setToken(token)
            .setMethod(REQUEST_PATCH)
            .setApiEndpoint(routingContext.normalizedPath())
            .build();
        RoutingContextHelper.setJwtAuthInfo(routingContext, jwtAuthenticationInfo);
        routingContext.next();
      } else {
        respBuilder = new RespBuilder()
            .withType(TYPE_INVALID_UUID)
            .withTitle(TITLE_INVALID_UUID)
            .withDetail("The id is invalid or not present");
        LOGGER.error("Invalid id : {}", stacId);
        processBackendResponse(response, respBuilder.getResponse());
      }
    }
  }

  public void handlePostStackRequest(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonObject validatedRequestBody = RoutingContextHelper.getValidatedRequest(routingContext);
    String path = routingContext.normalizedPath();
    Future<JsonObject> createStackFuture = stacService.create(validatedRequestBody);

    JwtData jwtDecodedInfo = RoutingContextHelper.getJwtData(routingContext);
    JsonObject authInfo = new JsonObject();
    // adding user id, user role and iid to response for auditing purpose
    authInfo
        .put(USER_ROLE, jwtDecodedInfo.getRole())
        .put(USER_ID, jwtDecodedInfo.getSub())
        .put(IID, jwtDecodedInfo.getIid());
    createStackFuture
        .onSuccess(stacServiceResult -> {
          LOGGER.debug("stacServiceResult : " + stacServiceResult);
          JsonArray results = stacServiceResult.getJsonArray("results");
          String stackId = results.getJsonObject(0).getString(ID);
          authInfo.put(IUDX_ID, stackId);
          authInfo.put(API, path);
          authInfo.put(HTTP_METHOD, REQUEST_POST);
          Future.future(fu -> auditHandler.updateAuditTable(authInfo));
          response.setStatusCode(201).end(stacServiceResult.toString());
        }).onFailure(
            stacServiceFailure -> {
              LOGGER.error("Fail: DB request has failed;" + stacServiceFailure.getMessage());
              processBackendResponse(response, stacServiceFailure.getMessage());
            });
  }

  public void handlePatchStackRequest(RoutingContext routingContext) {
    LOGGER.debug("method handlePatchStackRequest() started");
    HttpServerResponse response = routingContext.response();
    String path = routingContext.normalizedPath();

    JsonObject validatedRequestBody = RoutingContextHelper.getValidatedRequest(routingContext);
    String stacId = validatedRequestBody.getString(ID);

    Future<JsonObject> updateStackFuture = stacService.update(validatedRequestBody);

    JwtData jwtDecodedInfo = RoutingContextHelper.getJwtData(routingContext);
    JsonObject authInfo = new JsonObject();
    // adding user id, user role and iid to response for auditing purpose
    authInfo
        .put(USER_ROLE, jwtDecodedInfo.getRole())
        .put(USER_ID, jwtDecodedInfo.getSub())
        .put(IID, jwtDecodedInfo.getIid())
        .put(IUDX_ID, stacId)
        .put(API, path)
        .put(HTTP_METHOD, REQUEST_PATCH);
    updateStackFuture.onSuccess(
        stacServiceResult -> {
          LOGGER.debug("stacServiceResult : " + stacServiceResult);
          Future.future(fu -> auditHandler.updateAuditTable(authInfo));
          response.setStatusCode(201).end(stacServiceResult.toString());
        }).onFailure(
            stacServiceFailure -> {
              LOGGER.error("Fail: DB request has failed;" + stacServiceFailure.getMessage());
              processBackendResponse(response, stacServiceFailure.getMessage());
            });
  }

  public void deleteStackHandler(RoutingContext routingContext) {
    LOGGER.debug("method deleteStackHandler() started");
    HttpServerResponse response = routingContext.response();

    String stacId = routingContext.queryParams().get(ID);
    Future<JsonObject> deleteStackFuture = stacService.delete(stacId);

    JwtData jwtDecodedInfo = RoutingContextHelper.getJwtData(routingContext);
    JsonObject authInfo = new JsonObject();
    // adding user id, user role and iid to response for auditing purpose
    authInfo
        .put(USER_ROLE, jwtDecodedInfo.getRole())
        .put(USER_ID, jwtDecodedInfo.getSub())
        .put(IID, jwtDecodedInfo.getIid())
        .put(IUDX_ID, stacId)
        .put(API, routingContext.normalizedPath())
        .put(HTTP_METHOD, REQUEST_DELETE);;
    deleteStackFuture
        .onSuccess(stacServiceResult -> {
          LOGGER.debug("stacServiceResult : " + stacServiceResult);
          Future.future(fu -> auditHandler.updateAuditTable(authInfo));
          handleSuccessResponse(response, stacServiceResult.toString());
        }).onFailure(
            stacServiceFailure -> {
              LOGGER.error("Fail: DB request has failed;" + stacServiceFailure.getMessage());
              processBackendResponse(response, stacServiceFailure.getMessage());
            });
  }

  private boolean validateId(String itemId) {
    if (itemId.isEmpty() || itemId.isBlank()) {
      return false;
    }
    return UUID_PATTERN.matcher(itemId).matches();
  }

  private void processBackendResponse(HttpServerResponse response, String failureMessage) {
    int statusCode;
    try {
      JsonObject json = new JsonObject(failureMessage);
      switch (json.getString("type")) {
        case TYPE_ITEM_NOT_FOUND:
          statusCode = 404;
          break;
        case TYPE_CONFLICT:
          statusCode = 409;
          break;
        case TYPE_TOKEN_INVALID:
          statusCode = 401;
          break;
        case TYPE_INVALID_UUID:
        case TYPE_INVALID_SCHEMA:
          statusCode = 400;
          break;
        default:
          statusCode = 500;
          break;
      }
      response.setStatusCode(statusCode).end(failureMessage);
    } catch (DecodeException ex) {
      LOGGER.error("ERROR : Expecting Json from backend service [ jsonFormattingException ]");
      response.setStatusCode(400).end(BAD_REQUEST);
    }
  }

  private void handleSuccessResponse(HttpServerResponse response, String result) {
    response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(200).end(result);
  }

}
