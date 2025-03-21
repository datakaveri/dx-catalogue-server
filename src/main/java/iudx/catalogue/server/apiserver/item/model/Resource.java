package iudx.catalogue.server.apiserver.item.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.UUID;

public class Resource implements Item {
  private static final String NAME_REGEX = "^[a-zA-Z0-9]([\\w-]*[a-zA-Z0-9 ])?$";
  private static final String UUID_REGEX =
      "^[a-zA-Z0-9]{8}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{12}$";
  private static final String ACCESS_POLICY_REGEX = "^(SECURE|OPEN|PII|secure|open|pii)$";
  private final JsonObject requestJson;
  private String provider;
  private String resourceGroup;
  private String resourceServer;
  private List<String> tags;
  private String apdURL;
  private String accessPolicy;
  private UUID id;
  private List<String> type;
  private String name;
  private String description;
  private String ownerUserId;
  private String cos;

  @JsonProperty("@context")
  private String context;

  private String itemStatus;
  private String itemCreatedAt;

  @JsonProperty("_summary")
  private String summary;

  @JsonProperty("_geosummary")
  private JsonObject geoSummary;

  @JsonProperty("_word_vector")
  private JsonArray wordVector;

  public Resource(JsonObject json) {
    this.requestJson = json.copy(); // Store a copy of the input JSON
    this.context = json.getString("@context");
    if (json.containsKey("id")
        && !json.getString("id").isEmpty()
        && !json.getString("id").matches(UUID_REGEX)) { // Check regex first
      throw new IllegalArgumentException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]",
              UUID_REGEX, json.getString("id")));
    }
    this.id = UUID.fromString(json.getString("id", UUID.randomUUID().toString()));
    this.type = json.getJsonArray("type").getList();
    this.name = json.getString("name");
    this.description = json.getString("description");
    this.tags = json.getJsonArray("tags").getList();
    this.provider = json.getString("provider");
    this.resourceGroup = json.getString("resourceGroup");
    this.resourceServer = json.getString("resourceServer");
    this.ownerUserId = json.getString("ownerUserId");
    this.cos = json.getString("cos");
    this.apdURL = json.getString("apdURL");
    this.accessPolicy = json.getString("accessPolicy");
    this.itemStatus = json.getString("itemStatus");
    this.itemCreatedAt = json.getString("itemCreatedAt");
    // Perform validation
    validateFields();
  }

  private void validateFields() {
    if (!id.toString().matches(UUID_REGEX)) {
      throw new IllegalArgumentException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]", UUID_REGEX, id));
    }
    if (name == null) {
      throw new IllegalArgumentException("[object has missing required properties ([\"name\"])])");
    }
    if (description == null) {
      throw new IllegalArgumentException(
          "[object has missing required properties ([\"description\"])])");
    }
    if (!name.matches(NAME_REGEX)) {
      throw new IllegalArgumentException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]", NAME_REGEX, name));
    }
    if (tags == null) {
      throw new IllegalArgumentException("[object has missing required properties ([\"tags\"])])");
    }
    if (provider == null) {
      throw new IllegalArgumentException(
          "[object has missing required properties ([\"provider\"])])");
    }
    if (!provider.matches(UUID_REGEX)) {
      throw new IllegalArgumentException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]", UUID_REGEX, provider));
    }
    if (resourceGroup == null) {
      throw new IllegalArgumentException(
          "[object has missing required properties ([\"resourceGroup\"])])");
    }
    if (!resourceGroup.matches(UUID_REGEX)) {
      throw new IllegalArgumentException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]",
              UUID_REGEX, resourceGroup));
    }
    if (resourceServer == null) {
      throw new IllegalArgumentException(
          "[object has missing required properties ([\"resourceServer\"])])");
    }
    if (!resourceServer.matches(UUID_REGEX)) {
      throw new IllegalArgumentException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]",
              UUID_REGEX, resourceServer));
    }
    if (apdURL == null) {
      throw new IllegalArgumentException(
          "[object has missing required properties ([\"apdURL\"])])");
    }
    if (accessPolicy == null) {
      throw new IllegalArgumentException(
          "[object has missing required properties ([\"accessPolicy\"])])");
    }
    if (!accessPolicy.matches(ACCESS_POLICY_REGEX)) {
      throw new IllegalArgumentException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]",
              UUID_REGEX, resourceServer));
    }
  }

  @Override
  public UUID getId() {
    return id;
  }

  @Override
  public void setId(UUID id) {
    this.id = id;
  }

  @Override
  public List<String> getType() {
    return type;
  }

  @Override
  public void setType(List<String> type) {
    this.type = type;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public void setDescription(String description) {
    this.description = description;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public String getOwnerUserId() {
    return ownerUserId;
  }

  public void setOwnerUserId(String ownerUserId) {
    this.ownerUserId = ownerUserId;
  }

  public String getCos() {
    return cos;
  }

  public void setCos(String cos) {
    this.cos = cos;
  }

  @Override
  public String getContext() {
    return context;
  }

  @Override
  public void setContext(String context) {
    this.context = context;
  }

  @Override
  public String getItemStatus() {
    return itemStatus;
  }

  @Override
  public void setItemStatus(String itemStatus) {
    this.itemStatus = itemStatus;
  }

  @Override
  public String getItemCreatedAt() {
    return itemCreatedAt;
  }

  @Override
  public void setItemCreatedAt(String itemCreatedAt) {
    this.itemCreatedAt = itemCreatedAt;
  }

  @Override
  public String getSummary() {
    return summary;
  }

  @Override
  public void setSummary(String summary) {
    this.summary = summary;
  }

  @Override
  public JsonObject getGeoSummary() {
    return geoSummary;
  }

  @Override
  public void setGeoSummary(JsonObject geoSummary) {
    this.geoSummary = geoSummary;
  }

  @Override
  public JsonArray getWordVector() {
    return wordVector;
  }

  @Override
  public void setWordVector(JsonArray wordVector) {
    this.wordVector = wordVector;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getResourceGroup() {
    return resourceGroup;
  }

  public void setResourceGroup(String resourceGroup) {
    this.resourceGroup = resourceGroup;
  }

  public String getResourceServer() {
    return resourceServer;
  }

  public void setResourceServer(String resourceServer) {
    this.resourceServer = resourceServer;
  }

  public String getApdURL() {
    return apdURL;
  }

  public void setApdURL(String apdURL) {
    this.apdURL = apdURL;
  }

  public String getAccessPolicy() {
    return accessPolicy;
  }

  public void setAccessPolicy(String accessPolicy) {
    this.accessPolicy = accessPolicy;
  }

  public JsonObject getRequestJson() {
    return requestJson;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();

    json.put("@context", context);
    json.put("id", id.toString());
    json.put("type", new JsonArray(type));
    json.put("name", name);
    json.put("description", description);
    json.put("tags", new JsonArray(tags));
    json.put("provider", provider);
    json.put("ownerUserId", ownerUserId);
    json.put("cos", cos);
    json.put("resourceGroup", resourceGroup);
    json.put("resourceServer", resourceServer);
    json.put("apdURL", apdURL);
    json.put("accessPolicy", accessPolicy);
    json.put("itemStatus", itemStatus);
    json.put("itemCreatedAt", itemCreatedAt);
    if (summary != null) json.put("_summary", summary);
    if (geoSummary != null) json.put("_geosummary", geoSummary);
    if (wordVector != null) json.put("_word_vector", wordVector);
    // Add additional fields from the original JSON request
    JsonObject requestJson = getRequestJson();
    for (String key : requestJson.fieldNames()) {
      if (!json.containsKey(key)) {
        json.put(key, requestJson.getValue(key));
      }
    }
    return json;
  }
}
