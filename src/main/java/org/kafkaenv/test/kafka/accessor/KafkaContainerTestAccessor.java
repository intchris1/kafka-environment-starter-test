package org.kafkaenv.test.kafka.accessor;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.kafkaenv.test.kafka.EnableTestKafka;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS;
import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE;
import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;
import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG;
import static io.confluent.kafka.serializers.KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;
import static org.kafkaenv.test.kafka.config.KafkaTestContainersContextCustomizer.KAFKA_CONTAINER;
import static org.kafkaenv.test.kafka.config.KafkaTestContainersContextCustomizer.SCHEMA_REGISTRY_CONTAINER;
import static org.kafkaenv.test.kafka.container.KafkaContainer.getJaasConfig;

@Slf4j
public record KafkaContainerTestAccessor<T>(ProducerFactory<String, T> testProducerFactory,
                                            ConsumerFactory<String, T> testConsumerFactory) {

    public static <T> KafkaContainerTestAccessor<T> buildDefaultKafkaContainerTestAccessor(EnableTestKafka enableTestKafka) {
        ConsumerFactory<String, T> consumerFactory = consumerFactory(enableTestKafka);
        ProducerFactory<String, T> producerFactory = producerFactory(enableTestKafka);
        return new KafkaContainerTestAccessor<>(producerFactory, consumerFactory);
    }

    private static <T> DefaultKafkaConsumerFactory<String, T> consumerFactory(EnableTestKafka enableTestKafka) {
        boolean isSchemaRegistry = enableTestKafka.enableSchemaRegistry();
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_CONTAINER.getBootstrapServers());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, isSchemaRegistry ? KafkaAvroDeserializer.class : ByteArrayDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test_group");
        if (isSchemaRegistry) {
            props.put(SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_CONTAINER.getSchemaRegistryUrl());
            props.put(AUTO_REGISTER_SCHEMAS, true);
            props.put(SPECIFIC_AVRO_READER_CONFIG, true);
        }
        props.putAll(getCommonConfig(enableTestKafka));
        return new DefaultKafkaConsumerFactory<>(props);
    }

    private static <T> DefaultKafkaProducerFactory<String, T> producerFactory(EnableTestKafka enableTestKafka) {
        boolean isSchemaRegistry = enableTestKafka.enableSchemaRegistry();
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_CONTAINER.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, isSchemaRegistry ? KafkaAvroSerializer.class : ByteArraySerializer.class);
        if (isSchemaRegistry) {
            props.put(SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_CONTAINER.getSchemaRegistryUrl());
            props.put(AUTO_REGISTER_SCHEMAS, true);
        }
        props.putAll(getCommonConfig(enableTestKafka));
        return new DefaultKafkaProducerFactory<>(props);
    }

    private static Map<String, Object> getCommonConfig(EnableTestKafka enableTestKafka) {
        Map<String, Object> props = new HashMap<>();
        if (enableTestKafka.enableKafkaSasl()) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
            props.put(SASL_MECHANISM, "PLAIN");
            props.put(SASL_JAAS_CONFIG, getJaasConfig());
        }
        if (enableTestKafka.enableSchemaRegistryBasicAuth()) {
            props.put(BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
            props.put(USER_INFO_CONFIG, "admin:admin");
        }
        return props;
    }

    public void sendMessage(String topic, T payload) {
        sendMessage(topic, UUID.randomUUID().toString(), payload);
    }

    @SneakyThrows
    public void sendMessage(String topic, String key, T payload) {
        try (var producer = testProducerFactory.createProducer()) {
            var incomingMessage = new ProducerRecord<>(topic, key, payload);
            producer.send(incomingMessage).get();
        }
    }

    @SneakyThrows
    public void sendMessage(String topic, T payload, Iterable<Header> headers) {
        try (var producer = testProducerFactory.createProducer()) {
            var producerRecord = new ProducerRecord<>(topic, null, UUID.randomUUID().toString(), payload, headers);
            producer.send(producerRecord).get();
        }
    }

    @SneakyThrows
    public void sendMessage(ProducerRecord<String, T> pRecord) {
        try (var producer = testProducerFactory.createProducer()) {
            producer.send(pRecord).get();
        }
    }

    public T getLastMessage(Duration duration, String topic) {
        T payload = null;
        try (var consumer = testConsumerFactory.createConsumer()) {
            consumer.subscribe(Collections.singletonList(topic));
            var records = consumer.poll(duration);
            for (var cRecord : records) {
                payload = cRecord.value();
            }
            return payload;
        }
    }

    public T getMessage(Duration duration, String topic) {
        try (var consumer = testConsumerFactory.createConsumer()) {
            consumer.subscribe(Collections.singletonList(topic));
            var records = consumer.poll(duration);
            return Optional.ofNullable(records)
                    .map(ConsumerRecords::iterator)
                    .map(iter -> (iter.hasNext()) ? iter.next() : null)
                    .map(ConsumerRecord::value)
                    .orElse(null);
        }
    }

    public List<T> getMessages(Duration duration, String topic) {
        List<T> result = new ArrayList<>();
        var records = getConsumerRecords(duration, topic);
        for (var cRecord : records) {
            result.add(cRecord.value());
        }
        return result;
    }

    public ConsumerRecords<String, T> getConsumerRecords(Duration duration, String topic) {
        try (var consumer = testConsumerFactory.createConsumer()) {
            consumer.subscribe(Collections.singletonList(topic));
            return consumer.poll(duration);
        }
    }

    public ConsumerRecord<String, T> getConsumerRecord(Duration duration, String topic) {
        try (var consumer = testConsumerFactory.createConsumer()) {
            consumer.subscribe(Collections.singletonList(topic));
            var records = consumer.poll(duration);
            ConsumerRecord<String, T> lastConsumerRecord = null;
            for (var cRecord : records) {
                lastConsumerRecord = cRecord;
            }
            return lastConsumerRecord;
        }
    }
}
