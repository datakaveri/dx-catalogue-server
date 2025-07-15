package iudx.catalogue.server.authenticator.authorization;

import static iudx.catalogue.server.authenticator.authorization.Method.*;

import iudx.catalogue.server.util.Api;
import java.util.ArrayList;
import java.util.List;

public class OrgAdminAuthStrategy implements AuthorizationStratergy {

  static List<AuthorizationRequest> accessList = new ArrayList<>();
  private static volatile OrgAdminAuthStrategy instance;

  private OrgAdminAuthStrategy(Api api) {
    buildPermissions(api);
  }

  /**
   * Returns a singleton instance of the AdminAuthStrategy class for the specified API.
   *
   * @param api the API to create an AdminAuthStrategy instance for
   * @return a singleton instance of the AdminAuthStrategy class
   */
  public static OrgAdminAuthStrategy getInstance(Api api) {
    if (instance == null) {
      synchronized (OrgAdminAuthStrategy.class) {
        if (instance == null) {
          instance = new OrgAdminAuthStrategy(api);
        }
      }
    }
    return instance;
  }

  private void buildPermissions(Api api) {
    // /item access list
    accessList.add(new AuthorizationRequest(PATCH, api.getRouteOrgAsset(), ""));
    accessList.add(new AuthorizationRequest(GET, api.getRouteOrgAsset(), ""));
    accessList.add(new AuthorizationRequest(POST, api.getRouteOwnershipTransfer(), ""));
    accessList.add(new AuthorizationRequest(DELETE, api.getRouteOwnershipDelete(), ""));
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest) {
    return accessList.contains(authRequest);
  }
}
