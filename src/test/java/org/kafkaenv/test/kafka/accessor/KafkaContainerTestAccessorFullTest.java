package org.kafkaenv.test.kafka.accessor;

import org.apache.avro.specific.SpecificRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.kafkaenv.test.kafka.AbstractIntegrationTestFullTest;
import test.avro.Individual;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaContainerTestAccessorFullTest extends AbstractIntegrationTestFullTest {

    @Autowired
    private KafkaContainerTestAccessor<SpecificRecord> kafkaContainerTestAccessor;

    @Test
    void givenAllContainers_messageSentAndReceived() {
        var request = Individual.newBuilder()
                .setLastname("lastName")
                .setFirstname("firstName")
                .build();

        kafkaContainerTestAccessor.sendMessage("test-infull", request);
        var response = kafkaContainerTestAccessor.getMessage(Duration.ofSeconds(3), "test-outfull");

        assertThat(response).isEqualTo(request);
    }
}