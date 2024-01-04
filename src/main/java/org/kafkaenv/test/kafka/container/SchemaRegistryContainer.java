package org.kafkaenv.test.kafka.container;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.beans.factory.DisposableBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.kafkaenv.test.kafka.EnableTestKafka;

import static org.testcontainers.utility.MountableFile.forClasspathResource;
import static org.kafkaenv.test.kafka.config.KafkaTestContainersContextCustomizer.KAFKA_NETWORK;
import static org.kafkaenv.test.kafka.container.KafkaContainer.CONFLUENT_PLATFORM_VERSION;
import static org.kafkaenv.test.kafka.container.KafkaContainer.getJaasConfig;

@ToString
@EqualsAndHashCode
public class SchemaRegistryContainer extends GenericContainer<SchemaRegistryContainer> implements DisposableBean {
    public static final String SCHEMA_REGISTRY_IMAGE = "confluentinc/cp-schema-registry";

    private boolean isDestroyAfterTest = true;

    public SchemaRegistryContainer() {
        this(CONFLUENT_PLATFORM_VERSION);
    }

    public SchemaRegistryContainer(String version) {
        super(SCHEMA_REGISTRY_IMAGE + ":" + version);
        this.waitingFor(Wait.forHttp("/subjects").withBasicCredentials("admin", "admin").forStatusCode(200));
        this.withExposedPorts(8081);
    }

    public void withKafka(KafkaContainer kafka, EnableTestKafka enableTestKafka) {
        withKafka(KAFKA_NETWORK, kafka.getNetworkAliases().get(1) + ":9092", enableTestKafka);
    }

    public void withKafka(Network network, String bootstrapServers, EnableTestKafka enableTestKafka) {
        this.withNetwork(network);
        this.withNetworkAliases("schema-registry");
        this.withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry");
        this.withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081");
        this.withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", (enableTestKafka.enableKafkaSasl() ?
                "SASL_" : "") + "PLAINTEXT://" + bootstrapServers);
        if (enableTestKafka.enableKafkaSasl()) {
            this.withEnv("SCHEMA_REGISTRY_KAFKASTORE_SECURITY_PROTOCOL", "SASL_PLAINTEXT")
                    .withEnv("SCHEMA_REGISTRY_KAFKASTORE_SASL_MECHANISM", "PLAIN")
                    .withEnv("SCHEMA_REGISTRY_KAFKASTORE_SASL_JAAS_CONFIG", getJaasConfig());
        }
    }

    public String getSchemaRegistryUrl() {
        return "http://" + this.getHost() + ":" + this.getFirstMappedPort();
    }

    public void customize(EnableTestKafka enableTestKafka) {
        this.withReuse(true);
        this.isDestroyAfterTest = enableTestKafka.destroyContainersAfterTest();
        if (enableTestKafka.enableSchemaRegistryBasicAuth()) {
            this.withEnv("SCHEMA_REGISTRY_AUTHENTICATION_METHOD", "BASIC")
                    .withEnv("SCHEMA_REGISTRY_AUTHENTICATION_REALM", "SchemaRegistry-Props")
                    .withEnv("SCHEMA_REGISTRY_AUTHENTICATION_ROLES", "admin")
                    .withCopyFileToContainer(forClasspathResource("schema-registry/jaas_config.file"), "/etc/schema-registry/jaas_config.file")
                    .withCopyFileToContainer(forClasspathResource("schema-registry/password-file"), "/etc/schema-registry/password-file")
                    .withEnv("SCHEMA_REGISTRY_OPTS", "-Djava.security.auth.login.config=/etc/schema-registry/jaas_config.file");
        }
    }

    @Override
    public void destroy() {
        if (this.isDestroyAfterTest) {
            this.stop();
        }
    }
}