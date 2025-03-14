package iudx.catalogue.server.apiserver;

import static iudx.catalogue.server.apiserver.util.Constants.UAC_DEPLOYMENT;
import static iudx.catalogue.server.util.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import iudx.catalogue.server.Configuration;
import iudx.catalogue.server.apiserver.util.QueryMapper;
import iudx.catalogue.server.database.elastic.ElasticClient;
import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
import iudx.catalogue.server.database.elastic.service.ElasticsearchServiceImpl;
import iudx.catalogue.server.validator.service.ValidatorService;
import iudx.catalogue.server.validator.service.ValidatorServiceImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;


@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConstraintsValidationTest {

  private static ValidatorService validator;
  private static Vertx vertxObj;
  private static ElasticClient client;
  private static ElasticsearchService esService;
  private static String databaseIP;
  private static String docIndex;
  private static int databasePort;
  private static String databaseUser;
  private static String databasePassword;
  private static FileSystem fileSystem;
  private static boolean isUACinstance;
  private static String vocContext;

  @BeforeAll
  @DisplayName("Deploying Verticle")
  static void startVertx(Vertx vertx, VertxTestContext testContext) {

    JsonObject apiconfig = Configuration.getConfiguration("./configs/config-test.json", 3);
    JsonObject validationconfig = Configuration.getConfiguration("./configs/config-test.json", 2);

    vertx.deployVerticle(new ApiServerVerticle(), new DeploymentOptions().setConfig(apiconfig),
        testContext.completing());

    /* Configuration setup */
    databaseIP = validationconfig.getString(DATABASE_IP);
    databasePort = validationconfig.getInteger(DATABASE_PORT);
    databaseUser = validationconfig.getString(DATABASE_UNAME);
    databasePassword = validationconfig.getString(DATABASE_PASSWD);
    docIndex = validationconfig.getString(DOC_INDEX);
    isUACinstance = apiconfig.getBoolean(UAC_DEPLOYMENT);
    vocContext = "xyz";

    fileSystem = vertx.fileSystem();
    client = new ElasticClient(databaseIP, databasePort, docIndex, databaseUser, databasePassword);
    esService = new ElasticsearchServiceImpl(client);
    validator = new ValidatorServiceImpl(esService,docIndex,isUACinstance, vocContext);

    testContext.completed();
  }
  
  
  @Test
  @Order(1)
  @DisplayName("GeoPoint coordinate precision validation")
  public void coordinatePrecision(VertxTestContext testContext) {

    JsonObject requests = new JsonObject().put(GEOPROPERTY, LOCATION)
        .put(GEORELATION, GEOREL_WITHIN).put(MAX_DISTANCE, 5000).put(GEOMETRY, "Point")
        .put(COORDINATES, new JsonArray().add(73.927).add(18.502))
        .put(SEARCH_TYPE, SEARCH_TYPE_GEO);

    JsonObject json = QueryMapper.validateQueryParam(requests);

    assertEquals(SUCCESS, json.getString(STATUS));
    testContext.completeNow();
  }

  @Test
  @Order(2)
  @DisplayName("GeoPoint coordinate precision validation(failed)")
  public void coordinatePrecisionFailed(VertxTestContext testContext) {

    JsonObject requests = new JsonObject().put(GEOPROPERTY, LOCATION)
        .put(GEORELATION, GEOREL_WITHIN).put(MAX_DISTANCE, 5000).put(GEOMETRY, "Point")
        .put(COORDINATES, new JsonArray().add(73.9273456).add(18.502))
        .put(SEARCH_TYPE, SEARCH_TYPE_GEO);

    JsonObject json = QueryMapper.validateQueryParam(requests);

    assertEquals(FAILED, json.getString(STATUS));
    testContext.completeNow();
  }

  @Test
  @Order(3)
  @DisplayName("GeoPoint coordinate pair validation")
  public void coordinatePair(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
        .put(GEOPROPERTY, LOCATION)
        .put(GEORELATION, GEOREL_WITHIN)
        .put(MAX_DISTANCE, 5000)
        .put(GEOMETRY, POLYGON)
        .put(SEARCH_TYPE, SEARCH_TYPE_GEO)
        .put(COORDINATES,
            new JsonArray().add(new JsonArray().add(new JsonArray().add(75.9).add(14.5))
                .add(new JsonArray().add(72).add(13)).add(new JsonArray().add(73).add(20))
                .add(new JsonArray().add(75.9).add(14.5))));

    JsonObject json = QueryMapper.validateQueryParam(requests);

    assertEquals(SUCCESS, json.getString(STATUS));
    testContext.completeNow();
  }

  @Test
  @Order(4)
  @DisplayName("GeoPoint coordinate pair validation(failed)")
  public void coordinatePairFailed(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
        .put(GEOPROPERTY, LOCATION)
        .put(GEORELATION, GEOREL_WITHIN)
        .put(MAX_DISTANCE, 5000)
        .put(SEARCH_TYPE, SEARCH_TYPE_GEO)
        .put(GEOMETRY, POLYGON).put(COORDINATES,
            new JsonArray().add(new JsonArray().add(new JsonArray().add(75.9).add(14.5))
                .add(new JsonArray().add(72).add(13)).add(new JsonArray().add(73).add(20))
                .add(new JsonArray().add(75.9).add(14.5)).add(new JsonArray().add(75.9).add(14.5))
                .add(new JsonArray().add(72).add(13)).add(new JsonArray().add(73).add(20))
                .add(new JsonArray().add(75.9).add(14.5)).add(new JsonArray().add(75.9).add(14.5))
                .add(new JsonArray().add(72).add(13)).add(new JsonArray().add(73).add(20))
                .add(new JsonArray().add(75.9).add(14.5)).add(new JsonArray().add(75.9).add(14.5))
                .add(new JsonArray().add(72).add(13)).add(new JsonArray().add(73).add(20))
                .add(new JsonArray().add(75.9).add(14.5))));

    JsonObject json = QueryMapper.validateQueryParam(requests);

    assertEquals(FAILED, json.getString(STATUS));
    testContext.completeNow();
  }

  @Test
  @Order(5)
  @DisplayName("GeoPoint maxDistance validation")
  public void positiveMaxDistance(VertxTestContext testContext) {

    JsonObject requests = new JsonObject().put(GEOPROPERTY, LOCATION)
        .put(GEORELATION, GEOREL_WITHIN).put(MAX_DISTANCE, 5000).put(GEOMETRY, "Point")
        .put(COORDINATES, new JsonArray().add(73.927).add(18.502))
        .put(SEARCH_TYPE, SEARCH_TYPE_GEO);

    JsonObject json = QueryMapper.validateQueryParam(requests);

    assertEquals(SUCCESS, json.getString(STATUS));
    testContext.completeNow();
  }

  @Test
  @Order(6)
  @DisplayName("GeoPoint maxDistance validation(failed)")
  public void positiveMaxDistanceFailed(VertxTestContext testContext) {

    JsonObject requests = new JsonObject().put(GEOPROPERTY, LOCATION)
        .put(GEORELATION, GEOREL_WITHIN).put(MAX_DISTANCE, -5000).put(GEOMETRY, "Point")
        .put(COORDINATES, new JsonArray().add(73.927).add(18.502))
        .put(SEARCH_TYPE, SEARCH_TYPE_GEO);

    JsonObject json = QueryMapper.validateQueryParam(requests);

    assertEquals(FAILED, json.getString(STATUS));
    testContext.completeNow();
  }
  
  @Test
  @Order(7)
  @DisplayName("Text search validation")
  public void searchTextTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
        .put(Q_VALUE, "Golibar Square")
        .put(SEARCH_TYPE, SEARCH_TYPE_TEXT);

    JsonObject json = QueryMapper.validateQueryParam(requests);

    assertEquals(SUCCESS, json.getString(STATUS));
    testContext.completeNow();
  }
  
  @Test
  @Order(8)
  @DisplayName("Text search validation (exceed limit;failed)")
  public void searchTextFailed(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
        .put(Q_VALUE, "Goliber Square Sivaji chowk Maharashtra "
                + "near Railway station aqm pollution sensor iudx iudxadmin resource")
        .put(SEARCH_TYPE, SEARCH_TYPE_TEXT);

    JsonObject json = QueryMapper.validateQueryParam(requests);

    assertEquals(FAILED, json.getString(STATUS));
    testContext.completeNow();
  }
  
  @Test
  @Order(9)
  @DisplayName("Attribute search validation")
  public void searchAttribute(VertxTestContext testContext) {


    JsonObject requests = new JsonObject()
        .put(PROPERTY, new JsonArray().add(ID))
        .put(VALUE,
            new JsonArray().add(
                new JsonArray().add("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs."
                    + "iudx.io/aqm-bosch-climo/Ambedkarsocietycircle_29")))
        .put(SEARCH_TYPE, SEARCH_TYPE_ATTRIBUTE);

    JsonObject json = QueryMapper.validateQueryParam(requests);

    assertEquals(SUCCESS, json.getString(STATUS));
    testContext.completeNow();
  }
  
  @Test
  @Order(10)
  @DisplayName("Attribute search validation (exceed property;failed)")
  public void searchAttributePropertyFailed(VertxTestContext testContext) {


    JsonObject requests = new JsonObject()
        .put(PROPERTY,
            new JsonArray().add(ID).add("tags").add(LOCATION).add("deviceId").add("name"))
        .put(VALUE,
            new JsonArray().add(
                new JsonArray().add("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs."
                    + "iudx.io/aqm-bosch-climo/Ambedkar society circle_29").add("aqm").add("pune")
                    .add("1234").add("sensor1")))
        .put(SEARCH_TYPE, SEARCH_TYPE_ATTRIBUTE);

    JsonObject json = QueryMapper.validateQueryParam(requests);

    assertEquals(FAILED, json.getString(STATUS));
    testContext.completeNow();
  }
  
  @Test
  @Order(11)
  @DisplayName("Attribute search validation (exceed value;failed)")
  public void searchAttributeValueFailed(VertxTestContext testContext) {


    JsonObject requests = new JsonObject()
        .put(PROPERTY,
            new JsonArray().add(ID).add("tags").add(LOCATION).add("deviceId"))
        .put(VALUE,
            new JsonArray().add(new JsonArray()
                .add("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs."
                    + "iudx.io/aqm-bosch-climo/Ambedkar society circle_29")
                .add("aqm").add("pune").add("1234").add("sensor1")))
        .put(SEARCH_TYPE, SEARCH_TYPE_ATTRIBUTE);

    JsonObject json = QueryMapper.validateQueryParam(requests);

    assertEquals(FAILED, json.getString(STATUS));
    testContext.completeNow();
  }

  @Test
  @Order(12)
  @DisplayName("Attribute search validation (exceed value pair;failed)")
  public void searchAttributeValuePairFailed(VertxTestContext testContext) {


    JsonObject requests = new JsonObject()
        .put(PROPERTY,
            new JsonArray().add("tags"))
        .put(VALUE,
            new JsonArray().add(new JsonArray()
                .add("aqm").add("pm2").add("co2").add("environment").add("flood").add("pm10")))
        .put(SEARCH_TYPE, SEARCH_TYPE_ATTRIBUTE);

    JsonObject json = QueryMapper.validateQueryParam(requests);

    assertEquals(FAILED, json.getString(STATUS));
    testContext.completeNow();
  }

  @Test
  @Order(13)
  @DisplayName("Filter validation")
  public void responseFilter(VertxTestContext testContext) {


    JsonObject requests = new JsonObject()
        .put(Q_VALUE, "Golibar Square")
        .put("filter", new JsonArray().add(ID).add("tags").add("name"))
        .put(SEARCH_TYPE, RESPONSE_FILTER);


    JsonObject json = QueryMapper.validateQueryParam(requests);

    assertEquals(SUCCESS, json.getString(STATUS));
    testContext.completeNow();
  }

  @Test
  @Order(14)
  @DisplayName("Filter validation (exceed limit;failed")
  public void responseFilterFailed(VertxTestContext testContext) {


    JsonObject requests = new JsonObject().put(Q_VALUE, "Golibar Square")
        .put("filter",
            new JsonArray().add(ID).add("tags").add("name").add("deviceId").add("resourceServer")
                .add("provider").add("location").add("address").add("type").add("itemStatus")
                .add("authServerInfo"))
        .put(SEARCH_TYPE, RESPONSE_FILTER);


    JsonObject json = QueryMapper.validateQueryParam(requests);

    assertEquals(FAILED, json.getString(STATUS));
    testContext.completeNow();
  }

  @Test
  @Order(15)
  @DisplayName("Instance limit validation")
  public void instanceLimit(VertxTestContext testContext) {


    JsonObject requests = new JsonObject()
        .put(ID,
            "=datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa"
                + "41dc/rs.iudx.io/aqm-bosch-climo/test2")
        .put("rel", "type").put("instance", "pune");


    JsonObject json = QueryMapper.validateQueryParam(requests);

    assertEquals(SUCCESS, json.getString(STATUS));
    testContext.completeNow();
  }

  @Test
  @Order(16)
  @DisplayName("Limit validation")
  public void limitValidation(VertxTestContext testContext) {


    JsonObject requests = new JsonObject().put(Q_VALUE, "Golibar Square").put("limit", 100)
        .put(SEARCH_TYPE, RESPONSE_FILTER);


    JsonObject json = QueryMapper.validateQueryParam(requests);

    assertEquals(SUCCESS, json.getString(STATUS));
    testContext.completeNow();
  }

  @Test
  @Order(17)
  @DisplayName("Limit validation(exceed;failed")
  public void limitValidationFailed(VertxTestContext testContext) {


    JsonObject requests = new JsonObject().put(Q_VALUE, "Golibar Square").put("limit", 1000001)
        .put(SEARCH_TYPE, RESPONSE_FILTER);


    JsonObject json = QueryMapper.validateQueryParam(requests);

    assertEquals(FAILED, json.getString(STATUS));
    testContext.completeNow();
  }

  @Test
  @Order(18)
  @DisplayName("Offset validation")
  public void offsetValidation(VertxTestContext testContext) {


    JsonObject requests = new JsonObject().put(Q_VALUE, "Golibar Square").put("offset", 1)
        .put(SEARCH_TYPE, RESPONSE_FILTER);


    JsonObject json = QueryMapper.validateQueryParam(requests);

    assertEquals(SUCCESS, json.getString(STATUS));
    testContext.completeNow();
  }

  @Test
  @Order(19)
  @DisplayName("Offser validation(exceed;failed")
  public void offsetValidationFailed(VertxTestContext testContext) {


    JsonObject requests = new JsonObject().put(Q_VALUE, "Golibar Square").put("limit", 1000001)
        .put(SEARCH_TYPE, RESPONSE_FILTER);


    JsonObject json = QueryMapper.validateQueryParam(requests);

    assertEquals(FAILED, json.getString(STATUS));
    testContext.completeNow();
  }
  
  @Test
  @Order(20)
  @DisplayName("Valid ID Test (exceed limit;failed)")
  void validResourceSchemaTest(VertxTestContext testContext) {

    String id = "iudx.uttar.pradesh.vanarasi.org/f"
        + "7e044eee8122b5c87dce6e7ad64f3266044eee8122b5c87dce6e7adafa41dc/"
        + "vanarasi.resourceserver.iudx.io/aqm--pollution-aqm-pm-co2-bosch-climo/aqm_test_2aqm_test_2 enviro"
        + "nment  air quality  climate  air  aqi  aqm  climo  pollution  so2  co2  co  no  no2  pm2.5  pm10  humidity"
        + "  temperature  ozone  o3  noise  light  uv Description for Bosch-Climo AQM resource aqm_test_2aqm_test_2 en"
        + "vironment  air quality  climate  air  aqi  aqm  climo  pollution  so2  co2  co  no  no2  pm2.5  pm10  humidi"
        + "ty  temperature  ozone  o3  noise  light  uv Description for Bosch-Climo AQM resource";

    JsonObject resource = fileSystem.readFileBlocking("./src/test/resources/resources.json")
        .toJsonArray().getJsonObject(0);
    resource.put(NAME, id);

    validator.validateSchema(resource)
        .onComplete(handler -> {
          if(handler.succeeded()){
            testContext.failNow("Fail;");
          } else {
            testContext.completeNow();
          }
        });
  }

 /* @Test
  @Order(21)
  @DisplayName("Crud instance validity")
  void validInstanceCrudTest(VertxTestContext testContext) {

    String id = "pune";

    JsonObject resource = fileSystem.readFileBlocking("./src/test/resources/resources.json")
        .toJsonArray().getJsonObject(0);
    resource.put(INSTANCE, id);

    validator.validateSchema(resource, testContext.succeeding(response -> testContext.verify(() -> {
      testContext.completeNow();
    })));
  }*/

  @Test
  @Order(22)
  @DisplayName("Coordinate double limit")
  void doubleLimitCheckCoordinates(VertxTestContext testContext) {

    JsonObject requests = new JsonObject().put(GEOPROPERTY, LOCATION)
        .put(GEORELATION, GEOREL_WITHIN).put(MAX_DISTANCE, 5000).put(GEOMETRY, POLYGON)
        .put(SEARCH_TYPE, SEARCH_TYPE_GEO).put(COORDINATES,
            new JsonArray()
                .add(new JsonArray().add(new JsonArray().add(Double.POSITIVE_INFINITY).add(14.5))
                    .add(new JsonArray().add(72).add(13)).add(new JsonArray().add(73).add(20))
                    .add(new JsonArray().add(Double.POSITIVE_INFINITY).add(14.5))));

    JsonObject json = QueryMapper.validateQueryParam(requests);

    assertEquals(FAILED, json.getString(STATUS));
    testContext.completeNow();
  }

  @Test
  @Order(23)
  @DisplayName("GeoPoint maxDistance limit validation")
  public void positiveMaxDistanceLimit(VertxTestContext testContext) {

    JsonObject requests = new JsonObject().put(GEOPROPERTY, LOCATION)
        .put(GEORELATION, GEOREL_WITHIN).put(MAX_DISTANCE, 50000).put(GEOMETRY, "Point")
        .put(COORDINATES, new JsonArray().add(73.927).add(18.502))
        .put(SEARCH_TYPE, SEARCH_TYPE_GEO);

    JsonObject json = QueryMapper.validateQueryParam(requests);

    assertEquals(FAILED, json.getString(STATUS));
    testContext.completeNow();
  }

}
