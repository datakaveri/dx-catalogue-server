package iudx.catalogue.server.mlayer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.postgresql.hostchooser.HostRequirement.master;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.database.postgres.PostgresService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class MlayerServiceTest {
  static MlayerServiceImpl mlayerService;
  @Mock static DatabaseService databaseService;
  @Mock static PostgresService postgresService;
  private static Logger LOGGER = LogManager.getLogger(MlayerServiceTest.class);
  @Mock private static AsyncResult<JsonObject> asyncResult;
  private static String tableName = "database Table";
  private static String catSummaryTable = "cat_summary";
  private static JsonArray jsonArray = new JsonArray().add("excluded_ids").add("excluded_ids2");
  JsonObject jsonObject = new JsonObject()
          .put("databaseTable", tableName)
          .put("catSummaryTable",catSummaryTable)
          .put("excluded_ids",jsonArray);
  private static Vertx vertxObj;
  @Mock JsonObject json;

  private JsonObject requestJson() {
    return new JsonObject()
        .put("name", "pune")
        .put("cover", "path of cover.jpg")
        .put("icon", "path of icon.jpg")
        .put("logo", "path og logo.jpg");
  }

  @Test
  @DisplayName("Success: test create mlayer instance")
  void successfulMlayerInstanceCreationTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);

    json = requestJson();
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(new JsonObject());

    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .createMlayerInstance(any(), any());
    mlayerService.createMlayerInstance(
        json,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).createMlayerInstance(any(), any());
            testContext.completeNow();
          } else {
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Failure: test create mlayer instance")
  void failureMlayerInstanceCreationTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);
    JsonObject request = requestJson();
    when(asyncResult.succeeded()).thenReturn(false);
    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .createMlayerInstance(any(), any());

    mlayerService.createMlayerInstance(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).createMlayerInstance(any(), any());
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          } else {
            testContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Success: test get all mlayer instance")
  void successfulMlayerInstanceGetTest(VertxTestContext testContext) {

      JsonObject requestParams = new JsonObject(); //.put("id", ID).put("limit", LIMIT).put("offset",OFFSET)


    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);

    String id = "abc";
    when(asyncResult.succeeded()).thenReturn(true);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .getMlayerInstance(any(), any());
    mlayerService.getMlayerInstance(
        requestParams,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).getMlayerInstance(any(), any());
            testContext.completeNow();
          } else {
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Failure: test get all mlayer instance")
  void failureMlayerInstanceGetTest(VertxTestContext testContext) {

      JsonObject requestParams = new JsonObject();

    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);

    when(asyncResult.succeeded()).thenReturn(false);
    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .getMlayerInstance(any(), any());

    mlayerService.getMlayerInstance(
        requestParams,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).getMlayerInstance(any(), any());
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          } else {
            testContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Success: test delete mlayer instance")
  void successfulMlayerInstanceDeleteTest(VertxTestContext testContext) {
    String request = "dummy";
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);

    when(asyncResult.succeeded()).thenReturn(true);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .deleteMlayerInstance(any(), any());

    mlayerService.deleteMlayerInstance(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).deleteMlayerInstance(any(), any());
            testContext.completeNow();
          } else {
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Failure: test delete mlayer instance")
  void failureMlayerInstanceDeleteTest(VertxTestContext testContext) {
    String request = "dummy";
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);

    when(asyncResult.succeeded()).thenReturn(false);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .deleteMlayerInstance(any(), any());

    mlayerService.deleteMlayerInstance(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).deleteMlayerInstance(any(), any());
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          } else {
            testContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Success: test update mlayer instance")
  void successfulMlayerInstanceUpdateTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request.put("name", "instance name");
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);

    when(asyncResult.succeeded()).thenReturn(true);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .updateMlayerInstance(any(), any());

    mlayerService.updateMlayerInstance(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).updateMlayerInstance(any(), any());
            testContext.completeNow();
          } else {
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Failure: test update mlayer instance")
  void failureMlayerInstanceUpdateTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request.put("name", "instance name");
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);

    when(asyncResult.succeeded()).thenReturn(false);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .updateMlayerInstance(any(), any());

    mlayerService.updateMlayerInstance(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).updateMlayerInstance(any(), any());
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          } else {
            LOGGER.debug("Fail");
            testContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Success: test create mlayer domain")
  void successMlayerDomainCreateTest(VertxTestContext testContext) {
    JsonObject jsonObject = new JsonObject();
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);

    jsonObject.put("name", "dummy");
    when(asyncResult.succeeded()).thenReturn(true);

    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .createMlayerDomain(any(), any());
    mlayerService.createMlayerDomain(
        jsonObject,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).createMlayerDomain(any(), any());
            testContext.completeNow();

          } else {
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Failure: test create mlayer domain")
  void failureMlayerDomainCreateTest(VertxTestContext testContext) {
    JsonObject jsonObject = new JsonObject();
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);

    jsonObject.put("name", "dummy");
    when(asyncResult.succeeded()).thenReturn(false);

    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .createMlayerDomain(any(), any());
    mlayerService.createMlayerDomain(
        jsonObject,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).createMlayerDomain(any(), any());
            testContext.failNow(handler.cause());

          } else {
            LOGGER.debug("Fail");
            testContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Success: test update mlayer domain")
  void successMlayerDomainUpdateTest(VertxTestContext testContext) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("name", "dummy");
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);

    when(asyncResult.succeeded()).thenReturn(true);

    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .updateMlayerDomain(any(), any());
    mlayerService.updateMlayerDomain(
        jsonObject,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).updateMlayerDomain(any(), any());
            testContext.completeNow();

          } else {
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Failure: test update mlayer domain")
  void failureMlayerDomainUpdateTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);

    request.put("name", "instance name");
    when(asyncResult.succeeded()).thenReturn(false);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .updateMlayerDomain(any(), any());

    mlayerService.updateMlayerDomain(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).updateMlayerDomain(any(), any());
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          } else {
            LOGGER.debug("Fail");
            testContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Success: test delete mlayer domain")
  void successfulMlayerDomainDeleteTest(VertxTestContext testContext) {
    String request = "dummy";
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);

    when(asyncResult.succeeded()).thenReturn(true);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .deleteMlayerDomain(any(), any());

    mlayerService.deleteMlayerDomain(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).deleteMlayerDomain(any(), any());
            testContext.completeNow();
          } else {
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Failure: test delete mlayer domain")
  void failureMlayerDomainDeleteTest(VertxTestContext testContext) {
    String request = "dummy";
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);

    when(asyncResult.succeeded()).thenReturn(false);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .deleteMlayerDomain(any(), any());

    mlayerService.deleteMlayerDomain(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).deleteMlayerDomain(any(), any());
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          } else {
            testContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Success: test get all mlayer domain")
  void successfulMlayerDomainGetTest(VertxTestContext testContext) {
      JsonObject requestParams = new JsonObject();
    when(asyncResult.succeeded()).thenReturn(true);
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);

    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .getMlayerDomain(any(), any());
    mlayerService.getMlayerDomain(
        requestParams,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).getMlayerDomain(any(), any());
            testContext.completeNow();
          } else {
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Failure: test get all mlayer domain")
  void failureMlayerDomainGetTest(VertxTestContext testContext) {
      JsonObject requestParams = new JsonObject();
    when(asyncResult.succeeded()).thenReturn(false);
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);

    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .getMlayerDomain(any(), any());

    mlayerService.getMlayerDomain(
        requestParams,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).getMlayerDomain(any(), any());
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          } else {
            testContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Success: test get all mlayer providers")
  void successfulMlayerProvidersGetTest(VertxTestContext testContext) {

      JsonObject requestParams = new JsonObject();

    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);


    when(asyncResult.succeeded()).thenReturn(true);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .getMlayerProviders(any(),any());
    mlayerService.getMlayerProviders(
            requestParams,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).getMlayerProviders(any(),any());
            testContext.completeNow();
          } else {
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Failure: test get all mlayer providers")
  void failureMlayerProvidersGetTest(VertxTestContext testContext) {

      JsonObject requestParams = new JsonObject();


    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);

    when(asyncResult.succeeded()).thenReturn(false);
    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .getMlayerProviders(any(),any());

    mlayerService.getMlayerProviders(
            requestParams,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).getMlayerProviders(any(),any());
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          } else {
            testContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Success: test get dataset location and label")
  void successfulMlayerGeoQueryGetTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);

    JsonObject request = new JsonObject();
    when(asyncResult.succeeded()).thenReturn(true);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .getMlayerGeoQuery(any(), any());
    mlayerService.getMlayerGeoQuery(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).getMlayerGeoQuery(any(), any());
            testContext.completeNow();
          } else {
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Failure: test get dataset location and label")
  void failureMlayerGeoQueryGetTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);

    JsonObject request = new JsonObject();
    when(asyncResult.succeeded()).thenReturn(false);
    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .getMlayerGeoQuery(any(), any());

    mlayerService.getMlayerGeoQuery(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).getMlayerGeoQuery(any(), any());
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          } else {
            testContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Success: test get all datasets")
  void successfulGetMlayerAllDatasetsTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);

    JsonObject request = new JsonObject();
    when(asyncResult.succeeded()).thenReturn(true);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .getMlayerAllDatasets(any(),any(), any());
    mlayerService.getMlayerAllDatasets(
       request, handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).getMlayerAllDatasets(any(), any(), any());
            testContext.completeNow();
          } else {
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Failure: test get all datasets")
  void failureMlayerAllDatasetsTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);

    JsonObject request = new JsonObject();
    when(asyncResult.succeeded()).thenReturn(false);
    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .getMlayerAllDatasets(any(), any(), any());

    mlayerService.getMlayerAllDatasets(
        request, handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).getMlayerAllDatasets(any(), any(), any());
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          } else {
            testContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Success: test get dataset and its resources details")
  void successMlayerDatasetAndResourcesTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);
    JsonObject request = new JsonObject().put("id", "dummy id");
    when(asyncResult.succeeded()).thenReturn(true);
    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .getMlayerDataset(any(), any());
    mlayerService.getMlayerDataset(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).getMlayerDataset(any(), any());
            testContext.completeNow();

          } else {
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Failure: test get dataset and its resources details")
  void failureMlayerDatasetAndResourcesTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);
    JsonObject request = new JsonObject().put("id", "dummy id");
    when(asyncResult.succeeded()).thenReturn(false);
    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .getMlayerDataset(any(), any());
    mlayerService.getMlayerDataset(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).getMlayerDataset(any(), any());
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          } else {
            testContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Success: test get dataset details")
  void successMlayerDatasetTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);
    JsonArray tags = new JsonArray().add("flood");
    JsonArray providers = new JsonArray().add("26005f3b-a6a0-4edb-ae28-70474b4ef90c");
    JsonObject request =
        new JsonObject()
            .put("instance", "pune")
            .put("tags", tags)
            .put("providers", providers)
            .put("domains", tags);
    when(asyncResult.succeeded()).thenReturn(true);
    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .getMlayerAllDatasets(any(), any(), any());
    mlayerService.getMlayerDataset(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).getMlayerAllDatasets(any(), any(), any());
            testContext.completeNow();

          } else {
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Failure: test get dataset details")
  void failureMlayerDatasetInvalidParamTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);
    JsonObject request = new JsonObject().put("instances", "pune");
    mlayerService.getMlayerDataset(
        request,
        handler -> {
          if (handler.succeeded()) {
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());

          } else {
            testContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Failure: test get dataset details")
  void failureMlayerDatasetTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);
    JsonArray tags = new JsonArray().add("flood");
    JsonArray providers = new JsonArray().add("providerId");
    JsonObject request =
        new JsonObject()
            .put("instance", "dummy value")
            .put("tags", tags)
            .put("providers", providers);
    when(asyncResult.succeeded()).thenReturn(false);
    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .getMlayerAllDatasets(any(), any(), any());
    mlayerService.getMlayerDataset(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).getMlayerAllDatasets(any(), any(), any());
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          } else {
            testContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Success: test get overview detail")
  void successfulGetMlayerOverviewTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);
    JsonArray jsonArray = new JsonArray();
    JsonObject json = new JsonObject();
    json.put("results", jsonArray);
    json.put("resource_grpup", "abc");
    jsonArray.add(json);
    String instanceName = "dummy";
    when(asyncResult.result()).thenReturn(json);

    when(asyncResult.succeeded()).thenReturn(true);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .getMlayerPopularDatasets(any(), any(), any());
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(postgresService)
        .executeQuery(any(), any());

    mlayerService.getMlayerPopularDatasets(
        instanceName,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).getMlayerPopularDatasets(any(), any(), any());
            verify(postgresService, times(1)).executeQuery(any(), any());

            testContext.completeNow();
          } else {
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Fail: test get overview detail when postgres query fails")
  void failedPostgresQueryTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);
    JsonArray jsonArray = new JsonArray();
    JsonObject json = new JsonObject();
    json.put("results", jsonArray);
    jsonArray.add("dataset");
    String instance = "";

    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(postgresService)
        .executeQuery(any(), any());

    mlayerService.getMlayerPopularDatasets(
        instance,
        handler -> {
          if (handler.failed()) {
            verify(postgresService, times(1)).executeQuery(any(), any());
            testContext.completeNow();
          } else {
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Success: Get Summary Count Api")
  public void successGetTotalCountApi(VertxTestContext vertxTestContext) {
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);

    JsonArray jsonArray = new JsonArray();
    JsonObject json = new JsonObject();
    json.put("results", jsonArray);
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("counts", 122343243);
    jsonArray.add(jsonObject);
    when(asyncResult.result()).thenReturn(json);

    when(asyncResult.succeeded()).thenReturn(true);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(postgresService)
        .executeQuery(any(), any());

    mlayerService.getSummaryCountSizeApi(
        handler -> {
          if (handler.succeeded()) {
            assertEquals(handler.result(), json);
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Fail: Get Summary Count Api")
  void failGetTotalCountApi(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(postgresService)
        .executeQuery(any(), any());

    mlayerService.getSummaryCountSizeApi(
        handler -> {
          if (handler.failed()) {
            verify(postgresService, times(1)).executeQuery(any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Success: Get  Count Size Api")
  public void successGetCountSizeApi(VertxTestContext vertxTestContext) {
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);

    JsonArray jsonArray = new JsonArray();
    JsonObject json = new JsonObject();
    json.put("results", jsonArray);
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("month", "december");
    jsonObject.put("year", 2023);
    jsonObject.put("counts", 456);
    jsonObject.put("total_size", 122343243);
    jsonArray.add(jsonObject);
    when(asyncResult.result()).thenReturn(json);

    when(asyncResult.succeeded()).thenReturn(true);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(postgresService)
        .executeQuery(any(), any());

    mlayerService.getRealTimeDataSetApi(
        handler -> {
          if (handler.succeeded()) {
            assertEquals(handler.result(), json);
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Fail: Get Count Size Api")
  void failGetCountSizeApi(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(postgresService)
        .executeQuery(any(), any());

    mlayerService.getRealTimeDataSetApi(
        handler -> {
          if (handler.failed()) {
            verify(postgresService, times(1)).executeQuery(any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow(handler.cause());
          }
        });
  }

    @Test
    @DisplayName("Success: Get Count Size Api")
    public void successGetCountSizeApi2(VertxTestContext vertxTestContext) {
        jsonObject.put("excluded_ids", new JsonArray());
        mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);

        JsonArray jsonArray = new JsonArray();
        JsonObject json = new JsonObject();
        json.put("results", jsonArray);
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("month", "december");
        jsonObject.put("year", 2023);
        jsonObject.put("counts", 456);
        jsonObject.put("total_size", 122343243);
        jsonArray.add(jsonObject);
        when(asyncResult.result()).thenReturn(json);

        when(asyncResult.succeeded()).thenReturn(true);
        doAnswer(
                new Answer<AsyncResult<JsonObject>>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                        return null;
                    }
                })
                .when(postgresService)
                .executeQuery(any(), any());

        mlayerService.getRealTimeDataSetApi(
                handler -> {
                    if (handler.succeeded()) {
                        assertEquals(handler.result(), json);
                        vertxTestContext.completeNow();
                    } else {
                        vertxTestContext.failNow(handler.cause());
                    }
                });
    }
}
