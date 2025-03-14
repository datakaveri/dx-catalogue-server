package iudx.catalogue.server.apiserver.integrationtests.searchAPIsIT.geoSpatialSearchIT;

import io.restassured.response.Response;
import iudx.catalogue.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

/**
 * Integration tests for the search APIs through attributes in the Catalog Server.
 */
@ExtendWith(RestAssuredConfiguration.class)
public class AttributeSearchIT {
    private static final Logger LOGGER = LogManager.getLogger(AttributeSearchIT.class);
    @Test
    @DisplayName("testing Attribute Search - 200 Success - Simple Attribute")
    void GetSimpleAttribute() {
        Response response = given()
                .param("property","[id]")
                .param("value","[[b58da193-23d9-43eb-b98a-a103d4b6103c]]")
                .when()
                .get("/search")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
    }
    @Test
    @DisplayName("testing Attribute Search - 200 Success - Simple Attribute")
    void GetSimpleAttributeForDxType() {
        Response response = given()
            .param("property","[type]")
            .param("value","[[iudx:Resource, iudx:ResourceGroup]]")
            .when()
            .get("/search")
            .then()
            .statusCode(200)
            .body("type", is("urn:dx:cat:Success"))
            .extract()
            .response();
    }
    @Test
    @DisplayName("testing Attribute Search - 200 Success - Simple Attribute Multi value")
    void GetSimpleAttributeMulVal() {
        Response response = given()
                .param("property","[id]")
                .param("value","[[b58da193-23d9-43eb-b98a-a103d4b6103c,5b7556b5-0779-4c47-9cf2-3f209779aa22]]")
                .when()
                .get("/search")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
    }
    @Test
    @DisplayName("testing Attribute Search - 200 Success - Multi Attribute Multi value")
    void GetMultiAttributeMulVal() {
        Response response = given()
                .param("property","[tags,name]")
                .param("value","[[flooding, current level],[FWR055]]")
                .when()
                .get("/search")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
    }
    @Test
    @DisplayName("testing Nested Attribute Search - 200 Success")
    void NestedAttributeSearch() {
        Response response = given()
                .param("property","[tags,location.address]")
                .param("value","[[aqm, flood], [pune,delhi]]")
                .when()
                .get("/search")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
    }
    @Test
    @DisplayName("testing Attribute Search with non-existing id - 400 Invalid value")
    void AttributeSearchWithInvalidID() {
        given()
                .param("property","[id]")
                .param("value","[[inva!d-id]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidPropertyValue"));
    }
    @Test
    @DisplayName("testing Attribute Search - Simple Attribute Multi value - 400 Invalid value")
    void GetSimpleAttributeMulValInvalid() {
        given()
                .param("property","[id]")
                .param("value","[[af19727d-0ab8-4002-b745-ec183cb0ce69,inva!d-id]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidPropertyValue"));
    }
    @Test
    @DisplayName("testing Attribute Search  - 400 Invalid value")
    void GetSimpleAttributeInvalidProp1() {
        given()
                .param("property","[!id]")
                .param("value","[[af19727d-0ab8-4002-b745-ec183cb0ce60]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing Attribute Search - Simple Attribute Exceeded Property Query Param - 400 Invalid request")
    void GetSimpleAttributeExceedPropQueryParam() {
        given()
                .param("property","[id,name,tags,location,deviceId]")
                .param("value","[[af19727d-0ab8-4002-b745-ec183cb0ce60],[sensor1],[aqm],[pune],[1234]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
    @Test
    @DisplayName("testing Attribute Search - Simple Attribute Exceeded Value pair Query Param - 400 Invalid request")
    void GetSimpleAttributeExceedValPairQueryParam() {
        given()
                .param("property","[id,name,tags,location]")
                .param("value","[[af19727d-0ab8-4002-b745-ec183cb0ce60],[sensor1],[aqm],[pune],[1234]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
    @Test
    @DisplayName("testing Attribute Search - Simple Attribute Exceeded Value Query Param - 400 Invalid request")
    void GetSimpleAttributeExceedValQueryParam() {
        given()
                .param("property","[id,name,tags,location]")
                .param("value","[[af19727d-0ab8-4002-b745-ec183cb0ce60],[sensor1,sensor2,sensor3,sensor4,sensor5],[aqm],[pune]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
    @Test
    @DisplayName("testing Attribute Search, Simple Attribute Response filter - 400 Invalid request")
    void GetSimpleAttributeResponseFilter() {
        given()
                .param("property","[id]")
                .param("value","[[af19727d-0ab8-4002-b745-ec183cb0ce60]]")
                .param("filter","[id,name,tags,deviceId,resourceServer,provider,location,address,type,itemStatus,authServerInfo]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
    @Test
    @DisplayName("testing Attribute Search, Simple Attribute Exceed limit value - 400 Invalid request")
    void GetSimpleAttributeExceedLimitVal() {
        given()
                .param("property","[id]")
                .param("value","[[af19727d-0ab8-4002-b745-ec183cb0ce60]]")
                .param("filter","[id,name,tags]")
                .param("limit",10000000)
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
    @Test
    @DisplayName("testing Attribute Search, Simple Attribute Exceed offset value - 400 Invalid request")
    void GetSimpleAttributeExceedOffsetVal() {
        given()
                .param("property","[id]")
                .param("value","[[af19727d-0ab8-4002-b745-ec183cb0ce60]]")
                .param("filter","[id,name,tags]")
                .param("limit",10)
                .param("offset",1001001)
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
}
