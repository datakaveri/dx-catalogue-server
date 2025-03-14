package iudx.catalogue.server.apiserver.item.service;

import static iudx.catalogue.server.common.util.ResponseBuilderUtil.*;
import static iudx.catalogue.server.database.elastic.util.BoolOperator.*;
import static iudx.catalogue.server.database.elastic.util.Constants.*;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.util.Constants.KEYWORD_KEY;
import static iudx.catalogue.server.validator.util.Constants.CONTEXT;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.apiserver.item.model.Item;
import iudx.catalogue.server.common.RespBuilder;
import iudx.catalogue.server.common.util.DbResponseMessageBuilder;
import iudx.catalogue.server.database.elastic.model.ElasticsearchResponse;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
import iudx.catalogue.server.database.elastic.util.QueryType;
import iudx.catalogue.server.database.util.Summarizer;
import iudx.catalogue.server.geocoding.service.GeocodingService;
import iudx.catalogue.server.nlpsearch.service.NLPSearchService;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ItemServiceImpl implements ItemService {
  private static final Logger LOGGER = LogManager.getLogger(ItemServiceImpl.class);
  protected final ElasticsearchService esService;
  private final JsonObject config;
  private NLPSearchService nlpService;
  private GeocodingService geoService;
  private boolean nlpPluggedIn = false;
  private boolean geoPluggedIn = false;

  public ItemServiceImpl(
      ElasticsearchService esService,
      GeocodingService geoService,
      NLPSearchService nlpService,
      JsonObject config) {
    this.esService = esService;
    this.config = config;
    this.geoService = geoService;
    this.nlpService = nlpService;
    nlpPluggedIn = true;
    geoPluggedIn = true;
  }

  public ItemServiceImpl(ElasticsearchService esService, JsonObject config) {
    this.esService = esService;
    this.config = config;
  }

  private static QueryModel checkQueryModel(String id) {
    Map<String, Object> idParams = Map.of(FIELD, ID_KEYWORD, VALUE, id);
    Map<String, Object> resourceGroupParams = Map.of(FIELD, RESOURCE_GRP + KEYWORD_KEY, VALUE, id);
    Map<String, Object> providerParams = Map.of(FIELD, PROVIDER + KEYWORD_KEY, VALUE, id);
    Map<String, Object> resourceServerParams = Map.of(FIELD, RESOURCE_SVR + KEYWORD_KEY, VALUE, id);
    Map<String, Object> cosParams = Map.of(FIELD, COS + KEYWORD_KEY, VALUE, id);

    QueryModel idQuery = new QueryModel(QueryType.TERM, idParams);
    QueryModel resourceGroupQuery = new QueryModel(QueryType.TERM, resourceGroupParams);
    QueryModel providerQuery = new QueryModel(QueryType.TERM, providerParams);
    QueryModel resourceServerQuery = new QueryModel(QueryType.TERM, resourceServerParams);
    QueryModel cosQuery = new QueryModel(QueryType.TERM, cosParams);

    List<QueryModel> shouldQueries =
        List.of(idQuery, resourceGroupQuery, providerQuery, resourceServerQuery, cosQuery);

    return new QueryModel(SHOULD, shouldQueries);
  }

  @Override
  public Future<JsonObject> createItem(Item item) {
    Promise<JsonObject> promise = Promise.promise();

    JsonObject doc = item.toJson();
    String itemType = doc.getJsonArray(TYPE).getString(0);
    if (!Objects.equals(itemType, ITEM_TYPE_INSTANCE)) {
      doc.put(CONTEXT, config.getString(CONTEXT));
    }
    String id = doc.getString(ID);
    if (id == null) {
      LOGGER.error("Fail: id not present in the request");
      promise.fail(invalidSyntaxResponse(DETAIL_ID_NOT_FOUND));
      return promise.future();
    }

    final String instanceId = doc.getString(INSTANCE);
    Map<String, Object> termParams = new HashMap<>();
    termParams.put(FIELD, ID_KEYWORD);
    termParams.put(VALUE, id);
    QueryModel checkItemQueryModel = new QueryModel();
    checkItemQueryModel.setQueries(new QueryModel(QueryType.TERM, termParams));
    checkItemQueryModel.setLimit(MAX_LIMIT);
    checkItemQueryModel.setOffset(FILTER_PAGINATION_FROM);

    verifyInstance(instanceId)
        .compose(
            instanceExists -> {
              if (!instanceExists) {
                LOGGER.debug(INSTANCE_NOT_EXISTS);
                return Future.failedFuture("Fail: Instance doesn't exist/registered");
              }
              return Future.succeededFuture(doc);
            })
        .compose(v -> checkItemExists(checkItemQueryModel))
        .compose(
            exists -> {
              if (exists) {
                // Early exit to avoid further processing
                LOGGER.debug("Item already exists");
                return Future.failedFuture("Item already exists");
              }
              return Future.succeededFuture(doc);
            })
        .compose(this::addVectorAndGeographicInfoToItem)
        .onSuccess(
            document ->
                promise.complete(successfulItemOperationResp(document, "Success: Item created")))
        .onFailure(
            err -> {
              if ("Item already exists".equals(err.getMessage())) {
                LOGGER.error("Fail: Item exists; ID: " + id);
                promise.fail(itemAlreadyExistsResponse(id, "Fail: Doc Exists"));
              } else {
                LOGGER.error("Fail: Item creation failed; " + err.getMessage());
                promise.fail(
                    operationNotAllowedResponse(
                        doc.getString(ID), INSERT, err.getLocalizedMessage()));
              }
            });

    return promise.future();
  }

  @Override
  public Future<JsonObject> updateItem(Item item) {
    JsonObject doc = item.toJson();
    doc.put(CONTEXT, config.getString(CONTEXT));
    String id = doc.getString(ID);
    String type = doc.getJsonArray(TYPE).getString(0);
    String index = config.getString(DOC_INDEX);

    QueryModel queryModel = new QueryModel();
    QueryModel idTermQuery = new QueryModel(QueryType.TERM, Map.of(FIELD, ID_KEYWORD, VALUE, id));
    QueryModel typeMatchQuery =
        new QueryModel(QueryType.MATCH, Map.of(FIELD, TYPE_KEYWORD, VALUE, type));
    QueryModel checkItemExistenceQuery = new QueryModel(MUST, List.of(idTermQuery, typeMatchQuery));
    queryModel.setQueries(checkItemExistenceQuery);

    // Set the source configuration to include specified fields
    queryModel.setIncludeFields(List.of(ID));
    Promise<JsonObject> promise = Promise.promise();
    new Timer()
        .schedule(
            new TimerTask() {
              public void run() {
                esService
                    .search(index, queryModel)
                    .onComplete(
                        checkRes -> {
                          if (checkRes.failed()) {
                            LOGGER.error("Fail: Check query fail;" + checkRes.cause());
                            promise.fail(internalErrorResp());
                            return;
                          }
                          if (checkRes.succeeded()) {
                            if (checkRes.result().size() != 1) {
                              LOGGER.error("Fail: Doc doesn't exist, can't update");
                              promise.fail(
                                  itemNotFoundResponse(
                                      id, UPDATE, "Fail: Doc doesn't exist, can't update"));
                              return;
                            }
                            String docId = checkRes.result().get(0).getDocId();
                            esService
                                .updateDocument(index, docId, doc)
                                .onComplete(
                                    dbHandler -> {
                                      if (dbHandler.failed()) {
                                        LOGGER.error(
                                            "Fail: Item update failed; "
                                                + dbHandler.cause().getMessage());
                                        promise.fail(internalErrorResp());
                                      } else {
                                        LOGGER.info("Success: Item updated;");
                                        promise.complete(
                                            successfulItemOperationResp(
                                                doc, "Success: Item updated successfully"));
                                      }
                                    });
                          }
                        });
              }
            },
            STATIC_DELAY_TIME);
    return promise.future();
  }

  @Override
  public Future<JsonObject> deleteItem(String id) {
    Promise<JsonObject> promise = Promise.promise();

    new Timer()
        .schedule(
            new TimerTask() {
              public void run() {
                /* the check query checks if any type item is present more than once.
                If it's present then the item cannot be deleted.  */
                String index = config.getString(DOC_INDEX);

                QueryModel queryResourceGrp = new QueryModel();
                queryResourceGrp.setQueries(checkQueryModel(id));

                esService
                    .search(index, queryResourceGrp)
                    .onComplete(
                        checkRes -> {
                          if (checkRes.failed()) {
                            LOGGER.error("Fail: Check query fail;" + checkRes.cause().getMessage());
                            promise.fail(internalErrorResp());
                          } else {
                            LOGGER.debug("Success: Check index for doc");
                            if (checkRes.result().size() > 1) {
                              LOGGER.error("Fail: Can't delete, doc has associated item;");
                              promise.fail(
                                  operationNotAllowedResponse(
                                      id, "Fail: Can't delete, doc has associated item"));
                              return;
                            } else if (checkRes.result().isEmpty()) {
                              LOGGER.error("Fail: Doc doesn't exist, can't delete;");
                              promise.fail(
                                  itemNotFoundResponse(
                                      id, "Fail: Doc doesn't exist, can't delete"));
                              return;
                            }
                            String docId = checkRes.result().get(0).getDocId();
                            esService
                                .deleteDocument(index, docId)
                                .onComplete(
                                    dbHandler -> {
                                      if (dbHandler.succeeded()) {
                                        JsonObject response = dbHandler.result();
                                        LOGGER.info("Success: Item deleted;");
                                        if (TITLE_SUCCESS.equals(response.getString(TITLE))) {
                                          promise.complete(
                                              successResp(
                                                  id, "Success: Item deleted successfully"));
                                        } else {
                                          promise.fail(
                                              new NoSuchElementException(
                                                  "Fail: Doc doesn't exist, "
                                                      + "can't perform operation"));
                                        }
                                      } else {
                                        Throwable cause = dbHandler.cause();
                                        LOGGER.error("Fail: Deletion failed;" + cause);
                                        promise.fail(internalErrorResp());
                                      }
                                    });
                          }
                        });
              }
            },
            STATIC_DELAY_TIME);

    return promise.future();
  }

  @Override
  public Future<JsonObject> getItem(JsonObject requestBody) {

    List<String> includeFields = null;
    if (requestBody.containsKey(INCLUDE_FIELDS)) {
      includeFields =
          requestBody.getJsonArray(INCLUDE_FIELDS).stream()
              .map(Object::toString)
              .collect(Collectors.toList());
    }
    String id = requestBody.getString(ID);
    Map<String, Object> termParams = new HashMap<>();
    termParams.put(FIELD, ID_KEYWORD); // Field to match
    termParams.put(VALUE, id); // The ID value to search for
    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(new QueryModel(QueryType.TERM, termParams));
    if (includeFields != null) {
      queryModel.setIncludeFields(includeFields);
    }
    queryModel.setLimit(MAX_LIMIT);
    queryModel.setOffset(FILTER_PAGINATION_FROM);

    LOGGER.debug("Info: Retrieving item");
    Promise<JsonObject> promise = Promise.promise();
    String index = config.getString(DOC_INDEX);
    esService
        .search(index, queryModel)
        .onComplete(
            dbHandler -> {
              if (dbHandler.succeeded()) {
                List<ElasticsearchResponse> response = dbHandler.result();
                if (response.isEmpty()) {
                  LOGGER.error(new NoSuchElementException("Item not found"));
                  promise.fail(
                      new RespBuilder()
                          .withType(TYPE_ITEM_NOT_FOUND)
                          .withTitle(TITLE_ITEM_NOT_FOUND)
                          .withDetail("Fail: Doc doesn't exist, can't perform operation")
                          .getResponse());
                } else {
                  DbResponseMessageBuilder responseMsg = new DbResponseMessageBuilder();
                  responseMsg.statusSuccess().setTotalHits(response.size());
                  response.stream()
                      .map(ElasticsearchResponse::getSource)
                      .forEach(responseMsg::addResult);
                  responseMsg.setDetail("Success: Item fetched Successfully");
                  LOGGER.info("Success: Retrieved item");
                  promise.complete(responseMsg.getResponse());
                }
              } else {
                LOGGER.error("Fail: Item retrieval failed; " + dbHandler.cause().getMessage());
                promise.fail(
                    new RespBuilder()
                        .withType(TYPE_INTERNAL_SERVER_ERROR)
                        .withTitle(TITLE_INTERNAL_SERVER_ERROR)
                        .withDetail(DETAIL_INTERNAL_SERVER_ERROR)
                        .getResponse());
              }
            });
    return promise.future();
  }

  private Future<Boolean> checkItemExists(QueryModel queryModel) {
    Promise<Boolean> promise = Promise.promise();

    String index = config.getString(DOC_INDEX);
    esService
        .search(index, queryModel)
        .onComplete(
            dbHandler -> {
              if (dbHandler.succeeded()) {
                List<ElasticsearchResponse> response = dbHandler.result();
                if (response.isEmpty()) {
                  LOGGER.debug("Item doesn't exist");
                  promise.complete(false);
                } else {
                  LOGGER.debug("Item exists");
                  promise.complete(true);
                }
              } else {
                LOGGER.error(ERROR_DB_REQUEST + dbHandler.cause().getMessage());
                promise.fail(TYPE_INTERNAL_SERVER_ERROR);
              }
            });
    return promise.future();
  }

  private Future<JsonObject> addVectorAndGeographicInfoToItem(JsonObject doc) {
    Promise<JsonObject> promise = Promise.promise();
    doc.put(SUMMARY_KEY, Summarizer.summarize(doc));
    String instanceId = doc.getString(INSTANCE);

    /* If geo and nlp services are initialized */
    if (geoPluggedIn
        && nlpPluggedIn
        && !(instanceId == null || instanceId.isBlank() || instanceId.isEmpty())) {
      geoService
          .geoSummarize(doc)
          .onComplete(
              geoHandler -> {
                /* Not going to check if success or fail */
                JsonObject geoResult;
                try {
                  geoResult = new JsonObject(geoHandler.result());
                  LOGGER.debug("GeoHandler result: " + geoResult);
                } catch (Exception e) {
                  LOGGER.debug("no geocoding result generated");
                  geoResult = new JsonObject();
                }
                doc.put(GEOSUMMARY_KEY, geoResult);
                nlpService
                    .getEmbedding(doc)
                    .onComplete(
                        ar -> {
                          if (ar.succeeded()) {
                            LOGGER.debug("Info: Document embeddings created");
                            doc.put(WORD_VECTOR_KEY, ar.result().getJsonArray("result"));
                            /* Insert document */
                            new Timer()
                                .schedule(
                                    new TimerTask() {
                                      public void run() {
                                        esService
                                            .createDocument(config.getString(DOC_INDEX), doc)
                                            .onComplete(
                                                dbHandler -> {
                                                  if (dbHandler.failed()) {
                                                    LOGGER.error(
                                                        "Fail: Item creation failed; "
                                                            + dbHandler.cause().getMessage());
                                                    promise.fail(
                                                        dbHandler.cause().getLocalizedMessage());
                                                  } else {
                                                    LOGGER.info("Success: Item created;");
                                                    promise.complete(doc);
                                                  }
                                                });
                                      }
                                    },
                                    STATIC_DELAY_TIME);
                          } else {
                            LOGGER.error("Error: Document embeddings not created");
                          }
                        });
              });
    } else {
      /* Insert document */
      new Timer()
          .schedule(
              new TimerTask() {
                public void run() {
                  esService
                      .createDocument(config.getString(DOC_INDEX), doc)
                      .onComplete(
                          dbHandler -> {
                            if (dbHandler.failed()) {
                              LOGGER.error(
                                  "Fail: Item creation failed; " + dbHandler.cause().getMessage());
                              promise.fail(dbHandler.cause().getLocalizedMessage());
                            } else {
                              LOGGER.info("Success: Item created;");
                              promise.complete(doc);
                            }
                          });
                }
              },
              STATIC_DELAY_TIME);
    }
    return promise.future();
  }

  /* Verify the existence of an instance */
  public Future<Boolean> verifyInstance(String instanceId) {
    Promise<Boolean> promise = Promise.promise();

    if (instanceId == null || instanceId.startsWith("\"") || instanceId.isBlank()) {
      LOGGER.debug("Info: InstanceID null. Maybe provider item");
      promise.complete(true);
      return promise.future();
    }
    Map<String, Object> termParams = new HashMap<>();
    termParams.put(FIELD, ID);
    termParams.put(VALUE, instanceId);
    QueryModel checkInstanceQueryModel = new QueryModel();
    checkInstanceQueryModel.setQueries(new QueryModel(QueryType.MATCH, termParams));
    checkInstanceQueryModel.setLimit(MAX_LIMIT);
    checkInstanceQueryModel.setOffset(FILTER_PAGINATION_FROM);
    checkItemExists(checkInstanceQueryModel)
        .onSuccess(
            isItemExists -> {
              if (isItemExists) {
                LOGGER.debug("Info: Instance exists.");
                promise.complete(true);
              } else {
                LOGGER.debug("Info: Instance doesn't exist.");
                promise.complete(false);
              }
            })
        .onFailure(
            checkRes -> {
              promise.fail(checkRes.getLocalizedMessage());
            });
    return promise.future();
  }
}
