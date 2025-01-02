package org.umaxcode.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.sns.SnsClient;

@Configuration
public class AWSConfig {

    @Value("${application.aws.region}")
    private String awsRegion;

    @Bean
    public CognitoIdentityProviderClient cognitoClient() {
        return CognitoIdentityProviderClient.builder()
                .region(Region.of(awsRegion)) // Replace YOUR_REGION
                .build();
    }

    @Bean
    public SnsClient snsClient() {
        return SnsClient.builder()
                .region(Region.of(awsRegion)) // Set your region
                .build();
    }

    @Bean
    public LambdaClient lambdaClient(){
        return LambdaClient.builder()
                .region(Region.of(awsRegion))
                .build();
    }
}
