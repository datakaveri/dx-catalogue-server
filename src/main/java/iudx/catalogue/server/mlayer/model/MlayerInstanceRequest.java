package iudx.catalogue.server.mlayer.model;

import com.google.common.hash.Hashing;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.exceptions.InvalidSchemaException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@DataObject
public class MlayerInstanceRequest {
  private static final String UUID_REGEX =
      "^[a-zA-Z0-9]{8}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{12}$";
  private static final String NAME_REGEX = "^[a-zA-Z_\\- ]*$";
  private static final String IMAGE_URL_REGEX =
      "(http(s?):)([/|.|\\w|\\s|-])*\\.(?:jpg|gif|png|jpeg|JPG|GIF|PNG|JPEG)";
  private final JsonObject requestJson;
  private String id;
  private UUID instanceId;
  private String name;
  private String cover;
  private String icon;
  private String logo;
  private List<Double> coordinates;

  public MlayerInstanceRequest(JsonObject json) throws InvalidSchemaException {
    this.requestJson = json.copy();
    this.id = json.getString("id");
    if (json.containsKey("instanceId") && !json.getString("instanceId").isEmpty() &&
        !json.getString("instanceId").matches(UUID_REGEX)) {  //Check regex first
      throw new InvalidSchemaException(String.format(
          "[ECMA 262 regex \"%s\" does not match input string \"%s\"]", UUID_REGEX,
          json.getString("instanceId")));
    }
    this.instanceId = UUID.fromString(
        json.getString("instanceId", UUID.randomUUID().toString()));
    this.name = json.getString("name");
    this.cover = json.getString("cover");
    this.icon = json.getString("icon");
    this.logo = json.getString("logo");
    JsonArray coordArray = json.getJsonArray("coordinates", new JsonArray());
    this.coordinates = coordArray.stream()
        .filter(Number.class::isInstance)
        .map(o -> ((Number) o).doubleValue())
        .collect(Collectors.toList());
    if (this.id == null || this.id.isEmpty()) {
      this.id = Hashing.sha256().hashString(this.name.toLowerCase(), StandardCharsets.UTF_8).toString();
      this.requestJson.put("id", this.id);
    }
    validateFields();
  }

  private void validateFields() {
    if (!instanceId.toString().matches(UUID_REGEX)) {
      throw new InvalidSchemaException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]", UUID_REGEX,
              instanceId));
    }
    if (name == null) {
      throw new InvalidSchemaException("[object has missing required properties ([\"name\"])]");
    }
    if (cover == null) {
      throw new InvalidSchemaException("[object has missing required properties ([\"cover\"])]");
    }
    if (icon == null) {
      throw new InvalidSchemaException("[object has missing required properties ([\"icon\"])]");
    }
    if (logo == null) {
      throw new InvalidSchemaException("[object has missing required properties ([\"logo\"])]");
    }
    if (!name.matches(NAME_REGEX)) {
      throw new InvalidSchemaException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]", NAME_REGEX, name));
    }
    if (!cover.matches(IMAGE_URL_REGEX)) {
      throw new InvalidSchemaException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]", IMAGE_URL_REGEX, cover));
    }
    if (!icon.matches(IMAGE_URL_REGEX)) {
      throw new InvalidSchemaException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]", IMAGE_URL_REGEX, icon));
    }
    if (!logo.matches(IMAGE_URL_REGEX)) {
      throw new InvalidSchemaException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]", IMAGE_URL_REGEX, logo));
    }
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public UUID getInstanceId() {
    return instanceId;
  }

  public void setInstanceId(UUID instanceId) {
    this.instanceId = instanceId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCover() {
    return cover;
  }

  public void setCover(String cover) {
    this.cover = cover;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public String getLogo() {
    return logo;
  }

  public void setLogo(String logo) {
    this.logo = logo;
  }

  public List<Double> getCoordinates() {
    return coordinates;
  }

  public void setCoordinates(List<Double> coordinates) {
    this.coordinates = coordinates;
  }

  public JsonObject getRequestJson() {
    return requestJson;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put("name", name);
    json.put("cover", cover);
    json.put("icon", icon);
    json.put("logo", logo);
    json.put("coordinates", new JsonArray(coordinates));
    json.put("instanceId", instanceId.toString());
    json.put("id", id);
    return json;
  }
}
