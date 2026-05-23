package com.example.deliveryservice.config;

import com.example.deliveryservice.model.OrderEventPayload;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.lang.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer configuration for the delivery service. The consumer
 * deserializes messages into {@link OrderEventPayload} objects for further
 * processing. SSL settings are taken from environment variables similar to the
 * producer configuration in the user service.
 */
@Configuration
public class KafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

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

    @Value("${KAFKA_MAX_ATTEMPTS:3}")
    private long maxAttempts;

    @Value("${KAFKA_RETRY_INTERVAL_MS:0}")
    private long retryInterval;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderEventPayload> kafkaListenerContainerFactory() {
        log.info("Initializing kafkaListenerContainerFactory with maxAttempts={} and retryInterval={}", maxAttempts, retryInterval);
        DefaultKafkaConsumerFactory<String, OrderEventPayload> consumerFactory = getOrderEventPayloadDefaultKafkaConsumerFactory();

        ConcurrentKafkaListenerContainerFactory<String, OrderEventPayload> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        org.springframework.util.backoff.FixedBackOff backOff = new org.springframework.util.backoff.FixedBackOff(retryInterval, maxAttempts - 1);
        org.springframework.kafka.listener.DefaultErrorHandler errorHandler =
                new org.springframework.kafka.listener.DefaultErrorHandler(backOff);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    @NonNull
    private DefaultKafkaConsumerFactory<String, OrderEventPayload> getOrderEventPayloadDefaultKafkaConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
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

        // Manual instantiation of deserializers to ensure the correct type configuration.
        // This avoids issues where property-based configuration might not be
        // correctly applied when using ErrorHandlingDeserializer.
        JsonDeserializer<OrderEventPayload> jsonDeserializer = new JsonDeserializer<>(OrderEventPayload.class);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.setUseTypeHeaders(false);

        ErrorHandlingDeserializer<OrderEventPayload> valueDeserializer =
                new ErrorHandlingDeserializer<>(jsonDeserializer);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                valueDeserializer
        );
    }
}