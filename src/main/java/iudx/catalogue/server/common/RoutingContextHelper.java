package iudx.catalogue.server.common;

import static iudx.catalogue.server.util.Constants.ITEM_TYPE;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.authenticator.model.JwtAuthenticationInfo;
import iudx.catalogue.server.authenticator.model.JwtData;

/**
 * Utility class for managing context data in a {@link RoutingContext}.
 * <p>
 * This class provides methods to set and retrieve various types of information within the context,
 * including JWT authentication info, decoded JWT details, and validated request data.
 * </p>
 */
public class RoutingContextHelper {

  public static final String JWT_DECODED_INFO = "jwtDecodedInfo";
  public static final String JWT_AUTH_INFO_KEY = "jwtAuthenticationInfo";
  public static final String VALIDATED_REQ_KEY = "validatedRequest";

  /**
   * Stores the JWT authentication information in the context.
   *
   * @param context               the routing context in which to store the data
   * @param jwtAuthenticationInfo the JWT authentication information to store
   */
  public static void setJwtAuthInfo(RoutingContext context,
                                    JwtAuthenticationInfo jwtAuthenticationInfo) {
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
   * @param context        the routing context in which to store the data
   * @param jwtDecodedInfo the decoded JWT information to store
   */
  public static void setJwtDecodedInfo(RoutingContext context, JwtData jwtDecodedInfo) {
    context.put(JWT_DECODED_INFO, jwtDecodedInfo);
  }

  /**
   * Retrieves the decoded JWT information from the context.
   *
   * @param context the routing context from which to retrieve the data
   * @return the decoded JWT information, or {@code null} if not present
   */
  public static JwtData getJwtDecodedInfo(RoutingContext context) {
    return context.get(JWT_DECODED_INFO);
  }

  /**
   * Stores the validated request data in the context.
   *
   * @param context          the routing context in which to store the data
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
}
