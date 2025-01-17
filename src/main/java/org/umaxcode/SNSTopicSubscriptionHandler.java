package org.umaxcode;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.umaxcode.service.impl.SNSServiceImpl;
import software.amazon.awssdk.services.sns.SnsClient;

import java.util.Map;

public class SNSTopicSubscriptionHandler implements RequestHandler<Map<String, String>, Void> {

    private final SnsClient snsClient;

    public SNSTopicSubscriptionHandler() {
        snsClient = SnsClient.create();
    }

    @Override
    public Void handleRequest(Map<String, String> event, Context context) {

        System.out.println("Event" + event);
        // Extract the user's email from the event
        String userEmail = event.get("email");

        // SNS Topic ARN (can be passed in environment variables or hardcoded)
        String snsTopicArn = event.get("topicArn");

        SNSServiceImpl.subscribeToTopic(snsClient, context, snsTopicArn, userEmail);

        return null;
    }
}
