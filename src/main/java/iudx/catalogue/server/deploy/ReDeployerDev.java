package iudx.catalogue.server.deploy;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.apiserver.ApiServerVerticle;
import iudx.catalogue.server.authenticator.AuthenticationVerticle;
import iudx.catalogue.server.database.elastic.ElasticsearchVerticle;
import iudx.catalogue.server.geocoding.GeocodingVerticle;
import iudx.catalogue.server.nlpsearch.NLPSearchVerticle;
import iudx.catalogue.server.validator.ValidatorVerticle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ReDeployerDev extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(ReDeployerDev.class);

  @Override
  public void start(Promise<Void> promise) throws Exception {

    JsonObject commonConfigs = config().getJsonObject("commonConfig");
    /* Deploy the Database Service Verticle. */
    /* Get configs. Note the order matters. Don't change it */
    JsonObject dbConfig = config().getJsonArray("modules").getJsonObject(0)
            .mergeIn(commonConfigs, true);
    JsonObject authConfig = config().getJsonArray("modules").getJsonObject(1)
            .mergeIn(commonConfigs, true);
    JsonObject valConfig = config().getJsonArray("modules").getJsonObject(2)
            .mergeIn(commonConfigs, true);
    JsonObject apiConfig = config().getJsonArray("modules").getJsonObject(3)
            .mergeIn(commonConfigs, true);
    JsonObject geoConfig = config().getJsonArray("modules").getJsonObject(4)
            .mergeIn(commonConfigs, true);
    JsonObject nlpConfig = config().getJsonArray("modules").getJsonObject(5)
            .mergeIn(commonConfigs, true);
    vertx.deployVerticle(new ElasticsearchVerticle(), new DeploymentOptions().setConfig(dbConfig),
        databaseVerticle -> {
        if (databaseVerticle.succeeded()) {
          LOGGER.info("The Database Service is ready");

          /* Deploy the Authentication Service Verticle. */

          vertx.deployVerticle(new AuthenticationVerticle(),
                  new DeploymentOptions().setConfig(authConfig),
              authenticationVerticle -> {
              if (authenticationVerticle.succeeded()) {
                LOGGER.info("The Authentication Service is ready");

                /* Deploy the Validator Service Verticle. */

                vertx.deployVerticle(new ValidatorVerticle(),
                    new DeploymentOptions().setConfig(valConfig),
                    validatorVerticle -> {
                    if (validatorVerticle.succeeded()) {
                      LOGGER.info("The Validator Service is ready");


                      /* Deploy the Api Server Verticle. */

                      vertx.deployVerticle(new ApiServerVerticle(),
                        new DeploymentOptions().setConfig(apiConfig),
                          apiServerVerticle -> {
                          if (apiServerVerticle.succeeded()) {
                            LOGGER.info("The Catalogue API Server is ready");

                            vertx.deployVerticle(new GeocodingVerticle(),
                              new DeploymentOptions().setConfig(geoConfig),
                                geocodingVerticle -> {
                                if (geocodingVerticle.succeeded()) {
                                  LOGGER.info("The Geocoding Service is ready");

                                  vertx.deployVerticle(new NLPSearchVerticle(),
                                    new DeploymentOptions().setConfig(nlpConfig),
                                      nlpsearchVerticle -> {
                                      if (nlpsearchVerticle.succeeded()) {
                                        LOGGER.info("The NLP Service is ready");
                                        promise.complete();
                                      } else {
                                        LOGGER.info("The NLP Server startup failed !");
                                      }
                                    });
                                } else {
                                  LOGGER.info("The Geocoding Server starup failed !");
                                }
                              });
                    
                          } else {
                            LOGGER.info("The Catalogue API Server startup failed !");
                          }
                        });

                    } else {
                      LOGGER.info("The Validator Service failed !");
                    }
                  });

              } else {
                LOGGER.info("The Authentication Service failed !");
              }
            });

        } else {
          LOGGER.info("The Database Service failed !");
        }
      });
  }
}

