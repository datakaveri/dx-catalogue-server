package iudx.catalogue.server.auditing.handler;

import static iudx.catalogue.server.apiserver.util.Constants.USERID;
import static iudx.catalogue.server.auditing.util.Constants.API;
import static iudx.catalogue.server.auditing.util.Constants.EPOCH_TIME;
import static iudx.catalogue.server.auditing.util.Constants.IID;
import static iudx.catalogue.server.auditing.util.Constants.IUDX_ID;
import static iudx.catalogue.server.auditing.util.Constants.USER_ID;
import static iudx.catalogue.server.auditing.util.Constants.USER_ROLE;
import static iudx.catalogue.server.util.Constants.HTTP_METHOD;
import static iudx.catalogue.server.util.Constants.ID;
import static iudx.catalogue.server.util.Constants.REQUEST_DELETE;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.auditing.service.AuditingService;
import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.common.RoutingContextHelper;
import java.time.ZonedDateTime;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AuditHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(AuditHandler.class);
  private final AuditingService auditingService;

  public AuditHandler(AuditingService auditingService) {
    this.auditingService = auditingService;
  }

  /**
   * @param event the event to handle
   */
  @Override
  public void handle(RoutingContext event) {
    event.next();
  }

  public void handle(RoutingContext routingContext, String api) {
    JwtData jwtDecodedInfo = RoutingContextHelper.getJwtData(routingContext);
    String id;
    String httpMethod = routingContext.request().method().toString();

    if (httpMethod.equals(REQUEST_DELETE)) {
      id = routingContext.queryParams().get(ID);
    } else {
      id = RoutingContextHelper.getValidatedRequest(routingContext).getString(ID);
    }
    JsonObject auditInfo = new JsonObject();
    // adding user id, user role and iid to response for auditing purpose
    auditInfo
        .put(USER_ROLE, jwtDecodedInfo.getRole())
        .put(USER_ID, jwtDecodedInfo.getSub())
        .put(IID, jwtDecodedInfo.getIid())
        .put(IUDX_ID, id)
        .put(API, api)
        .put(HTTP_METHOD, httpMethod);
    updateAuditTable(auditInfo);
  }

  /**
   * function to handle call to audit service.
   *
   * @param auditInfo contains the user-role, user-id, iid item-id, api-endpoint and the HTTP
   *     method.
   */
  public void updateAuditTable(JsonObject auditInfo) {
    LOGGER.info("Updating audit table on successful transaction");

    ZonedDateTime zst = ZonedDateTime.now();
    LOGGER.debug("TIME ZST: " + zst);
    long epochTime = getEpochTime(zst);
    auditInfo.put(EPOCH_TIME, epochTime).put(USERID, auditInfo.getString(USER_ID));

    LOGGER.debug("audit data: " + auditInfo.encodePrettily());
    auditingService
        .insertAuditingValuesInRmq(auditInfo)
        .onSuccess(result -> LOGGER.info("Message published in RMQ."))
        .onFailure(err -> LOGGER.error("Failed to publish message in RMQ.", err));
  }

  private long getEpochTime(ZonedDateTime zst) {
    return zst.toInstant().toEpochMilli();
  }
}
