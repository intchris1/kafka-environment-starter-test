package org.kafkaenv.test.kafka.testconfig;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import test.avro.Individual;

@RequiredArgsConstructor
@Component
@Profile("testfull")
public class TestFullListener {

    private final KafkaTemplate<String, SpecificRecord> template;

    @KafkaListener(topics = "test-infull")
    public void listen(ConsumerRecord<String, Individual> consumerRecord) {
        template.send("test-outfull", consumerRecord.value());
    }
}
