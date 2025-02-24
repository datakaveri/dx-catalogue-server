package iudx.catalogue.server.common;

import static iudx.catalogue.server.apiserver.util.Constants.HEADER_BEARER_AUTHORIZATION;
import static iudx.catalogue.server.apiserver.util.Constants.HEADER_TOKEN;
import static iudx.catalogue.server.authenticator.Constants.API_ENDPOINT;
import static iudx.catalogue.server.util.Constants.ID;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.authenticator.model.JwtAuthenticationInfo;
import iudx.catalogue.server.authenticator.model.JwtData;

/**
 * Utility class for managing context data in a {@link RoutingContext}.
 *
 * <p>This class provides methods to set and retrieve various types of information within the
 * context, including JWT authentication info, decoded JWT details, and validated request data.
 */
public class RoutingContextHelper {

  public static final String JWT_DATA = "jwtData";
  public static final String JWT_AUTH_INFO_KEY = "jwtAuthenticationInfo";
  public static final String VALIDATED_REQ_KEY = "validatedRequest";

  public static String getRequestPath(RoutingContext routingContext) {
    return routingContext.request().path();
  }

  public static String getMethod(RoutingContext routingContext) {
    return routingContext.request().method().toString();
  }

  public static void setId(RoutingContext event, String id) {
    event.put(ID, id);
  }

  public static String getId(RoutingContext event) {
    return event.get(ID);
  }

  /**
   * Stores the JWT authentication information in the context.
   *
   * @param context the routing context in which to store the data
   * @param jwtAuthenticationInfo the JWT authentication information to store
   */
  public static void setJwtAuthInfo(
      RoutingContext context, JwtAuthenticationInfo jwtAuthenticationInfo) {
    context.put(JWT_AUTH_INFO_KEY, jwtAuthenticationInfo);
  }

  /**
   * Retrieves the JWT authentication information from the context.
   *
   * @param context the routing context from which to retrieve the data
   * @return the JWT authentication information, or {@code null} if not present
   */
  public static JwtAuthenticationInfo getJwtAuthInfo(RoutingContext context) {
    return context.get(JWT_AUTH_INFO_KEY);
  }

  /**
   * Stores the decoded JWT information in the context.
   *
   * @param context the routing context in which to store the data
   * @param jwtDecodedInfo the decoded JWT information to store
   */
  public static void setJwtData(RoutingContext context, JwtData jwtDecodedInfo) {
    context.put(JWT_DATA, jwtDecodedInfo);
  }

  /**
   * Retrieves the decoded JWT information from the context.
   *
   * @param context the routing context from which to retrieve the data
   * @return the decoded JWT information, or {@code null} if not present
   */
  public static JwtData getJwtData(RoutingContext context) {
    return context.get(JWT_DATA);
  }

  /**
   * Stores the validated request data in the context.
   *
   * @param context the routing context in which to store the data
   * @param validatedRequest the validated request data to store
   */
  public static void setValidatedRequest(RoutingContext context, JsonObject validatedRequest) {
    context.put(VALIDATED_REQ_KEY, validatedRequest);
  }

  /**
   * Retrieves the validated request data from the context.
   *
   * @param context the routing context from which to retrieve the data
   * @return the validated request data, or {@code null} if not present
   */
  public static JsonObject getValidatedRequest(RoutingContext context) {
    return context.get(VALIDATED_REQ_KEY);
  }

  public static String getItemType(RoutingContext context) {
    return context.get(ITEM_TYPE);
  }

  public static void setItemType(RoutingContext context, String itemType) {
    context.put(ITEM_TYPE, itemType);
  }

  public static String getToken(RoutingContext routingContext) {
    /* token would can be of the type : Bearer <JWT-Token>, <JWT-Token> */
    /* Send Bearer <JWT-Token> if Authorization header is present */
    /* allowing both the tokens to be authenticated for now */
    /* TODO: later, 401 error is thrown if the token does not contain Bearer keyword */
    String token = routingContext.request().headers().get(HEADER_BEARER_AUTHORIZATION);
    boolean isBearerAuthHeaderPresent =
        routingContext.request().headers().contains(HEADER_BEARER_AUTHORIZATION);
    if (isBearerAuthHeaderPresent && token.trim().split(" ").length == 2) {
      String[] tokenWithoutBearer = token.split(HEADER_BEARER_AUTHORIZATION);
      token = tokenWithoutBearer[1].replaceAll("\\s", "");
      return token;
    }
    return routingContext.request().headers().get(HEADER_TOKEN);
  }

  public static void setEndPoint(RoutingContext event, String normalisedPath) {
    event.put(API_ENDPOINT, normalisedPath);
  }

  public static String getEndPoint(RoutingContext event) {
    return event.get(API_ENDPOINT);
  }
}
