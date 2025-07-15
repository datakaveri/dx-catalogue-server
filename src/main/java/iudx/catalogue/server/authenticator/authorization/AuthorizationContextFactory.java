package iudx.catalogue.server.authenticator.authorization;

import iudx.catalogue.server.util.Api;

public class AuthorizationContextFactory {

  /**
   * Creates an instance of the appropriate AuthorizationStratergy subclass
   * based on the given role and API.
   * @param role the role of the user
   * @param api the API endpoint being accessed
   * @return an instance of the appropriate AuthorizationStratergy subclass
   * @throws IllegalArgumentException if the given role is not defined in IUDX
   */
  public static AuthorizationStratergy create(String role, Api api) {
    switch (role) {
      case "consumer": {
        return ConsumerAuthStrategy.getInstance(api);
      }
      case "provider": {
        return ProviderAuthStrategy.getInstance(api);
      }
      case "delegate": {
        return DelegateAuthStrategy.getInstance(api);
      }
      case "admin": {
        return AdminAuthStrategy.getInstance(api);
      }
      case "cos_admin": {
        return CosAdminAuthStrategy.getInstance(api);
      }
      case "org_admin": {
        return OrgAdminAuthStrategy.getInstance(api);
      }
      default:
        throw new IllegalArgumentException(role + "role is not defined in IUDX");
    }
  }
}
