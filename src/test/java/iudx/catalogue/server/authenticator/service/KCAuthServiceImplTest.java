package iudx.catalogue.server.authenticator.service;

import static iudx.catalogue.server.authenticator.Constants.*;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_PROVIDER;
import static org.mockito.Mockito.*;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.server.Configuration;
import iudx.catalogue.server.authenticator.Util;
import iudx.catalogue.server.authenticator.model.JwtAuthenticationInfo;
import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.util.Api;
import java.text.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class KCAuthServiceImplTest {
  private static final Logger LOGGER = LogManager.getLogger(KCAuthServiceImplTest.class);
  private static KcAuthenticationServiceImpl kcAuthenticationService, kcAuthenticationServiceSpy;
  private static ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
  private static Api api;
  private static String admin;
  private static AsyncResult<JsonObject> asyncResult;

  @BeforeAll
  @DisplayName("Initialize vertx and deploy auth verticle")
  static void init(Vertx vertx, VertxTestContext testContext) {

    JsonObject config = Configuration.getConfiguration("./configs/config-test.json", 1);
    admin = config.getString("admin");
    api = Api.getInstance("/iudx/cat/v1");
    jwtProcessor = mock(DefaultJWTProcessor.class);
    kcAuthenticationService = new KcAuthenticationServiceImpl(jwtProcessor, config, api);
    kcAuthenticationServiceSpy = spy(kcAuthenticationService);
    asyncResult = mock(AsyncResult.class);
    testContext.completeNow();
  }

  @Test
  @DisplayName("Success: Test token introspect")
  public void TestTokenIntrospect(Vertx vertx, VertxTestContext testContext) {
    JsonObject authInfo =
        new JsonObject()
            .put(API_ENDPOINT, api.getRouteItems())
            .put(METHOD, "POST")
            .put(TOKEN, "")
            .put(ITEM_TYPE, ITEM_TYPE_PROVIDER)
            .put(RESOURCE_SERVER_URL, "cos.iudx.io");
    JwtAuthenticationInfo jwtAuthenticationInfo = new JwtAuthenticationInfo(authInfo);

    JwtData jwtData = new JwtData();
    jwtData.setIss("cos.iudx.io");
    doAnswer(Answer -> Future.succeededFuture(jwtData))
        .when(kcAuthenticationServiceSpy)
        .decodeToken(anyString());

      kcAuthenticationServiceSpy.tokenIntrospect(
              jwtData,
              jwtAuthenticationInfo).onComplete(handler -> {
          if (handler.succeeded()) {
              LOGGER.debug("success");
              testContext.completeNow();
          } else {
              LOGGER.debug("fail");
              testContext.failNow(handler.cause());
          }
      });

  }

  @Test
  @DisplayName("Fail: Test token introspect - invalid endpoint")
  public void FailureTestTokenIntrospect(Vertx vertx, VertxTestContext testContext)
      throws ParseException {

    JsonObject authInfo = mock(JsonObject.class);
    JwtAuthenticationInfo jwtAuthenticationInfo = new JwtAuthenticationInfo(authInfo);
    JWTClaimsSet jwtClaimsSet = jwtClaimsSetBuilder();
    JwtData jwtData = new JwtData(new JsonObject(jwtClaimsSet.toString()));
    doAnswer(Answer -> Future.succeededFuture(jwtData))
        .when(kcAuthenticationServiceSpy)
        .decodeToken(anyString());

    kcAuthenticationServiceSpy.tokenIntrospect(
        jwtData,
        jwtAuthenticationInfo).onComplete(handler -> {
        if (handler.failed()) {
            testContext.completeNow();
        } else {
            testContext.failNow("Failed");
        }
    });
  }

  @Test
  @DisplayName("Fail: Test token introspect - token decode fail")
  public void FailureTestTokenIntrospect(VertxTestContext testContext) throws ParseException {

    JsonObject authInfo = mock(JsonObject.class);
    JWTClaimsSet jwtClaimsSet = jwtClaimsSetBuilder();
    JwtData jwtData = new JwtData(new JsonObject(jwtClaimsSet.toString()));
    doAnswer(Answer -> Future.failedFuture("decode failed"))
        .when(kcAuthenticationServiceSpy)
        .decodeToken(anyString());

    kcAuthenticationServiceSpy.tokenIntrospect(
        jwtData,
        new JwtAuthenticationInfo(authInfo)).onComplete(
        handler -> {
          if (handler.failed()) {
            testContext.completeNow();
          } else {
            LOGGER.error("failed");
            testContext.failNow("Failed");
          }
        });
  }

  private JWTClaimsSet jwtClaimsSetBuilder() throws ParseException {
    return JWTClaimsSet.parse(
        "{\n"
            + "\t\"exp\": 1687091138,\n"
            + "\t\"iat\": 1687089398,\n"
            + "\t\"jti\": \"f801adad-704e-40cc-b2ea-c2e42408e3bc\",\n"
            + "\t\"iss\": \"https://keycloak.demo.org/auth/realms/demo\",\n"
            + "\t\"aud\": \"account\",\n"
            + "\t\"sub\": \"dummy-admin-id\",\n"
            + "\t\"typ\": \"Bearer\",\n"
            + "\t\"client_id\": \"cos.iudx.io\"\n"
            + "}");
  }

    private JWTClaimsSet jwtClaimsSetBuilder2() throws ParseException {
        return JWTClaimsSet.parse(
                "{\n"
                        + "\t\"exp\": 1687091138,\n"
                        + "\t\"iat\": 1687089398,\n"
                        + "\t\"jti\": \"f801adad-704e-40cc-b2ea-c2e42408e3bc\",\n"
                        + "\t\"iss\": \"https://keycloak.demo.org/auth/realms/demo\",\n"
                        + "\t\"aud\": \"account\",\n"
                        + "\t\"sub\": \"dummy-admin-id\",\n"
                        + "\t\"typ\": \"Bearer\",\n"
                        + "\t\"role\": \"admin\"\n"
                        + "}");
    }

    private JWTClaimsSet jwtClaimsSetBuilder3() throws ParseException {
        return JWTClaimsSet.parse(
                "{\n"
                        + "\t\"exp\": 1687091138,\n"
                        + "\t\"iat\": 1687089398,\n"
                        + "\t\"jti\": \"f801adad-704e-40cc-b2ea-c2e42408e3bc\",\n"
                        + "\t\"iss\": \"https://keycloak.demo.org/auth/realms/demo\",\n"
                        + "\t\"aud\": \"account\",\n"
                        + "\t\"sub\": \"dummy-admin-id\",\n"
                        + "\t\"typ\": \"Bearer\",\n"
                        + "\t\"role\": \"user\"\n"
                        + "}");
    }

  @Test
  @DisplayName("Success: Test decode token")
  public void TestDecodeTokenFuture(Vertx vertx, VertxTestContext testContext)
      throws BadJOSEException, ParseException, JOSEException {

    JWTClaimsSet jwtClaimsSet = jwtClaimsSetBuilder();
    when(jwtProcessor.process(anyString(), any())).thenReturn(jwtClaimsSet);
    kcAuthenticationService
        .decodeToken("token")
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                testContext.completeNow();
              } else {
                testContext.failNow("decode token test failed");
              }
            });
  }

  @Test
  @DisplayName("Success: Test validUACAdmin Future")
  public void TestisValidUAC(Vertx vertx, VertxTestContext testContext) throws ParseException {

    JWTClaimsSet jwtClaimsSet = jwtClaimsSetBuilder();
    JwtData jwtData = new JwtData(new JsonObject(jwtClaimsSet.toString()));
    Util.isValidAdmin("cos.iudx.io", jwtData, true)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                testContext.completeNow();
              } else {
                testContext.failNow("valid UAC token failed");
              }
            });
  }

    @Test
    @DisplayName("Success: Test validUACAdmin Future")
    public void TestisValidUAC2(Vertx vertx, VertxTestContext testContext) throws ParseException {

        JWTClaimsSet jwtClaimsSet = jwtClaimsSetBuilder2();
        JwtData jwtData = new JwtData(new JsonObject(jwtClaimsSet.toString()));
        Util.isValidAdmin("cos.iudx.io", jwtData, false)
                .onComplete(
                        handler -> {
                            if (handler.succeeded()) {
                                testContext.completeNow();
                            } else {
                                testContext.failNow("valid UAC token failed");
                            }
                        });
    }

    @Test
    @DisplayName("Fail: Test validUACAdmin Future")
    public void TestisValidUACFail(Vertx vertx, VertxTestContext testContext) throws ParseException {

        JWTClaimsSet jwtClaimsSet = jwtClaimsSetBuilder3();
        JwtData jwtData = new JwtData(new JsonObject(jwtClaimsSet.toString()));
        Util.isValidAdmin("cos.iudx.io", jwtData, false)
                .onComplete(
                        handler -> {
                            if (handler.succeeded()) {
                                testContext.failNow("Invalid Token: Admin token required");
                            } else {
                                testContext.completeNow();
                            }
                        });
    }

  @Test
  @DisplayName("successful valid endpoint check")
  public void validEndpointCheck(VertxTestContext vertxTestContext) {

    kcAuthenticationService
        .isValidEndpoint(api.getRouteItems())
        .onComplete(
            handler -> {
              if (handler.failed()) {
                vertxTestContext.failNow("fail");
              } else {
                vertxTestContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("Invalid enpoint check")
  public void invalidEndpointCheck(VertxTestContext testContext) {

    kcAuthenticationService
        .isValidEndpoint(api.getRouteSearch())
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                testContext.failNow("fail");
              } else {
                testContext.completeNow();
              }
            });
  }
}
