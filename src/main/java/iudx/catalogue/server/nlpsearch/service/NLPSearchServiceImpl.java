package iudx.catalogue.server.nlpsearch.service;

import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The NLP Search Service Implementation.
 *
 * <h1>NLP Search Service Implementation</h1>
 *
 * <p>The NLP Search Service implementation in the IUDX Catalogue Server implements the definitions
 * of the {@link NLPSearchService}.
 *
 * @version 1.0
 * @since 2020-12-21
 */
public class NLPSearchServiceImpl implements NLPSearchService {

  private static final Logger LOGGER = LogManager.getLogger(NLPSearchServiceImpl.class);
  static WebClient webClient;
  private final String nlpServiceUrl;
  private final int nlpServicePort;

  /**
   * Constructs a new instance of NLPSearchServiceImpl with the given parameters.
   *
   * @param client the WebClient used to make HTTP requests to the NLP service
   * @param nlpServiceUrl the URL of the NLP service
   * @param nlpServicePort the port number of the NLP service
   */
  public NLPSearchServiceImpl(WebClient client, String nlpServiceUrl, int nlpServicePort) {
    webClient = client;
    this.nlpServiceUrl = nlpServiceUrl;
    this.nlpServicePort = nlpServicePort;
  }

  @Override
  public Future<JsonObject> search(String query) {
    Promise<JsonObject> promise = Promise.promise();
    webClient
        .get(nlpServicePort, nlpServiceUrl, "/search")
        .timeout(SERVICE_TIMEOUT)
        .addQueryParam("q", query)
        .putHeader("Accept", "application/json")
        .send(
            ar -> {
              if (ar.succeeded()) {
                LOGGER.debug("Success: NLP Search; Request succeeded");
                promise.complete(ar.result().body().toJsonObject());
              } else {
                LOGGER.error("Fail: NLP Search failed");
                promise.fail(ar.cause());
              }
            });
    return promise.future();
  }

  @Override
  public Future<JsonObject> getEmbedding(JsonObject doc) {
    Promise<JsonObject> promise = Promise.promise();
    webClient
        .post(nlpServicePort, nlpServiceUrl, "/indexdoc")
        .timeout(SERVICE_TIMEOUT)
        .sendJsonObject(
            doc,
            ar -> {
              if (ar.succeeded()) {
                LOGGER.debug("Info: Document embeddings created");
                promise.complete(ar.result().body().toJsonObject());
              } else {
                LOGGER.error("Error: Document embeddings not created");
                promise.fail(ar.cause());
              }
            });
    return promise.future();
  }
}
