package iudx.catalogue.server.common.util;

import static iudx.catalogue.server.database.elastic.util.Constants.ERROR_INVALID_PARAMETER;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.apiserver.item.util.ItemCategory;
import iudx.catalogue.server.common.RespBuilder;

public class ResponseBuilderUtil {

  private static RespBuilder createResponseBuilder(String type, String title, String detail) {
    return new RespBuilder().withType(type).withTitle(title).withDetail(detail);
  }

  private static RespBuilder createResponseBuilder(String type, String title) {
    return new RespBuilder().withType(type).withTitle(title);
  }

  public static String invalidSchemaResponse(String detail) {
    return createResponseBuilder(TYPE_INVALID_SCHEMA, TITLE_INVALID_SCHEMA, detail).getResponse();
  }

  public static String linkValidationFailureResponse(String detail) {
    return createResponseBuilder(TYPE_LINK_VALIDATION_FAILED, TITLE_LINK_VALIDATION_FAILED, detail)
        .getResponse();
  }

  public static String invalidUuidResponse(String detail) {
    return createResponseBuilder(TYPE_INVALID_UUID, TITLE_INVALID_UUID, detail).getResponse();
  }

  public static String itemNotFoundResponse(String detail) {
    return createResponseBuilder(TYPE_ITEM_NOT_FOUND, TITLE_ITEM_NOT_FOUND, detail).getResponse();
  }

  public static String itemNotFoundResponse(String id, String detail) {
    return createResponseBuilder(TYPE_ITEM_NOT_FOUND, TITLE_ITEM_NOT_FOUND)
        .withResult(id, detail)
        .getResponse();
  }

  public static String itemNotFoundResponse(String id, String method, String detail) {
    return createResponseBuilder(TYPE_ITEM_NOT_FOUND, TITLE_ITEM_NOT_FOUND, detail)
        .withResult(id, method, FAILED, detail)
        .getResponse();
  }

  public static String itemNotFoundResponse(String id, String method, ItemCategory mode) {
    String detail = "Fail: Doc doesn't exist, can't update";
    if (mode == ItemCategory.MLAYER_INSTANCE) {
      return new RespBuilder()
          .withType(TYPE_ITEM_NOT_FOUND)
          .withTitle(TITLE_ITEM_NOT_FOUND)
          .withResult(id, "Fail : Instance doesn't exist, can't update")
          .getResponse();
    } else if (mode == ItemCategory.MLAYER_DOMAIN)  {
      return new RespBuilder()
          .withType(TYPE_ITEM_NOT_FOUND)
          .withTitle(TITLE_ITEM_NOT_FOUND)
          .withResult(id, "Fail: Domain doesn't exist, can't update")
          .withDetail("Fail: Domain doesn't exist, can't update")
          .getResponse();
    } else {
      return createResponseBuilder(TYPE_ITEM_NOT_FOUND, TITLE_ITEM_NOT_FOUND, detail)
          .withResult(id, method, FAILED, detail)
          .getResponse();
    }
  }

  public static JsonObject itemNotFoundJsonResp(String detail) {
    return createResponseBuilder(TYPE_ITEM_NOT_FOUND, TITLE_ITEM_NOT_FOUND, detail)
        .getJsonResponse();
  }

  public static String invalidSyntaxResponse(String detail) {
    return createResponseBuilder(TYPE_INVALID_SYNTAX, TITLE_INVALID_SYNTAX, detail).getResponse();
  }

  public static String internalErrorResp() {
    return createResponseBuilder(
            TYPE_INTERNAL_SERVER_ERROR, TITLE_INTERNAL_SERVER_ERROR, DETAIL_INTERNAL_SERVER_ERROR)
        .getResponse();
  }
  public static String StacDelFailure(String stacId){
    return new RespBuilder()
        .withType(FAILED)
        .withResult(stacId, REQUEST_DELETE, FAILED)
        .withDetail(DATABASE_ERROR)
        .getResponse();
  }
  public static String wrongInstanceNameResp(){
    return createResponseBuilder(
        TYPE_FAIL, TITLE_WRONG_INSTANCE_NAME, WRONG_INSTANCE_NAME).getResponse();
  }

  public static String itemAlreadyExistsResponse(String id, String detail) {
    return createResponseBuilder(TYPE_ALREADY_EXISTS, TITLE_ALREADY_EXISTS, detail)
        .withResult(id, INSERT, FAILED, detail)
        .getResponse();
  }

  public static String insertionFailedResponse(String id) {
    return createResponseBuilder(FAILED, "Insertion Failed", "Insertion Failed")
        .withResult(id, INSERT, FAILED)
        .getResponse();
  }

  public static JsonObject successResponse(JsonArray result) {
    return createResponseBuilder(TYPE_SUCCESS, TITLE_SUCCESS).withResult(result).getJsonResponse();
  }

  public static JsonObject successResponse(String id) {
    return createResponseBuilder(TYPE_SUCCESS, TITLE_SUCCESS)
        .withResult(id, INSERT, TYPE_SUCCESS)
        .getJsonResponse();
  }

  public static JsonObject ratingSuccessResponse(String id) {
    return createResponseBuilder(TYPE_SUCCESS, TITLE_SUCCESS).withResult(id).getJsonResponse();
  }

  public static String failureResponse(String id) {
    return createResponseBuilder(TYPE_FAIL, "Insertion Failed", "Insertion Failed")
        .withResult(id, INSERT, FAILED)
        .getResponse();
  }

  public static String failureResp(String id) {
    return createResponseBuilder(
            FAILED, "Fail: Insertion of rating failed", "Fail: Insertion of rating failed")
        .withResult(id)
        .getResponse();
  }

  public static JsonObject successfulItemOperationResp(JsonObject doc, String detail) {
    return createResponseBuilder(TYPE_SUCCESS, TITLE_SUCCESS, detail)
        .withResult(doc)
        .getJsonResponse();
  }
  public static JsonObject DxPostOperationRespSuccess(JsonObject doc) {
    return createResponseBuilder(TYPE_SUCCESS, TITLE_SUCCESS, "Success: Item created")
        .withResult(doc)
        .getJsonResponse();
  }
  public static JsonObject DxPutRespSuccess(JsonObject document) {
    return new RespBuilder()
        .withType(TYPE_SUCCESS)
        .withTitle(TITLE_SUCCESS)
        .withDetail("Success: Item updated successfully")
        .withResult(document)
        .getJsonResponse();
  }
  public static JsonObject DxItemDelRespSuccess(String id, String detail) {
    return createResponseBuilder(TYPE_SUCCESS, TITLE_SUCCESS, detail)
        .withResult(id)
        .getJsonResponse();
  }
  public static JsonObject StacDelRespSuccess (String id) {
    return new RespBuilder()
        .withType(SUCCESS)
        .withResult(id)
        .withDetail(STAC_DELETION_SUCCESS)
        .getJsonResponse();
  }
  public static JsonObject StacPostRespSuccess(String stacId) {
    return new RespBuilder()
        .withType(SUCCESS)
        .withResult(stacId, INSERT, SUCCESS)
        .withDetail(STAC_CREATION_SUCCESS)
        .getJsonResponse();
  }


  public static String operationNotAllowedResponse(String id, String method, String cause) {
    return createResponseBuilder(TYPE_OPERATION_NOT_ALLOWED, TITLE_OPERATION_NOT_ALLOWED, cause)
        .withResult(id, method, FAILED, cause)
        .getResponse();
  }

  public static String operationNotAllowedResponse(String id, String detail) {
    return createResponseBuilder(TYPE_OPERATION_NOT_ALLOWED, TITLE_OPERATION_NOT_ALLOWED)
        .withResult(id, detail)
        .getResponse();
  }

  public static String invalidSearchError() {
    return createResponseBuilder(
            TYPE_INVALID_SEARCH_ERROR, TITLE_INVALID_SEARCH_ERROR, TITLE_INVALID_SEARCH_ERROR)
        .getResponse();
  }

  public static String invalidParameterResp() {
    return createResponseBuilder(FAILED, ERROR_INVALID_PARAMETER, ERROR_INVALID_PARAMETER)
        .getResponse();
  }
}
