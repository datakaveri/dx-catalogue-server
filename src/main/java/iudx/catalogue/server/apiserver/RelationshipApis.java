/**
 * <h1>RelationshipApis.java</h1>
 * Callback handlers for Relationship APIs
 */

package iudx.catalogue.server.apiserver;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.apiserver.util.QueryMapper;
import iudx.catalogue.server.apiserver.util.RespBuilder;
import iudx.catalogue.server.database.elastic.ElasticsearchService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class RelationshipApis {


  private static final Logger LOGGER = LogManager.getLogger(RelationshipApis.class);
  private ElasticsearchService esService;

  public void setEsService(ElasticsearchService esService) {
    this.esService = esService;
  }

  /**
   * Get all items belonging to the itemType.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  public void listRelationshipHandler(RoutingContext routingContext) {

    LOGGER.info("Info: Searching for relationship of resource");

    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    MultiMap queryParameters = routingContext.queryParams();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject requestBody = new JsonObject();
    String instanceId = request.getHeader(HEADER_INSTANCE);

    if (request.getParam(ID) != null && request.getParam(REL_KEY) != null) {
      String id = request.getParam(ID);
      String itemType = request.getParam(REL_KEY);

      if (!id.isBlank() && isValidId(id) && ITEMS_KEY.contains(itemType)) {
        requestBody = QueryMapper.map2Json(queryParameters);

        if (requestBody != null) {
          requestBody.put(INSTANCE, instanceId);
          requestBody.put(RELATIONSHIP, queryParameters.get(REL_KEY));
          JsonObject resp = QueryMapper.validateQueryParam(requestBody);

          if (resp.getString(STATUS).equals(SUCCESS)) {

            /*
             * Request database service with requestBody for listing resource relationship
             */
            esService.listRelationship(requestBody, eshandler -> {
              if (eshandler.succeeded()) {
                LOGGER.info("Success: Retrieved relationships of " + itemType);
                response.setStatusCode(200).end(eshandler.result().toString());
              } else if (eshandler.failed()) {
                LOGGER
                    .error("Fail: Issue in listing relationship;" + eshandler.cause().getMessage());
                response.setStatusCode(400)
                        .end(eshandler.cause().getMessage());
              }
            });
          } else {
            LOGGER.error("Fail: Search; Invalid request query parameters");
            response.setStatusCode(400)
                    .end(resp.toString());
          }
        } else {
          LOGGER.error("Fail: Search; Invalid request query parameters");
          response.setStatusCode(400)
                  .end(new RespBuilder()
                        .withType(TYPE_INVALID_SYNTAX)
                        .withTitle(TITLE_INVALID_SYNTAX)
                          .withDetail("Invalid Syntax")
                          .getResponse());
        }
      } else {
        LOGGER.error("Fail: Issue in query parameter");
        response.setStatusCode(400)
                  .end(new RespBuilder()
                        .withType(TYPE_INVALID_QUERY_PARAM_VALUE)
                        .withTitle(TITLE_INVALID_QUERY_PARAM_VALUE)
                        .withDetail("Invalid relationship value")
                        .getResponse());
      }
    } else {
      LOGGER.error("Fail: Issue in query parameter");
      response.setStatusCode(400)
                  .end(new RespBuilder()
                        .withType(TYPE_MISSING_PARAMS)
                        .withTitle(TITLE_MISSING_PARAMS)
                        .withDetail("Mandatory field(s) not provided")
                        .getResponse());
    }
  }

  private boolean isValidId(String id) {
    return UUID_PATTERN.matcher(id).matches();
  }


  /**
   * Relationship search of the cataloque items.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void relSearchHandler(RoutingContext routingContext) {

    LOGGER.debug("Info: Relationship search");

    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    JsonObject requestBody = new JsonObject();

    String instanceId = request.getHeader(HEADER_INSTANCE);

    MultiMap queryParameters = routingContext.queryParams();

    /* validating proper actual query parameters from request */
    if (request.getParam(RELATIONSHIP) != null && request.getParam(VALUE) != null) {

      /* converting query parameters in json */
      requestBody = QueryMapper.map2Json(queryParameters);

      if (requestBody != null) {

        requestBody.put(INSTANCE, instanceId);

        /* Request database service with requestBody for listing domains */
        esService.relSearch(requestBody, eshandler -> {
          if (eshandler.succeeded()) {
            LOGGER.info("Info: Relationship search completed");
            response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                    .setStatusCode(200)
                    .end(eshandler.result().toString());
          } else if (eshandler.failed()) {
            LOGGER.error("Fail: Issue in relationship search "
                + eshandler.cause().getMessage());
            response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                    .setStatusCode(400)
                .end(eshandler.cause().getMessage());
          }
        });
      } else {
        LOGGER.error("Fail: Invalid request query parameters");
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                .setStatusCode(400)
                  .end(new RespBuilder()
                        .withType(TYPE_INVALID_SYNTAX)
                        .withTitle(TITLE_INVALID_SYNTAX)
                          .withDetail("Invalid Syntax")
                          .getResponse());
      }
    } else {
      LOGGER.error("Fail: Invalid request query parameters");
      response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
              .setStatusCode(400)
                  .end(new RespBuilder()
                        .withType(TYPE_INVALID_SYNTAX)
                        .withTitle(TITLE_INVALID_SYNTAX)
                          .withDetail("Invalid Syntax")
                          .getResponse());
    }
  }
}
