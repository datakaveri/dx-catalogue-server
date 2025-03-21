package iudx.catalogue.server.mlayer.service;

import static iudx.catalogue.server.mlayer.util.QueryModelUtil.getMlayerAllDatasetsQuery;
import static iudx.catalogue.server.mlayer.util.QueryModelUtil.getMlayerAllResourcesQuery;
import static iudx.catalogue.server.util.Constants.RESULTS;
import static iudx.catalogue.server.util.Constants.TITLE_INVALID_QUERY_PARAM_VALUE;
import static iudx.catalogue.server.util.Constants.TYPE_INVALID_PROPERTY_VALUE;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import iudx.catalogue.server.common.RespBuilder;
import iudx.catalogue.server.database.cache.service.CacheService;
import iudx.catalogue.server.database.cache.service.CacheServiceImpl;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
import iudx.catalogue.server.exceptions.DatabaseFailureException;
import iudx.catalogue.server.exceptions.DocAlreadyExistsException;
import iudx.catalogue.server.mlayer.model.MlayerDatasetRequest;
import iudx.catalogue.server.mlayer.model.MlayerDomainRequest;
import iudx.catalogue.server.mlayer.model.MlayerGeoQueryRequest;
import iudx.catalogue.server.mlayer.model.MlayerInstanceRequest;
import iudx.catalogue.server.mlayer.util.QueryBuilder;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.common.database.postgres.service.PostgresService;

public class MlayerServiceImpl implements MlayerService {
  private static final Logger LOGGER = LogManager.getLogger(MlayerServiceImpl.class);
  ElasticsearchService esService;
  PostgresService postgresService;
  CacheService cacheService;
  QueryBuilder queryBuilder = new QueryBuilder();
  private final WebClient webClient;
  private final String mlayerInstanceIndex;
  private final String mlayerDomainIndex;
  private final String docIndex;
  private final String databaseTable;
  private final String catSummaryTable;
  private final JsonArray excludedIdsJson;

  public MlayerServiceImpl(WebClient webClient, ElasticsearchService esService,
                           PostgresService postgresService,
                           JsonObject config) {
    this.webClient = webClient;
    this.esService = esService;
    this.postgresService = postgresService;
    cacheService = new CacheServiceImpl(postgresService);
    databaseTable = config.getString("databaseTable");
    catSummaryTable = config.getString("catSummaryTable");
    excludedIdsJson = config.getJsonArray("excluded_ids");
    mlayerInstanceIndex = config.getString("mlayerInstanceIndex");
    mlayerDomainIndex = config.getString("mlayerDomainIndex");
    docIndex = config.getString("docIndex");
  }

  @Override
  public Future<JsonObject> createMlayerInstance(MlayerInstanceRequest instanceRequest) {
    MlayerInstanceService getMlayerInstance =
        new MlayerInstanceService(esService, mlayerInstanceIndex);
    return getMlayerInstance.createMlayerInstance(instanceRequest)
        .compose(createMlayerInstanceHandler -> {
            LOGGER.info("Success: Mlayer Instance Recorded");
            return Future.succeededFuture(createMlayerInstanceHandler);
          })
        .recover(err -> {
            LOGGER.error("Fail: Mlayer Instance creation failed");
          if (err instanceof DocAlreadyExistsException existsException) {
            LOGGER.debug("IAE exception for ID: {}", existsException.getItemId());
            JsonObject errorResponse = new JsonObject()
                .put("type", "DocAlreadyExistsException")
                .put("message", existsException.getMessage())
                .put("itemId", existsException.getItemId());
            return Future.failedFuture(errorResponse.encodePrettily());
          } else if (err instanceof DatabaseFailureException dbException) {
            LOGGER.debug("IAE exception for ID: {}", dbException.getId());
            JsonObject errorResponse = new JsonObject()
                .put("type", "DatabaseFailureException")
                .put("message", dbException.getMessage())
                .put("itemId", dbException.getId());

            return Future.failedFuture(errorResponse.encode());
          } else {
            return Future.failedFuture(err);
          }
        });
  }

  @Override
  public Future<JsonObject> getMlayerInstance(JsonObject requestParams) {
    Promise<JsonObject> promise = Promise.promise();

    MlayerInstanceService getMlayerInstance = new MlayerInstanceService(esService, mlayerInstanceIndex);
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

    MlayerInstanceService getMlayerInstance = new MlayerInstanceService(esService, mlayerInstanceIndex);
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
  public Future<JsonObject> updateMlayerInstance(MlayerInstanceRequest instanceRequest) {
    Promise<JsonObject> promise = Promise.promise();

    MlayerInstanceService getMlayerInstance = new MlayerInstanceService(esService, mlayerInstanceIndex);
    getMlayerInstance.updateMlayerInstance(instanceRequest)
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
  public Future<JsonObject> createMlayerDomain(MlayerDomainRequest request) {
    MlayerDomainService mlayerDomain = new MlayerDomainService(esService, mlayerDomainIndex);
    return mlayerDomain.createMlayerDomain(request)
        .compose(createMlayerInstanceHandler -> {
          LOGGER.info("Success: Mlayer Domain Recorded");
          return Future.succeededFuture(createMlayerInstanceHandler);
        })
        .recover(err -> {
          LOGGER.error("Fail: Mlayer Domain creation failed");
          if (err instanceof DocAlreadyExistsException existsException) {
            LOGGER.debug("IAE exception for ID: {}", existsException.getItemId());
            JsonObject errorResponse = new JsonObject()
                .put("type", "DocAlreadyExistsException")
                .put("message", existsException.getMessage())
                .put("itemId", existsException.getItemId());

            return Future.failedFuture(errorResponse.encode());
          } else if (err instanceof DatabaseFailureException dbException) {
            LOGGER.debug("IAE exception for ID: {}", dbException.getId());
            JsonObject errorResponse = new JsonObject()
                .put("type", "DatabaseFailureException")
                .put("message", dbException.getMessage())
                .put("itemId", dbException.getId());

            return Future.failedFuture(errorResponse.encode());
          } else {
            return Future.failedFuture(err);
          }
        });
  }

  @Override
  public Future<JsonObject> getMlayerDomain(JsonObject requestParams) {
    Promise<JsonObject> promise = Promise.promise();

    MlayerDomainService mlayerDomain = new MlayerDomainService(esService, mlayerDomainIndex);
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
  public Future<JsonObject> deleteMlayerDomain(String domainId) {
    Promise<JsonObject> promise = Promise.promise();

    MlayerDomainService mlayerDomain = new MlayerDomainService(esService, mlayerDomainIndex);
    mlayerDomain.deleteMlayerDomain(domainId)
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
  public Future<JsonObject> updateMlayerDomain(MlayerDomainRequest request) {
    MlayerDomainService mlayerDomain = new MlayerDomainService(esService, mlayerDomainIndex);
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

    MlayerProviderService mlayerProvider = new MlayerProviderService(esService, docIndex);
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
  public Future<JsonObject> getMlayerGeoQuery(MlayerGeoQueryRequest request) {
    Promise<JsonObject> promise = Promise.promise();

    MlayerGeoQueryService mlayerGeoQuery = new MlayerGeoQueryService(esService, docIndex);
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
  public Future<JsonObject> getMlayerAllDatasets(Integer limit, Integer offset) {

    LOGGER.debug("database get mlayer all datasets called");
    MlayerDatasetService mlayerDataset = new MlayerDatasetService(webClient, esService, docIndex,
        mlayerInstanceIndex);
    Promise<JsonObject> promise = Promise.promise();
    QueryModel getMlayerAllDatasetsQuery = getMlayerAllDatasetsQuery();
    mlayerDataset.getMlayerAllDatasets(getMlayerAllDatasetsQuery, limit, offset)
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
  public Future<JsonObject> getMlayerDataset(MlayerDatasetRequest requestData) {
    Promise<JsonObject> promise = Promise.promise();
    if (requestData.getId() != null && !requestData.getId().isBlank()) {
      MlayerDatasetService mlayerDataset = new MlayerDatasetService(webClient, esService, docIndex,
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
    } else if ((requestData.getTags()!=null
        || requestData.getInstance()!=null
        || requestData.getProviders()!=null
        || requestData.getDomains()!=null)
        && (requestData.getId() == null || requestData.getId().isBlank())) {
      if (requestData.getDomains()!=null && !requestData.getDomains().isEmpty()) {
        List<String> domainsArray = requestData.getDomains();
        List<String> tagsArray =
            requestData.getTags()!=null ? requestData.getTags() : new ArrayList<>();

        tagsArray.addAll(domainsArray);
        requestData.setTags(tagsArray);
      }
      LOGGER.debug("database get mlayer all datasets called");
      MlayerDatasetService mlayerDataset = new MlayerDatasetService(webClient, esService, docIndex,
          mlayerInstanceIndex);

      QueryModel getAllMlayerResourcesQuery = getMlayerAllResourcesQuery(requestData.toJson());
      mlayerDataset.getMlayerAllDatasets(getAllMlayerResourcesQuery, requestData.getLimit(),
              requestData.getOffset())
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

    cacheService.getPopularDatasetCounts(databaseTable).onComplete(cacheHandler -> {
      if (cacheHandler.succeeded()) {

        JsonArray popularDataset = cacheHandler.result().getJsonArray(RESULTS);
        LOGGER.debug("popular datasets are {}", popularDataset);

        JsonArray popularRgs = new JsonArray();
        for (int popularRgCount = 0; popularRgCount < popularDataset.size(); popularRgCount++) {
          String rgId = popularDataset.getJsonObject(popularRgCount).getString("resource_group");
          if (rgId != null) {
            popularRgs.add(rgId);
          }
        }

        MlayerPopularDatasetsService mlayerPopularDatasets =
            new MlayerPopularDatasetsService(webClient, esService, docIndex,
                mlayerInstanceIndex, mlayerDomainIndex);

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
        LOGGER.error("Cache retrieval failed, falling back to database.");
        promise.fail(cacheHandler.cause());
      }
    });

    return promise.future();
  }

  @Override
  public Future<JsonObject> getSummaryCountSizeApi() {
    Promise<JsonObject> promise = Promise.promise();
    LOGGER.info(" into get summary count api");
    String query = queryBuilder.buildSummaryCountSizeQuery(catSummaryTable);
    LOGGER.debug("Query =  {}", query);

    cacheService.getSummaryCountSizeApi(catSummaryTable)
        .onComplete(allQueryHandler -> {
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

    cacheService.getRealTimeDataSetApi(databaseTable, excludedIdsJson)
        .onComplete(dbHandler -> {
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
