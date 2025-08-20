package iudx.catalogue.server.apiserver.integrationtests.mlayerapis.mlayerdataset;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/*Rest Assured Integration tests to fetch the dataset details of a particular dataset all the datasets
or using a particular dataset_id in Catalogue Middle layer specific APIs.
Domains, Instance, Tags and provider field are also used to filter the datasets.*/

@ExtendWith(RestAssuredConfiguration.class)
public class DatasetIT {
    @Test
    @DisplayName("Get All Mlayer Datasets Success Test-200")
    public void getAllMlayerDatasetsTest(){
        given()
                .when()
                .get("/internal/ui/dataset")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type",equalTo("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("Post Mlayer DataSet Test-200")
    public void createMlayerDatasetTest(){
        JsonObject requestBody = new JsonObject().put("id", "8b95ab80-2aaf-4636-a65e-7f2563d0d371");
        given()
                .header("Content-Type", "application/json")
                .body(requestBody.encodePrettily())
                .when()
                .post("/internal/ui/dataset")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("Post Mlayer DataSet With Tags Field Test-200")
    public void createMlayerDatasetWithTagsFieldTest(){
        JsonObject requestBody = new JsonObject().put("tags", new JsonArray().add("flood").add("transport"));
        given()
                .header("Content-Type", "application/json")
                .body(requestBody.encodePrettily())
                .when()
                .post("/internal/ui/dataset")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("Post Mlayer DataSet With tags, instance, provider field Test-200")
    public void createMlayerDatasetWithTagsInstanceProviderFieldTest(){
        JsonObject requestBody = new JsonObject()
                .put("tags", new JsonArray().add("flood").add("transport"))
                .put("instance", "pune-city")
                .put("providers", new JsonArray().add("bbeacb12-5e54-339d-92e0-d8e063b551a8"))
                .put("domains", new JsonArray().add("itms"));

        given()
                .header("Content-Type", "application/json")
                .body(requestBody.encodePrettily())
                .when()
                .post("/internal/ui/dataset")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("Get All Mlayer DataSets With tags, instance, provider field Test-200")
    public void getAllMlayerDatasetWithTagsInstanceProviderFieldTest(){
        JsonObject requestBody = new JsonObject()
                .put("tags", new JsonArray())
                .put("instance", "")
                .put("providers", new JsonArray())
                .put("domains", new JsonArray());

        given()
                .header("Content-Type", "application/json")
                .body(requestBody.encodePrettily())
                .when()
                .post("/internal/ui/dataset")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("Item Not Found Test-404")
    public void itemNotFoundTest(){
        JsonObject requestBody = new JsonObject().put("id", "8b95ab80-2aaq-4636-a65e-7f2563d0d371");

        given()
                .body(requestBody.encodePrettily())
                .when()
                .post("/internal/ui/dataset")
                .then()
                .statusCode(404)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:ItemNotFound"));
    }
    @Test
    @DisplayName("Invalid Schema Test-400")
    public void invalidSchemaTest(){
        JsonObject requestBody = new JsonObject().put("id", "8b95ab80-2aaf-4636-a6e");
        given()
                .body(requestBody.encodePrettily())
                .when()
                .post("/internal/ui/dataset")
                .then()
                .statusCode(400)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:InvalidSchema"));
    }


}
