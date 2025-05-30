package iudx.catalogue.server.auditing.util;

import static iudx.catalogue.server.auditing.util.Constants.*;

import io.vertx.core.json.JsonObject;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class QueryBuilder {

  private static final Logger LOGGER = LogManager.getLogger(QueryBuilder.class);

  /**
   * Builds a message object in JSON format to be sent to RabbitMQ,
   * based on the given request object.
   *
   * @param request The request object to build the message from.
   * @return The built message object in JSON format.
   *
   */
  public JsonObject buildMessageForRmq(JsonObject request) {
    String primaryKey = UUID.randomUUID().toString();
    request.put(ID, primaryKey).put(ORIGIN_SERVER, CAT_SERVER);

    LOGGER.debug("request " + request);
    return request;
  }
}
