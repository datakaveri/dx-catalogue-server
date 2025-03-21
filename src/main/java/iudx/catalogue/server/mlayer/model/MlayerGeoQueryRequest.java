package iudx.catalogue.server.mlayer.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.exceptions.InvalidSchemaException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@DataObject
@JsonGen
public class MlayerGeoQueryRequest {
  private static final String INSTANCE_REGEX = "^[a-zA-Z ]*$";
  private static final String UUID_REGEX =
      "^[a-zA-Z0-9]{8}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{12}$";
  private List<UUID> id;
  private String instance;

  public MlayerGeoQueryRequest(JsonObject json) throws InvalidSchemaException {
    super();
    JsonArray idArray = json.getJsonArray("id", new JsonArray());
    this.id = idArray.stream()
        .map(Object::toString)
        .map(value -> {
          if (!value.matches(UUID_REGEX)) {  // Check regex first
            throw new InvalidSchemaException(String.format(
                "[ECMA 262 regex \"%s\" does not match input string \"%s\"]", UUID_REGEX, value));
          }
          try {
            return UUID.fromString(value);  // Convert only if it passes regex
          } catch (IllegalArgumentException e) {
            throw new InvalidSchemaException(String.format(
                "[Invalid UUID format] \"%s\"", value));
          }
        })
        .collect(Collectors.toList());
    this.instance = json.getString("instance");

    validateFields();
  }

  private void validateFields() {
    if (id == null || id.isEmpty()) {
      throw new InvalidSchemaException("[object has missing required properties ([\"id\"])]");
    }

    for (UUID id : id) {
      if (!id.toString().matches(UUID_REGEX)) {
        throw new InvalidSchemaException(
            String.format(
                "[ECMA 262 regex \"%s\" does not match input string \"%s\"]", UUID_REGEX, id));
      }
    }
    if (instance == null) {
      throw new InvalidSchemaException(
          "[object has missing required properties ([\"instance\"])]");
    }
    if (!instance.matches(INSTANCE_REGEX)) {
      throw new InvalidSchemaException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]",
              INSTANCE_REGEX, instance));
    }
  }

  public List<UUID> getId() {
    return id;
  }

  public void setId(List<UUID> ids) {
    this.id = ids;
  }

  public String getInstance() {
    return instance;
  }

  public void setInstance(String instance) {
    this.instance = instance;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put("id", new JsonArray(id.stream().map(UUID::toString).collect(Collectors.toList())));
    json.put("instance", instance);
    return json;
  }
}
