package iudx.catalogue.server.mlayer;

import static iudx.catalogue.server.database.Constants.KEY;
import static iudx.catalogue.server.mlayer.util.Constants.MLAYER_ID;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.util.Constants.OFFSET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.database.ElasticClient;
import iudx.catalogue.server.database.postgres.PostgresService;
import iudx.catalogue.server.mlayer.vocabulary.DataModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
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
  private static Vertx vertxObj;
  @Mock ElasticClient elasticClient;
  @Mock WebClient webClient;
  @Mock DataModel dataModel;
  JsonObject jsonObject =
      new JsonObject()
          .put("databaseTable", tableName)
          .put("catSummaryTable", catSummaryTable)
          .put("excluded_ids", jsonArray);
  @Mock JsonObject json;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    if (webClient == null) {
      throw new NullPointerException("webClient is null");
    }
    dataModel = new DataModel(elasticClient, "test-index", webClient);
  }

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

    JsonObject requestParams = new JsonObject();

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
        .getMlayerProviders(any(), any());
    mlayerService.getMlayerProviders(
        requestParams,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).getMlayerProviders(any(), any());
            testContext.completeNow();
          } else {
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Success: test get all mlayer providers with instance")
  void successfulMlayerProvidersGetWithInstanceTest(VertxTestContext testContext) {

    JsonObject requestParams =
        new JsonObject().put("instance", "pune").put("limit", 0).put("offset", 1);
    JsonObject request =
        new JsonObject()
            .put(
                "results",
                new JsonArray()
                    .add(
                        new JsonObject()
                            .put("providerCount", 2)
                            .put(
                                "resourceGroupAndProvider",
                                new JsonArray()
                                    .add(
                                        new JsonObject()
                                            .put("type", new JsonArray().add("iudx:Provider"))
                                            .put("description", "dummy")
                                            .put("id", "id")
                                            .put("itemCreatedAt", "time"))
                                    .add(
                                        new JsonObject()
                                            .put("type", new JsonArray().add("iudx:Provider"))
                                            .put("description", "dummy")
                                            .put("id", "id")
                                            .put("itemCreatedAt", "time")))));
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);

    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
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
        .getMlayerProviders(any(), any());
    mlayerService.getMlayerProviders(
        requestParams,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).getMlayerProviders(any(), any());
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
        .getMlayerProviders(any(), any());

    mlayerService.getMlayerProviders(
        requestParams,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).getMlayerProviders(any(), any());
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          } else {
            testContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Failure: test get all mlayer providers with instance")
  void failureMlayerProvidersGetWithInstanceTest(VertxTestContext testContext) {

    JsonObject requestParams = new JsonObject().put("instance", "abc");

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
        .getMlayerProviders(any(), any());

    mlayerService.getMlayerProviders(
        requestParams,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).getMlayerProviders(any(), any());
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
    JsonArray id = new JsonArray();
    id.add(0, "dummy id");
    request.put(INSTANCE, "instance").put(MLAYER_ID, id);
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
    JsonArray id = new JsonArray();
    id.add(0, "dummy id");
    request.put(INSTANCE, "instance").put(MLAYER_ID, id);
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
    JsonObject resourceGrpList = new JsonObject();
    JsonArray typeArray = new JsonArray().add(0, "iudx:Provider");
    JsonObject record =
        new JsonObject()
            .put("id", "dummy-id")
            .put("instance", "instance")
            .put("description", "dummy-desc")
            .put("resourceServerRegURL", "abc/abc/abc")
            .put("provider", "")
            .put("cosURL", "abc/abc")
            .put("itemCreatedAt", "2022-12-15T04:23:28+0530");
    JsonArray results =
        new JsonArray()
            .add(0, record.put("type", new JsonArray().add(0, "iudx:ResourceGroup")))
            .add(1, record.put("type", typeArray))
            .add(2, record.put("type", new JsonArray().add(0, "iudx:ResourceGroup")));
    resourceGrpList.put("results", results);
    JsonObject instances =
        new JsonObject()
            .put(
                "results",
                new JsonArray()
                    .add(
                        0,
                        new JsonObject()
                            .put("name", "dummy")
                            .put("icon", "abc.jpg")
                            .put(TYPE, typeArray)));
    JsonObject resourceAndPolicyCnt =
        new JsonObject()
            .put(
                "results",
                new JsonArray()
                    .add(
                        0,
                        new JsonObject()
                            .put("key", "19390c5-30c0-4339-b0f2-1be292312104")
                            .put("doc_count", 2)
                            .put(TYPE, typeArray)
                            .put(
                                "access_policies",
                                new JsonObject()
                                    .put(
                                        "buckets",
                                        new JsonArray()
                                            .add(
                                                0,
                                                new JsonObject()
                                                    .put(
                                                        "key",
                                                        "19390c5-30c0-4339-b0f2-1be292312104")
                                                    .put("doc_count", 2))
                                            .add(
                                                1,
                                                new JsonObject()
                                                    .put("key", "dummy")
                                                    .put("doc_count", 2)))))
                    .add(
                        1,
                        new JsonObject()
                            .put("key", "19390c5-30c0-4339-b0f2-1be292312104")
                            .put("doc_count", 2)
                            .put(
                                "access_policies",
                                new JsonObject()
                                    .put(
                                        "buckets",
                                        new JsonArray()
                                            .add(
                                                0,
                                                new JsonObject()
                                                    .put(
                                                        "key",
                                                        "89390c5-30c0-4339-b0f2-1be292312104")
                                                    .put("doc_count", 2))
                                            .add(
                                                1,
                                                new JsonObject()
                                                    .put("key", "dummy")
                                                    .put("doc_count", 2))))));
    request
        .put("resourceGroupList", resourceGrpList)
        .put("instanceResult", instances)
        .put("resourceAndPolicyCount", resourceAndPolicyCnt)
        .put("idAndDomainList", new JsonObject())
        .put(LIMIT, 0)
        .put(OFFSET, 0);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
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
        .getMlayerAllDatasets(any(), any(), any());
    mlayerService.getMlayerAllDatasets(
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
  @DisplayName("Failure: test get all datasets when hits are zero")
  void MlayerAllDatasetsTest0Hitsfailure(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);

    JsonObject resourceGroupList = new JsonObject();
    JsonArray results = new JsonArray();
    resourceGroupList.put("results", results);
    JsonObject request =
        new JsonObject()
            .put("instanceResult", new JsonObject().put("results", results))
            .put("resourceGroupList", resourceGroupList)
            .put("resourceAndPolicyCount", new JsonObject().put("results", results));
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
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
  @DisplayName("Success: test get dataset and its resources details")
  void successMlayerDatasetAndResourcesTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(databaseService, postgresService, jsonObject);
    JsonArray results = new JsonArray();
    JsonObject json = new JsonObject().put("instance", "pune");
    JsonArray jsonArray = new JsonArray();
    JsonObject provider =
        new JsonObject()
            .put("provider", "dummy id")
            .put("cos", "cis id")
            .put("dataset", json)
            .put("resource", jsonArray);
    results.add(0, provider);
    JsonObject request = new JsonObject().put(TOTAL_HITS, 0).put(ID, "dummy").put(RESULTS, results);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
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
        .getProviderAndResourceServerId(any(), any());
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
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
        .getDataset(any(), any());
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
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
        .getInstanceIcon(any(), any());
    mlayerService.getMlayerDataset(
        new JsonObject().put("id", "dummy-id"),
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).getInstanceIcon(any(), any());
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
        .getProviderAndResourceServerId(any(), any());
    mlayerService.getMlayerDataset(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).getInstanceIcon(any(), any());
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
    JsonObject request = new JsonObject();
    JsonObject resourceGroupList = new JsonObject();
    JsonObject resourceAndPolicyCount = new JsonObject();
    JsonObject resourceGrpList = new JsonObject();
    JsonArray typeArray = new JsonArray().add(0, "iudx:Provider");
    JsonObject record =
        new JsonObject()
            .put("id", "dummy-id")
            .put("description", "dummy-desc")
            .put("resourceServerRegUrl", "abc/abc/abc")
            .put("provider", "")
            .put("cos", "")
            .put(TYPE, typeArray);
    JsonArray results = new JsonArray().add(0, record).add(1, record).add(2, record);
    resourceGrpList.put("results", results);
    resourceGroupList
        .put("resourceGroupCount", 2)
        .put(
            "resourceGroup",
            new JsonArray()
                .add(
                    new JsonObject()
                        .put("type", new JsonArray().add("dummy"))
                        .put("description", "dummy")
                        .put("id", "id")
                        .put("instance", "dummy"))
                .add(
                    new JsonObject()
                        .put("type", new JsonArray().add("exampleType"))
                        .put("description", "example_description")
                        .put("id", "example-id")
                        .put("instance", "dummy")));
    resourceAndPolicyCount
        .put("resourceItemCount", new JsonObject())
        .put("resourceAccessPolicy", new JsonObject().put("id", new JsonObject()));
    JsonObject instances =
        new JsonObject()
            .put(
                "results",
                new JsonArray()
                    .add(
                        0,
                        new JsonObject()
                            .put("name", "dummy")
                            .put("icon", "abc.jpg")
                            .put(TYPE, typeArray)));
    JsonObject resourceAndPolicyCnt =
        new JsonObject()
            .put(
                "results",
                new JsonArray()
                    .add(
                        0,
                        new JsonObject()
                            .put("key", "19390c5-30c0-4339-b0f2-1be292312104")
                            .put("doc_count", 2)
                            .put(TYPE, typeArray)
                            .put(
                                "access_policies",
                                new JsonObject()
                                    .put(
                                        "buckets",
                                        new JsonArray()
                                            .add(
                                                0,
                                                new JsonObject()
                                                    .put(
                                                        "key",
                                                        "19390c5-30c0-4339-b0f2-1be292312104")
                                                    .put("doc_count", 2))
                                            .add(
                                                1,
                                                new JsonObject()
                                                    .put("key", "dummy")
                                                    .put("doc_count", 2)))))
                    .add(
                        1,
                        new JsonObject()
                            .put("key", "19390c5-30c0-4339-b0f2-1be292312104")
                            .put("doc_count", 2)
                            .put(
                                "access_policies",
                                new JsonObject()
                                    .put(
                                        "buckets",
                                        new JsonArray()
                                            .add(
                                                0,
                                                new JsonObject()
                                                    .put(
                                                        "key",
                                                        "89390c5-30c0-4339-b0f2-1be292312104")
                                                    .put("doc_count", 2))
                                            .add(
                                                1,
                                                new JsonObject()
                                                    .put("key", "dummy")
                                                    .put("doc_count", 2))))));
    request
        .put("instance", "pune")
        .put("tags", tags)
        .put("providers", providers)
        .put("domains", tags)
        .put("resourceGroupList", resourceGrpList)
        .put("instanceResult", instances)
        .put("resourceAndPolicyCount", resourceAndPolicyCnt)
        .put(
            "idAndDomainList",
            new JsonObject().put("dummy-id-1", "domain-1").put("dummy-id-2", "domain-2"))
        .put(LIMIT, 0)
        .put(OFFSET, 0);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
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
    jsonArray.add("dataset");
    String instanceName = "dummy";
    JsonArray accessPolicy =
        new JsonArray()
            .add(
                0,
                new JsonObject()
                    .put(KEY, "719390c5-30c0-4339-b0f2-1be292312104")
                    .put("doc_count", 2))
            .add(
                1,
                new JsonObject()
                    .put(KEY, "719390c5-30c0-4339-b0f2-1be292312104")
                    .put("doc_count", 2));
    JsonObject accessPolicyJson =
        new JsonObject()
            .put("resource_group", "abcd/abcd/abcd/abcd")
            .put("instance", "instance")
            .put(BUCKETS, accessPolicy)
            .put("resourceGroup", "abc");
    JsonObject json2 =
        new JsonObject()
            .put("resource group", "abcd/abcd/abcd/abcd")
            .put("instance", "instance")
            .put(BUCKETS, accessPolicy)
            .put("resourceGroup", "abc");

    JsonArray highestCountResource = new JsonArray().add(accessPolicyJson).add(json2);

    JsonArray resourceArray = new JsonArray();
    JsonArray typeArray = new JsonArray().add(0, "iudx:Provider");

    JsonObject instance =
        new JsonObject()
            .put("name", "agra")
            .put("icon", "path_of_agra-icon.jpg")
            .put(TYPE, typeArray)
            .put("id", "abcd/abcd/abcd/abcd")
            .put("rgid", "abcd/abcd/abcd/abcd")
            .put("instance", "instance")
            .put("itemCreatedAt", "2023-08-30T05:09:54+0530")
            .put(KEY, "719390c5-30c0-4339-b0f2-1be292312104")
            .put("doc_count", 2)
            .put(KEY, accessPolicyJson)
            .put("access_policies", accessPolicyJson)
            .put("resourceGroupAndProvider", resourceArray)
            .put("providerCount", 7);
    resourceArray.add(instance).add(instance).add(instance).add(instance).add(instance);
    JsonArray latestDataset = new JsonArray().add(instance);
    JsonArray resultArray =
        new JsonArray().add(instance).add(instance).add(instance).add(instance).add(instance);

    JsonObject result =
        new JsonObject()
            .put("instanceList", new JsonObject().put(TOTAL_HITS, 1).put(RESULTS, resultArray))
            .put("domainList", new JsonArray())
            .put(
                "datasetJson",
                new JsonObject()
                    .put("results", latestDataset)
                    .put("latestDataset", latestDataset)
                    .put("featuredDataset", latestDataset)
                    .put(
                        "cat_results",
                        new JsonArray()
                            .add(
                                0,
                                new JsonObject()
                                    .put("description", "dummy-data")
                                    .put("id", "id")
                                    .put("type", typeArray))
                            .add(
                                1,
                                new JsonObject()
                                    .put("description", "dummy-data")
                                    .put("id", "id")
                                    .put("type", typeArray))
                            .add(
                                2,
                                new JsonObject()
                                    .put("id", "id")
                                    .put("type", new JsonArray().add(0, "iudx:ResourceGroup"))
                                    .put("itemCreatedAt", "2023-08-30T05:09:54+0530"))
                            .add(
                                3,
                                new JsonObject()
                                    .put("id", "id")
                                    .put("type", new JsonArray().add(0, "iudx:ResourceGroup"))
                                    .put("itemCreatedAt", "2023-08-30T05:09:54+0530")))
                    .put("resultSize", 4)
                    .put("frequentlyUsedResourceGroup", highestCountResource));
    JsonObject results = new JsonObject().put("results", highestCountResource);
    AsyncResult<JsonObject> asyncResult1 = Future.succeededFuture(results);

    when(asyncResult.result()).thenReturn(result);
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
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult1);
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

  @Test
  @DisplayName("Success: Get Data Model Info")
  public void testGetDataModelInfo(VertxTestContext testContext) {
    // Mock response from ElasticSearch
    JsonObject mockResponse =
        new JsonObject()
            .put(
                "results",
                new JsonArray()
                    .add(
                        new JsonObject()
                            .put("id", "test-id")
                            .put("@context", "http://example.com/")
                            .put("type", new JsonArray().add("iudx:Type1").add("iudx:Class1"))));

    // Mock response from WebClient
    JsonObject mockDmJson =
        new JsonObject()
            .put(
                "@graph",
                new JsonArray()
                    .add(
                        new JsonObject()
                            .put("@id", "iudx:Class1")
                            .put(
                                "rdfs:subClassOf",
                                new JsonObject().put("@id", "iudx:SuperClass"))));

    // Stub ElasticClient behavior
    when(elasticClient.searchAsync(any(), any(), any()))
        .thenAnswer(
            invocation -> {
              Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(2);
              handler.handle(Future.succeededFuture(mockResponse));
              return null;
            });

    // Mock WebClient behavior
    HttpRequest<Buffer> mockHttpRequest = mock(HttpRequest.class);
    HttpResponse<Buffer> mockHttpResponse = mock(HttpResponse.class);
    // Creating a mock MultiMap to return from the headers() method
    MultiMap mockHeaders = MultiMap.caseInsensitiveMultiMap();
    mockHeaders.add("content-type", "application/json");

    when(mockHttpResponse.body()).thenReturn(Buffer.buffer(mockDmJson.encode()));
    when(mockHttpResponse.headers()).thenReturn(mockHeaders);

    doAnswer(
            invocation -> {
              Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(0);
              handler.handle(Future.succeededFuture(mockHttpResponse));
              LOGGER.debug("Mocking HttpRequest send method");
              return null;
            })
        .when(mockHttpRequest)
        .send(any());

    when(webClient.getAbs(anyString())).thenReturn(mockHttpRequest);
    // Adding a debug statement to verify the mock setup
    LOGGER.debug("Mock setup complete. Starting test.");
    dataModel
        .getDataModelInfo()
        .onComplete(
            ar -> {
              if (ar.failed()) {
                testContext.failNow(ar.cause());
              } else {
                JsonObject result = ar.result();
                assertEquals("SuperClass", result.getString("test-id"));
                testContext.completeNow();
              }
            });
    // Verify that webClient.getAbs was called
    verify(webClient).getAbs(anyString());
  }
}
