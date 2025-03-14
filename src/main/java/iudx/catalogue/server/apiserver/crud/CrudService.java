package iudx.catalogue.server.apiserver.crud;

import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.apiserver.item.model.*;
import iudx.catalogue.server.apiserver.item.service.ItemService;
import java.util.NoSuchElementException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CrudService {
  private final ItemService itemService;
  private final Logger LOGGER = LogManager.getLogger(CrudService.class);

  public CrudService(ItemService itemService) {
    this.itemService = itemService;
  }

  public Future<JsonObject> createItem(JsonObject requestBody) {
    Promise<JsonObject> promise = Promise.promise();
    String itemType = requestBody.getJsonArray(TYPE).getString(0);
    LOGGER.debug("Item type: " + itemType);
    Item item = createItemFromType(itemType, requestBody);
    LOGGER.debug("Info: Inserting item");

    itemService
        .createItem(item)
        .onComplete(
            dbHandler -> {
              if (dbHandler.failed()) {
                LOGGER.error("Fail: Item creation failed; " + dbHandler.cause().getMessage());
                promise.fail(dbHandler.cause());
              } else {
                JsonObject response = dbHandler.result();
                LOGGER.info("Success: Item created;");
                promise.complete(response);
              }
            });

    return promise.future();
  }

  public Future<JsonObject> updateItem(JsonObject requestBody) {
    Promise<JsonObject> promise = Promise.promise();

    String itemType = requestBody.getJsonArray(TYPE).getString(0);
    LOGGER.debug("Item type: " + itemType);
    Item item = createItemFromType(itemType, requestBody);

    LOGGER.debug("Info: Updating item");

    itemService
        .updateItem(item)
        .onComplete(
            dbHandler -> {
              if (dbHandler.failed()) {
                LOGGER.error("Fail: Item update failed; " + dbHandler.cause().getMessage());
                promise.fail(dbHandler.cause());
              } else {
                JsonObject response = dbHandler.result();
                LOGGER.info("Success: Item updated;");
                promise.complete(response);
              }
            });

    return promise.future();
  }

  public Future<JsonObject> getItem(JsonObject requestBody) {
    Promise<JsonObject> promise = Promise.promise();

    itemService
        .getItem(requestBody)
        .onComplete(
            res -> {
              if (res.failed()) {
                promise.fail(res.cause());
              } else {
                JsonObject response = res.result();
                LOGGER.info("Success: Item retrieved;");
                promise.complete(response);
              }
            });

    return promise.future();
  }

  public Future<JsonObject> deleteItem(JsonObject requestBody) {
    Promise<JsonObject> promise = Promise.promise();

    String id = requestBody.getString(ID);
    itemService
        .deleteItem(id)
        .onComplete(
            dbHandler -> {
              if (dbHandler.succeeded()) {
                JsonObject response = dbHandler.result();
                LOGGER.info("Success: Item deleted;");
                if (TITLE_SUCCESS.equals(response.getString(TITLE))) {
                  promise.complete(response);
                } else {
                  promise.fail(
                      new NoSuchElementException(
                          "Fail: Doc doesn't exist, can't perform operation"));
                }
              } else {
                Throwable cause = dbHandler.cause();
                if (cause.getMessage().contains(TYPE_ITEM_NOT_FOUND)) {
                  LOGGER.error("Fail: Item not found; " + cause.getMessage());
                  promise.fail(cause.getMessage());
                } else {
                  LOGGER.error("Fail: Item deletion failed; " + cause.getMessage());
                  promise.fail(cause.getMessage());
                }
              }
            });

    return promise.future();
  }

  private Item createItemFromType(String itemType, JsonObject requestBody) {
    switch (itemType) {
      case ITEM_TYPE_OWNER:
        return new Owner(requestBody);
      case ITEM_TYPE_COS:
        return new COS(requestBody);
      case ITEM_TYPE_RESOURCE_SERVER:
        return new ResourceServer(requestBody);
      case ITEM_TYPE_PROVIDER:
        return new Provider(requestBody);
      case ITEM_TYPE_RESOURCE_GROUP:
        return new ResourceGroup(requestBody);
      case ITEM_TYPE_RESOURCE:
        return new Resource(requestBody);
      case ITEM_TYPE_INSTANCE:
        return new Instance(requestBody);
      default:
        throw new IllegalArgumentException("Invalid item type: " + itemType);
    }
  }
}
