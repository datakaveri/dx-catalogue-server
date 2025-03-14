package iudx.catalogue.server.authenticator;

public class Constants {

  public static final String AUTH_SERVER_HOST = "authServerHost";
  public static final String UAC_DEPLOYMENT = "isUACInstance";
  public static final String UAC_ADMIN = "uacAdmin";
  public static final String KEYCLOACK_HOST = "keycloakServerHost";
  public static final String CERTS_ENDPOINT = "certsEndpoint";
  public static final String DUMMY_TOKEN_KEY = "authDummyToken";
  public static final String DUMMY_PROVIDER_PREFIX =
          "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc";
  public static final String AUTH_TIP_PATH = "/token/introspect";

  public static final String TOKEN = "token";
  public static final String OPERATION = "operation";
  public static final String REQUEST = "request";
  public static final String BODY = "body";
  public static final String RESOURCE_SERVER_URL = "resourceServerRegURL";
  public static final String RATINGS = "ratings";

  public static final String AUTH_SERVER_ERROR = "Error calling the Auth Server";

  public static final int TOKEN_SIZE = 512;
  public static final String TOKEN_REGEX = "^[a-zA-Z0-9\\/\\@\\.\\-]*$";

  /* JWT specific */
  public static final String JSON_USERID = "userid";
  public static final String AUTH_CERTIFICATE_PATH = "/cert";
  public static final String API_ENDPOINT = "apiEndpoint";
  public static final String METHOD = "method";
  public static final String RATINGS_ENDPOINT = "/consumer/ratings";

  public static final String MLAYER_BASE_PATH = "/internal/ui";
  public static final String MLAYER_INSTANCE_ENDPOINT = MLAYER_BASE_PATH + "/instance";
  public static final String MLAYER_DOMAIN_ENDPOINT = MLAYER_BASE_PATH + "/domain";
  public static final String MLAYER_PROVIDERS_ENDPOINT = MLAYER_BASE_PATH + "/providers";

}
