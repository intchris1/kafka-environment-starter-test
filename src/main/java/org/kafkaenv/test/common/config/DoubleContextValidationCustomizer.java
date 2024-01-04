package org.kafkaenv.test.common.config;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

@RequiredArgsConstructor
@EqualsAndHashCode
public class DoubleContextValidationCustomizer implements ContextCustomizer {

    private static int count = 0;

    @Override
    public void customizeContext(@NonNull ConfigurableApplicationContext context, @NonNull MergedContextConfiguration mergedConfig) {
        count++;
        if (count == 2) {
            throw new IllegalStateException("Application context started twice: check your test configuration and application beans");
        }
    }
}
