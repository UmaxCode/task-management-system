package org.umaxcode;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SetSubscriptionAttributesRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;

import java.util.Map;

public class SNSTopicSubscriptionHandler implements RequestHandler<Map<String, String>, String> {

    private final SnsClient snsClient;

    public SNSTopicSubscriptionHandler() {
        snsClient = SnsClient.create();
    }

    @Override
    public String handleRequest(Map<String, String> event, Context context) {

        System.out.println("Event" + event);
        // Extract the user's email from the event
        String userEmail = event.get("email");

        // SNS Topic ARN (can be passed in environment variables or hardcoded)
        String snsTopicArn = event.get("topicArn");

        // Create SNS subscription request
        SubscribeRequest subscribeRequest = SubscribeRequest.builder()
                .protocol("email")  // Protocol is email to receive notifications
                .endpoint(userEmail)  // User's email address to subscribe
                .returnSubscriptionArn(true)
                .topicArn(snsTopicArn)  // SNS Topic ARN
                .build();

        try {
            SubscribeResponse response = snsClient.subscribe(subscribeRequest);
            context.getLogger().log("Subscription result: " + response);

            // Define a filter policy
            String filterPolicy = String.format("{ \"endpointEmail\": [\"%s\"] }", userEmail);

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

            return "Successfully subscribed " + userEmail + " to the SNS topic with filter policy: " + snsTopicArn;
        } catch (Exception e) {
            context.getLogger().log("Error subscribing user: " + e.getMessage());
            return "Failed to subscribe user to SNS topic.";
        }
    }
}
