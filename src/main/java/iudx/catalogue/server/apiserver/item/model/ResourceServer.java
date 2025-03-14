package iudx.catalogue.server.apiserver.item.model;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ResourceServer implements Item {

  private static final String UUID_REGEX =
      "^[a-zA-Z0-9]{8}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{12}$";
  private static final String NAME_PATTERN = "^[a-zA-Z0-9]([\\w-. ]*[a-zA-Z0-9 ])?$";
  private static final String REG_URL_PATTERN = "^[a-zA-Z0-9-]{2,}(\\.[a-zA-Z0-9-]{2,}){1,10}$";
  private final JsonObject requestJson;
  private UUID cos;
  private UUID owner;
  private String itemStatus;
  private String itemCreatedAt;
  private ResourceServerOrg resourceServerOrg;
  private Location location;
  private String resourceServerRegURL;
  private List<ResourceAccessModality> resourceAccessModalities;
  private UUID id;
  private List<String> type;
  private String name;
  private String description;
  private List<String> tags;
  private String context;

  public ResourceServer(JsonObject json) {
    this.requestJson = json.copy(); // Store a copy of the input JSON
    this.context = json.getString("@context");
    this.id = UUID.fromString(json.getString("id", UUID.randomUUID().toString()));
    this.type = json.getJsonArray("type").getList();
    this.name = json.getString("name");
    this.description = json.getString("description");
    this.tags = json.getJsonArray("tags").getList();
    this.cos = parseUUID(json.getString("cos"), "cos");
    this.owner = parseUUID(json.getString("owner"), "owner");
    this.itemStatus = json.getString("itemStatus");
    this.itemCreatedAt = json.getString("itemCreatedAt");
    this.resourceServerRegURL = json.getString("resourceServerRegURL");
    this.itemStatus = json.getString("itemStatus");
    this.itemCreatedAt = json.getString("itemCreatedAt");

    JsonObject orgJson = json.getJsonObject("resourceServerOrg");
    if (orgJson != null) {
      this.resourceServerOrg = new ResourceServerOrg(orgJson);
    }

    JsonObject locationJson = json.getJsonObject("location");
    if (locationJson != null) {
      this.location = new Location(locationJson);
    }

    JsonArray modalitiesJson = json.getJsonArray("resourceAccessModalities");
    if (modalitiesJson != null) {
      this.resourceAccessModalities =
          modalitiesJson.stream()
              .map(obj -> new ResourceAccessModality((JsonObject) obj))
              .collect(Collectors.toList());
    }

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
    if (!name.matches(NAME_PATTERN)) {
      throw new IllegalArgumentException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]", NAME_PATTERN, name));
    }
    if (cos == null) {
      throw new IllegalArgumentException("[object has missing required properties ([\"cos\"])])");
    }
    if (!cos.toString().matches(UUID_REGEX)) {
      throw new IllegalArgumentException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]", UUID_REGEX, cos));
    }
    if (owner == null) {
      throw new IllegalArgumentException("[object has missing required properties ([\"owner\"])])");
    }
    if (!owner.toString().matches(UUID_REGEX)) {
      throw new IllegalArgumentException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]", UUID_REGEX, owner));
    }
    if (resourceServerRegURL == null) {
      throw new IllegalArgumentException(
          "[object has missing required properties ([\"resourceServerRegURL\"])])");
    }
    if (!resourceServerRegURL.matches(REG_URL_PATTERN)) {
      throw new IllegalArgumentException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]",
              REG_URL_PATTERN, resourceServerRegURL));
    }
    if (resourceServerOrg == null) {
      throw new IllegalArgumentException(
          "[object has missing required properties ([\"resourceServerOrg\"])])");
    }
    if (tags == null) {
      throw new IllegalArgumentException("[object has missing required properties ([\"tags\"])])");
    }
    if (resourceAccessModalities == null) {
      throw new IllegalArgumentException(
          "[object has missing required properties ([\"resourceAccessModalities\"])])");
    }
  }

  // Utility method to parse and validate UUIDs
  private UUID parseUUID(String value, String fieldName) {
    if (value == null) {
      throw new IllegalArgumentException(
          "[object has missing required properties ([\"" + fieldName + "\"])])");
    }
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]", UUID_REGEX, value));
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

  public UUID getCos() {
    return cos;
  }

  public void setCos(UUID cos) {
    this.cos = cos;
  }

  public UUID getOwner() {
    return owner;
  }

  public void setOwner(UUID owner) {
    this.owner = owner;
  }

  public ResourceServerOrg getResourceServerOrg() {
    return resourceServerOrg;
  }

  public void setResourceServerOrg(ResourceServerOrg resourceServerOrg) {
    this.resourceServerOrg = resourceServerOrg;
  }

  public Location getLocation() {
    return location;
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  public String getResourceServerRegURL() {
    return resourceServerRegURL;
  }

  public void setResourceServerRegURL(String resourceServerRegURL) {
    this.resourceServerRegURL = resourceServerRegURL;
  }

  public List<ResourceAccessModality> getResourceAccessModalities() {
    return new ArrayList<>(resourceAccessModalities);
  }

  public void setResourceAccessModalities(List<ResourceAccessModality> resourceAccessModalities) {
    this.resourceAccessModalities = new ArrayList<>(resourceAccessModalities);
  }

  public JsonObject getRequestJson() {
    return requestJson;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put("@context", context);
    json.put("id", id.toString());
    json.put("type", new JsonArray(type));
    json.put("name", name);
    json.put("description", description);
    json.put("tags", new JsonArray(tags));
    json.put("cos", cos.toString());
    json.put("owner", owner.toString());
    json.put("itemStatus", itemStatus);
    json.put("itemCreatedAt", itemCreatedAt);
    json.put("resourceServerRegURL", resourceServerRegURL);

    if (resourceServerOrg != null) {
      json.put("resourceServerOrg", resourceServerOrg.toJson());
    }
    if (location != null) {
      json.put("location", location.toJson());
    }
    if (resourceAccessModalities != null && !resourceAccessModalities.isEmpty()) {
      json.put(
          "resourceAccessModalities",
          new JsonArray(
              resourceAccessModalities.stream()
                  .map(ResourceAccessModality::toJson)
                  .collect(Collectors.toList())));
    }
    // Add additional fields from the original JSON request
    JsonObject requestJson = getRequestJson();
    for (String key : requestJson.fieldNames()) {
      if (!json.containsKey(key)) {
        json.put(key, requestJson.getValue(key));
      }
    }

    return json;
  }

  public static class ResourceServerOrg {
    private final String name;
    private final String additionalInfoURL;
    private Location location;

    public ResourceServerOrg(JsonObject json) {
      this.name = json.getString("name");
      this.additionalInfoURL = json.getString("additionalInfoURL");
      JsonObject locationJson = json.getJsonObject("location");
      if (locationJson != null) {
        this.location = new Location(locationJson);
      }
      validateResourceServerOrgFields();
    }

    private void validateResourceServerOrgFields() {
      if (name == null) {
        throw new IllegalArgumentException(
            "[object has missing required properties ([\"name\"])])");
      }
      if (additionalInfoURL == null) {
        throw new IllegalArgumentException(
            "[object has missing required properties ([\"additionalInfoURL\"])])");
      }
      if (location == null) {
        throw new IllegalArgumentException(
            "[object has missing required properties ([\"location\"])])");
      }
    }

    public JsonObject toJson() {
      JsonObject json = new JsonObject();
      json.put("name", name);
      json.put("additionalInfoURL", additionalInfoURL);
      json.put("location", location.toJson());
      return json;
    }
  }

  public static class Location {
    private final String type;
    private final String address;
    private final Geometry geometry;

    public Location(JsonObject json) {
      this.type = json.getString("type");
      this.address = json.getString("address");
      JsonObject geometryJson = json.getJsonObject("geometry");
      this.geometry = new Geometry(geometryJson);
      validateLocationFields();
    }

    private void validateLocationFields() {
      if (type == null) {
        throw new IllegalArgumentException(
            "[object has missing required properties ([\"type\"])])");
      }
      if (address == null) {
        throw new IllegalArgumentException(
            "[object has missing required properties ([\"address\"])])");
      }
      if (geometry == null) {
        throw new IllegalArgumentException(
            "[object has missing required properties ([\"geometry\"])])");
      }
    }

    public JsonObject toJson() {
      JsonObject json = new JsonObject();
      json.put("type", type);
      json.put("address", address);
      json.put("geometry", geometry.toJson());
      return json;
    }
  }

  public static class Geometry {
    private final String type;
    private final List<Double> coordinates;

    public Geometry(JsonObject json) {
      this.type = json.getString("type");
      this.coordinates =
          json.getJsonArray("coordinates").stream()
              .map(obj -> ((Number) obj).doubleValue())
              .collect(Collectors.toList());
      validateGeometryFields();
    }

    private void validateGeometryFields() {
      if (type == null) {
        throw new IllegalArgumentException(
            "[object has missing required properties ([\"type\"])])");
      }
      if (coordinates == null || coordinates.isEmpty()) {
        throw new IllegalArgumentException(
            "[object has missing required properties ([\"coordinates\"])])");
      }
    }

    public JsonObject toJson() {
      JsonObject json = new JsonObject();
      json.put("type", type);
      json.put("coordinates", new JsonArray(coordinates));
      return json;
    }
  }

  public static class ResourceAccessModality {
    private final List<String> type;
    private final String protocol;
    private final String accessURL;
    private final Integer port;

    public ResourceAccessModality(JsonObject json) {
      this.type = json.getJsonArray("type").getList();
      this.protocol = json.getString("protocol");
      this.accessURL = json.getString("accessURL");
      this.port = json.getInteger("port");
      validateResourceAccessModalityFields();
    }

    private void validateResourceAccessModalityFields() {
      if (type == null || type.isEmpty()) {
        throw new IllegalArgumentException(
            "[object has missing required properties ([\"type\"])])");
      }
      if (protocol == null) {
        throw new IllegalArgumentException(
            "[object has missing required properties ([\"protocol\"])])");
      }
      if (accessURL == null) {
        throw new IllegalArgumentException(
            "[object has missing required properties ([\"accessURL\"])])");
      }
    }

    public JsonObject toJson() {
      JsonObject json = new JsonObject();
      json.put("type", new JsonArray(type)); // Convert List<String> to JsonArray
      json.put("protocol", protocol);
      json.put("accessURL", accessURL);
      json.put("port", port);
      return json;
    }
  }
}
