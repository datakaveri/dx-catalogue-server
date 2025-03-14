package iudx.catalogue.server.nlpsearch.controller;

import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.apiserver.util.RespBuilder;
import iudx.catalogue.server.nlpsearch.service.NLPSearchService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NLPSearchController {
  private static final Logger LOGGER = LogManager.getLogger(NLPSearchController.class);

  private final NLPSearchService nlpSearchService;

  public NLPSearchController(NLPSearchService nlpSearchService) {
    this.nlpSearchService = nlpSearchService;
  }

  public void setupRoutes(Router router) {
    router.get("/nlp/search").handler(this::handleSearch);
    router.post("/nlp/embedding").handler(this::handleGetEmbedding);
  }

  void handleSearch(RoutingContext routingContext) {
    String query = routingContext.request().getParam("q");
    if (query == null || query.isEmpty()) {
      routingContext
          .response()
          .setStatusCode(400)
          .end(
              new RespBuilder()
                  .withType(TYPE_INVALID_SYNTAX)
                  .withTitle(TITLE_INVALID_SYNTAX)
                  .getResponse());
      return;
    }

    nlpSearchService
        .search(query)
        .onSuccess(
            result -> {
              LOGGER.debug("Success: " + result);
              routingContext
                  .response()
                  .setStatusCode(200)
                  .putHeader("content-type", "application/json")
                  .end(result.encode());
            })
        .onFailure(
            err -> {
              LOGGER.error("NLP Search failed", err);
              routingContext
                  .response()
                  .setStatusCode(500)
                  .end(
                      new RespBuilder()
                          .withType(TYPE_INTERNAL_SERVER_ERROR)
                          .withTitle(TITLE_INTERNAL_SERVER_ERROR)
                          .getResponse());
            });
  }

  void handleGetEmbedding(RoutingContext routingContext) {
    routingContext
        .request()
        .body()
        .onSuccess(
            buffer -> {
              JsonObject doc = buffer.toJsonObject();
              nlpSearchService
                  .getEmbedding(doc)
                  .onSuccess(
                      result -> {
                        routingContext
                            .response()
                            .setStatusCode(200)
                            .putHeader("content-type", "application/json")
                            .end(result.encode());
                      })
                  .onFailure(
                      err -> {
                        LOGGER.error("Get embedding failed", err);
                        routingContext
                            .response()
                            .setStatusCode(500)
                            .end(
                                new RespBuilder()
                                    .withType(TYPE_INTERNAL_SERVER_ERROR)
                                    .withTitle(TITLE_INTERNAL_SERVER_ERROR)
                                    .getResponse());
                      });
            })
        .onFailure(
            err -> {
              LOGGER.error("Failed to parse request body", err);
              routingContext
                  .response()
                  .setStatusCode(400)
                  .end(
                      new RespBuilder()
                          .withType(TYPE_INVALID_SYNTAX)
                          .withTitle(TITLE_INVALID_SYNTAX)
                          .getResponse());
            });
  }
}
