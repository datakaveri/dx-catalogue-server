package iudx.catalogue.server.authenticator.authorization;

import static iudx.catalogue.server.authenticator.authorization.Method.*;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_AI_MODEL;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_APPS;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_DATA_BANK;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_RESOURCE;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_RESOURCE_GROUP;

import iudx.catalogue.server.util.Api;
import java.util.ArrayList;
import java.util.List;

public class ProviderAuthStrategy implements AuthorizationStratergy {
  static List<AuthorizationRequest> accessList = new ArrayList<>();
  private static volatile ProviderAuthStrategy instance;

  private ProviderAuthStrategy(Api api) {
    buildPermissions(api);
  }

  /**
   * This method ensures that there is only one instance of ProviderAuthStrategy class created.
   *
   * @param api The API object for which the ProviderAuthStrategy instance is created.
   * @return The ProviderAuthStrategy instance.
   */
  public static ProviderAuthStrategy getInstance(Api api) {
    if (instance == null) {
      synchronized (ProviderAuthStrategy.class) {
        if (instance == null) {
          instance = new ProviderAuthStrategy(api);
        }
      }
    }
    return instance;
  }

  private void buildPermissions(Api api) {
    // /item access list
    accessList.add(new AuthorizationRequest(POST, api.getRouteItems(), ITEM_TYPE_RESOURCE_GROUP));
    accessList.add(new AuthorizationRequest(POST, api.getRouteItems(), ITEM_TYPE_RESOURCE));
    accessList.add(new AuthorizationRequest(POST, api.getRouteSearchMyAssets(), ITEM_TYPE_AI_MODEL));
    accessList.add(new AuthorizationRequest(POST, api.getRouteSearchMyAssets(), ITEM_TYPE_DATA_BANK));
    accessList.add(new AuthorizationRequest(POST, api.getRouteItems(), ITEM_TYPE_AI_MODEL));
    accessList.add(new AuthorizationRequest(POST, api.getRouteItems(), ITEM_TYPE_DATA_BANK));
    accessList.add(new AuthorizationRequest(PUT, api.getRouteItems(), ITEM_TYPE_RESOURCE_GROUP));
    accessList.add(new AuthorizationRequest(PUT, api.getRouteItems(), ITEM_TYPE_RESOURCE));
    accessList.add(new AuthorizationRequest(PUT, api.getRouteItems(), ITEM_TYPE_AI_MODEL));
    accessList.add(new AuthorizationRequest(PUT, api.getRouteItems(), ITEM_TYPE_DATA_BANK));
    accessList.add(new AuthorizationRequest(DELETE, api.getRouteItems(), ITEM_TYPE_RESOURCE_GROUP));
    accessList.add(new AuthorizationRequest(DELETE, api.getRouteItems(), ITEM_TYPE_RESOURCE));
    accessList.add(new AuthorizationRequest(DELETE, api.getRouteItems(), ITEM_TYPE_AI_MODEL));
    accessList.add(new AuthorizationRequest(DELETE, api.getRouteItems(), ITEM_TYPE_DATA_BANK));
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest) {
    return accessList.contains(authRequest);
  }
}
