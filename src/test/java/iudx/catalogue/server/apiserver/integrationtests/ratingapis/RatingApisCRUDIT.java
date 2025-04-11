package iudx.catalogue.server.apiserver.integrationtests.ratingapis;
import io.restassured.RestAssured;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static io.restassured.RestAssured.given;
import static iudx.catalogue.server.authenticator.TokensForITs.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/* Rest Assured Integration tests for the CRUD operations of consumer ratings in Catalogue server APIs. The tests cover
creating, updating, retrieving and deleting ratings in catalogue server APIs*/

@ExtendWith(RestAssuredConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RatingApisCRUDIT {
    String itemId ="83c2e5c2-3574-4e11-9530-2b1fbdfce832";
    String approvedItemId ="b58da193-23d9-43eb-b98a-a103d4b6103e";
    static {
        RestAssured.basePath = "";
    }
    // Create RatingRequest
    @Test
    @Order(1)
    @DisplayName("Create RatingRequest Success Response-201")
    public void createRatingSuccessTest() {
        // Request Body
        JsonObject requestBody = new JsonObject()
                .put("rating", 4.8)
                .put("comment", "good resource");

        given()
                .queryParam("id", itemId)
                .queryParam("type","group")
                .header("Content-Type", "application/json")
                .header("token", consumerToken)
                .body(requestBody.encodePrettily())
                .when()
                .post("/consumer/ratings")
                .then()
                .statusCode(201)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:Success"))
                .body("results[0].id", notNullValue());
    }
    @Test
    @Order(2)
    @DisplayName("Create rating with invalid token test- 401")
    public void createRatingWithInvalidTokenTest() {
        // Request Body
        JsonObject requestBody = new JsonObject()
                .put("rating", 4.8)
                .put("comment", "v.good resource");

        given()
                .queryParam("id", itemId)
                .header("Content-Type", "application/json")
                .header("token", "abc")
                .body(requestBody.encodePrettily())
                .when()
                .post("/consumer/ratings")
                .then()
                .statusCode(401);// Expecting a 401 Unauthorized response status
                //.log().body();

    }
    @Test
    @Order(3)
    @DisplayName("Create rating with invalid schema test- 400")
    public void createRatingWithInvalidSchemaTest(){
        //Request Body
        JsonObject requestBody = new JsonObject()
                .put("ratingzzz", 4.8);
        given()
                .queryParam("id", itemId)
                .header("Content-Type","application/json")
                .header("token",consumerToken)
                .body(requestBody.encodePrettily())
                .when()
                .post("/consumer/ratings")
                .then()
                .statusCode(400)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:InvalidSchema"));
    }
    @Test
    @Order(4)
    @DisplayName("Create rating with invalid access count test-400")
    public void createRatingWithInvalidAccessCount(){
        String itemIdWIAC = "db9deaf0-2e6f-4d8c-9cc0-5f738cd75b46";
        //request body
        JsonObject requestBody = new JsonObject()
                .put("rating", 4.8)
                .put("comment", "good resource");
        given()
                .queryParam("id", itemIdWIAC)
                .header("Content-Type","application/json")
                .header("token",consumerToken)
                .body(requestBody.encodePrettily())
                .when()
                .post("/consumer/ratings")
                .then()
                .statusCode(400);
                //.log().body();
    }

    //Update RatingRequest

    @Test
    @Order(5)
    @DisplayName("Update rating success response test- 200")
    public void updateRatingSuccessTest(){
        //request body
        JsonObject requestBody = new JsonObject()
                .put("rating", 4.8)
                .put("comment", "very good resource");
        given()
                .queryParam("id",itemId)
                .header("Content-Type","application/json")
                .header("token",consumerToken)
                .body(requestBody.encodePrettily())
                .when()
                .put("/consumer/ratings")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:Success"));

    }
    @Test
    @Order(6)
    @DisplayName("Update rating with Invalid Token Test- 401")
    public void updateRatingWithInvalidTokenTest(){
        //request body
        JsonObject requestBody = new JsonObject()
                .put("rating", 4.8)
                .put("comment", "very good resource");
        given()
                .queryParam("id",itemId)
                .header("Content-Type","application/json")
                .header("token","abc")
                .body(requestBody.encodePrettily())
                .when()
                .put("/consumer/ratings")
                .then()
                .statusCode(401)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:InvalidAuthorizationToken"));

    }
    @Test
    @Order(7)
    @DisplayName("Update rating with Invalid Schema Test- 400")
    public void updateRatingWithInvalidSchemaTest(){
        //request body
        JsonObject requestBody = new JsonObject()
                .put("ratingzzz", 4.8)
                .put("comment", "v.good resource");

        given()
                .queryParam("id",itemId)
                .header("Content-Type","application/json")
                .header("token",consumerToken)
                .body(requestBody.encodePrettily())
                .when()
                .put("/consumer/ratings")
                .then()
                .statusCode(400)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:InvalidSchema"));

    }

    //Get RatingRequest

    @Test
    @Order(8)
    @DisplayName("Get RatingRequest Success Test-200")
    public void getRatingSuccessTest(){
        given()
                .queryParam("id", itemId)
                .header("token",consumerToken)
                .when()
                .get("/consumer/ratings")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:Success"));
    }
    @Test
    @Order(9)
    @DisplayName("Get RatingRequest With No Content Test-204")
    public void getRatingWithNoContentTest(){
        given()
                .queryParam("id","8054c01a-14dd-4214-9d4f-a45dde44e121")
                .header("token",consumerToken)
                .when()
                .get("/consumer/ratings")
                .then()
                .statusCode(204);
                //.log().body();
    }
    @Test
    @Order(10)
    @DisplayName("Get All Ratings Success Test-200")
    public void getAllRatingsSuccessTest(){
        given()
                .queryParam("id",approvedItemId)
                .queryParam("type","group")
                .when()
                .get("/consumer/ratings")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:Success"));
    }
    @Test
    @Order(11)
    @DisplayName("Get Average RatingRequest Success Test-200")
    public void getAverageRatingSuccessTest(){
        given()
                .queryParam("id","5b7556b5-0779-4c47-9cf2-3f209779aa22")
                .queryParam("type","average")
                .when()
                .get("/consumer/ratings")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:Success"));
    }
    @Test
    @Order(12)
    @DisplayName("Get Average RatingRequest With Invalid ID Test-400")
    public void getAverageRatingWithInvalidIdTest(){
        given()
                .queryParam("id","5b7556b5-0779-4c47-9cf2-3f209779aa22\\")
                .queryParam("type","average")
                .when()
                .get("/consumer/ratings")
                .then()
                .statusCode(400)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:InvalidParamValue"));

    }
    @Test
    @Order(13)
    @DisplayName("Get RatingRequest With Invalid Token Test-401")
    public void getRatingWithInvalidTokenTest(){
        given()
                .queryParam("id",approvedItemId)
                .header("token","abc")
                .when()
                .get("/consumer/ratings")
                .then()
                .statusCode(401)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:InvalidAuthorizationToken"));
    }

    //Delete rating
    @Test
    @Order(14)
    @DisplayName("Delete RatingRequest Success Response Test-200")
    public void deleteRatingSuccessTest() {
        given()
                .queryParam("id",itemId)
                .header("Content-Type", "application/json")
                .header("token", consumerToken)
                .when()
                .delete("/consumer/ratings")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("Delete RatingRequest With Invalid Token Test-401")
    public void deleteRatingWithInvalidTokenTest() {
        given()
                .queryParam("id",itemId)
                .header("Content-Type", "application/json")
                .header("token", "abc")
                .when()
                .delete("/consumer/ratings")
                .then()
                .statusCode(401)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:InvalidAuthorizationToken"));
    }
    @AfterEach
    public void tearDown() {
        // Introduce a delay
        try {
            Thread.sleep(1000); // 1 second delay
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
