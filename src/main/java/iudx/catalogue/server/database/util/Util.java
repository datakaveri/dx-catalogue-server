package iudx.catalogue.server.database.util;

import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.validator.util.Constants.VALIDATION_FAILURE_MSG;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Util {
  private static final Logger LOGGER = LogManager.getLogger(Util.class);

  public static String getItemType(JsonObject request) {
    Set<String> type = new HashSet<String>(new JsonArray().getList());
    try {
      type = new HashSet<String>(request.getJsonArray(TYPE).getList());
    } catch (Exception e) {
      LOGGER.error("Item type mismatch");
      return VALIDATION_FAILURE_MSG;
    }
    type.retainAll(ITEM_TYPES);
    String itemType = type.toString().replaceAll("\\[", "").replaceAll("\\]", "");
    return itemType;
  }
}
