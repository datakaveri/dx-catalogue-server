package iudx.catalogue.server.authenticator;


import static iudx.catalogue.server.authenticator.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import java.util.Arrays;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * The Authentication Service Implementation.
 * <h1>Authentication Service Implementation</h1>
 *
 * <p>
 * The Authentication Service implementation in the IUDX Catalogue Server implements the definitions
 * of the {@link iudx.catalogue.server.authenticator.AuthenticationService}.
 * </p>
 *
 * @version 1.0
 * @since 2020-05-31
 */

public class AuthenticationServiceImpl implements AuthenticationService {

  private static final Logger LOGGER = LogManager.getLogger(AuthenticationServiceImpl.class);
  static WebClient webClient;
  private String authHost;
  private JsonObject config;

  /**
   * Constructs a new instance of AuthenticationServiceImpl.
   * @param client The web client to use for authentication requests.
   * @param authHost The authentication host to send requests to.
   * @param config The configuration properties for the authentication service.
   */
  public AuthenticationServiceImpl(WebClient client, String authHost, JsonObject config) {
    webClient = client;
    this.authHost = authHost;
    this.config = config;
  }

  static void validateAuthInfo(JsonObject authInfo) throws IllegalArgumentException {
    if (authInfo.isEmpty()) {
      throw new IllegalArgumentException("AuthInfo argument is empty/missing");
    }
    String token = authInfo.getString(TOKEN, "");
    String operation = authInfo.getString(OPERATION, "");
    if (token.isBlank() || operation.isBlank()) {
      throw new IllegalArgumentException("Token/Operation in authenticationInfo is blank/missing");
    } else if (token.length() > TOKEN_SIZE) {
      throw new IllegalArgumentException(
              "Supported max size of Token in authenticationInfo is " + TOKEN_SIZE);
    } else if (!token.matches(TOKEN_REGEX)) {
      throw new IllegalArgumentException(
              "Invalid Token pattern, supported pattern " + TOKEN_REGEX);
    }
    try {
      HttpMethod.valueOf(operation);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid operation in the authenticationInfo object");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AuthenticationService tokenInterospect(JsonObject request, JsonObject authenticationInfo,
                                                  Handler<AsyncResult<JsonObject>> handler) {
    JsonObject result = new JsonObject();
    String providerId;
    try {
      validateAuthInfo(authenticationInfo);
      providerId = request.getString(PROVIDER, "");
      if (providerId.isBlank()) {
        throw new IllegalArgumentException("Missing provider in request object");
      }
    } catch (IllegalArgumentException e) {
      result.put(STATUS, ERROR);
      result.put(MESSAGE, e.getMessage());
      handler.handle(Future.succeededFuture(result));
      return this;
    }

    JsonObject body = new JsonObject();
    body.put(TOKEN, authenticationInfo.getString(TOKEN));
    String tokenIntrospectPath = config.getString("dxAuthBasePath") + AUTH_TIP_PATH;
    webClient
            .post(443, authHost, tokenIntrospectPath)
                .expect(ResponsePredicate.JSON)
                .sendJsonObject(body, httpResponseAsyncResult -> {
                  if (httpResponseAsyncResult.failed()) {
                    result.put(STATUS, ERROR);
                    result.put(MESSAGE, AUTH_SERVER_ERROR);
                    LOGGER.error(AUTH_SERVER_ERROR + ";", httpResponseAsyncResult.cause());
                    handler.handle(Future.succeededFuture(result));
                    return;
                  }
                  HttpResponse<Buffer> response = httpResponseAsyncResult.result();
                  if (response.statusCode() != HttpStatus.SC_OK) {
                    result.put(STATUS, ERROR);
                    result.put(MESSAGE, response
                                .bodyAsJsonObject()
                                .getJsonObject(ERROR)
                                .getString(MESSAGE));
                    handler.handle(Future.succeededFuture(result));
                    return;
                  }

                  JsonArray responseRequests = response.bodyAsJsonObject().getJsonArray(REQUEST);
                  for (Object req : responseRequests) {
                    JsonObject requestEntry = (JsonObject) req;
                    String requestId = requestEntry.getString(ID, "");
                    if (isPermittedProviderId(requestId, providerId)) {
                      result.put(STATUS, SUCCESS);
                      result.put(BODY, new JsonObject());
                      handler.handle(Future.succeededFuture(result));
                      return;
                    }
                  }

                  result.put(STATUS, ERROR);
                  result.put(MESSAGE, "ID/Operations not permitted with presented token");
                  handler.handle(Future.succeededFuture(result));
                });

    return null;
  }

  private boolean isPermittedProviderId(String requestId, String providerId) {
    String tipProvider = String.join("/", Arrays.asList(requestId.split("/", 3)).subList(0, 2));
    return providerId.equals(tipProvider);
  }

  /**
   * Iterates through each method in the given array and checks if
   * it matches the given operation or if the method is "*", which represents all methods.
   * @param methods The list of methods to check.
   * @param operation The operation to check against the list of methods.
   * @return True if the operation is permitted, false otherwise.
   */
  public boolean isPermittedMethod(JsonArray methods, String operation) {
    for (Object o : methods) {
      String method = (String) o;
      if (method.equals("*") || method.equals(operation)) {
        return true;
      }
    }
    return false;
  }
}
