package org.kafkaenv.test.kafka;

import lombok.experimental.UtilityClass;
import org.kafkaenv.test.kafka.config.KafkaTestContainersContextCustomizer;
import org.kafkaenv.test.kafka.container.KafkaContainer;
import org.kafkaenv.test.kafka.container.KafkaUiContainer;
import org.kafkaenv.test.kafka.container.SchemaRegistryContainer;

@UtilityClass
public class TestUtil {

    public static void destroyContainers() {
        KafkaTestContainersContextCustomizer.KAFKA_CONTAINER.destroy();
        KafkaTestContainersContextCustomizer.SCHEMA_REGISTRY_CONTAINER.destroy();
        KafkaTestContainersContextCustomizer.KAFKA_UI_CONTAINER.destroy();
        KafkaTestContainersContextCustomizer.KAFKA_CONTAINER = new KafkaContainer();
        KafkaTestContainersContextCustomizer.SCHEMA_REGISTRY_CONTAINER = new SchemaRegistryContainer();
        KafkaTestContainersContextCustomizer.KAFKA_UI_CONTAINER = new KafkaUiContainer();
    }
}
