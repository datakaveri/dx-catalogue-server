package iudx.catalogue.server.mlayer.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.exceptions.InvalidSchemaException;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@DataObject
public class MlayerDatasetRequest {
  private static final Logger LOGGER = LogManager.getLogger(MlayerDatasetRequest.class);
  private static final String UUID_REGEX = "^(|\\w{8}-\\w{4}-\\w{4}-\\w{4}-\\w{12})$";
  private static final String INSTANCE_REGEX = "^[a-zA-Z0-9_ ]*$";
  private static final String TAGS_REGEX = "^[a-zA-Z ]*$";
  private static final String PROVIDERS_REGEX =
      "^[a-zA-Z0-9]{8}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{12}$";
  private String id;
  private String instance;
  private List<String> tags;
  private List<String> providers;
  private List<String> domains;
  private int limit;
  private int offset;

  public MlayerDatasetRequest(JsonObject json)
      throws InvalidSchemaException, IllegalArgumentException {
    if (json == null) {
      throw new InvalidSchemaException("Input JSON object is null.");
    }
    this.id = json.getString("id", null);
    this.instance = json.getString("instance", null);
    this.tags = json.getJsonArray("tags", new JsonArray()).getList();
    this.providers = json.getJsonArray("providers", new JsonArray()).getList();
    this.domains = json.getJsonArray("domains", new JsonArray()).getList();
    this.limit = json.getInteger("limit");
    this.offset = json.getInteger("offset");

    validateFields();
  }

  private void validateFields() {

    // Validate instance
    if (id != null && !id.matches(UUID_REGEX)) {
      LOGGER.debug("id; " + id.matches(UUID_REGEX));
      throw new InvalidSchemaException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]", UUID_REGEX, id));
    }
    if (instance != null && !instance.matches(INSTANCE_REGEX)) {
      throw new InvalidSchemaException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]",
              INSTANCE_REGEX, instance));
    }

    // Validate tags
    if (tags != null) {
      for (String tag : tags) {
        if (!tag.matches(TAGS_REGEX)) {
          throw new InvalidSchemaException(
              String.format(
                  "[ECMA 262 regex \"%s\" does not match input string \"%s\"]", TAGS_REGEX, tag));
        }
      }
    }

    // Validate UUIDs in 'providers' field
    if (providers != null) {
      for (String provider : providers) {
        if (!provider.matches(PROVIDERS_REGEX)) {
          throw new InvalidSchemaException(
              String.format(
                  "[ECMA 262 regex \"%s\" does not match input string \"%s\"]",
                  PROVIDERS_REGEX, provider));
        }
      }
    }

    // Validate domains
    if (domains != null) {
      for (String domain : domains) {
        if (!domain.matches(TAGS_REGEX)) {
          throw new InvalidSchemaException(
              String.format(
                  "[ECMA 262 regex \"%s\" does not match input string \"%s\"]",
                  TAGS_REGEX, domain));
        }
      }
    }
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getInstance() {
    return instance;
  }

  public void setInstance(String instance) {
    this.instance = instance;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public List<String> getProviders() {
    return providers;
  }

  public void setProviders(List<String> providers) {
    this.providers = providers;
  }

  public List<String> getDomains() {
    return domains;
  }

  public void setDomains(List<String> domains) {
    this.domains = domains;
  }

  public int getLimit() {
    return limit;
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }

  public int getOffset() {
    return offset;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    if (id != null) {
      json.put("id", id);
    }
    if (instance != null) {
      json.put("instance", instance);
    }
    if (tags != null) {
      json.put("tags", new JsonArray(tags));
    }
    if (providers != null) {
      json.put("providers", new JsonArray(providers));
    }
    if (domains != null) {
      json.put("domains", new JsonArray(domains));
    }
    json.put("limit", limit);
    json.put("offset", offset);
    return json;
  }
}
