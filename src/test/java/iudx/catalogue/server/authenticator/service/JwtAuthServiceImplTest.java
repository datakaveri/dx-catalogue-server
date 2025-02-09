//package iudx.catalogue.server.authenticator.service;
//
//import static iudx.catalogue.server.authenticator.Constants.*;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//import io.vertx.core.Future;
//import io.vertx.core.Vertx;
//import io.vertx.core.json.JsonObject;
//import io.vertx.ext.auth.PubSecKeyOptions;
//import io.vertx.ext.auth.jwt.JWTAuth;
//import io.vertx.ext.auth.jwt.JWTAuthOptions;
//import io.vertx.ext.web.client.WebClient;
//import io.vertx.junit5.VertxExtension;
//import io.vertx.junit5.VertxTestContext;
//import iudx.catalogue.server.Configuration;
//import iudx.catalogue.server.authenticator.AuthenticationVerticle;
//import iudx.catalogue.server.authenticator.Constants;
//import iudx.catalogue.server.authenticator.JwtTokenHelper;
//import iudx.catalogue.server.authenticator.model.JwtData;
//import iudx.catalogue.server.authenticator.model.JwtAuthenticationInfo;
//import iudx.catalogue.server.util.Api;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.Arguments;
//import org.junit.jupiter.params.provider.MethodSource;
//
//import java.util.stream.Stream;
//
//@ExtendWith(VertxExtension.class)
//public class JwtAuthServiceImplTest {
//
//  private static final Logger LOGGER = LogManager.getLogger(JwtAuthServiceImplTest.class);
//  private static final String AUTH_CERTINFO_PATH = "/auth/v1/certificate-info";
//  private static JsonObject authConfig;
//  private static JwtAuthenticationServiceImpl jwtAuthenticationService;
//  private static AuthenticationService authenticationService;
//  private static Vertx vertxObj;
//  private static String dxApiBasePath;
//  private static Api api;
//  /**
//   * Initialize and start the auth service for testing.
//   *
//   * @param vertx       the vertx instance object. Injected by VertxExtension and not started in clustered mode.
//   * @param testContext the context object injected by VertxExtension
//   */
//  @BeforeAll
//  @DisplayName("Initialize Vertx and deploy Auth Verticle")
//  static void init(Vertx vertx, VertxTestContext testContext) {
//    authConfig = Configuration.getConfiguration("./configs/config-test.json",1);
//    String cert = authConfig.getString("cert");
//    authConfig.put("dxApiBasePath", "/iudx/cat/v1");
//    authConfig.put("tempCopIssuer", "cos.iudx.io");
//    authConfig.put("tempCopAudience", "cos.iudx.io");
//    JWTAuthOptions jwtAuthOptions = new JWTAuthOptions();
//    jwtAuthOptions.addPubSecKey(
//            new PubSecKeyOptions()
//                    .setAlgorithm("ES256")
//                    .setBuffer(cert));
//
//    // ignore token expiration only for test
//    jwtAuthOptions.getJWTOptions().setIgnoreExpiration(true);
//    JWTAuth jwtAuth = JWTAuth.create(vertx, jwtAuthOptions);
//    dxApiBasePath = authConfig.getString("dxApiBasePath");
//    api = Api.getInstance(dxApiBasePath);
//    jwtAuthenticationService = new JwtAuthenticationServiceImpl(jwtAuth, authConfig);
//
//    LOGGER.info("Auth tests setup complete");
//    testContext.completeNow();
//  }
//
//  @Test
//  @DisplayName("Test getting public key from auth server")
//  public void testPublicKeySetup(Vertx vertx, VertxTestContext testContext) {
//    AuthenticationVerticle.getJwtPublicKey(vertx, authConfig)
//            .onComplete(handler -> {
//              if(handler.succeeded())
//              {
//                testContext.completeNow();
//              } else {
//            LOGGER.error(handler.cause().getMessage());
//                testContext.failNow(handler.cause());
//              }
//            });
//  }
//
//  @Test
//  @DisplayName("Test if webClient has been initialized properly")
//  public void testWebClientSetup(Vertx vertx, VertxTestContext testContext) {
//    WebClient client = AuthenticationVerticle.createWebClient(vertx, authConfig, true);
//    String host = authConfig.getString(Constants.AUTH_SERVER_HOST);
//    client.post(443, host, AUTH_CERTINFO_PATH).send(httpResponseAsyncResult -> {
//      if (httpResponseAsyncResult.failed()) {
//        LOGGER.error("Cert info call failed");
//        testContext.failNow(httpResponseAsyncResult.cause());
//        return;
//      }
//      LOGGER.info("Cert info call to auth server succeeded");
//      testContext.completeNow();
//    });
//  }
//
//  private JsonObject authJson() {
//    JsonObject jsonObject = new JsonObject();
//    jsonObject
//        .put("token", JwtTokenHelper.providerToken)
//        .put("id", "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86")
//        .put("apiEndpoint", "/iudx/cat/v1/item")
//        .put("itemType", "iudx:Resource")
//        .put("resourceServerRegURL", "rs.iudx.io")
//        .put("ownerUserId", "b2c27f3f-2524-4a84-816e-91f9ab23f837")
//        .put("method", "POST");
//    return jsonObject;
//  }
//
//  private JwtData jwtDataObject() {
//    JwtData jwtData = new JwtData();
//    jwtData.setIss("authvertx.iudx.io");
//    jwtData.setAud("cat-test.iudx.io");
//    jwtData.setExp(1627408865L);
//    jwtData.setIat(1627408865L);
//    jwtData.setIid("rs:cat-test.iudx.io");
//    jwtData.setRole("provider");
//    jwtData.setSub("844e251b-574b-46e6-9247-f76f1f70a637");
//    jwtData.setCons(new JsonObject());
//
//    return jwtData;
//  }
//
//  @Test
//  @DisplayName("successful allow access to protected endpoint")
//  public void providerTokenInterospectSuccess(VertxTestContext vertxTestContext) {
//    JwtAuthenticationInfo authInfo = new JwtAuthenticationInfo(authJson());
//
//    jwtAuthenticationService
//            .isValidEndpoint(authInfo.getApiEndpoint()).onComplete( handler -> {
//              if(handler.succeeded()) {
//                LOGGER.debug("Successfully introspected the token");
//                vertxTestContext.completeNow();
//              }
//              else  {
//                vertxTestContext.failNow(handler.cause());
//              }
//            });
//  }
//
//  @Test
//  @DisplayName("fail: allow access to protected endpoint")
//  public void providerTokenIntrospectFail(VertxTestContext vertxTestContext) {
//    JsonObject authInfo = authJson();
//    authInfo.put(TOKEN, "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiI4NDRlMjUxYi01NzRiLTQ2ZTYtOTI0Ny1mNzZmMWY3MGE2NadjMDKqd4NjPjKieYSpenWzeIhvbYW21212pbyIsImF1ZCI6ImNhdGFsb2d1ZS5pdWR4LmlvIiwiZXhwIjoxNjMyMjYxMjkxLCJpYXQiOjE2MzIyMTgwOTEsImlpZCI6InJpOmlpc2MuYWMuaW4vODlhMzYyNzNkNzdkYWM0Y2YzODExNGZjYTFiYmU2NDM5MjU0N2Y4Ni9jYXRhbG9ndWUuaXVkeC5pby9jYXRhbG9ndWUvY3J1ZCIsInJvbGUiOiJwcm92aWRlciIsImNvbnMiOnt9fQ.BTNDXRQ90C9wTWGtcYzIgjZgbhoV_ELX6smaJxjbvceKFHbVaHMaxYMMyyTrQUGe3b7BpGgODu4vR6JAycfmRg");
//
//    jwtAuthenticationService
//            .tokenIntrospect(new JwtData(), new JwtAuthenticationInfo(authInfo)).onComplete(  handler -> {
//              if(handler.succeeded())
//                vertxTestContext.failNow(handler.cause());
//              else
//                vertxTestContext.completeNow();
//            });
//  }
//
//  @Test
//  @DisplayName("Decode valid provider JWT")
//  public void decodeJwtProviderSuccess(VertxTestContext vertxTestContext) {
//    jwtAuthenticationService.decodeToken(JwtTokenHelper.providerToken)
//            .onComplete(handler -> {
//              if(handler.succeeded()) {
//                assertEquals("provider", handler.result().getRole());
//                vertxTestContext.completeNow();
//              } else {
//                vertxTestContext.failNow(handler.cause());
//              }
//            });
//  }
//
//  @Test
//  @DisplayName("Decode valid delegate JWT")
//  public void decodeJwtDelegateSuccess(VertxTestContext vertxTestContext) {
//    jwtAuthenticationService.decodeToken(JwtTokenHelper.delegateToken)
//            .onComplete(handler -> {
//              if(handler.succeeded()) {
//                assertEquals("delegate", handler.result().getRole());
//                vertxTestContext.completeNow();
//              } else {
//                vertxTestContext.failNow(handler.cause());
//              }
//            });
//  }
//
//  @Test
//  @DisplayName("Decode valid admin JWT")
//  public void decodeJwtAdminSuccess(VertxTestContext vertxTestContext) {
//    jwtAuthenticationService.decodeToken(JwtTokenHelper.adminToken)
//            .onComplete(handler -> {
//              if(handler.succeeded()) {
//                assertEquals("admin", handler.result().getRole());
//                vertxTestContext.completeNow();
//              } else {
//                vertxTestContext.failNow(handler.cause());
//              }
//            });
//  }
//
//  @Test
//  @DisplayName("Decode valid cos admin JWT")
//  public void decodeJwtCosAdminSuccess(VertxTestContext testContext) {
//    jwtAuthenticationService.decodeToken(JwtTokenHelper.cosAdminToken)
//        .onComplete(handler -> {
//          if(handler.succeeded()) {
//            assertEquals("cos_admin", handler.result().getRole());
//            testContext.completeNow();
//          } else {
//            testContext.failNow(handler.cause());
//          }
//        });
//  }
//
//  @Test
//  @DisplayName("Decode invalid JWT")
//  public void decodeInvalidJwt(VertxTestContext vertxTestContext) {
//    jwtAuthenticationService.decodeToken("eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9" +
//            ".eyJzdWIiOiI4NDRlMjUxYi01NzRiLTQ2ZTYtOTI0Ny1mNzZmMWY3MGE2MzciLCJpc3MiOiJhdXRodmVydHguaXVkeC5pbyIsImF1ZCI6ImNhdGFsb2d1ZS5pdWR4LmlvIiwiZXhwIjoxNjMyMjYxMjkxLCJpYXQiOjE2MzIyMTgwOTEsImlpZCI6InJpOmlpc2MuYWMuaW4vODlhMzYyNzNkNzdkYWM0Y2YzODExNGZjYTFiYmU2NDM5MjU0N2Y4Ni9jYXRhbG9ndWUuaXVkeC5pby9jYXRhbG9ndWUvY3J1ZCIsInJvbGUiOiJwcm92aWRlciIsImNvbnMiOnt9fQ.BTNDXRQ90C9wTWGtcYzIgjZgbhoV_ELX6smaJxjbvceKFHbVaHMaxYMMyyTrQUGe3b7BpGgODu4vR6JA123456")
//            .onComplete(handler -> {
//              if(handler.succeeded()) {
//                vertxTestContext.failNow(handler.cause());
//              } else {
//                vertxTestContext.completeNow();
//              }
//            });
//  }
//
//  @Test
//  @DisplayName("successful valid endpoint check")
//  public void validEndpointCheck(VertxTestContext vertxTestContext) {
//    JsonObject authInfo = authJson();
//
//    jwtAuthenticationService.isValidEndpoint(authInfo.getString("apiEndpoint")).onComplete(handler -> {
//      if (handler.failed()) {
//        vertxTestContext.failNow("fail");
//      } else {
//        vertxTestContext.completeNow();
//
//      }
//    });
//  }
//
//  @Test
//  @DisplayName("invalid endpoint check")
//  public void invalidEndpointCheck(VertxTestContext vertxTestContext) {
//    JsonObject authInfo = authJson();
//    authInfo.put("apiEndpoint", "/iudx/rs/v1/item");
//
//    jwtAuthenticationService.isValidEndpoint(authInfo.getString("apiEndpoint")).onComplete(handler -> {
//      if (handler.failed()) {
//        vertxTestContext.completeNow();
//      } else {
//        vertxTestContext.failNow("fail");
//
//      }
//    });
//  }
//
//  @Test
//  @DisplayName("successful valid id check")
//  public void validIdCheckForJwtToken(VertxTestContext vertxTestContext) {
//    JwtData jwtData = jwtDataObject();
//    String id = "844e251b-574b-46e6-9247-f76f1f70a637";
//    jwtAuthenticationService.isValidProvider(jwtData, id).onComplete(handler -> {
//      if (handler.failed()) {
//        vertxTestContext.failNow("fail");
//      } else {
//        vertxTestContext.completeNow();
//
//      }
//    }
//    ) ;
//  }
//
//  @Test
//  @DisplayName("invalid id check")
//  public void invalidIdCheckForJwtToken(VertxTestContext vertxTestContext) {
//    JwtData jwtData = jwtDataObject();
//    String id = "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547fab";
//
//    jwtAuthenticationService.isValidProvider(jwtData, id).onComplete(handler -> {
//      if (handler.failed()) {
//        vertxTestContext.completeNow();
//      } else {
//        vertxTestContext.failNow("fail");
//
//      }
//    });
//  }
//
//  @Test
//  @DisplayName("successful valid audience check")
//  public void validAudienceCheck(VertxTestContext vertxTestContext) {
//    JwtData jwtData = jwtDataObject();
//    jwtAuthenticationService.isValidAudienceValue(jwtData, "iudx:Resource", "cat-test.iudx.io").onComplete(handler -> {
//          if (handler.failed()) {
//            vertxTestContext.failNow("fail");
//          } else {
//            vertxTestContext.completeNow();
//
//          }
//      });
//  }
//
//  @Test
//  @DisplayName("invalid audience check")
//  public void invalidAudienceCheck(VertxTestContext vertxTestContext) {
//    JwtData jwtData = jwtDataObject();
//    jwtData.setAud("invalid.audience");
//
//    jwtAuthenticationService.isValidAudienceValue(jwtData, "iudx:Resource", "rs.iudx.io").onComplete(handler -> {
//          if (handler.failed()) {
//            vertxTestContext.completeNow();
//          } else {
//            vertxTestContext.failNow("fail");
//
//          }
//      });
//  }
//
//  private static Stream<Arguments> itemTypes1() {
//    return Stream.of(
//        Arguments.of("iudx:Resource"),
//        Arguments.of("iudx:ResourceGroup"),
//        Arguments.of("iudx:Provider")
//    );
//  }
//
//  @ParameterizedTest
//  @MethodSource("itemTypes1")
//  @DisplayName("successful valid iid check - against resource server")
//  public void validIidCheckAgainstRs(String itemType, VertxTestContext testContext) {
//    JwtData jwtData = jwtDataObject();
//
//    jwtAuthenticationService.isValidItemId(jwtData, itemType, "cat-test.iudx.io")
//        .onComplete(handler -> {
//          if(handler.succeeded()) {
//            testContext.completeNow();
//          } else {
//            testContext.failNow("unxepected behaviour");
//          }
//        });
//  }
//
//  private static Stream<Arguments> itemTypes2() {
//    return Stream.of(
//        Arguments.of("iudx:ResourceServer"),
//        Arguments.of("iudx:COS")
//    );
//  }
//
//  @ParameterizedTest
//  @MethodSource("itemTypes2")
//  @DisplayName("successful valid iid check - against cos")
//  public void validIidCheckAgainstCos(String itemType, VertxTestContext testContext) {
//    JwtData jwtData = jwtDataObject();
//    jwtData.setIid("cos:cos.iudx.io");
//
//    jwtAuthenticationService.isValidItemId(jwtData, itemType, "")
//        .onComplete(handler -> {
//          if(handler.succeeded()) {
//            testContext.completeNow();
//          } else {
//            testContext.failNow("unxepected behaviour");
//          }
//        });
//  }
//
//  @Test
//  @DisplayName("invalid idd check")
//  public void invalidIidCheck(VertxTestContext testContext) {
//    JwtData jwtData = jwtDataObject();
//    jwtData.setIid("abc:xyz.def.io");
//
//    jwtAuthenticationService.isValidItemId(jwtData, "iudx:Resource", "")
//        .onComplete(handler -> {
//          if(handler.succeeded()) {
//            testContext.failNow("unxepected behaviour");
//          } else {
//            testContext.completeNow();
//          }
//        });
//  }
//
//  @Test
//  @DisplayName("successful valid issuer check")
//  public void validIssuerCheck(VertxTestContext testContext) {
//    JwtData jwtData = jwtDataObject();
//
//    jwtAuthenticationService.isValidIssuer(jwtData, "authvertx.iudx.io")
//        .onComplete(handler -> {
//          if(handler.succeeded()) {
//            testContext.completeNow();
//          } else {
//            testContext.failNow("unxepected behaviour");
//          }
//        });
//  }
//
//  @Test
//  @DisplayName("invalid issuer check")
//  public void invalidIssuerCheck(VertxTestContext testContext) {
//    JwtData jwtData = jwtDataObject();
//
//    jwtAuthenticationService.isValidIssuer(jwtData, "abc.xyz.io")
//        .onComplete(handler -> {
//          if(handler.succeeded()) {
//            testContext.failNow("unxepected behaviour");
//          } else {
//            testContext.completeNow();
//          }
//        });
//  }
//
//  @Test
//  @DisplayName("successful valid admin check")
//  public void validAdminCheck(VertxTestContext testContext) {
//    JwtData jwtData = jwtDataObject();
//    jwtData.setRole("cos_admin");
//
//    jwtAuthenticationService.isValidAdmin(jwtData)
//        .onComplete(handler -> {
//          if(handler.succeeded()) {
//            testContext.completeNow();
//          } else {
//            testContext.failNow("unxepected behaviour");
//          }
//        });
//  }
//
//  @Test
//  @DisplayName("invalid admin check")
//  public void invalidAdminCheck(VertxTestContext testContext) {
//    JwtData jwtData = jwtDataObject();
//    jwtData.setRole("not_admin");
//
//    jwtAuthenticationService.isValidAdmin(jwtData)
//        .onComplete(handler -> {
//          if(handler.succeeded()) {
//            testContext.failNow("unxepected behaviour");
//          } else {
//            testContext.completeNow();
//          }
//        });
//  }
//
//  @Test
//  @DisplayName("fail validate access test")
//  public void invalidValidateAccessTest(VertxTestContext vertxTestContext) {
//    JwtData jwtData = jwtDataObject();
//    JsonObject authInfo = authJson();
//    authInfo.put("apiEndpoint","/iudx/cat/v1/itemzzz");
//
//    jwtAuthenticationService.tokenIntrospect(jwtData,new JwtAuthenticationInfo(authInfo))
//            .onComplete(handler -> {
//              if(handler.succeeded()){
//                vertxTestContext.failNow(handler.cause());
//              } else {
//                vertxTestContext.completeNow();
//              }
//            });
//  }
//
//
//  @Test
//  @DisplayName("successful allow admin access to protected endpoint")
//  public void adminTokenInterospectSuccess(VertxTestContext vertxTestContext) {
//
//    JsonObject authInfo = new JsonObject();
//
//    authInfo
//        .put("token", JwtTokenHelper.cosAdminToken)
//        .put("id", "catalogue.iudx.io")
//        .put("apiEndpoint", "/iudx/cat/v1/instance")
//        .put("itemType", "iudx:Instance")
//        .put("method", "POST");
//    Future<JwtData> jwtData = jwtAuthenticationService.decodeToken(authInfo.getString("token"));
//
//    jwtAuthenticationService
//            .isValidAdmin(jwtData.result()).onComplete( handler -> {
//              if(handler.succeeded()) {
//                LOGGER.debug("Successfully introspected the token");
//                vertxTestContext.completeNow();
//              }
//              else  {
//                vertxTestContext.failNow(handler.cause());
//              }
//            });
//  }
//
//}
