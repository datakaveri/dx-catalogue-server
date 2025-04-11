package iudx.catalogue.server.exceptions;

import static iudx.catalogue.server.apiserver.util.Constants.APPLICATION_JSON;
import static iudx.catalogue.server.apiserver.util.Constants.BAD_REQUEST;
import static iudx.catalogue.server.apiserver.util.Constants.CONTENT_TYPE;
import static iudx.catalogue.server.apiserver.util.Constants.HEADER_CONTENT_TYPE;
import static iudx.catalogue.server.apiserver.util.Constants.MIME_APPLICATION_JSON;
import static iudx.catalogue.server.common.util.ResponseBuilderUtil.internalErrorResp;
import static iudx.catalogue.server.common.util.ResponseBuilderUtil.invalidSyntaxResponse;
import static iudx.catalogue.server.common.util.ResponseBuilderUtil.itemNotFoundResponse;
import static iudx.catalogue.server.common.util.ResponseBuilderUtil.operationNotAllowedResponse;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.validator.util.Constants.INVALID_SCHEMA_MSG;

import io.vertx.core.Handler;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.BodyProcessorException;
import io.vertx.ext.web.validation.ParameterProcessorException;
import io.vertx.ext.web.validation.RequestPredicateException;
import io.vertx.json.schema.ValidationException;
import iudx.catalogue.server.apiserver.item.exception.ItemAlreadyExistsException;
import iudx.catalogue.server.apiserver.item.exception.ItemDatabaseFailureException;
import iudx.catalogue.server.apiserver.item.exception.ItemNotFoundException;
import iudx.catalogue.server.apiserver.stack.exceptions.StacDatabaseFailureException;
import iudx.catalogue.server.apiserver.stack.exceptions.StacAlreadyExistsException;
import iudx.catalogue.server.apiserver.stack.exceptions.StacNotFoundException;
import iudx.catalogue.server.common.HttpStatusCode;
import iudx.catalogue.server.common.RespBuilder;
import iudx.catalogue.server.common.ResponseUrn;
import java.util.Objects;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FailureHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(FailureHandler.class);

  @Override
  public void handle(RoutingContext routingContext) {

    Throwable failure = routingContext.failure();
    failure.printStackTrace();
    LOGGER.debug("Exception caught; " + failure.getLocalizedMessage());

    if (routingContext.response().ended()) {
      LOGGER.debug("Response already ended");
      return;
    }

    if (failure instanceof DecodeException) {
      handleDecodeException(routingContext);
    } else if (failure instanceof InternalServerException) {
      handleInternalServerException(routingContext, (InternalServerException) failure);
    } else if (failure instanceof OperationNotAllowedException){
      handleOperationNotAllowedException(routingContext, (OperationNotAllowedException) failure);
    } else if (failure instanceof StacAlreadyExistsException) {
      handleStacAlreadyExistsException(routingContext, (StacAlreadyExistsException) failure);
    } else if (failure instanceof ItemAlreadyExistsException) {
      handleDxCrudAlreadyExistsException(routingContext, (ItemAlreadyExistsException) failure);
    } else if (failure instanceof StacDatabaseFailureException) {
      handleDbExceptionForStac(routingContext, (StacDatabaseFailureException) failure);
    } else if (failure instanceof ItemDatabaseFailureException) {
      handleDxCrudDatabaseFailureException(routingContext,
          (ItemDatabaseFailureException) failure);
    } else if(failure instanceof ItemNotFoundException){
      handleDxItemNotFoundException(routingContext, (ItemNotFoundException) failure);
    } else if (failure instanceof StacNotFoundException) {
      handleStacItemNotFoundException(routingContext, (StacNotFoundException) failure);
    } else if (failure instanceof DocNotFoundException) {
      handleItemNotFoundException(routingContext, (DocNotFoundException) failure);
    } else if (failure instanceof InvalidSchemaException exception) {
      handleInvalidSchemaException(routingContext, exception);
    } else if (failure instanceof InvalidSyntaxException) {
      handleInvalidSyntaxException(routingContext, (InvalidSyntaxException) failure);
    } else if (failure instanceof DocAlreadyExistsException exception) {
      handleItemAlreadyExistsException(routingContext, exception);
    } else if (failure instanceof IllegalArgumentException
        || failure instanceof NullPointerException) {
      handleIllegalArgumentException(routingContext);
    } else if (failure instanceof ClassCastException) {
      handleClassCastException(routingContext);
    } else if (failure instanceof DxRuntimeException exception) {
      LOGGER.error(exception.getUrn().getUrn() + " : " + exception.getMessage());
      handleDxRuntimeException(routingContext, exception);
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

  private void handleInvalidSyntaxException(RoutingContext routingContext,
                                            InvalidSyntaxException exception) {
    routingContext
        .response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(exception.getStatusCode())
        .end(invalidSyntaxResponse(exception.getMessage()));
  }

  private void handleOperationNotAllowedException(RoutingContext routingContext,
                                                  OperationNotAllowedException exception) {
    String responseMsg;
    if (exception.getMessage().contains(INSTANCE_VERIFICATION_FAILED)) {
      responseMsg = new RespBuilder()
          .withType(TYPE_OPERATION_NOT_ALLOWED)
          .withTitle(TITLE_OPERATION_NOT_ALLOWED)
          .withResult(exception.getId(), INSERT, FAILED, exception.getMessage())
          .withDetail(exception.getMessage())
          .getResponse();
    } else {
      responseMsg = operationNotAllowedResponse(exception.getId(), exception.getMessage());
    }
    routingContext
        .response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(exception.getStatusCode())
        .end(responseMsg);
  }

  private void handleInternalServerException(RoutingContext routingContext,
                                             InternalServerException exception) {
    String responseMsg;
    if (exception.getMethod().equals(REQUEST_POST)) {
      responseMsg = new RespBuilder()
          .withType(FAILED)
          .withResult(exception.getId(), INSERT, FAILED)
          .withDetail("Insertion Failed")
          .getResponse();
    } else {
      responseMsg = internalErrorResp();
    }
    routingContext
        .response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(exception.getStatusCode())
        .end(responseMsg);
  }

  private void handleStacItemNotFoundException(RoutingContext routingContext,
                                               StacNotFoundException exception) {
    String responseMsg;
    if (exception.getMethod().equals(DELETE)) {
      responseMsg = new RespBuilder()
          .withType(TYPE_ITEM_NOT_FOUND)
          .withResult(exception.getId(), REQUEST_DELETE, FAILED)
          .withDetail("Item not found, can't delete")
          .getResponse();
    } else if (exception.getMethod().equals(REQUEST_GET)) {
      responseMsg = new RespBuilder()
              .withType(TYPE_ITEM_NOT_FOUND)
              .withTitle(TITLE_ITEM_NOT_FOUND)
              .withDetail("Fail: Stac doesn't exist")
              .getResponse();
    } else {
      responseMsg = itemNotFoundResponse(
          exception.getId(), UPDATE, "Fail: Doc doesn't exist, can't update");
    }
    routingContext
        .response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(exception.getStatusCode())
        .end(responseMsg);
  }

  private void handleDxItemNotFoundException(RoutingContext routingContext,
                                             ItemNotFoundException exception) {
    String responseMsg;
    if (Objects.equals(exception.getMethod(), UPDATE)) {
      responseMsg = itemNotFoundResponse(
          exception.getId(), UPDATE, "Fail: Doc doesn't exist, can't update");
    } else {
      responseMsg = itemNotFoundResponse(exception.getId(), "Fail: Doc doesn't exist, can't " +
          "delete");
    }
    routingContext
        .response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(exception.getStatusCode())
        .end(responseMsg);
  }

  private void handleItemNotFoundException(RoutingContext routingContext,
                                           DocNotFoundException exception) {
    routingContext
        .response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(exception.getStatusCode())
        .end(itemNotFoundResponse(
            exception.getId(), UPDATE, "Fail: Doc doesn't exist, can't update"));
  }

  private void handleDxCrudDatabaseFailureException(RoutingContext routingContext,
                                                    ItemDatabaseFailureException exception) {
    routingContext
        .response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(exception.getStatusCode())
        .end(operationNotAllowedResponse(exception.getId(), INSERT,
            exception.getMessage()));
  }

  private void handleDbExceptionForStac(RoutingContext routingContext, StacDatabaseFailureException exception) {
    String responseMsg;
    if (exception.getMethod().equals(REQUEST_POST)) {
      responseMsg = new RespBuilder()
          .withType(FAILED)
          .withResult("stac", INSERT, FAILED)
          .withDetail(DATABASE_ERROR)
          .getResponse();
    } else {
      responseMsg = new RespBuilder()
          .withType(FAILED)
          .withResult(exception.getId(), REQUEST_GET, FAILED)
          .withDetail(DATABASE_ERROR)
          .getResponse();
    }
    routingContext
        .response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(exception.getStatusCode())
        .end(responseMsg);
  }

  private void handleDxRuntimeException(RoutingContext routingContext, DxRuntimeException exception) {
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
  }

//  private void handleDatabaseFailureException(RoutingContext routingContext,
//                                              DatabaseFailureException exception) {
//    if(exception.getMode() == ItemCategory.DX_CRUD || exception.getMode() == null) {
//      routingContext
//          .response()
//          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
//          .setStatusCode(exception.getStatusCode())
//          .end(operationNotAllowedResponse(exception.getId(), INSERT,
//              exception.getMessage()));
//    } else if (exception.getMode() == ItemCategory.MLAYER_INSTANCE) {
//      if (exception.getMessage().contains(TYPE_INTERNAL_SERVER_ERROR)) {
//        routingContext
//            .response()
//            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
//            .setStatusCode(exception.getStatusCode())
//            .end(new RespBuilder()
//                .withType(FAILED)
//                .withResult(MLAYER_ID)
//                .withDetail("Fail: Insertion of Instance failed")
//                .getResponse());
//      } else {
//        routingContext
//            .response()
//            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
//            .setStatusCode(exception.getStatusCode())
//            .end(new RespBuilder()
//                .withType(TYPE_FAIL)
//                .withResult(FAILED)
//                .withDetail(DETAIL_INTERNAL_SERVER_ERROR)
//                .getResponse());
//      }
//    } else if (exception.getMode() == ItemCategory.MLAYER_DOMAIN) {
//      if (exception.getMessage().contains(TYPE_INTERNAL_SERVER_ERROR)) {
//        routingContext
//            .response()
//            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
//            .setStatusCode(exception.getStatusCode())
//            .end(new RespBuilder()
//                .withType(FAILED)
//                .withResult(exception.getId())
//                .withDetail("Fail: Insertion of mLayer domain failed: ")
//                .getResponse());
//      } else {
//        routingContext
//            .response()
//            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
//            .setStatusCode(exception.getStatusCode())
//            .end(new RespBuilder()
//                .withType(TYPE_FAIL)
//                .withResult(FAILED)
//                .withDetail(DETAIL_INTERNAL_SERVER_ERROR)
//                .getResponse());
//      }
//    }  else if (exception.getMode() == ItemCategory.RATING) {
//      if (exception.getMessage().contains(TYPE_INTERNAL_SERVER_ERROR)) {
//        routingContext
//            .response()
//            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
//            .setStatusCode(exception.getStatusCode())
//            .end(new RespBuilder()
//                .withType(FAILED)
//                .withTitle("Fail: Insertion of rating failed")
//                .withDetail("Fail: Insertion of rating failed")
//                .withResult(exception.getId())
//                .getResponse());
//      } else {
//        routingContext
//            .response()
//            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
//            .setStatusCode(exception.getStatusCode())
//            .end(new RespBuilder()
//                .withType(TYPE_FAIL)
//                .withResult("Insertion Failed")
//                .withDetail("Insertion Failed")
//                .withResult(exception.getId(), INSERT, FAILED)
//                .getResponse());
//      }
//    } else if (exception.getMode() == ItemCategory.STAC) {
//        routingContext
//            .response()
//            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
//            .setStatusCode(exception.getStatusCode())
//            .end(new RespBuilder()
//                .withType(FAILED)
//                .withResult("stac", INSERT, FAILED)
//                .withDetail(DATABASE_ERROR)
//                .getResponse());
//    }
//  }
//
//  private void handleItemAlreadyExistsExceptionWithMode(RoutingContext routingContext,
//                                                DocAlreadyExistsException exception) {
//    if (exception.getMode().equals(ItemCategory.STAC)) {
//      routingContext
//          .response()
//          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
//          .setStatusCode(exception.getStatusCode())
//          .end(new RespBuilder()
//              .withType(TYPE_CONFLICT)
//              .withTitle(DETAIL_CONFLICT)
//              .withDetail("STAC already exists,creation skipped")
//              .getResponse());
//      return;
//    }
//    String itemId = exception.getItemId();
//    JsonObject response;
//    RespBuilder respBuilder =
//        new RespBuilder()
//            .withType(exception.getUrn().getUrn())
//            .withTitle(exception.getUrn().getMessage());
//    if (exception.getMode().equals(ItemCategory.MLAYER_INSTANCE)) {
//      response = respBuilder
//          .withResult(itemId, " Fail: Instance Already Exists")
//          .withDetail(" Fail: Instance Already Exists")
//          .getJsonResponse();
//    } else if (exception.getMode().equals(ItemCategory.MLAYER_DOMAIN)) {
//      response = respBuilder
//          .withResult(itemId, " Fail: Domain Already Exists")
//          .withDetail(" Fail: Domain Already Exists")
//          .getJsonResponse();
//    } else {
//      response = respBuilder
//          .withDetail(" Fail: Doc Already Exists")
//          .withResult(itemId, INSERT, FAILED, " Fail: Doc Already Exists")
//          .getJsonResponse();
//    }
//    routingContext
//        .response()
//        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
//        .setStatusCode(exception.getStatusCode())
//        .end(response.encode());
//  }
  private void handleItemAlreadyExistsException(RoutingContext routingContext,
                                                DocAlreadyExistsException exception) {
    String itemId = exception.getItemId();
    RespBuilder respBuilder =
        new RespBuilder()
            .withType(exception.getUrn().getUrn())
            .withTitle(exception.getUrn().getMessage())
            .withDetail(" Fail: Doc Already Exists")
          .withResult(itemId, INSERT, FAILED, " Fail: Doc Already Exists");
    routingContext
        .response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(exception.getStatusCode())
        .end(respBuilder.getResponse());
  }

  private void handleStacAlreadyExistsException(RoutingContext routingContext,
                                                StacAlreadyExistsException exception) {
    routingContext
        .response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(exception.getStatusCode())
        .end(new RespBuilder()
            .withType(TYPE_CONFLICT)
            .withTitle(DETAIL_CONFLICT)
            .withDetail(exception.getDetail())
            .getResponse());
  }

  private void handleDxCrudAlreadyExistsException(RoutingContext routingContext,
                                                  ItemAlreadyExistsException exception) {
    String itemId = exception.getItemId();
    RespBuilder respBuilder = new RespBuilder()
        .withType(exception.getUrn().getUrn())
        .withTitle(exception.getUrn().getMessage())
        .withDetail("Fail: Doc Already Exists")
        .withResult(itemId, INSERT, FAILED, "Fail: Doc Already Exists");

    routingContext
        .response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(exception.getStatusCode())
        .end(respBuilder.getResponse());
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
  private void handleInvalidSchemaException(RoutingContext routingContext,
                                            InvalidSchemaException exception) {
    LOGGER.error("Error: Invalid Schema; " + exception.getErrorMessage());
    if (exception.isStacInstance()){
      routingContext
          .response()
          .setStatusCode(exception.getStatusCode())
          .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
          .end(new RespBuilder()
              .withType(TYPE_INVALID_SCHEMA)
              .withTitle(INVALID_SCHEMA_MSG)
              .withDetail(DETAIL_INVALID_SCHEMA)
              .getResponse());
      return;
    }
    routingContext
        .response()
        .setStatusCode(exception.getStatusCode())
        .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
        .end(
            new RespBuilder()
                .withType(exception.getUrn().getUrn())
                .withTitle(exception.getUrn().getMessage())
                .withDetail("The Schema of requested body is invalid.")
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
