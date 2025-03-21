package iudx.catalogue.server.authenticator.handler.authorization;

import static iudx.catalogue.server.common.ResponseUrn.INTERNAL_SERVER_ERROR;
import static iudx.catalogue.server.common.ResponseUrn.INVALID_TOKEN_URN;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.authenticator.model.JwtAuthenticationInfo;
import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.authenticator.service.AuthenticationService;
import iudx.catalogue.server.common.HttpStatusCode;
import iudx.catalogue.server.common.RoutingContextHelper;
import iudx.catalogue.server.exceptions.DxRuntimeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AuthValidationHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(AuthValidationHandler.class);
  private final AuthenticationService authService;

  public AuthValidationHandler(AuthenticationService authService) {
    this.authService = authService;
  }

  /**
   * @param event The RoutingContext object representing the event
   */
  @Override
  public void handle(RoutingContext event) {
    JwtData jwtData = RoutingContextHelper.getJwtData(event);
    //  getting token authentication info ->
    JwtAuthenticationInfo authenticationInfo = RoutingContextHelper.getJwtAuthInfo(event);
    Future<JwtData> jwtDataFuture = authService.tokenIntrospect(jwtData, authenticationInfo);
    jwtDataFuture
        .onSuccess(
            authResult -> {
              LOGGER.debug("Success: Token authentication successful");
              event.next(); // Proceed to the next handler
            })
        .onFailure(
            cause -> {
              LOGGER.error("Error: " + cause.getMessage());
              processAuthFailure(event, cause.getMessage());
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
