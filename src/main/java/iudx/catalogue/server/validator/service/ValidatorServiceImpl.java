package iudx.catalogue.server.validator.service;

import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.validator.util.Constants.*;
import static iudx.catalogue.server.validator.util.QueryModelUtil.*;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.apiserver.item.service.ItemService;
import iudx.catalogue.server.apiserver.util.RespBuilder;
import iudx.catalogue.server.common.util.DbResponseMessageBuilder;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import iudx.catalogue.server.database.elastic.util.ResponseFilter;
import iudx.catalogue.server.validator.Validator;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Validator Service Implementation.
 *
 * <h1>Validator Service Implementation</h1>
 *
 * <p>The Validator Service implementation in the IUDX Catalogue Server implements the definitions
 * of the {@link ValidatorService}.
 *
 * @version 1.0
 * @since 2020-05-31
 */
public class ValidatorServiceImpl implements ValidatorService {

  private static final Logger LOGGER = LogManager.getLogger(ValidatorServiceImpl.class);
  private final ItemService itemService;
  private final boolean isUacInstance;
  private final String vocContext;
  private Future<String> isValidSchema;
  private Validator resourceValidator;
  private Validator resourceGroupValidator;
  private Validator providerValidator;
  private Validator resourceServerValidator;
  private Validator cosItemValidator;
  private Validator ownerItemSchema;
  private Validator ratingValidator;
  private Validator mlayerInstanceValidator;
  private Validator mlayerDomainValidator;
  private Validator mlayerGeoQueryValidator;
  private Validator mlayerDatasetValidator;
  private Validator stack4PatchValidator;
  private Validator stackSchema4Post;

  /**
   * Constructs a new ValidatorServiceImpl object with the specified OldElasticClient and docIndex.
   *
   * @param itemService the itemService object to use for interacting with the Elasticsearch
   *                       service
   *     instance
   */
  public ValidatorServiceImpl(ItemService itemService, boolean isUacInstance,
                              String vocContext) {
    this.itemService = itemService;
    this.isUacInstance = isUacInstance;
    this.vocContext = vocContext;
    try {
      resourceValidator = new Validator("/resourceItemSchema.json");
      resourceGroupValidator = new Validator("/resourceGroupItemSchema.json");
      resourceServerValidator = new Validator("/resourceServerItemSchema.json");
      providerValidator = new Validator("/providerItemSchema.json");
      cosItemValidator = new Validator("/cosItemSchema.json");
      ownerItemSchema = new Validator("/ownerItemSchema.json");
      ratingValidator = new Validator("/ratingSchema.json");
      mlayerInstanceValidator = new Validator("/mlayerInstanceSchema.json");
      mlayerDomainValidator = new Validator("/mlayerDomainSchema.json");
      mlayerGeoQueryValidator = new Validator("/mlayerGeoQuerySchema.json");
      mlayerDatasetValidator = new Validator("/mlayerDatasetSchema.json");
      stack4PatchValidator = new Validator("/stackSchema4Patch.json");
      stackSchema4Post = new Validator("/stackSchema4Post.json");
    } catch (IOException | ProcessingException e) {
      e.printStackTrace();
    }
  }

  /** Generates timestamp with timezone +05:30. */
  public static String getUtcDatetimeAsString() {
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ");
    df.setTimeZone(TimeZone.getTimeZone("IST"));
    final String utcTime = df.format(new Date());
    return utcTime;
  }

  private static String getItemType(JsonObject requestBody) {
    Set<String> type = new HashSet<String>(new JsonArray().getList());
    try {
      type = new HashSet<String>(requestBody.getJsonArray(TYPE).getList());
    } catch (Exception e) {
      LOGGER.error("Fail: Invalid type");
    }
    type.retainAll(ITEM_TYPES);
    return String.join(", ", type);
  }

  private static boolean isValidUuid(String uuidString) {
    return UUID_PATTERN.matcher(uuidString).matches();
  }

  private static boolean validateId(JsonObject request, boolean isUacInstance) {
    if (request.containsKey("id")) {
      String id = request.getString("id");
      LOGGER.debug("id in the request body: " + id);

      if (!isValidUuid(id)) {
        return true;
      }
    } else {
      return !isUacInstance || request.containsKey("id");
    }
    return true;
  }

  String getReturnTypeForValidation(JsonObject result) {
    LOGGER.info("result : " + result);
    return result.getJsonArray(RESULTS).stream()
        .map(JsonObject.class::cast)
        .map(r -> r.getString(TYPE))
        .toList()
        .toString();
  }

  /*
   * {@inheritDoc}
   */
  @Override
  public Future<JsonObject> validateSchema(JsonObject request) {
    LOGGER.debug("Info: Reached Validator service validate schema");
    String itemType = null;
    itemType =
        request.containsKey("stack_type") ? request.getString("stack_type") : getItemType(request);
    request.remove("api");

    LOGGER.debug("Info: itemType: " + itemType);
    Promise<JsonObject> promise = Promise.promise();
    switch (itemType) {
      case ITEM_TYPE_RESOURCE:
        isValidSchema = resourceValidator.validate(request.toString());
        break;
      case ITEM_TYPE_RESOURCE_GROUP:
        isValidSchema = resourceGroupValidator.validate(request.toString());
        break;
      case ITEM_TYPE_RESOURCE_SERVER:
        isValidSchema = resourceServerValidator.validate(request.toString());
        break;
      case ITEM_TYPE_PROVIDER:
        isValidSchema = providerValidator.validate(request.toString());
        break;
      case ITEM_TYPE_COS:
        isValidSchema = cosItemValidator.validate(request.toString());
        break;
      case ITEM_TYPE_OWNER:
        isValidSchema = ownerItemSchema.validate(request.toString());
        break;
      case "patch:Stack":
        isValidSchema = stack4PatchValidator.validate(request.toString());
        break;
      case "post:Stack":
        isValidSchema = stackSchema4Post.validate(request.toString());
        break;
      default:
        promise.fail("Invalid Item Type");
        return promise.future();
    }

    return validateSchema();
  }

  private Future<JsonObject> validateSchema() {
    Promise<JsonObject> promise = Promise.promise();
    isValidSchema
        .onSuccess(x -> promise.complete(new JsonObject().put(STATUS, SUCCESS)))
        .onFailure(
            x -> {
              LOGGER.error("Fail: Invalid Schema");
              LOGGER.error(x.getMessage());
              promise.fail(String.valueOf(new JsonArray().add(x.getMessage())));
            });
    return promise.future();
  }

  @Override
  public Future<JsonObject> validateItem(JsonObject request) {
    request.put(CONTEXT, vocContext);
    String method = (String) request.remove(HTTP_METHOD);

    String itemType = getItemType(request);
    LOGGER.debug("Info: itemType: " + itemType);

    if (!validateId(request, isUacInstance)) {
      RespBuilder responseBuilder =
          new RespBuilder()
              .withType(TYPE_INVALID_UUID)
              .withTitle(TITLE_INVALID_UUID)
              .withDetail("Invalid Id in Request");
      return Future.failedFuture(responseBuilder.getResponse());
    }

    // Validate if Resource
    if (itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE)) {
      return validateResource(request, method);
    } else if (itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_SERVER)) {
      // Validate if Resource Server TODO: More checks and auth rules
      return validateResourceServer(request, method);
    } else if (itemType.equalsIgnoreCase(ITEM_TYPE_PROVIDER)) {
      return validateProvider(request, method);
    } else if (itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_GROUP)) {
      return validateResourceGroup(request, method);
    } else if (itemType.equalsIgnoreCase(ITEM_TYPE_COS)) {
      return validateCosItem(request, method);
    } else if (itemType.equalsIgnoreCase(ITEM_TYPE_OWNER)) {
      return validateOwnerItem(request, method);
    }

    return Future.failedFuture("Invalid Item Type");
  }

  @Override
  public Future<JsonObject> validateRating(JsonObject request) {
    isValidSchema = ratingValidator.validate(request.toString());
    return validateSchema();
  }

  @Override
  public Future<JsonObject> validateMlayerInstance(JsonObject request) {
    isValidSchema = mlayerInstanceValidator.validate(request.toString());
    return validateSchema();
  }

  @Override
  public Future<JsonObject> validateMlayerDomain(JsonObject request) {
    isValidSchema = mlayerDomainValidator.validate(request.toString());
    return validateSchema();
  }

  @Override
  public Future<JsonObject> validateMlayerGeoQuery(JsonObject request) {
    isValidSchema = mlayerGeoQueryValidator.validate(request.toString());
    return validateSchema();
  }

  @Override
  public Future<JsonObject> validateMlayerDatasetId(JsonObject request) {
    isValidSchema = mlayerDatasetValidator.validate(request.toString());
    return validateSchema();
  }

  private Future<JsonObject> validateResourceGroup(JsonObject request, String method) {
    //    validateId(request, isUacInstance);
    if (!isUacInstance && !request.containsKey(ID)) {
      UUID uuid = UUID.randomUUID();
      request.put(ID, uuid.toString());
    }

    request.put(ITEM_STATUS, ACTIVE).put(ITEM_CREATED_AT, getUtcDatetimeAsString());
    String provider = request.getString(PROVIDER);

    Promise<JsonObject> promise = Promise.promise();
    QueryModel queryModel = getValidateQueryForResourceGroup(request.getString(NAME), provider);
    itemService.search(queryModel, ResponseFilter.SOURCE_WITHOUT_EMBEDDINGS)
        .onComplete(
            res -> {
              if (res.failed()) {
                LOGGER.debug("Fail: DB Error");
                promise.fail(VALIDATION_FAILURE_MSG);
              }
              DbResponseMessageBuilder responseMsg = res.result();
              String returnType = getReturnTypeForValidation(responseMsg.getResponse());
              LOGGER.debug(returnType);
              if (responseMsg.getResponse().getInteger(TOTAL_HITS) < 1
                  || !returnType.contains(ITEM_TYPE_PROVIDER)) {
                LOGGER.debug("Provider does not exist");
                promise.fail("Fail: Provider item doesn't exist");
              } else if (method.equalsIgnoreCase(REQUEST_POST)
                  && returnType.contains(ITEM_TYPE_RESOURCE_GROUP)) {
                LOGGER.debug("RG already exists");
                promise.fail("Fail: Resource Group item already exists");
              } else {
                promise.complete(request);
              }
            });
    return promise.future();
  }

  private Future<JsonObject> validateProvider(JsonObject request, String method) {
    // Validate if Provider
    //    validateId(request, handler, isUacInstance);
    if (!isUacInstance && !request.containsKey(ID)) {
      UUID uuid = UUID.randomUUID();
      request.put(ID, uuid.toString());
    }
    request.put(ITEM_STATUS, ACTIVE).put(ITEM_CREATED_AT, getUtcDatetimeAsString());
    String resourceServer = request.getString(RESOURCE_SVR);
    String ownerUserId = request.getString(PROVIDER_USER_ID);
    String resourceServerUrl = request.getString(RESOURCE_SERVER_URL);

    Promise<JsonObject> promise = Promise.promise();
    QueryModel queryModel = getValidateQueryForProvider(resourceServer, ownerUserId,
        resourceServerUrl);
    itemService.search(queryModel, ResponseFilter.SOURCE_WITHOUT_EMBEDDINGS)
        .onComplete(
            res -> {
              if (res.failed()) {
                LOGGER.debug("Fail: DB Error");
                promise.fail(VALIDATION_FAILURE_MSG);
                return;
              }
              DbResponseMessageBuilder responseMsg = res.result();
              String returnType = getReturnTypeForValidation(responseMsg.getResponse());
              LOGGER.debug(returnType);

              LOGGER.debug("res result " + res.result());
              if (!returnType.contains(ITEM_TYPE_RESOURCE_SERVER)) {
                LOGGER.debug("RS does not exist");
                promise.fail("Fail: Resource Server item doesn't exist");
              } else if (method.equalsIgnoreCase(REQUEST_POST)
                  && returnType.contains(ITEM_TYPE_PROVIDER)) {
                LOGGER.debug("Provider already exists");
                promise.fail("Fail: Provider item for this resource server already exists");
              } else {
                promise.complete(request);
              }
            });
    return promise.future();
  }

  private Future<JsonObject> validateResourceServer(JsonObject request, String method) {
    //    validateId(request, handler, isUacInstance);
    if (!isUacInstance && !request.containsKey(ID)) {
      UUID uuid = UUID.randomUUID();
      request.put(ID, uuid.toString());
    }
    request.put(ITEM_STATUS, ACTIVE).put(ITEM_CREATED_AT, getUtcDatetimeAsString());
    String cos = request.getString(COS_ITEM);
    String resourceServerUrl = request.getString(RESOURCE_SERVER_URL);

    Promise<JsonObject> promise = Promise.promise();
    QueryModel queryModel = getValidateQueryForResourceServer(cos, resourceServerUrl);
    itemService.search(queryModel, ResponseFilter.SOURCE_WITHOUT_EMBEDDINGS)
        .onComplete(
            res -> {
              if (res.failed()) {
                LOGGER.debug("Fail: DB Error");
                promise.fail(VALIDATION_FAILURE_MSG);
                return;
              }
              DbResponseMessageBuilder responseMsg = res.result();
              String returnType = getReturnTypeForValidation(responseMsg.getResponse());
              LOGGER.debug(returnType);

              if (responseMsg.getResponse().getInteger(TOTAL_HITS) < 1
                  || !returnType.contains(ITEM_TYPE_COS)) {
                LOGGER.debug("Cos does not exist");
                promise.fail("Fail: Cos item doesn't exist");
              } else if (method.equalsIgnoreCase(REQUEST_POST)
                  && returnType.contains(ITEM_TYPE_RESOURCE_SERVER)) {
                LOGGER.debug("RS already exists");
                promise.fail(
                    String.format(
                        "Fail: Resource Server item with url %s already exists for this COS",
                        resourceServerUrl));
              } else {
                promise.complete(request);
              }
            });
    return promise.future();
  }

  private Future<JsonObject> validateResource(JsonObject request, String method) {
    //    validateId(request, handler, isUacInstance);
    if (!isUacInstance && !request.containsKey("id")) {
      UUID uuid = UUID.randomUUID();
      request.put("id", uuid.toString());
    }
    request.put(ITEM_STATUS, ACTIVE).put(ITEM_CREATED_AT, getUtcDatetimeAsString());
    String resourceServer = request.getString(RESOURCE_SVR);
    String provider = request.getString(PROVIDER);
    String resourceGroup = request.getString(RESOURCE_GRP);

    Promise<JsonObject> promise = Promise.promise();
    QueryModel queryModel = getValidateQueryForResource(request.getString(NAME), resourceServer,
        provider, resourceGroup);
    itemService.search(queryModel, ResponseFilter.SOURCE_WITHOUT_EMBEDDINGS)
        .onComplete(
            res -> {
              if (res.failed()) {
                LOGGER.debug("Fail: DB Error");
                promise.fail(VALIDATION_FAILURE_MSG);
                return;
              }
              DbResponseMessageBuilder responseMsg = res.result();
              String returnType = getReturnTypeForValidation(responseMsg.getResponse());
              LOGGER.debug(returnType);

              if (responseMsg.getResponse().getInteger(TOTAL_HITS) < 3
                  && !returnType.contains(ITEM_TYPE_RESOURCE_SERVER)) {
                LOGGER.debug("RS does not exist");
                promise.fail("Fail: Resource Server item doesn't exist");
              } else if (responseMsg.getResponse().getInteger(TOTAL_HITS) < 3
                  && !returnType.contains(ITEM_TYPE_PROVIDER)) {
                LOGGER.debug("Provider does not exist");
                promise.fail("Fail: Provider item doesn't exist");
              } else if (responseMsg.getResponse().getInteger(TOTAL_HITS) < 3
                  && !returnType.contains(ITEM_TYPE_RESOURCE_GROUP)) {
                LOGGER.debug("RG does not exist");
                promise.fail("Fail: Resource Group item doesn't exist");
              } else if (method.equalsIgnoreCase(REQUEST_POST)
                  && responseMsg.getResponse().getInteger(TOTAL_HITS) > 3) {
                LOGGER.debug("RI already exists");
                promise.fail("Fail: Resource item already exists");
              } else {
                promise.complete(request);
              }
            });
    return promise.future();
  }

  private Future<JsonObject> validateCosItem(JsonObject request, String method) {
    //    validateId(request, handler, isUacInstance);
    if (!isUacInstance && !request.containsKey(ID)) {
      UUID uuid = UUID.randomUUID();
      request.put(ID, uuid.toString());
    }
    request.put(ITEM_STATUS, ACTIVE).put(ITEM_CREATED_AT, getUtcDatetimeAsString());
    String owner = request.getString(OWNER);

    Promise<JsonObject> promise = Promise.promise();
    QueryModel queryModel = getValidateQueryForCos(request.getString(NAME), owner);
    itemService.search(queryModel, ResponseFilter.SOURCE_WITHOUT_EMBEDDINGS)
        .onComplete(
            res -> {
              if (res.failed()) {
                LOGGER.debug("Fail: DB Error");
                promise.fail(VALIDATION_FAILURE_MSG);
                return;
              }
              DbResponseMessageBuilder responseMsg = res.result();
              String returnType = getReturnTypeForValidation(responseMsg.getResponse());
              LOGGER.debug(returnType);
              if (responseMsg.getResponse().getInteger(TOTAL_HITS) < 1
                  || !returnType.contains(ITEM_TYPE_OWNER)) {
                LOGGER.debug("Owner does not exist");
                promise.fail("Fail: Owner item doesn't exist");
              } else if (method.equalsIgnoreCase(REQUEST_POST)
                  && returnType.contains(ITEM_TYPE_COS)) {
                LOGGER.debug("COS already exists");
                promise.fail("Fail: COS item already exists");
              } else {
                promise.complete(request);
              }
            });
    return promise.future();
  }

  private Future<JsonObject> validateOwnerItem(JsonObject request, String method) {
    //    validateId(request, handler, isUacInstance);
    if (!isUacInstance && !request.containsKey(ID)) {
      UUID uuid = UUID.randomUUID();
      request.put(ID, uuid.toString());
    }
    request.put(ITEM_STATUS, ACTIVE).put(ITEM_CREATED_AT, getUtcDatetimeAsString());

    Promise<JsonObject> promise = Promise.promise();
    QueryModel queryModel = getValidateQueryForOwner(request.getString(NAME));
    itemService.search(queryModel, ResponseFilter.SOURCE_ONLY)
        .onComplete(
            res -> {
              if (res.failed()) {
                LOGGER.debug("Fail: DB Error");
                promise.fail(VALIDATION_FAILURE_MSG);
                return;
              }
              if (method.equalsIgnoreCase(REQUEST_POST) &&
                  res.result().getResponse().getInteger(TOTAL_HITS) != 0) {
                LOGGER.debug("Owner item already exists");
                promise.fail("Fail: Owner item already exists");
              } else {
                promise.complete(request);
              }
            });
    return promise.future();
  }
}
