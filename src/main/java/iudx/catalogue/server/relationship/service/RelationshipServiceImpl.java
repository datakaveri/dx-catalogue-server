package iudx.catalogue.server.relationship.service;

import static iudx.catalogue.server.common.util.ResponseBuilderUtil.invalidParameterResp;
import static iudx.catalogue.server.common.util.ResponseBuilderUtil.invalidSearchError;
import static iudx.catalogue.server.common.util.ResponseBuilderUtil.itemNotFoundResponse;
import static iudx.catalogue.server.relationship.QueryModelUtil.createIdWildcardQueryModel;
import static iudx.catalogue.server.relationship.QueryModelUtil.createRelationshipQueryModel;
import static iudx.catalogue.server.relationship.QueryModelUtil.getResourceGroupsForRsQuery;
import static iudx.catalogue.server.relationship.QueryModelUtil.getRsForResourceGroupQuery;
import static iudx.catalogue.server.relationship.QueryModelUtil.listRelQuery;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.apiserver.item.service.ItemService;
import iudx.catalogue.server.common.util.DbResponseMessageBuilder;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import iudx.catalogue.server.database.elastic.util.QueryDecoder;
import iudx.catalogue.server.database.elastic.util.ResponseFilter;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RelationshipServiceImpl implements RelationshipService {
  private static final Logger LOGGER = LogManager.getLogger(RelationshipServiceImpl.class);
  private final ItemService itemService;
  private final String docIndex;
  private final QueryDecoder queryDecoder = new QueryDecoder();

  public RelationshipServiceImpl(ItemService itemService, String docIndex) {
    this.itemService = itemService;
    this.docIndex = docIndex;
  }

  private static boolean isInvalidRelForGivenItem(JsonObject request, String itemType) {
    if (request.getString(RELATIONSHIP).equalsIgnoreCase("resource")
        && itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE)) {
      return true;
    } else if (request.getString(RELATIONSHIP).equalsIgnoreCase("resourceGroup")
        && itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_GROUP)) {
      return true;
    } else if (request.getString(RELATIONSHIP).equalsIgnoreCase("provider")
        && itemType.equalsIgnoreCase(ITEM_TYPE_PROVIDER)) {
      return true;
    } else if (request.getString(RELATIONSHIP).equalsIgnoreCase("resourceServer")
        && itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_SERVER)) {
      return true;
    } else if (request.getString(RELATIONSHIP).equalsIgnoreCase("cos")
        && itemType.equalsIgnoreCase(ITEM_TYPE_COS)) {
      return true;
    } else if (request.getString(RELATIONSHIP).equalsIgnoreCase("all")
        && itemType.equalsIgnoreCase(ITEM_TYPE_COS)) {
      return true;
    }
    return false;
  }

  @Override
  public Future<JsonObject> listRelationship(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    QueryModel listRelQueryModel = listRelQuery(request.getString(ID));
    itemService.search(listRelQueryModel, ResponseFilter.SOURCE_ONLY)
        .onComplete(queryHandler -> {
          if (queryHandler.succeeded()) {
            DbResponseMessageBuilder responseMsg = queryHandler.result();
            if (responseMsg.getResponse().getInteger(TOTAL_HITS) == 0) {
              promise.fail(
                  itemNotFoundResponse("Item id given is not present"));
              return;
            }
            JsonObject relType = responseMsg.getResponse().getJsonArray(RESULTS).getJsonObject(0);
            Set<String> type = new HashSet<String>(relType.getJsonArray(TYPE).getList());
            type.retainAll(ITEM_TYPES);
            String itemType = type.toString().replaceAll("\\[", "")
                .replaceAll("\\]", "");
            LOGGER.debug("Info: itemType: " + itemType);
            relType.put("itemType", itemType);

            if (isInvalidRelForGivenItem(request, itemType)) {
              promise.fail(invalidSearchError());
              return;
            }

            if ((request.getString(RELATIONSHIP).equalsIgnoreCase(RESOURCE_SVR) ||
                request.getString(RELATIONSHIP).equalsIgnoreCase(ALL)) &&
                itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_GROUP)) {
              LOGGER.debug(relType);
              handleRsFetchForResourceGroup(request, promise, relType);
            } else if (request.getString(RELATIONSHIP).equalsIgnoreCase(RESOURCE_GRP) &&
                itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_SERVER)) {
              handleResourceGroupFetchForRs(request, promise, relType);
            } else {
              request.mergeIn(relType);
              QueryModel elasticQuery = new QueryModel();
              elasticQuery.setQueries(queryDecoder.listRelationshipQueryModel(request));
              elasticQuery.setLimit(MAX_LIMIT);
              LOGGER.debug("Info: Query constructed;" + elasticQuery.toJson());
              if (elasticQuery.getQueries() != null) {
                handleClientSearchAsync(promise, elasticQuery);
              } else {
                promise.fail(invalidSearchError());
              }
            }
          } else {
            LOGGER.error(queryHandler.cause().getMessage());
          }
        });
    return promise.future();
  }

  private void handleClientSearchAsync(Promise<JsonObject> promise, QueryModel queryModel) {
    itemService.search(queryModel, ResponseFilter.SOURCE_WITHOUT_EMBEDDINGS)
        .onComplete(
            res -> {
              if (res.failed()) {
                LOGGER.error("Fail: DB request has failed;" + res.cause());
                promise.fail(res.cause());
              } else {
                DbResponseMessageBuilder responseMsg = res.result();
                LOGGER.debug("Success: Successful DB Request");
                promise.complete(responseMsg.getResponse());
              }
            });
  }

  private void handleResourceGroupFetchForRs(JsonObject request, Promise<JsonObject> promise,
                                             JsonObject relType) {

    QueryModel queryModel = getResourceGroupsForRsQuery(relType.getString(ID));
    itemService.search(queryModel, ResponseFilter.SOURCE_WITHOUT_EMBEDDINGS)
        .onComplete(res -> {
          if (res.failed()) {
            LOGGER.error("Fail: Search failed;" + res.cause().getMessage());
            promise.fail(res.cause());
            return;
          }
          JsonArray serverResult = new JsonArray();
          res.result().getResponse()
              .getJsonArray(RESULTS)
              .forEach(serverResult::add);
          request.put("providerIds", serverResult);
          request.mergeIn(relType);

          QueryModel elasticQuery = new QueryModel();
          elasticQuery.setQueries(queryDecoder.listRelationshipQueryModel(request));
          LOGGER.debug("Info: Query constructed;" + elasticQuery.toJson());
          handleClientSearchAsync(promise, elasticQuery);
        });
    promise.future();
  }

  private void handleRsFetchForResourceGroup(JsonObject request,
                                             Promise<JsonObject> promise,
                                             JsonObject relType) {
    QueryModel queryModel = getRsForResourceGroupQuery(relType.getString(PROVIDER));
    itemService.search(queryModel, ResponseFilter.SOURCE_WITHOUT_EMBEDDINGS)
        .onComplete(
            serverSearch -> {
              if (serverSearch.succeeded() &&
                  serverSearch.result().getResponse().getInteger(TOTAL_HITS)!=0) {
                JsonObject serverResult =
                    serverSearch.result().getResponse().getJsonArray(RESULTS).getJsonObject(0);
                request.mergeIn(serverResult);
                request.mergeIn(relType);

                QueryModel elasticQuery = new QueryModel();
                elasticQuery.setQueries(queryDecoder.listRelationshipQueryModel(request));
                LOGGER.debug("Info: Query constructed;" + elasticQuery.toJson());
                handleClientSearchAsync(promise, elasticQuery);
              } else {
                promise.fail(itemNotFoundResponse("Resource Group for given item not found"));
              }
            });
  }

  @Override
  public Future<JsonObject> relSearch(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    QueryModel queryModel;
    /* Validating the request */
    if (request.containsKey(RELATIONSHIP) && request.containsKey(VALUE)) {

      /* parsing data parameters from the request */
      String relReq = request.getJsonArray(RELATIONSHIP).getString(0);
      if (relReq.contains(".")) {

        LOGGER.debug("Info: Reached relationship search dbServiceImpl");

        String typeValue = null;
        String[] relReqs = relReq.split("\\.", 2);
        String relReqsKey = relReqs[1];
        String relReqsValue = request.getJsonArray(VALUE).getJsonArray(0).getString(0);
        if (relReqs[0].equalsIgnoreCase(PROVIDER)) {
          typeValue = ITEM_TYPE_PROVIDER;

        } else if (relReqs[0].equalsIgnoreCase(RESOURCE)) {
          typeValue = ITEM_TYPE_RESOURCE;

        } else if (relReqs[0].equalsIgnoreCase(RESOURCE_GRP)) {
          typeValue = ITEM_TYPE_RESOURCE_GROUP;

        } else if (relReqs[0].equalsIgnoreCase(RESOURCE_SVR)) {
          typeValue = ITEM_TYPE_RESOURCE_SERVER;

        } else {
          LOGGER.error("Fail: Incorrect/missing query parameters");
          promise.fail(invalidParameterResp());
          return promise.future();
        }
        queryModel = createRelationshipQueryModel(typeValue, relReqsKey, relReqsValue);
      } else {
        LOGGER.error("Fail: Incorrect/missing query parameters");
        promise.fail(invalidParameterResp());
        return promise.future();
      }
      
      /* Initial db query to filter matching attributes */
      itemService.search(queryModel, ResponseFilter.IDS_ONLY)
          .onComplete(
              searchRes -> {
                if (searchRes.succeeded()) {
                  JsonArray ids = searchRes.result().getResponse().getJsonArray(RESULTS);
                  if (ids == null || ids.isEmpty()) {
                    promise.complete(searchRes.result().getResponse());
                    return;
                  }
                  /* checking the requests for limit attribute */
                  String limit = null;
                  String offset = null;
                  if (request.containsKey(LIMIT)) {
                    Integer sizeFilter = request.getInteger(LIMIT);
                    limit = sizeFilter.toString();
                  }

                  /* checking the requests for offset attribute */
                  if (request.containsKey(OFFSET)) {
                    Integer offsetFilter = request.getInteger(OFFSET);
                    offset = offsetFilter.toString();
                  }
                  QueryModel relSearchQueryModel = createIdWildcardQueryModel(ids, limit, offset);
                  /* db query to find the relationship to the initial query */
                  itemService.search(relSearchQueryModel, ResponseFilter.SOURCE_WITHOUT_EMBEDDINGS)
                      .onComplete(relSearchRes -> {
                            if (relSearchRes.succeeded()) {
                              promise.complete(relSearchRes.result().getResponse());
                            } else if (relSearchRes.failed()) {
                              LOGGER.error(
                                  "Fail: DB request has failed;"
                                      + relSearchRes.cause().getMessage());
                              promise.fail(relSearchRes.cause());
                            }
                          });
                } else {
                  LOGGER.error("Fail: DB request has failed;" + searchRes.cause().getMessage());
                  promise.fail(searchRes.cause());
                }
              });
    }
    return promise.future();
  }

}
