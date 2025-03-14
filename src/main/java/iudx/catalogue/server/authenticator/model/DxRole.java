package iudx.catalogue.server.authenticator.model;

import java.util.stream.Stream;

public enum DxRole {
  CONSUMER("consumer"),
  COS_ADMIN("cos_admin"),
  ADMIN("admin"),
  PROVIDER("provider"),
  DELEGATE("delegate");

  private final String role;

  DxRole(String role) {
    this.role = role;
  }

  /**
   * Returns the DxRole corresponding to the given role string.
   *
   * @param jwtData {@link JwtData}
   * @return the DxRole corresponding to the given role string, or null if no match is found
   */
  public static DxRole fromRole(final JwtData jwtData) {
    String role =
        jwtData.getRole().equalsIgnoreCase(DELEGATE.getRole())
            ? jwtData.getDrl()
            : jwtData.getRole();
    return
        Stream.of(values()).filter(v -> v.role.equalsIgnoreCase(role)).findAny().orElse(null);
  }

  public String getRole() {
    return this.role;
  }
}
