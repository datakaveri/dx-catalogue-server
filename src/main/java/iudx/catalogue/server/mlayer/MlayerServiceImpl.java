package iudx.catalogue.server.mlayer;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.mlayer.util.Constants.*;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.util.Constants.NAME;
import static iudx.catalogue.server.util.Constants.PROVIDERS;

import com.google.common.hash.Hashing;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.database.RespBuilder;
import iudx.catalogue.server.database.postgres.PostgresService;
import iudx.catalogue.server.mlayer.util.QueryBuilder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MlayerServiceImpl implements MlayerService {
  private static final Logger LOGGER = LogManager.getLogger(MlayerServiceImpl.class);
  DatabaseService databaseService;
  PostgresService postgresService;
  QueryBuilder queryBuilder = new QueryBuilder();
  private String databaseTable;
  private String catSummaryTable;
  private JsonObject configJson;
  private JsonArray excludedIdsJson;

  MlayerServiceImpl(
          DatabaseService databaseService,
          PostgresService postgresService,
          JsonObject config) {
    this.databaseService = databaseService;
    this.postgresService = postgresService;
    this.configJson = config;
    databaseTable = configJson.getString("databaseTable");
    catSummaryTable = configJson.getString("catSummaryTable");
    excludedIdsJson = configJson.getJsonArray("excluded_ids");
  }

  @Override
  public MlayerService createMlayerInstance(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String name = request.getString(NAME).toLowerCase();
    String id = Hashing.sha256().hashString(name, StandardCharsets.UTF_8).toString();
    String instanceId = UUID.randomUUID().toString();
    if (!request.containsKey("instanceId")) {
      request.put(INSTANCE_ID, instanceId);
    }
    request.put(MLAYER_ID, id);

    databaseService.createMlayerInstance(
        request,
        createMlayerInstanceHandler -> {
          if (createMlayerInstanceHandler.succeeded()) {
            LOGGER.info("Success: Mlayer Instance Recorded");
            handler.handle(Future.succeededFuture(createMlayerInstanceHandler.result()));
          } else {
            LOGGER.error("Fail: Mlayer Instance creation failed");
            handler.handle(Future.failedFuture(createMlayerInstanceHandler.cause()));
          }
        });
    return this;
  }

  @Override
  public MlayerService getMlayerInstance(
      JsonObject requestParams, Handler<AsyncResult<JsonObject>> handler) {

    databaseService.getMlayerInstance(
        requestParams,
        getMlayerInstancehandler -> {
          if (getMlayerInstancehandler.succeeded()) {
            LOGGER.info("Success: Getting all Instance Values");
            handler.handle(Future.succeededFuture(getMlayerInstancehandler.result()));
          } else {
            LOGGER.error("Fail: Getting all instances failed");
            handler.handle(Future.failedFuture(getMlayerInstancehandler.cause()));
          }
        });
    return this;
  }

  @Override
  public MlayerService deleteMlayerInstance(
      String request, Handler<AsyncResult<JsonObject>> handler) {
    databaseService.deleteMlayerInstance(
        request,
        deleteMlayerInstanceHandler -> {
          if (deleteMlayerInstanceHandler.succeeded()) {
            LOGGER.info("Success: Mlayer Instance Deleted");
            handler.handle(Future.succeededFuture(deleteMlayerInstanceHandler.result()));
          } else {
            LOGGER.error("Fail: Mlayer Instance deletion failed");
            handler.handle(Future.failedFuture(deleteMlayerInstanceHandler.cause()));
          }
        });
    return this;
  }

  @Override
  public MlayerService updateMlayerInstance(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String name = request.getString(NAME).toLowerCase();
    String id = Hashing.sha256().hashString(name, StandardCharsets.UTF_8).toString();
    LOGGER.debug(id);
    request.put("id", id);
    databaseService.updateMlayerInstance(
        request,
        updateMlayerHandler -> {
          if (updateMlayerHandler.succeeded()) {
            LOGGER.info("Success: mlayer instance Updated");
            handler.handle(Future.succeededFuture(updateMlayerHandler.result()));
          } else {
            LOGGER.error("Fail: Mlayer Instance updation failed");
            handler.handle(Future.failedFuture(updateMlayerHandler.cause()));
          }
        });

    return this;
  }

  @Override
  public MlayerService createMlayerDomain(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String name = request.getString(NAME).toLowerCase();
    String id = Hashing.sha256().hashString(name, StandardCharsets.UTF_8).toString();
    String domainId = UUID.randomUUID().toString();
    if (!request.containsKey("domainId")) {
      request.put(DOMAIN_ID, domainId);
    }
    request.put(MLAYER_ID, id);

    databaseService.createMlayerDomain(
        request,
        createMlayerDomainHandler -> {
          if (createMlayerDomainHandler.succeeded()) {
            LOGGER.info("Success: Mlayer Domain Recorded");
            handler.handle(Future.succeededFuture(createMlayerDomainHandler.result()));
          } else {
            LOGGER.error("Fail: Mlayer Domain creation failed");
            handler.handle(Future.failedFuture(createMlayerDomainHandler.cause()));
          }
        });
    return this;
  }

  @Override
  public MlayerService getMlayerDomain(
      JsonObject requestParams, Handler<AsyncResult<JsonObject>> handler) {
    databaseService.getMlayerDomain(
        requestParams,
        getMlayerDomainHandler -> {
          if (getMlayerDomainHandler.succeeded()) {
            LOGGER.info("Success: Getting all domain values");
            handler.handle(Future.succeededFuture(getMlayerDomainHandler.result()));
          } else {
            LOGGER.error("Fail: Getting all domains failed");
            handler.handle(Future.failedFuture(getMlayerDomainHandler.cause()));
          }
        });
    return this;
  }

  @Override
  public MlayerService deleteMlayerDomain(
      String request, Handler<AsyncResult<JsonObject>> handler) {
    databaseService.deleteMlayerDomain(
        request,
        deleteMlayerDomainHandler -> {
          if (deleteMlayerDomainHandler.succeeded()) {
            LOGGER.info("Success: Mlayer Doamin Deleted");
            handler.handle(Future.succeededFuture(deleteMlayerDomainHandler.result()));
          } else {
            LOGGER.error("Fail: Mlayer Domain deletion failed");
            handler.handle(Future.failedFuture(deleteMlayerDomainHandler.cause()));
          }
        });
    return this;
  }

  @Override
  public MlayerService updateMlayerDomain(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String name = request.getString(NAME).toLowerCase();
    LOGGER.debug(name);
    String id = Hashing.sha256().hashString(name, StandardCharsets.UTF_8).toString();
    LOGGER.debug(id);
    request.put(MLAYER_ID, id);
    databaseService.updateMlayerDomain(
        request,
        updateMlayerHandler -> {
          if (updateMlayerHandler.succeeded()) {
            LOGGER.info("Success: mlayer domain updated");
            handler.handle(Future.succeededFuture(updateMlayerHandler.result()));
          } else {
            LOGGER.error("Fail: Mlayer Domain updation Failed");
            handler.handle(Future.failedFuture(updateMlayerHandler.cause()));
          }
        });

    return this;
  }

  @Override
  public MlayerService getMlayerProviders(
      JsonObject requestParams, Handler<AsyncResult<JsonObject>> handler) {
    databaseService.getMlayerProviders(
        requestParams,
        getMlayerDomainHandler -> {
          if (getMlayerDomainHandler.succeeded()) {
            LOGGER.info("Success: Getting all  providers");
            handler.handle(Future.succeededFuture(getMlayerDomainHandler.result()));
          } else {
            LOGGER.error("Fail: Getting all providers failed");
            handler.handle(Future.failedFuture(getMlayerDomainHandler.cause()));
          }
        });
    return this;
  }

  @Override
  public MlayerService getMlayerGeoQuery(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    databaseService.getMlayerGeoQuery(
        request,
        postMlayerGeoQueryHandler -> {
          if (postMlayerGeoQueryHandler.succeeded()) {
            LOGGER.info("Success: Getting locations of datasets");
            handler.handle(Future.succeededFuture(postMlayerGeoQueryHandler.result()));
          } else {
            LOGGER.error("Fail: Getting locations of datasets failed");
            handler.handle(Future.failedFuture(postMlayerGeoQueryHandler.cause()));
          }
        });
    return this;
  }

  @Override
  public MlayerService getMlayerAllDatasets(
      JsonObject requestParam, Handler<AsyncResult<JsonObject>> handler) {
    String query = GET_MLAYER_ALL_DATASETS;
    LOGGER.debug("databse get mlayer all datasets called");
    databaseService.getMlayerAllDatasets(
        requestParam,
        query,
        getMlayerAllDatasets -> {
          if (getMlayerAllDatasets.succeeded()) {
            LOGGER.info("Success: Getting all datasets");
            handler.handle(Future.succeededFuture(getMlayerAllDatasets.result()));
          } else {
            LOGGER.error("Fail: Getting all datasets failed");
            handler.handle(Future.failedFuture(getMlayerAllDatasets.cause()));
          }
        });
    return this;
  }

  @Override
  public MlayerService getMlayerDataset(
      JsonObject requestData, Handler<AsyncResult<JsonObject>> handler) {
    if (requestData.containsKey(ID) && !requestData.getString(ID).isBlank()) {
      databaseService.getMlayerDataset(
          requestData,
          getMlayerDatasetHandler -> {
            if (getMlayerDatasetHandler.succeeded()) {
              LOGGER.info("Success: Getting details of dataset");
              handler.handle(Future.succeededFuture(getMlayerDatasetHandler.result()));
            } else {
              LOGGER.error("Fail: Getting details of dataset");
              handler.handle(Future.failedFuture(getMlayerDatasetHandler.cause()));
            }
          });
    } else if ((requestData.containsKey("tags")
            || requestData.containsKey("instance")
            || requestData.containsKey("providers")
            || requestData.containsKey("domains"))
        && (!requestData.containsKey(ID) || requestData.getString(ID).isBlank())) {
      if (requestData.containsKey("domains")
          && !requestData.getJsonArray("domains").isEmpty()) {
        JsonArray domainsArray = requestData.getJsonArray("domains");
        JsonArray tagsArray =
            requestData.containsKey("tags")
                ? requestData.getJsonArray("tags")
                : new JsonArray();

        tagsArray.addAll(domainsArray);
        requestData.put("tags", tagsArray);
      }
      String query = GET_ALL_DATASETS_BY_FIELDS;

      if (requestData.containsKey(TAGS) && !requestData.getJsonArray(TAGS).isEmpty()) {
        JsonArray tagsArray = requestData.getJsonArray(TAGS);
        String tagQueryString = "";

        for (Object tagValue : tagsArray) {
          if (tagValue instanceof String) {
            tagQueryString = tagQueryString.concat(tagValue + " AND ");
          }
        }

        if (!tagQueryString.isEmpty()) {
          tagQueryString = "(" + tagQueryString.substring(0, tagQueryString.length() - 4) + ")";
          query += ",{\"query_string\":{\"default_field\":\"tags\",\"query\":\""
                  + tagQueryString + "\"}}";
        }
      }
      if (requestData.containsKey(INSTANCE) && !requestData.getString(INSTANCE).isBlank()) {
        query =
            query.concat(
                ",{\"match\":{\"instance.keyword\":\"$1\"}}"
                    .replace("$1", requestData.getString(INSTANCE).toLowerCase()));
      }
      if (requestData.containsKey(PROVIDERS)
          && !requestData.getJsonArray(PROVIDERS).isEmpty()) {
        query =
            query.concat(
                ",{\"terms\":{\"provider.keyword\":$1}}"
                    .replace("$1", requestData.getJsonArray(PROVIDERS).toString()));
      }
      query = query.concat(GET_ALL_DATASETS_BY_FIELD_SOURCE);
      LOGGER.debug("databse get mlayer all datasets called");
      databaseService.getMlayerAllDatasets(
          requestData,
          query,
          getAllDatasetsHandler -> {
            if (getAllDatasetsHandler.succeeded()) {
              LOGGER.info("Success: Getting details of all datasets");
              handler.handle(Future.succeededFuture(getAllDatasetsHandler.result()));
            } else {
              LOGGER.error("Fail: Getting details of all datasets");
              handler.handle(Future.failedFuture(getAllDatasetsHandler.cause()));
            }
          });
    } else {
      LOGGER.error("Invalid field present in request body");
      handler.handle(
          Future.failedFuture(
              new RespBuilder()
                  .withType(TYPE_INVALID_PROPERTY_VALUE)
                  .withTitle(TITLE_INVALID_QUERY_PARAM_VALUE)
                  .withDetail("The schema is Invalid")
                  .getResponse()));
    }

    return this;
  }

  @Override
  public MlayerService getMlayerPopularDatasets(
      String instance, Handler<AsyncResult<JsonObject>> handler) {
    String query = GET_HIGH_COUNT_DATASET.replace("$1", databaseTable);
    LOGGER.debug("postgres query" + query);
    postgresService.executeQuery(
        query,
        dbHandler -> {
          if (dbHandler.succeeded()) {
            JsonArray popularDataset = dbHandler.result().getJsonArray("results");
            JsonArray popularRgs = new JsonArray();
            for (int popularRgCount = 0; popularRgCount < popularDataset.size(); popularRgCount++) {
              String rgId =
                  popularDataset.getJsonObject(popularRgCount).getString("resource_group");

              if (rgId != null) {
                popularRgs.add(rgId);
              }
            }
            LOGGER.debug("popular resource group's id retrieved {}", popularRgs);
            databaseService.getMlayerPopularDatasets(
                instance,
                popularRgs,
                getPopularDatasetsHandler -> {
                  if (getPopularDatasetsHandler.succeeded()) {
                    LOGGER.info("Success: Getting data for the landing page.");
                    handler.handle(Future.succeededFuture(getPopularDatasetsHandler.result()));
                  } else {
                    LOGGER.error("Fail: Getting data for the landing page.");
                    handler.handle(Future.failedFuture(getPopularDatasetsHandler.cause()));
                  }
                });

          } else {
            LOGGER.debug("postgres query failed");
            handler.handle(Future.failedFuture(dbHandler.cause()));
          }
        });

    return this;
  }

  @Override
  public MlayerService getSummaryCountSizeApi(Handler<AsyncResult<JsonObject>> handler) {
    LOGGER.info(" into get summary count api");
    String query = queryBuilder.buildSummaryCountSizeQuery(catSummaryTable);

    LOGGER.debug(" Query: {} ", query);
    postgresService.executeQuery(
        query,
        allQueryHandler -> {
          if (allQueryHandler.succeeded()) {
            handler.handle(Future.succeededFuture(allQueryHandler.result()));
          } else {
            handler.handle(Future.failedFuture(allQueryHandler.cause()));
          }
        });
    return this;
  }

  @Override
  public MlayerService getRealTimeDataSetApi(Handler<AsyncResult<JsonObject>> handler) {
    LOGGER.info(" into get real time dataset api");
    String query = queryBuilder.buildCountAndSizeQuery(databaseTable, excludedIdsJson);
    LOGGER.debug("Query =  {}", query);

    postgresService.executeQuery(
        query,
        dbHandler -> {
          if (dbHandler.succeeded()) {
            JsonObject results = dbHandler.result();
            handler.handle(Future.succeededFuture(results));
          } else {
            LOGGER.debug("postgres query failed");
            handler.handle(Future.failedFuture(dbHandler.cause()));
          }
        });
    return this;
  }
}
