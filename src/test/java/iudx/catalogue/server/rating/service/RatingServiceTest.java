package iudx.catalogue.server.rating.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgPool;
import iudx.catalogue.server.Configuration;
import iudx.catalogue.server.database.cache.service.CacheService;
import iudx.catalogue.server.database.elastic.model.ElasticsearchResponse;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
import iudx.catalogue.server.databroker.service.RabbitMQService;
import iudx.catalogue.server.rating.model.FilterRatingRequest;
import iudx.catalogue.server.rating.model.RatingRequest;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class RatingServiceTest {

  private static final Logger LOGGER = LogManager.getLogger(RatingServiceTest.class);
  private static JsonObject config;
  private static String exchangeName;
  private static String rsauditingtable;
  private static int minReadNumber;
  private static String ratingIndex;
  private static String docIndex;
  private static RatingServiceImpl ratingService, ratingServiceSpy;
  private static PgPool pgPool;
  private static AsyncResult<JsonObject> asyncResult;
  private static ElasticsearchService esService;
  private static RabbitMQService dataBrokerService;
  private static CacheService cacheService;

  @BeforeAll
  @DisplayName("Initialize vertx and deploy verticle")
  public static void init(Vertx vertx, VertxTestContext testContext) {
    config = Configuration.getConfiguration("./configs/config-test.json", 7);
    exchangeName = config.getString("ratingExchangeName");
    rsauditingtable = config.getString("rsAuditingTableName");
    minReadNumber = config.getInteger("minReadNumber");
    ratingIndex = config.getString("ratingIndex");
    docIndex = config.getString("docIndex");
    esService = mock(ElasticsearchService.class);
    dataBrokerService = mock(RabbitMQService.class);
    cacheService = mock(CacheService.class);
    asyncResult = mock(AsyncResult.class);
    ratingService =
        new RatingServiceImpl(
            exchangeName,
            rsauditingtable,
            minReadNumber,
            ratingIndex,
            esService,
            docIndex,
            dataBrokerService,
            cacheService);
    ratingServiceSpy = spy(ratingService);
    testContext.completeNow();
  }

  @Test
  @DisplayName("testing setup")
  public void shouldSucceed(VertxTestContext testContext) {
    LOGGER.info("setup test is passing");
    testContext.completeNow();
  }

  private JsonObject requestJson() {
    return new JsonObject()
        .put("rating", 4.5)
        .put("comment", "some comment")
        .put("id", "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood")
        .put("userID", "some-user")
        .put("status", "pending");
  }

  @Test
  @DisplayName("Success: test create rating")
  void successfulRatingCreationTest(VertxTestContext testContext) {
    JsonObject request = requestJson();
    RatingRequest ratingRequestModel = new RatingRequest(request);
    JsonObject auditInfo = new JsonObject().put("totalHits", minReadNumber + 1);

    // Mocking an empty search result
    List<ElasticsearchResponse> searchResults = Collections.emptyList();

    // Mocking getAuditingInfo method
//    doAnswer(invocation -> Future.succeededFuture(auditInfo))
//        .when(ratingServiceSpy)
//        .getAuditingInfo(any());

    // Mocking search method of esService
    doAnswer(invocation -> Future.succeededFuture(searchResults))
        .when(esService)
        .search(any(), any());

    // Mocking createDocument method
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(new JsonObject());

    // Create a valid Handler<AsyncResult<JsonObject>> in the mock setup
    doAnswer(invocation -> {
      // Get the handler from the invocation
      JsonObject document = invocation.getArgument(1); // Get the document argument
      // Simulate the successful creation of the document by returning a Future
      return Future.succeededFuture(document);
    }).when(esService).createDocument(any(), any());

    // Test execution
    ratingServiceSpy.createRating(ratingRequestModel).onComplete(handler -> {
      if (handler.succeeded()) {
        // Verify interactions
       // verify(ratingServiceSpy, times(2)).getAuditingInfo(any());
        verify(esService, times(5)).search(any(), any()); // Ensure search is called once
        verify(esService, times(1)).createDocument(any(), any()); // Ensure document creation is called
        testContext.completeNow();
      } else {
        LOGGER.error("Failed to create rating", handler.cause());
        testContext.failNow(handler.cause());
      }
    });
  }



  @Test
  @DisplayName("Failure testing rating creation")
  void failureTestingRatingCreation(VertxTestContext testContext) {
    JsonObject request = requestJson();
    RatingRequest ratingRequestModel = new RatingRequest(request);
    JsonObject auditInfo = new JsonObject().put("totalHits", 1);

//    doAnswer(Answer -> Future.succeededFuture(auditInfo))
//        .when(ratingServiceSpy)
//        .getAuditingInfo(any());

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
        .when(esService)
        .createDocument(any(), any());

    ratingServiceSpy.createRating(ratingRequestModel).onComplete(
        handler -> {
          if (handler.succeeded()) {
            //verify(ratingServiceSpy, times(1)).getAuditingInfo(any());
            verify(esService, times(1)).createDocument(any(), any());
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          } else {
            testContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("test create rating - get audit info failed")
  void testAuditInfoFailed(VertxTestContext testContext) {

//    doAnswer(Answer -> Future.failedFuture(new Throwable("empty message")))
//        .when(ratingServiceSpy)
//        .getAuditingInfo(any());
    RatingRequest ratingRequestModel = new RatingRequest(requestJson());
    ratingServiceSpy.createRating(ratingRequestModel).onComplete(
        handler -> {
          if (handler.succeeded()) {
//            verify(ratingServiceSpy, times(1)).getAuditingInfo(any());
            testContext.failNow(handler.cause());
          } else {
            testContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Success: test get rating")
  void testGetingRating(VertxTestContext testContext) {
    JsonObject request = requestJson();
    FilterRatingRequest filterRequestModel = new FilterRatingRequest(request);

    when(asyncResult.succeeded()).thenReturn(true);

    Mockito.doAnswer(
            new Answer<AsyncResult<List<ElasticsearchResponse>>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<List<ElasticsearchResponse>> answer(InvocationOnMock arg0) throws Throwable {
                QueryModel queryModel = arg0.getArgument(1); // Get the document argument
                List<ElasticsearchResponse> searchResults = List.of(new ElasticsearchResponse(
                    "dummy-id", new JsonObject()));
                // Simulate the successful creation of the document by returning a Future
                return Future.succeededFuture(searchResults);
              }
            })
        .when(esService)
        .search(any(), any());

    ratingServiceSpy.getRating(filterRequestModel).onComplete(
        handler -> {
          if (handler.succeeded()) {
            verify(ratingServiceSpy, times(2)).getRating(any());
            testContext.completeNow();
          } else {
            LOGGER.error("Fail");
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("failure testing get rating")
  void failureTestingGetRating(VertxTestContext testContext) {
    JsonObject request = requestJson();
    FilterRatingRequest requestModel = new FilterRatingRequest(request);

    when(asyncResult.succeeded()).thenReturn(false);

    Mockito.doAnswer(
            new Answer<AsyncResult<List<ElasticsearchResponse>>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<List<ElasticsearchResponse>> answer(InvocationOnMock arg0) throws Throwable {
                QueryModel queryModel = arg0.getArgument(1); // Get the document argument
                List<ElasticsearchResponse> searchResults = List.of(new ElasticsearchResponse(
                    "dummy-id", new JsonObject()));
                // Simulate the successful creation of the document by returning a Future
                return Future.failedFuture("Fail");
              }
            })
        .when(esService)
        .search(any(), any());

    ratingServiceSpy.getRating(requestModel).onComplete(
        handler -> {
          if (handler.failed()) {
            verify(ratingServiceSpy, times(1)).getRating(any());
            testContext.completeNow();
          } else {
            testContext.failNow("Fail");
          }
        });
  }

  @Test
  @DisplayName("Success: test update rating")
  void successfulRatingUpdationTest(VertxTestContext testContext) {
    JsonObject request = requestJson();
    RatingRequest requestModel = new RatingRequest(request);

    // Mocking an empty search result
    ElasticsearchResponse elasticsearchResponse = new ElasticsearchResponse();
    elasticsearchResponse.setSource(request);
    elasticsearchResponse.setDocId("dummy-docId");
    List<ElasticsearchResponse> searchResults = List.of(elasticsearchResponse);

    // Mocking search method of esService
    doAnswer(invocation -> Future.succeededFuture(searchResults))
        .when(esService)
        .search(any(), any());

    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                JsonObject document = arg0.getArgument(2); // Get the document argument
                // Simulate the successful update of the document by returning a Future
                return Future.succeededFuture(document);
              }
            })
        .when(esService)
        .updateDocument(anyString(), anyString(), any());

    ratingServiceSpy.updateRating(requestModel)
        .onComplete(handler -> {
          if (handler.succeeded()) {
            verify(esService, times(2)).updateDocument(anyString(), anyString(), any());
            testContext.completeNow();
          } else {
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Failure testing rating updation")
  void failureTestingRatingUpdation(VertxTestContext testContext) {
    JsonObject request = requestJson();
    RatingRequest requestModel = new RatingRequest(request);
    ElasticsearchResponse elasticsearchResponse = new ElasticsearchResponse();
    elasticsearchResponse.setSource(request);
    elasticsearchResponse.setDocId("dummy-docId");
    // Mocking an empty search result
    List<ElasticsearchResponse> searchResults = List.of(elasticsearchResponse);

    // Mocking search method of esService
    doAnswer(invocation -> Future.succeededFuture(searchResults))
        .when(esService)
        .search(any(), any());

    doAnswer(invocation -> Future.failedFuture("Fail"))
        .when(esService)
        .updateDocument(anyString(), anyString(), any());

    ratingServiceSpy.updateRating(requestModel)
        .onComplete(handler -> {
          if (handler.succeeded()) {
            verify(esService, times(1)).updateDocument(any(), anyString(), any());
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          } else {
            testContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Success: test delete rating")
  void successfulRatingDeletionTest(VertxTestContext testContext) {
    JsonObject request = requestJson();
    FilterRatingRequest filterRequest = new FilterRatingRequest(request);

    List<ElasticsearchResponse> searchResults = List.of(new ElasticsearchResponse());
    // Mocking search method of esService
    doAnswer(invocation -> Future.succeededFuture(searchResults))
        .when(esService)
        .search(any(), any());

    Mockito.doAnswer(
            new Answer<AsyncResult<String>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<String> answer(InvocationOnMock arg0) throws Throwable {
                String id = arg0.getArgument(1);
                // Simulate the successful deletion of the document by returning a Future
                return Future.succeededFuture(id);
              }
            })
        .when(esService)
        .deleteDocument(any(), any());

    ratingServiceSpy.deleteRating(filterRequest)
        .onComplete(handler -> {
          if (handler.succeeded()) {
            verify(esService, times(1)).deleteDocument(any(), any());
            testContext.completeNow();
          } else {
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Failure testing rating deletion")
  void failureTestingRatingDeletion(VertxTestContext testContext) {
    JsonObject request = requestJson();
    FilterRatingRequest filterRequest = new FilterRatingRequest(request);

    List<ElasticsearchResponse> searchResults = List.of(new ElasticsearchResponse());
    // Mocking search method of esService
    doAnswer(invocation -> Future.succeededFuture(searchResults))
        .when(esService)
        .search(any(), any());

    Mockito.doAnswer(
            new Answer<AsyncResult<String>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<String> answer(InvocationOnMock arg0) throws Throwable {
                String id = arg0.getArgument(1);
                return (Future.failedFuture("Fail"));
              }
            })
        .when(esService)
        .deleteDocument(any(), any());

    ratingServiceSpy.deleteRating(filterRequest)
        .onComplete(handler -> {
          if (handler.succeeded()) {
            verify(esService, times(2)).deleteDocument(any(), any());
            testContext.failNow("Fail");
          } else {
            testContext.completeNow();
          }
        });
  }

//  @Test
//  @DisplayName("Success: Test publish message")
//  void testPublishMessage(VertxTestContext testContext) {
//
//    Mockito.doAnswer(
//            new Answer<AsyncResult<Void>>() {
//              @SuppressWarnings("unchecked")
//              @Override
//              public AsyncResult<Void> answer(InvocationOnMock arg0) throws Throwable {
//                String routingKey = arg0.getArgument(2);
//                return Future.succeededFuture();
//              }
//            })
//        .when(dataBrokerService)
//        .publishMessage(any(), anyString(), eq("#"));
//    QueryObject rmqMessage = QueryBuilder.buildMessageForRmq(requestJson());
//    ratingServiceSpy.publishMessage(rmqMessage);
//    testContext.completeNow();
//  }

//  @Test
//  @DisplayName("Success: Test get auditing info future")
//  public void testGetAuditingInfo(VertxTestContext testContext) {
//    StringBuilder query = new StringBuilder("select * from nosuchtable");
//    doAnswer(
//            new Answer<AsyncResult<JsonObject>>() {
//              @Override
//              public AsyncResult<JsonObject> answer(InvocationOnMock arg0)
//                  throws Throwable {
//                String query = arg0.getArgument(0);
//                return Future.succeededFuture(new JsonObject());
//              }
//            })
//        .when(postgresService)
//        .executeCountQuery(anyString());
//
//    ratingService
//        .getAuditingInfo(query)
//        .onComplete(
//            handler -> {
//              if (handler.succeeded()) {
//                verify(postgresService, times(1)).executeCountQuery(anyString());
//                testContext.completeNow();
//              } else {
//                testContext.failNow("get auditing info test failed");
//              }
//            });
//  }
}
