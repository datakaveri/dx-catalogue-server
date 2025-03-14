package iudx.catalogue.server.authenticator.service;

import static iudx.catalogue.server.authenticator.Constants.*;

import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.JWTProcessor;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.authenticator.model.JwtAuthenticationInfo;
import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.util.Api;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The KC(Keycloak) Authentication Service Implementation.
 *
 * <h1>KC Authentication Service Implementation</h1>
 *
 * <p>The KC Authentication Service Implementation in the DX Catalogue Server implements the
 * definitions of the {@link AuthenticationService}.
 *
 * @version 1.0
 * @since 2023-06-14 }
 */
public class KcAuthenticationServiceImpl implements AuthenticationService {
  private static final Logger LOGGER = LogManager.getLogger(KcAuthenticationServiceImpl.class);

  final JWTProcessor<SecurityContext> jwtProcessor;
  private final Api api;
  private final String uacAdmin;
  private final String issuer;

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

  @Override
  public Future<JwtData> decodeToken(String token) {
    Promise<JwtData> promise = Promise.promise();
    try {
      JWTClaimsSet claimsSet = jwtProcessor.process(token, null);
      String stringJson = claimsSet.toString();
      JwtData jwtData = new JwtData(new JsonObject(stringJson));
      LOGGER.debug(jwtData);
      promise.complete(jwtData);
    } catch (Exception e) {
      LOGGER.error(e.getMessage());
      promise.fail(e.getMessage());
    }

    return promise.future();
  }

  @Override
  public Future<JwtData> tokenIntrospect(
      JwtData decodedKc, JwtAuthenticationInfo authenticationInfo) {
    Promise<JwtData> promise = Promise.promise();
    String endpoint = authenticationInfo.getApiEndpoint();
    // String id = authenticationInfo.getId();
    String token = authenticationInfo.getToken();
    //    String resourceServerUrl = authenticationInfo.getResourceServerUrl();
    String cosAdmin = Objects.requireNonNullElse(authenticationInfo.getCosAdmin(), "");

    ResultContainer result = new ResultContainer();
    result.jwtData = decodedKc;

    isValidIssuer(result.jwtData, issuer)
        .compose(validIssuer -> isValidEndpoint(endpoint))
        .compose(
            isValidEndpointHandler -> {
              // if the token is of UAC admin, bypass ownership, else verify ownership
              if (uacAdmin.equalsIgnoreCase(result.jwtData.getSub())) {
                return Future.succeededFuture(true);
              } else {
                return isValidAdmin(result.jwtData, cosAdmin);
              }
            })
        .onComplete(
            completeHandler -> {
              if (completeHandler.succeeded()) {
                promise.complete();
              } else {
                LOGGER.debug(completeHandler.cause().getMessage());
                promise.fail(completeHandler.cause().getMessage());
              }
            });
    return promise.future();
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

    if (endpoint.equals(api.getRouteItems())
        || endpoint.equals(api.getRouteInstance())
        || endpoint.equals(api.getRouteMlayerInstance())
        || endpoint.equals(api.getRouteMlayerDomains())) {
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

  final class ResultContainer {
    JwtData jwtData;
  }
}
