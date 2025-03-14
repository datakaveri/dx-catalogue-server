package iudx.catalogue.server.mlayer.service;

import static iudx.catalogue.server.mlayer.util.Constants.DOMAIN_ID;
import static iudx.catalogue.server.mlayer.util.Constants.GET_HIGH_COUNT_DATASET;
import static iudx.catalogue.server.mlayer.util.Constants.INSTANCE_ID;
import static iudx.catalogue.server.mlayer.util.Constants.MLAYER_ID;
import static iudx.catalogue.server.util.Constants.FIELD;
import static iudx.catalogue.server.util.Constants.ID;
import static iudx.catalogue.server.util.Constants.INSTANCE;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_COS;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_PROVIDER;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_RESOURCE;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_RESOURCE_GROUP;
import static iudx.catalogue.server.util.Constants.MAX_LIMIT;
import static iudx.catalogue.server.util.Constants.NAME;
import static iudx.catalogue.server.util.Constants.PROVIDERS;
import static iudx.catalogue.server.util.Constants.TAGS;
import static iudx.catalogue.server.util.Constants.TITLE_INVALID_QUERY_PARAM_VALUE;
import static iudx.catalogue.server.util.Constants.TYPE_INVALID_PROPERTY_VALUE;
import static iudx.catalogue.server.util.Constants.VALUE;

import com.google.common.hash.Hashing;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import iudx.catalogue.server.common.RespBuilder;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
import iudx.catalogue.server.database.elastic.util.QueryType;
import iudx.catalogue.server.database.postgres.service.PostgresService;
import iudx.catalogue.server.mlayer.util.QueryBuilder;
import iudx.catalogue.server.mlayer.util.model.MlayerDataset;
import iudx.catalogue.server.mlayer.util.model.MlayerDomain;
import iudx.catalogue.server.mlayer.util.model.MlayerGeoQuery;
import iudx.catalogue.server.mlayer.util.model.MlayerInstance;
import iudx.catalogue.server.mlayer.util.model.MlayerPopularDatasets;
import iudx.catalogue.server.mlayer.util.model.MlayerProvider;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MlayerServiceImpl implements MlayerService {
  private static final Logger LOGGER = LogManager.getLogger(MlayerServiceImpl.class);
  ElasticsearchService esService;
  PostgresService postgresService;
  QueryBuilder queryBuilder = new QueryBuilder();
  private final WebClient webClient;
  private final String mlayerInstanceIndex;
  private final String mlayerDomainIndex;
  private final String docIndex;
  private final String databaseTable;
  private final String catSummaryTable;
  private JsonObject configJson;
  private final JsonArray excludedIdsJson;

  public MlayerServiceImpl(WebClient webClient, ElasticsearchService esService,
                           PostgresService postgresService,
                           JsonObject config) {
    this.webClient = webClient;
    this.esService = esService;
    this.postgresService = postgresService;
    this.configJson = config;
    databaseTable = configJson.getString("databaseTable");
    catSummaryTable = configJson.getString("catSummaryTable");
    excludedIdsJson = configJson.getJsonArray("excluded_ids");
    mlayerInstanceIndex = configJson.getString("mlayerInstanceIndex");
    mlayerDomainIndex = configJson.getString("mlayerDomainIndex");
    docIndex = configJson.getString("docIndex");
  }

  @Override
  public Future<JsonObject> createMlayerInstance(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    String name = request.getString(NAME).toLowerCase();
    String id = Hashing.sha256().hashString(name, StandardCharsets.UTF_8).toString();
    String instanceId = UUID.randomUUID().toString();
    if (!request.containsKey("instanceId")) {
      request.put(INSTANCE_ID, instanceId);
    }
    request.put(MLAYER_ID, id);

    MlayerInstance getMlayerInstance = new MlayerInstance(esService, mlayerInstanceIndex);
    getMlayerInstance.createMlayerInstance(request)
        .onComplete(createMlayerInstanceHandler -> {
          if (createMlayerInstanceHandler.succeeded()) {
            LOGGER.info("Success: Mlayer Instance Recorded");
            promise.complete(createMlayerInstanceHandler.result());
          } else {
            LOGGER.error("Fail: Mlayer Instance creation failed");
            promise.fail(createMlayerInstanceHandler.cause());
          }
        });
    return promise.future();
  }

  @Override
  public Future<JsonObject> getMlayerInstance(JsonObject requestParams) {
    Promise<JsonObject> promise = Promise.promise();

    MlayerInstance getMlayerInstance = new MlayerInstance(esService, mlayerInstanceIndex);
    getMlayerInstance.getMlayerInstance(requestParams)
        .onComplete(getMlayerInstancehandler -> {
          if (getMlayerInstancehandler.succeeded()) {
            LOGGER.info("Success: Getting all Instance Values");
            promise.complete(getMlayerInstancehandler.result());
          } else {
            LOGGER.error("Fail: Getting all instances failed");
            promise.fail(getMlayerInstancehandler.cause());
          }
        });
    return promise.future();
  }

  @Override
  public Future<JsonObject> deleteMlayerInstance(String request) {
    Promise<JsonObject> promise = Promise.promise();

    MlayerInstance getMlayerInstance = new MlayerInstance(esService, mlayerInstanceIndex);
    getMlayerInstance.deleteMlayerInstance(request)
        .onComplete(deleteMlayerInstanceHandler -> {
          if (deleteMlayerInstanceHandler.succeeded()) {
            LOGGER.info("Success: Mlayer Instance Deleted");
            promise.complete(deleteMlayerInstanceHandler.result());
          } else {
            LOGGER.error("Fail: Mlayer Instance deletion failed");
            promise.fail(deleteMlayerInstanceHandler.cause());
          }
        });
    return promise.future();
  }

  @Override
  public Future<JsonObject> updateMlayerInstance(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    String name = request.getString(NAME).toLowerCase();
    String id = Hashing.sha256().hashString(name, StandardCharsets.UTF_8).toString();
    LOGGER.debug(id);
    request.put("id", id);
    MlayerInstance getMlayerInstance = new MlayerInstance(esService, mlayerInstanceIndex);
    getMlayerInstance.updateMlayerInstance(request)
        .onComplete(updateMlayerHandler -> {
          if (updateMlayerHandler.succeeded()) {
            LOGGER.info("Success: mlayer instance Updated");
            promise.complete(updateMlayerHandler.result());
          } else {
            LOGGER.error("Fail: Mlayer Instance updation failed");
            promise.fail(updateMlayerHandler.cause());
          }
        });

    return promise.future();
  }

  @Override
  public Future<JsonObject> createMlayerDomain(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    String name = request.getString(NAME).toLowerCase();
    String id = Hashing.sha256().hashString(name, StandardCharsets.UTF_8).toString();
    String domainId = UUID.randomUUID().toString();
    if (!request.containsKey("domainId")) {
      request.put(DOMAIN_ID, domainId);
    }
    request.put(MLAYER_ID, id);

    MlayerDomain mlayerDomain = new MlayerDomain(esService, mlayerDomainIndex);
    mlayerDomain.createMlayerDomain(request)
        .onComplete(createMlayerDomainHandler -> {
          if (createMlayerDomainHandler.succeeded()) {
            LOGGER.info("Success: Mlayer Domain Recorded");
            promise.complete(createMlayerDomainHandler.result());
          } else {
            LOGGER.error("Fail: Mlayer Domain creation failed");
            promise.fail(createMlayerDomainHandler.cause());
          }
        });
    return promise.future();
  }

  @Override
  public Future<JsonObject> getMlayerDomain(JsonObject requestParams) {
    Promise<JsonObject> promise = Promise.promise();

    MlayerDomain mlayerDomain = new MlayerDomain(esService, mlayerDomainIndex);
    mlayerDomain.getMlayerDomain(requestParams)
        .onComplete(getMlayerDomainHandler -> {
          if (getMlayerDomainHandler.succeeded()) {
            LOGGER.info("Success: Getting all domain values");
            promise.complete(getMlayerDomainHandler.result());
          } else {
            LOGGER.error("Fail: Getting all domains failed");
            promise.fail(getMlayerDomainHandler.cause());
          }
        });
    return promise.future();
  }

  @Override
  public Future<JsonObject> deleteMlayerDomain(String request) {
    Promise<JsonObject> promise = Promise.promise();

    MlayerDomain mlayerDomain = new MlayerDomain(esService, mlayerDomainIndex);
    mlayerDomain.deleteMlayerDomain(request)
        .onComplete(deleteMlayerDomainHandler -> {
          if (deleteMlayerDomainHandler.succeeded()) {
            LOGGER.info("Success: Mlayer Doamin Deleted");
            promise.complete(deleteMlayerDomainHandler.result());
          } else {
            LOGGER.error("Fail: Mlayer Domain deletion failed");
            promise.fail(deleteMlayerDomainHandler.cause());
          }
        });
    return promise.future();
  }

  @Override
  public Future<JsonObject> updateMlayerDomain(JsonObject request) {
    String name = request.getString(NAME).toLowerCase();
    LOGGER.debug(name);
    String id = Hashing.sha256().hashString(name, StandardCharsets.UTF_8).toString();
    LOGGER.debug(id);
    request.put(MLAYER_ID, id);
    MlayerDomain mlayerDomain = new MlayerDomain(esService, mlayerDomainIndex);
    Promise<JsonObject> promise = Promise.promise();
    mlayerDomain.updateMlayerDomain(request)
        .onComplete(updateMlayerHandler -> {
          if (updateMlayerHandler.succeeded()) {
            LOGGER.info("Success: mlayer domain updated");
            promise.complete(updateMlayerHandler.result());
          } else {
            LOGGER.error("Fail: Mlayer Domain updation Failed");
            promise.fail(updateMlayerHandler.cause());
          }
        });

    return promise.future();
  }

  @Override
  public Future<JsonObject> getMlayerProviders(JsonObject requestParams) {
    Promise<JsonObject> promise = Promise.promise();

    MlayerProvider mlayerProvider = new MlayerProvider(esService, docIndex);
    mlayerProvider.getMlayerProviders(requestParams)
        .onComplete(getMlayerDomainHandler -> {
          if (getMlayerDomainHandler.succeeded()) {
            LOGGER.info("Success: Getting all  providers");
            promise.complete(getMlayerDomainHandler.result());
          } else {
            LOGGER.error("Fail: Getting all providers failed");
            promise.fail(getMlayerDomainHandler.cause());
          }
        });
    return promise.future();
  }

  @Override
  public Future<JsonObject> getMlayerGeoQuery(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();

    MlayerGeoQuery mlayerGeoQuery = new MlayerGeoQuery(esService, docIndex);
    mlayerGeoQuery.getMlayerGeoQuery(request)
        .onComplete(postMlayerGeoQueryHandler -> {
          if (postMlayerGeoQueryHandler.succeeded()) {
            LOGGER.info("Success: Getting locations of datasets");
            promise.complete(postMlayerGeoQueryHandler.result());
          } else {
            LOGGER.error("Fail: Getting locations of datasets failed");
            promise.fail(postMlayerGeoQueryHandler.cause());
          }
        });
    return promise.future();
  }

  @Override
  public Future<JsonObject> getMlayerAllDatasets(JsonObject requestParam) {
    QueryModel query = new QueryModel(QueryType.BOOL);
    query.addMustQuery(new QueryModel(QueryType.TERMS,
        Map.of(FIELD, "type.keyword", VALUE,
            List.of(ITEM_TYPE_PROVIDER, ITEM_TYPE_COS, ITEM_TYPE_RESOURCE_GROUP,
                ITEM_TYPE_RESOURCE))));

    QueryModel queryModel = new QueryModel();
    queryModel.setQueries(query);
    queryModel.setIncludeFields(
        List.of("type", "id", "label", "accessPolicy", "tags", "instance", "provider",
            "resourceServerRegURL", "description", "cosURL", "cos", "resourceGroup",
            "resourceType", "itemCreatedAt", "icon_base64"));
    queryModel.setLimit(MAX_LIMIT);

    LOGGER.debug("database get mlayer all datasets called");
    MlayerDataset mlayerDataset = new MlayerDataset(webClient, esService, docIndex,
        mlayerInstanceIndex);
    Promise<JsonObject> promise = Promise.promise();
    mlayerDataset.getMlayerAllDatasets(requestParam, queryModel)
        .onComplete(getMlayerAllDatasets -> {
          if (getMlayerAllDatasets.succeeded()) {
            LOGGER.info("Success: Getting all datasets");
            promise.complete(getMlayerAllDatasets.result());
          } else {
            LOGGER.error("Fail: Getting all datasets failed");
            promise.fail(getMlayerAllDatasets.cause());
          }
        });
    return promise.future();
  }

  @Override
  public Future<JsonObject> getMlayerDataset(JsonObject requestData) {
    Promise<JsonObject> promise = Promise.promise();
    if (requestData.containsKey(ID) && !requestData.getString(ID).isBlank()) {
      MlayerDataset mlayerDataset = new MlayerDataset(webClient, esService, docIndex,
          mlayerInstanceIndex);
      mlayerDataset.getMlayerDataset(requestData)
          .onComplete(getMlayerDatasetHandler -> {
            if (getMlayerDatasetHandler.succeeded()) {
              LOGGER.info("Success: Getting details of dataset");
              promise.complete(getMlayerDatasetHandler.result());
            } else {
              LOGGER.error("Fail: Getting details of dataset");
              promise.fail(getMlayerDatasetHandler.cause());
            }
          });
    } else if ((requestData.containsKey("tags")
        || requestData.containsKey("instance")
        || requestData.containsKey("providers")
        || requestData.containsKey("domains"))
        && (!requestData.containsKey(ID) || requestData.getString(ID).isBlank())) {
      if (requestData.containsKey("domains") && !requestData.getJsonArray("domains").isEmpty()) {
        JsonArray domainsArray = requestData.getJsonArray("domains");
        JsonArray tagsArray =
            requestData.containsKey("tags") ? requestData.getJsonArray("tags") : new JsonArray();

        tagsArray.addAll(domainsArray);
        requestData.put("tags", tagsArray);
      }

      QueryModel baseResourceGroupQuery = new QueryModel(QueryType.BOOL);
      baseResourceGroupQuery.addMustQuery(new QueryModel(QueryType.MATCH, Map.of(FIELD,
          "type.keyword", VALUE, ITEM_TYPE_RESOURCE_GROUP)));

      if (requestData.containsKey(TAGS) && !requestData.getJsonArray(TAGS).isEmpty()) {
        JsonArray tagsArray = requestData.getJsonArray(TAGS);
        String tagQueryString = tagsArray.stream()
            .filter(tag -> tag instanceof String)
            .map(Object::toString)
            .reduce((a, b) -> a + " AND " + b)
            .orElse("");

        if (!tagQueryString.isEmpty()) {
          QueryModel tagQuery = new QueryModel(QueryType.QUERY_STRING, Map.of(
              "default_field", "tags",
              "query", "(" + tagQueryString + ")"
          ));
          baseResourceGroupQuery.addMustQuery(tagQuery);
        }
      }
      if (requestData.containsKey(INSTANCE) && !requestData.getString(INSTANCE).isBlank()) {
        String instanceValue = requestData.getString(INSTANCE).toLowerCase();
        baseResourceGroupQuery.addMustQuery(
            new QueryModel(QueryType.MATCH, Map.of(FIELD, "instance.keyword", VALUE,
                instanceValue)));
      }
      if (requestData.containsKey(PROVIDERS) && !requestData.getJsonArray(PROVIDERS).isEmpty()) {
        baseResourceGroupQuery.addMustQuery(new QueryModel(QueryType.TERMS, Map.of(FIELD,
            "provider.keyword", VALUE, requestData.getJsonArray(PROVIDERS)
        )));
      }

      QueryModel baseProviderCosQuery = new QueryModel(QueryType.BOOL);
      baseProviderCosQuery.addMustQuery(new QueryModel(QueryType.TERMS, Map.of(FIELD,
          "type.keyword", VALUE, List.of(ITEM_TYPE_PROVIDER, ITEM_TYPE_COS))));

      QueryModel mainQueryModel = new QueryModel();
      QueryModel queryModel = new QueryModel(QueryType.BOOL);
      queryModel.setShouldQueries(List.of(baseResourceGroupQuery, baseProviderCosQuery));
      mainQueryModel.setQueries(queryModel);
      mainQueryModel.setIncludeFields(List.of(
          "type", "id", "label", "accessPolicy", "tags", "instance",
          "provider", "resourceServerRegURL", "description", "cosURL",
          "cos", "resourceGroup", "itemCreatedAt", "icon_base64"
      ));
      mainQueryModel.setLimit(MAX_LIMIT);
      LOGGER.debug("database get mlayer all datasets called");
      MlayerDataset mlayerDataset = new MlayerDataset(webClient, esService, docIndex,
          mlayerInstanceIndex);
      mlayerDataset.getMlayerAllDatasets(requestData, mainQueryModel)
          .onComplete(getAllDatasetsHandler -> {
            if (getAllDatasetsHandler.succeeded()) {
              LOGGER.info("Success: Getting details of all datasets");
              promise.complete(getAllDatasetsHandler.result());
            } else {
              LOGGER.error("Fail: Getting details of all datasets");
              promise.fail(getAllDatasetsHandler.cause());
            }
          });
    } else {
      LOGGER.error("Invalid field present in request body");
      promise.fail(
          new RespBuilder()
              .withType(TYPE_INVALID_PROPERTY_VALUE)
              .withTitle(TITLE_INVALID_QUERY_PARAM_VALUE)
              .withDetail("The schema is Invalid")
              .getResponse());
    }

    return promise.future();
  }

  @Override
  public Future<JsonObject> getMlayerPopularDatasets(String instance) {
    Promise<JsonObject> promise = Promise.promise();
    String query = GET_HIGH_COUNT_DATASET.replace("$1", databaseTable);
    LOGGER.debug("postgres query " + query);
    postgresService.executeQuery(query).onComplete(dbHandler -> {
      if (dbHandler.succeeded()) {
        JsonArray popularDataset = dbHandler.result().getJsonArray("results");
        LOGGER.debug("popular datasets are {}", popularDataset);
        JsonArray popularRgs = new JsonArray();
        for (int popularRgCount = 0; popularRgCount < popularDataset.size(); popularRgCount++) {
          String rgId =
              popularDataset.getJsonObject(popularRgCount).getString("resource_group");

          if (rgId != null) {
            popularRgs.add(rgId);
          }
        }

        MlayerPopularDatasets mlayerPopularDatasets =
            new MlayerPopularDatasets(webClient, esService, docIndex, mlayerInstanceIndex,
                mlayerDomainIndex);
        mlayerPopularDatasets.getMlayerPopularDatasets(instance, popularRgs)
            .onComplete(getPopularDatasetsHandler -> {
              if (getPopularDatasetsHandler.succeeded()) {
                LOGGER.debug("Success: Getting data for the landing page.");
                promise.complete(getPopularDatasetsHandler.result());
              } else {
                LOGGER.error("Fail: Getting data for the landing page.");
                promise.fail(getPopularDatasetsHandler.cause());
              }
            });

      } else {
        LOGGER.error("postgres query failed");
        promise.fail(dbHandler.cause());
      }
    });

    return promise.future();
  }

  @Override
  public Future<JsonObject> getSummaryCountSizeApi() {
    Promise<JsonObject> promise = Promise.promise();
    LOGGER.info(" into get summary count api");
    String query = queryBuilder.buildSummaryCountSizeQuery(catSummaryTable);

    LOGGER.debug(" Query: {} ", query);
    postgresService.executeQuery(query).onComplete(allQueryHandler -> {
      if (allQueryHandler.succeeded()) {
        promise.complete(allQueryHandler.result());
      } else {
        promise.fail(allQueryHandler.cause());
      }
    });
    return promise.future();
  }

  @Override
  public Future<JsonObject> getRealTimeDataSetApi() {
    Promise<JsonObject> promise = Promise.promise();
    LOGGER.info(" into get real time dataset api");
    String query = queryBuilder.buildCountAndSizeQuery(databaseTable, excludedIdsJson);
    LOGGER.debug("Query =  {}", query);

    postgresService.executeQuery(query).onComplete(dbHandler -> {
      if (dbHandler.succeeded()) {
        JsonObject results = dbHandler.result();
        promise.complete(results);
      } else {
        LOGGER.error("postgres query failed");
        promise.fail(dbHandler.cause());
      }
    });
    return promise.future();
  }
}
