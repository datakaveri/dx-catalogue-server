package iudx.catalogue.server.authenticator;

import static iudx.catalogue.server.auditing.util.Constants.*;
import static iudx.catalogue.server.authenticator.Constants.*;
import static iudx.catalogue.server.authenticator.Constants.METHOD;
import static iudx.catalogue.server.authenticator.Constants.RESOURCE_SERVER_URL;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import iudx.catalogue.server.authenticator.authorization.AuthorizationContextFactory;
import iudx.catalogue.server.authenticator.authorization.AuthorizationRequest;
import iudx.catalogue.server.authenticator.authorization.AuthorizationStratergy;
import iudx.catalogue.server.authenticator.authorization.JwtAuthorization;
import iudx.catalogue.server.authenticator.authorization.Method;
import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.util.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The JWT Authentication Service Implementation.
 *
 * <h1>JWT Authentication Service Implementation</h1>
 *
 * <p>The JWT Authentication Service implementation in the IUDX Catalogue Server implements the
 * definitions of the {@link iudx.catalogue.server.authenticator.AuthenticationService}.
 *
 * @version 1.0
 * @since 2021-09-23
 */
public class JwtAuthenticationServiceImpl implements AuthenticationService {
  private static final Logger LOGGER = LogManager.getLogger(JwtAuthenticationServiceImpl.class);

  final JWTAuth jwtAuth;
  final String audience;
  final String consumerAudience;
  final String issuer;
  private Api api;

  JwtAuthenticationServiceImpl(final JWTAuth jwtAuth, final JsonObject config, final Api api) {
    this.jwtAuth = jwtAuth;
    this.audience = config.getString("host");
    this.consumerAudience = config.getString("consumerHost");
    this.issuer = config.getString("issuer");
    this.api = api;
  }

  Future<JwtData> decodeJwt(String jwtToken) {
    Promise<JwtData> promise = Promise.promise();

    TokenCredentials credentials = new TokenCredentials(jwtToken);

    jwtAuth
        .authenticate(credentials)
        .onSuccess(
            user -> {
              JwtData jwtData = new JwtData(user.principal());
              promise.complete(jwtData);
            })
        .onFailure(
            err -> {
              LOGGER.error("failed to decode/validate jwt token : {}", err.getMessage());
              promise.fail("failed to decode/validate jwt token : " + err.getMessage());
            });
    return promise.future();
  }

  Future<Boolean> isValidAudienceValue(JwtData jwtData, String itemType, String serverUrl) {

    LOGGER.debug("Audience in jwt is: " + jwtData.getAud());
    LOGGER.debug(serverUrl);
    LOGGER.debug(audience);
    boolean isValidAudience;
    switch (itemType) {
      case ITEM_TYPE_PROVIDER:
      case ITEM_TYPE_RESOURCE_GROUP:
      case ITEM_TYPE_RESOURCE:
        isValidAudience = serverUrl != null && serverUrl.equalsIgnoreCase(jwtData.getAud());
        break;
      case RATINGS:
        isValidAudience =
            consumerAudience != null && consumerAudience.equalsIgnoreCase(jwtData.getAud());
        break;
      default:
        isValidAudience = audience != null && audience.equalsIgnoreCase(jwtData.getAud());
        break;
    }

    Promise<Boolean> promise = Promise.promise();

    if (isValidAudience) {
      promise.complete(true);
    } else {
      LOGGER.error("Incorrect audience value in jwt");
      promise.fail("Incorrect audience value in jwt");
    }
    return promise.future();
  }

  Future<Boolean> isValidProvider(JwtData jwtData, String provider) {

    String jwtId = "";
    if (jwtData.getRole().equalsIgnoreCase(PROVIDER)) {

      jwtId = jwtData.getSub();
    } else if (jwtData.getRole().equalsIgnoreCase("delegate")
        && jwtData.getDrl().equalsIgnoreCase(PROVIDER)) {
      jwtId = jwtData.getDid();
    }

    LOGGER.debug(provider);
    LOGGER.debug(jwtId);
    Promise<Boolean> promise = Promise.promise();
    if (provider.equalsIgnoreCase(jwtId)) {
      promise.complete(true);
    } else {
      LOGGER.error("Incorrect sub value in jwt");
      promise.fail("Provider or delegate token required for this operation");
    }
    return promise.future();
  }

  Future<Boolean> isValidEndpoint(String endPoint) {
    Promise<Boolean> promise = Promise.promise();

    LOGGER.debug("Endpoint in JWt is : " + endPoint);
    if (endPoint.equals(api.getRouteItems())
        || endPoint.equals(api.getRouteInstance())
        || endPoint.equals(RATINGS_ENDPOINT)
        || endPoint.equals(api.getRouteMlayerInstance())
        || endPoint.equals(api.getRouteMlayerDomains())
        || endPoint.equals(api.getStackRestApis())) {
      promise.complete(true);
    } else {
      LOGGER.error("Incorrect endpoint in jwt");
      promise.fail("Incorrect endpoint in jwt");
    }
    return promise.future();
  }

  /**
   * Validates the user's access to the given API endpoint based on the user's role and the HTTP
   * method used.
   *
   * @param jwtData the user's JWT data
   * @param authenticationInfo a JsonObject containing the HTTP method
   * @return a Future containing a JsonObject with user information if the access is allowed, or a
   *     failure if the access is denied
   */
  public Future<JsonObject> validateAccess(
      JwtData jwtData, JsonObject authenticationInfo, String itemType) {
    LOGGER.trace("validateAccess() started");
    Promise<JsonObject> promise = Promise.promise();

    Method method = Method.valueOf(authenticationInfo.getString(METHOD));
    String api = authenticationInfo.getString(API_ENDPOINT);

    AuthorizationRequest authRequest = new AuthorizationRequest(method, api, itemType);

    AuthorizationStratergy authStrategy =
        AuthorizationContextFactory.create(jwtData.getRole(), this.api);
    LOGGER.debug("strategy: " + authStrategy.getClass().getSimpleName());

    JwtAuthorization jwtAuthStrategy = new JwtAuthorization(authStrategy);
    LOGGER.debug("endpoint: " + authenticationInfo.getString(API_ENDPOINT));

    if (jwtAuthStrategy.isAuthorized(authRequest)) {
      LOGGER.debug("User access is allowed");
      JsonObject response = new JsonObject();
      // adding user id, user role and iid to response for auditing purpose
      response
          .put(USER_ROLE, jwtData.getRole())
          .put(USER_ID, jwtData.getSub())
          .put(IID, jwtData.getIid());
      promise.complete(response);
    } else {
      LOGGER.error("user access denied");
      JsonObject result = new JsonObject().put("401", "no access provided to endpoint");
      promise.fail(result.toString());
    }
    return promise.future();
  }

  @Override
  public AuthenticationService tokenInterospect(
      JsonObject request, JsonObject authenticationInfo, Handler<AsyncResult<JsonObject>> handler) {
    String endPoint = authenticationInfo.getString(API_ENDPOINT);
    String provider = authenticationInfo.getString(PROVIDER_USER_ID, "");
    String token = authenticationInfo.getString(TOKEN);
    String itemType = authenticationInfo.getString(ITEM_TYPE, "");
    // TODO: remove rsUrl check
    String resourceServerRegUrl = authenticationInfo.getString(RESOURCE_SERVER_URL, "");
    LOGGER.debug(resourceServerRegUrl);

    LOGGER.debug("endpoint : " + endPoint);
    Future<JwtData> jwtDecodeFuture = decodeJwt(token);

    ResultContainer result = new ResultContainer();
    // skip provider id check for non-provider operations
    boolean skipProviderIdCheck = provider.equalsIgnoreCase("");
    boolean skipAdminCheck =
        itemType.equalsIgnoreCase("")
            || itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_GROUP)
            || itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE);

    jwtDecodeFuture
        .compose(
            decodeHandler -> {
              result.jwtData = decodeHandler;

              // audience for ratings is different from other cos endpoints
              if (endPoint.equalsIgnoreCase(api.getRouteRating())) {
                return isValidAudienceValue(result.jwtData, RATINGS, resourceServerRegUrl);
              }
              return isValidAudienceValue(result.jwtData, itemType, resourceServerRegUrl);
            })
        .compose(
            audienceHandler -> {
              return isValidIssuer(result.jwtData, issuer);
            })
        .compose(
            issuerHandler -> {
              if (skipProviderIdCheck) {
                return Future.succeededFuture(true);
              } else {
                return isValidProvider(result.jwtData, provider);
              }
            })
        .compose(
            validIdHandler -> {
              return isValidEndpoint(endPoint);
            })
        .compose(
            validEndpointHandler -> {
              // verify admin if itemType is COS/RS/Provider
              if (skipAdminCheck) {
                return Future.succeededFuture(true);
              } else {
                return isValidAdmin(result.jwtData);
              }
            })
        .compose(
            validAdmin -> {
              return isValidItemId(result.jwtData, itemType, resourceServerRegUrl);
            })
        .compose(
            validIid -> {
              return validateAccess(result.jwtData, authenticationInfo, itemType);
            })
        .onComplete(
            completeHandler -> {
              if (completeHandler.succeeded()) {
                handler.handle(Future.succeededFuture(completeHandler.result()));
              } else {
                handler.handle(Future.failedFuture(completeHandler.cause().getMessage()));
              }
            });
    return this;
  }

  /**
   * This method validates the iid of the token for the respective operation.
   *
   * @param jwtData which is result of decoded jwt token
   * @param itemType which is a String
   * @param resourceServerRegUrl which is a String
   * @return Vertx Future which is of the type boolean
   */
  Future<Boolean> isValidItemId(JwtData jwtData, String itemType, String resourceServerRegUrl) {
    String iid = jwtData.getIid();
    String type = iid.substring(0, iid.indexOf(":"));
    String server = iid.substring(iid.indexOf(":") + 1);
    boolean isValidIid;

    LOGGER.debug(server.equalsIgnoreCase(resourceServerRegUrl));
    LOGGER.debug(type);

    switch (itemType) {
      case ITEM_TYPE_OWNER:
      case ITEM_TYPE_COS:
      case ITEM_TYPE_RESOURCE_SERVER:
        isValidIid = type.equalsIgnoreCase("cos") && server.equalsIgnoreCase(issuer);
        break;
      case ITEM_TYPE_PROVIDER:
      case ITEM_TYPE_RESOURCE_GROUP:
      case ITEM_TYPE_RESOURCE:
        isValidIid = type.equalsIgnoreCase("rs") && server.equalsIgnoreCase(resourceServerRegUrl);
        break;
      default:
        isValidIid = true;
    }

    if (isValidIid) {
      return Future.succeededFuture(true);
    } else {
      return Future.failedFuture("Token used is not issued for this item");
    }
  }

  Future<Boolean> isValidIssuer(JwtData jwtData, String issuer) {
    if (jwtData.getIss().equalsIgnoreCase(issuer)) {
      return Future.succeededFuture(true);
    } else {
      return Future.failedFuture("Token not issued for this server");
    }
  }

  Future<Boolean> isValidAdmin(JwtData jwtData) {
    if (jwtData.getRole().equalsIgnoreCase("cos_admin")) {
      return Future.succeededFuture(true);
    } else if (jwtData.getRole().equalsIgnoreCase("admin")) {
      return Future.succeededFuture(true);
    } else {
      return Future.failedFuture("admin token required for this operation");
    }
  }

  // class to contain intermediate data for token introspection
  final class ResultContainer {
    JwtData jwtData;
  }
}
