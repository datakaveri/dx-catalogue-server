package iudx.catalogue.server.database.elastic;

import static iudx.catalogue.server.util.Constants.DATABASE_IP;
import static iudx.catalogue.server.util.Constants.DATABASE_PASSWD;
import static iudx.catalogue.server.util.Constants.DATABASE_PORT;
import static iudx.catalogue.server.util.Constants.DATABASE_UNAME;
import static iudx.catalogue.server.util.Constants.DOC_INDEX;
import static iudx.catalogue.server.util.Constants.RATING_INDEX;
import static org.junit.jupiter.api.Assertions.*;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.server.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class ElasticClientTest {
  private static final Logger LOGGER = LogManager.getLogger(ElasticClientTest.class);
  private static ElasticClient elasticClient;
  private static String databaseIP;
  private static int databasePort;
  private static String docIndex, ratingIndex;
  private static String databaseUser;
  private static String databasePassword;
  private static String databaseIndex;

  @BeforeAll
  @DisplayName("")
  static void initClient(VertxTestContext testContext) {
    /* Read the configuration and set the elastic service properties. */

    JsonObject elasticConfig = Configuration.getConfiguration("./configs/config-test.json", 0);
    databaseIndex =
        Configuration.getConfiguration("./configs/config-test.json").getString("databaseIndex");

    databaseIP = elasticConfig.getString(DATABASE_IP);
    databasePort = elasticConfig.getInteger(DATABASE_PORT);
    databaseUser = elasticConfig.getString(DATABASE_UNAME);
    databasePassword = elasticConfig.getString(DATABASE_PASSWD);
    docIndex = elasticConfig.getString(DOC_INDEX);
    ratingIndex = elasticConfig.getString(RATING_INDEX);

    elasticClient = new ElasticClient(databaseIP, databasePort, databaseUser, databasePassword);
    LOGGER.info("Read config file");

    testContext.completeNow();
  }

  // Retrieves ElasticsearchAsyncClient using getClient method
  @Test
  public void TestGetClient() {

    // Retrieving ElasticsearchAsyncClient using getClient method
    ElasticsearchAsyncClient client = elasticClient.getClient();

    //Assertion
    assertNotNull(client);
  }

}