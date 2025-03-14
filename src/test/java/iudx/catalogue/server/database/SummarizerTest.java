//package iudx.catalogue.server.database;
//
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.MethodOrderer;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestMethodOrder;
//import org.junit.jupiter.api.extension.ExtendWith;
//import io.vertx.junit5.VertxExtension;
//import io.vertx.junit5.VertxTestContext;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import io.vertx.core.json.JsonObject;
//
//
//@ExtendWith(VertxExtension.class)
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//public class SummarizerTest {
//
//  private static final Logger LOGGER = LogManager.getLogger(SummarizerTest.class);
//  JsonObject doc = new JsonObject("{\"tags\": [\"a\",\"b\",\"c\"], \"description\": \"some description,with characters\", \"name\": \"iudx\", \"label\": \"thisisiudx\", \"descriptor\": {\"co2\": \"high\", \"no2\": [\"low\", \"medium\"]}}");
//
//  @Test
//  @DisplayName("Summarize test")
//  void summarize(VertxTestContext testContext) {
//    LOGGER.debug(Summarizer.summarize(doc));
//    testContext.completeNow();
//  }
//}
//
