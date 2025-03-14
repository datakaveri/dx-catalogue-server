package iudx.catalogue.server.apiserver.item.model;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.UUID;

public class Instance implements Item {
  private final List<String> type;
  private String instance;
  private String id;

  public Instance(JsonObject json) {
    this.id = json.getString("id");
    this.type = json.getJsonArray("type").getList();
    this.instance = json.getString("instance");
  }

  public String getInstance() {
    return instance;
  }

  public void setInstance(String instance) {
    this.instance = instance;
  }

  @Override
  public String getContext() {
    return null;
  }

  @Override
  public void setContext(String context) {}

  @Override
  public UUID getId() {
    return null;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override
  public void setId(UUID id) {}

  @Override
  public List<String> getType() {
    return null;
  }

  @Override
  public void setType(List<String> type) {}

  @Override
  public String getName() {
    return null;
  }

  @Override
  public void setName(String name) {}

  @Override
  public String getDescription() {
    return null;
  }

  @Override
  public void setDescription(String description) {}

  @Override
  public String getItemStatus() {
    return null;
  }

  @Override
  public void setItemStatus(String itemStatus) {}

  @Override
  public String getItemCreatedAt() {
    return null;
  }

  @Override
  public void setItemCreatedAt(String itemCreatedAt) {}

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();

    json.put("id", id);
    json.put("type", new JsonArray(type));
    json.put("instance", instance);

    return json;
  }
}
