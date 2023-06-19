package iudx.catalogue.server.authenticator;

import static iudx.catalogue.server.authenticator.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

import com.nimbusds.jose.*;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.*;
import com.nimbusds.jose.proc.*;
import com.nimbusds.jwt.*;
import com.nimbusds.jwt.proc.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.catalogue.server.util.Api;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Authentication Verticle.
 *
 * <h1>Authentication Verticle</h1>
 *
 * <p>The Authentication Verticle implementation in the the IUDX Catalogue Server exposes the {@link
 * iudx.catalogue.server.authenticator.AuthenticationService} over the Vert.x Event Bus.
 *
 * @version 1.0
 * @since 2020-05-31
 */
public class AuthenticationVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(AuthenticationVerticle.class);
  private static WebClient webClient;
  private AuthenticationService jwtAuthenticationService, kcAuthenticationService;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;
  private Api api;
  private String dxApiBasePath;
  private boolean isUAC;

  static WebClient createWebClient(Vertx vertx, JsonObject config) {
    return createWebClient(vertx, config, false);
  }

  static WebClient createWebClient(Vertx vertx, JsonObject config, boolean testing) {
    WebClientOptions webClientOptions = new WebClientOptions();
    if (testing) {
      webClientOptions.setTrustAll(true).setVerifyHost(false);
    }
    webClientOptions.setSsl(true);
    return WebClient.create(vertx, webClientOptions);
  }

  static Future<String> getJwtPublicKey(Vertx vertx, JsonObject config) {
    Promise<String> promise = Promise.promise();
    webClient = createWebClient(vertx, config);
    if (config.containsKey(PUBLIC_KEY)) {
      promise.complete(config.getString(PUBLIC_KEY));
    } else {
      String authCert = config.getString("dxAuthBasePath") + AUTH_CERTIFICATE_PATH;

      webClient
          .get(443, config.getString("authServerHost"), authCert)
          .send(
              handler -> {
                if (handler.succeeded()) {
                  JsonObject json = handler.result().bodyAsJsonObject();
                  promise.complete(json.getString("cert"));
                } else {
                  promise.fail("fail to get JWT public key");
                }
              });
    }
    return promise.future();
  }

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event bus against an address, publishes the service with the service discovery
   * interface.
   *
   * @throws Exception which is a startup exception
   */
  @Override
  public void start() throws Exception {
    isUAC = config().getBoolean(UAC_DEPLOYMENT);
    binder = new ServiceBinder(vertx);
    binder.setAddress(AUTH_SERVICE_ADDRESS);
    if (isUAC) {
      String keyCloakHost = config().getString(KEYCLOACK_HOST);
      String certsEndpoint = config().getString(CERTS_ENDPOINT);
      String audience = config().getString("host");

      URL jwksURL = new URL(keyCloakHost.concat(certsEndpoint));
      JWKSet publicKeys = JWKSet.load(jwksURL);
      JWKSource<SecurityContext> keySource = new ImmutableJWKSet<>(publicKeys);
      JWSAlgorithm expectedJWSAlgo = JWSAlgorithm.RS256;
      JWSKeySelector<SecurityContext> keySelector =
          new JWSVerificationKeySelector<>(expectedJWSAlgo, keySource);
      ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
      jwtProcessor.setJWSTypeVerifier(
          new DefaultJOSEObjectTypeVerifier<>(new JOSEObjectType("jwt")));
      jwtProcessor.setJWSKeySelector(keySelector);
      JWTClaimsSetVerifier<SecurityContext> claimsSetVerifier =
          new DefaultJWTClaimsVerifier<>(
              new HashSet<>(Arrays.asList(audience)),
              new JWTClaimsSet.Builder().issuer(keyCloakHost).build(),
              new HashSet<>(Arrays.asList("exp", "sub", "iat", "iss", "aud", "clientId")),
              Collections.singleton("nonce"));

      jwtProcessor.setJWTClaimsSetVerifier(claimsSetVerifier);

      dxApiBasePath = config().getString("dxApiBasePath");
      api = Api.getInstance(dxApiBasePath);
      kcAuthenticationService = new KCAuthenticationServiceImpl(jwtProcessor, config(), api);

      consumer = binder.register(AuthenticationService.class, kcAuthenticationService);
      LOGGER.debug("AuthVerticle Deployed");
    } else {
      getJwtPublicKey(vertx, config())
          .onSuccess(
              handler -> {
                String cert = handler;

                JWTAuthOptions jwtAuthOptions = new JWTAuthOptions();
                jwtAuthOptions.addPubSecKey(
                    new PubSecKeyOptions().setAlgorithm("ES256").setBuffer(cert));
                /* Default jwtIgnoreExpiry is false. If set through config, then that value is taken */
                boolean jwtIgnoreExpiry =
                    config().getBoolean("jwtIgnoreExpiry") != null
                        && config().getBoolean("jwtIgnoreExpiry");
                if (jwtIgnoreExpiry) {
                  jwtAuthOptions.getJWTOptions().setIgnoreExpiration(true);
                  LOGGER.warn(
                      "JWT ignore expiration set to true, do not "
                          + "set IgnoreExpiration in production!!");
                }
                JWTAuth jwtAuth = JWTAuth.create(vertx, jwtAuthOptions);

                dxApiBasePath = config().getString("dxApiBasePath");
                api = Api.getInstance(dxApiBasePath);
                jwtAuthenticationService =
                    new JwtAuthenticationServiceImpl(vertx, jwtAuth, config(), api);

                /* Publish the Authentication service with the Event Bus against an address. */
                consumer = binder.register(AuthenticationService.class, jwtAuthenticationService);

                LOGGER.info("Authentication verticle deployed");
              })
          .onFailure(
              handler -> {
                LOGGER.error("failed to get JWT public key from auth server");
                LOGGER.error("Authentication verticle deployment failed.");
              });
    }
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
