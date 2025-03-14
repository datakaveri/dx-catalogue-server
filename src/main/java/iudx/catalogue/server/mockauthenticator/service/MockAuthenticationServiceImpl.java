package iudx.catalogue.server.mockauthenticator.service;

import static iudx.catalogue.server.authenticator.Constants.BODY;
import static iudx.catalogue.server.util.Constants.STATUS;
import static iudx.catalogue.server.util.Constants.SUCCESS;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import iudx.catalogue.server.authenticator.model.JwtAuthenticationInfo;
import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.authenticator.service.AuthenticationService;
import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Mock Auth Service. Bypass main auth service.
 */

public class MockAuthenticationServiceImpl implements AuthenticationService {

  private static final Logger LOGGER = LogManager.getLogger(MockAuthenticationServiceImpl.class);
  private final String authHost;

  public MockAuthenticationServiceImpl(WebClient client, String authHost) {
    this.authHost = authHost;
  }

  static void validateAuthInfo(JsonObject authInfo) throws IllegalArgumentException {
  }

  @Override
  public Future<JwtData> decodeToken(String token) {

    JsonObject result = new JsonObject();
    result.put(STATUS, SUCCESS);
    result.put(BODY, new JsonObject());
    JwtData jwtData = new JwtData(result);
    return Future.succeededFuture(jwtData);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<JwtData> tokenIntrospect(JwtData jwtData,
                                         JwtAuthenticationInfo authenticationInfo) {
    LOGGER.debug("In mock auth");
    return Future.succeededFuture(jwtData);
  }


  public boolean isPermittedProviderId(String requestId, String providerId) {
    String tipProvider = String.join("/", Arrays.asList(requestId.split("/", 3)).subList(0, 2));
    return providerId.equals(tipProvider);
  }

  public boolean isPermittedMethod(JsonArray methods, String operation) {
    return false;
  }

}
