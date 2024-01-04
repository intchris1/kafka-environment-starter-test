package org.kafkaenv.test.kafka.config;

import com.github.dockerjava.api.DockerClient;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.VisibleForTesting;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.kafkaenv.test.kafka.accessor.KafkaContainerTestAccessor;
import org.kafkaenv.test.kafka.container.KafkaContainer;
import org.kafkaenv.test.kafka.container.KafkaUiContainer;
import org.kafkaenv.test.kafka.container.SchemaRegistryContainer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.kafkaenv.test.kafka.EnableTestKafka;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@EqualsAndHashCode
public class KafkaTestContainersContextCustomizer implements ContextCustomizer {

    public static final Network KAFKA_NETWORK = createReusableNetwork("kafka");

    @VisibleForTesting
    public static KafkaContainer KAFKA_CONTAINER = new KafkaContainer();

    @VisibleForTesting
    public static SchemaRegistryContainer SCHEMA_REGISTRY_CONTAINER = new SchemaRegistryContainer();

    @VisibleForTesting
    public static KafkaUiContainer KAFKA_UI_CONTAINER = new KafkaUiContainer();

    private final EnableTestKafka enableTestKafka;

    @Override
    public void customizeContext(@NonNull ConfigurableApplicationContext context, @NonNull MergedContextConfiguration mergedConfig) {
        startKafkaContainer(context);
        if (enableTestKafka.enableSchemaRegistry()) {
            startSchemaRegistryContainer(context);
        }
        if (enableTestKafka.enableKafkaUi()) {
            startKafkaUiContainer(context);
        }
        createTestAccessor(context);
    }

    private void startKafkaContainer(ConfigurableApplicationContext context) {
        if (KAFKA_CONTAINER.isRunning()) {
            log.warn("KAFKA_CONTAINER is already running");
            return;
        }
        KAFKA_CONTAINER.customize(enableTestKafka);
        startAndRegister(context, KAFKA_CONTAINER, "kafkaContainer");
        String bootstrapServers = KAFKA_CONTAINER.getBootstrapServers();
        TestPropertyValues values = TestPropertyValues.of(Map.of(
                "spring.kafka.bootstrap-servers", bootstrapServers,
                "spring.kafka.properties.bootstrap.servers", bootstrapServers
        ));
        log.info("Set 'bootstrap-servers' for application properties: {}", bootstrapServers);
        values.applyTo(context);
    }

    private void createTestAccessor(ConfigurableApplicationContext context) {
        var beanFactory = context.getBeanFactory();
        var kafkaContainerTestAccessor = KafkaContainerTestAccessor.buildDefaultKafkaContainerTestAccessor(enableTestKafka);
        beanFactory.registerSingleton("kafkaContainerTestAccessor", kafkaContainerTestAccessor);
    }

    private void startSchemaRegistryContainer(ConfigurableApplicationContext context) {
        if (SCHEMA_REGISTRY_CONTAINER.isRunning()) {
            log.warn("SCHEMA_REGISTRY_CONTAINER is already running");
            return;
        }
        SCHEMA_REGISTRY_CONTAINER.withKafka(KAFKA_CONTAINER, enableTestKafka);
        SCHEMA_REGISTRY_CONTAINER.customize(enableTestKafka);
        startAndRegister(context, SCHEMA_REGISTRY_CONTAINER, "schemaRegistryContainer");
        String schemaRegistryUrl = SCHEMA_REGISTRY_CONTAINER.getSchemaRegistryUrl();
        var values = TestPropertyValues.of(Map.of("spring.kafka.properties.schema.registry.url", schemaRegistryUrl));
        values.applyTo(context);
        log.info("Set 'schema-registry-url' for application properties: {}", schemaRegistryUrl);
    }

    private void startKafkaUiContainer(ConfigurableApplicationContext context) {
        if (KAFKA_UI_CONTAINER.isRunning()) {
            log.warn("KAFKA_UI_CONTAINER is already running");
            return;
        }
        KAFKA_UI_CONTAINER.withKafka(KAFKA_CONTAINER, enableTestKafka);
        KAFKA_UI_CONTAINER.withSchemaRegistry(SCHEMA_REGISTRY_CONTAINER, enableTestKafka);
        KAFKA_UI_CONTAINER.customize(enableTestKafka);
        startAndRegister(context, KAFKA_UI_CONTAINER, "kafkaUiContainer");
    }


    private static void startAndRegister(ConfigurableApplicationContext context, GenericContainer<?> container, String beanName) {
        String label = "test-name";
        container.withLabel(label, "ucp-starter-test-" + container.getDockerImageName());
        container.start();
        var containers = DockerClientFactory.instance().client().listContainersCmd()
                .withStatusFilter(List.of("running")).withLabelFilter(Map.of(label, container.getLabels().get(label))).exec();
        if (containers.size() > 2) {
            container.stop();
            throw new IllegalStateException(
                    String.format("Container %s was configured for reuse, but its configuration and hash have changed during this launch:" +
                                    " reuse is no longer allowed. " +
                                    "The running container has to be stopped manually, and @EnableTestKafka#destroyContainersAfterTest must be set to true",
                            container.getDockerImageName()));
        }
        var beanFactory = context.getBeanFactory();
        if (beanFactory instanceof DefaultListableBeanFactory dlbf && container instanceof DisposableBean disposableBean) {
            dlbf.registerDisposableBean(beanName, disposableBean);
        }
        beanFactory.registerSingleton(beanName, container);
    }

    public static Network createReusableNetwork(String name) {
        var client = DockerClientFactory.instance().client();
        var network = getNetwork(name, client);
        String id = network.map(com.github.dockerjava.api.model.Network::getId).orElseGet(() -> client.createNetworkCmd()
                .withName(name)
                .withCheckDuplicate(true)
                .withLabels(DockerClientFactory.DEFAULT_LABELS)
                .exec().getId());
        return new Network() {
            @Override
            public Statement apply(Statement base, Description description) {
                return base;
            }

            @Override
            public String getId() {
                return id;
            }

            @Override
            public void close() {
                // never close
            }
        };
    }

    private static Optional<com.github.dockerjava.api.model.Network> getNetwork(String name, DockerClient client) {
        return client.listNetworksCmd().withNameFilter(name).exec().stream()
                .filter(n -> n.getName().equals(name)
                        && n.getLabels().equals(DockerClientFactory.DEFAULT_LABELS))
                .findFirst();
    }
}
