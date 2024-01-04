package org.kafkaenv.test.common.config;

import org.kafkaenv.test.common.DisableDoubleContextValidation;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;

import java.util.List;

public class DoubleContextValidationCustomizerFactory implements ContextCustomizerFactory {

    @Override
    public ContextCustomizer createContextCustomizer(@NonNull Class<?> testClass,
                                                     @NonNull List<ContextConfigurationAttributes> configAttributes) {
        var disableDoubleContextValidation = AnnotationUtils.findAnnotation(testClass, DisableDoubleContextValidation.class);
        if (disableDoubleContextValidation == null) {
            return new DoubleContextValidationCustomizer();
        }
        return null;
    }
}
