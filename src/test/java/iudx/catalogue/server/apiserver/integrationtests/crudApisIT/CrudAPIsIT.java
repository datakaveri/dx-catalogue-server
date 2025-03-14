package iudx.catalogue.server.apiserver.integrationtests.crudApisIT;

import io.restassured.response.Response;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.*;
import static iudx.catalogue.server.authenticator.TokensForITs.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for the CRUD operations of items in the catalogue server. The tests cover
 * creating, updating, listing and deleting owner items, COS (Catalog Operating System) items, DX Resource Server items,
 * DX Provider items, DX Resource Group items, and DX Resource items.
 */
@ExtendWith(RestAssuredConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CrudAPIsIT {
    public static final Logger LOGGER = LogManager.getLogger(CrudAPIsIT.class);
    public static String owner_id;
    public static String cos_id;
    public static String resource_server_id;
    public static String provider_id;
    public static String resource_group_id;
    public static String resource_group_vthInstance_id;
    public static String resource_item_id;

    // Helper method to check item existence
    private boolean itemExists(String id) {
        // Perform a GET request to retrieve information whether item exists or not
        Response response = given()
                .param("id",id)
                .when()
                .get("/item/");

        // Check if the response status code is 200 (OK)
        if (response.statusCode() == 200) {
            LOGGER.info("item with ID " + id + " exists.");
            return true;
        } else {
            LOGGER.info("item with ID " + id + " does not exist.");
            return false;
        }
    }

    /**
     * Integration tests for the CRUD operations of items in the catalogue server. The tests cover
     * creating owner items, COS (Catalog Operating System) items, DX Resource Server items,
     * DX Provider items, DX Resource Group items, and DX Resource items.
     */

    @Test
    @Order(1)
    @DisplayName("testing create owner item - 201")
    void createOwnerItem() {
        JsonObject jsonPayload = new JsonObject();
        jsonPayload.put("@context", "https://voc.iudx.org.in/");
        JsonArray typeArray = new JsonArray().add("iudx:Owner");
        jsonPayload.put("type", typeArray);
        jsonPayload.put("name", "OwnerItemPM18");
        jsonPayload.put("description", "testing owner item creation through integration tests");

        Response response= given()
                .contentType("application/json")
                .header("token", cosAdminToken)
                .body(jsonPayload.toString())
                .when()
                .post("/item/")
                .then()
                .statusCode(201)
                .body("type", is("urn:dx:cat:Success"))
                .body("title", is("Success"))
                .body("detail", equalTo("Success: Item created"))
                .body("results.id", notNullValue())
                .extract()
                .response();
        // Extract the generated ID from the response
        owner_id = response.path("results.id");
        LOGGER.info(owner_id);
    }

    @Test
    @Order(2)
    @DisplayName("testing create COS item - 201")
    void createCOSItem() {
        LOGGER.info("owner_id: from cos item method.. "+owner_id);
        // Introduce a delay between creating COS item and DX Resource Server item
        try {
            Thread.sleep(2000); // 2 seconds delay
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Check if owner item exists before creating DX cos item
        if (!itemExists(owner_id)) {
            LOGGER.warn("Owner item does not exist. Retrying...");
            // Add retry logic or handle the situation accordingly
        }
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type",new JsonArray().add("iudx:COS"))
                .put("name", "iudxCOSPM18")
                .put("owner", owner_id)
                .put("description", "testing COS item creation through integration tests")
                .put("cosURL","cat-test.iudx.io")
                .put("cosUI","cat-test.iudx.io");

        Response response = given()
                .contentType("application/json")
                .header("token", cosAdminToken)
                .body(jsonPayload.toString())
                .when()
                .post("/item/")
                .then()
                .statusCode(201)
                .body("type", is("urn:dx:cat:Success"))
                .body("title", is("Success"))
                .body("detail", equalTo("Success: Item created"))
                .body("results.id", notNullValue())
                .extract()
                .response();
        // Extract the generated ID from the response
        cos_id = response.path("results.id");
    }

    @Test
    @Order(3)
    @DisplayName("testing the creation of DX Resource Server Item Entity - 201")
    void createDXResourceServerItemEntity() {
        LOGGER.debug("cos_id from resource server item creation..."+cos_id);
        // Introduce a delay between creating COS item and DX Resource Server item
        try {
            Thread.sleep(2000); // 2 seconds delay
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Check if COS item exists before creating DX Resource Server item
        if (!itemExists(cos_id)) {
            LOGGER.warn("COS item does not exist. Retrying...");
            // Add retry logic or handle the situation accordingly
        }

        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:ResourceServer"))
                .put("name", "IudxResourceServerPM")
                .put("description", "Multi tenanted IUDX resource server for integration tests")
                .put("tags",new JsonArray().add("IUDX").add("Resource").add("Server").add("Platform"))
                .put("cos", cos_id)
                .put("owner", owner_id)
                .put("resourceServerOrg",new JsonObject()
                .put("name", "iudx")
                .put("additionalInfoURL", "https://iudx.org.in")
                .put("location", new JsonObject()
                        .put("type", "Place")
                        .put("address", "IISc, Bangalore")
                        .put("geometry", new JsonObject()
                                .put("type", "Point")
                                .put("coordinates", new JsonArray().add(77.570423).add(13.013945))
                        )))
                .put("resourceServerRegURL", "rs-test-pm.iudx.io")
                .put("resourceAccessModalities",new JsonArray()
                        .add(new JsonObject()
                .put("type", new JsonArray().add("iudx:HTTPAccess"))
                .put("protocol", "http")
                .put("accessURL", "rs")
                .put("port", 8080)))
                .put("locations", new JsonObject()
                        .put("type", "Place")
                        .put("address", "IISc, Bangalore")
                        .put("geometry", new JsonObject()
                                .put("type", "Point")
                                .put("coordinates", new JsonArray().add(77.570423).add(13.013945))
                        ));

        Response response = given()
                .contentType("application/json")
                .header("token", cosAdminToken)
                .body(jsonPayload.toString())
                .when()
                .post("/item/")
                .then()
                .statusCode(201)
                .body("type", is("urn:dx:cat:Success"))
                .body("title", is("Success"))
                .body("detail", equalTo("Success: Item created"))
                .body("results.id", notNullValue())
                .extract()
                .response();
        // Extract the generated ID from the response
        resource_server_id = response.path("results.id");
    }

    @Test
    @Order(4)
    @DisplayName("testing create DX Provider item - 201")
    void createDXProviderItem() {
        LOGGER.debug("resource server id from provider item creation..."+resource_server_id);

        // Introduce a delay
        try {
            Thread.sleep(2000); // 2 seconds delay
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Check if resource server item exists before creating DX Resource Server item
        if (!itemExists(resource_server_id)) {
            LOGGER.warn("RS item does not exist. Retrying...");
            // Add retry logic or handle the situation accordingly
        }

        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:Provider"))
                .put("name", "Provider for integration tests")
                .put("resourceServer", resource_server_id)
                .put("description", "provider for the integration test")
                .put("ownerUserId", "cbe6c060-1767-488f-b624-1424e22004a2")
                .put("providerOrg", new JsonObject()
                        .put("name", "Datakaveri")
                        .put("additionalInfoURL", "https://datakaveri.org")
                        .put("location", new JsonObject()
                                .put("type", "Place")
                                .put("address", "IIsc")
                                .put("geometry", new JsonObject()
                                        .put("type", "Point")
                                        .put("coordinates", new JsonArray().add(75.92).add(14.5))
                                )
                        )
                );
        Response response = given()
                .contentType("application/json")
                .header("token", adminToken)
                .body(jsonPayload.toString())
                .when()
                .post("/item/")
                .then()
                .statusCode(201)
                .body("type", is("urn:dx:cat:Success"))
                .body("title", is("Success"))
                .body("detail", equalTo("Success: Item created"))
                .body("results.id", notNullValue())
                .extract()
                .response();
        // Extract the generated ID from the response
        provider_id = response.path("results.id");
    }

    @Test
    @Order(5)
    @DisplayName("testing create DX Resource Group Item Entity - 201")
    void createDXResourceGroupItemEntity() {
        LOGGER.debug("provider id before check..."+provider_id);
        // Introduce a delay
       try {
            Thread.sleep(2000); // 2 seconds delay
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Check if provider item exists before creating DX Resource Group item
       if (!itemExists(provider_id)) {
            LOGGER.warn("provider item does not exist. Retrying...");
            // Add retry logic or handle the situation accordingly
        }
        /*if (!itemExists(resource_server_id)) {
            LOGGER.warn("RS item does not exist. RS Group creation skipped.");
            // You might choose to throw an exception or handle it as needed
            return;
        }*/

        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:ResourceGroup").add("iudx:IssueReporting"))
                .put("name", "ResourceGroupPM19")
                .put("description", "resource group item for the postman collection")
                .put("tags", new JsonArray().add("swachhata").add("complaints").add("construction material").add("requests"))
                .put("provider", provider_id);
        Response response = given()
                .contentType("application/json")
                .header("token", token)
                .body(jsonPayload.toString())
                .when()
                .post("/item/")
                .then()
                .statusCode(201)
                .body("type", is("urn:dx:cat:Success"))
                .body("title", is("Success"))
                .body("detail", equalTo("Success: Item created"))
                .body("results.id", notNullValue())
                .extract()
                .response();
        // Extract the generated ID from the response
        resource_group_id = response.path("results.id");
    }

    @Test
    @Order(6)
    @DisplayName("Testing create DX Resource Group Item Entity - With Instance - 201")
    void createDXResourceGroupItemEntityVthInstance() {
        LOGGER.debug("Provider ID before check: " + provider_id);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Thread interrupted", e);
        }

        if (!itemExists(provider_id)) {
            LOGGER.warn("Provider item does not exist. Retrying...");
            return;
        }

        JsonObject jsonPayload = new JsonObject()
            .put("@context", "https://voc.iudx.org.in/")
            .put("name", "TESTRGX")
            .put("label", "TEST RG-X")
            .put("type", new JsonArray().add("iudx:ResourceGroup"))
            .put("description", "TEST RG")
            .put("tags", new JsonArray().add("Functional Areas (Administrative Boundaries)"))
            .put("location", new JsonObject()
                .put("type", "Place")
                .put("address", "India")
                .put("geometry", new JsonObject()
                    .put("type", "Polygon")
                    .put("coordinates", new JsonArray().add(
                        new JsonArray()
                            .add(new JsonArray().add(82.08984375000001).add(21.53484700204879))
                            .add(new JsonArray().add(83.84765625000001).add(21.53484700204879))
                            .add(new JsonArray().add(83.84765625000001).add(25.562265014427492))
                            .add(new JsonArray().add(82.08984375000001).add(25.562265014427492))
                            .add(new JsonArray().add(82.08984375000001).add(21.53484700204879))
                    ))));

        jsonPayload.put("provider", provider_id);
        jsonPayload.put("itemStatus", "ACTIVE");

        Response response = given()
            .contentType("application/json")
            .header("token", token)
            .body(jsonPayload.toString())
            .when()
            .post("/item/")
            .then()
            .statusCode(201)
            .body("type", is("urn:dx:cat:Success"))
            .body("title", is("Success"))
            .body("detail", equalTo("Success: Item created"))
            .body("results.id", notNullValue())
            .extract()
            .response();

        resource_group_vthInstance_id = response.path("results.id");
    }

    @Test
    @Order(7)
    @DisplayName("testing create DX Resource Item - 201")
    void createDXResourceItem() {
        LOGGER.debug("from DX RS item..."+resource_server_id +" "+provider_id+" "+resource_group_id);

        // Introduce a delay
        try {
            Thread.sleep(2000); // 2 seconds delay
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Check if RS item exists before creating DX Resource item
        if (!itemExists(resource_server_id)) {
            LOGGER.debug("Inside RS id check..");
            LOGGER.warn("item does not exist. Retrying...");
            // Add retry logic or handle the situation accordingly
        }
        if (!itemExists(provider_id)) {
            LOGGER.debug("Inside provider id check..");
            LOGGER.warn("item does not exist. Retrying...");
            // Add retry logic or handle the situation accordingly
        }
        if (!itemExists(resource_group_id)) {
            LOGGER.debug("Inside RS group id check..");
            LOGGER.warn("item does not exist. Retrying...");
            // Add retry logic or handle the situation accordingly
        }

        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:Resource").add("iudx:PointOfInterest"))
                .put("name", "ResourceItemPM19")
                .put("label", "item for test only")
                .put("description", "resource item for the postman collection")
                .put("tags", new JsonArray().add("swachhata").add("complaints").add("construction material").add("requests"))
                .put("apdURL", "rs.apd.iudx.org.in")
                .put("accessPolicy", "SECURE")
                .put("resourceServer", resource_server_id)
                .put("provider", provider_id)
                .put("resourceGroup", resource_group_id);

        Response response = given()
                .contentType("application/json")
                .header("token", token)
                .body(jsonPayload.toString())
                .when()
                .post("/item/")
                .then()
                .statusCode(201)
                .body("type", is("urn:dx:cat:Success"))
                .body("title", is("Success"))
                .body("detail", equalTo("Success: Item created"))
                .body("results.id", notNullValue())
                .extract()
                .response();
        // Extract the generated ID from the response
        resource_item_id = response.path("results.id");
    }

    @Test
    @Order(8)
    @DisplayName("testing create a DX Resource item - 400")
    void createDXResourceItem400() {
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:Resource").add("iudx:PointOfInterest"))
                .put("name", "wifi-location")
                .put("label", "Wi-Fi Locations in Pimpri-Chinchwad City")
                .put("description", "The physical coordinates of Wi-Fi system locations in Pimpri-Chinchwad city.")
                .put("tags", new JsonArray().add("Wi-Fi").add("Wi-Fi zone").add("hotspot").add("internet").add("Wi-Fi access").add("Wi-Fi location").add("wireless internet").add("internet access"))
                .put("apdURL", "rs.apd.iudx.org.in")
                .put("accessPolicy", "SECURE")
                .put("resourceServer", "83995e8c-fa80-4241-93c0-e86a66154eb6")
                .put("provider", "83995e8c-fa80-4241-93c0-e86a66154eb6")
                .put("resourceGroup", "83995e8c-fa80-4241-93c0-e86a66154eb6");

        Response response = given()
                .contentType("application/json")
                .header("token", token)
                .body(jsonPayload.toString())
                .when()
                .post("/item/")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:LinkValidationFailed"))
                .extract()
                .response();
    }

    @Test
    @Order(9)
    @DisplayName("testing create DX Entity - 400")
    void createDXEntity400() {
        LOGGER.info("owner_id: "+owner_id);
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:ResourceGroup").add("iudx:IssueReporting"))
                .put("name", "agra-swachhata-app")
                .put("description", "Civic Issues like open defecation, garbage dumping, etc., reported in Agra city via Swachhata app. Publishes all the complaints/issues lodged on the current day, on a daily basis.")
                .put("tags", new JsonArray().add("swachhata").add("complaints").add("reporting").add("issue").add("garbage dump").add("debris removal").add("open defecation").add("construction material").add("requests"))
                .put("provider", "83995e8c-fa80-4241-93c0");

        Response response = given()
                .contentType("application/json")
                .header("token", token)
                .body(jsonPayload.toString())
                .when()
                .post("/item/")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSchema"))
                .extract()
                .response();
    }

    @Test
    @Order(10)
    @DisplayName("testing create DX Entity - 400 Invalid UUID")
    void createDXEntityInvalidUUID400() {
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:Provider"))
                .put("name", "uuid-test")
                .put("description", "provider id for uuid test")
                .put("providerOrg", new JsonObject()
                        .put("name", "Datakaveri")
                        .put("additionalInfoURL", "https://datakaveri.org")
                        .put("location", new JsonObject()
                                .put("type", "Place")
                                .put("address", "IIsc")
                                .put("geometry", new JsonObject()
                                        .put("type", "Point")
                                        .put("coordinates", new JsonArray().add(75.92).add(14.5))
                                )
                        )
                        .put("_summary", "uuid-test provider id for uuid test")
                        .put("id", "2ed6f05f-2f35-3f70-a472-79763fc81")
                        .put("ownerUserId", "d8e46706-b9db-44e1-a9aa-e40839396b01")
                );
        Response response = given()
                .contentType("application/json")
                .header("token", token)
                .body(jsonPayload.toString())
                .when()
                .post("/item/")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSchema"))
                .extract()
                .response();
    }

    @Test
    @Order(11)
    @DisplayName("testing create DX Entity - 401 Invalid Credentials")
    void createDXEntityInvalidCredentials() {
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:COS"))
                .put("name", "cos.cop.iudx.org.in")
                .put("owner", owner_id)
                .put("description", "COS Smart Kalyan-Dombivli Development Corporation Limited")
                .put("cosURL", "kdmc.cop-nec.iudx.org.in")
                .put("cosUI", "https://catalogue.kdmc.cop-nec.iudx.org.in/");

        Response response = given()
                .contentType("application/json")
                .header("token", "abc")
                .body(jsonPayload.toString())
                .when()
                .post("/item/")
                .then()
                .statusCode(401)
                .body("type", is("urn:dx:cat:InvalidAuthorizationToken"))
                .extract()
                .response();
    }

    /**
     * Integration tests for the CRUD operations of items in the catalogue server. The tests cover
     * updating owner items, COS (Catalog Operating System) items, DX Resource Server items,
     * DX Provider items, DX Resource Group items, and DX Resource items.
     */

    @Test
    @Order(12)
    @DisplayName("testing update owner item - 200")
    void updateOwnerItem() {
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:Owner"))
                .put("name", "iudxOwnerPM18")
                .put("description", "testing the owner id updation through integration tests")
                .put("id", owner_id);
        Response response = given()
                .contentType("application/json")
                .header("token", cosAdminToken)
                .body(jsonPayload.toString())
                .when()
                .put("/item/")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .body("title", is("Success"))
                .extract()
                .response();
    }

    @Test
    @Order(13)
    @DisplayName("testing update COS item - 200")
    void updateCOSItem() {
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:COS"))
                .put("name", "IudxCosPM18")
                .put("owner", owner_id)
                .put("id", cos_id)
                .put("description", "COS item for postman collection")
                .put("cosURL", "cat-test.iudx.io")
                .put("cosUI", "cat-test.iudx.io");

        Response response = given()
                .contentType("application/json")
                .header("token", cosAdminToken)
                .body(jsonPayload.toString())
                .when()
                .put("/item/")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .body("title", is("Success"))
                .extract()
                .response();
    }

    @Test
    @Order(14)
    @DisplayName("testing update DX Resource Server Item - 200")
    void updateDXResourceItem(){
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:ResourceServer"))
                .put("name", "ResourceServerPM19")
                .put("id", resource_server_id)
                .put("description", "Multi tenanted IUDX resource server for postman collection")
                .put("tags", new JsonArray().add("IUDX").add("Resource").add("Server").add("Platform"))
                .put("cos", cos_id)
                .put("owner", owner_id)
                .put("resourceServerOrg", new JsonObject()
                        .put("name", "iudx")
                        .put("additionalInfoURL", "https://iudx.org.in")
                        .put("location", new JsonObject()
                                .put("type", "Place")
                                .put("address", "IISc, Bangalore")
                                .put("geometry", new JsonObject()
                                        .put("type", "Point")
                                        .put("coordinates", new JsonArray().add(77.570423).add(13.013945))
                                )
                        )
                )
                .put("resourceServerRegURL", "rs-test-pm.iudx.io")
                .put("resourceAccessModalities", new JsonArray()
                        .add(new JsonObject()
                                .put("type", new JsonArray().add("iudx:HTTPAccess"))
                                .put("protocol", "http")
                                .put("accessURL", "rs")
                                .put("port", 8080)
                        )
                )
                .put("location", new JsonObject()
                        .put("type", "Place")
                        .put("address", "IISc, Bangalore")
                        .put("geometry", new JsonObject()
                                .put("type", "Point")
                                .put("coordinates", new JsonArray().add(77.570423).add(13.013945))
                        )
                );

        Response response = given()
                .contentType("application/json")
                .header("token", cosAdminToken)
                .body(jsonPayload.toString())
                .when()
                .put("/item/")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .body("title", is("Success"))
                .extract()
                .response();
    }

    @Test
    @Order(15)
    @DisplayName("testing updation of DX Provider Item - 200")
    void updateDXProviderItem() {
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:Provider"))
                .put("name", "ProviderPM19")
                .put("id", provider_id)
                .put("resourceServer", resource_server_id)
                .put("description", "provider for the postman collection")
                .put("ownerUserId","cbe6c060-1767-488f-b624-1424e22004a2")
                .put("providerOrg", new JsonObject()
                        .put("name", "Datakaveri")
                        .put("additionalInfoURL", "https://datakaveri.org")
                        .put("location", new JsonObject()
                                .put("type", "Place")
                                .put("address", "IIsc")
                                .put("geometry", new JsonObject()
                                        .put("type", "Point")
                                        .put("coordinates", new JsonArray().add(75.92).add(14.5))
                                )
                        )
                );
        Response response = given()
                .contentType("application/json")
                .header("token", adminToken)
                .body(jsonPayload.toString())
                .when()
                .put("/item/")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
    }

    @Test
    @Order(16)
    @DisplayName("testing updation of DX Resource Group Item - 200")
    void updateDXResourceGroupItem() {
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:ResourceGroup").add("iudx:IssueReporting"))
                .put("name", "ResourceGroupPM19")
                .put("id", resource_group_id)
                .put("description", "resource group item for the postman collection")
                .put("tags", new JsonArray().add("swachhata").add("complaints").add("construction material").add("requests"))
                .put("provider", provider_id);
        Response response = given()
                .contentType("application/json")
                .header("token", token)
                .body(jsonPayload.toString())
                .when()
                .put("/item/")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
    }
    @Test
    @Order(17)
    @DisplayName("testing updation of DX Resource Item - 200")
    void updateDXResourceItem200() {
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:Resource").add("iudx:PointOfInterest"))
                .put("name", "ResourceItemPM19")
                .put("id", resource_item_id)
                .put("label", "item for test only")
                .put("description", "resource item for the postman collection")
                .put("tags", new JsonArray().add("swachhata").add("complaints").add("construction material").add("requests"))
                .put("apdURL", "rs.apd.iudx.org.in")
                .put("accessPolicy", "SECURE")
                .put("resourceServer", resource_server_id)
                .put("provider", provider_id)
                .put("resourceGroup", resource_group_id);

        Response response = given()
                .contentType("application/json")
                .header("token", token)
                .body(jsonPayload.toString())
                .when()
                .put("/item/")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
    }

    @Test
    @Order(18)
    @DisplayName("testing updation of DX Entity - 400")
    void updateDXEntity400() {
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:ResourceGroup").add("iudx:IssueReporting"))
                .put("name", "agra-swachhata-app")
                .put("description", "Civic Issues like open defecation, garbage dumping, etc., reported in Agra city via Swachhata app. Publishes all the complaints/issues lodged on the current day, on a daily basis.")
                .put("tags", new JsonArray().add("swachhata").add("complaints").add("reporting").add("issue").add("garbage dump").add("debris removal").add("open defecation").add("construction material").add("requests"))
                .put("provider", "e0760835-6066-301b-b224-");

        Response response = given()
                .contentType("application/json")
                .header("token", token)
                .body(jsonPayload.toString())
                .when()
                .put("/item/")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSchema"))
                .extract()
                .response();
    }

    @Test
    @Order(19)
    @DisplayName("testing updation of DX Entity - 400 (Invalid links)")
    void updateDXEntity400InvalidLinks() {
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:Resource").add("iudx:PointOfInterest"))
                .put("name", "wifi-location")
                .put("label", "Wi-Fi Locations in Pimpri-Chinchwad City")
                .put("description", "The physical coordinates of Wi-Fi system locations in Pimpri-Chinchwad city.")
                .put("tags", new JsonArray().add("Wi-Fi").add("Wi-Fi zone").add("hotspot").add("internet").add("Wi-Fi access").add("Wi-Fi location").add("wireless internet").add("internet access"))
                .put("apdURL", "rs.apd.iudx.org.in")
                .put("accessPolicy", "SECURE")
                .put("resourceServer", resource_server_id)
                .put("provider", provider_id)
                .put("resourceGroup", "83995e8c-fa80-4241-93c0-e86a66154eb6");

        Response response = given()
                .contentType("application/json")
                .header("token", token)
                .body(jsonPayload.toString())
                .when()
                .put("/item/")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:LinkValidationFailed"))
                .extract()
                .response();
    }

    @Test
    @Order(20)
    @DisplayName("testing update DX Entity - 401 Invalid Credentials")
    void updateDXEntityInvalidCredentials() {
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:COS"))
                .put("name", "cos.cop.iudx.org.in")
                .put("owner", owner_id)
                .put("description", "COS Smart Kalyan-Dombivli Development Corporation Limited")
                .put("cosURL", "kdmc.cop-nec.iudx.org.in")
                .put("cosUI", "https://catalogue.kdmc.cop-nec.iudx.org.in/");


        Response response = given()
                .contentType("application/json")
                .header("token", "abc")
                .body(jsonPayload.toString())
                .when()
                .put("/item/")
                .then()
                .statusCode(401)
                .body("type", is("urn:dx:cat:InvalidAuthorizationToken"))
                .extract()
                .response();
    }

    @Test
    @Order(21)
    @DisplayName("testing update DX Entity - 404 Not Found")
    void updateDXEntityNotFound() {
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:Resource").add("iudx:PointOfInterest"))
                .put("name", "wyfy-location")
                .put("label", "Wi-Fi Locations in Pimpri-Chinchwad City")
                .put("description", "The physical coordinates of Wi-Fi system locations in Pimpri-Chinchwad city.")
                .put("tags", new JsonArray().add("Wi-Fi").add("Wi-Fi zone").add("hotspot").add("internet").add("Wi-Fi access").add("Wi-Fi location").add("wireless internet").add("internet access"))
                .put("apdURL", "rs.apd.iudx.org.in")
                .put("accessPolicy", "SECURE")
                .put("resourceServer", resource_server_id)
                .put("provider", provider_id)
                .put("resourceGroup", resource_group_id);

        Response response = given()
                .contentType("application/json")
                .header("token", token)
                .body(jsonPayload.toString())
                .when()
                .put("/item/")
                .then()
                .statusCode(404)
                .body("type", is("urn:dx:cat:ItemNotFound"))
                .extract()
                .response();
    }

    /**
     * Integration tests for the CRUD operations of items in the catalogue server. The tests cover
     * listing owner items, COS (Catalog Operating System) items, DX Resource Server items,
     * DX Provider items, DX Resource Group items, and DX Resource items.
     */
    @Test
    @Order(22)
    @DisplayName("testing get DX Entity by ID- 200 Success")
    void getDXEntityByID() {
        LOGGER.info("owner_id: "+owner_id);
        Response response = given()
                .param("id",owner_id)
                .contentType("application/json")
                .when()
                .get("/item")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
    }

    @Test
    @Order(23)
    @DisplayName("testing get DX Entity by ID - 404 Not Found")
    void getDXEntityNotFound() {
        Response response = given()
                .param("id", "7c8b58a7-6e5b-4a97-a15d-8f4aeb4e987e")
                .contentType("application/json")
                .when()
                .get("/item")
                .then()
                .statusCode(404)
                .body("type", is("urn:dx:cat:ItemNotFound"))
                .extract()
                .response();
    }

    @Test
    @Order(24)
    @DisplayName("testing get DX Entity by ID - 400 Invalid UUID")
    void getDXEntityInvalidUUID() {
        Response response = given()
                .param("id", "dummy-id")
                .contentType("application/json")
                .when()
                .get("/item")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidUUID"))
                .extract()
                .response();
    }

    @Test
    @Order(25)
    @DisplayName("testing list Type (Data Model) given Resource Id - 200 type of item")
    void getDXEntityTypeRS() {
        Response response = given()
                .param("id", resource_server_id)
                .param("rel","type")
                .contentType("application/json")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .body("title",is("Success"))
                .extract()
                .response();
    }

    @Test
    @Order(26)
    @DisplayName("testing list Type (Data Model) given Resource Group Id - 200 type of item")
    void getDXEntityRSGroup() {
        Response response = given()
                .param("id", resource_group_id)
                .param("rel","type")
                .contentType("application/json")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .body("title",is("Success"))
                .extract()
                .response();
    }

    /**
     * Integration tests for the CRUD operations of items in the catalogue server. The tests cover
     * deleting owner items, COS (Catalog Operating System) items, DX Resource Server items,
     * DX Provider items, DX Resource Group items, and DX Resource items.
     */
    @Test
    @Order(27)
    @DisplayName("testing delete a Resource Item DX Entity - 200")
    void DeleteRSItemDXEntity() {
        Response response = given()
                .param("id", resource_item_id)
                .header("token", token)
                .contentType("application/json")
                .when()
                .delete("/item")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
    }
    @Test
    @Order(28)
    @DisplayName("testing delete a Resource Group DX Entity - 200")
    void DeleteRSGroupDXEntity() {
        Response response = given()
                .param("id", resource_group_id)
                .header("token", token)
                .contentType("application/json")
                .when()
                .delete("/item")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
    }
    @Test
    @Order(29)
    @DisplayName("testing delete a Resource Group DX Entity - 200")
    void DeleteRSGroupVthInstanceDXEntity() {
        Response response = given()
            .param("id", resource_group_vthInstance_id)
            .header("token", token)
            .contentType("application/json")
            .when()
            .delete("/item")
            .then()
            .statusCode(200)
            .body("type", is("urn:dx:cat:Success"))
            .extract()
            .response();
    }
    @Test
    @Order(30)
    @DisplayName("testing delete a Provider DX Entity - 200")
    void DeleteProviderDXEntity() {
        Response response = given()
                .param("id", provider_id)
                .header("token", adminToken)
                .contentType("application/json")
                .when()
                .delete("/item")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
    }
    @Test
    @Order(31)
    @DisplayName("testing delete a Resource Server DX Entity - 200")
    void DeleteResourceServerDXEntity() {
        Response response = given()
                .param("id", resource_server_id)
                .header("token", cosAdminToken)
                .contentType("application/json")
                .when()
                .delete("/item")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
    }
    @Test
    @Order(32)
    @DisplayName("testing delete a Owner DX Entity - 200")
    void DeleteOwnerDXEntity() {
        Response response = given()
                .param("id", owner_id)
                .header("token", cosAdminToken)
                .contentType("application/json")
                .when()
                .delete("/item")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
    }
    @Test
    @Order(33)
    @DisplayName("testing delete a COS DX Entity - 200")
    void DeleteCosDXEntity() {
        Response response = given()
                .param("id", cos_id)
                .header("token", cosAdminToken)
                .contentType("application/json")
                .when()
                .delete("/item")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
    }
    @Test
    @Order(34)
    @DisplayName("testing delete a DX Entity - 404 Not Found")
    void DeleteDXEntity404() {
        Response response = given()
                .param("id", "7c8b58a7-6e5b-4a97-a15d-8f4aeb4e987e")
                .header("token", token)
                .contentType("application/json")
                .when()
                .delete("/item")
                .then()
                .statusCode(404)
                .body("type", is("urn:dx:cat:ItemNotFound"))
                .extract()
                .response();
    }
    @Test
    @Order(35)
    @DisplayName("testing delete a DX Entity - 400 Invalid UUID")
    void DeleteDXEntityInvalidUUID() {
        Response response = given()
                .queryParam("id", "dummy-id")
                .header("token", token)
                .contentType("application/json")
                .when()
                .delete("/item")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"))
                .extract()
                .response();
    }
    @Test
    @Order(36)
    @DisplayName("testing delete a DX Entity - 401 Invalid credentials")
    void DeleteDXEntityInvalidCred() {
        Response response = given()
                .queryParam("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
                .header("token", "abc")
                .contentType("application/json")
                .when()
                .delete("/item")
                .then()
                .statusCode(401)
                .body("type", is("urn:dx:cat:InvalidAuthorizationToken"))
                .extract()
                .response();
    }
}
