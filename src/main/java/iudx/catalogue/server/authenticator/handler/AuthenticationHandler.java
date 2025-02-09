package iudx.catalogue.server.authenticator.handler;

import static iudx.catalogue.server.apiserver.util.Constants.HEADER_TOKEN;
import static iudx.catalogue.server.authenticator.Constants.TOKEN;
import static iudx.catalogue.server.common.ResponseUrn.INTERNAL_SERVER_ERROR;
import static iudx.catalogue.server.common.ResponseUrn.INVALID_TOKEN_URN;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.authenticator.service.AuthenticationService;
import iudx.catalogue.server.authenticator.model.JwtAuthenticationInfo;
import iudx.catalogue.server.common.RoutingContextHelper;
import iudx.catalogue.server.common.HttpStatusCode;
import iudx.catalogue.server.exceptions.DxRuntimeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AuthenticationHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(AuthenticationHandler.class);

  private final AuthenticationService authService;
  public AuthenticationHandler(AuthenticationService authService) {
    this.authService = authService;
  }

  @Override
  public void handle(RoutingContext context) throws DxRuntimeException {
    // Checks if the request contains a token field.
    if (!context.request().headers().contains(HEADER_TOKEN)) {
      LOGGER.warn("Fail: Unauthorized CRUD operation");
      context.response().setStatusCode(401).end();
      return;
    }
    decodeToken(context)
        .compose(decodeToken -> checkIfTokenIsAuthenticated(context))
        .onFailure(cause -> {
          //Handle failure in either decoding or authorization
          LOGGER.error("User Verification Failed. " + cause.getMessage());
          processAuthFailure(context, cause.getMessage());
        });
  }

  public Future<Void> decodeToken(RoutingContext context) {
    Promise<Void> promise = Promise.promise();
    String token = RoutingContextHelper.getJwtAuthInfo(context).toJson().getString(TOKEN);

    authService.decodeToken(token)
        .onSuccess(decodedToken -> {
          RoutingContextHelper.setJwtDecodedInfo(context, decodedToken);
          promise.complete(); // Proceed to authorization on success
        })
        .onFailure(cause -> {
          LOGGER.error("Fail: " + cause.getMessage());
          promise.fail(cause.getMessage());
        });
    return promise.future();
  }

  public Future<Void> checkIfTokenIsAuthenticated(RoutingContext routingContext) {
    Promise<Void> promise = Promise.promise();
    //  getting token authentication info ->
    JwtAuthenticationInfo jwtAuthInfo = RoutingContextHelper.getJwtAuthInfo(routingContext);
    JwtData decodedToken = RoutingContextHelper.getJwtDecodedInfo(routingContext);
    authService.tokenIntrospect(decodedToken, jwtAuthInfo)
        .onSuccess(authResult -> {
          LOGGER.debug("Success: Token authentication successful");
          routingContext.next(); // Proceed to the next handler
          promise.complete();
        })
        .onFailure(cause -> {
          LOGGER.error("Error: " + cause.getMessage());
          promise.fail( cause.getMessage());
        });
    return promise.future();
  }

  private void processAuthFailure(RoutingContext context, String failureMessage) throws DxRuntimeException {
    LOGGER.error("Error : Authentication Failure : {}", failureMessage);
    if (failureMessage.equalsIgnoreCase("User information is invalid")) {
      LOGGER.error("User information is invalid");
      context.fail(new DxRuntimeException(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue(), INTERNAL_SERVER_ERROR));
      return;
    }
    context.fail(new DxRuntimeException(HttpStatusCode.INVALID_TOKEN_URN.getValue(),
        INVALID_TOKEN_URN, failureMessage));
  }
}

