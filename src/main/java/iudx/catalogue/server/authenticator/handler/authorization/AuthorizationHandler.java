package iudx.catalogue.server.authenticator.handler.authorization;

import static iudx.catalogue.server.util.Constants.ITEM_TYPE_COS;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_OWNER;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_PROVIDER;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_RESOURCE;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_RESOURCE_GROUP;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_RESOURCE_SERVER;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.authenticator.model.DxRole;
import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.common.HttpStatusCode;
import iudx.catalogue.server.common.ResponseUrn;
import iudx.catalogue.server.common.RoutingContextHelper;
import iudx.catalogue.server.exceptions.DxRuntimeException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AuthorizationHandler implements Handler<RoutingContext> {

  // Precompute allowed CRUD operations for each role on which type using Set for optimal lookup
  private static final Map<DxRole, Set<String>> roleEntityPermissions = new HashMap<>();

  static {
    roleEntityPermissions.put(
        DxRole.COS_ADMIN, Set.of(ITEM_TYPE_OWNER, ITEM_TYPE_COS, ITEM_TYPE_RESOURCE_SERVER));
    roleEntityPermissions.put(DxRole.ADMIN, Set.of(ITEM_TYPE_PROVIDER));
    roleEntityPermissions.put(
        DxRole.DELEGATE, Set.of(ITEM_TYPE_RESOURCE_GROUP, ITEM_TYPE_RESOURCE));
    roleEntityPermissions.put(
        DxRole.PROVIDER, Set.of(ITEM_TYPE_RESOURCE_GROUP, ITEM_TYPE_RESOURCE));
  }

  @Override
  public void handle(RoutingContext event) {
    event.next(); // Default to passing through to the next handler
  }

  /**
   * Method to set role-based access control for routes that require only role checks.
   *
   * @param allowedRoles The roles allowed for this route.
   * @return A handler to check user roles.
   */
  public Handler<RoutingContext> forRoleBasedAccess(DxRole... allowedRoles) {
    return context -> {
      JwtData jwtData = RoutingContextHelper.getJwtData(context);
      DxRole userRole = DxRole.fromRole(jwtData);

      // If the user's role is not in allowedRoles, fail with unauthorized error
      if (isRoleAllowed(userRole, allowedRoles)) {
        context.fail(unauthorizedError());
        return;
      }

      context.next(); // Proceed if role is allowed
    };
  }

  /**
   * Method to set role and entity-based access control for routes like ITEM_ROUTES.
   *
   * @param allowedRoles The roles allowed for this route.
   * @return A handler to check both user roles and item types.
   */
  public Handler<RoutingContext> forRoleAndEntityAccess(DxRole... allowedRoles) {
    return context -> {
      JwtData jwtData = RoutingContextHelper.getJwtData(context);
      DxRole userRole = DxRole.fromRole(jwtData);

      // Role-based check
      if (isRoleAllowed(userRole, allowedRoles)) {
        context.fail(unauthorizedError());
        return;
      }

      // Entity-based check
      String itemType = RoutingContextHelper.getItemType(context);
      if (!isEntityAllowedForRole(userRole, itemType)) {
        context.fail(unauthorizedError("No access for the given entity type"));
        return;
      }

      context.next(); // Proceed if both role and entity checks pass
    };
  }

  /** Helper method to check if a user's role is allowed. */
  private boolean isRoleAllowed(DxRole userRole, DxRole[] allowedRoles) {
    return !Arrays.asList(allowedRoles).contains(userRole);
  }

  /** Helper method to check if the entity is allowed for the given user role. */
  boolean isEntityAllowedForRole(DxRole userRole, String itemType) {
    Set<String> allowedEntities = roleEntityPermissions.get(userRole);
    return allowedEntities != null && allowedEntities.contains(itemType);
  }

  /** Helper method to return a DxRuntimeException for unauthorized access. */
  private DxRuntimeException unauthorizedError() {
    return new DxRuntimeException(
        HttpStatusCode.UNAUTHORIZED.getValue(),
        ResponseUrn.INVALID_TOKEN_URN,
        "No access for the given role");
  }

  private DxRuntimeException unauthorizedError(String message) {
    return new DxRuntimeException(
        HttpStatusCode.UNAUTHORIZED.getValue(), ResponseUrn.INVALID_TOKEN_URN, message);
  }
}
