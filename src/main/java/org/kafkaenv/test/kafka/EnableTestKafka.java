package org.kafkaenv.test.kafka;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableTestKafka {

    boolean enableKafkaSasl() default false;

    boolean enableSchemaRegistry() default false;

    boolean enableSchemaRegistryBasicAuth() default false;

    boolean enableKafkaUi() default false;

    boolean destroyContainersAfterTest() default true;

}
