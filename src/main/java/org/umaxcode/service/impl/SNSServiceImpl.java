package org.umaxcode.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.umaxcode.service.SNSService;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;

@Service
@RequiredArgsConstructor
public class SNSServiceImpl implements SNSService {

    private final SnsClient snsClient;

    @Async
    @Override
    public void subscribeToTopic(String email, String topic) {

        SubscribeRequest request = SubscribeRequest.builder()
                .protocol("email")  // Email protocol
                .endpoint(email) // User's email
                .topicArn(topic)  // Topic ARN
                .build();

        snsClient.subscribe(request); // Subscribe the user to SNS
        System.out.println("User : " + email + "subscribed to SNS topic: " + topic);

    }
}
