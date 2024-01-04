package org.kafkaenv.test.kafka.accessor;

import org.junit.jupiter.api.Test;
import org.kafkaenv.test.kafka.AbstractIntegrationTestByteTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaContainerTestAccessorByteTest extends AbstractIntegrationTestByteTest {

    @Autowired
    private KafkaContainerTestAccessor<byte[]> kafkaContainerTestAccessor;

    @Test
    void givenAllContainers_messageSentAndReceived() {
        var request = "test_string";

        kafkaContainerTestAccessor.sendMessage("test-inbyte", request.getBytes(StandardCharsets.UTF_8));
        var response = kafkaContainerTestAccessor.getMessage(Duration.ofSeconds(3), "test-outbyte");

        assertThat(new String(response, StandardCharsets.UTF_8)).isEqualTo(request);
    }
}