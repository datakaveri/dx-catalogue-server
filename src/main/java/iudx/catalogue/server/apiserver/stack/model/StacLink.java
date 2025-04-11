package iudx.catalogue.server.apiserver.stack.model;

import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.exceptions.InvalidSchemaException;

public class StacLink {
  private static final String UUID_REGEX =
      "^[a-zA-Z0-9]{8}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{12}$";
  private static final String REL_REGEX = "^.*$";

  private String id;
  private String rel;
  private String href;
  private String type;
  private String title;

  public StacLink(JsonObject json) throws InvalidSchemaException {
    this.id = json.getString("id");
    this.rel = json.getString("rel");
    this.href = json.getString("href");
    this.type = json.getString("type");
    this.title = json.getString("title");
    validateFields();
  }

  private void validateFields() {
    if (id == null) {
      throw new InvalidSchemaException("[object has missing required properties ([\"id\"])])",
          true);
    }
    if (rel == null || rel.isEmpty()) {
      throw new InvalidSchemaException("[object has missing required properties ([\"rel\"])])",
          true);
    }
    if (href == null || href.isEmpty()) {
      throw new InvalidSchemaException("[object has missing required properties ([\"href\"])])",
          true);
    }
    if (!id.matches(UUID_REGEX)) {
      throw new InvalidSchemaException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]", UUID_REGEX, id), true);
    }
    if (!rel.matches(REL_REGEX)) {
      throw new InvalidSchemaException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]", REL_REGEX, rel), true);
    }
    if (!href.matches(REL_REGEX)) {
      throw new InvalidSchemaException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]", REL_REGEX, href), true);
    }
    if (!type.matches(REL_REGEX)) {
      throw new InvalidSchemaException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]", REL_REGEX, type), true);
    }
    if (!title.matches(REL_REGEX)) {
      throw new InvalidSchemaException(
          String.format(
              "[ECMA 262 regex \"%s\" does not match input string \"%s\"]", REL_REGEX, title),
          true);
    }
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getRel() {
    return rel;
  }

  public void setRel(String rel) {
    this.rel = rel;
  }

  public String getHref() {
    return href;
  }

  public void setHref(String href) {
    this.href = href;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject()
        .put("id", id)
        .put("rel", rel)
        .put("href", href);
    if (type != null) {
      json.put("type", type);
    }
    if (title != null){
      json.put("title", title);
    }
    return json;
  }
}
