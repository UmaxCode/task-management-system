package org.umaxcode.service.impl;

import com.amazonaws.services.lambda.runtime.Context;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.umaxcode.service.SNSService;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SetSubscriptionAttributesRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;

@Service
@RequiredArgsConstructor
public class SNSServiceImpl implements SNSService {

    public static void subscribeToTopic(SnsClient snsClient, Context context, String topic, String email) {

        // Create SNS subscription request
        SubscribeRequest subscribeRequest = SubscribeRequest.builder()
                .protocol("email")  // Protocol is email to receive notifications
                .endpoint(email)  // User's email address to subscribe
                .returnSubscriptionArn(true)
                .topicArn(topic)  // SNS Topic ARN
                .build();

        try {
            SubscribeResponse response = snsClient.subscribe(subscribeRequest);
            context.getLogger().log("Subscription result: " + response);

            // Define a filter policy
            String filterPolicy = String.format("{ \"endpointEmail\": [\"%s\"] }", email);

            // Set the filter policy for the subscription
            String subscriptionArn = response.subscriptionArn();

            System.out.println("SubscriptionArn" + subscriptionArn);
            SetSubscriptionAttributesRequest filterPolicyRequest = SetSubscriptionAttributesRequest.builder()
                    .subscriptionArn(subscriptionArn)
                    .attributeName("FilterPolicy")
                    .attributeValue(filterPolicy)
                    .build();

            snsClient.setSubscriptionAttributes(filterPolicyRequest);

            context.getLogger().log("Filter policy set for subscription: " + subscriptionArn);
            context.getLogger().log("Successfully subscribed " + email + " to the SNS topic with filter policy: " + topic);

        } catch (Exception e) {
            context.getLogger().log("Error subscribing user: " + e.getMessage());
        }
    }
}
