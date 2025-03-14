package iudx.catalogue.server.authenticator.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import iudx.catalogue.server.authenticator.model.JwtAuthenticationInfo;
import iudx.catalogue.server.authenticator.model.JwtData;

/**
 * The Authentication Service.
 *
 * <h1>Authentication Service</h1>
 *
 * <p>The Authentication Service in the IUDX Catalogue Server defines the operations to be performed
 * with the IUDX Authentication and Authorization server.
 *
 * @version 1.0
 * @see io.vertx.codegen.annotations.ProxyGen
 * @see io.vertx.codegen.annotations.VertxGen
 * @since 2020-05-31
 */
@VertxGen
@ProxyGen
public interface AuthenticationService {

  /**
   * The createProxy helps the code generation blocks to generate proxy code.
   *
   * @param vertx which is the vertx instance
   * @param address which is the proxy address
   * @return AuthenticationServiceVertxEBProxy which is a service proxy
   */
  @GenIgnore
  static AuthenticationService createProxy(Vertx vertx, String address) {
    return new AuthenticationServiceVertxEBProxy(vertx, address);
  }

  /**
   * Decodes the provided JWT token and extracts the data embedded in it.
   *
   * <p>This method performs the decoding of a JWT (JSON Web Token) to extract the user information
   * and claims embedded in the token, such as user roles, issuer, subject, etc.
   *
   * @param token {@link String} token to be decoded
   * @return a {@link Future} of {@link JwtData} which will contain the decoded token data if the
   *     decoding is successful
   */
  Future<JwtData> decodeToken(String token);

  /**
   * The tokenIntrospect method implements the authentication and authorization module using IUDX
   * APIs.
   *
   * @param jwtData which is a JwtData
   * @param authenticationInfo which is a JwtAuthenticationInfo
   * @return Future which is a vert.x Future of type JwtData
   */
  Future<JwtData> tokenIntrospect(JwtData jwtData, JwtAuthenticationInfo authenticationInfo);
}
