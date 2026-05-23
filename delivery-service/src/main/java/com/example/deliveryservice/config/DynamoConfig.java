package com.example.deliveryservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;
import java.net.URI;

/**
 * Configuration for DynamoDB in the delivery service. Uses the same
 * environment variables as the user service to support local development
 * against DynamoDB Local.
 */
@Configuration
public class DynamoConfig {

    @Value("${AWS_REGION:us-east-1}")
    private String region;

    @Value("${AWS_ACCESS_KEY_ID:local}")
    private String accessKeyId;

    @Value("${AWS_SECRET_ACCESS_KEY:local}")
    private String secretAccessKey;

    @Value("${DYNAMODB_ENDPOINT:}")
    private String dynamoEndpoint;

    @Bean
    public DynamoDbAsyncClient dynamoDbAsyncClient() {
        DynamoDbAsyncClientBuilder builder = DynamoDbAsyncClient.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKeyId, secretAccessKey)
            ));
        if (dynamoEndpoint != null && !dynamoEndpoint.isEmpty()) {
            builder.endpointOverride(URI.create(dynamoEndpoint));
        }
        return builder.build();
    }
}