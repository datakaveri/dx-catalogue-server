package iudx.catalogue.server.rating.controller;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.auditing.util.Constants.API;
import static iudx.catalogue.server.auditing.util.Constants.EPOCH_TIME;
import static iudx.catalogue.server.auditing.util.Constants.ID;
import static iudx.catalogue.server.auditing.util.Constants.IUDX_ID;
import static iudx.catalogue.server.auditing.util.Constants.USER_ID;
import static iudx.catalogue.server.authenticator.Constants.RATINGS_ENDPOINT;
import static iudx.catalogue.server.authenticator.model.DxRole.CONSUMER;
import static iudx.catalogue.server.geocoding.util.Constants.RESULTS;
import static iudx.catalogue.server.rating.util.Constants.APPROVED;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.apiserver.util.RespBuilder;
import iudx.catalogue.server.auditing.handler.AuditHandler;
import iudx.catalogue.server.auditing.service.AuditingService;
import iudx.catalogue.server.authenticator.handler.AuthenticationHandler;
import iudx.catalogue.server.authenticator.handler.AuthorizationHandler;
import iudx.catalogue.server.authenticator.model.JwtAuthenticationInfo;
import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.authenticator.service.AuthenticationService;
import iudx.catalogue.server.common.RoutingContextHelper;
import iudx.catalogue.server.exceptions.FailureHandler;
import iudx.catalogue.server.rating.service.RatingService;
import iudx.catalogue.server.rating.util.Constants;
import iudx.catalogue.server.validator.service.ValidatorService;
import java.time.ZonedDateTime;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RatingController {
  private static final Logger LOGGER = LogManager.getLogger(RatingController.class);

  private final Router router;
  private ValidatorService validatorService;
  private RatingService ratingService;
  private final String host;
  private final AuthenticationHandler authenticationHandler;
  private final AuthorizationHandler authorizationHandler;
  private final AuditHandler auditHandler;
  private final FailureHandler failureHandler;

  public RatingController(Router router, ValidatorService validatorService,
                          RatingService ratingService, String host,
                          AuthenticationHandler authenticationHandler,
                          AuthorizationHandler authorizationHandler,
                          AuditHandler auditHandler,
                          FailureHandler failureHandler) {
    this.router = router;
    this.validatorService = validatorService;
    this.ratingService = ratingService;
    this.host = host;
    this.authenticationHandler = authenticationHandler;
    this.authorizationHandler = authorizationHandler;
    this.auditHandler = auditHandler;
    this.failureHandler = failureHandler;

    setupRoutes();
  }

  public void setupRoutes() {
    //  Routes for Rating APIs

    /* Create Rating */
    router
        .post(ROUTE_RATING)
        .consumes(MIME_APPLICATION_JSON)
        .produces(MIME_APPLICATION_JSON)
        .handler(this::validateAuth)
        .handler(this::validateID)
        .handler(routingContext -> setAuthInfo(routingContext, REQUEST_POST))
        .handler(authenticationHandler)
        .handler(authorizationHandler.forRoleBasedAccess(CONSUMER))
        .handler(this::validateSchema)
        .handler(this::createRatingHandler)
        .handler(context -> auditHandler.handle(context, ROUTE_RATING))
        .failureHandler(failureHandler);

    /* Get Ratings */
    router
        .get(ROUTE_RATING)
        .produces(MIME_APPLICATION_JSON)
        .handler(this::validateID)
        .handler(this::validateTypeParam)
        .handler(routingContext -> {
          // Set flag to skip auth if "type" parameter is present
          if (routingContext.request().params().contains("type")) {
            routingContext.put("skipAuth", true); // Store flag in context
            routingContext.next();
          } else {
            setAuthInfo(routingContext, REQUEST_GET);
          }
        })
        .handler(routingContext -> {
          Boolean skipAuth = routingContext.get("skipAuth");
          if (skipAuth != null && skipAuth) {
            routingContext.next();  // Skip authHandler and move to the next handler
          } else {
            authenticationHandler.handle(routingContext);  // Call authHandler only if skipAuth is false
            routingContext.next();
          }
        })
        .handler(routingContext -> {
          Boolean skipAuth = routingContext.get("skipAuth");
          if (skipAuth == null || !skipAuth) {
            authorizationHandler.forRoleBasedAccess(
                CONSUMER);  // Only validate access if auth is required
          } else {
            routingContext.next();  // Move to the next handler regardless
          }
        })
        .handler(this::getRatingHandler)
        .handler(routingContext -> {
          Boolean skipAuth = routingContext.get("skipAuth");
          if (skipAuth == null || !skipAuth) {
            auditHandler.handle(routingContext, ROUTE_RATING);
          }
        })
        .failureHandler(failureHandler);

    /* Update Rating */
    router
        .put(ROUTE_RATING)
        .consumes(MIME_APPLICATION_JSON)
        .produces(MIME_APPLICATION_JSON)
        .handler(this::validateAuth)
        .handler(this::validateID)
        .handler(routingContext -> setAuthInfo(routingContext, REQUEST_PUT))
        .handler(authenticationHandler)
        .handler(authorizationHandler.forRoleBasedAccess(CONSUMER))
        .handler(this::validateSchema)
        .handler(this::updateRatingHandler)
        .handler(context -> auditHandler.handle(context, ROUTE_RATING))
        .failureHandler(failureHandler);

    /* Delete Rating */
    router
        .delete(ROUTE_RATING)
        .produces(MIME_APPLICATION_JSON)
        .handler(this::validateAuth)
        .handler(this::validateID)
        .handler(routingContext -> setAuthInfo(routingContext, REQUEST_DELETE))
        .handler(authenticationHandler)
        .handler(authorizationHandler.forRoleBasedAccess(CONSUMER))
        .handler(this::deleteRatingHandler)
        .handler(context -> auditHandler.handle(context, ROUTE_RATING))
        .failureHandler(failureHandler);
  }

  // Method to return the router for mounting
  public Router getRouter() {
    return this.router;
  }

  /**
   * Validates the authorization of the incoming request. Checks if the request contains a
   * token field.
   *
   * @param routingContext {@link RoutingContext}
   */
  void validateAuth(RoutingContext routingContext) {
    /* checking authentication info in requests */
    if (routingContext.request().headers().contains(HEADER_TOKEN)) {
      routingContext.next();
    } else {
      LOGGER.warn("Fail: Unauthorized CRUD operation");
      routingContext.response().setStatusCode(401).end();
    }
  }

  public void validateID(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    String id = request.getParam(ID);
    if (!isValidId(id)) {
      response
          .setStatusCode(400)
          .end(
              new RespBuilder()
                  .withType(TYPE_INVALID_QUERY_PARAM_VALUE)
                  .withTitle(TITLE_INVALID_QUERY_PARAM_VALUE)
                  .withDetail("Invalid value for id")
                  .getResponse());
      return;
    }
    routingContext.next();
  }

  void setAuthInfo(RoutingContext routingContext, String method) {
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JwtAuthenticationInfo jwtAuthenticationInfo = new JwtAuthenticationInfo.Builder()
        .setToken(request.getHeader(HEADER_TOKEN))
        .setMethod(method)
        .setApiEndpoint(RATINGS_ENDPOINT)
        .setId(host)
        .build();
    RoutingContextHelper.setJwtAuthInfo(routingContext, jwtAuthenticationInfo);
    routingContext.next();
  }

  void validateSchema(RoutingContext routingContext) {
    LOGGER.debug("Info: Validating Schema");

    HttpServerResponse response = routingContext.response();
    JsonObject requestBody = routingContext.body().asJsonObject();
    String id = routingContext.request().getParam(ID);
    JwtData jwtData = RoutingContextHelper.getJwtDecodedInfo(routingContext);
    String userID = jwtData.getSub();

    requestBody.put(ID, id).put(USER_ID, userID).put("status", APPROVED);
    RoutingContextHelper.setValidatedRequest(routingContext, requestBody);

    Future<JsonObject> validationFuture = validatorService.validateRating(requestBody);

    validationFuture
        .onFailure(validationFailure -> response
            .setStatusCode(400)
            .end(
                new RespBuilder()
                    .withType(TYPE_INVALID_SCHEMA)
                    .withTitle(TITLE_INVALID_SCHEMA)
                    .withDetail("The Schema of requested body is invalid.")
                    .getResponse()))
        .onSuccess(validationResult -> {
          routingContext.next();
        });
  }

  /**
   * Create Rating handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void createRatingHandler(RoutingContext routingContext) {
    LOGGER.debug("Info: Creating Rating");

    JsonObject requestBody = RoutingContextHelper.getValidatedRequest(routingContext);
    ratingService.createRating(requestBody).onComplete(handler -> {
      if (handler.succeeded()) {
        routingContext.response().setStatusCode(201).end(handler.result().toString());
        routingContext.next();
      } else {
        routingContext.response().setStatusCode(400).end(handler.cause().getMessage());
      }
    });
  }

  public void validateTypeParam(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    String id = request.getParam(ID);
    JsonObject requestBody = new JsonObject().put(ID, id);

    // If the 'type' parameter is present, skip authorization
    if (request.params().contains("type")) {
      String requestType = request.getParam("type");
      if (requestType.equalsIgnoreCase("average") || requestType.equalsIgnoreCase("group")) {
        requestBody.put("type", requestType);
        RoutingContextHelper.setValidatedRequest(routingContext, requestBody);
        routingContext.next(); // Skip the authHandler
      } else {
        response
            .setStatusCode(400)
            .end(
                new RespBuilder()
                    .withType(TYPE_INVALID_QUERY_PARAM_VALUE)
                    .withTitle(TITLE_INVALID_QUERY_PARAM_VALUE)
                    .withDetail("Query parameter type cannot have value : " + requestType)
                    .getResponse());
      }
    } else {
      routingContext.next();
    }
  }

  /**
   * GET Rating handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  void getRatingHandler(RoutingContext routingContext) {
    LOGGER.debug("Info: fetching ratings");

    HttpServerRequest request = routingContext.request();
    JsonObject requestBody;
    if (!request.params().contains("type")) {
      String userID = RoutingContextHelper.getJwtDecodedInfo(routingContext).getSub();
      requestBody = new JsonObject()
          .put(ID, request.getParam(Constants.ID))
          .put(USER_ID, userID);
    } else {
      requestBody = RoutingContextHelper.getValidatedRequest(routingContext);
    }

    ratingService.getRating(requestBody).onComplete(handler -> {
      if (handler.succeeded()) {
        if (handler.result().getJsonArray(RESULTS) != null) {
          routingContext.response().setStatusCode(200).end(handler.result().toString());
          routingContext.next();
        } else {
          routingContext.response().setStatusCode(204).end();
        }
      } else {
        if (handler.cause().getLocalizedMessage().contains("Doc doesn't exist")) {
          routingContext.response().setStatusCode(404);
        } else {
          routingContext.response().setStatusCode(400);
        }
        routingContext.response().end(handler.cause().getMessage());
      }
    });
  }
  boolean isValidId(String id) {
    return UUID_PATTERN.matcher(id).matches();
  }

  /**
   * Update Rating handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void updateRatingHandler(RoutingContext routingContext) {
    LOGGER.debug("Info: Updating Rating");
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
    JsonObject requestBody = RoutingContextHelper.getValidatedRequest(routingContext);

    ratingService.updateRating(requestBody).onComplete(handler -> {
      if (handler.succeeded()) {
        response.setStatusCode(200).end(handler.result().toString());
        routingContext.next();
      } else {
        if (handler
            .cause()
            .getLocalizedMessage()
            .contains("Doc doesn't exist")) {
          response.setStatusCode(404);
        } else {
          response.setStatusCode(400);
        }
        response.end(handler.cause().getMessage());
      }
    });
  }

  /**
   * Delete Rating handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void deleteRatingHandler(RoutingContext routingContext) {
    LOGGER.debug("Info: Deleting Rating");
    String id = routingContext.request().getParam(ID);
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject requestBody =
        new JsonObject()
            .put(USER_ID, RoutingContextHelper.getJwtDecodedInfo(routingContext).getSub())
            .put(ID, id);
    ratingService.deleteRating(requestBody).onComplete(dbHandler -> {
      if (dbHandler.succeeded()) {
        LOGGER.info("Success: Item deleted;");
        LOGGER.debug(dbHandler.result().toString());
        if (dbHandler.result().getString(STATUS).equals(TITLE_SUCCESS)) {
          response.setStatusCode(200).end(dbHandler.result().toString());
          routingContext.next();
        } else {
          response.setStatusCode(404).end(dbHandler.result().toString());
        }
      } else if (dbHandler.failed()) {
        response.setStatusCode(400).end(dbHandler.cause().getMessage());
      }
    });
  }

}
