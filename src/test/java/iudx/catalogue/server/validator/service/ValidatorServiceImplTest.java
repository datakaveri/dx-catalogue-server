//package iudx.catalogue.server.validator.service;
//
//import static iudx.catalogue.server.util.Constants.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//import io.vertx.core.AsyncResult;
//import io.vertx.core.Handler;
//import io.vertx.core.Vertx;
//import io.vertx.core.file.FileSystem;
//import io.vertx.core.json.JsonArray;
//import io.vertx.core.json.JsonObject;
//import io.vertx.junit5.VertxExtension;
//import io.vertx.junit5.VertxTestContext;
//import iudx.catalogue.server.Configuration;
//import iudx.catalogue.server.database.elastic.ElasticClient;
//import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
//import iudx.catalogue.server.database.elastic.service.ElasticsearchServiceImpl;
//import java.util.stream.Stream;
//
//import jdk.jfr.Description;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.Arguments;
//import org.junit.jupiter.params.provider.MethodSource;
//import org.mockito.Mock;
//import org.mockito.invocation.InvocationOnMock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.mockito.stubbing.Answer;
//
//@ExtendWith(MockitoExtension.class)
//@ExtendWith(VertxExtension.class)
//public class ValidatorServiceImplTest {
//
//  static ValidatorServiceImpl validatorService;
//  static AsyncResult<JsonObject> asyncResult;
//  private static Logger LOGGER = LogManager.getLogger(ValidatorServiceImplTest.class);
//  private static Vertx vertxObj;
//  private static ElasticClient client;
//  private static ElasticsearchService esService;
//  private static String databaseIP;
//  private static String docIndex;
//  private static int databasePort;
//  private static String databaseUser;
//  private static String databasePassword;
//  private static FileSystem fileSystem;
//  private static boolean isUacInstance;
//  private static JsonObject jsonMock;
//  private static JsonArray jsonArrayMock;
//  private static Stream<Object> streamMock;
//  private static String vocContext;
//  @Mock Handler<AsyncResult<JsonObject>> handler;
//
//  @BeforeAll
//  @DisplayName("Deploying Verticle")
//  static void startVertx(Vertx vertx, VertxTestContext testContext) {
//    vertxObj = vertx;
//    fileSystem = vertx.fileSystem();
//    JsonObject validatorConfig = Configuration.getConfiguration("./configs/config-test.json", 2);
//
//    /* Configuration setup */
//    databaseIP = validatorConfig.getString(DATABASE_IP);
//    databasePort = validatorConfig.getInteger(DATABASE_PORT);
//    databaseUser = validatorConfig.getString(DATABASE_UNAME);
//    databasePassword = validatorConfig.getString(DATABASE_PASSWD);
//    docIndex = validatorConfig.getString(DOC_INDEX);
//    isUacInstance = false;
//    vocContext = "xyz";
//
//    //    client = new OldElasticClient(databaseIP, databasePort, docIndex, databaseUser,
//    // databasePassword);
//    client = mock(ElasticClient.class);
//    esService = new ElasticsearchServiceImpl(client);
//    validatorService = new ValidatorServiceImpl(esService, docIndex, isUacInstance, vocContext);
//
//    asyncResult = mock(AsyncResult.class);
//    doAnswer(
//            new Answer<AsyncResult<JsonObject>>() {
//              @Override
//              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
//                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
//                return null;
//              }
//            })
//        .when(esService)
//        .search(anyString(), any());
//    testContext.completeNow();
//  }
//
//  private static Stream<Arguments> itemTypes() {
//    return Stream.of(
//        Arguments.of(ITEM_TYPE_RESOURCE),
//        Arguments.of(ITEM_TYPE_RESOURCE_GROUP),
//        Arguments.of(ITEM_TYPE_PROVIDER),
//        Arguments.of(ITEM_TYPE_RESOURCE_SERVER),
//        Arguments.of(ITEM_TYPE_COS),
//        Arguments.of(ITEM_TYPE_OWNER));
//  }
//
//  private static JsonObject requestBody() {
//
//    return new JsonObject()
//        .put(NAME, "name")
//        .put(OWNER, "owner")
//        .put(COS, "cos")
//        .put(RESOURCE_SVR, "rs")
//        .put(PROVIDER, "provider")
//        .put(RESOURCE_GRP, "rg");
//  }
//
//  private static Stream<Arguments> validateItemTypes() {
//    return Stream.of(
//        Arguments.of(ITEM_TYPE_COS, ITEM_TYPE_OWNER, 6),
//        Arguments.of(ITEM_TYPE_RESOURCE_SERVER, ITEM_TYPE_COS, 7),
//        Arguments.of(ITEM_TYPE_PROVIDER, ITEM_TYPE_RESOURCE_SERVER, 8));
//
////            ,
////        Arguments.of(ITEM_TYPE_RESOURCE_GROUP, ITEM_TYPE_PROVIDER, 9));
//  }
//
//  private static Stream<Arguments> parentTypes() {
//    return Stream.of(
//        Arguments.of(ITEM_TYPE_RESOURCE_SERVER, ITEM_TYPE_PROVIDER),
//        Arguments.of(ITEM_TYPE_RESOURCE_SERVER, ITEM_TYPE_RESOURCE_GROUP),
//        Arguments.of(ITEM_TYPE_PROVIDER, ITEM_TYPE_RESOURCE_GROUP)
//    );
//  }
//
//  @ParameterizedTest
//  @MethodSource("itemTypes")
//  @Description("testing the method validateSchema when itemType equals ITEM_TYPE_RESOURCE_SERVER")
//  public void testValidateSchema(String itemType, VertxTestContext vertxTestContext) {
//    validatorService = new ValidatorServiceImpl(esService, docIndex, isUacInstance, vocContext);
//    JsonObject request = new JsonObject();
//    JsonArray jsonArray = new JsonArray();
//    jsonArray.add(itemType);
//    request.put(TYPE, jsonArray);
//    Assertions.assertNotNull(validatorService.validateSchema(request));
//    vertxTestContext.completeNow();
//  }
//
//  @ParameterizedTest
//  @MethodSource("validateItemTypes")
//  @DisplayName("test validate item")
//  public void testValidateItem(
//      String item, String parent, int invocations, VertxTestContext testContext) {
//    JsonObject request = requestBody().put(TYPE, new JsonArray().add(item)).put(HTTP_METHOD, REQUEST_POST).put(PROVIDER_USER_ID, "own-usr-id").put(RESOURCE_SERVER_URL, "fs.iudx.io");
//
//    when(asyncResult.failed()).thenReturn(false);
//    when(asyncResult.result())
//        .thenReturn(
//            new JsonObject()
//                .put(TOTAL_HITS, 1)
//                .put(RESULTS, new JsonArray().add(new JsonObject().put(TYPE, parent))));
//
//    validatorService.validateItem(
//        request).onComplete(
//        handler -> {
//          if (handler.succeeded()) {
//            Assertions.assertTrue(handler.result().containsKey(ID));
//            testContext.completeNow();
//          } else {
//            testContext.failNow("Fail");
//          }
//        });
//  }
//
//  @ParameterizedTest
//  @MethodSource("validateItemTypes")
//  @DisplayName("failure test validate item: DB error")
//  public void testValidateItemDbError(
//      String item, String parent, int invocations, VertxTestContext testContext) {
//    JsonObject request = requestBody().put(TYPE, new JsonArray().add(item)).put(HTTP_METHOD, REQUEST_POST).put(PROVIDER_USER_ID, "own-usr-id").put(RESOURCE_SERVER_URL, "fs.iudx.io");
//
//
//    when(asyncResult.failed()).thenReturn(true);
//
//    validatorService.validateItem(
//        request).onComplete(
//        handler -> {
//          if (handler.failed()) {
//            Assertions.assertEquals("Validation failed", handler.cause().getMessage());
//            testContext.completeNow();
//          } else {
//            testContext.failNow("failed to invalidate");
//          }
//        });
//  }
//
//  @ParameterizedTest
//  @MethodSource("validateItemTypes")
//  @DisplayName("failure test validate item: parent does not exist")
//  public void testValidateItemParentNotExists(
//      String item, String parent, int invocations, VertxTestContext testContext) {
//    JsonObject request = requestBody().put(TYPE, new JsonArray().add(item)).put(HTTP_METHOD, REQUEST_POST).put(PROVIDER_USER_ID, "own-usr-id").put(RESOURCE_SERVER_URL, "fs.iudx.io");
//
//    when(asyncResult.failed()).thenReturn(false);
//    when(asyncResult.result())
//        .thenReturn(
//            new JsonObject()
//                .put(TOTAL_HITS, 0)
//                .put(RESULTS, new JsonArray().add(new JsonObject())));
//
//    validatorService.validateItem(
//        request).onComplete(
//        handler -> {
//          if (handler.failed()) {
//            LOGGER.error(handler.cause());
//            Assertions.assertTrue(handler.cause().getMessage().contains("doesn't exist"));
//            testContext.completeNow();
//          } else {
//            testContext.failNow("Fail");
//          }
//        });
//  }
//
//  @Test
//  @DisplayName("Test validate owner item")
//  public void testValidateOwnerItem(VertxTestContext testContext) {
//    JsonObject request = requestBody();
//    request.put(TYPE, new JsonArray().add(ITEM_TYPE_OWNER));
//    request.put(HTTP_METHOD, REQUEST_POST);
//
//    doAnswer(
//            new Answer<AsyncResult<JsonObject>>() {
//              @Override
//              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
//                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
//                return null;
//              }
//            })
//        .when(esService)
//        .search(anyString(), any());
//
//    when(asyncResult.failed()).thenReturn(false);
//    when(asyncResult.result()).thenReturn(new JsonObject().put(TOTAL_HITS, 0));
//
//    validatorService.validateItem(
//        request).onComplete(
//        handler -> {
//          if (handler.succeeded()) {
//            verify(esService, times(1)).search(anyString(), any());
//            testContext.completeNow();
//          } else {
//            testContext.failNow("Failed to invalidate");
//          }
//        });
//  }
//
//  @Test
//  @Description("testing the method validateItem when item type mismatch")
//  public void testValidateItemCatch(VertxTestContext vertxTestContext) {
//    JsonObject request = new JsonObject();
//    validatorService.validateItem(
//        request).onComplete(
//        handler -> {
//          if (handler.failed()) {
//            Assertions.assertEquals("Invalid Item Type", handler.cause().getMessage());
//            vertxTestContext.completeNow();
//          } else {
//            vertxTestContext.failNow("failed to invalidate");
//          }
//        });
//  }
//
//  @Test
//  @Description("testing the method validateItem when handler failed")
//  public void testValidateItemFailure(VertxTestContext vertxTestContext) {
//    JsonObject request =
//        new JsonObject()
//            .put(NAME, "name")
//            .put(OWNER, "owner")
//            .put(COS, "cos")
//            .put(RESOURCE_SVR, "rs")
//            .put(PROVIDER, "provider")
//            .put(RESOURCE_GRP, "rg")
//            .put(TYPE, new JsonArray().add(ITEM_TYPE_RESOURCE));
//
//    when(asyncResult.failed()).thenReturn(true);
//
//    validatorService.validateItem(
//        request).onComplete(
//        handler -> {
//          if (handler.failed()) {
//            verify(esService, times(7)).search(anyString(), any());
//            vertxTestContext.completeNow();
//          } else {
//            vertxTestContext.failNow("Fail");
//          }
//        });
//  }
//
//  @ParameterizedTest
//  @MethodSource("parentTypes")
//  @DisplayName("failure test validate item: ri parent not exists")
//  public void testResourceParentNotExists(String parent1,String parent2, VertxTestContext testContext) {
//    JsonObject request = requestBody().put(TYPE, new JsonArray().add(ITEM_TYPE_RESOURCE));
//
//    when(asyncResult.failed()).thenReturn(false);
//    when(asyncResult.result())
//        .thenReturn(
//            new JsonObject()
//                .put(TOTAL_HITS, 0)
//                .put(
//                    RESULTS,
//                    new JsonArray()
//                        .add(new JsonObject().put(TYPE, parent1))
//                        .add(new JsonObject().put(TYPE, parent2))));
//
//    validatorService.validateItem(
//        request).onComplete(
//        handler -> {
//          if (handler.failed()) {
//            Assertions.assertTrue(handler.cause().getMessage().contains("doesn't exist"));
//            testContext.completeNow();
//          } else {
//            testContext.failNow("Failed to invalidate");
//          }
//        });
//  }
//}
