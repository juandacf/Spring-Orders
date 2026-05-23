package com.example.userservice.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for Kafka producer. The bootstrap servers and SSL settings
 * are provided via environment variables. Spring Boot automatically picks up
 * the `spring.kafka.*` properties, but we define a producer factory here to
 * explicitly set serializers.
 */
@Configuration
public class KafkaConfig {

    @Value("${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}")
    private String bootstrapServers;

    @Value("${KAFKA_SECURITY_PROTOCOL:PLAINTEXT}")
    private String securityProtocol;

    @Value("${KAFKA_SSL_TRUSTSTORE_LOCATION:}")
    private String truststoreLocation;

    @Value("${KAFKA_SSL_TRUSTSTORE_PASSWORD:}")
    private String truststorePassword;

    @Value("${KAFKA_SSL_KEYSTORE_LOCATION:}")
    private String keystoreLocation;

    @Value("${KAFKA_SSL_KEYSTORE_PASSWORD:}")
    private String keystorePassword;

    @Value("${KAFKA_SSL_KEY_PASSWORD:}")
    private String keyPassword;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        props.put("security.protocol", securityProtocol);
        if (truststoreLocation != null && !truststoreLocation.isEmpty()) {
            props.put("ssl.truststore.location", truststoreLocation);
        }
        if (truststorePassword != null && !truststorePassword.isEmpty()) {
            props.put("ssl.truststore.password", truststorePassword);
        }
        if (keystoreLocation != null && !keystoreLocation.isEmpty()) {
            props.put("ssl.keystore.location", keystoreLocation);
        }
        if (keystorePassword != null && !keystorePassword.isEmpty()) {
            props.put("ssl.keystore.password", keystorePassword);
        }
        if (keyPassword != null && !keyPassword.isEmpty()) {
            props.put("ssl.key.password", keyPassword);
        }
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}