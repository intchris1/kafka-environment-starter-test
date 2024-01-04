package org.kafkaenv.test.kafka.container;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.beans.factory.DisposableBean;
import org.testcontainers.utility.DockerImageName;
import org.kafkaenv.test.kafka.EnableTestKafka;

import static org.kafkaenv.test.kafka.config.KafkaTestContainersContextCustomizer.KAFKA_NETWORK;

@ToString
@EqualsAndHashCode
public class KafkaContainer extends org.testcontainers.containers.KafkaContainer implements DisposableBean {

    public static final String CONFLUENT_PLATFORM_VERSION = "7.4.0";
    private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("confluentinc/cp-kafka")
            .withTag(CONFLUENT_PLATFORM_VERSION);

    private boolean isDestroyAfterTest = true;

    public KafkaContainer() {
        super(KAFKA_IMAGE);
        this.withNetwork(KAFKA_NETWORK)
                .withNetworkAliases("kafka")
                .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
                .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1");
    }

    public static String getJaasConfig() {
        return """
                org.apache.kafka.common.security.plain.PlainLoginModule required username="admin" password="admin" user_admin="admin";""";
    }

    public void customize(EnableTestKafka enableTestKafka) {
        this.withReuse(true);
        this.isDestroyAfterTest = enableTestKafka.destroyContainersAfterTest();
        if (enableTestKafka.enableKafkaSasl()) {
            this.withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "PLAINTEXT:SASL_PLAINTEXT,BROKER:SASL_PLAINTEXT")
                    .withEnv("KAFKA_SASL_MECHANISM_INTER_BROKER_PROTOCOL", "PLAIN")
                    .withEnv("KAFKA_LISTENER_NAME_PLAINTEXT_SASL_ENABLED_MECHANISMS", "PLAIN")
                    .withEnv("KAFKA_LISTENER_NAME_BROKER_SASL_ENABLED_MECHANISMS", "PLAIN")
                    .withEnv("KAFKA_LISTENER_NAME_BROKER_PLAIN_SASL_JAAS_CONFIG", getJaasConfig())
                    .withEnv("KAFKA_LISTENER_NAME_PLAINTEXT_PLAIN_SASL_JAAS_CONFIG", getJaasConfig());
        }
    }

    @Override
    public void destroy() {
        if (this.isDestroyAfterTest) {
            this.stop();
        }
    }
}