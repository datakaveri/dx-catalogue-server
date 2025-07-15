package iudx.catalogue.server.apiserver;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.authenticator.Constants.*;
import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.util.Constants.METHOD;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.apiserver.util.QueryMapper;
import iudx.catalogue.server.apiserver.util.RespBuilder;
import iudx.catalogue.server.authenticator.AuthenticationService;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.util.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OrganisationApis {
  private static final Logger LOGGER = LogManager.getLogger(OrganisationApis.class);
  private final Api api;
  private DatabaseService dbService;
  private AuthenticationService authService;

  public OrganisationApis(Api api) {
    this.api = api;
  }

  /**
   * Sets the database service, and auth service for this class.
   *
   * @param dbService        the database service to be set
   * @param authService      the authservice to be set
   */
  public void setService(DatabaseService dbService, AuthenticationService authService) {
    this.dbService = dbService;
    this.authService = authService;
  }

  public void partialUpdateItemHandler(RoutingContext ctx) {
    HttpServerResponse response = ctx.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject jwtAuthenticationInfo = new JsonObject();

    // populating jwt authentication info ->
    jwtAuthenticationInfo
        .put(TOKEN, ctx.get(HEADER_TOKEN))
        .put(METHOD, REQUEST_PATCH)
        .put(API_ENDPOINT, api.getRouteOrgAsset());
    JsonObject requestBody = ctx.body().asJsonObject();

    authService.tokenInterospect(new JsonObject(),
        jwtAuthenticationInfo, authHandler -> {
          if (authHandler.failed()) {
            LOGGER.error("Error: " + authHandler.cause().getMessage());
            response.setStatusCode(401)
                .end(new RespBuilder()
                    .withType(TYPE_TOKEN_INVALID)
                    .withTitle(TITLE_TOKEN_INVALID)
                    .withDetail(authHandler.cause().getMessage())
                    .getResponse());
          } else {
            LOGGER.debug("Success: JWT Auth successful");

            String itemId = ctx.request().getParam(ID);
            if (itemId == null || itemId.isEmpty()) {
              response.setStatusCode(400)
                  .end(new JsonObject().put("error", "Missing or invalid item ID").encode());
              return;
            }

            // Optionally validate token, role, etc. here

            requestBody.put(ID, itemId);

            dbService.partialUpdate(requestBody, dbRes -> {
              if (dbRes.succeeded()) {
                LOGGER.debug("Success: " + dbRes.result());
                JsonObject result = dbRes.result();
                response.setStatusCode(200)
                    .end(new RespBuilder()
                        .withType(TYPE_SUCCESS)
                        .withTitle(TITLE_SUCCESS)
                        .withResult(new JsonArray().add(result))
                        .withDetail("Item updated successfully")
                        .getJsonResponse().encodePrettily());
              } else {
                if (dbRes.cause().getMessage().contains(TYPE_ITEM_NOT_FOUND)) {
                  response.setStatusCode(404).end(dbRes.cause().getMessage());
                } else {
                  response.setStatusCode(400).end(dbRes.cause().getMessage());
                }
              }
            });
          }
        });
  }

  public void getItemsByOrgHandler(RoutingContext ctx) {
    HttpServerResponse response = ctx.response();
    HttpServerRequest request = ctx.request();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject requestBody = new JsonObject();
    // Set default size and page
    int size = DEFAULT_MAX_PAGE_SIZE;
    int page = DEFAULT_PAGE_NUMBER;

    // Override if query params are present
    if (request.getParam(SIZE_KEY) != null) {
      try {
        size = Integer.parseInt(request.getParam(SIZE_KEY));
      } catch (NumberFormatException e) {
        LOGGER.warn("Invalid size param, defaulting to {}", size);
      }
    }

    if (request.getParam(PAGE_KEY) != null) {
      try {
        page = Integer.parseInt(request.getParam(PAGE_KEY));
      } catch (NumberFormatException e) {
        LOGGER.warn("Invalid page param, defaulting to {}", page);
      }
    }

    int offset = size * (page - 1);

    requestBody.put(SIZE_KEY, size);
    requestBody.put(LIMIT, size);
    requestBody.put(PAGE_KEY, page);
    requestBody.put(OFFSET, offset);

    QueryMapper.extractSortFromRawUri(request.uri(), requestBody);

    // Build JWT auth info
    JsonObject jwtAuthenticationInfo = new JsonObject()
        .put(TOKEN, ctx.get(HEADER_TOKEN))
        .put(METHOD, REQUEST_GET)
        .put(API_ENDPOINT, api.getRouteOrgAsset());

    authService.tokenInterospect(new JsonObject(), jwtAuthenticationInfo, authHandler -> {
      if (authHandler.failed()) {
        LOGGER.error("Token validation failed: " + authHandler.cause().getMessage());
        response.setStatusCode(401)
            .end(new RespBuilder()
                .withType(TYPE_TOKEN_INVALID)
                .withTitle(TITLE_TOKEN_INVALID)
                .withDetail(authHandler.cause().getMessage())
                .getJsonResponse().encodePrettily());
      } else {
        LOGGER.debug("Success: JWT Auth successful");
        JsonObject authResult = authHandler.result();
        String orgId = authResult.getString(ORGANIZATION_ID);
        requestBody.put(ORGANIZATION_ID, orgId);

        if (orgId == null || orgId.isEmpty()) {
          response.setStatusCode(400)
              .end(new JsonObject().put(ERROR, "Missing organizationId in token").encode());
          return;
        }

        dbService.getDocsByOrgId(requestBody, dbHandler -> {
          if (dbHandler.succeeded()) {
            JsonObject resultJson = dbHandler.result();
            String status = resultJson.getString(STATUS);
            if (status.equalsIgnoreCase(SUCCESS)) {
              LOGGER.info("Success: search query");
              response.setStatusCode(200);
            } else if (status.equalsIgnoreCase(PARTIAL_CONTENT)) {
              LOGGER.info("Success: search query");
              response.setStatusCode(206);
            } else {
              LOGGER.error("Fail: search query");
              response.setStatusCode(400);
            }
            response.end(resultJson.toString());
          } else if (dbHandler.failed()) {
            LOGGER.error("Fail: Search;" + dbHandler.cause().getMessage());
            response.setStatusCode(400).end(dbHandler.cause().getMessage());
          }
        });
      }
    });
  }
}
