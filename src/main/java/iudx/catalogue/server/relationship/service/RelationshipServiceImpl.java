package iudx.catalogue.server.relationship.service;

import static iudx.catalogue.server.common.util.ResponseBuilderUtil.internalErrorResp;
import static iudx.catalogue.server.common.util.ResponseBuilderUtil.invalidParameterResp;
import static iudx.catalogue.server.common.util.ResponseBuilderUtil.invalidSearchError;
import static iudx.catalogue.server.common.util.ResponseBuilderUtil.itemNotFoundResponse;
import static iudx.catalogue.server.database.elastic.util.Constants.ID_KEYWORD;
import static iudx.catalogue.server.database.elastic.util.Constants.KEYWORD_KEY;
import static iudx.catalogue.server.database.elastic.util.Constants.SUMMARY_KEY;
import static iudx.catalogue.server.database.elastic.util.Constants.TYPE_KEYWORD;
import static iudx.catalogue.server.database.elastic.util.Constants.WORD_VECTOR_KEY;
import static iudx.catalogue.server.util.Constants.ALL;
import static iudx.catalogue.server.util.Constants.FIELD;
import static iudx.catalogue.server.util.Constants.ID;
import static iudx.catalogue.server.util.Constants.ITEM_TYPES;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_COS;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_PROVIDER;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_RESOURCE;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_RESOURCE_GROUP;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_RESOURCE_SERVER;
import static iudx.catalogue.server.util.Constants.LIMIT;
import static iudx.catalogue.server.util.Constants.MAX_LIMIT;
import static iudx.catalogue.server.util.Constants.OFFSET;
import static iudx.catalogue.server.util.Constants.PROVIDER;
import static iudx.catalogue.server.util.Constants.RELATIONSHIP;
import static iudx.catalogue.server.util.Constants.RESOURCE;
import static iudx.catalogue.server.util.Constants.RESOURCE_GRP;
import static iudx.catalogue.server.util.Constants.RESOURCE_SVR;
import static iudx.catalogue.server.util.Constants.RESULTS;
import static iudx.catalogue.server.util.Constants.TYPE;
import static iudx.catalogue.server.util.Constants.VALUE;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.common.util.DbResponseMessageBuilder;
import iudx.catalogue.server.database.elastic.model.ElasticsearchResponse;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
import iudx.catalogue.server.database.elastic.util.QueryDecoder;
import iudx.catalogue.server.database.elastic.util.QueryType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RelationshipServiceImpl implements RelationshipService {
  private static final Logger LOGGER = LogManager.getLogger(RelationshipServiceImpl.class);
  private final ElasticsearchService esService;
  private final String docIndex;
  private final QueryDecoder queryDecoder = new QueryDecoder();

  public RelationshipServiceImpl(ElasticsearchService esService, String docIndex) {
    this.esService = esService;
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
    QueryModel termsQuery = new QueryModel(QueryType.TERMS);
    Map<String, Object> termsParameters = new HashMap<>();
    termsParameters.put(FIELD, ID_KEYWORD);
    termsParameters.put(VALUE, request.getString(ID));
    termsQuery.setQueryParameters(termsParameters);

    QueryModel typeQuery = new QueryModel(QueryType.BOOL);
    typeQuery.setFilterQueries(List.of(termsQuery));
    List<String> sourceFields =
        List.of("cos", "resourceServer", "type", "provider", "resourceGroup", "id");
    QueryModel queryModel = new QueryModel();
    queryModel.setIncludeFields(sourceFields);
    queryModel.setQueries(typeQuery);

    Promise<JsonObject> promise = Promise.promise();
    esService.search(docIndex, queryModel).onComplete(queryHandler -> {
      if (queryHandler.succeeded()) {
        if (queryHandler.result().isEmpty()) {
          promise.fail(
              itemNotFoundResponse("Item id given is not present"));
          return;
        }
        JsonObject relType = queryHandler.result().get(0).getSource();

        Set<String> type = new HashSet<String>(relType.getJsonArray(TYPE).getList());
        type.retainAll(ITEM_TYPES);
        String itemType = type.toString().replaceAll("\\[", "").replaceAll("\\]", "");
        LOGGER.debug("Info: itemType: " + itemType);
        relType.put("itemType", itemType);

        if (isInvalidRelForGivenItem(request, itemType)) {
          promise.fail(invalidSearchError());
          return;
        }

        if ((request.getString(RELATIONSHIP).equalsIgnoreCase(RESOURCE_SVR)
            || request.getString(RELATIONSHIP).equalsIgnoreCase(ALL))
            && itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_GROUP)) {
          LOGGER.debug(relType);
          handleRsFetchForResourceGroup(request, promise, relType);
        } else if (request.getString(RELATIONSHIP).equalsIgnoreCase(RESOURCE_GRP)
            && itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_SERVER)) {
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

  private void handleClientSearchAsync(
      Promise<JsonObject> promise, QueryModel elasticQuery) {
    esService.search(docIndex, elasticQuery)
        .onComplete(searchRes -> {
          if (searchRes.succeeded()) {
            LOGGER.debug("Success: Successful DB request");
            List<ElasticsearchResponse> response = searchRes.result();
            DbResponseMessageBuilder responseMsg = new DbResponseMessageBuilder();
            responseMsg.statusSuccess().setTotalHits(response.size());
            response.stream()
                .map(ElasticsearchResponse::getSource)
                .peek(source -> {
                  source.remove(SUMMARY_KEY);
                  source.remove(WORD_VECTOR_KEY);
                })
                .forEach(responseMsg::addResult);

            promise.complete(responseMsg.getResponse());
          } else {
            LOGGER.error("Fail: DB request has failed;" + searchRes.cause());
            /* Handle request error */
            promise.fail(internalErrorResp());
          }
        });
  }

  private void handleResourceGroupFetchForRs(
      JsonObject request,
      Promise<JsonObject> promise,
      JsonObject relType) {
    QueryModel queryModel = new QueryModel();

    QueryModel typeQueryModel4RsGroup = new QueryModel(QueryType.BOOL);
    typeQueryModel4RsGroup.addMustQuery(new QueryModel(QueryType.MATCH,
        Map.of(FIELD, RESOURCE_SVR + KEYWORD_KEY, VALUE, relType.getString(ID))));
    typeQueryModel4RsGroup.addMustQuery(new QueryModel(QueryType.TERM,
        Map.of(FIELD, TYPE_KEYWORD, VALUE, ITEM_TYPE_PROVIDER)));

    queryModel.setQueries(typeQueryModel4RsGroup);
    queryModel.setIncludeFields(List.of(ID));
    queryModel.setLimit(MAX_LIMIT);

    LOGGER.debug("INFO: typeQueryModel4RsGroup build");

    esService.search(docIndex, queryModel)
        .onComplete(
            serverSearch -> {
              if (serverSearch.failed()) {
                LOGGER.error("Fail: Search failed;" + serverSearch.cause().getMessage());
                promise.fail(internalErrorResp());
                return;
              }
              JsonArray serverResult = new JsonArray();
              serverSearch.result().stream()
                  .map(ElasticsearchResponse::getSource)
                  .peek(source -> {
                    source.remove(SUMMARY_KEY);
                    source.remove(WORD_VECTOR_KEY);
                  })
                  .forEach(serverResult::add);

              request.put("providerIds", serverResult);
              request.mergeIn(relType);
              QueryModel elasticQuery = new QueryModel();
              elasticQuery.setQueries(queryDecoder.listRelationshipQueryModel(request));

              LOGGER.debug("Info: QueryModel build;" + elasticQuery.toJson());

              handleClientSearchAsync(promise, elasticQuery);
            });
    promise.future();
  }

  private void handleRsFetchForResourceGroup(JsonObject request,
                                             Promise<JsonObject> promise,
                                             JsonObject relType) {
    QueryModel queryModel = new QueryModel();

    QueryModel typeQueryModel4RsServer = new QueryModel(QueryType.BOOL);
    typeQueryModel4RsServer.addFilterQuery(new QueryModel(QueryType.TERMS,
        Map.of(FIELD, ID_KEYWORD, VALUE, List.of(relType.getString(PROVIDER)))));
    typeQueryModel4RsServer.setIncludeFields(List.of("cos", "resourceServer", "type", "provider",
        "resourceGroup", "id"));

    queryModel.setQueries(typeQueryModel4RsServer);
    LOGGER.debug("INFO: typeQueryModel4RsServer build");

    esService.search(docIndex, queryModel)
        .onComplete(
            serverSearch -> {
              if (serverSearch.succeeded() && !serverSearch.result().isEmpty()) {
                JsonObject serverResult = serverSearch.result().get(0).getSource();
                serverResult.remove(SUMMARY_KEY);
                serverResult.remove(WORD_VECTOR_KEY);
                request.mergeIn(serverResult);
                request.mergeIn(relType);
                QueryModel elasticQuery = new QueryModel();
                elasticQuery.setQueries(queryDecoder.listRelationshipQueryModel(request));

                LOGGER.debug("Info: Query constructed;" + elasticQuery);

                if (elasticQuery.getQueries() != null) {
                  handleClientSearchAsync(promise, elasticQuery);
                } else {
                  promise.fail(invalidSearchError());
                }
              } else {
                promise.fail(itemNotFoundResponse("Resource Group for given item not found"));
              }
            });
  }

  @Override
  public Future<JsonObject> relSearch(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();

    QueryModel queryModel = new QueryModel();
    QueryModel subQueryModel = new QueryModel(QueryType.BOOL);

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

        subQueryModel.addMustQuery(
            new QueryModel(QueryType.TERM, Map.of(FIELD, TYPE_KEYWORD, VALUE, typeValue)));
        subQueryModel.addMustQuery(new QueryModel(QueryType.MATCH, Map.of(FIELD, relReqsKey, VALUE,
            relReqsValue)));
      } else {
        LOGGER.error("Fail: Incorrect/missing query parameters");
        promise.fail(invalidParameterResp());
        return promise.future();
      }

      queryModel.setQueries(subQueryModel);
      queryModel.setIncludeFields(List.of(ID));

      /* Initial db query to filter matching attributes */
      esService.search(docIndex, queryModel)
          .onComplete(
              searchRes -> {
                if (searchRes.succeeded()) {

                  List<ElasticsearchResponse> response = searchRes.result();
                  DbResponseMessageBuilder responseMsg = new DbResponseMessageBuilder();
                  responseMsg.statusSuccess().setTotalHits(response.size());
                  response.stream()
                      .map(ElasticsearchResponse::getSource)
                      .peek(source -> {
                        source.remove(SUMMARY_KEY);
                        source.remove(WORD_VECTOR_KEY);
                      })
                      .forEach(responseMsg::addResult);

                  JsonArray resultValues = responseMsg.getResponse().getJsonArray(RESULTS);
                  QueryModel esQueryModel = new QueryModel();
                  QueryModel idCollectionModel = new QueryModel(QueryType.BOOL);

                  /* iterating over the filtered response json array */
                  if (!resultValues.isEmpty()) {

                    for (Object idIndex : resultValues) {
                      JsonObject id = (JsonObject) idIndex;
                      if (!id.isEmpty()) {
                        idCollectionModel.addShouldQuery(new QueryModel(QueryType.WILDCARD,
                            Map.of(FIELD, ID_KEYWORD, VALUE, id.getString(ID) + "*")));
                      }
                    }
                  } else {
                    promise.complete(responseMsg.getResponse());
                  }

                  esQueryModel.setQueries(idCollectionModel);

                  /* checking the requests for limit attribute */
                  if (request.containsKey(LIMIT)) {
                    Integer sizeFilter = request.getInteger(LIMIT);
                    esQueryModel.setLimit(sizeFilter.toString());
                  }

                  /* checking the requests for offset attribute */
                  if (request.containsKey(OFFSET)) {
                    Integer offsetFilter = request.getInteger(OFFSET);
                    esQueryModel.setOffset(offsetFilter.toString());
                  }

                  LOGGER.debug("INFO: QueryModel build;" + esQueryModel.toJson());

                  /* db query to find the relationship to the initial query */
                  esService.search(docIndex, esQueryModel)
                      .onComplete(
                          relSearchRes -> {
                            if (relSearchRes.succeeded()) {

                              LOGGER.debug("Success: Successful DB request");
                              List<ElasticsearchResponse> responseList = relSearchRes.result();
                              DbResponseMessageBuilder responseMessage =
                                  new DbResponseMessageBuilder();
                              responseMessage.statusSuccess().setTotalHits(responseList.size());
                              responseList.stream()
                                  .map(ElasticsearchResponse::getSource)
                                  .peek(source -> {
                                    source.remove(SUMMARY_KEY);
                                    source.remove(WORD_VECTOR_KEY);
                                  })
                                  .forEach(responseMessage::addResult);

                              promise.complete(responseMessage.getResponse());
                            } else if (relSearchRes.failed()) {
                              LOGGER.error(
                                  "Fail: DB request has failed;"
                                      + relSearchRes.cause().getMessage());
                              promise.fail(internalErrorResp());
                            }
                          });
                } else {
                  LOGGER.error("Fail: DB request has failed;" + searchRes.cause().getMessage());
                  promise.fail(internalErrorResp());
                }
              });
    }
    return promise.future();
  }

}
