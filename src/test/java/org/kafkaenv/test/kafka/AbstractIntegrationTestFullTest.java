package org.kafkaenv.test.kafka;

import org.junit.jupiter.api.AfterAll;
import org.kafkaenv.test.common.DisableDoubleContextValidation;
import org.kafkaenv.test.kafka.testconfig.App;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = App.class)
@ActiveProfiles("testfull")
@EnableTestKafka(enableKafkaSasl = true, enableSchemaRegistryBasicAuth = true, enableSchemaRegistry = true)
@DisableDoubleContextValidation(reason = "Test containers with different environment")
public abstract class AbstractIntegrationTestFullTest {

    @AfterAll
    static void destroy() {
        TestUtil.destroyContainers();
    }
}