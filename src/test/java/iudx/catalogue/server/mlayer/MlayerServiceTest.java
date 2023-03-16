package iudx.catalogue.server.mlayer;

import com.google.j2objc.annotations.J2ObjCIncompatible;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.database.postgres.PostgresService;
import iudx.catalogue.server.rating.RatingServiceTest;
import org.apache.curator.shaded.com.google.common.hash.HashCode;
import org.apache.curator.shaded.com.google.common.hash.HashFunction;
import org.apache.curator.shaded.com.google.common.hash.Hashing;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static iudx.catalogue.server.mlayer.util.Constants.INSTANCE_ID;
import static iudx.catalogue.server.mlayer.util.Constants.MLAYER_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class MlayerServiceTest {
    private static Logger LOGGER = LogManager.getLogger(MlayerServiceTest.class);
    static MlayerServiceImpl mlayerService;
    @Mock private static AsyncResult<JsonObject> asyncResult;
    @Mock static DatabaseService databaseService;
    @Mock static PostgresService postgresService;

    @Mock JsonObject json;
    private static String tableName = "database Table";
    private static Vertx vertxObj;
    public static final String GET_HIGH_COUNT_DATASET =
            "with auditing_rs_view as (select resourceid, count(*) as hits, (select count(*) from regexp_matches(resourceid, '/', 'g')) as idtype from $1 group by resourceid) select left(resourceid,length(resourceid) -strpos(reverse(resourceid),'/')) as rgid, sum(hits) as totalhits from auditing_rs_view where idtype=4 group by rgid order by totalhits desc limit 6";

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
        mlayerService = new MlayerServiceImpl(databaseService, postgresService, tableName);

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
        mlayerService = new MlayerServiceImpl(databaseService, postgresService, tableName);
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
        mlayerService = new MlayerServiceImpl(databaseService, postgresService, tableName);
        when(asyncResult.succeeded()).thenReturn(true);
        doAnswer(
                new Answer<AsyncResult<JsonObject>>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                        return null;
                    }
                })
                .when(databaseService)
                .getMlayerInstance(any());
        mlayerService.getMlayerInstance(
                handler -> {
                    if (handler.succeeded()) {
                        verify(databaseService, times(1)).getMlayerInstance(any());
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
        mlayerService = new MlayerServiceImpl(databaseService, postgresService, tableName);
        when(asyncResult.succeeded()).thenReturn(false);
        Mockito.doAnswer(
                        new Answer<AsyncResult<JsonObject>>() {
                            @SuppressWarnings("unchecked")
                            @Override
                            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                                return null;
                            }
                        })
                .when(databaseService)
                .getMlayerInstance(any());

        mlayerService.getMlayerInstance(
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
    @DisplayName("Success: test delete mlayer instance")
    void successfulMlayerInstanceDeleteTest(VertxTestContext testContext) {
        String request = "dummy";
        mlayerService = new MlayerServiceImpl(databaseService, postgresService, tableName);

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
        mlayerService = new MlayerServiceImpl(databaseService, postgresService, tableName);

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
        mlayerService = new MlayerServiceImpl(databaseService, postgresService, tableName);

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
        mlayerService = new MlayerServiceImpl(databaseService, postgresService, tableName);

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
        mlayerService = new MlayerServiceImpl(databaseService, postgresService, tableName);

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
        mlayerService = new MlayerServiceImpl(databaseService, postgresService, tableName);

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
        mlayerService = new MlayerServiceImpl(databaseService, postgresService, tableName);

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
        mlayerService = new MlayerServiceImpl(databaseService, postgresService, tableName);

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
        mlayerService = new MlayerServiceImpl(databaseService, postgresService, tableName);

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
        mlayerService = new MlayerServiceImpl(databaseService, postgresService, tableName);

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
        when(asyncResult.succeeded()).thenReturn(true);
        mlayerService = new MlayerServiceImpl(databaseService, postgresService, tableName);

        doAnswer(
                new Answer<AsyncResult<JsonObject>>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                        return null;
                    }
                })
                .when(databaseService)
                .getMlayerDomain(any());
        mlayerService.getMlayerDomain(
                handler -> {
                    if (handler.succeeded()) {
                        verify(databaseService, times(1)).getMlayerDomain(any());
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
        when(asyncResult.succeeded()).thenReturn(false);
        mlayerService = new MlayerServiceImpl(databaseService, postgresService, tableName);

        Mockito.doAnswer(
                        new Answer<AsyncResult<JsonObject>>() {
                            @SuppressWarnings("unchecked")
                            @Override
                            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                                return null;
                            }
                        })
                .when(databaseService)
                .getMlayerDomain(any());

        mlayerService.getMlayerDomain(
                handler -> {
                    if (handler.succeeded()) {
                        verify(databaseService, times(1)).getMlayerDomain(any());
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
        mlayerService = new MlayerServiceImpl(databaseService, postgresService, tableName);

        when(asyncResult.succeeded()).thenReturn(true);
        doAnswer(
                new Answer<AsyncResult<JsonObject>>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                        return null;
                    }
                })
                .when(databaseService)
                .getMlayerProviders(any());
        mlayerService.getMlayerProviders(
                handler -> {
                    if (handler.succeeded()) {
                        verify(databaseService, times(1)).getMlayerProviders(any());
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
        mlayerService = new MlayerServiceImpl(databaseService, postgresService, tableName);

        when(asyncResult.succeeded()).thenReturn(false);
        Mockito.doAnswer(
                        new Answer<AsyncResult<JsonObject>>() {
                            @SuppressWarnings("unchecked")
                            @Override
                            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                                return null;
                            }
                        })
                .when(databaseService)
                .getMlayerProviders(any());

        mlayerService.getMlayerProviders(
                handler -> {
                    if (handler.succeeded()) {
                        verify(databaseService, times(1)).getMlayerProviders(any());
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
        mlayerService = new MlayerServiceImpl(databaseService, postgresService, tableName);

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
        mlayerService = new MlayerServiceImpl(databaseService, postgresService, tableName);

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
        mlayerService = new MlayerServiceImpl(databaseService, postgresService, tableName);

        JsonObject request = new JsonObject();
        when(asyncResult.succeeded()).thenReturn(true);
        doAnswer(
                new Answer<AsyncResult<JsonObject>>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                        return null;
                    }
                })
                .when(databaseService)
                .getMlayerAllDatasets(any());
        mlayerService.getMlayerAllDatasets(
                handler -> {
                    if (handler.succeeded()) {
                        verify(databaseService, times(1)).getMlayerAllDatasets(any());
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
        mlayerService = new MlayerServiceImpl(databaseService, postgresService, tableName);

        JsonObject request = new JsonObject();
        when(asyncResult.succeeded()).thenReturn(false);
        Mockito.doAnswer(
                        new Answer<AsyncResult<JsonObject>>() {
                            @SuppressWarnings("unchecked")
                            @Override
                            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                                return null;
                            }
                        })
                .when(databaseService)
                .getMlayerAllDatasets(any());

        mlayerService.getMlayerAllDatasets(
                handler -> {
                    if (handler.succeeded()) {
                        verify(databaseService, times(1)).getMlayerAllDatasets(any());
                        LOGGER.debug("Fail");
                        testContext.failNow(handler.cause());
                    } else {
                        testContext.completeNow();
                    }
                });
    }

    @Test
    @DisplayName("Success: test get dataset detail")
    void successfulGetMlayerDatasetTest(VertxTestContext testContext) {
        mlayerService = new MlayerServiceImpl(databaseService, postgresService, tableName);

        String dataset_id = "abc/abc/abc";
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
                .getMlayerDataset(any(), any());
        mlayerService.getMlayerDataset(
                dataset_id,
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
    @DisplayName("Failure: test get dataset details")
    void failureMlayerDatasetTest(VertxTestContext testContext) {
        mlayerService = new MlayerServiceImpl(databaseService, postgresService, tableName);

        String dataset_id = "abc/abc/abc";
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
                dataset_id,
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
    @DisplayName("Success: test get overview detail")
    void successfulGetMlayerOverviewTest(VertxTestContext testContext) {
        mlayerService = new MlayerServiceImpl(databaseService, postgresService, tableName);
        JsonArray jsonArray = new JsonArray();
        JsonObject json = new JsonObject();
        json.put("results", jsonArray);
        jsonArray.add("dataset");
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
                .when(databaseService)
                .getMlayerPopularDatasets(any(), any());
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
                handler -> {
                    if (handler.succeeded()) {
                        verify(databaseService, times(1)).getMlayerPopularDatasets(any(), any());
                        verify(postgresService, times(1)).executeQuery(any(), any());

                        testContext.completeNow();
                    } else {
                        LOGGER.debug("Fail");
                        testContext.failNow(handler.cause());
                    }
                });
    }

    @Test
    @DisplayName("Fail: test get overview detail")
    void failedGetMlayerOverviewTest(VertxTestContext testContext) {
        mlayerService = new MlayerServiceImpl(databaseService, postgresService, tableName);
        JsonArray jsonArray = new JsonArray();
        JsonObject json = new JsonObject();
        json.put("results", jsonArray);
        jsonArray.add("dataset");
        //        when(asyncResult.result()).thenReturn(json);

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
}