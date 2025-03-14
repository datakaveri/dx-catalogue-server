package iudx.catalogue.server.authenticator.handler.authentication;

import static iudx.catalogue.server.apiserver.util.Constants.HEADER_BEARER_AUTHORIZATION;
import static iudx.catalogue.server.apiserver.util.Constants.HEADER_TOKEN;
import static iudx.catalogue.server.common.ResponseUrn.INTERNAL_SERVER_ERROR;
import static iudx.catalogue.server.common.ResponseUrn.INVALID_TOKEN_URN;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.authenticator.service.AuthenticationService;
import iudx.catalogue.server.common.HttpStatusCode;
import iudx.catalogue.server.common.RoutingContextHelper;
import iudx.catalogue.server.exceptions.DxRuntimeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** IUDX Authentication handler to authenticate token passed in HEADER */
public class AuthHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(AuthHandler.class);

  private final AuthenticationService authService;

  public AuthHandler(AuthenticationService authService) {
    this.authService = authService;
  }

  @Override
  public void handle(RoutingContext context) throws DxRuntimeException {
    // Checks if the request contains a Header Token or Header Bearer
    if (!context.request().headers().contains(HEADER_TOKEN)
        && !context.request().headers().contains(HEADER_BEARER_AUTHORIZATION)) {
      LOGGER.warn("Fail: Unauthorized CRUD operation");
      context.response().setStatusCode(401).end();
      return;
    }
    String token = RoutingContextHelper.getToken(context);
    Future<JwtData> jwtDataFuture = authService.decodeToken(token);
    jwtDataFuture
        .onSuccess(
            jwtData -> {
              RoutingContextHelper.setJwtData(context, jwtData);
              context.next();
            })
        .onFailure(
            cause -> {
              LOGGER.error("Fail: " + cause.getMessage());
              processAuthFailure(context, cause.getMessage());
            });
  }

  private void processAuthFailure(RoutingContext context, String failureMessage)
      throws DxRuntimeException {
    LOGGER.error("Error : Authentication Failure : {}", failureMessage);
    if (failureMessage.equalsIgnoreCase("User information is invalid")) {
      LOGGER.error("User information is invalid");
      context.fail(
          new DxRuntimeException(
              HttpStatusCode.INTERNAL_SERVER_ERROR.getValue(), INTERNAL_SERVER_ERROR));
      return;
    }
    context.fail(
        new DxRuntimeException(
            HttpStatusCode.INVALID_TOKEN_URN.getValue(), INVALID_TOKEN_URN, failureMessage));
  }
}
