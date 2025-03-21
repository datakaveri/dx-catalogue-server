package iudx.catalogue.server.apiserver.stack.model;

import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.exceptions.InvalidSchemaException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class StacCatalog {
  private static final Pattern URL_PATTERN = Pattern.compile("^https?://.*$");
  private static final String ID_REGEX =
      "^[a-zA-Z0-9]{8}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{12}$";
  private UUID id;
  private String type;
  private final String stacVersion;
  private String description;
  private final List<Link> links;

  public StacCatalog(JsonObject json) throws InvalidSchemaException {
    this.id = UUID.fromString(json.getString("id", UUID.randomUUID().toString()));
    this.type = json.getString("type");
    this.stacVersion = json.getString("stac_version");
    this.description = json.getString("description");
    this.links = json.getJsonArray("links").stream()
        .map(obj -> new Link((JsonObject) obj))
        .toList();
    validateFields();
  }

  private void validateFields() {
    if (!id.toString().matches(ID_REGEX)) {
      throw new IllegalArgumentException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]", ID_REGEX, id));
    }
    if (type == null || type.isEmpty()) {
      throw new InvalidSchemaException("[object has missing required properties ([\"type\"])])",
          true);
    }
    if (description == null || description.isEmpty()) {
      throw new InvalidSchemaException("[object has missing required properties " +
          "([\"description\"])])", true);
    }
    if (links == null || links.isEmpty()) {
      throw new InvalidSchemaException("[object has missing required properties ([\"links\"])])",
          true);
    }
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject()
        .put("id", id.toString())
        .put("type", type)
        .put("description", description)
        .put("links", links.stream().map(Link::toJson).toList());
    if (stacVersion != null) {
      json.put("stac_version", stacVersion);
    }
    return json;
  }

  public UUID getId() {
    return id;
  }

  public String getType() {
    return type;
  }

  public String getStacVersion() {
    return stacVersion;
  }

  public String getDescription() {
    return description;
  }

  public List<Link> getLinks() {
    return links;
  }

  public static class Link {
    private static final Pattern REL_PATTERN = Pattern.compile(
        "^(self|item|root|child|parent)$");
    private final String rel;
    private final String href;
    private final String type;
    private final String title;

    public Link(JsonObject json) throws InvalidSchemaException {
      this.rel = json.getString("rel");
      this.href = json.getString("href");
      this.type = json.getString("type");
      this.title = json.getString("title");
      validateFields();
    }

    private void validateFields() {
      if (rel == null) {
        throw new InvalidSchemaException("[object has missing required properties ([\"rel\"])])",
            true);
      }
      if (href == null) {
        throw new InvalidSchemaException("[object has missing required properties ([\"href\"])])"
            , true);
      }
      if (!REL_PATTERN.matcher(rel).matches()) {
        throw new InvalidSchemaException(
            "Invalid relation type. Must be one of: self, item, root, child, parent", true);
      }
      if (!URL_PATTERN.matcher(href).matches()) {
        throw new InvalidSchemaException(
            "Href must be a valid URI starting with http:// or https://", true);
      }
    }

    public JsonObject toJson() {
      JsonObject json = new JsonObject()
          .put("rel", rel)
          .put("href", href);
      if (type != null) {
        json.put("type", type);
      }
      if (title != null) {
        json .put("title", title);
      }
      return json;
    }
  }
}
