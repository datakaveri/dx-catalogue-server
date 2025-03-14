package iudx.catalogue.server.apiserver.item.model;

import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.UUID;

/**
 * Abstract class representing a catalog item in the system.
 * Provides getter, setter, and JSON conversion methods for common item attributes.
 */
public interface Item {
  String getContext();

  void setContext(String context);


  UUID getId();

  void setId(UUID id);

  List<String> getType();

  void setType(List<String> type);

  String getName();

  void setName(String name);

  String getDescription();

  void setDescription(String description);

  String getItemStatus();

  void setItemStatus(String itemStatus);

  String getItemCreatedAt();

  void setItemCreatedAt(String itemCreatedAt);

  JsonObject toJson();
}
