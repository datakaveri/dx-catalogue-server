package iudx.catalogue.server.authenticator.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@DataObject(generateConverter = true, publicConverter = false)
public final class JwtData {

  private String accessToken;
  private String sub;
  private String iss;
  private String aud;
  private long exp;
  private long iat;
  private String iid;
  private String role;
  private JsonObject cons;
  private String clientId;
  private String did;
  private String drl;
  private List<String> roles = new ArrayList<>();
  private String organizationId;
  private String organizationName;

  public JwtData() {
    super();
  }

  public JwtData(JsonObject json) {
    JwtDataConverter.fromJson(json, this);
    setAccessToken(json.getString("access_token"));
    setClientId(json.getString("client_id"));
    setOrganizationId(json.getString("organisation_id"));
    setOrganizationName(json.getString("organisation_name"));
    extractRoles(json);
  }

  private void extractRoles(JsonObject json) {
    JsonObject realmAccess = json.getJsonObject("realm_access");
    if (realmAccess != null) {
      JsonArray rolesArray = realmAccess.getJsonArray("roles", new JsonArray());
      this.roles = rolesArray.stream()
          .map(Object::toString)
          .collect(Collectors.toList());
    }
  }

  /**
   * Returns a JSON representation of the JwtData object.
   *
   * @return the JSON representation of the JwtData object.
   */
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    JwtDataConverter.toJson(this, json);
    json.put("roles", new JsonArray(roles));
    return json;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(String organizationId) {
    this.organizationId = organizationId;
  }

  public String getSub() {
    return sub;
  }

  public void setSub(String sub) {
    this.sub = sub;
  }

  public String getIss() {
    return iss;
  }

  public void setIss(String iss) {
    this.iss = iss;
  }

  public String getAud() {
    return aud;
  }

  public void setAud(String aud) {
    this.aud = aud;
  }

  public String getIid() {
    return iid;
  }

  public void setIid(String iid) {
    this.iid = iid;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public JsonObject getCons() {
    return cons;
  }

  public void setCons(JsonObject cons) {
    this.cons = cons;
  }

  public long getExp() {
    return exp;
  }

  public void setExp(long exp) {
    this.exp = exp;
  }

  public long getIat() {
    return iat;
  }

  public void setIat(long iat) {
    this.iat = iat;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getDid() {
    return did;
  }

  public void setDid(String did) {
    this.did = did;
  }

  public String getDrl() {
    return drl;
  }

  public void setDrl(String drl) {
    this.drl = drl;
  }

  public List<String> getRoles() {
    return roles;
  }

  public void setRoles(List<String> roles) {
    this.roles = roles;
  }

  public String getOrganizationName() {
    return organizationName;
  }

  public void setOrganizationName(String organizationName) {
    this.organizationName = organizationName;
  }

  @Override
  public String toString() {
    return "JwtData ["
        + "accessToken=" + accessToken
        + ", sub=" + sub
        + ", iss=" + iss
        + ", aud=" + aud
        + ", exp=" + exp
        + ", iat=" + iat
        + ", iid=" + iid
        + ", role=" + role
        + ", cons=" + cons
        + ", clientId=" + clientId
        + ", did=" + did
        + ", drl=" + drl
        + ", roles=" + roles
        + ", organizationId=" + organizationId
        + ", organizationName=" + organizationName
        + "]";
  }
}
