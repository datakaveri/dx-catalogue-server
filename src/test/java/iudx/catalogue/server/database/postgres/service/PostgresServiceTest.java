package iudx.catalogue.server.database.postgres.service;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import iudx.catalogue.server.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static iudx.catalogue.server.mlayer.util.Constants.GET_HIGH_COUNT_DATASET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(VertxExtension.class)
public class PostgresServiceTest {
  public static final Logger LOGGER = LogManager.getLogger(PostgresServiceTest.class);
  private static PostgresServiceImpl pgService;
  static String table;
  private static JsonObject dbConfig;

  @Container
  static PostgreSQLContainer container =
      new PostgreSQLContainer<>("postgres:12.11").withInitScript("pg_test_schema.sql");

  @BeforeAll
  public static void setup(VertxTestContext testContext) {
    dbConfig = Configuration.getConfiguration("./configs/config-test.json", 10);
    table = dbConfig.getString("auditingTableName");
    // Now we have an address and port for Postgresql, no matter where it is running
    Integer port = container.getFirstMappedPort();
    String host = container.getHost();
    String db = container.getDatabaseName();
    String user = container.getUsername();
    String password = container.getPassword();

    PgConnectOptions connectOptions =
        new PgConnectOptions()
            .setPort(port)
            .setHost(host)
            .setDatabase(db)
            .setUser(user)
            .setPassword(password);

    PoolOptions poolOptions = new PoolOptions().setMaxSize(10);

    Vertx vertxObj = Vertx.vertx();

    PgPool pool = PgPool.pool(vertxObj, connectOptions, poolOptions);

    pgService = new PostgresServiceImpl(pool);
    testContext.completeNow();
  }

  @Test
  @Order(1)
  @DisplayName("Test execute query - success")
  public void testExecuteQuerySuccess(VertxTestContext testContext) {
    StringBuilder stringBuilder = new StringBuilder(GET_HIGH_COUNT_DATASET.replace("$1", table));

    String expected =
        "{\"type\":\"urn:dx:cat:Success\",\"title\":\"Success\",\"results\":[{\"resource_group\":\"dummy-rg-id\",\"totalhits\":1}]}";
    pgService.executeQuery(stringBuilder.toString()).onComplete(handler -> {
          if (handler.succeeded()) {
            LOGGER.debug(handler.result().toString());
            assertEquals(expected, handler.result().toString());
            assertTrue(handler.result().containsKey("type"));
            assertTrue(handler.result().containsKey("title"));
            assertTrue(handler.result().containsKey("results"));
            assertEquals("Success", handler.result().getString("title"));
            assertEquals("urn:dx:cat:Success", handler.result().getString("type"));
            testContext.completeNow();
          } else {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(2)
  @DisplayName("test execute query - failure")
  public void testExecuteQueryFailure(VertxTestContext testContext) {

    StringBuilder stringBuilder = new StringBuilder("select * from nosuchtable");

    String expected =
        "{\"type\":\"urn:dx:cat:DatabaseError\",\"title\":\"database error\",\"detail\":\"ERROR: relation \\\"nosuchtable\\\" does not exist (42P01)\"}";
    pgService.executeQuery(stringBuilder.toString()).onComplete(handler -> {
          if (handler.failed()) {
            assertEquals(expected, handler.cause().getMessage());
            testContext.completeNow();
          } else {
            testContext.failNow("test execute query unexpectedly failed");
          }
        });
  }

  @Test
  @Order(3)
  @DisplayName("Test execute count query - success")
  public void testExecuteCountQuery(VertxTestContext testContext) {
    StringBuilder stringBuilder = new StringBuilder("select count(*) from " + table);

    pgService.executeCountQuery(stringBuilder.toString()).onComplete(handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result();
            int hits = result.getInteger("totalHits");
            assertEquals(1, hits);
            testContext.completeNow();
          } else {
            testContext.failNow("execute count test failed");
          }
        });
  }

  @Test
  @Order(4)
  @DisplayName("Test execute count query - failure")
  public void testExecuteCountQueryFailure(VertxTestContext testContext) {
    String query = "select count(*) from nosuchtable";

    String expected =
        "{\"type\":\"urn:dx:cat:DatabaseError\",\"title\":\"database error\",\"detail\":\"ERROR: relation \\\"nosuchtable\\\" does not exist (42P01)\"}";
    pgService.executeCountQuery(query).onComplete(handler -> {
          if (handler.failed()) {
            assertEquals(expected, handler.cause().getMessage());
            testContext.completeNow();
          } else {
            testContext.failNow("execute count unexpectedly failed");
          }
        });
  }
}
