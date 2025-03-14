package iudx.catalogue.server.deploy;

import com.hazelcast.config.Config;
import com.hazelcast.config.DiscoveryStrategyConfig;
import com.hazelcast.zookeeper.ZookeeperDiscoveryProperties;
import com.hazelcast.zookeeper.ZookeeperDiscoveryStrategyFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.cli.CLI;
import io.vertx.core.cli.CommandLine;
import io.vertx.core.cli.Option;
import io.vertx.core.cli.TypedOption;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;


/**
 * Deploys clustered vert.x instance of the server. As a JAR, the application requires 3 runtime
 * arguments:
 * <ul>
 * <li>--config/-c : path to the config file</li>
 * <li>--hostname/-i : the hostname for clustering</li>
 * <li>--modules/-m : comma separated list of module names to deploy</li>
 * </ul>
 * e.g. <i>java -jar ./fatjar.jar --host $(hostname) -c configs/config.json -m
 * iudx.catalogue.server.ApiServerVerticle</i></p>
 */


public class Deployer {
  private static final Logger LOGGER = LogManager.getLogger(Deployer.class);
  private static ClusterManager mgr;
  public static Vertx vertx;

  /**
   * Recursively deploy all modules.
   *
   * @param vertx   the vert.x instance
   * @param configs the JSON configuration
   * @param i       for recursive base case
   */

  public static void recursiveDeploy(Vertx vertx, JsonObject configs, int i) {
    if (i >= configs.getJsonArray("modules").size()) {
      LOGGER.info("Deployed all");
      return;
    }
    //    JsonObject config = configs.getJsonArray("modules").getJsonObject(i);
    JsonObject moduleConfigurations = getConfigForModule(i, configs);

    String moduleName = moduleConfigurations.getString("id");
    int numInstances = moduleConfigurations.getInteger("verticleInstances");
    vertx.deployVerticle(moduleName,
        new DeploymentOptions().setInstances(numInstances).setConfig(moduleConfigurations), ar -> {
          if (ar.succeeded()) {
            LOGGER.info("Deployed " + moduleName);
            recursiveDeploy(vertx, configs, i + 1);
          } else {
            LOGGER.fatal("Failed to deploy " + moduleName + " cause:", ar.cause());
          }
        });
  }

  /**
   * Recursively deploy modules/verticles (if they exist) present in the `modules` list.
   *
   * @param vertx   the vert.x instance
   * @param configs the JSON configuration
   * @param modules the list of modules to deploy
   */

  public static void recursiveDeploy(Vertx vertx, JsonObject configs, List<String> modules) {
    if (modules.isEmpty()) {
      LOGGER.info("Deployed requested verticles");
      return;
    }

    JsonArray configuredModules = configs.getJsonArray("modules");

    String moduleName = modules.get(0);
    JsonObject config = configuredModules.stream().map(obj -> (JsonObject) obj)
        .filter(obj -> obj.getString("id").equals(moduleName))
        .findFirst().orElse(new JsonObject());

    if (config.isEmpty()) {
      LOGGER.fatal("Failed to deploy " + moduleName + " cause: Not Found");
      return;
    }

    //get common configs and add this to config object
    JsonObject commonConfigs = configs.getJsonObject("commonConfig");
    config.mergeIn(commonConfigs, true);

    int numInstances = config.getInteger("verticleInstances");
    vertx.deployVerticle(moduleName,
        new DeploymentOptions().setInstances(numInstances).setConfig(config), ar -> {
          if (ar.succeeded()) {
            LOGGER.info("Deployed " + moduleName);
            modules.remove(0);
            recursiveDeploy(vertx, configs, modules);
          } else {
            LOGGER.fatal("Failed to deploy " + moduleName + " cause:", ar.cause());
          }
        });
  }

  private static JsonObject getConfigForModule(int moduleIndex, JsonObject configurations) {
    JsonObject commonConfigs = configurations.getJsonObject("commonConfig");
    JsonObject config = configurations.getJsonArray("modules").getJsonObject(moduleIndex);
    return config.mergeIn(commonConfigs, true);
  }

  /**
   * Returns a ClusterManager instance that uses Hazelcast as the underlying clustering technology,
   * with Zookeeper-based discovery strategy.
   *
   * @param host       The public IP address of the current node in the cluster.
   * @param zookeepers A list of IP addresses/hosts where the Zookeeper instances are running.
   * @param clusterID  A unique identifier for the cluster to which the node belongs.
   * @return A ClusterManager instance that uses Hazelcast and Zookeeper-based discovery.
   */
  public static ClusterManager getClusterManager(
      String host, List<String> zookeepers, String clusterID) {
    Config config = new Config();
    config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
    config.getNetworkConfig().setPublicAddress(host);
    config.setProperty("hazelcast.discovery.enabled", "true");
    config.setProperty("hazelcast.logging.type", "log4j2");
    DiscoveryStrategyConfig discoveryStrategyConfig =
        new DiscoveryStrategyConfig(new ZookeeperDiscoveryStrategyFactory());
    discoveryStrategyConfig.addProperty(ZookeeperDiscoveryProperties.ZOOKEEPER_URL.key(),
        String.join(",", zookeepers));
    discoveryStrategyConfig.addProperty(ZookeeperDiscoveryProperties.GROUP.key(), clusterID);
    config.getNetworkConfig().getJoin().getDiscoveryConfig()
        .addDiscoveryStrategyConfig(discoveryStrategyConfig);

    return new HazelcastClusterManager(config);
  }

  /**
   * Returns an instance of {@link MetricsOptions} configured with Micrometer
   * metrics options for Prometheus
   * along with additional labels and enabled status.
   *
   * @return an instance of {@link MetricsOptions}
   */
  public static MetricsOptions getMetricsOptions() {
    return new MicrometerMetricsOptions()
        .setPrometheusOptions(
            new VertxPrometheusOptions().setEnabled(true).setStartEmbeddedServer(true)
                .setEmbeddedServerOptions(new HttpServerOptions().setPort(9000)))
        // .setPublishQuantiles(true))
        .setLabels(EnumSet.of(Label.EB_ADDRESS, Label.EB_FAILURE, Label.HTTP_CODE,
            Label.HTTP_METHOD))
        .setEnabled(true);
  }

  /**
   * Binds JVM metrics to the default meter registry.
   * The registry collects and records various JVM-related metrics such as memory usage,
   * garbage collection statistics, and thread count.
   */
  public static void setJvmMetrics() {
    MeterRegistry registry = BackendRegistries.getDefaultNow();
    LOGGER.debug(registry);
    new ClassLoaderMetrics().bindTo(registry);
    new JvmMemoryMetrics().bindTo(registry);
    new JvmGcMetrics().bindTo(registry);
    new ProcessorMetrics().bindTo(registry);
    new JvmThreadMetrics().bindTo(registry);

  }

  /**
   * Deploy clustered vert.x instance.
   *
   * @param configPath the path for JSON config file
   * @param host       is String consisting of host name
   * @param modules    list of modules to deploy. If list is empty, all modules are deployed
   */
  public static void deploy(String configPath, String host, List<String> modules) {
    String config;
    try {
      config = new String(Files.readAllBytes(Paths.get(configPath)), StandardCharsets.UTF_8);
    } catch (Exception e) {
      LOGGER.fatal("Couldn't read configuration file");
      return;
    }
    if (config.length() < 1) {
      LOGGER.fatal("Couldn't read configuration file");
      return;
    }
    JsonObject configuration = new JsonObject(config);
    List<String> zookeepers = configuration.getJsonArray("zookeepers").getList();
    String clusterId = configuration.getString("clusterId");
    mgr = getClusterManager(host, zookeepers, clusterId);
    EventBusOptions ebOptions = new EventBusOptions().setHost(host);
    VertxOptions options = new VertxOptions().setClusterManager(mgr).setEventBusOptions(ebOptions)
        .setMetricsOptions(getMetricsOptions());

    LOGGER.debug("metrics-options" + options.getMetricsOptions());
    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {
        vertx = res.result();
        LOGGER.debug(vertx.isMetricsEnabled());
        setJvmMetrics();
        if (modules.isEmpty()) {
          recursiveDeploy(vertx, configuration, 0);
        } else {
          recursiveDeploy(vertx, configuration, modules);
        }
      } else {
        LOGGER.fatal("Could not join cluster");
      }
    });

  }

  /**
   * Graceful shutdown of Vertx application in a sequential manner - 1) undeploy verticle including
   * unregistering of services through the stop method of verticle 2) unregister the vertx from
   * cluster 3) shutdown of vertx 4) shutdown of log4j2. The function is triggered by shutdown hook
   * on a normal shutdown of application.
   */
  public static void gracefulShutdown() {
    Set<String> deployIdSet = vertx.deploymentIDs();
    Logger logger = LogManager.getLogger(Deployer.class);
    logger.info("Shutting down the application");
    CountDownLatch latchVerticles = new CountDownLatch(deployIdSet.size());
    CountDownLatch latchCluster = new CountDownLatch(1);
    CountDownLatch latchVertx = new CountDownLatch(1);
    logger.debug("number of verticles being undeployed are:" + deployIdSet.size());
    // shutdown verticles
    for (String deploymentId : deployIdSet) {
      vertx.undeploy(deploymentId, handler -> {
        if (handler.succeeded()) {
          logger.debug(deploymentId + " verticle  successfully Undeployed");
          latchVerticles.countDown();
        } else {
          logger.warn(deploymentId + "Undeploy failed!");
        }

      });
    }
    try {
      latchVerticles.await(5, TimeUnit.SECONDS);
      logger.info("All the verticles undeployed");
      Promise<Void> promise = Promise.promise();
      // leave the cluster
      mgr.leave(promise);
      logger.info("vertx left cluster succesfully");
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      latchCluster.await(5, TimeUnit.SECONDS);
      // shutdown the vertx
      vertx.close(handler -> {
        if (handler.succeeded()) {
          logger.info("vertx closed successfully");
          latchVertx.countDown();
        } else {
          logger.warn("Vertx didn't close properly, reason:" + handler.cause());
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
    }

    try {
      latchVertx.await(5, TimeUnit.SECONDS);
      // then shut down log4j
      if (LogManager.getContext() instanceof LoggerContext) {
        LOGGER.info("Shutting down log4j2");
        LogManager.shutdown((LoggerContext) LogManager.getContext());
      } else {
        LOGGER.warn("Unable to shutdown log4j2");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Main method for IUDX Cat CLI.
   *
   * @param args an array of command line arguments
   */
  public static void main(String[] args) {
    CLI cli = CLI.create("IUDX Cat").setSummary("A CLI to deploy the catalogue")
        .addOption(new Option().setLongName("help").setShortName("h").setFlag(true)
            .setDescription("display help"))
        .addOption(new Option().setLongName("config").setShortName("c").setRequired(true)
            .setDescription("configuration file"))
        .addOption(new Option().setLongName("host").setShortName("i").setRequired(true)
            .setDescription("hostname for vertx clustering"))
        .addOption(new TypedOption<String>().setType(String.class).setLongName("modules")
            .setShortName("m").setRequired(false).setDefaultValue("all").setParsedAsList(true)
            .setDescription("comma separated list of verticle names to deploy. "
                + "If omitted, or if `all` is passed, all verticles are deployed"));

    StringBuilder usageString = new StringBuilder();
    cli.usage(usageString);
    CommandLine commandLine = cli.parse(Arrays.asList(args), false);
    if (commandLine.isValid() && !commandLine.isFlagEnabled("help")) {
      String configPath = commandLine.getOptionValue("config");
      String host = commandLine.getOptionValue("host");
      List<String> passedModules = commandLine.getOptionValues("modules");
      List<String> modules = passedModules.stream().distinct().collect(Collectors.toList());

      /* `all` is also passed by default if no -m option given. */
      if (modules.contains("all")) {
        deploy(configPath, host, List.of());
      } else {
        deploy(configPath, host, modules);
      }
      Runtime.getRuntime().addShutdownHook(new Thread(() -> gracefulShutdown()));
    } else {
      System.out.println(usageString);
    }
  }

}
