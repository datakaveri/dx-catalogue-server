package iudx.catalogue.server.apiserver.util;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.Handler;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ExceptionHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(ExceptionHandler.class);

  @Override
  public void handle(RoutingContext routingContext) {

    Throwable failure = routingContext.failure();
    if (failure instanceof DecodeException) {
      handleDecodeException(routingContext);
    } else if (failure instanceof ClassCastException) {
      handleClassCastException(routingContext);
    } else {
      routingContext.response()
                    .setStatusCode(400)
                    .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                    .end(new RespBuilder()
                              .withType(TYPE_INVALID_SYNTAX)
                              .withTitle(TITLE_INVALID_SYNTAX)
                              .getResponse());
    }
  }


  /**
   * Handles the JsonDecode Exception.
   * 
   *
   * @param routingContext for handling HTTP Request
   */
  public void handleDecodeException(RoutingContext routingContext) {

    LOGGER.error("Error: Invalid Json payload; " + routingContext.failure().getLocalizedMessage());
    String response = "";

    response =
        new RespBuilder()
            .withType(TYPE_INVALID_SCHEMA)
            .withTitle(TITLE_INVALID_SCHEMA)
            .withDetail("Invalid Json payload")
            .getResponse();

    routingContext
        .response()
        .setStatusCode(400)
        .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
        .end(response);
  }

  /**
   * Handles the exception from casting a object to different object.
   * 
   *@param routingContext the routing context of the request
   */
  public void handleClassCastException(RoutingContext routingContext) {

    LOGGER.error("Error: Invalid request payload; "
            + routingContext.failure().getLocalizedMessage());
    
    routingContext.response()
                  .setStatusCode(400)
                  .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                  .end(
                      new JsonObject()
                      .put(TYPE, TYPE_FAIL)
                      .put(TITLE, "Invalid payload")
                      .encode());

    routingContext.next();
  }
}
