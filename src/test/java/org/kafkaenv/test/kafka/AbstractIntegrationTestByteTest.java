package org.kafkaenv.test.kafka;

import org.junit.jupiter.api.AfterAll;
import org.kafkaenv.test.common.DisableDoubleContextValidation;
import org.kafkaenv.test.kafka.testconfig.App;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.kafkaenv.test.kafka.TestUtil.destroyContainers;

@SpringBootTest(classes = App.class)
@ActiveProfiles("testbyte")
@EnableTestKafka
@DisableDoubleContextValidation(reason = "Test containers with different environment")
public abstract class AbstractIntegrationTestByteTest {

    @AfterAll
    static void destroy() {
        destroyContainers();
    }
}