package iudx.catalogue.server.authenticator.authorization;

import static iudx.catalogue.server.apiserver.util.Constants.ROUTE_RATING;
import static iudx.catalogue.server.authenticator.authorization.Method.*;

import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.util.Api;
import java.util.ArrayList;
import java.util.List;


public class ConsumerAuthStrategy implements AuthorizationStratergy {

  static List<AuthorizationRequest> accessList = new ArrayList<>();
  private static volatile ConsumerAuthStrategy instance;
  private Api api;

  private ConsumerAuthStrategy(Api api) {
    this.api = api;
    buildPermissions(api);
  }

  /**
   * Returns the singleton instance of ConsumerAuthStrategy for a given API.
   * If the instance doesn't exist, creates one and returns it.
   * @param api the API for which the instance needs to be created
   * @return the singleton instance of ConsumerAuthStrategy for the given API
   */
  public static ConsumerAuthStrategy getInstance(Api api) {
    if (instance == null) {
      synchronized (ConsumerAuthStrategy.class) {
        if (instance == null) {
          instance = new ConsumerAuthStrategy(api);
        }
      }
    }
    return instance;
  }


  private void buildPermissions(Api api) {
    accessList.add(new AuthorizationRequest(GET, ROUTE_RATING));
    accessList.add(new AuthorizationRequest(POST, ROUTE_RATING));
    accessList.add(new AuthorizationRequest(PUT, ROUTE_RATING));
    accessList.add(new AuthorizationRequest(DELETE, ROUTE_RATING));
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authorizationRequest, JwtData jwtData) {
    return accessList.contains(authorizationRequest);
  }
}
