package iudx.catalogue.server.apiserver.item.service;

import static iudx.catalogue.server.apiserver.item.model.QueryModelUtil.checkQueryModel;
import static iudx.catalogue.server.apiserver.item.model.QueryModelUtil.createGetItemQuery;
import static iudx.catalogue.server.apiserver.item.model.QueryModelUtil.createItemExistenceQuery;
import static iudx.catalogue.server.apiserver.item.model.QueryModelUtil.createVerifyInstanceQuery;
import static iudx.catalogue.server.apiserver.stack.util.StackConstants.DOC_ID;
import static iudx.catalogue.server.auditing.util.Constants.ID;
import static iudx.catalogue.server.database.elastic.util.Constants.ERROR_DB_REQUEST;
import static iudx.catalogue.server.database.elastic.util.Constants.GEOSUMMARY_KEY;
import static iudx.catalogue.server.database.elastic.util.Constants.STATIC_DELAY_TIME;
import static iudx.catalogue.server.database.elastic.util.Constants.SUMMARY_KEY;
import static iudx.catalogue.server.database.elastic.util.Constants.WORD_VECTOR_KEY;
import static iudx.catalogue.server.util.Constants.DELETE;
import static iudx.catalogue.server.util.Constants.DETAIL_ID_NOT_FOUND;
import static iudx.catalogue.server.util.Constants.INSTANCE;
import static iudx.catalogue.server.util.Constants.INSTANCE_VERIFICATION_FAILED;
import static iudx.catalogue.server.util.Constants.REQUEST_DELETE;
import static iudx.catalogue.server.util.Constants.REQUEST_GET;
import static iudx.catalogue.server.util.Constants.REQUEST_POST;
import static iudx.catalogue.server.util.Constants.REQUEST_PUT;
import static iudx.catalogue.server.util.Constants.SOURCE;
import static iudx.catalogue.server.util.Constants.TITLE_ITEM_NOT_FOUND;
import static iudx.catalogue.server.util.Constants.TOTAL_HITS;
import static iudx.catalogue.server.util.Constants.TYPE_INTERNAL_SERVER_ERROR;
import static iudx.catalogue.server.util.Constants.UPDATE;

import com.hazelcast.jet.datamodel.Tuple2;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.apiserver.item.exception.ItemNotFoundException;
import iudx.catalogue.server.apiserver.item.model.Item;
import iudx.catalogue.server.common.util.DbResponseMessageBuilder;
import iudx.catalogue.server.database.elastic.model.ElasticsearchResponse;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
import iudx.catalogue.server.database.elastic.util.ResponseFilter;
import iudx.catalogue.server.database.util.Summarizer;
import iudx.catalogue.server.exceptions.DatabaseFailureException;
import iudx.catalogue.server.exceptions.DocNotFoundException;
import iudx.catalogue.server.exceptions.InternalServerException;
import iudx.catalogue.server.exceptions.InvalidSyntaxException;
import iudx.catalogue.server.exceptions.DocAlreadyExistsException;
import iudx.catalogue.server.exceptions.OperationNotAllowedException;
import iudx.catalogue.server.geocoding.service.GeocodingService;
import iudx.catalogue.server.nlpsearch.service.NLPSearchService;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ItemServiceImpl implements ItemService {
  private static final Logger LOGGER = LogManager.getLogger(ItemService.class);
  private final String index;
  private final ElasticsearchService esService;
  private NLPSearchService nlpService;
  private GeocodingService geoService;
  private boolean nlpPluggedIn = false;
  private boolean geoPluggedIn = false;

  public ItemServiceImpl(String index, ElasticsearchService esService) {
    this.index = index;
    this.esService = esService;
  }

  public ItemServiceImpl(String index, ElasticsearchService esService, GeocodingService geoService,
                         NLPSearchService nlpService) {
    this.index = index;
    this.esService = esService;
    this.geoService = geoService;
    this.nlpService = nlpService;
    nlpPluggedIn = true;
    geoPluggedIn = true;
  }


  @Override
  public Future<DbResponseMessageBuilder> search(String id, List<String> includes) {
    Promise<DbResponseMessageBuilder> promise = Promise.promise();
    QueryModel queryModel = createGetItemQuery(id, includes);
    search(queryModel, ResponseFilter.SOURCE_ONLY)
        .onComplete(res -> {
          if (res.failed()) {
            promise.fail(res.cause());
          } else {
            if (res.result().getResponse().getInteger(TOTAL_HITS) == 0) {
              promise.fail(new NoSuchElementException("Item not found"));
            } else {
              DbResponseMessageBuilder responseMsg = res.result();
              responseMsg.setDetail("Success: Item fetched Successfully");
              LOGGER.info("Success: Item retrieved;");
              promise.complete(responseMsg);
            }
          }
        });
    return promise.future();
  }
  @Override
  public Future<Item> create(Item item) {
    String id = item.getId().toString();
    String instance = item.toJson().getString(INSTANCE);
    if (id == null) {
      LOGGER.error("Fail: id not present in the request");
      return Future.failedFuture(new InvalidSyntaxException(DETAIL_ID_NOT_FOUND));
    }

    return addVectorAndGeographicInfoToItem(item.toJson())
        .onFailure(err -> LOGGER.error("Fail: Failed to process document - {}",
            err.getMessage()))
        .compose(updatedDoc -> {
          if (updatedDoc.containsKey(SUMMARY_KEY)) {
            LOGGER.debug("Info: Updating item summary; " + item.getSummary());
            item.setSummary(updatedDoc.getString(SUMMARY_KEY));
          }
          if (updatedDoc.containsKey(GEOSUMMARY_KEY)) {
            LOGGER.debug("Info: Updating item geo summary;");
            item.setGeoSummary(updatedDoc.getJsonObject(GEOSUMMARY_KEY));
          }
          if (updatedDoc.containsKey(WORD_VECTOR_KEY)) {
            LOGGER.debug("Info: Updating item word vector;");
            item.setWordVector(updatedDoc.getJsonArray(WORD_VECTOR_KEY));
          }
          LOGGER.debug("Info: Inserting item " + item.toJson());
          QueryModel query = createGetItemQuery(id, null);
          return verifyInstance(instance)
              .recover(err -> Future.failedFuture(new OperationNotAllowedException(id,
                  INSTANCE_VERIFICATION_FAILED, err.getLocalizedMessage())))
              .compose(
                  instanceExists ->
                      instanceExists
                          ? createItem(item, query, id)
                          : Future.failedFuture(new DatabaseFailureException(
                          id, "Fail: Instance doesn't exist/registered")));

        })
        .onSuccess(dbHandler -> LOGGER.info("Success: Item created"))
        .recover(err -> {
          if (err instanceof DocAlreadyExistsException existsException) {
            LOGGER.error("Item already exists with id {}, skipping creation",
                existsException.getItemId());
            return Future.failedFuture(existsException);
          } else if (err instanceof DatabaseFailureException dbException) {
            LOGGER.error("Fail: Search operation failed - {}", dbException.getMessage());
            return Future.failedFuture(dbException);
          } else {
            LOGGER.error("Fail: Item creation failed - {}", err.getMessage());
            return Future.failedFuture(err);
          }
        });
  }

  @Override
  public Future<Item> update(Item item) {
    Promise<Item> promise = Promise.promise();

    String id = item.getId().toString();
    String type = item.getType().getFirst();
    LOGGER.debug("Info: Updating item");

    QueryModel query = createItemExistenceQuery(id, type);
    updateItem(item, query)
        .onComplete(dbHandler -> {
          if (dbHandler.failed()) {
            LOGGER.error("Fail: Item update failed; " + dbHandler.cause().getMessage());
            if (dbHandler.cause() instanceof DocNotFoundException exception) {
              promise.fail(new ItemNotFoundException(exception.getId(), UPDATE));
            } else {
              promise.fail(dbHandler.cause());
            }
          } else {
            LOGGER.info("Success: Item updated;");
            promise.complete(dbHandler.result());
          }
        });

    return promise.future();
  }

  @Override
  public Future<String> delete(String id) {
    Promise<String> promise = Promise.promise();
    QueryModel query = checkQueryModel(id);

    deleteItem(id, query)
        .onComplete(dbHandler -> {
          if (dbHandler.succeeded()) {
            LOGGER.info("Success: Item deleted;");
            promise.complete(dbHandler.result());
          } else {
            Throwable cause = dbHandler.cause();
            if (cause instanceof DocNotFoundException exception) {
              LOGGER.error("Fail: Item not found; " + cause.getMessage());
              promise.fail(exception);
            } else if (cause instanceof OperationNotAllowedException exception) {
              LOGGER.debug("Operation not allowed");
              promise.fail(exception);
            } else {
              LOGGER.error("Fail: Item deletion failed; " + cause.getMessage());
              promise.fail(cause.getMessage());
            }
          }
        });

    return promise.future();
  }


  /**
   * Searches for items in the Elasticsearch index based on the provided query model and response filter.
   *
   * @param queryModel The query model defining search criteria.
   * @param filter The response filter determining the format of search results.
   * @return A Future containing the search results wrapped in a DbResponseMessageBuilder.
   */
  @Override
  public Future<DbResponseMessageBuilder> search(QueryModel queryModel, ResponseFilter filter) {
    LOGGER.debug("Info: Retrieving item");
    return esService
        .search(index, queryModel)
        .compose(res -> processSearchResponse(res, filter))
        .recover(
            err -> {
              LOGGER.error("Fail: Item retrieval failed; " + err.getMessage());
              return Future.failedFuture(new InternalServerException(err.getMessage(),
                  REQUEST_GET));
            });
  }
  private Future<DbResponseMessageBuilder> processSearchResponse(
      List<ElasticsearchResponse> response, ResponseFilter filter) {

    DbResponseMessageBuilder responseMsg = new DbResponseMessageBuilder()
        .statusSuccess().setTotalHits(response.size());

    switch (filter) {
      case SOURCE_ONLY -> response.forEach(res -> responseMsg.addResult(res.getSource()));
      case DOC_ID_ONLY -> response.stream()
          .map(elasticResponse -> new JsonObject().put("doc_id", elasticResponse.getDocId()))
          .forEach(responseMsg::addResult);
      case SOURCE_AND_DOC_ID -> response.stream()
          .map(elasticResponse -> new JsonObject(elasticResponse.getSource().toString())
              .put("doc_id", elasticResponse.getDocId()))
          .forEach(responseMsg::addResult);
      case SOURCE_AND__ID -> response.stream()
          .map(elasticResponse -> new JsonObject()
              .put(SOURCE, elasticResponse.getSource())
              .put(DOC_ID, elasticResponse.getDocId()))
          .forEach(responseMsg::addResult);
      case SOURCE_WITHOUT_EMBEDDINGS -> response.stream()
          .map(ElasticsearchResponse::getSource)
          .peek(source -> {
            source.remove(SUMMARY_KEY);
            source.remove(WORD_VECTOR_KEY);
          })
          .forEach(responseMsg::addResult);
      case IDS_ONLY -> {
        List<String> ids = extractIds(response);
        if (ids == null || ids.isEmpty()) {
          LOGGER.debug("Info: No IDs found in the response");
          responseMsg.addResult();
        } else {
          ids.forEach(responseMsg::addResult);
        }
      }
    }

    LOGGER.info("Success: Retrieved item");
    return Future.succeededFuture(responseMsg);
  }
  private List<String> extractIds(List<ElasticsearchResponse> response) {
    return response.stream()
        .map(ElasticsearchResponse::getSource)
        .filter(Objects::nonNull)
        .map(d -> d.getString(ID))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  /**
   * Creates a new item in the Elasticsearch index after checking if it already exists.
   *
   * @param item The item to be created.
   * @param checkItemExistenceQuery Query model to check if the item already exists.
   * @param id The unique identifier for the item.
   * @return A Future containing the created item.
   * @throws DocAlreadyExistsException If the item already exists.
   * @throws DatabaseFailureException If there is an issue with the database operation.
   */
  public Future<Item> createItem(Item item, QueryModel checkItemExistenceQuery, String id)
      throws DocAlreadyExistsException, DatabaseFailureException {
    JsonObject doc = item.toJson();
    LOGGER.debug("item json: " + doc);
    AtomicReference<String> itemId = new AtomicReference<>(id);

    return checkItemExists(index, checkItemExistenceQuery)
        .compose(result -> handleItemExistence(result, item))
        .compose(savedDoc -> insertDocumentWithDelay(index, savedDoc))
        .recover(
            err -> {
              LOGGER.error("Fail: Item creation failed; " + err.getMessage());
              if (err instanceof DocAlreadyExistsException) {
                LOGGER.debug("IAE exception");
                return Future.failedFuture(new DocAlreadyExistsException(itemId.get()));
              } else if (err.getMessage().contains(TYPE_INTERNAL_SERVER_ERROR)) {
                return Future.failedFuture(new InternalServerException(itemId.get(),
                    err.getMessage(), REQUEST_POST));
              }
              LOGGER.debug("DB exception");
              return Future.failedFuture(
                  new DatabaseFailureException(itemId.get(), err.getMessage()));
            });
  }
  private Future<Item> handleItemExistence(Tuple2<Boolean, String> result,
                                           Item item) {
    boolean exists = Boolean.TRUE.equals(result.f0());
    return exists ? Future.failedFuture(new DocAlreadyExistsException(result.f1()))
        : Future.succeededFuture(item);
  }
  private Future<Item> insertDocumentWithDelay(String index, Item item) {
    Promise<Item> promise = Promise.promise();
    new Timer().schedule(new TimerTask() {
      @Override
      public void run() {
        esService.createDocument(index, item.toJson())
            .onSuccess(res -> {
              LOGGER.info("Success: Item created; " + res);
              promise.complete(item);
            })
            .onFailure(err -> {
              LOGGER.error("Fail: Item creation failed; " + err.getMessage());
              promise.fail(TYPE_INTERNAL_SERVER_ERROR);
            });
      }
    }, STATIC_DELAY_TIME);
    return promise.future();
  }

  /**
   * Updates an existing item in the Elasticsearch index.
   *
   * @param item The item containing updated information.
   * @param checkItemExistenceQuery Query model to verify if the item exists.
   * @return A Future containing the updated item.
   */
  public Future<Item> updateItem(Item item, QueryModel checkItemExistenceQuery) {
    JsonObject doc = item.toJson();
    String id = doc.getString(ID);

    return esService
        .search(index, checkItemExistenceQuery)
        .compose(this::handleExistenceCheck)
        .compose(docId -> esService.updateDocument(index, docId, doc))
        .map(v -> item)
        .recover(
            err -> {
              if (err.getMessage().contains(TITLE_ITEM_NOT_FOUND)) {
                return Future.failedFuture(new DocNotFoundException(id, err.getMessage(), REQUEST_POST));
              }
              LOGGER.error("Fail: Item update failed; " + err.getMessage());
              return Future.failedFuture(new InternalServerException(err.getMessage(), REQUEST_PUT));
            });
  }
  private Future<String> handleExistenceCheck(List<ElasticsearchResponse> checkRes) {
    if (checkRes.isEmpty()) {
      LOGGER.error("Fail: Doc doesn't exist, can't update");
      return Future.failedFuture(TITLE_ITEM_NOT_FOUND);
    }
    return Future.succeededFuture(checkRes.getFirst().getDocId());
  }


  /**
   * Deletes an item from the Elasticsearch index based on its ID.
   *
   * @param id The unique identifier of the item to be deleted.
   * @param queryModel Query model to verify if the item exists.
   * @return A Future containing the ID of the deleted item.
   */
  public Future<String> deleteItem(String id, QueryModel queryModel) {

    return esService.search(index, queryModel)
        .compose(checkRes -> {
          if (checkRes.isEmpty()) {
            return Future.failedFuture(new DocNotFoundException(id, "Fail: Doc doesn't exist, " +
                "can't delete", DELETE));
          }
          if (checkRes.size() > 1) {
            return Future.failedFuture(new OperationNotAllowedException(id,
                "Fail: Can't delete, doc has associated item;"));
          }

          String docId = checkRes.getFirst().getDocId();
          return esService.deleteDocument(index, docId)
              .compose(deleteRes -> {
                LOGGER.info("Success: Item deleted;");
                return Future.succeededFuture(id);

              })
              .recover(err -> {
                LOGGER.error("Fail: Deletion failed; " + err.getMessage());
                return Future.failedFuture(
                    new InternalServerException(err.getMessage(), REQUEST_DELETE));
              });
        })
        .recover(err -> {
          LOGGER.error("Fail: Check query fail;" + err.getMessage());
          return Future.failedFuture(err);
        });
  }

  /**
   * Verifies whether a given instance exists in the Elasticsearch index.
   *
   * @param instanceId The ID of the instance to verify.
   * @return A Future containing a boolean indicating whether the instance exists.
   */
  private Future<Boolean> verifyInstance(String instanceId) {
    if (instanceId == null || instanceId.startsWith("\"") || instanceId.isBlank()) {
      LOGGER.debug("Info: InstanceID null. Maybe provider item");
      return Future.succeededFuture(true);
    }

    QueryModel checkInstanceQueryModel = createVerifyInstanceQuery(instanceId);
    return checkItemExists(index, checkInstanceQueryModel)
        .map(result -> {
          boolean exists = Boolean.TRUE.equals(result.f0());
          LOGGER.debug("Info: Instance " + (exists ? "exists." : "doesn't exist."));
          return exists;
        })
        .otherwise(err -> {
          LOGGER.error("Error verifying instance: " + err.getMessage());
          return false;  // Return false in case of failure
        });
  }

  /**
   * Counts the number of items matching a given query model in the Elasticsearch index.
   *
   * @param queryModel The query model defining count criteria.
   * @return A Future containing a JsonObject with the count result.
   */
  @Override
  public Future<DbResponseMessageBuilder> count(QueryModel queryModel) {
    Promise<DbResponseMessageBuilder> promise = Promise.promise();
    LOGGER.debug("Info: Query constructed;" + queryModel.toString());
    esService
        .count(index, queryModel)
        .onComplete(
            searchRes -> {
              if (searchRes.succeeded()) {
                LOGGER.debug("Success: Successful DB request");
                DbResponseMessageBuilder responseMsg = new DbResponseMessageBuilder();
                responseMsg.statusSuccess().setTotalHits(searchRes.result());
                promise.complete(responseMsg);
              } else {
                LOGGER.error("Fail: DB Request;" + searchRes.cause().getMessage());
                promise.fail(new InternalServerException(searchRes.cause().getMessage(),
                    REQUEST_GET));
              }
            });
    return promise.future();
  }

  /**
   * Checks if an item exists in the Elasticsearch index based on a given query model.
   *
   * @param index The index to search in.
   * @param queryModel The query model defining search criteria.
   * @return A Future containing a Tuple2<Boolean, String> where the first value indicates existence
   *         and the second value contains the item ID if found.
   */
  private Future<Tuple2<Boolean, String>> checkItemExists(String index, QueryModel queryModel) {
    Promise<Tuple2<Boolean, String>> promise = Promise.promise();
    esService.search(index, queryModel)
        .onComplete(dbHandler -> {
          if (dbHandler.succeeded()) {
            List<ElasticsearchResponse> response = dbHandler.result();
            if (response.isEmpty()) {
              LOGGER.debug("Item doesn't exist");
              promise.complete(Tuple2.tuple2(false, null));
            } else {
              LOGGER.debug("Item exists");
              String id= dbHandler.result().getFirst().getSource().getString(ID);
              promise.complete(Tuple2.tuple2(true, id));
            }
          } else {
            LOGGER.error(ERROR_DB_REQUEST + dbHandler.cause().getMessage());
            promise.fail(TYPE_INTERNAL_SERVER_ERROR);
          }
        });
    return promise.future();
  }

  private Future<JsonObject> addVectorAndGeographicInfoToItem(JsonObject doc) {
    doc.put(SUMMARY_KEY, Summarizer.summarize(doc));
    String instanceId = doc.getString(INSTANCE);

    /* Check if geo and nlp services are initialized */
    if (geoPluggedIn && nlpPluggedIn &&
        !(instanceId == null || instanceId.isBlank() || instanceId.isEmpty())) {
      return geoService.geoSummarize(doc)
          .recover(err -> {
            LOGGER.debug("No geocoding result generated");
            return Future.succeededFuture("{}"); // Return empty JSON string if failed
          })
          .map(JsonObject::new)
          .compose(geoResult -> {
            LOGGER.debug("GeoHandler result: " + geoResult);
            doc.put(GEOSUMMARY_KEY, geoResult);
            return nlpService.getEmbedding(doc);
          })
          .compose(embeddingRes -> {
            LOGGER.debug("Info: Document embeddings created");
            doc.put(WORD_VECTOR_KEY, embeddingRes.getJsonArray("result"));
            return Future.succeededFuture(doc);
          })
          .otherwise(err -> {
            LOGGER.error("Error: Document embeddings not created");
            return doc;  // Return the document even if embeddings fail
          });
    } else {
      return Future.succeededFuture(doc);
    }
  }

}

