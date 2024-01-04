package org.kafkaenv.test.kafka.container;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.kafkaenv.test.kafka.EnableTestKafka;
import org.springframework.beans.factory.DisposableBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import static org.kafkaenv.test.kafka.config.KafkaTestContainersContextCustomizer.KAFKA_NETWORK;
import static org.kafkaenv.test.kafka.container.KafkaContainer.getJaasConfig;
import static org.testcontainers.utility.MountableFile.forClasspathResource;

@ToString
@EqualsAndHashCode
public class KafkaUiContainer extends GenericContainer<KafkaUiContainer> implements DisposableBean {

    private static final DockerImageName KAFKA_UI_IMAGE = DockerImageName.parse("provectuslabs/kafka-ui:master");

    private boolean isDestroyAfterTest = true;

    public KafkaUiContainer() {
        super(KAFKA_UI_IMAGE);
    }

    public void withKafka(KafkaContainer kafka, EnableTestKafka enableTestKafka) {
        withKafka(KAFKA_NETWORK, kafka.getNetworkAliases().get(1) + ":9092", enableTestKafka);
        if (enableTestKafka.enableKafkaSasl()) {
            this.withEnv("KAFKA_CLUSTERS_0_PROPERTIES_SECURITY_PROTOCOL", "SASL_PLAINTEXT")
                .withEnv("KAFKA_CLUSTERS_0_PROPERTIES_SASL_MECHANISM", "PLAIN")
                .withEnv("KAFKA_CLUSTERS_0_PROPERTIES_SASL_JAAS_CONFIG", getJaasConfig());
        }
    }

    public void withKafka(Network network, String bootstrapServers, EnableTestKafka enableTestKafka) {
        this.withNetwork(network)
            .withNetworkAliases("kafka-ui")
            .withEnv("KAFKA_CLUSTERS_0_NAME", "local")
            .withEnv("KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS", (enableTestKafka.enableKafkaSasl()
                                                                   ?
                                                                   "SASL_"
                                                                   : "") + "PLAINTEXT://" + bootstrapServers)
            .withEnv("DYNAMIC_CONFIG_ENABLED", "true")
            .withExposedPorts(8080);
    }

    public void withSchemaRegistry(SchemaRegistryContainer schemaRegistry, EnableTestKafka enableTestKafka) {
        if (schemaRegistry.isRunning()) {
            this.withEnv("KAFKA_CLUSTERS_0_SCHEMAREGISTRY",
                    "http://" + schemaRegistry.getNetworkAliases().get(1) + ":8081");
        }
        if (enableTestKafka.enableSchemaRegistryBasicAuth()) {
            this.withEnv("KAFKA_CLUSTERS_0_SCHEMAREGISTRYAUTH_USERNAME", "admin");
            this.withEnv("KAFKA_CLUSTERS_0_SCHEMAREGISTRYAUTH_PASSWORD", "admin");
        }
    }

    /**
     * Ability to connect to a remote SchemaRegistry.
     * Kafka-ui still requires a working connection to kafka to work, you can use a locally raised container.
     * For example: run a test over which will be @EnableTestKafka (enableKafkaUi = true, destroyContainersAfterTest
     * = false)
     * and instead of using {@link KafkaUiContainer#withSchemaRegistry} use this method.
     *
     * @param schemaRegistryUrl  SchemaRegistry url
     * @param login              login for basic auth
     * @param password           password for basic auth
     * @param truststoreLocation location of truststore.jks (in /resources)
     * @param truststorePassword password for truststore.jks
     */
    public void withRemoteSchemaRegistry(String schemaRegistryUrl,
                                         String login, String password,
                                         String truststoreLocation, String truststorePassword) {
        this.withEnv("KAFKA_CLUSTERS_0_SCHEMAREGISTRY", schemaRegistryUrl);
        this.withEnv("KAFKA_CLUSTERS_0_SCHEMAREGISTRYAUTH_USERNAME", login);
        this.withEnv("KAFKA_CLUSTERS_0_SCHEMAREGISTRYAUTH_PASSWORD", password)
            .withEnv("DYNAMIC_CONFIG_ENABLED", "true")
            .withCopyFileToContainer(forClasspathResource(truststoreLocation), truststoreLocation)
            .withEnv("KAFKA_CLUSTERS_0_SSL_TRUSTSTORE_LOCATION", truststoreLocation)
            .withEnv("KAFKA_CLUSTERS_0_SSL_TRUSTSTORE_PASSWORD", truststorePassword)
            .withExposedPorts(8080);
    }

    public void customize(EnableTestKafka enableTestKafka) {
        this.withReuse(true);
        this.isDestroyAfterTest = enableTestKafka.destroyContainersAfterTest();
    }

    @Override
    public void destroy() {
        if (this.isDestroyAfterTest) {
            this.stop();
        }
    }
}