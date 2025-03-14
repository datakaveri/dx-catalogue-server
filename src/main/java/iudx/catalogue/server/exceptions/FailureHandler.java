package iudx.catalogue.server.exceptions;

import static iudx.catalogue.server.apiserver.util.Constants.APPLICATION_JSON;
import static iudx.catalogue.server.apiserver.util.Constants.BAD_REQUEST;
import static iudx.catalogue.server.apiserver.util.Constants.CONTENT_TYPE;
import static iudx.catalogue.server.apiserver.util.Constants.HEADER_CONTENT_TYPE;
import static iudx.catalogue.server.apiserver.util.Constants.MIME_APPLICATION_JSON;
import static iudx.catalogue.server.util.Constants.TITLE;
import static iudx.catalogue.server.util.Constants.TITLE_INVALID_SCHEMA;
import static iudx.catalogue.server.util.Constants.TITLE_INVALID_SYNTAX;
import static iudx.catalogue.server.util.Constants.TYPE;
import static iudx.catalogue.server.util.Constants.TYPE_FAIL;
import static iudx.catalogue.server.util.Constants.TYPE_INVALID_SCHEMA;
import static iudx.catalogue.server.util.Constants.TYPE_INVALID_SYNTAX;

import io.vertx.core.Handler;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.BodyProcessorException;
import io.vertx.ext.web.validation.ParameterProcessorException;
import io.vertx.ext.web.validation.RequestPredicateException;
import io.vertx.json.schema.ValidationException;
import iudx.catalogue.server.common.HttpStatusCode;
import iudx.catalogue.server.common.RespBuilder;
import iudx.catalogue.server.common.ResponseUrn;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FailureHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(FailureHandler.class);

  @Override
  public void handle(RoutingContext routingContext) {

    Throwable failure = routingContext.failure();
    failure.printStackTrace();
    LOGGER.debug("Exception caught");

    if (routingContext.response().ended()) {
      LOGGER.debug("Already ended");
      return;
    }

    if (failure instanceof DecodeException) {
      handleDecodeException(routingContext);
    } else if (failure instanceof IllegalArgumentException
        || failure instanceof NullPointerException) {
      handleIllegalArgumentException(routingContext);
    } else if (failure instanceof ClassCastException) {
      handleClassCastException(routingContext);
    } else if (failure instanceof DxRuntimeException) {
      DxRuntimeException exception = (DxRuntimeException) failure;
      LOGGER.error(exception.getUrn().getUrn() + " : " + exception.getMessage());
      //HttpStatusCode code = HttpStatusCode.getByValue(exception.getStatusCode());

      JsonObject response =
          new RespBuilder()
              .withType(exception.getUrn().getUrn())
              .withTitle(exception.getUrn().getMessage())
              .withDetail(exception.getMessage())
              .getJsonResponse();

      routingContext
          .response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(exception.getStatusCode())
          .end(response.encode());
    } else if (failure instanceof ValidationException
        || failure instanceof BodyProcessorException
        || failure instanceof RequestPredicateException
        || failure instanceof ParameterProcessorException) {
      String type = ResponseUrn.BAD_REQUEST_URN.getUrn();
      JsonObject response =
          new RespBuilder()
              .withDetail("Missing or malformed request")
              .withType(type)
              .withTitle(HttpStatusCode.BAD_REQUEST.getDescription())
              .getJsonResponse();
      routingContext
          .response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(HttpStatus.SC_BAD_REQUEST)
          .end(response.toString());
    } else if (failure instanceof RuntimeException) {
      routingContext
          .response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(HttpStatus.SC_BAD_REQUEST)
          .end(validationFailureResponse(BAD_REQUEST).toString());
    } else {
      routingContext
          .response()
          .setStatusCode(400)
          .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
          .end(
              new RespBuilder()
                  .withType(TYPE_INVALID_SYNTAX)
                  .withTitle(TITLE_INVALID_SYNTAX)
                  .getResponse());
    }
  }

  private void handleIllegalArgumentException(RoutingContext routingContext) {
    LOGGER.error("Error: Invalid Schema; " + routingContext.failure().getLocalizedMessage());
    routingContext
        .response()
        .setStatusCode(400)
        .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
        .end(
            new RespBuilder()
                .withType(TYPE_INVALID_SCHEMA)
                .withTitle(TITLE_INVALID_SCHEMA)
                .withDetail(TITLE_INVALID_SCHEMA)
                .withResult(new JsonArray().add(routingContext.failure().getLocalizedMessage()))
                .getResponse());
  }

  private JsonObject validationFailureResponse(String message) {
    return new JsonObject()
        .put("type", HttpStatus.SC_BAD_REQUEST)
        .put("title", BAD_REQUEST)
        .put("detail", message);
  }

  /**
   * Handles the JsonDecode Exception.
   *
   * @param routingContext for handling HTTP Request
   */
  public void handleDecodeException(RoutingContext routingContext) {

    LOGGER.error("Error: Invalid Json payload; " + routingContext.failure().getLocalizedMessage());

    routingContext
        .response()
        .setStatusCode(400)
        .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
        .end(
            new RespBuilder()
                .withType(TYPE_INVALID_SCHEMA)
                .withTitle(TITLE_INVALID_SCHEMA)
                .withDetail("Invalid Json payload")
                .getResponse());
  }

  /**
   * Handles the exception from casting an object to different object.
   *
   * @param routingContext the routing context of the request
   */
  public void handleClassCastException(RoutingContext routingContext) {

    LOGGER.error(
        "Error: Invalid request payload; " + routingContext.failure().getLocalizedMessage());

    routingContext
        .response()
        .setStatusCode(400)
        .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
        .end(new JsonObject().put(TYPE, TYPE_FAIL).put(TITLE, "Invalid payload").encode());

    routingContext.next();
  }
}
