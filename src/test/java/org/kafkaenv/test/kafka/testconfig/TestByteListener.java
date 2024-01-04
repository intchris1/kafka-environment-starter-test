package org.kafkaenv.test.kafka.testconfig;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Profile("testbyte")
public class TestByteListener {

    private final KafkaTemplate<String, String> template;

    @KafkaListener(topics = "test-inbyte")
    public void listen(ConsumerRecord<String, String> consumerRecord) {
        template.send("test-outbyte", consumerRecord.value());
    }
}
