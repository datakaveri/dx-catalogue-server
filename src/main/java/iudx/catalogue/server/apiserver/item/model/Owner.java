package iudx.catalogue.server.apiserver.item.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.UUID;

public class Owner implements Item {
  private static final String ID_REGEX =
      "^[a-zA-Z0-9]{8}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{12}$";
  private static final String NAME_REGEX = "^[a-zA-Z0-9]([\\w-]*[a-zA-Z0-9 ])?$";
  private final JsonObject requestJson;
  private UUID id;
  private List<String> type;
  private String name;
  private String description;

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

  public Owner(JsonObject json) throws IllegalArgumentException {
    this.requestJson = json.copy(); // Store a copy of the input JSON
    this.context = json.getString("@context");
    if (json.containsKey("id")
        && !json.getString("id").isEmpty()
        && !json.getString("id").matches(ID_REGEX)) { // Check regex first
      throw new IllegalArgumentException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]",
              ID_REGEX, json.getString("id")));
    }
    this.id = UUID.fromString(json.getString("id", UUID.randomUUID().toString()));
    this.type = json.getJsonArray("type").getList();
    this.name = json.getString("name");
    this.description = json.getString("description");
    this.itemStatus = json.getString("itemStatus");
    this.itemCreatedAt = json.getString("itemCreatedAt");
    validateFields();
  }

  private void validateFields() {
    if (!id.toString().matches(ID_REGEX)) {
      throw new IllegalArgumentException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]", ID_REGEX, id));
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

  public JsonObject getRequestJson() {
    return requestJson;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();

    // Set explicitly defined fields
    json.put("@context", context);
    json.put("id", id.toString());
    json.put("type", new JsonArray(type));
    json.put("name", name);
    json.put("description", description);
    json.put("itemStatus", getItemStatus());
    json.put("itemCreatedAt", getItemCreatedAt());
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
