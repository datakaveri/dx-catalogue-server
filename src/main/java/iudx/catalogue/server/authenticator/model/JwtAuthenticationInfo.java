package iudx.catalogue.server.authenticator.model;

import static iudx.catalogue.server.authenticator.Constants.API_ENDPOINT;
import static iudx.catalogue.server.authenticator.Constants.METHOD;
import static iudx.catalogue.server.authenticator.Constants.TOKEN;
import static iudx.catalogue.server.util.Constants.COS_ITEM;
import static iudx.catalogue.server.util.Constants.ID;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE;
import static iudx.catalogue.server.util.Constants.PROVIDER_USER_ID;
import static iudx.catalogue.server.util.Constants.RESOURCE_SERVER_URL;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

/** Represents JWT authentication information. */
@DataObject
public class JwtAuthenticationInfo {
  private final String token;
  private final String method;
  private final String apiEndpoint;
  private final String itemType;
  private final String resourceServerUrl;
  private final String providerUserId;
  private final String cosAdmin;
  private final String id;

  /**
   * Constructs JwtAuthenticationInfo from a JsonObject.
   *
   * @param json the JsonObject containing JWT authentication info
   */
  public JwtAuthenticationInfo(JsonObject json) {
    this.token = json.getString(TOKEN);
    this.method = json.getString(METHOD);
    this.apiEndpoint = json.getString(API_ENDPOINT);
    this.itemType = json.getString(ITEM_TYPE);
    this.resourceServerUrl = json.getString(RESOURCE_SERVER_URL);
    this.providerUserId = json.getString(PROVIDER_USER_ID);
    this.cosAdmin = json.getString(COS_ITEM);
    this.id = json.getString(ID);
  }

  private JwtAuthenticationInfo(Builder builder) {
    this.token = builder.token;
    this.method = builder.method;
    this.apiEndpoint = builder.apiEndpoint;
    this.itemType = builder.itemType;
    this.resourceServerUrl = builder.resourceServerUrl;
    this.providerUserId = builder.providerUserId;
    this.cosAdmin = builder.cosAdmin;
    this.id = builder.id;
  }

  // Getters
  public String getToken() {
    return token;
  }

  public String getMethod() {
    return method;
  }

  public String getApiEndpoint() {
    return apiEndpoint;
  }

  public String getItemType() {
    return itemType;
  }

  public String getResourceServerUrl() {
    return resourceServerUrl;
  }

  public String getProviderUserId() {
    return providerUserId;
  }

  public String getCosAdmin() {
    return cosAdmin;
  }

  public String getId() {
    return id;
  }

  /**
   * Converts this instance to a JsonObject.
   *
   * @return a JsonObject representation of this instance
   */
  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put(TOKEN, token);
    jsonObject.put(METHOD, method);
    jsonObject.put(API_ENDPOINT, apiEndpoint);
    jsonObject.put(ITEM_TYPE, itemType);
    if (resourceServerUrl != null) {
      jsonObject.put(RESOURCE_SERVER_URL, resourceServerUrl);
    }
    if (providerUserId != null) {
      jsonObject.put(PROVIDER_USER_ID, providerUserId);
    }
    if (cosAdmin != null) {
      jsonObject.put(COS_ITEM, cosAdmin); // Changed to use constant
    }
    if (id != null) {
      jsonObject.put(ID, id);
    }
    return jsonObject;
  }

  /** Builder class for JwtAuthenticationInfo. */
  public static class Builder {
    private String token;
    private String method;
    private String apiEndpoint;
    private String itemType;
    private String resourceServerUrl;
    private String providerUserId;
    private String cosAdmin;
    private String id;

    public Builder() {}

    public Builder(JwtAuthenticationInfo jwtAuthenticationInfo) {
      this.token = jwtAuthenticationInfo.token;
      this.method = jwtAuthenticationInfo.method;
      this.apiEndpoint = jwtAuthenticationInfo.apiEndpoint;
      this.itemType = jwtAuthenticationInfo.itemType;
      this.resourceServerUrl = jwtAuthenticationInfo.resourceServerUrl;
      this.providerUserId = jwtAuthenticationInfo.providerUserId;
      this.cosAdmin = jwtAuthenticationInfo.cosAdmin;
      this.id = jwtAuthenticationInfo.id;
    }

    public Builder setToken(String token) {
      this.token = token;
      return this;
    }

    public Builder setMethod(String method) {
      this.method = method;
      return this;
    }

    public Builder setApiEndpoint(String apiEndpoint) {
      this.apiEndpoint = apiEndpoint;
      return this;
    }

    public Builder setItemType(String itemType) {
      this.itemType = itemType;
      return this;
    }

    public Builder setResourceServerUrl(String resourceServerUrl) {
      this.resourceServerUrl = resourceServerUrl;
      return this;
    }

    public Builder setProviderUserId(String providerUserId) {
      this.providerUserId = providerUserId;
      return this;
    }

    public Builder setCosAdmin(String cosAdmin) {
      this.cosAdmin = cosAdmin;
      return this;
    }

    public Builder setId(String id) { // Fixed parameter name
      this.id = id;
      return this;
    }

    public JwtAuthenticationInfo build() {
      return new JwtAuthenticationInfo(this);
    }
  }

  /** Helper class for mutable JwtAuthenticationInfo. */
  public static class MutableJwtInfo {
    private JwtAuthenticationInfo jwtAuthenticationInfo;

    public MutableJwtInfo(JwtAuthenticationInfo initial) {
      this.jwtAuthenticationInfo = initial;
    }

    public JwtAuthenticationInfo get() {
      return jwtAuthenticationInfo;
    }

    public void set(JwtAuthenticationInfo jwtAuthenticationInfo) {
      this.jwtAuthenticationInfo = jwtAuthenticationInfo;
    }
  }
}
