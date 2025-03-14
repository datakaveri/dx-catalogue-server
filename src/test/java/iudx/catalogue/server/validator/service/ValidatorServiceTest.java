package iudx.catalogue.server.validator.service;

import iudx.catalogue.server.database.elastic.ElasticClient;
import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
import iudx.catalogue.server.database.elastic.service.ElasticsearchServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static iudx.catalogue.server.util.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import iudx.catalogue.server.Configuration;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ValidatorServiceTest {

  private static final Logger LOGGER = LogManager.getLogger(ValidatorServiceTest.class);
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
  private static boolean isUacInstance;
  private static String vocContext;

  @BeforeAll
  @DisplayName("Deploying Verticle")
  static void startVertx(Vertx vertx, VertxTestContext testContext) {
    vertxObj = vertx;
    fileSystem = vertx.fileSystem();
    JsonObject validatorConfig = Configuration.getConfiguration("./configs/config-test.json", 2);

    /* Configuration setup */
    databaseIP = validatorConfig.getString(DATABASE_IP);
    databasePort = validatorConfig.getInteger(DATABASE_PORT);
    databaseUser = validatorConfig.getString(DATABASE_UNAME);
    databasePassword = validatorConfig.getString(DATABASE_PASSWD);
    docIndex = validatorConfig.getString(DOC_INDEX);
    vocContext = "xyz";

    // TODO : Need to enable TLS using xpack security
    client = new ElasticClient(databaseIP, databasePort, docIndex, databaseUser, databasePassword);
    esService = new ElasticsearchServiceImpl(client);
    validator = new ValidatorServiceImpl(esService, docIndex,isUacInstance, vocContext);
    testContext.completeNow();
  }

  @AfterEach
  public void finish(VertxTestContext testContext) {
    LOGGER.info("Finishing....");
    vertxObj.close(testContext.succeeding(response -> testContext.completeNow()));
  }

  /*  @Test
  @Order(1)
  @DisplayName("Test Link Validation [Resource]")
  void validResourceLinkTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request
        .put("type", new JsonArray().add("iudx:Resource"))
        .put(
            "id",
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/aqm-bosch-climo/aqm_test_1")
        .put(
            "resourceGroup",
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/aqm-bosch-climo")
        .put("provider", "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86");
    validator.validateItem(
        request,
        testContext.succeeding(
            response ->
                testContext.verify(
                    () -> {
                      testContext.completeNow();
                    })));
  }

  @Test
  @Order(2)
  @DisplayName("Test Link Validation [ResourceGroup]")
  void validResourceGroupLinkTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request
        .put("type", new JsonArray().add("iudx:ResourceGroup"))
        .put("id",
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/aqm-bosch-climo")
        .put(
            "provider",
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86")
        .put(
            "resourceServer", "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io");
    validator.validateItem(
        request,
        testContext.succeeding(
            response ->
                testContext.verify(
                    () -> {
                      testContext.completeNow();
                    })));
  }*/

  @Test
  @Order(3)
  @DisplayName("Test Invalid Link [Resource]")
  void invalidResourceLinkTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request
        .put("type", new JsonArray().add("iudx:Resource"))
        .put( "id", "ri-id")
        .put("name", "ri-name")
        .put( "resourceGroup", "rg-id")
        .put("resourceServer", "rs-id")
        .put("provider", "provider-id");

    validator.validateItem(request)
            .onComplete(handler -> {
              if(handler.succeeded())
              {
                testContext.failNow("Succeeded with invalid link resource");
              }
              else
              {
                testContext.verify(
                        () -> {
                          testContext.completeNow();
                        });
              }
            });
    testContext.completeNow();
  }

  @Test
  @Order(4)
  @DisplayName("Test Invalid Link [ResourceGroup]")
  void invalidResourceGroupLinkTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request
        .put("type", new JsonArray().add("iudx:ResourceGroup"))
        .put("id", "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.iudx.org.in/sensors")
        .put("name", "rg-name")
        .put(
            "provider",
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.iudx.org.in/sensors")
        .put(
            "resourceServer",
            "rbccps.org/00D75505FD5256B142AFD9C0E32790FA7180D500/rs.invalid.org.in");

    validator.validateItem(request)
            .onComplete(handler -> {
              if(handler.succeeded())
              {
                testContext.failNow("Succeeded with invalid link resource group");
              }
              else
              {
                testContext.verify(
                        () -> {
                          testContext.completeNow();
                        });
              }
            });
    testContext.completeNow();
  }

 /* @Test
  @Order(5)
  @DisplayName("Valid Schema Test [Resource]")
  void validResourceSchemaTest(VertxTestContext testContext) {

    JsonObject resource =
        fileSystem
            .readFileBlocking("./src/test/resources/resources.json")
            .toJsonArray()
            .getJsonObject(0);

    validator.validateSchema(
        resource,
        testContext.succeeding(
            response ->
                testContext.verify(
                    () -> {
                      testContext.completeNow();
                    })));
  }

  @Test
  @Order(7)
  @DisplayName("Valid Schema Test [ResourceGroup]")
  void validResourceGroupSchemaTest(VertxTestContext testContext) {

    JsonObject resourceGrp =
        fileSystem
            .readFileBlocking("./src/test/resources/resourceGroup.json")
            .toJsonArray()
            .getJsonObject(0);

    LOGGER.debug(resourceGrp.toString());

    validator.validateSchema(
        resourceGrp,
        testContext.succeeding(
            response ->
                testContext.verify(
                    () -> {
                      testContext.completeNow();
                    })));
  }*/

  @Test
  @Order(8)
  @DisplayName("Invalid Schema Test")
  void invalidSchemaTest(VertxTestContext testContext) {
    try {
      JsonObject request =
          new JsonObject(
              "{\"id\":\"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.iudx.org.in/sensors\",\"description\": \"Description of this resource group\",\"name\": \"sensors\",\"tags\": \"sensor, sensing, resource, battery operated\",\"itemStatus\": \"ACTIVE\",\"provider\": \"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531\",\"resourceServer\": \"rbccps.org/00D75505FD5256B142AFD9C0E32790FA7180D500/rs.iudx.org.in\",\"resourceAuthControlLevel\": \"INDIVIDUAL\",\"resourceType\": \"messageStream\",\"authServerInfo\": {\"type\": [\"AuthServerInfoValue\"],\"authServerURL\": \"https://auth.iudx.org.in\",\"authType\": \"iudx-auth\"},\"accessObjectInfo\":{\"type\": [\"AccessObjectInfoValue\"],\"accessObject\": \"https://example.com/sensorsApis.json\",\"additionalInfoURL\": \"https://example.com/sensorsApis\",\"accessObjectType\": \"openAPI\"},\"iudxResourceAPIs\": [\"attribute\", \"temporal\"],\"itemCreatedAt\": \"2019-02-20T10:30:06.093121\",\"location\": {\"type\": \"Place\",\"address\": \"Bangalore\"}}");
      LOGGER.debug(request.toString());
      validator.validateSchema(request)
                      .onComplete(handler -> {
                        if(handler.succeeded())
                        {
                          testContext.failNow("Succeeded with invalid schema");
                        }
                        else
                        {
                          testContext.verify(
                              testContext::completeNow);
                        }
                      });

    } catch (Exception e) {
      e.printStackTrace();
    }
    testContext.completeNow();
  }

  @Test
  @Order(9)
  @DisplayName("Valid Schema Test, large name [Resource]")
  void validResourceSchemaTestName(VertxTestContext testContext) {

    JsonObject resource =
        fileSystem
            .readFileBlocking("./src/test/resources/resources.json")
            .toJsonArray()
            .getJsonObject(0);
    resource.put(
        "name",
        "2AgEk25l7odg91lTOolXHouSDUjbB_JbNvqYrhQUjfIfAbkv03tBqBW_EQK7f733MdtfNcqti7K1xt4o3rLEFVQqVrQZHTm4vf8fAGs8KsqVjDFPGcJM/UgDxrcF7YMnbTdePN7pRr8/9T1o_2bUkH3DoktbOTk3FkD8IsdHm_OdKIuGEvjeMis0oqQiEEXqPNqdUpPA5lqjV1c76ihPoOmO/1XzJkWRcgc_MXSWy8Q2u/2FAPTmOKGSW5LE6wHXIDt/0hPlwCByXNazmQxcO/GRdAznMJKo_Xj7BtJHsx3m/oYus9cJYj1KDTJt2qL98mQ1Z0Al_PsknycOspHWplfesuVSsebZ92Xe5wbpy/4OFSHxUjxevkCSUm38Q/XkUTa1zfByV6P2VOSz3Fc_VN0kRHZyNx32NwcG76CV3QDoUoDDyHNvsK8vgdR3Z/AVSP4P/h/IoX3s6o/rcLLYWD7ioOMHDPYqMLcarkSDMKOG_PvLVCGdJbh44n583VbY");
    validator.validateSchema(resource)
            .onComplete(handler -> {
              if(handler.succeeded())
              {
                testContext.failNow("Succeeded with invalid schema");
              }
              else
              {
                testContext.verify(
                        () -> {
                          testContext.completeNow();
                        });
              }
            });
    testContext.completeNow();
  }

  @Test
  @DisplayName("Valid Rating schema")
  void validRatingSchemaTest(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put("rating", 4.5)
            .put("comment", "some-comment")
            .put(
                "id",
                "2596264a-ff2a-40f7-90cc-17a57b2adffe")
            .put("userID", "some-user")
            .put("status", "pending");

    validator.validateRating(request)
            .onComplete(handler -> {
              if(handler.failed())
              {
                testContext.failNow("Failed with valid rating schema");
              }
              else
              {
                testContext.verify(
                        () -> {
                          String status = handler.result().getString(TITLE);
                          assertEquals(SUCCESS, status);
                          testContext.completeNow();
                        });
              }
            });
    testContext.completeNow();
  }

  @Test
  @DisplayName("Invalid Rating schema")
  void invalidRatingSchemaTest(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put("rating", 4.5)
            .put("comment", "some-comment")
            .put("userID", "some-user")
            .put("status", "pending");

    validator.validateRating(request)
            .onComplete(handler -> {
              if(handler.succeeded())
              {
                testContext.failNow("Succeeded with invalid rating schema");
              }
              else
              {
                testContext.verify(
                        () -> {
                          testContext.completeNow();
                        });
              }
            });
    testContext.completeNow();
  }

  @Test
  @DisplayName("Valid mlayer instance schema")
  void validMLInstanceSchemaTest(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put("name", "lucknow")
            .put(
                "icon",
                "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/covers/lucknow.jpg")
            .put(
                "cover",
                "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/covers/lucknow.jpg")
            .put(
                "logo",
                "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/covers/lucknow.jpg");

    validator.validateMlayerInstance(request)
            .onComplete(handler -> {
              if(handler.failed())
              {
                testContext.failNow("Failed with valid instance schema");
              }
              else
              {
                testContext.verify(
                        () -> {
                          testContext.completeNow();
                        });
              }
            });
    testContext.completeNow();
  }

  @Test
  @DisplayName("invalid mlayer instance schema")
  void invalidMLInstanceSchemaTest(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put("name", "lucknow")
            .put(
                "icon",
                "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/covers/lucknow.jpg")
            .put(
                "logo",
                "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/covers/lucknow.jpg");

    validator.validateMlayerInstance(request)
            .onComplete(handler -> {
              if(handler.succeeded())
              {
                testContext.failNow("Failed with invalid instance schema");
              }
              else
              {
                testContext.verify(
                        () -> {
                          testContext.completeNow();
                        });
              }
            });
    testContext.completeNow();
  }

  @Test
  @DisplayName("valid mlayer domain schema")
  void validMLDomainSchemaTest(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put("description", "Data Models that pertain to civic domain")
            .put(
                "icon",
                "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/icons/civic.png")
            .put("label", "Civic")
            .put("name", "civic");

    validator.validateMlayerDomain(request)
            .onComplete(handler -> {
              if(handler.failed())
              {
                testContext.failNow("Failed with valid domain schema");
              }
              else
              {
                testContext.verify(
                        () -> {
                          testContext.completeNow();
                        });
              }
            });
    testContext.completeNow();
  }

  @Test
  @DisplayName("invalid mlayer domain schema")
  void invalidMLDomainSchemaTest(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put("description", "Data Models that pertain to civic domain")
            .put("icon", "akjbaijfbai")
            .put("label", "Civic")
            .put("name", "civic");

    validator.validateMlayerDomain(request)
            .onComplete(handler -> {
              if(handler.succeeded())
              {
                testContext.failNow("Succeeded with invalid domain schema");
              }
              else
              {
                testContext.verify(
                        () -> {
                          testContext.completeNow();
                        });
              }
            });
    testContext.completeNow();
  }

  @Test
  @DisplayName("valid mlayer gq schema")
  void validMLGQSchemaTest(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put(
                "id",
                new JsonArray()
                    .add(
                        "8b95ab80-2aaf-4636-a65e-7f2563d0d371")
                    .add(
                        "8b95ab80-2aaf-4636-a65e-7f2563d0d371"))
            .put("instance", "pune");

    validator.validateMlayerGeoQuery(request)
            .onComplete(handler -> {
              if(handler.failed())
              {
                testContext.failNow("Failed with valid geoquery schema");
              }
              else
              {
                testContext.verify(
                        () -> {
                          testContext.completeNow();
                        });
              }
            });
    testContext.completeNow();
  }

  @Test
  @DisplayName("invalid mlaery gq schema")
  void invalidMLGQSchemaTest(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put(
                "id",
                new JsonArray()
                    .add(
                        "datakaveri.org/b8bd3e3f39615c8ec96722131ae95056b5938f2f/rs.iudx.io/agra-swachhata-app")
                    .add(
                        "datakaveri.org/b8bd3e3f39615c8ec96722131ae95056b5938f2f/rs.iudx.io/chennai-bike-docking-locations"))
            .put("eynstance", "pune");

    validator.validateMlayerGeoQuery(request)
            .onComplete(handler -> {
              if(handler.succeeded())
              {
                testContext.failNow("Succeeded with invalid geoquery schema");
              }
              else
              {
                testContext.verify(
                        () -> {
                          testContext.completeNow();
                        });
              }
            });
    testContext.completeNow();
  }

  @Test
  @DisplayName("valid mlayer dataset schema")
  void validMLdataetTest(VertxTestContext testContext) {
    JsonObject request =
            new JsonObject()
                    .put(
                            "id", "8b95ab80-2aaf-4636-a65e-7f2563d0d371")
                    .put("instance", "pune");
    validator.validateMlayerDatasetId(request)
            .onComplete(handler -> {
              if(handler.failed())
              {
                testContext.failNow("Failed with valid mlayer dataset schema");
              }
              else
              {
                testContext.verify(
                        () -> {
                          testContext.completeNow();
                        });
              }
            });
    testContext.completeNow();
  }

  @Test
  @DisplayName("invalid mlayer dataset schema")
  void invalidMLDatasetchemaTest(VertxTestContext testContext) {
    JsonObject request =
            new JsonObject()
                    .put(
                            "id", "datakaveri.org/b8bd3e3f39615c8ec96722131ae95056b5938f2f/rs.iudx.io/agra-swachhata-app")
                    .put("eynstance", "pune");

    validator.validateMlayerDatasetId(request)
            .onComplete(handler -> {
              if(handler.succeeded())
              {
                testContext.failNow("Succeeded with invalid mlayer dataset schema");
              }
              else
              {
                testContext.verify(
                        () -> {
                          testContext.completeNow();
                        });
              }
            });
    testContext.completeNow();
  }
}
