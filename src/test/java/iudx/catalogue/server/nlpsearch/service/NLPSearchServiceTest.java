package iudx.catalogue.server.nlpsearch.service;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import iudx.catalogue.server.Configuration;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled
public class NLPSearchServiceTest {
    private static NLPSearchService nlpService;
    private static JsonObject config;
    private static Vertx vertxObj;
    
    private static final Logger LOGGER = LogManager.getLogger(NLPSearchServiceTest.class);

    @BeforeAll
    static void startVertx(Vertx vertx, VertxTestContext testContext) {
      vertxObj = vertx;

      config = Configuration.getConfiguration("./configs/config-test.json", 1);
      
      WebClientOptions webClientOptions = new WebClientOptions();
      webClientOptions.setTrustAll(true).setVerifyHost(false);
      WebClient client =  WebClient.create(vertx, webClientOptions);
      nlpService = new NLPSearchServiceImpl(client, "es-vectorised-search_web_1", 5000);
      
      LOGGER.info("NLP Service setup complete");
      testContext.completeNow();
    }

    @AfterEach
    public void finish(VertxTestContext testContext) {
      LOGGER.info("Finishing....");
      vertxObj.close(testContext.succeeding(response -> testContext.completeNow()));
    }

    @Test
    @DisplayName("NLP Search test")
    void nlpSearchTest(VertxTestContext testContext) {
      String query = "Flood levels";  
      nlpService.search(query).onComplete(ar-> {
          LOGGER.debug("Result: " + ar.result().toString());
          testContext.completeNow();
      });
      testContext.completeNow();
    }
}
