package org.kafkaenv.test.kafka.config;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.kafkaenv.test.kafka.EnableTestKafka;

import java.util.List;

public class TestContainerKafkaContextCustomizerFactory implements ContextCustomizerFactory {

    @Override
    public ContextCustomizer createContextCustomizer(@NonNull Class<?> testClass,
                                                     @NonNull List<ContextConfigurationAttributes> configAttributes) {
        var enableTestKafka = AnnotationUtils.findAnnotation(testClass, EnableTestKafka.class);
        if (enableTestKafka != null) {
            return new KafkaTestContainersContextCustomizer(enableTestKafka);
        }
        return null;
    }
}
