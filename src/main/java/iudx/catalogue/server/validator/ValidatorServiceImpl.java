package iudx.catalogue.server.validator;

import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.validator.Constants.*;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.ElasticClient;
import iudx.catalogue.server.validator.util.SearchQueryValidatorHelper;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Validator Service Implementation.
 *
 * <h1>Validator Service Implementation</h1>
 *
 * <p>The Validator Service implementation in the IUDX Catalogue Server implements the definitions
 * of the {@link iudx.catalogue.server.validator.ValidatorService}.
 *
 * @version 1.0
 * @since 2020-05-31
 */
public class ValidatorServiceImpl implements ValidatorService {

  private static final Logger LOGGER = LogManager.getLogger(ValidatorServiceImpl.class);

  /**
   * ES client.
   */
  static ElasticClient client;
  private final String docIndex;
  private final boolean isUacInstance;
  private final String vocContext;
  private Future<String> isValidSchema;
  private Validator resourceValidator;
  private Validator resourceGroupValidator;
  private Validator providerValidator;
  private Validator resourceServerValidator;
  private Validator cosItemValidator;
  private Validator ownerItemSchema;
  private Validator aiModelValidator;
  private Validator dataBankResourceValidator;
  private Validator adexAppsValidator;
  private Validator ratingValidator;
  private Validator mlayerInstanceValidator;
  private Validator mlayerDomainValidator;
  private Validator mlayerGeoQueryValidator;
  private Validator mlayerDatasetValidator;
  private Validator termValidator;
  private Validator rangeValidator;
  private Validator geoSearchQueryValidator;
  private Validator temporalValidator;
  private Validator textSearchQueryValidator;
  private Validator filterSearchQueryValidator;
  private Validator stack4PatchValidator;
  private Validator stackSchema4Post;

  /**
   * Constructs a new ValidatorServiceImpl object with the specified ElasticClient and docIndex.
   *
   * @param client   the ElasticClient object to use for interacting with the Elasticsearch instance
   * @param docIndex the index name to use for storing documents in Elasticsearch
   */
  public ValidatorServiceImpl(
      ElasticClient client, String docIndex, boolean isUacInstance, String vocContext) {

    ValidatorServiceImpl.client = client;
    this.docIndex = docIndex;
    this.isUacInstance = isUacInstance;
    this.vocContext = vocContext;
    try {
      resourceValidator = new Validator("/resourceItemSchema.json");
      resourceGroupValidator = new Validator("/resourceGroupItemSchema.json");
      resourceServerValidator = new Validator("/resourceServerItemSchema.json");
      providerValidator = new Validator("/providerItemSchema.json");
      cosItemValidator = new Validator("/cosItemSchema.json");
      ownerItemSchema = new Validator("/ownerItemSchema.json");
      aiModelValidator = new Validator("/adexAiModelItemSchema.json");
      dataBankResourceValidator = new Validator("/adexDataBankResourceItemSchema.json");
      adexAppsValidator = new Validator("/adexAppsItemSchema.json");
      ratingValidator = new Validator("/ratingSchema.json");
      mlayerInstanceValidator = new Validator("/mlayerInstanceSchema.json");
      mlayerDomainValidator = new Validator("/mlayerDomainSchema.json");
      mlayerGeoQueryValidator = new Validator("/mlayerGeoQuerySchema.json");
      mlayerDatasetValidator = new Validator("/mlayerDatasetSchema.json");
      stack4PatchValidator = new Validator("/stackSchema4Patch.json");
      stackSchema4Post = new Validator("/stackSchema4Post.json");
      geoSearchQueryValidator = new Validator("/geoSearchQuerySchema.json");
      textSearchQueryValidator = new Validator("/textSearchQuerySchema.json");
      filterSearchQueryValidator = new Validator("/filterSearchQuerySchema.json");
      rangeValidator = new Validator("/rangeSearchQuerySchema.json");
      temporalValidator = new Validator("/temporalSearchQuerySchema.json");
      termValidator = new Validator("/attributeSearchQuerySchema.json");
    } catch (IOException | ProcessingException e) {
      e.printStackTrace();
    }
  }

  /**
   * Generates timestamp with timezone +05:30.
   */
  public static String getUtcDatetimeAsString() {
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ");
    df.setTimeZone(TimeZone.getTimeZone("IST"));
    return df.format(new Date());
  }

  public static String getPrettyLastUpdatedForUI() {
    DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    DateTimeFormatter outputFormatter = DateTimeFormatter
        .ofPattern("dd MMMM, yyyy - hh:mm a", Locale.ENGLISH);

    // Format the current date in IST
    ZonedDateTime nowIst = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
    String istTime = nowIst.format(inputFormatter);

    // Parse using OffsetDateTime (handles the +0530 format correctly)
    OffsetDateTime offsetDateTime = OffsetDateTime.parse(istTime, inputFormatter);

    // Format to the desired output
    return offsetDateTime.format(outputFormatter);
  }

  private static String getItemType(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    Set<String> type = new HashSet<String>(new JsonArray().getList());
    try {
      type = new HashSet<String>(request.getJsonArray(TYPE).getList());
    } catch (Exception e) {
      LOGGER.error("Item type mismatch");
      handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
    }
    type.retainAll(ITEM_TYPES);
    String itemType = type.toString().replaceAll("\\[", "").replaceAll("\\]", "");
    return itemType;
  }

  String getReturnTypeForValidation(JsonObject result) {
    LOGGER.debug(result);
    return result.getJsonArray(RESULTS).stream()
        .map(JsonObject.class::cast)
        .map(r -> r.getString(TYPE))
        .collect(Collectors.toList())
        .toString();
  }

  /*
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  public ValidatorService validateSchema(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    LOGGER.debug("Info: Reached Validator service validate schema");
    String itemType = null;
    itemType =
        request.containsKey("stack_type")
            ? request.getString("stack_type")
            : getItemType(request, handler);
    request.remove("api");

    LOGGER.debug("Info: itemType: " + itemType);

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
      case ITEM_TYPE_AI_MODEL:
        isValidSchema = aiModelValidator.validate(request.toString());
        break;
      case ITEM_TYPE_DATA_BANK:
        isValidSchema = dataBankResourceValidator.validate(request.toString());
        break;
      case ITEM_TYPE_APPS:
        isValidSchema = adexAppsValidator.validate(request.toString());
        break;
      case "patch:Stack":
        isValidSchema = stack4PatchValidator.validate(request.toString());
        break;
      case "post:Stack":
        isValidSchema = stackSchema4Post.validate(request.toString());
        break;
      default:
        handler.handle(Future.failedFuture("Invalid Item Type"));
        return this;
    }

    validateSchema(handler);
    return this;
  }

  private void validateSchema(Handler<AsyncResult<JsonObject>> handler) {
    isValidSchema
        .onSuccess(
            x -> handler.handle(Future.succeededFuture(new JsonObject().put(STATUS, SUCCESS))))
        .onFailure(
            x -> {
              LOGGER.error("Fail: Invalid Schema");
              LOGGER.error(x.getMessage());
              handler.handle(
                  Future.failedFuture((x.getMessage())));
            });
  }

  /*
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public ValidatorService validateItem(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    request.put(CONTEXT, vocContext);
    String method = (String) request.remove(HTTP_METHOD);

    String itemType = getItemType(request, handler);
    LOGGER.debug("Info: itemType: " + itemType);

    // Validate if Resource
    if (itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE)) {
      validateResource(request, method, handler);
    } else if (itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_SERVER)) {
      // Validate if Resource Server TODO: More checks and auth rules
      validateResourceServer(request, method, handler);
    } else if (itemType.equalsIgnoreCase(ITEM_TYPE_PROVIDER)) {
      validateProvider(request, method, handler);
    } else if (itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_GROUP)) {
      validateResourceGroup(request, method, handler);
    } else if (itemType.equalsIgnoreCase(ITEM_TYPE_COS)) {
      validateCosItem(request, method, handler);
    } else if (itemType.equalsIgnoreCase(ITEM_TYPE_OWNER)) {
      validateOwnerItem(request, method, handler);
    } else if (itemType.equalsIgnoreCase(ITEM_TYPE_AI_MODEL)) {
      validateAiModelItem(request, method, handler);
    } else if (itemType.equalsIgnoreCase(ITEM_TYPE_DATA_BANK)) {
      validateDataBankItem(request, method, handler);
    } else if (itemType.equalsIgnoreCase(ITEM_TYPE_APPS)) {
      validateAppsItem(request, method, handler);
    }
    return this;
  }

  private void validateAppsItem(JsonObject request, String method,
                                Handler<AsyncResult<JsonObject>> handler) {
    validateId(request, handler, isUacInstance);
    if (!isUacInstance && !request.containsKey(ID)) {
      UUID uuid = UUID.randomUUID();
      request.put(ID, uuid.toString());
    }
    request.put(ITEM_STATUS, ACTIVE).put(LAST_UPDATED, getPrettyLastUpdatedForUI())
        .put(ITEM_CREATED_AT, getUtcDatetimeAsString());
    String checkQuery = ITEM_WITH_NAME_EXISTS_QUERY
        .replace("$1", ITEM_TYPE_APPS).replace("$2", request.getString(NAME));
    LOGGER.debug(checkQuery);
    client.searchGetId(
        checkQuery,
        docIndex,
        res -> {
          if (res.failed()) {
            LOGGER.debug("Fail: DB Error");
            handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
            return;
          }
          if (method.equalsIgnoreCase(REQUEST_POST) && res.result().getInteger(TOTAL_HITS) > 0) {
            LOGGER.debug("potential apps item already exists with the given name");
            handler.handle(Future.failedFuture("Fail: Apps item already exists"));
          } else {
            handler.handle(Future.succeededFuture(request));
          }
        });
  }

  private void validateAiModelItem(JsonObject request, String method,
                                   Handler<AsyncResult<JsonObject>> handler) {
    validateId(request, handler, isUacInstance);
    if (!isUacInstance && !request.containsKey(ID)) {
      UUID uuid = UUID.randomUUID();
      request.put(ID, uuid.toString());
    }

    request.put(ITEM_STATUS, ACTIVE)
        .put(LAST_UPDATED, getPrettyLastUpdatedForUI())
        .put(ITEM_CREATED_AT, getUtcDatetimeAsString());

    String checkQuery = ITEM_WITH_NAME_EXISTS_QUERY
        .replace("$1", ITEM_TYPE_AI_MODEL).replace("$2", request.getString(NAME));

    client.searchAsync(
        checkQuery,
        docIndex,
        res -> {
          if (res.failed()) {
            LOGGER.debug("Fail: DB Error");
            handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
            return;
          }
          String returnType = getReturnTypeForValidation(res.result());
          LOGGER.debug(returnType);
          if (method.equalsIgnoreCase(REQUEST_POST)
              && returnType.contains(ITEM_TYPE_AI_MODEL)) {
            LOGGER.debug("AI Model already exists with the name {} in organization {}",
                request.getString(NAME), request.getString(ORGANIZATION_ID));
            handler.handle(Future.failedFuture("Fail: AI Model item already exists"));
            return;
          }

          // Handle dataUploadStatus here — after checking existing ES data
          boolean mediaUrlPresent =
              request.containsKey(MEDIA_URL) && !request.getString(MEDIA_URL).isBlank();
          if (method.equalsIgnoreCase(REQUEST_POST)) {
            // On POST: Always infer from mediaURL
            request.put(DATA_UPLOAD_STATUS, mediaUrlPresent);
            request.put(PUBLISH_STATUS, PENDING);
          } else if (method.equalsIgnoreCase(REQUEST_PUT)) {
            // On PUT: Preserve previous true if already set
            boolean wasPreviouslyUploaded = extractDataUploadStatusFromES(res.result());
            boolean previousMediaUrlPresent = extractMediaUrlFromES(res.result());

            if (mediaUrlPresent) {
              request.put(DATA_UPLOAD_STATUS, true);
            } else if (wasPreviouslyUploaded && !previousMediaUrlPresent) {
              // Preserve only if upload was done via the alternate (non-mediaURL) flow
              request.put(DATA_UPLOAD_STATUS, true);
            } else {
              request.put(DATA_UPLOAD_STATUS, false);
            }

            String publishStatus = extractPublishStatusFromES(res.result());
            request.put(PUBLISH_STATUS, publishStatus);
          }

          handler.handle(Future.succeededFuture(request));
        });
  }

  private void validateDataBankItem(JsonObject request, String method,
                                   Handler<AsyncResult<JsonObject>> handler) {
    validateId(request, handler, isUacInstance);
    if (!isUacInstance && !request.containsKey(ID)) {
      UUID uuid = UUID.randomUUID();
      request.put(ID, uuid.toString());
    }

    request.put(ITEM_STATUS, ACTIVE)
        .put(LAST_UPDATED, getPrettyLastUpdatedForUI())
        .put(ITEM_CREATED_AT, getUtcDatetimeAsString());

    String checkQuery = ITEM_WITH_NAME_EXISTS_QUERY
        .replace("$1", ITEM_TYPE_DATA_BANK)
        .replace("$2", request.getString(NAME));

    client.searchAsync(
        checkQuery,
        docIndex,
        res -> {
          if (res.failed()) {
            LOGGER.debug("Fail: DB Error");
            handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
            return;
          }
          String returnType = getReturnTypeForValidation(res.result());
          LOGGER.debug(returnType);
          if (method.equalsIgnoreCase(REQUEST_POST)
              && returnType.contains(ITEM_TYPE_DATA_BANK)) {
            LOGGER.debug("Data Bank item already exists with the name {} in organization {}",
                request.getString(NAME), request.getString(ORGANIZATION_ID));
            handler.handle(Future.failedFuture("Fail: DataBank item already exists"));
            return;
          }

          // Handle dataUploadStatus here — after checking existing ES data
          boolean mediaUrlPresent =
              request.containsKey(MEDIA_URL) && !request.getString(MEDIA_URL).isBlank();
          if (method.equalsIgnoreCase(REQUEST_POST)) {
            // On POST: Always infer from mediaURL
            request.put(DATA_UPLOAD_STATUS, mediaUrlPresent);
            request.put(PUBLISH_STATUS, PENDING);
          } else if (method.equalsIgnoreCase(REQUEST_PUT)) {
            // On PUT: Preserve previous true if already set
            boolean wasPreviouslyUploaded = extractDataUploadStatusFromES(res.result());
            boolean previousMediaUrlPresent = extractMediaUrlFromES(res.result());

            if (mediaUrlPresent) {
              request.put(DATA_UPLOAD_STATUS, true);
            } else if (wasPreviouslyUploaded && !previousMediaUrlPresent) {
              // Preserve only if upload was done via the alternate (non-mediaURL) flow
              request.put(DATA_UPLOAD_STATUS, true);
            } else {
              request.put(DATA_UPLOAD_STATUS, false);
            }

            String publishStatus = extractPublishStatusFromES(res.result());
            request.put(PUBLISH_STATUS, publishStatus);
          }

          handler.handle(Future.succeededFuture(request));
        });
  }

  private boolean extractDataUploadStatusFromES(JsonObject esResult) {
    try {
      JsonObject res = esResult.getJsonArray(RESULTS).getJsonObject(0);
      if (res != null && !res.isEmpty()) {
        return res.getBoolean(DATA_UPLOAD_STATUS, false);
      }
    } catch (Exception e) {
      LOGGER.error("Error extracting dataUploadStatus from ES", e);
    }
    return false;
  }

  private boolean extractMediaUrlFromES(JsonObject esResult) {
    try {
      JsonObject res = esResult.getJsonArray(RESULTS).getJsonObject(0);
      if (res != null && !res.isEmpty()) {
        String mediaUrl = res.getString(MEDIA_URL, "");
        return !mediaUrl.isBlank();
      }
    } catch (Exception e) {
      LOGGER.error("Error extracting mediaURL from ES", e);
    }
    return false;
  }

  private String extractPublishStatusFromES(JsonObject esResult) {
    try {
      JsonObject res = esResult.getJsonArray(RESULTS).getJsonObject(0);
      if (res != null && !res.isEmpty()) {
        return res.getString(PUBLISH_STATUS, PENDING);
      }
    } catch (Exception e) {
      LOGGER.error("Error extracting mediaURL from ES", e);
    }
    return PENDING;
  }

  private void validateResourceGroup(
      JsonObject request, String method, Handler<AsyncResult<JsonObject>> handler) {
    validateId(request, handler, isUacInstance);
    if (!isUacInstance && !request.containsKey(ID)) {
      UUID uuid = UUID.randomUUID();
      request.put(ID, uuid.toString());
    }

    request.put(ITEM_STATUS, ACTIVE).put(LAST_UPDATED, getPrettyLastUpdatedForUI())
        .put(ITEM_CREATED_AT, getUtcDatetimeAsString());
    String provider = request.getString(PROVIDER);
    String checkQuery =
        ITEM_EXISTS_QUERY
            .replace("$1", provider)
            .replace("$2", ITEM_TYPE_RESOURCE_GROUP)
            .replace("$3", NAME)
            .replace("$4", request.getString(NAME));
    client.searchAsync(
        checkQuery,
        docIndex,
        res -> {
          if (res.failed()) {
            LOGGER.debug("Fail: DB Error");
            handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
            return;
          }
          String returnType = getReturnTypeForValidation(res.result());
          LOGGER.debug(returnType);
          if (res.result().getInteger(TOTAL_HITS) < 1 || !returnType.contains(ITEM_TYPE_PROVIDER)) {
            LOGGER.debug("Provider does not exist");
            handler.handle(Future.failedFuture("Fail: Provider item doesn't exist"));
          } else if (method.equalsIgnoreCase(REQUEST_POST)
              && returnType.contains(ITEM_TYPE_RESOURCE_GROUP)) {
            LOGGER.debug("RG already exists");
            handler.handle(Future.failedFuture("Fail: Resource Group item already exists"));
          } else {
            handler.handle(Future.succeededFuture(request));
          }
        });
  }

  private void validateProvider(
      JsonObject request, String method, Handler<AsyncResult<JsonObject>> handler) {
    // Validate if Provider
    validateId(request, handler, isUacInstance);
    if (!isUacInstance && !request.containsKey(ID)) {
      UUID uuid = UUID.randomUUID();
      request.put(ID, uuid.toString());
    }

    request.put(ITEM_STATUS, ACTIVE).put(LAST_UPDATED, getPrettyLastUpdatedForUI())
        .put(ITEM_CREATED_AT, getUtcDatetimeAsString());
    String resourceServer = request.getString(RESOURCE_SVR);
    String ownerUserId = request.getString(PROVIDER_USER_ID);
    String resourceServerUrl = request.getString(RESOURCE_SERVER_URL);
    String checkQuery =
        PROVIDER_ITEM_EXISTS_QUERY
            .replace("$1", resourceServer)
            .replace("$2", ownerUserId)
            .replace("$3", resourceServerUrl);

    LOGGER.debug("query provider exists " + checkQuery);
    client.searchAsync(
        checkQuery,
        docIndex,
        res -> {
          if (res.failed()) {
            LOGGER.debug("Fail: DB Error");
            handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
            return;
          }
          String returnType = getReturnTypeForValidation(res.result());
          LOGGER.debug(returnType);

          LOGGER.debug("res result " + res.result());
          if (!returnType.contains(ITEM_TYPE_RESOURCE_SERVER)) {
            LOGGER.debug("RS does not exist");
            handler.handle(Future.failedFuture("Fail: Resource Server item doesn't exist"));
          } else if (method.equalsIgnoreCase(REQUEST_POST)
              && returnType.contains(ITEM_TYPE_PROVIDER)) {
            LOGGER.debug("Provider already exists");
            handler.handle(
                Future.failedFuture("Fail: Provider item for this resource server already exists"));
          } else {
            handler.handle(Future.succeededFuture(request));
          }
        });
  }

  private void validateResourceServer(
      JsonObject request, String method, Handler<AsyncResult<JsonObject>> handler) {
    validateId(request, handler, isUacInstance);
    if (!isUacInstance && !request.containsKey(ID)) {
      UUID uuid = UUID.randomUUID();
      request.put(ID, uuid.toString());
    }

    request.put(ITEM_STATUS, ACTIVE).put(LAST_UPDATED, getPrettyLastUpdatedForUI())
        .put(ITEM_CREATED_AT, getUtcDatetimeAsString());
    String cos = request.getString(COS_ITEM);
    String resourceServerUrl = request.getString(RESOURCE_SERVER_URL);
    String checkQuery =
        ITEM_EXISTS_QUERY
            .replace("$1", cos)
            .replace("$2", ITEM_TYPE_RESOURCE_SERVER)
            .replace("$3", RESOURCE_SERVER_URL)
            .replace("$4", resourceServerUrl);
    LOGGER.debug(checkQuery);
    client.searchAsync(
        checkQuery,
        docIndex,
        res -> {
          if (res.failed()) {
            LOGGER.debug("Fail: DB Error");
            handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
            return;
          }
          String returnType = getReturnTypeForValidation(res.result());
          LOGGER.debug(returnType);

          if (res.result().getInteger(TOTAL_HITS) < 1 || !returnType.contains(ITEM_TYPE_COS)) {
            LOGGER.debug("Cos does not exist");
            handler.handle(Future.failedFuture("Fail: Cos item doesn't exist"));
          } else if (method.equalsIgnoreCase(REQUEST_POST)
              && returnType.contains(ITEM_TYPE_RESOURCE_SERVER)) {
            LOGGER.debug("RS already exists");
            handler.handle(
                Future.failedFuture(
                    String.format(
                        "Fail: Resource Server item with url %s already exists for this COS",
                        resourceServerUrl)));
          } else {
            handler.handle(Future.succeededFuture(request));
          }
        });
  }

  private void validateResource(
      JsonObject request, String method, Handler<AsyncResult<JsonObject>> handler) {
    validateId(request, handler, isUacInstance);
    if (!isUacInstance && !request.containsKey(ID)) {
      UUID uuid = UUID.randomUUID();
      request.put(ID, uuid.toString());
    }

    request.put(ITEM_STATUS, ACTIVE).put(LAST_UPDATED, getPrettyLastUpdatedForUI())
        .put(ITEM_CREATED_AT, getUtcDatetimeAsString());
    String provider = request.getString(PROVIDER);
    String resourceGroup = request.getString(RESOURCE_GRP);
    String resourceServer = request.getString(RESOURCE_SVR);

    String checkQuery =
        RESOURCE_ITEM_EXISTS_QUERY
            .replace("$1", resourceServer)
            .replace("$2", provider)
            .replace("$3", resourceGroup)
            .replace("$4", request.getString(NAME));
    LOGGER.debug(checkQuery);

    client.searchAsync(
        checkQuery,
        docIndex,
        res -> {
          if (res.failed()) {
            LOGGER.debug("Fail: DB Error");
            handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
            return;
          }
          String returnType = getReturnTypeForValidation(res.result());
          LOGGER.debug(returnType);

          if (res.result().getInteger(TOTAL_HITS) < 3
              && !returnType.contains(ITEM_TYPE_RESOURCE_SERVER)) {
            LOGGER.debug("RS does not exist");
            handler.handle(Future.failedFuture("Fail: Resource Server item doesn't exist"));
          } else if (res.result().getInteger(TOTAL_HITS) < 3
              && !returnType.contains(ITEM_TYPE_PROVIDER)) {
            LOGGER.debug("Provider does not exist");
            handler.handle(Future.failedFuture("Fail: Provider item doesn't exist"));
          } else if (res.result().getInteger(TOTAL_HITS) < 3
              && !returnType.contains(ITEM_TYPE_RESOURCE_GROUP)) {
            LOGGER.debug("RG does not exist");
            handler.handle(Future.failedFuture("Fail: Resource Group item doesn't exist"));
          } else if (method.equalsIgnoreCase(REQUEST_POST)
              && res.result().getInteger(TOTAL_HITS) > 3) {
            LOGGER.debug("RI already exists");
            String errorMessage = String.format(
                "Fail: Resource item with the name '%s' already exists in the resource group '%s'",
                request.getString(NAME), resourceGroup);
            handler.handle(Future.failedFuture(errorMessage));
          } else {
            handler.handle(Future.succeededFuture(request));
          }
        });
  }

  private void validateCosItem(
      JsonObject request, String method, Handler<AsyncResult<JsonObject>> handler) {
    validateId(request, handler, isUacInstance);
    if (!isUacInstance && !request.containsKey(ID)) {
      UUID uuid = UUID.randomUUID();
      request.put(ID, uuid.toString());
    }
    request.put(ITEM_STATUS, ACTIVE).put(LAST_UPDATED, getPrettyLastUpdatedForUI())
        .put(ITEM_CREATED_AT, getUtcDatetimeAsString());

    String owner = request.getString(OWNER);
    String checkQuery =
        ITEM_EXISTS_QUERY
            .replace("$1", owner)
            .replace("$2", ITEM_TYPE_COS)
            .replace("$3", NAME)
            .replace("$4", request.getString(NAME));
    LOGGER.debug(checkQuery);
    client.searchAsync(
        checkQuery,
        docIndex,
        res -> {
          if (res.failed()) {
            LOGGER.debug("Fail: DB Error");
            handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
            return;
          }
          String returnType = getReturnTypeForValidation(res.result());
          LOGGER.debug(returnType);
          if (res.result().getInteger(TOTAL_HITS) < 1 || !returnType.contains(ITEM_TYPE_OWNER)) {
            LOGGER.debug("Owner does not exist");
            handler.handle(Future.failedFuture("Fail: Owner item doesn't exist"));
          } else if (method.equalsIgnoreCase(REQUEST_POST) && returnType.contains(ITEM_TYPE_COS)) {
            LOGGER.debug("COS already exists");
            handler.handle(Future.failedFuture("Fail: COS item already exists"));
          } else {
            handler.handle(Future.succeededFuture(request));
          }
        });
  }

  private void validateOwnerItem(
      JsonObject request, String method, Handler<AsyncResult<JsonObject>> handler) {
    validateId(request, handler, isUacInstance);
    if (!isUacInstance && !request.containsKey(ID)) {
      UUID uuid = UUID.randomUUID();
      request.put(ID, uuid.toString());
    }
    request.put(ITEM_STATUS, ACTIVE).put(LAST_UPDATED, getPrettyLastUpdatedForUI())
        .put(ITEM_CREATED_AT, getUtcDatetimeAsString());
    String checkQuery = OWNER_ITEM_EXISTS_QUERY.replace("$1", request.getString(NAME));
    LOGGER.debug(checkQuery);
    client.searchGetId(
        checkQuery,
        docIndex,
        res -> {
          if (res.failed()) {
            LOGGER.debug("Fail: DB Error");
            handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
            return;
          }
          if (method.equalsIgnoreCase(REQUEST_POST) && res.result().getInteger(TOTAL_HITS) > 0) {
            LOGGER.debug("Owner item already exists");
            handler.handle(Future.failedFuture("Fail: Owner item already exists"));
          } else {
            handler.handle(Future.succeededFuture(request));
          }
        });
  }

  private boolean isValidUuid(String uuidString) {
    return UUID_PATTERN.matcher(uuidString).matches();
  }

  private void validateId(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler, boolean isUacInstance) {
    if (request.containsKey(ID)) {
      String id = request.getString(ID);
      LOGGER.debug("id in the request body: " + id);

      if (!isValidUuid(id)) {
        handler.handle(Future.failedFuture("validation failed. Incorrect id"));
      }
    } else if (isUacInstance && !request.containsKey(ID)) {
      handler.handle(Future.failedFuture("mandatory id field not present in request body"));
    }
  }

  @Override
  public ValidatorService validateRating(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    isValidSchema = ratingValidator.validate(request.toString());

    validateSchema(handler);
    return this;
  }

  @Override
  public ValidatorService validateMlayerInstance(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    isValidSchema = mlayerInstanceValidator.validate(request.toString());
    validateSchema(handler);
    return null;
  }

  @Override
  public ValidatorService validateMlayerDomain(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    isValidSchema = mlayerDomainValidator.validate(request.toString());

    validateSchema(handler);
    return this;
  }

  @Override
  public ValidatorService validateMlayerGeoQuery(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    isValidSchema = mlayerGeoQueryValidator.validate(request.toString());

    validateSchema(handler);
    return this;
  }

  @Override
  public ValidatorService validateMlayerDatasetId(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    isValidSchema = mlayerDatasetValidator.validate(request.toString());

    validateSchema(handler);
    return this;
  }

  @Override
  public ValidatorService validateSearchQuery(JsonObject request,
                                              Handler<AsyncResult<JsonObject>> handler) {
    LOGGER.debug("Info: Validating attributes limits and  constraints");
    String searchType = request.getString(SEARCH_TYPE, "");

    List<Future<JsonObject>> validations = new ArrayList<>();

    if (searchType.contains(SEARCH_TYPE_TEXT)) {
      Promise<JsonObject> p = Promise.promise();
      this.validateTextSearchQuery(request, p);
      validations.add(p.future());
    }
    if (searchType.contains(SEARCH_TYPE_CRITERIA)) {
      Promise<JsonObject> p = Promise.promise();
      this.validateSearchCriteria(request, p);
      validations.add(p.future());
    }
    if (searchType.contains(SEARCH_TYPE_GEO)) {
      Promise<JsonObject> p = Promise.promise();
      this.validateGeoSearchQuery(request, p);
      validations.add(p.future());
    }
    if (searchType.contains(RESPONSE_FILTER)) {
      Promise<JsonObject> p = Promise.promise();
      this.validateFilterSearchQuery(request, p);
      validations.add(p.future());
    }

    Future.all(validations)
        .onFailure(err -> {
          LOGGER.error("Fail: Invalid Schema: {}", err.getMessage());
          handler.handle(Future.failedFuture(err.getLocalizedMessage()));
        });

    // Additional validation logic for instance, limit, and
    // offset fields (similar to your previous implementation)
    // Validating the 'instance' field
    JsonObject errResponse = new JsonObject().put(STATUS, FAILED);
    if (request.containsKey(INSTANCE)) {
      String instance = request.getString("instance", "");
      if (instance != null && instance.length() > INSTANCE_SIZE) { // Example size check
        LOGGER.error("Error: The instance length has exceeded the limit");
        errResponse
            .put(TYPE, TYPE_INVALID_PROPERTY_VALUE)
            .put(DESC, "The max length of 'instance' should be " + INSTANCE_SIZE);
        handler.handle(Future.failedFuture(errResponse.encode()));
        return this;
      }
    }

    // Validating the 'limit' and 'offset' fields
    if (request.containsKey(LIMIT) || request.containsKey(OFFSET)) {
      Integer limit = request.getInteger(LIMIT, 0);
      Integer offset = request.getInteger(OFFSET, 0);
      int totalSize = limit + offset;

      if (totalSize <= 0 || totalSize > MAX_RESULT_WINDOW) { // Example max size check
        LOGGER.error("Error: The limit + offset param has exceeded the limit");
        errResponse
            .put(TYPE, TYPE_INVALID_PROPERTY_VALUE)
            .put(DESC, "The limit + offset should be between 1 to " + MAX_RESULT_WINDOW);
        handler.handle(Future.failedFuture(errResponse.encode()));
        return this;
      }
    }
    handler.handle(Future.succeededFuture(new JsonObject().put(STATUS, SUCCESS)));
    return this;
  }

  public ValidatorService validateSearchCriteria(JsonObject request,
                                                 Handler<AsyncResult<JsonObject>> handler) {
    JsonArray criteriaArray = request.getJsonArray(SEARCH_CRITERIA_KEY);
    List<Future<String>> validationFutures = new ArrayList<>();

    for (int i = 0; i < criteriaArray.size(); i++) {
      JsonObject criterion = criteriaArray.getJsonObject(i);
      String searchType = criterion.getString(SEARCH_TYPE);

      if (searchType == null || searchType.isBlank()) {
        JsonObject error = new JsonObject()
            .put(STATUS, FAILED)
            .put(TYPE, TYPE_INVALID_PROPERTY_VALUE)
            .put(DESC, "'searchType' is missing or empty in searchCriteria at index " + i);
        handler.handle(Future.failedFuture(error.encode()));
        return this;
      }

      Future<String> validationFuture;
      switch (searchType) {
        case TERM:
          validationFuture = termValidator.validateSearchCriteria(criterion.encode());
          break;

        case BETWEEN_RANGE:
        case BEFORE_RANGE:
        case AFTER_RANGE:
          validationFuture = rangeValidator.validateSearchCriteria(criterion.encode());
          break;

        case BETWEEN_TEMPORAL:
        case BEFORE_TEMPORAL:
        case AFTER_TEMPORAL:
          validationFuture = temporalValidator.validateSearchCriteria(criterion.encode());
          break;

        default:
          JsonObject error = new JsonObject()
              .put(STATUS, FAILED)
              .put(TYPE, TYPE_INVALID_PROPERTY_VALUE)
              .put(DESC, "Invalid searchType: " + searchType);
          handler.handle(Future.failedFuture(error.encode()));
          return this;
      }

      // Wrap each validation future to handle individual failure
      Future<String> wrappedFuture = validationFuture.recover(err -> {
        // Fail-fast on any individual failure
        JsonObject errorMsg = new JsonObject()
            .put(STATUS, FAILED)
            .put(TYPE, TYPE_INVALID_PROPERTY_VALUE)
            .put(DESC, err.getMessage());
        handler.handle(Future.failedFuture(errorMsg.encode()));
        return Future.failedFuture(err); // stop execution
      });

      validationFutures.add(wrappedFuture);
    }

    // All validations passed
    CompositeFuture.all(new ArrayList<>(validationFutures))
        .onSuccess(res -> {
          isValidSchema = Future.succeededFuture(SUCCESS);
          SearchQueryValidatorHelper.handleSearchCriteriaResult(isValidSchema, request, handler);
        });

    return this;
  }

  public ValidatorService validateGeoSearchQuery(JsonObject request,
                                                 Handler<AsyncResult<JsonObject>> handler) {
    isValidSchema = geoSearchQueryValidator.validateSearchCriteria(request.toString());

    SearchQueryValidatorHelper.handleGeoSearchValidationResult(isValidSchema, request, handler);
    return this;
  }

  public ValidatorService validateTextSearchQuery(JsonObject request,
                                                  Handler<AsyncResult<JsonObject>> handler) {
    isValidSchema = textSearchQueryValidator.validateSearchCriteria(request.toString());

    SearchQueryValidatorHelper.handleTextSearchValidationResult(isValidSchema, request, handler);
    return this;
  }

  public ValidatorService validateFilterSearchQuery(JsonObject request,
                                                    Handler<AsyncResult<JsonObject>> handler) {
    isValidSchema = filterSearchQueryValidator.validateSearchCriteria(request.toString());

    SearchQueryValidatorHelper.handleFilterSearchValidationResult(isValidSchema, request, handler);
    return this;
  }
}
