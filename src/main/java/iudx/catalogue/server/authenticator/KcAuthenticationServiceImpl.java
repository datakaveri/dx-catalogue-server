package iudx.catalogue.server.authenticator;

import static iudx.catalogue.server.auditing.util.Constants.IID;
import static iudx.catalogue.server.auditing.util.Constants.USER_ID;
import static iudx.catalogue.server.auditing.util.Constants.USER_ROLE;
import static iudx.catalogue.server.authenticator.Constants.*;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_AI_MODEL;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_APPS;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_DATA_BANK;
import static iudx.catalogue.server.util.Constants.METHOD;
import static iudx.catalogue.server.util.Constants.ORGANIZATION_ID;
import static iudx.catalogue.server.util.Constants.PROVIDER_USER_ID;
import static iudx.catalogue.server.util.Constants.REQUEST_GET;

import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.JWTProcessor;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.authenticator.authorization.AuthorizationContextFactory;
import iudx.catalogue.server.authenticator.authorization.AuthorizationRequest;
import iudx.catalogue.server.authenticator.authorization.AuthorizationStratergy;
import iudx.catalogue.server.authenticator.authorization.JwtAuthorization;
import iudx.catalogue.server.authenticator.authorization.Method;
import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.util.Api;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The KC(Keycloak) Authentication Service Implementation.
 *
 * <h1>KC Authentication Service Implementation</h1>
 *
 * <p>The KC Authentication Service Implementation in the DX Catalogue Server implements the
 * definitions of the {@link iudx.catalogue.server.authenticator.AuthenticationService}.
 *
 * @version 1.0
 * @since 2023-06-14 }
 */
public class KcAuthenticationServiceImpl implements AuthenticationService {
  private static final Logger LOGGER = LogManager.getLogger(KcAuthenticationServiceImpl.class);

  final JWTProcessor<SecurityContext> jwtProcessor;
  private Api api;
  private String uacAdmin;
  private String issuer;

  /**
   * Constructs a new instance of KcAuthenticationServiceImpl.
   *
   * @param jwtProcessor The JWTProcessor used for JWT token processing and validation.
   * @param config The JsonObject configuration object containing various settings.
   * @param api The Api object used for communication with external services.
   */
  public KcAuthenticationServiceImpl(
      final JWTProcessor<SecurityContext> jwtProcessor, final JsonObject config, final Api api) {
    this.jwtProcessor = jwtProcessor;
    this.uacAdmin = config.getString(UAC_ADMIN) != null ? config.getString(UAC_ADMIN) : "";
    this.issuer = config.getString("issuer");
    this.api = api;
  }

  Future<JwtData> decodeKcToken(String token) {
    Promise<JwtData> promise = Promise.promise();
    try {
      LOGGER.debug("Starting JWT decoding...");

      // Decode the JWT header for inspection before processing
      String[] parts = token.split("\\.");
      if (parts.length != 3) {
        throw new IllegalArgumentException("Invalid JWT format");
      }

      JWTClaimsSet claimsSet = jwtProcessor.process(token, null);
      String stringjson = claimsSet.toString();

      JwtData jwtData = new JwtData(new JsonObject(stringjson));

      promise.complete(jwtData);
    } catch (Exception e) {
      LOGGER.error("Error in decoding token", e);
      promise.fail(e);
    }

    return promise.future();
  }

  @Override
  public AuthenticationService tokenInterospect(
      JsonObject request, JsonObject authenticationInfo, Handler<AsyncResult<JsonObject>> handler) {
    String endpoint = authenticationInfo.getString(API_ENDPOINT);
    // String id = authenticationInfo.getString(ID);
    String token = authenticationInfo.getString(TOKEN);
    //    String resourceServerUrl = authenticationInfo.getString(RESOURCE_SERVER_URL);
    String cosAdmin = authenticationInfo.getString("cos_admin", "");
    String itemType = authenticationInfo.getString(ITEM_TYPE, "");
    String httpMethod = authenticationInfo.getString(METHOD, "");
    String requestOrgId = authenticationInfo.getString(ORGANIZATION_ID, "");
    Future<JwtData> decodeTokenFuture = decodeKcToken(token);

    ResultContainer result = new ResultContainer();

    decodeTokenFuture
        .compose(
            decodeHandler -> {
              result.jwtData = decodeHandler;
              return isValidIssuer(result.jwtData, issuer);
            })
        .compose(
            validIssuer -> {
              return isValidEndpoint(endpoint);
            })
        .compose(
            isValidEndpointHandler -> {
              // if the token is of UAC admin, bypass ownership, else verify ownership
              if (uacAdmin.equalsIgnoreCase(result.jwtData.getSub())) {
                return Future.succeededFuture(true);
              } else {
                return isValidAdmin(result.jwtData, cosAdmin);
              }
            })
        .compose(validAdmin -> {
          if (!httpMethod.equals(REQUEST_GET)) {
            return validateAccess(result.jwtData, authenticationInfo, itemType)
                .compose(accessInfo -> {
                  JsonObject jwtJson = result.jwtData.toJson();
                  jwtJson.mergeIn(accessInfo);

                  // Extract organization IDs for comparison
                  String tokenOrgId = jwtJson.getString(ORGANIZATION_ID);

                  // Check if organization IDs match for specific item types
                  if ((itemType.equalsIgnoreCase(ITEM_TYPE_AI_MODEL)
                      || itemType.equalsIgnoreCase(ITEM_TYPE_DATA_BANK))
                      && (tokenOrgId == null || !tokenOrgId.equals(requestOrgId))) {
                    LOGGER.error("Organization ID mismatch: token={}, request={}",
                        tokenOrgId, requestOrgId);
                    return Future.failedFuture("Unauthorized: Organization ID mismatch");
                  }

                  return Future.succeededFuture(jwtJson);
                });

          } else {
            JsonObject accessInfo = new JsonObject()
                .put(USER_ROLE, result.jwtData.getRoles().get(0))
                .put(USER_ID, result.jwtData.getSub());
            JsonObject jwtJson = result.jwtData.toJson();
            jwtJson.mergeIn(accessInfo);
            return Future.succeededFuture(jwtJson);
          }
        })
        .onComplete(completeHandler -> {
          if (completeHandler.succeeded()) {
            handler.handle(Future.succeededFuture(completeHandler.result()));
          } else {
            LOGGER.debug("Error in introspecting the token: {}",
                completeHandler.cause().getMessage());
            handler.handle(Future.failedFuture(completeHandler.cause().getMessage()));
          }
        });
    return this;
  }

  private Future<Boolean> isValidIssuer(JwtData jwtData, String issuer) {
    if (jwtData.getIss().equalsIgnoreCase(issuer)) {
      return Future.succeededFuture(true);
    } else {
      return Future.failedFuture("Token not issued for this server");
    }
  }

  Future<Boolean> isValidEndpoint(String endpoint) {
    Promise<Boolean> promise = Promise.promise();

    if (endpoint.equals(api.getRouteItems()) || endpoint.equals(api.getRouteInstance())
        || endpoint.equals(api.getRouteMlayerInstance())
        || endpoint.equals(api.getRouteMlayerDomains())
        || endpoint.equals(api.getRouteSearch())
        || endpoint.equals(api.getRouteSearchMyAssets())
        || endpoint.equals(api.getRouteListMulItems())
        || endpoint.equals(api.getRouteCount())) {
      promise.complete(true);
    } else {
      LOGGER.error("Unauthorized access to endpoint {}", endpoint);
      promise.fail("Unauthorized access to endpoint " + endpoint);
    }
    return promise.future();
  }

  private Future<Boolean> isValidAdmin(JwtData jwtData, String cosAdmin) {
    // TODO: implement logic
    return Future.succeededFuture(true);
  }

  public Future<JsonObject> validateAccess(
      JwtData jwtData, JsonObject authenticationInfo, String itemType) {

    LOGGER.trace("validateAccess() started");
    Promise<JsonObject> promise = Promise.promise();

    Method method = Method.valueOf(authenticationInfo.getString(METHOD));
    String api = authenticationInfo.getString(API_ENDPOINT);
    AuthorizationRequest authRequest = new AuthorizationRequest(method, api, itemType);

    // Extra DELETE method check: match jwtData.getSub() with ownerUserId
    if (method == Method.DELETE
        && (itemType.equalsIgnoreCase(ITEM_TYPE_DATA_BANK)
        || itemType.equalsIgnoreCase(ITEM_TYPE_AI_MODEL)
        || itemType.equalsIgnoreCase(ITEM_TYPE_APPS))) {
      String subId = jwtData.getSub();
      String ownerUserId = authenticationInfo.getString(PROVIDER_USER_ID);

      if (ownerUserId == null || !ownerUserId.equals(subId)) {
        LOGGER.error("Unauthorized DELETE: Token subject [{}] does not match ownerUserId [{}]",
            subId, ownerUserId);
        promise.fail("Unauthorized: DELETE not allowed by non-owner");
        return promise.future();
      }
    }

    List<String> roles = jwtData.getRoles();
    boolean authorized = false;

    for (String role : roles) {
      if (!Set.of("consumer", "provider", "delegate", "admin", "cos_admin").contains(role)) {
        LOGGER.debug("Skipping unsupported role: " + role);
        continue;
      }
      AuthorizationStratergy authStrategy = AuthorizationContextFactory.create(role, this.api);
      JwtAuthorization jwtAuthStrategy = new JwtAuthorization(authStrategy);

      LOGGER.debug("Checking strategy for role: " + role + " ("
          + authStrategy.getClass().getSimpleName() + ")");

      if (jwtAuthStrategy.isAuthorized(authRequest)) {
        LOGGER.debug("Access allowed for role: " + role);

        JsonObject response = new JsonObject()
            .put(USER_ROLE, role)
            .put(USER_ID, jwtData.getSub())
            .put(IID, jwtData.getIid());

        promise.complete(response);
        authorized = true;
        break; // No need to check further if one role is authorized
      }
    }

    if (!authorized) {
      LOGGER.error("User access denied for all roles: " + roles);
      JsonObject result = new JsonObject().put("401", "no access provided to endpoint");
      promise.fail(result.toString());
    }

    return promise.future();
  }

  final class ResultContainer {
    JwtData jwtData;
  }

}
