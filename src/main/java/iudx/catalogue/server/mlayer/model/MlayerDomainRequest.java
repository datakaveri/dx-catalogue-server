package iudx.catalogue.server.mlayer.model;

import com.google.common.hash.Hashing;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.exceptions.InvalidSchemaException;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@DataObject
public class MlayerDomainRequest {
  private static final String UUID_REGEX = "^(|\\w{8}-\\w{4}-\\w{4}-\\w{4}-\\w{12})$";
  private static final String IMAGE_URL_REGEX =
      "(http(s?):)([/|.|\\w|\\s|-])*\\.(?:jpg|gif|png|jpeg|JPG|GIF|PNG|JPEG)";
  private static final String NAME_REGEX = "^[a-zA-Z ]*$";
  private final JsonObject requestJson;
  private String id;
  private UUID domainId;

  @NotNull(message = "[object has missing required properties ([\"description\"])]]")
  @NotEmpty(message = "Description cannot be empty")
  private String description;

  @NotNull(message = "[object has missing required properties ([\"icon\"])]]")
  @NotEmpty(message = "Icon URL cannot be empty")
  @Pattern(
      regexp = IMAGE_URL_REGEX,
      message = "Icon must be a valid image URL with extensions jpg, jpeg, png, or gif")
  private String icon;

  @NotNull(message = "[object has missing required properties ([\"label\"])]]")
  @NotEmpty(message = "Label cannot be empty")
  private String label;

  @NotNull(message = "[object has missing required properties ([\"name\"])]]")
  @NotEmpty(message = "Name cannot be empty")
  @Pattern(regexp = "^[a-zA-Z ]*$", message = "Name must contain only letters and spaces")
  private String name;

  public MlayerDomainRequest(JsonObject json) throws InvalidSchemaException {
    this.requestJson = json.copy();
    this.id = json.getString("id");
    if (!json.getString("domainId").matches(UUID_REGEX)) { // Check regex first
      throw new InvalidSchemaException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]",
              UUID_REGEX, json.getString("domainId")));
    }
    this.domainId = UUID.fromString(json.getString("domainId", UUID.randomUUID().toString()));
    this.description = json.getString("description");
    this.icon = json.getString("icon");
    this.label = json.getString("label");
    this.name = json.getString("name");

    if (this.id == null || this.id.isEmpty()) {
      this.id =
          Hashing.sha256().hashString(this.name.toLowerCase(), StandardCharsets.UTF_8).toString();
      this.requestJson.put("id", this.id);
    }
    validateFields();
  }

  private void validateFields() {
    if (description == null) {
      throw new InvalidSchemaException(
          "[object has missing required properties ([\"description\"])]]");
    }
    if (icon == null) {
      throw new InvalidSchemaException("[object has missing required properties ([\"label\"])]]");
    }
    if (label == null) {
      throw new InvalidSchemaException("[object has missing required properties ([\"label\"])]]");
    }
    if (name == null) {
      throw new InvalidSchemaException("[object has missing required properties ([\"name\"])]]");
    }
    if (!icon.matches(IMAGE_URL_REGEX)) {
      throw new InvalidSchemaException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]", IMAGE_URL_REGEX, icon));
    }
    if (!name.matches(NAME_REGEX)) {
      throw new InvalidSchemaException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]", NAME_REGEX, name));
    }
  }

  public UUID getDomainId() {
    return domainId;
  }

  public void setDomainId(UUID domainId) {
    this.domainId = domainId;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public JsonObject getRequestJson() {
    return requestJson;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put("id", id);
    json.put("description", description);
    json.put("icon", icon);
    json.put("label", label);
    json.put("name", name);
    json.put("domainId", domainId.toString());
    return json;
  }
}
