package iudx.catalogue.server.apiserver;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.authenticator.Constants.API_ENDPOINT;
import static iudx.catalogue.server.authenticator.Constants.TOKEN;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.authenticator.AuthenticationService;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.database.RespBuilder;
import iudx.catalogue.server.util.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OwnershipApis {

  private static final Logger LOGGER = LogManager.getLogger(OwnershipApis.class);
  private final Api api;
  private DatabaseService dbService;
  private AuthenticationService authService;

  public OwnershipApis(Api api) {
    this.api = api;
  }

  public void setDbService(DatabaseService dbService) {
    this.dbService = dbService;
  }

  public void setAuthService(AuthenticationService authService) {
    this.authService = authService;
  }

  public void transferOwnershipHandler(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
    JsonObject requestBody = routingContext.body().asJsonObject();

    String oldUserId = requestBody.getString(OLD_USER_ID);
    String newUserId = requestBody.getString(NEW_USER_ID);

    JsonObject jwtAuthenticationInfo = new JsonObject();
    jwtAuthenticationInfo
        .put(TOKEN, routingContext.get(HEADER_TOKEN))
        .put(METHOD, REQUEST_POST)
        .put(API_ENDPOINT, api.getRouteOwnershipTransfer());

    authService.tokenInterospect(new JsonObject(), jwtAuthenticationInfo, authHandler -> {
      if (authHandler.failed()) {
        LOGGER.error("Error: " + authHandler.cause().getMessage());
        response.setStatusCode(401)
            .end(new iudx.catalogue.server.apiserver.util.RespBuilder()
                .withType(TYPE_TOKEN_INVALID)
                .withTitle(TITLE_TOKEN_INVALID)
                .withDetail(authHandler.cause().getMessage())
                .getResponse());
      } else {
        LOGGER.debug("Success: JWT Auth successful");

        JsonObject userInfo = authHandler.result();
        String requesterId = userInfo.getString(SUB);
        String organizationId = userInfo.getString(ORGANIZATION_ID);

        // Authorization Check: Ensure the user is transferring their own items
        if (!requesterId.equals(oldUserId)) {
          LOGGER.warn("Unauthorized: Requester is not the owner");
          response.setStatusCode(403).end(new JsonObject()
              .put(TYPE, TYPE_ACCESS_DENIED)
              .put(TITLE, "Forbidden")
              .put(DETAIL, "You are not authorized to transfer these items.")
              .encode());
          return;
        }

        //  Missing Input Check
        if (oldUserId.isEmpty() || newUserId == null || newUserId.isEmpty()) {
          response.setStatusCode(400)
              .end(new JsonObject()
                  .put(TYPE, "urn:dx:cat:InvalidInput")
                  .put(TITLE, "Missing Parameters")
                  .put(DETAIL, "Missing oldUserId or newUserId")
                  .encode());
          return;
        }

        // Proceed with the update_by_query
        dbService.updateByQueryRequest(requestBody, organizationId, handler -> {
          if (handler.succeeded()) {
            JsonObject resultJson = handler.result();
            RespBuilder respBuilder = new RespBuilder();
            response.setStatusCode(200);
            response.end(respBuilder
                .withType(TYPE_SUCCESS)
                .withTitle(TITLE_SUCCESS)
                .withResult(resultJson)
                .withDetail("Success: Items updated successfully")
                .getJsonResponse().encodePrettily());
          } else {
            LOGGER.error("Fail: Update By Query Search;" + handler.cause().getMessage());
            response.setStatusCode(400).end(handler.cause().getMessage());
          }
        });
      }
    });
  }

  public void deleteOwnershipHandler(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    String oldUserId = routingContext.queryParams().get(OLD_USER_ID);
    JsonObject request = new JsonObject().put(OLD_USER_ID, oldUserId);

    if (oldUserId == null) {
      response.setStatusCode(400).end("Missing oldUserId for delete operation");
    }

    JsonObject jwtAuthenticationInfo = new JsonObject();
    jwtAuthenticationInfo
        .put(TOKEN, routingContext.get(HEADER_TOKEN))
        .put(METHOD, REQUEST_DELETE)
        .put(API_ENDPOINT, api.getRouteOwnershipDelete());

    authService.tokenInterospect(new JsonObject(), jwtAuthenticationInfo, authHandler -> {
      if (authHandler.failed()) {
        LOGGER.error("JWT Auth failed: " + authHandler.cause().getMessage());
        response.setStatusCode(401)
            .end(new RespBuilder()
                .withType(TYPE_TOKEN_INVALID)
                .withTitle(TITLE_TOKEN_INVALID)
                .withDetail(authHandler.cause().getMessage())
                .getResponse());
      } else {
        LOGGER.debug("Success: JWT Auth successful");

        JsonObject userInfo = authHandler.result();
        String requesterId = userInfo.getString(SUB);
        String organizationId = userInfo.getString(ORGANIZATION_ID);


        // Authorization Check
        if (!requesterId.equals(oldUserId)) {
          response.setStatusCode(403).end(new RespBuilder()
              .withType(TYPE_ACCESS_DENIED)
              .withTitle("Forbidden")
              .withDetail("You are not authorized to delete items owned by another user.")
              .getResponse());
          return;
        }

        // Proceed with deleteByQuery
        dbService.deleteByQueryRequest(request, organizationId, handler -> {
          if (handler.succeeded()) {
            JsonObject resultJson = handler.result();
            RespBuilder respBuilder = new RespBuilder();
            response.setStatusCode(200);
            response.end(respBuilder
                .withType(TYPE_SUCCESS)
                .withTitle(TITLE_SUCCESS)
                .withResult(resultJson)
                .withDetail("Success: Items deleted successfully")
                .getJsonResponse().encodePrettily());
          } else {
            LOGGER.error("Delete by query failed: " + handler.cause().getMessage());
            response.setStatusCode(400).end(handler.cause().getMessage());
          }
        });
      }
    });
  }

}
