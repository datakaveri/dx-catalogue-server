package iudx.catalogue.server.authenticator.service;

import static iudx.catalogue.server.apiserver.util.Constants.ROUTE_INSTANCE;
import static iudx.catalogue.server.apiserver.util.Constants.ROUTE_ITEMS;
import static iudx.catalogue.server.apiserver.util.Constants.ROUTE_MLAYER_DOMAIN;
import static iudx.catalogue.server.apiserver.util.Constants.ROUTE_MLAYER_INSTANCE;
import static iudx.catalogue.server.apiserver.util.Constants.ROUTE_RATING;
import static iudx.catalogue.server.apiserver.util.Constants.ROUTE_STACK;
import static iudx.catalogue.server.authenticator.Constants.RATINGS;
import static iudx.catalogue.server.authenticator.Constants.RATINGS_ENDPOINT;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_COS;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_OWNER;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_PROVIDER;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_RESOURCE;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_RESOURCE_GROUP;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_RESOURCE_SERVER;
import static iudx.catalogue.server.util.Constants.PROVIDER;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import iudx.catalogue.server.authenticator.model.JwtAuthenticationInfo;
import iudx.catalogue.server.authenticator.model.JwtData;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The JWT Authentication Service Implementation.
 *
 * <h1>JWT Authentication Service Implementation</h1>
 *
 * <p>The JWT Authentication Service implementation in the IUDX Catalogue Server implements the
 * definitions of the {@link AuthenticationService}.
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
  final String dxApiBasePath;

  public JwtAuthenticationServiceImpl(final JWTAuth jwtAuth, final JsonObject config) {
    this.jwtAuth = jwtAuth;
    this.audience = config.getString("host");
    this.consumerAudience = config.getString("consumerHost");
    this.issuer = config.getString("issuer");
    this.dxApiBasePath = config.getString("dxApiBasePath");
  }

  @Override
  public Future<JwtData> decodeToken(String jwtToken) {
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

  @Override
  public Future<JwtData> tokenIntrospect(
      JwtData decodedJwt, JwtAuthenticationInfo authenticationInfo) {

    String endPoint = authenticationInfo.getApiEndpoint();
    // TODO: remove rsUrl check
    String resourceServerRegUrl =
        Objects.requireNonNullElse(authenticationInfo.getResourceServerUrl(), "");
    LOGGER.debug(resourceServerRegUrl);

    LOGGER.debug("endpoint : " + endPoint);

    ResultContainer result = new ResultContainer();
    result.jwtData = decodedJwt;

    // skip provider id check for non-provider operations
    String provider = Objects.requireNonNullElse(authenticationInfo.getProviderUserId(), "");
    String itemType = Objects.requireNonNullElse(authenticationInfo.getItemType(), "");
    boolean skipProviderIdCheck = provider.equalsIgnoreCase("");
    boolean skipAdminCheck =
        itemType.equalsIgnoreCase("")
            || itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_GROUP)
            || itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE);

    Promise<JwtData> promise = Promise.promise();
    // Introspection logic with conditional audience validation
    // audience for ratings is different from other cos endpoints
    isValidAudienceValue(result.jwtData, determineAudienceType(endPoint, itemType),
        resourceServerRegUrl)
        .compose(
            audienceHandler -> isValidIssuer(result.jwtData, issuer))
        .compose(
            issuerHandler -> {
              if (skipProviderIdCheck) {
                return Future.succeededFuture(true);
              } else {
                return isValidProvider(result.jwtData, provider);
              }
            })
        .compose(
            validIdHandler -> isValidEndpoint(endPoint))
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
            validAdmin -> isValidItemId(result.jwtData, itemType, resourceServerRegUrl))
        .onComplete(
            completeHandler -> {
              if (completeHandler.succeeded()) {
                promise.complete();
              } else {
                promise.fail(completeHandler.cause().getMessage());
              }
            });
    return promise.future();
  }

  private String determineAudienceType(String endPoint, String itemType) {
    if (endPoint.equalsIgnoreCase(ROUTE_RATING)) {
      return RATINGS;
    }
    return itemType;
  }

  public Future<Boolean> isValidAudienceValue(JwtData jwtData, String itemType, String serverUrl) {

    LOGGER.debug("itemType: " + itemType);
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

  public Future<Boolean> isValidProvider(JwtData jwtData, String provider) {
    String jwtId = "";
    if (jwtData.getRole().equalsIgnoreCase(PROVIDER)) {

      jwtId = jwtData.getSub();
    } else if (jwtData.getRole().equalsIgnoreCase("delegate")
        && jwtData.getDrl().equalsIgnoreCase(PROVIDER)) {
      jwtId = jwtData.getDid();
    }
    LOGGER.debug("provider: " + provider);
    LOGGER.debug("jwtid: " + jwtId);

    Promise<Boolean> promise = Promise.promise();
    if (provider.equalsIgnoreCase(jwtId)) {
      promise.complete(true);
    } else {
      LOGGER.error("Incorrect sub value in jwt");
      promise.fail("Provider or delegate token required for this operation");
    }
    return promise.future();
  }

  public Future<Boolean> isValidEndpoint(String endPoint) {
    Promise<Boolean> promise = Promise.promise();

    LOGGER.debug("Endpoint in JWt is : " + endPoint);
    if (endPoint.equals(RATINGS_ENDPOINT)
        || endPoint.equals(dxApiBasePath + ROUTE_ITEMS + "/")
        || endPoint.equals(dxApiBasePath + ROUTE_ITEMS) //delete endpoint
        || endPoint.equals(dxApiBasePath + ROUTE_INSTANCE)
        || endPoint.equals(dxApiBasePath + ROUTE_MLAYER_INSTANCE)
        || endPoint.equals(dxApiBasePath + ROUTE_MLAYER_DOMAIN)
        || endPoint.equals(dxApiBasePath + ROUTE_STACK)) {
      promise.complete(true);
    } else {
      LOGGER.error("Incorrect endpoint in jwt");
      promise.fail("Incorrect endpoint in jwt");
    }
    return promise.future();
  }

  /**
   * This method validates the iid of the token for the respective operation.
   *
   * @param jwtData              which is result of decoded jwt token
   * @param itemType             which is a String
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
